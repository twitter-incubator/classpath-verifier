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

import com.twitter.classpathverifier.BuildInfo
import com.twitter.classpathverifier.testutil.Build
import com.twitter.classpathverifier.testutil.TestBuilds

class ClassSummarySuite extends BaseLinkerSuite with Summary {

  testSummary("test.MyTrait", TestBuilds.abstractMembers, ClassKind.Interface) {
    _.abstractMeth("foo", "(Ltest.MyTrait;)I")
  }

  testSummary(
    "test.MyAbstractClass",
    TestBuilds.abstractMembers,
    ClassKind.AbstractClass,
    interfaces = List("test.MyTrait")
  ) {
    _.emptyCtor
  }

  testSummary("test.MyConcreteClass", TestBuilds.abstractMembers, parent = "test.MyAbstractClass") {
    _.emptyCtor
      .meth("foo", "(Ltest.MyTrait;)I")
  }

  testSummary("test.Valid", TestBuilds.valid) { b =>
    b.emptyCtor
      .meth(
        "multiply",
        "(II)I",
        classDep("test.Helper"),
        methDep("test.Helper#<init>:(II)V"),
        methDep("test.Helper#multiply:()I"),
        classDep("test.Helper")
      )
      .meth(
        "foo",
        "(I)V",
        b.methDep("multiply"),
        methDep("java.io.PrintStream#println:(I)V")
      )
  }

  testSummary("test.ValidObject$", TestBuilds.valid, disableOnScala3 = true) { b =>
    b.obj
      .staticMeth("$anonfun$main$1", "([Ljava.lang.String;)I")
      .deserializeLambda("$anonfun$main$1")
      .main(
        classDep("cats.syntax.TryOps$"),
        methDep(
          "cats.syntax.TryOps$#toValidated$extension:(Lscala.util.Try;)Lcats.data.Validated;"
        ),
        classDep("cats.syntax.package$try_$"),
        methDep(
          "cats.syntax.package$try_$#catsSyntaxTry:(Lscala.util.Try;)Lscala.util.Try;"
        ),
        b.altMetafactory,
        methDep("scala.Predef$#println:(Ljava.lang.Object;)V"),
        methDep("scala.util.Try$#apply:(Lscala.Function0;)Lscala.util.Try;"),
        classDep("test.Valid"),
        methDep("test.Valid#<init>:()V"),
        methDep("test.Valid#foo:(I)V"),
        b.methDep("$anonfun$main$1"),
      )
  }

  testSummary("test.InheritedMembers", TestBuilds.inherited) { b =>
    b.emptyCtor
      .main(
        classDep("test.Child"),
        methDep("test.Child#<init>:()V"),
        methDep("test.Child#foo:()I"),
        methDep("test.Child#bar:()I"),
        methDep("test.Child#baz:()I"),
        methDep("test.Child#toString:()Ljava.lang.String;"),
        classDep("test.Child")
      )
  }

  testSummary("test.Parent", TestBuilds.inherited, interfaces = List("test.ParentInterface")) { b =>
    b.emptyCtor
      .meth("foo", "()I")
  }

  testSummary(
    "test.Middle",
    TestBuilds.inherited,
    parent = "test.Parent",
    interfaces = List("test.MiddleInterface")
  ) { b =>
    b.emptyCtor
      .meth(
        "bar",
        "()I",
        methDep("test.Middle#foo:()I")
      )
  }

  testSummary(
    "test.Child",
    TestBuilds.inherited,
    parent = "test.Middle",
    interfaces = List("test.ChildInterface")
  ) { b =>
    b.emptyCtor
      .meth(
        "baz",
        "()I",
        methDep("test.Child#foo:()I"),
        methDep("test.Child#bar:()I")
      )
  }

  testSummary("test.ArrayClone", TestBuilds.arrayClone) { b =>
    b.emptyCtor
      .main(
        methDep("java.lang.Object#clone:()Ljava.lang.Object;"),
      )
  }

  testSummary("test.Lambda", TestBuilds.lambda, disableOnScala3 = true) { b =>
    b.emptyCtor
      .staticMeth(
        "$anonfun$main$1",
        "(Ljava.lang.String;)Lcats.data.Validated$Valid;",
        methDep("cats.data.Validated$Valid#<init>:(Ljava.lang.Object;)V"),
        classDep("cats.data.Validated$Valid"),
        classDep("cats.data.Validated$Valid")
      )
      .deserializeLambda("$anonfun$main$1")
      .meth(
        "applyToFoo",
        "(Lscala.Function1;)Ljava.lang.Object;",
        methDep("scala.Function1#apply:(Ljava.lang.Object;)Ljava.lang.Object;")
      )
      .main(b.altMetafactory, b.methDep("$anonfun$main$1"), b.methDep("applyToFoo"))
  }

  testSummary("test.Override", TestBuilds.overrideMembers) { b =>
    b.emptyCtor
      .main(
        methDep("test.MyBaseTrait#foo:(Ljava.lang.Object;)Ljava.lang.Object;"),
        methDep("test.MyTraitImpl#foo:(Ljava.lang.Object;)Ltest.MyBaseTrait;")
      )
  }

  private def testSummary(
      className: String,
      build: Build,
      kind: ClassKind = ClassKind.Class,
      parent: String = "java.lang.Object",
      interfaces: List[String] = Nil,
      disableOnScala3: Boolean = false
  )(builder: ClassSummaryBuilder => Unit)(implicit loc: munit.Location): Unit = {
    test(s"Summarize $className") {
      implicit val ctx: Context = failOnError
      val expectedSummary = summary(className, kind, parent, interfaces)(builder)
      val classpath = build.allClasspath.full
      Finder(classpath).map(new ClassSummarizer(_)).use { summarizer =>
        val obtainedSummary =
          summarizer(className)

        if (BuildInfo.scalaBinaryVersion != "3" || !disableOnScala3) {
          val obtained = comparableSummary(obtainedSummary)
          val expected = comparableSummary(expectedSummary)
          assertEquals(obtained, expected)
        }
      }
    }
  }

  private def comparableSummary(summary: ClassSummary): ClassSummary =
    summary.copy(
      interfaces = summary.interfaces.sorted,
      methods = comparableMethodSummaries(summary.methods)
    )

  private def comparableMethodSummaries(methods: List[MethodSummary]): List[MethodSummary] =
    methods.map(comparableMethodSummary).sortBy { method =>
      (method.fullClassName, method.methodName, method.descriptor, method.isAbstract)
    }

  private def comparableMethodSummary(summary: MethodSummary): MethodSummary =
    summary.copy(
      dependencies = comparableDependencies(summary.dependencies)
    )

  private def comparableDependencies(dependencies: List[Dependency]): List[Dependency] =
    dependencies.distinct.sortBy(_.ref.show)
}
