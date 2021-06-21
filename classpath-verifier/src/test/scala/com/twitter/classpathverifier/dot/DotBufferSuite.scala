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

package com.twitter.classpathverifier.dot

import java.nio.file.Path

import scala.collection.mutable.LinkedHashSet

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.config.Config
import com.twitter.classpathverifier.linker.BaseLinkerSuite
import com.twitter.classpathverifier.linker.Context
import com.twitter.classpathverifier.linker.Linker
import com.twitter.classpathverifier.testutil.Build
import com.twitter.classpathverifier.testutil.Project
import com.twitter.classpathverifier.testutil.TestBuilds
class DotBufferSuite extends BaseLinkerSuite {

  testGraphOf(TestBuilds.inherited, """new test.Child().baz()""") { obtainedEvents =>
    val expectedEvents = List(
      AddDependency(Reference.Method.NoMethod, methRef("test.main.Main")),
      AddDependency(methRef("test.main.Main"), classRef("test.Child")),
      AddDependency(methRef("test.main.Main"), methRef("test.Child#<init>:()V")),
      AddDependency(methRef("test.main.Main"), methRef("test.Child#baz:()I")),
      AddDependency(methRef("test.main.Main"), classRef("test.Middle")),
      AddDependency(methRef("test.main.Main"), classRef("test.ChildInterface")),
      AddDependency(methRef("test.Child#<init>:()V"), methRef("test.Middle#<init>:()V")),
      AddDependency(methRef("test.Child#baz:()I"), methRef("test.Child#bar:()I")),
      AddDependency(methRef("test.Child#baz:()I"), methRef("test.Child#foo:()I")),
      AddDependency(methRef("test.main.Main"), classRef("test.Parent")),
      AddDependency(methRef("test.main.Main"), classRef("test.MiddleInterface")),
      AddDependency(methRef("test.main.Main"), classRef("java.lang.Object")),
      AddDependency(methRef("test.Middle#<init>:()V"), methRef("test.Parent#<init>:()V")),
      AddDependency(methRef("test.Middle#bar:()I"), methRef("test.Middle#foo:()I")),
      AddDependency(methRef("test.main.Main"), classRef("test.ParentInterface")),
      AddDependency(methRef("test.Parent#<init>:()V"), methRef("java.lang.Object#<init>:()V"))
    )
    assertEquals(obtainedEvents, expectedEvents)
  }

  testGraphOf(TestBuilds.testValueType, """test.Main.typeCheck(null)""") { obtainedEvents =>
    val expectedEvents = List(
      AddDependency(Reference.Method.NoMethod, methRef("test.main.Main")),
      AddDependency(
        methRef("test.main.Main"),
        methRef("test.Main$#typeCheck:(Ljava.lang.Object;)Z")
      ),
      AddDependency(
        methRef("test.Main$#typeCheck:(Ljava.lang.Object;)Z"),
        classRef("test.TheClass")
      ),
      AddDependency(
        methRef("test.Main$#typeCheck:(Ljava.lang.Object;)Z"),
        classRef("java.lang.Object")
      )
    )
    assertEquals(obtainedEvents, expectedEvents)
  }

  testBrokenGraphOf(TestBuilds.renameMethod, "test.Main$#proxy0:()I") { obtainedEvents =>
    val expectedEvents = List(
      AddDependency(Reference.Method.NoMethod, methRef("test.Main$#proxy0:()I")),
      AddDependency(methRef("test.Main$#proxy0:()I"), methRef("test.Library$#originalMethod:()I")),
      Missing(methRef("test.Library$#originalMethod:()I"))
    )
    assertEquals(obtainedEvents, expectedEvents)
  }

  private def testBrokenGraphOf(build: Build, entrypoint: String)(op: List[DotEvent] => Unit) =
    test(s"Graph of ${build.name}: `$entrypoint`") {
      val events = dotEvents(build.brokenMainClasspath, entrypoint)
      op(events)
    }

  private def testGraphOf(build: Build, code: String)(
      op: List[DotEvent] => Unit
  )(implicit loc: munit.Location): Unit =
    test(s"Graph of ${build.name}: `$code`") {
      val testProj = Project("test-project")
        .withSource(s"""|package test
                        |package main
                        |class Main {
                        |  def main(args: Array[String]): Unit = { $code }
                        |}""".stripMargin)
        .dependsOn(build)
      val testBuild = Build("test-build", testProj)
      val classpath = testBuild.classpath("test-project").full
      val events = dotEvents(classpath, "test.main.Main")
      op(events)
    }

  private def dotEvents(classpath: List[Path], entrypoint: String): List[DotEvent] = {
    val config =
      Config.empty.copy(classpath = classpath, entrypoints = List(methRef(entrypoint)))
    val buffer = new TestDotBuffer
    val ctx = Context.init(config).copy(dot = buffer)
    Linker.verify(ctx)
    buffer.events.toList
  }

  private class TestDotBuffer extends DotBuffer {
    val events: LinkedHashSet[DotEvent] = LinkedHashSet.empty
    override def addDependency(to: Reference)(implicit ctx: Context): Unit =
      events += AddDependency(ctx.currentMethod, to)
    override def markMissing(ref: Reference): Unit = events += Missing(ref)
    override def writeGraph(): Unit = ()
  }

  private sealed trait DotEvent
  private case class AddDependency(from: Reference, to: Reference) extends DotEvent
  private case class Missing(ref: Reference) extends DotEvent
}
