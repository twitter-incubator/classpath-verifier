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

import scala.collection.mutable.Buffer

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.descriptors.Type
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

sealed case class MethodSummary(
    className: String,
    methodName: String,
    descriptor: String,
    dependencies: List[Dependency],
    isAbstract: Boolean
) {
  def ref: Reference.Method = Reference.Method(className, methodName, descriptor)
}

object MethodSummary {
  object Empty
      extends MethodSummary(
        className = "",
        methodName = "",
        descriptor = "",
        dependencies = Nil,
        isAbstract = false
      )

  class Visitor(callback: List[Dependency] => Unit) extends MethodVisitor(Opcodes.ASM9) {
    private val dependencies = Buffer.empty[Dependency]
    override def visitEnd(): Unit =
      callback(dependencies.toList)

    private def actualOwner(owner: String): String =
      if (isArrayClass(owner)) Constants.JavaLangObjectClass
      else Type.pathToName(owner)

    private def toMethRef(handle: Handle): Reference.Method = {
      val owner = actualOwner(handle.getOwner())
      new Reference.Method(owner, handle.getName(), Type.pathToName(handle.getDesc()))
    }

    override def visitInvokeDynamicInsn(
        name: String,
        descriptor: String,
        bootstrapMethodHandle: Handle,
        bootstrapMethodArguments: Object*
    ): Unit = {
      super.visitInvokeDynamicInsn(
        name,
        descriptor,
        bootstrapMethodHandle,
        bootstrapMethodArguments: _*
      )
      dependencies += MethodDependency.Dynamic(toMethRef(bootstrapMethodHandle))
      bootstrapMethodArguments.foreach {
        case handle: Handle => dependencies += MethodDependency.Static(toMethRef(handle))
        case _              => ()
      }
    }

    override def visitMethodInsn(
        opcode: Int,
        rawOwner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ): Unit = {
      super.visitMethodInsn(opcode, rawOwner, name, descriptor, isInterface)
      val owner = actualOwner(rawOwner)
      val reference = new Reference.Method(owner, name, Type.pathToName(descriptor))
      if (name == Constants.InitMethod) {
        val clazzReference = new Reference.Clazz(owner)
        dependencies += ClassDependency(clazzReference)
      }
      dependencies += MethodDependency.Static(reference)
    }

    override def visitTypeInsn(
        opcode: Int,
        classType: String
    ): Unit = {
      super.visitTypeInsn(opcode, classType)
      if (opcode != Opcodes.ANEWARRAY) {
        val tpe = actualOwner(classType)
        val reference = new Reference.Clazz(tpe)
        dependencies += new ClassDependency(reference)
      }
    }
  }

  private def isArrayClass(name: String): Boolean =
    name.startsWith(Constants.ArrayMarker)
}
