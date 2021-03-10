package com.twitter.classpathverifier.linker

import java.io.InputStream

import scala.collection.compat.immutable.LazyList
import scala.collection.mutable.Buffer

import com.twitter.classpathverifier.descriptors.Parser
import com.twitter.classpathverifier.descriptors.Type
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

sealed case class ClassSummary(
    kind: ClassKind,
    name: String,
    parent: Option[String],
    interfaces: List[String],
    methods: List[MethodSummary]
) {

  val superTypes: List[String] = parent.toList ++ interfaces

  def subtypeOf(
      summarize: Summarizer,
      parent: String
  )(implicit ctx: Context): Boolean = {
    this.name == parent ||
    this.parent.exists(_ == parent) ||
    this.interfaces.contains(parent) ||
    this.superTypes.exists(summarize(_).subtypeOf(summarize, parent))
  }

  def resolveDep(
      summarize: Summarizer,
      dep: MethodDependency
  )(implicit ctx: Context): Option[MethodSummary] = {
    dep match {
      case MethodDependency.Dynamic(callSiteRef) =>
        val methodName = callSiteRef.methodName
        val callSiteDesc = Parser.parse(callSiteRef.descriptor)
        allMethods(summarize).find { method =>
          !method.isAbstract &&
          method.methodName == methodName &&
          callSiteDesc.compatibleWith(summarize, Parser.parse(method.descriptor))
        }

      case MethodDependency.Static(callSiteRef) =>
        val methodName = callSiteRef.methodName
        val descriptor = callSiteRef.descriptor
        allMethods(summarize).find { method =>
          !method.isAbstract &&
          method.methodName == methodName &&
          method.descriptor == descriptor
        }
    }
  }

  private def allMethods(summarize: Summarizer)(implicit ctx: Context): LazyList[MethodSummary] = {
    val parentsMethods =
      LazyList
        .from(superTypes)
        .map(summarize(_))
        .flatMap(_.allMethods(summarize))

    methods ++: parentsMethods
  }
}

object ClassSummary {
  object Missing
      extends ClassSummary(
        kind = ClassKind.Class,
        name = "",
        parent = None,
        interfaces = Nil,
        methods = Nil
      ) {
    override def resolveDep(
        summarize: Summarizer,
        dep: MethodDependency
    )(implicit ctx: Context): Some[MethodSummary.Empty.type] = {
      Some(MethodSummary.Empty)
    }
  }

  def collect(stream: InputStream): ClassSummary = {
    val buffer = Buffer.empty[MethodSummary]
    val visitor = new Visitor(buffer)
    val reader = new ClassReader(stream)
    reader.accept(visitor, 0)
    val parent = Option(Type.pathToName(reader.getSuperName()))
    val interfaces = reader.getInterfaces().toList.map(Type.pathToName)
    val methods = buffer.toList
    val isInterface = (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0
    val isAbstract = (reader.getAccess() & Opcodes.ACC_ABSTRACT) != 0
    val kind =
      if (isInterface) ClassKind.Interface
      else if (isAbstract) ClassKind.AbstractClass
      else ClassKind.Class
    ClassSummary(kind, Type.pathToName(visitor.className), parent, interfaces, methods)
  }

  private class Visitor(buffer: Buffer[MethodSummary]) extends ClassVisitor(Opcodes.ASM9) {

    private var myClassName: String = _
    def className: String = myClassName

    override def visit(
        version: Int,
        access: Int,
        name: String,
        signature: String,
        superName: String,
        interfaces: Array[String]
    ): Unit = {
      myClassName = name
      super.visit(version, access, name, signature, superName, interfaces)
    }

    override def visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String,
        exceptions: Array[String]
    ): MethodVisitor = {
      val isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0
      val callback = (deps: List[Dependency]) => {
        val summary =
          MethodSummary(
            Type.pathToName(myClassName),
            name,
            Type.pathToName(descriptor),
            deps,
            isAbstract
          )
        buffer += summary
        ()
      }
      new MethodSummary.Visitor(callback)
    }
  }
}
