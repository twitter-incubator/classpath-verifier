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
  def apply(clazz: Reference.Clazz)(implicit ctx: Context): ClassSummary
  def apply(clazz: String)(implicit ctx: Context): ClassSummary =
    apply(Reference.Clazz(clazz))
}

object Summarizer {
  def summarizeJar(jar: Path)(implicit ctx: Context): List[ClassSummary] = {
    Finder(jar :: Nil).use { finder =>
      val summarizer = new ClassSummarizer(finder)
      finder.allClasses().map(summarizer(_))
    }
  }
}

class ClassSummarizer(finder: Finder) extends Summarizer {

  private val summaries = new ConcurrentHashMap[Reference.Clazz, ClassSummary]()

  override def apply(clazz: Reference.Clazz)(implicit ctx: Context): ClassSummary =
    summaries.computeIfAbsent(clazz, summarize) match {
      case ClassSummary.Missing =>
        val error = MissingClassError(clazz)
        ctx.reporter.report(error)
        ctx.dot.addDependency(clazz)
        ctx.dot.markMissing(clazz)
        ClassSummary.Missing
      case summary =>
        summary
    }

  private def summarize(clazz: Reference.Clazz)(implicit ctx: Context): ClassSummary =
    finder.find(clazz.fullClassName) match {
      case None        => ClassSummary.Missing
      case Some(entry) => entry.stream.use(ClassSummary.collect(clazz, entry.path, _))
    }
}
