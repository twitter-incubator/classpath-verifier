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

import java.nio.file.Files
import java.nio.file.Path

import com.twitter.classpathverifier.BuildInfo
import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.config.Config
import com.twitter.classpathverifier.diagnostics.ClassfileVersionError
import com.twitter.classpathverifier.diagnostics.LinkerError
import com.twitter.classpathverifier.diagnostics.MissingClassError
import com.twitter.classpathverifier.diagnostics.MissingMethodError
import com.twitter.classpathverifier.testutil.Build
import com.twitter.classpathverifier.testutil.IOUtil
import com.twitter.classpathverifier.testutil.Project
import com.twitter.classpathverifier.testutil.TestBuilds
import org.objectweb.asm.Opcodes

class LinkerSuite extends BaseLinkerSuite {

  linksInBuild(TestBuilds.plain, "Plain.foo(1, \"hello\")")

  linksInBuild(TestBuilds.privateAccess, "Proxy.proxy")

  linksInBuild(TestBuilds.valid, "new Valid().foo(4)")
  linksInBuild(
    TestBuilds.valid,
    s"""|import cats.syntax.try_._
        |import scala.util.Try
        |val validated = Try(10 / args.length).toValidated
        |println(validated)""".stripMargin
  )

  linksInBuild(TestBuilds.inherited, "new Child().foo()")
  linksInBuild(TestBuilds.inherited, "new Child().bar()")
  linksInBuild(TestBuilds.inherited, "new Child().baz()")
  linksInBuild(TestBuilds.inherited, "new Child().toString()")
  linksInBuild(TestBuilds.inherited, "new InheritedMembers().main(args)")

  linksInBuild(
    TestBuilds.abstractMembers,
    """|val asTrait: MyTrait = new MyConcreteClass
       |asTrait.foo(asTrait)""".stripMargin
  )
  linksInBuild(
    TestBuilds.abstractMembers,
    """|val asAbstract: MyAbstractClass = new MyConcreteClass
       |asAbstract.foo(asAbstract)""".stripMargin
  )
  linksInBuild(
    TestBuilds.abstractMembers,
    """|val asConcrete: MyConcreteClass = new MyConcreteClass
       |asConcrete.foo(asConcrete)""".stripMargin
  )

  linksInBuild(TestBuilds.lambda, "new Lambda().main(args)")
  linksInBuild(TestBuilds.lambda, "new Lambda().applyToFoo(println)")

  linksInBuild(
    TestBuilds.overrideMembers,
    "new Override().main(args)"
  )

  changeIntroducesErrors(
    TestBuilds.renameMethod,
    "test.Main$",
    List(
      MissingMethodError(
        Reference.Method("test.Library$", "originalMethod", "()I"),
        List(
          Reference.Method("test.Main$", "proxy0", "()I"),
          Reference.Method("test.Main$", "proxy1", "()I"),
          Reference.Method("test.Main$", "main", "([Ljava.lang.String;)V"),
        )
      )
    )
  )

  changeIntroducesErrors(
    TestBuilds.testValueType,
    "test.Main$#typeCheck:(Ljava.lang.Object;)Z",
    List(
      MissingClassError(
        classRef("test.TheClass"),
        Reference.Method("test.Main$", "typeCheck", "(Ljava.lang.Object;)Z") :: Nil
      )
    )
  )

  changeIntroducesErrors(
    TestBuilds.castValueType,
    "test.Main$#typeCast:(Ljava.lang.Object;)V",
    List(
      MissingClassError(
        classRef("test.CastClass"),
        Reference.Method("test.Main$", "typeCast", "(Ljava.lang.Object;)V") :: Nil
      )
    )
  )

  changeIntroducesErrors(
    TestBuilds.missingParentClass,
    "test.Main$#direct:()V",
    List(
      MissingClassError(
        classRef("test.Parent"),
        Reference.Method("test.Main$", "direct", "()V") :: Nil
      )
    )
  )

  changeIntroducesErrors(
    TestBuilds.missingParentClass,
    "test.Main$#transitive:()V",
    List(
      MissingClassError(
        classRef("test.TransitiveParent0"),
        Reference.Method("test.Main$", "transitive", "()V") :: Nil
      )
    )
  )

  changeIntroducesErrors(
    TestBuilds.missingInterface,
    "test.Main$#direct:()V",
    List(
      MissingClassError(
        classRef("test.Interface"),
        Reference.Method("test.Main$", "direct", "()V") :: Nil
      )
    )
  )

  changeIntroducesErrors(
    TestBuilds.missingInterface,
    "test.Main$#transitive:()V",
    List(
      MissingClassError(
        classRef("test.InterfaceTransitive0"),
        Reference.Method("test.Main$", "transitive", "()V") :: Nil
      )
    )
  )

  changeIntroducesErrors(
    TestBuilds.missingParentClassInVal,
    "test.Main$#main:()V",
    List(
      MissingClassError(
        classRef("test.Parent"),
        Reference.Method("test.Main$", "<clinit>", "()V") :: Nil
      ),
    )
  )

  changeIntroducesErrors(
    TestBuilds.missingParentClassInValInModule,
    "test.Main$#main:()V",
    List(
      MissingClassError(
        classRef("test.Parent"),
        List(
          Reference.Method("test.DataModule$", "<clinit>", "()V"),
          Reference.Method("test.Main$", "<clinit>", "()V"),
        )
      ),
    )
  )

  linksInBuild(TestBuilds.fastpass, "scala.meta.fastpass.Fastpass.main(args)")
  linksInBuild(
    TestBuilds.scalac,
    if (BuildInfo.scalaBinaryVersion == "3") "dotty.tools.dotc.Main.main(args)"
    else "scala.tools.nsc.Main.main(args)"
  )

  test("Detects classes that cannot be read by this JDK") {
    val maxAsmVersion: Byte = Opcodes.V16
    implicit val ctx = Context.init(Config.empty)

    assume(ctx.config.javaMajorApiVersion < maxAsmVersion, "Latest Java version supported by ASM.")

    val arrayCloneClassfile =
      TestBuilds.arrayClone.classpath("root").classesDir.resolve("test").resolve("ArrayClone.class")
    assert(Files.isRegularFile(arrayCloneClassfile))
    val bytes = Files.readAllBytes(arrayCloneClassfile)

    // Set the major version to the latest version supported by ASM, which is assumed to be newer
    // than what the curent JVM supports, then write the "broken" classfile.
    bytes(6) = 0
    bytes(7) = maxAsmVersion

    IOUtil.withTempDirectory { temp =>
      val newClassfile = temp.resolve("test").resolve("ArrayClone.class")
      Files.createDirectories(newClassfile.getParent)
      Files.write(newClassfile, bytes)
      for {
        finder <- Finder(temp :: Nil)
        summarizer = new ClassSummarizer(finder)
      } summarizer("test.ArrayClone")

      val expectedErrors =
        ClassfileVersionError(Reference.Clazz("test.ArrayClone"), maxAsmVersion) :: Nil
      assertEquals(ctx.reporter.errors, expectedErrors)
    }
  }

  test("find classes in composite jar") {
    IOUtil.withTempDirectory { tmp =>
      val v1Jar = tmp.resolve("v1.jar")
      val mainJar = tmp.resolve("main.jar")
      IOUtil.buildJarIn(
        v1Jar,
        TestBuilds.castValueType.classpath("v1").classesDir :: Nil,
        None,
        None
      )
      IOUtil.buildJarIn(
        mainJar,
        TestBuilds.castValueType.classpath("main").classesDir :: Nil,
        None,
        Some("v1.jar" :: Nil)
      )

      assertEquals(linkErrors("test.Main$", mainJar :: Nil), Nil)
    }
  }

  private def changeIntroducesErrors(
      build: Build,
      entrypoint: String,
      errors: List[LinkerError]
  )(implicit loc: munit.Location): Unit =
    test(s"${build.name}: `$entrypoint` links with v1, not with v2") {
      assertEquals(linkErrors(entrypoint, build.mainClasspath), Nil)
      assertEquals(linkErrors(entrypoint, build.brokenMainClasspath), errors)
    }

  private def linksInBuild(build: Build, code: String)(implicit loc: munit.Location): Unit = {
    test(s"${build.name}: `$code`") {
      val testProj = Project("test-project")
        .withSource(s"""|package test
                        |package main
                        |class Main {
                        |  def main(args: Array[String]): Unit = { $code }
                        |}""".stripMargin)
        .dependsOn(build)
      val testBuild = Build("test-build", testProj)
      val classpath = testBuild.classpath("test-project")
      val obtainedErrors = linkErrors("test.main.Main", classpath.full)
      assertEquals(obtainedErrors, Nil)
    }
  }

  private def linkErrors(entrypoint: String, classpath: List[Path]): Seq[LinkerError] = {
    val reference = Reference.Method(entrypoint).getOrElse(fail(s"Cannot parse: '$entrypoint'"))
    val config = Config.empty.copy(classpath = classpath, entrypoints = reference :: Nil)
    val ctx = Context.init(config)
    Linker.verify(ctx)
    ctx.reporter.errors
  }

}
