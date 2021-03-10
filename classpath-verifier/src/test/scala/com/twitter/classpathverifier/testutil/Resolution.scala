package com.twitter.classpathverifier.testutil

import java.nio.file.Path

import com.twitter.classpathverifier.BuildInfo
import coursier.Fetch
import coursier.parse.DependencyParser

object Resolution {

  def jarsOf(deps: String*): List[Path] = {
    val coursierDeps = dependencies(deps)
    val files = Fetch().addDependencies(coursierDeps: _*).run()
    files.map(_.toPath).toList
  }

  lazy val scalaLibrary: List[Path] =
    if (BuildInfo.scalaBinaryVersion == "3")
      Resolution.jarsOf(s"org.scala-lang:scala3-library_3:${BuildInfo.scalaVersion}")
    else Resolution.jarsOf(s"org.scala-lang:scala-library:${BuildInfo.scalaVersion}")

  private def dependencies(deps: Seq[String]): Seq[coursier.Dependency] = {
    DependencyParser
      .dependencies(deps, scala.util.Properties.versionNumberString)
      .either match {
      case Left(err)           => sys.error(err.mkString(System.lineSeparator))
      case Right(dependencies) => dependencies
    }
  }
}
