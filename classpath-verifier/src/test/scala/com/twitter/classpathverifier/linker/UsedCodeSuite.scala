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

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.config.Config
import com.twitter.classpathverifier.config.UsedCodeConfig
import com.twitter.classpathverifier.testutil.Build
import com.twitter.classpathverifier.testutil.Project
import com.twitter.classpathverifier.testutil.TestBuilds

class UsedCodeSuite extends BaseLinkerSuite {
  unusedJarsInBuild(TestBuilds.plain, "Plain.foo(1, \"hello\")")

  unusedJarsInBuild(
    TestBuilds.extraLibrary,
    "Plain.foo(1, \"hello\")",
    Set(
      "cats-core-2.6.1.jar",
      "cats-kernel-2.6.1.jar",
      "simulacrum-scalafix-annotations-0.5.4.jar",
    )
  )

  unusedJarsInBuild(
    TestBuilds.extraLibrary,
    "Plain.foo(1, \"hello\")",
    Set.empty,
    List("**/cats-core*.jar", "**/cats-kernel*.jar", "**/simulacrum-scalafix-annotations*.jar")
  )

  // TODO: catch unused subproject in test
  unusedJarsInBuild(TestBuilds.unusedSubproject, "Plain.foo(1, \"hello\")")

  private def unusedJarsInBuild(
      build: Build,
      code: String,
      expected: Set[String] = Set.empty,
      excludePatterns: List[String] = Nil,
  )(implicit
      loc: munit.Location
  ): Unit = {
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
      val obtainedUnusedJars = unusedJars("test.main.Main", classpath.full, excludePatterns)
      val obtainedFileNames = obtainedUnusedJars
        .toSet[Path]
        .map(_.getFileName().toString)
        .map(_.replaceAll("""_(\d)(\.\d+)?""", "")) // drop cross suffix
      assertEquals(obtainedFileNames, expected)
    }
  }

  private def unusedJars(
      entrypoint: String,
      classpath: List[Path],
      excludePatterns: List[String]
  ): Seq[Path] = {
    val reference = Reference.Method(entrypoint).getOrElse(fail(s"Cannot parse: '$entrypoint'"))
    val usedCodeConfig = UsedCodeConfig(enabled = true, excludePatterns = excludePatterns)
    val config = Config.empty.copy(
      classpath = classpath,
      entrypoints = reference :: Nil,
      usedCodeConfig = usedCodeConfig,
    )
    val ctx = Context.init(config)
    Linker.verify(ctx)
    ctx.used.unusedJars(ctx)
  }

}
