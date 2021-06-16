package com.twitter.classpathverifier

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths

import com.twitter.classpathverifier.diagnostics.Reporter
import com.twitter.classpathverifier.linker.Constants
import com.twitter.classpathverifier.linker.Context
import com.twitter.classpathverifier.linker.MethodSummary
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

  private def allMethods(jar: Path): List[MethodSummary] = {
    val ctx = Context.init(this)
    for {
      summary <- Summarizer.summarizeJar(jar)(ctx)
      method <- summary.methods
    } yield method
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
