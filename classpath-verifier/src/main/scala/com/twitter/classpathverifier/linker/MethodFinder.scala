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

import java.nio.file.Path

import scala.collection.mutable.Buffer

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.descriptors.Type
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class MethodFinder(classpath: List[Path]) {
  private val classFinder = new ClassFinder(classpath)

  def find(entrypoint: Reference.Method): Option[MethodVisitor] =
    classFinder
      .find(entrypoint.fullClassName)
      .map(_.stream.use { clazz =>
        val reader = new ClassReader(clazz)
        val descriptor = Type.nameToPath(entrypoint.descriptor)
        val visitor =
          new MethodFinderVisitor(entrypoint.methodName, descriptor)
        reader.accept(visitor, 0)
        visitor.results
      })
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
