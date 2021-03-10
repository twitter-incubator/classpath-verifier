package com.twitter.classpathverifier.linker

import com.twitter.classpathverifier.testutil.Build
import com.twitter.classpathverifier.testutil.Resolution
import com.twitter.classpathverifier.testutil.TestBuilds

class ClassFinderSuite extends BaseLinkerSuite {
  val classesToFind: List[String] = List(
    classOf[List[_]],
    classOf[Map[_, _]]
  ).map(_.getName)

  val finder = new ClassFinder(Resolution.scalaLibrary)

  classesToFind.foreach { name =>
    test(s"find $name") {
      assert(finder.find(name).isDefined, s"Not found: $name")
    }
  }

  test("test.Valid") {
    val finder = new ClassFinder(TestBuilds.valid.classpath("root").full)
    assert(finder.find("test.Valid").isDefined, s"Not found: test.Valid")
  }

  listAll(
    TestBuilds.inherited,
    "root",
    List(
      "test.ParentInterface",
      "test.MiddleInterface",
      "test.ChildInterface",
      "test.Parent",
      "test.Middle",
      "test.Child",
      "test.InheritedMembers"
    )
  )

  private def listAll(build: Build, projectName: String, expectedClasses: List[String])(implicit
      loc: munit.Location
  ): Unit =
    test(s"List all: ${build.name}[$projectName]") {
      val finder = new ClassFinder(build.classpath(projectName).classesDir :: Nil)
      val obtainedClasses = finder.allClasses()
      assertEquals(obtainedClasses.length, expectedClasses.length)
      assertEquals(obtainedClasses.sorted, expectedClasses.sorted)
    }

}
