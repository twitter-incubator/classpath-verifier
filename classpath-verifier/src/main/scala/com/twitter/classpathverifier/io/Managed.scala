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

package com.twitter.classpathverifier.io

trait Managed[T] { self =>
  def use[U](op: T => U): U

  def map[Z](fn: T => Z): Managed[Z] =
    new Managed[Z] {
      override def use[U](op: Z => U): U = self.use(t => op(fn(t)))
    }

  def flatMap[Z](fn: T => Managed[Z]): Managed[Z] =
    new Managed[Z] {
      override def use[U](op: Z => U): U = self.use(t => fn(t).use(op))
    }

  def foreach(op: T => Unit): Unit = use(op)
}

object Managed {

  private def create[T](materialize: => T, close: T => Unit): Managed[T] =
    new Managed[T] {
      override def use[U](op: T => U): U = {
        val resource = materialize
        try op(resource)
        finally if (resource != null) close(resource)
      }
    }

  def apply[T](materialize: => T)(close: T => Unit): Managed[T] =
    create(materialize, close)

  def apply[T <: AutoCloseable](materialize: => T): Managed[T] =
    create(materialize, _.close())

  def wrap[T](value: T): Managed[T] = apply(value)(_ => ())

}
