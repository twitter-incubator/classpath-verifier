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
