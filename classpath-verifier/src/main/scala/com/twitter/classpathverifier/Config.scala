package com.twitter.classpathverifier

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths

import com.twitter.classpathverifier.diagnostics.Reporter
import com.twitter.classpathverifier.linker.Context
import com.twitter.classpathverifier.linker.Summarizer

case class Config(
    classpath: List[Path],
    entrypoints: List[Reference.Method],
    showPaths: Boolean,
    reporter: Reporter
) {
  lazy val classloader: URLClassLoader = {
    val entries = classpath.map(_.toUri.toURL).toArray
    new URLClassLoader(entries)
  }

  def addJarEntrypoints(jar: Path): Config = {
    val ctx = Context.init(this)
    val jarEntrypoints = for {
      summary <- Summarizer.summarizeJar(jar)(ctx)
      entrypoint <- summary.methods
    } yield entrypoint.ref

    copy(entrypoints = entrypoints ::: jarEntrypoints)
  }
}

object Config {
  def empty: Config = Config(Nil, Nil, showPaths = true, Reporter.newReporter)

  def toClasspath(classpath: String): List[Path] =
    classpath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .toList
}
