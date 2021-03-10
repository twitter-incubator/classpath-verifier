package com.twitter.classpathverifier.linker

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.diagnostics.MissingClassError

trait Summarizer {
  def apply(clazz: Reference.Clazz)(implicit ctx: Context): ClassSummary =
    apply(clazz.className)
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

  override def apply(className: String)(implicit ctx: Context): ClassSummary =
    summaries.computeIfAbsent(className, summarize) match {
      case ClassSummary.Missing =>
        val error = MissingClassError(Reference.Clazz(className))
        ctx.reporter.report(error)
        ClassSummary.Missing
      case summary =>
        summary
    }

  private def summarize(className: String): ClassSummary =
    finder.find(className) match {
      case None         => ClassSummary.Missing
      case Some(stream) => ClassSummary.collect(stream)
    }
}
