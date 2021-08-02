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

package com.twitter.classpathverifier.jdk

import java.nio.file.Path

object ClassfileVersion {

  def majorFromJavaVersion(version: Int, default: => Int = majorFromJavaVersion(8, 8)): Int =
    if (version < 1 || version > 17) default
    else version + 44

  def majorFromJavaHome(home: Path): Int =
    JavaHome.javaVersionFromJavaHome(home) match {
      case None          => majorFromJavaVersion(8)
      case Some(version) => majorFromJavaVersion(version)
    }
}
