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

package com.twitter.classpathverifier.linker

import com.twitter.classpathverifier.Reference

sealed trait Dependency {
  def ref: Reference
}

case class ClassDependency(ref: Reference.Clazz) extends Dependency

sealed trait MethodDependency extends Dependency {
  type Dep <: Dependency
  def ref: Reference.Method
  def inClass(fullName: String): Dep
}

object MethodDependency {

  case class Static(ref: Reference.Method) extends MethodDependency {
    type Dep = Static
    def dynamic: Dynamic = Dynamic(ref)
    override def inClass(fullName: String): Dep = copy(ref = ref.copy(fullClassName = fullName))
  }
  case class Dynamic(ref: Reference.Method) extends MethodDependency {
    type Dep = Dynamic
    def static: Static = Static(ref)
    override def inClass(fullName: String): Dep = copy(ref = ref.copy(fullClassName = fullName))
  }
}
