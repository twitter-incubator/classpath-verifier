package com.twitter.classpathverifier.linker

import java.nio.file.Path

import scala.collection.mutable.Buffer

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.descriptors.Type
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class MethodFinder(classpath: List[Path]) {
  val classFinder = new ClassFinder(classpath)

  def find(entrypoint: Reference.Method): Option[MethodVisitor] =
    classFinder
      .find(entrypoint.className)
      .map { clazz =>
        val reader = new ClassReader(clazz)
        val descriptor = Type.nameToPath(entrypoint.descriptor)
        val visitor =
          new MethodFinderVisitor(entrypoint.methodName, descriptor)
        reader.accept(visitor, 0)
        visitor.results
      }
      .getOrElse(Nil) match {
      case Nil =>
        None
      case visitor :: Nil =>
        Some(visitor)
      case _ =>
        None
    }

  private class MethodFinderVisitor(
      expectedName: String,
      expectedDescriptor: String
  ) extends ClassVisitor(Opcodes.ASM7) {

    private var needles: Buffer[MethodVisitor] = Buffer.empty

    def results: List[MethodVisitor] = needles.toList

    override def visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String,
        exceptions: Array[String]
    ): MethodVisitor = {
      if (name == expectedName && expectedDescriptor == descriptor)
        needles += super.visitMethod(
          access,
          name,
          descriptor,
          signature,
          exceptions
        )
      null
    }
  }
}
