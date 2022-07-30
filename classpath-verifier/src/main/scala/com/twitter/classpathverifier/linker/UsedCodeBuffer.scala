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

package com.twitter.classpathverifier.linker

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.{ Set => JSet }

import scala.util.control.NonFatal

import com.twitter.classpathverifier.config.UsedCodeConfig

/**
 * Track used code, so we can report unused / dead code.
 */
trait UsedCodeBuffer {
  def addDependency(to: ClassSummary)(implicit ctx: Context): Unit
  def unusedJars(implicit ctx: Context): List[Path]
}

object NoUsedCodeBuffer extends UsedCodeBuffer {
  override def addDependency(to: ClassSummary)(implicit ctx: Context): Unit = ()
  override def unusedJars(implicit ctx: Context): List[Path] = Nil
}

object UsedCodeBuffer {
  def apply(config: UsedCodeConfig): UsedCodeBuffer = {
    if (config.enabled) new DefaultUseCodeBuffer(config)
    else NoUsedCodeBuffer
  }
}

private class DefaultUseCodeBuffer(config: UsedCodeConfig) extends UsedCodeBuffer {
  private val usedJars: JSet[Path] = ConcurrentHashMap.newKeySet[Path]
  override def addDependency(to: ClassSummary)(implicit ctx: Context): Unit = {
    to.path match {
      case None        => ()
      case Some(value) => usedJars.add(value)
    }
  }

  private def rawUnused(implicit ctx: Context): Set[Path] = {
    import scala.jdk.CollectionConverters._
    val classpath = ctx.config.fullClasspath.map(_.toAbsolutePath()).toSet
    val usedJarsAbs = usedJars.asScala.toSet[Path].map(_.toAbsolutePath())
    (classpath -- usedJarsAbs)
  }

  private def filterOutPatterns(value: Set[Path], excludePatterns: List[String]): Set[Path] = {
    val fs = FileSystems.getDefault()
    val excludeMatchers = excludePatterns.map { g =>
      fs.getPathMatcher(
        if (g.startsWith("glob:") || g.startsWith("regex:")) g
        else s"glob:$g"
      )
    }
    value.filterNot(p => excludeMatchers.exists(_.matches(p)))
  }

  private def unusedJarsSet(implicit ctx: Context): Set[Path] = {
    val cwd = Paths.get(".").toAbsolutePath()
    val unused1 = rawUnused.filter(_.toString().endsWith(".jar"))
    val unused2 = unused1
      .map { (p) =>
        try {
          cwd.relativize(p)
        } catch {
          case NonFatal(_) => p
        }
      }
    val defaultExcludes = List(
      "**/scala-library*.jar",
      "**/scala3-library*.jar",
      "**/jre/lib/*.jar",
      "**/jre/lib/ext/*.jar",
      "**/jre/lib/security/**/*.jar",
    )
    filterOutPatterns(unused2, config.excludePatterns ++ defaultExcludes)
  }

  override def unusedJars(implicit ctx: Context): List[Path] =
    unusedJarsSet.toList.sortBy(_.toString())
}
