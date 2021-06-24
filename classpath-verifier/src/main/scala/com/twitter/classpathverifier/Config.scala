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

package com.twitter.classpathverifier

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

import com.twitter.classpathverifier.diagnostics.Reporter
import com.twitter.classpathverifier.jdk.JavaHome
import com.twitter.classpathverifier.linker.Constants
import com.twitter.classpathverifier.linker.Context
import com.twitter.classpathverifier.linker.MethodSummary
import com.twitter.classpathverifier.linker.Summarizer

case class Config(
    javahome: Path,
    classpath: List[Path],
    entrypoints: List[Reference.Method],
    showPaths: Boolean,
    reporter: Reporter
) {
  def addJarEntrypoints(jar: Path): Config = {
    val jarEntrypoints = for {
      method <- allMethods(jar)
    } yield method.ref

    copy(entrypoints = entrypoints ::: jarEntrypoints)
  }

  def addMains(jar: Path): Config = {
    def isMain(method: MethodSummary): Boolean =
      method.methodName == Constants.MainMethodName && method.descriptor == Constants.MainMethodDescriptor
    val mains = for {
      method <- allMethods(jar)
      if isMain(method)
    } yield method.ref

    copy(entrypoints = entrypoints ::: mains)
  }

  lazy val fullClasspath: List[Path] =
    JavaHome.jreClasspathEntries(javahome) ::: classpath

  private def allMethods(jar: Path): List[MethodSummary] = {
    val ctx = Context.init(this)
    for {
      summary <- Summarizer.summarizeJar(jar)(ctx)
      method <- summary.methods
    } yield method
  }
}

object Config {
  def empty: Config = Config(JavaHome.javahome(), Nil, Nil, showPaths = true, Reporter.newReporter)

  def toClasspath(classpath: String): List[Path] =
    classpath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .toList
}
