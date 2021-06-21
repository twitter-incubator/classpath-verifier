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

package com.twitter.classpathverifier.descriptors

sealed trait Type
object Type {
  sealed trait Primitive extends Type
  object Primitive {
    case object Byte extends Primitive
    case object Char extends Primitive
    case object Double extends Primitive
    case object Float extends Primitive
    case object Int extends Primitive
    case object Long extends Primitive
    case object Short extends Primitive
    case object Boolean extends Primitive
    case object Void extends Primitive
  }

  case class Reference(fullName: String) extends Type
  case class Array(tpe: Type) extends Type

  def pathToName(path: String): String =
    if (path == null) null
    else path.replace('/', '.')

  def nameToPath(name: String): String =
    if (name == null) null
    else name.replace('.', '/')
}
