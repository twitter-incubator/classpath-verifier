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

package com.twitter.classpathverifier.config

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.diagnostics.Reporter
import com.twitter.classpathverifier.jdk.ClassfileVersion
import com.twitter.classpathverifier.jdk.JavaHome
import com.twitter.classpathverifier.linker.Context
import com.twitter.classpathverifier.linker.Discovery

final case class Config(
    javahome: Path,
    classpath: List[Path],
    entrypoints: List[Reference.Method],
    showPaths: Boolean,
    reporter: Reporter,
    dotConfig: DotConfig
) {
  def addJarEntrypoints(jar: Path): Config =
    Discovery.allRefs(jar)(Context.init(this)) match {
      case Nil  => this
      case refs => copy(entrypoints = entrypoints ::: refs)
    }

  def addMains(jar: Path): Config =
    Discovery.allMains(jar)(Context.init(this)) match {
      case Nil  => this
      case refs => copy(entrypoints = entrypoints ::: refs)
    }

  def addMainFromManifest(jar: Path): Config = {
    if (!Files.isReadable(jar)) {
      throw new IllegalArgumentException(s"Cannot read: '$jar'")
    }

    Discovery.mainFromManifest(jar) match {
      case None      => this
      case Some(ref) => copy(entrypoints = entrypoints :+ ref)
    }
  }

  lazy val fullClasspath: List[Path] =
    JavaHome.jreClasspathEntries(javahome) ::: classpath

  lazy val javaVersion: Option[Int] = JavaHome.javaVersionFromJavaHome(javahome)

  lazy val javaMajorApiVersion: Int = ClassfileVersion.majorFromJavaHome(javahome)
}

object Config {
  def empty: Config =
    Config(
      javahome = JavaHome.javahome(),
      classpath = Nil,
      entrypoints = Nil,
      showPaths = true,
      reporter = Reporter.newReporter,
      dotConfig = DotConfig.empty
    )

  def toClasspath(classpath: String): List[Path] =
    classpath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .toList
}
