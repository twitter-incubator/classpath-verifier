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

import com.twitter.classpathverifier.Config
import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.testutil.FailingReporter

abstract class BaseLinkerSuite extends munit.FunSuite {
  def methDep(r: String): MethodDependency.Static =
    MethodDependency.Static(Reference.Method(r).getOrElse(fail(s"Cannot parse: '$r'")))
  def classDep(name: String): ClassDependency = ClassDependency(classRef(name))
  def classRef(name: String): Reference.Clazz =
    Reference.Clazz(name)

  val failOnError: Context =
    Context.init(Config.empty.copy(reporter = new FailingReporter((msg: String) => fail(msg))))
}
