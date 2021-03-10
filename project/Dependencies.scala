import sbt._
import sbt.Keys._

object Dependencies {

  val scopt = "com.github.scopt" %% "scopt" % "4.0.1"
  val asm = "org.ow2.asm" % "asm" % "9.0"
  val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4"
  val coursier = ("io.get-coursier" %% "coursier" % "2.0.16").cross(CrossVersion.for3Use2_13)
  val munit = "org.scalameta" %% "munit" % "0.7.26"
  val scalaCompiler = Def.setting {
    if (scalaBinaryVersion.value == "3") {
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
    } else {
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    }
  }
}
