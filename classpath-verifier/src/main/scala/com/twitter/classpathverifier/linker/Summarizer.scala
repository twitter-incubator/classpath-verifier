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

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.diagnostics.MissingClassError

trait Summarizer {
  def apply(clazz: Reference.Clazz)(implicit ctx: Context): ClassSummary =
    apply(clazz.fullClassName)
  def apply(clazz: String)(implicit ctx: Context): ClassSummary
}

object Summarizer {
  def summarizeJar(jar: Path)(implicit ctx: Context): List[ClassSummary] = {
    val finder = new ClassFinder(jar :: Nil)
    val summarizer = new ClassSummarizer(finder)
    finder.allClasses().map(summarizer(_))
  }
}

class ClassSummarizer(finder: Finder) extends Summarizer {

  def this(classpath: List[Path]) = this(new ClassFinder(classpath))

  private val summaries = new ConcurrentHashMap[String, ClassSummary]()

  override def apply(fullClassName: String)(implicit ctx: Context): ClassSummary =
    summaries.computeIfAbsent(fullClassName, summarize) match {
      case ClassSummary.Missing =>
        val missingRef = Reference.Clazz(fullClassName)
        val error = MissingClassError(missingRef)
        ctx.reporter.report(error)
        ctx.dot.addDependency(missingRef)
        ctx.dot.markMissing(missingRef)
        ClassSummary.Missing
      case summary =>
        summary
    }

  private def summarize(fullClassName: String): ClassSummary =
    finder.find(fullClassName) match {
      case None         => ClassSummary.Missing
      case Some(stream) => ClassSummary.collect(stream)
    }
}
