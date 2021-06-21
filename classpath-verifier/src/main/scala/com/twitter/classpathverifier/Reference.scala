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
  def fullClassName: String
  def packageName: String
  def className: String
  def classRef: Reference.Clazz
  def show: String
}

object Reference {

  private def packageName(fullClassName: String): String =
    fullClassName.lastIndexOf('.') match {
      case -1  => ""
      case dot => fullClassName.substring(0, dot)
    }

  private def className(fullClassName: String): String =
    packageName(fullClassName) match {
      case ""  => fullClassName
      case pkg => fullClassName.substring(pkg.length + 1)
    }

  case class Clazz(packageName: String, className: String) extends Reference {
    override def fullClassName: String =
      if (packageName.isEmpty) className
      else s"$packageName.$className"
    def classRef: Clazz = this
    def show: String = fullClassName
  }

  object Clazz {
    def apply(fullName: String): Clazz = Clazz(packageName(fullName), className(fullName))
  }

  case class Method(
      fullClassName: String,
      methodName: String,
      descriptor: String
  ) extends Reference {
    def packageName: String = Reference.packageName(fullClassName)
    def className: String = Reference.className(fullClassName)
    def classRef: Reference.Clazz = Clazz(packageName, className)
    def show: String = s"$fullClassName#$methodName:$descriptor"
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
