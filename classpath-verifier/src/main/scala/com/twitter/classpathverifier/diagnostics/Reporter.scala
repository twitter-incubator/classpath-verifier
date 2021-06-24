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
