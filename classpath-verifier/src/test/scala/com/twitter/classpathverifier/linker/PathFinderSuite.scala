package com.twitter.classpathverifier.linker

import com.twitter.classpathverifier.testutil.Build
import com.twitter.classpathverifier.testutil.Classpath
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
      val summarizer = new ClassSummarizer(Classpath.bootClasspath ::: classpath)
      val obtainedPath = PathFinder.path(summarizer, start, end)
      val expectedPath = if (expected.isEmpty) None else Some(expected.map(_.ref))
      assertEquals(obtainedPath, expectedPath)
    }
  }
}
