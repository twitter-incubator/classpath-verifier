package com.twitter.classpathverifier.testutil

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

import scala.util.control.NonFatal

case class Project(
    name: String,
    sources: List[String] = Nil,
    dependencies: List[String] = Nil,
    libraries: List[String] = Nil,
    additionalDependencies: List[Path] = Resolution.scalaLibrary,
) {
  def root(build: Build): Path = build.rootPath.resolve(name)
  def classpath(build: Build): Classpath = {
    val librariesClasspath = Resolution.jarsOf(libraries: _*)
    val dependenciesClasspath = dependencies.flatMap(build.classpath(_).full)
    val classesDir = root(build).resolve("classes")
    val classpath =
      Classpath(librariesClasspath ++ dependenciesClasspath ++ additionalDependencies, classesDir)
    doBuild(classpath)
    classpath
  }

  def withSource(src: String): Project = copy(sources = src :: sources)
  def dependsOn(name: String): Project = copy(dependencies = name :: dependencies)
  def dependsOn(build: Build): Project = build.projects.keys.foldLeft(this)(_.dependsOn(build, _))
  def dependsOn(build: Build, project: String): Project = {
    val classpath = build.classpath(project)
    withAdditionalDependencies(classpath.full)
  }
  def withLibrary(module: String): Project = copy(libraries = module :: libraries)
  def withAdditionalDependency(dep: Path): Project = withAdditionalDependencies(dep :: Nil)
  def withAdditionalDependencies(deps: List[Path]): Project =
    copy(additionalDependencies = deps ++ additionalDependencies)

  private def doBuild(classpath: Classpath): Unit =
    synchronized {
      if (!Files.isDirectory(classpath.classesDir)) {
        Files.createDirectories(classpath.classesDir)
        val options = Array("-cp", classpath.dependencies.distinct.mkString(File.pathSeparator))
        try Compiler.compile(name, sources, classpath.classesDir, options)
        catch { case NonFatal(e) => Files.delete(classpath.classesDir); throw e }
      }
    }
}
