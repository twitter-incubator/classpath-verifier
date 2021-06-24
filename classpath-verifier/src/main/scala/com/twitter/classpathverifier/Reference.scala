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

package com.twitter.classpathverifier

import com.twitter.classpathverifier.linker.Constants
import scopt.Read

sealed trait Reference {
  def className: String
  def classRef: Reference.Clazz
  def show: String
}

object Reference {

  case class Clazz(className: String) extends Reference {
    def classRef: Clazz = this
    def show: String = className
  }

  case class Method(
      className: String,
      methodName: String,
      descriptor: String
  ) extends Reference {
    def classRef: Reference.Clazz = Clazz(className)
    def show: String = s"$className#$methodName:$descriptor"
  }

  object Method {

    private val regex = "^([^#]*)(?:#([^:]+)(?::(.+))?)?$".r

    implicit val referenceReader: Read[Method] =
      Read.reads(apply(_) match {
        case Some(methodReference) =>
          methodReference
        case None =>
          throw new IllegalArgumentException("Not a valid method reference")
      })

    def apply(spec: String): Option[Method] =
      spec match {
        case regex(c, m, s) =>
          Some(
            Method(
              c,
              Option(m).getOrElse(Constants.MainMethodName),
              Option(s).getOrElse(Constants.MainMethodDescriptor)
            )
          )
        case _ =>
          None
      }

    val NoMethod: Method = Method("<no class>", "<no method>", "")
  }
}
