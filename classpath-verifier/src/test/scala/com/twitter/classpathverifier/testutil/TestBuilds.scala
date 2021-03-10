package com.twitter.classpathverifier.testutil

import com.twitter.classpathverifier.BuildInfo

object TestBuilds {

  val catsCore: String = s"org.typelevel:cats-core_${BuildInfo.scalaBinaryVersion}:2.6.1"

  val empty: Build = Build("empty")

  val arrayClone: Build = Build(
    "array-clone",
    Project("root")
      .withSource("""|package test
           |class ArrayClone {
           |  def main(args: Array[String]): Unit = args.clone()
           |}""".stripMargin)
  )

  val plain: Build = Build(
    "plain",
    Project("root")
      .withSource("""|package test
           |object Plain {
           |  def foo(x: Int, y: String): Unit = {
           |    val xStr = x.toString
           |    val concat = xStr + y
           |    System.out.println(concat)
           |  }
           |}""".stripMargin)
  )

  val valid: Build = Build(
    "valid",
    Project("root")
      .withSource(
        """|package test
           |class Valid {
           |  def multiply(x: Int, y: Int): Int = {
           |    val helper = new Helper(x, y)
           |    helper.multiply
           |  }
           |  def foo(x: Int): Unit = {
           |    val bar = multiply(x, 5)
           |    System.out.println(bar)
           |  }
           |}""".stripMargin
      )
      .withSource(
        """|package test
           |import scala.util.Try
           |import cats.syntax.try_._
           |object ValidObject {
           |  def main(args: Array[String]): Unit = {
           |    val valid = new Valid
           |    valid.foo(4)
           |    val validated = Try(10 / args.length).toValidated
           |    println(validated)
           |  }
           |}""".stripMargin
      )
      .withSource(
        """|package test
           |class Helper(x: Int, y: Int) {
           |  def multiply: Int = x * y
           |}""".stripMargin
      )
      .withLibrary(catsCore)
  )

  val privateAccess: Build = Build(
    "private",
    Project("root")
      .withSource(
        """|package test
           |object Proxy {
           |  def proxy = new Private().foo()
           |}
           |private class Private {
           |  def foo(): Unit = bar(0)
           |  private def bar(x: Int): Unit = println(x)
           |}""".stripMargin
      )
  )

  val inherited: Build = Build(
    "inherited-members",
    Project("root")
      .withSource(
        """|package test
           |trait ParentInterface { def foo(): Int }
           |trait MiddleInterface { def bar(): Int }
           |trait ChildInterface  { def baz(): Int }
           |
           |class Parent extends ParentInterface {
           |  def foo(): Int = 0
           |}
           |
           |class Middle extends Parent with MiddleInterface {
           |  def bar(): Int = foo()
           |}
           |
           |class Child extends Middle with ChildInterface {
           |  def baz(): Int = bar() + foo()
           |}""".stripMargin
      )
      .withSource(
        """|package test
         |class InheritedMembers {
         |  def main(args: Array[String]): Unit = {
         |    val child = new Child()
         |    child.foo()
         |    child.bar()
         |    child.baz()
         |    child.toString()
         |  }
         |}""".stripMargin
      )
  )

  val abstractMembers: Build = Build(
    "abstract-members",
    Project("root")
      .withSource(
        """|package test
           |trait MyTrait { def foo(o: MyTrait): Int }
           |abstract class MyAbstractClass extends MyTrait
           |class MyConcreteClass extends MyAbstractClass {
           |  override def foo(o: MyTrait): Int = 0
           |}""".stripMargin
      )
  )

  val lambda: Build = Build(
    "lambda",
    Project("root")
      .withSource(
        """|package test
           |import cats.data.Validated
           |class Lambda {
           |  def main(args: Array[String]): Unit = {
           |    val fn = (x: String) => Validated.Valid(x)
           |    applyToFoo(fn)
           |  }
           |  def applyToFoo[T](op: String => T): T = op("foo")
           |}""".stripMargin
      )
      .withLibrary(catsCore)
  )

  val overrideMembers: Build = Build(
    "override-members",
    Project("root")
      .withSource(
        """|package test
           |trait MyBaseTrait { def foo(x: Object): Object }""".stripMargin
      )
      .withSource(
        """|package test
           |class MyTraitImpl extends MyBaseTrait {
           |  override def foo(x: Object): MyBaseTrait = null
           |}""".stripMargin
      )
      .withSource(
        """|package test
           |class Override {
           |  def main(args: Array[String]): Unit = {
           |    val trt: MyBaseTrait = null
           |    trt.foo(trt)
           |    val impl: MyTraitImpl = null
           |    impl.foo(trt)
           |  }
           |}""".stripMargin
      )
  )

  val renameMethod: Build = Build(
    "rename-method",
    Project("v1")
      .withSource("""|package test
                       |object Library {
                       |  def originalMethod: Int = 0
                       |}""".stripMargin),
    Project("v2")
      .withSource("""|package test
                       |object Library {
                       |  def renamedMethod: Int = 0
                       |}""".stripMargin),
    Project("main")
      .withSource("""|package test
                       |object Main {
                       |  def main(args: Array[String]): Unit = proxy1
                       |  def proxy1 = proxy0
                       |  def proxy0 = Library.originalMethod
                       |}""".stripMargin)
      .dependsOn("v1")
  )

  val missingInterface: Build = Build(
    "missing-interface",
    Project("v1")
      .withSource("""|package test
                     |trait InterfaceTransitive0
                     |trait InterfaceTransitive extends InterfaceTransitive0
                     |trait Interface
                     |""".stripMargin),
    Project("v2")
      .withSource("""|package test
                     |trait InterfaceTransitive extends InterfaceTransitive0""".stripMargin)
      .dependsOn("v1"),
    Project("main")
      .withSource("""|package test
                     |class ClazzTransitive extends InterfaceTransitive
                     |class Clazz extends Interface""".stripMargin)
      .withSource("""|package test
                     |object Main {
                     |  def transitive: Unit = { new ClazzTransitive; () }
                     |  def direct: Unit = { new Clazz; () }
                     |}""".stripMargin)
      .dependsOn("v1")
  )

  val missingParentClass: Build = Build(
    "missing-parent",
    Project("v1")
      .withSource("""|package test
                     |abstract class TransitiveParent0
                     |abstract class TransitiveParent extends TransitiveParent0
                     |abstract class Parent""".stripMargin),
    Project("v2")
      .withSource("""|package test
                     |abstract class TransitiveParent extends TransitiveParent0""".stripMargin)
      .dependsOn("v1"),
    Project("main")
      .withSource("""|package test
                     |class ClazzTransitive extends TransitiveParent
                     |class Clazz extends Parent""".stripMargin)
      .withSource("""|package test
                     |object Main {
                     |  def transitive: Unit = { new ClazzTransitive; () }
                     |  def direct: Unit = { new Clazz; () }
                     |}""".stripMargin)
      .dependsOn("v1")
  )

  val fastpass: Build = Build(
    "fastpass",
    Project("fastpass")
      .withLibrary("org.scalameta:fastpass_2.12:1.8.0")
  )

  val scalac: Build = Build(
    "scalac",
    if (BuildInfo.scalaBinaryVersion == "3") {
      Project("dotc")
        .withLibrary(s"org.scala-lang:scala3-compiler_3:${BuildInfo.scalaVersion}")
    } else {
      Project("scalac")
        .withLibrary(s"org.scala-lang:scala-compiler:${BuildInfo.scalaVersion}")
    }
  )

  val testValueType: Build = Build(
    "test-value-type",
    Project("v1")
      .withSource("""|package test
                       |class TheClass
                       |""".stripMargin),
    Project("v2"),
    Project("main")
      .withSource("""|package test
                       |import test.TheClass
                       |object Main {
                       |  def typeCheck(c: Object): Boolean = c.isInstanceOf[TheClass]
                       |}""".stripMargin)
      .dependsOn("v1")
  )

  val castValueType: Build = Build(
    "cast-value-type",
    Project("v1")
      .withSource("""|package test
                       |class CastClass
                       |""".stripMargin),
    Project("v2"),
    Project("main")
      .withSource("""|package test
                       |import test.CastClass
                       |object Main {
                       |  def typeCast(c: Object): Unit = { c.asInstanceOf[CastClass]; () }
                       |}""".stripMargin)
      .dependsOn("v1")
  )
}
