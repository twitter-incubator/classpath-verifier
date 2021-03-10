package com.twitter.classpathverifier.diagnostics

import java.util.concurrent.ConcurrentHashMap

import scala.collection.mutable.Buffer

import com.twitter.classpathverifier.Reference

abstract class Reporter {
  def report(error: LinkerError): Unit
  def errors: Seq[LinkerError]
  def hasErrors: Boolean
}

object Reporter {
  def newReporter: Reporter = new DeduplicatingReporter
  def noReporter: Reporter =
    new Reporter {
      override def report(error: LinkerError): Unit = ()
      override def errors: Seq[LinkerError] = Nil
      override def hasErrors: Boolean = false
    }
}

class DeduplicatingReporter extends Reporter {
  private val _errors: ConcurrentHashMap[Reference, LinkerError] = new ConcurrentHashMap

  override def report(err: LinkerError): Unit =
    _errors.computeIfAbsent(err.reference, _ => err)

  override def errors: Seq[LinkerError] = collect(_errors)
  override def hasErrors: Boolean = _errors.size() > 0

  private def collect[T](container: ConcurrentHashMap[_, T]): List[T] = {
    val buffer = Buffer.empty[T]
    val iterator = container.values.iterator
    while (iterator.hasNext()) buffer += iterator.next()
    buffer.toList
  }
}
