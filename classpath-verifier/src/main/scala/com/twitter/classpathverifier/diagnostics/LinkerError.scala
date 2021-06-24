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

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.linker.Context

sealed trait LinkerError {
  def path: List[Reference.Method]
  def reference: Reference
  def show: String
}

private object LinkerError {
  def caller(path: List[Reference.Method]): String =
    path match {
      case Nil         => "<unknown>"
      case caller :: _ => caller.show
    }
}

case class MissingClassError(reference: Reference.Clazz, path: List[Reference.Method])
    extends LinkerError {
  override def show: String =
    s"class '${reference.show}' is missing (called from '${LinkerError.caller(path)}')"
}

object MissingClassError {
  def apply(reference: Reference.Clazz)(implicit ctx: Context): MissingClassError =
    MissingClassError(reference, ctx.path)
}

case class MissingMethodError(reference: Reference.Method, path: List[Reference.Method])
    extends LinkerError {
  override def show: String =
    s"method '${reference.show}' is missing (called from '${LinkerError.caller(path)}"
}

object MissingMethodError {
  def apply(reference: Reference.Method)(implicit ctx: Context): MissingMethodError =
    MissingMethodError(reference, ctx.path)
}
