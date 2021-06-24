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

import java.{ util => ju }

import scala.collection.mutable.Buffer

import com.twitter.classpathverifier.Reference

trait Summary {

  def summary(
      className: String,
      kind: ClassKind = ClassKind.Class,
      parent: String = "java.lang.Object",
      interfaces: List[String] = Nil
  )(op: ClassSummaryBuilder => Unit): ClassSummary = {
    val builder = new ClassSummaryBuilder(className, parent)
    op(builder)
    ClassSummary(kind, className, Option(parent), interfaces, builder.methods)
  }

  class ClassSummaryBuilder(className: String, parent: String) {
    private val buffer = Buffer.empty[MethodSummary]

    def methods: List[MethodSummary] = buffer.toList

    def methDep(name: String): MethodDependency.Static = {
      buffer.find(_.methodName == name) match {
        case None =>
          throw new ju.NoSuchElementException(s"Method '$name' not found in current builder")
        case Some(m) =>
          MethodDependency.Static(Reference.Method(className, name, m.descriptor))
      }
    }

    def meth(
        name: String,
        descriptor: String,
        dependencies: Dependency*
    ): ClassSummaryBuilder = {
      buffer += MethodSummary(
        className,
        name,
        descriptor,
        dependencies.toList,
        isAbstract = false
      )
      this
    }

    def abstractMeth(name: String, descriptor: String): ClassSummaryBuilder = {
      buffer += MethodSummary(className, name, descriptor, Nil, isAbstract = true)
      this
    }

    def emptyCtor: ClassSummaryBuilder = {
      buffer += MethodSummary(
        className,
        "<init>",
        "()V",
        List(
          ClassDependency(Reference.Clazz(parent)),
          MethodDependency.Static(Reference.Method(parent, "<init>", "()V"))
        ),
        false
      )
      this
    }

    def obj: ClassSummaryBuilder = {
      emptyCtor
      buffer += MethodSummary(
        className,
        "<clinit>",
        "()V",
        List(
          ClassDependency(Reference.Clazz(s"${className}")),
          ClassDependency(Reference.Clazz(s"${className}")),
          methDep("<init>")
        ),
        false
      )
      this
    }

    def deserializeLambda(lambda: String): ClassSummaryBuilder =
      meth(
        "$deserializeLambda$",
        "(Ljava.lang.invoke.SerializedLambda;)Ljava.lang.Object;",
        MethodDependency.Dynamic(
          Reference.Method(
            "scala.runtime.LambdaDeserialize",
            "bootstrap",
            "(Ljava.lang.invoke.MethodHandles$Lookup;Ljava.lang.String;Ljava.lang.invoke.MethodType;[Ljava.lang.invoke.MethodHandle;)Ljava.lang.invoke.CallSite;"
          )
        ),
        methDep(lambda)
      )

    val altMetafactory: MethodDependency.Dynamic =
      MethodDependency.Dynamic(
        Reference.Method(
          "java.lang.invoke.LambdaMetafactory",
          "altMetafactory",
          "(Ljava.lang.invoke.MethodHandles$Lookup;Ljava.lang.String;Ljava.lang.invoke.MethodType;[Ljava.lang.Object;)Ljava.lang.invoke.CallSite;"
        )
      )

    def main(deps: Dependency*): ClassSummaryBuilder =
      meth("main", "([Ljava.lang.String;)V", deps: _*)
  }
}
