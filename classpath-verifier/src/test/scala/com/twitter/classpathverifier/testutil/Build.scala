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

final case class Build(
    name: String,
    projects: Map[String, Project]
) {
  def rootPath: Path =
    BuildInfo.target.toPath
      .resolve("test-builds-cache")
      .resolve(BuildInfo.scalaVersion)
      .resolve(##.toHexString)
  def project(name: String): Project = projects(name)
  def classpath(name: String): Classpath = project(name).classpath(this)
  def classpaths(): Map[String, Classpath] =
    projects.map { case (k, v) => k -> v.classpath(this) }
  def allClasspath: Classpath =
    Build("synthetic", Project("synthetic").dependsOn(this)).classpath("synthetic")
}

object Build {
  def apply(name: String, projects: Project*): Build =
    new Build(name, projects.map(p => p.name -> p).toMap)
}
