/*
 * Copyright 2021 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.classpathverifier.linker

import java.io.InputStream

import scala.collection.compat.immutable.LazyList
import scala.collection.mutable.Buffer

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.descriptors.Parser
import com.twitter.classpathverifier.descriptors.Type
import com.twitter.classpathverifier.diagnostics.ClassfileVersionError
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

  def collect(ref: Reference.Clazz, stream: InputStream)(implicit ctx: Context): ClassSummary = {
    val buffer = Buffer.empty[MethodSummary]
    val visitor = new Visitor(buffer)
    val reader = new ClassReader(stream)
    val majorVersion = reader.readUnsignedShort(6)

    if (majorVersion <= ctx.config.javaMajorApiVersion) {
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
      ClassSummary(kind, Type.pathToName(visitor.fullName), parent, interfaces, methods)
    } else {
      val error = ClassfileVersionError(ref, majorVersion)
      ctx.reporter.report(error)
      ClassSummary.Missing
    }
  }

  private class Visitor(buffer: Buffer[MethodSummary]) extends ClassVisitor(Opcodes.ASM9) {

    private var myFullName: String = _
    def fullName: String = myFullName

    override def visit(
        version: Int,
        access: Int,
        name: String,
        signature: String,
        superName: String,
        interfaces: Array[String]
    ): Unit = {
      myFullName = name
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
            Type.pathToName(myFullName),
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
