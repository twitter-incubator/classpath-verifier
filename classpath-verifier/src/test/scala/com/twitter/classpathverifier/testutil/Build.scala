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
