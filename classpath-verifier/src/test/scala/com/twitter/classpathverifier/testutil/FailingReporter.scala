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

package com.twitter.classpathverifier.testutil

import com.twitter.classpathverifier.diagnostics.LinkerError
import com.twitter.classpathverifier.diagnostics.Reporter

class FailingReporter(fail: String => Unit) extends Reporter {
  override def report(error: LinkerError): Unit = fail(error.show)
  override def errors: Seq[LinkerError] = Nil
  override def hasErrors: Boolean = false
}
