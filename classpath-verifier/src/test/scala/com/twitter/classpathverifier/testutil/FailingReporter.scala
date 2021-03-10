package com.twitter.classpathverifier.testutil

import com.twitter.classpathverifier.diagnostics.LinkerError
import com.twitter.classpathverifier.diagnostics.Reporter

class FailingReporter(fail: String => Unit) extends Reporter {
  override def report(error: LinkerError): Unit = fail(error.show)
  override def errors: Seq[LinkerError] = Nil
  override def hasErrors: Boolean = false
}
