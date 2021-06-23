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

import com.twitter.classpathverifier.jdk.JavaHome
import com.twitter.classpathverifier.testutil.Build
import com.twitter.classpathverifier.testutil.Project
import com.twitter.classpathverifier.testutil.TestBuilds

class PathFinderSuite extends BaseLinkerSuite {

  checkPath(
    List(
      methDep("test.Private#foo:()V"),
      methDep("test.Private#bar:(I)V"),
      methDep("scala.Predef$#println:(Ljava.lang.Object;)V")
    ),
    TestBuilds.privateAccess
  )

  private def checkPath(
      expected: List[MethodDependency],
      build: Build
  )(implicit loc: munit.Location): Unit = {
    implicit val ctx: Context = failOnError
    val testProject = Project("test-project").dependsOn(build)
    val classpath = testProject.classpath(build).full
    val start = expected.head.ref
    val end = expected.last.ref
    test(s"path: ${start.show} -> ${end.show}") {
      val jreClasses = JavaHome.jreClasspathEntries(JavaHome.javahome())
      Finder(jreClasses ::: classpath).use { finder =>
        val summarizer = new ClassSummarizer(finder)
        val obtainedPath = PathFinder.path(summarizer, start, end)
        val expectedPath = if (expected.isEmpty) None else Some(expected.map(_.ref))
        assertEquals(obtainedPath, expectedPath)
      }
    }
  }
}
