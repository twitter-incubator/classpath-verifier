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

import java.nio.file.Files
import java.nio.file.Path

import com.twitter.classpathverifier.testutil.IOUtil

class JavaHomeSuite extends munit.FunSuite {

  private val testJavaVersions = List(
    "1.6.0" -> Some(6),
    "1.8.0" -> Some(8),
    "11.0.7" -> Some(11),
    "14.0.2" -> Some(14),
    "garbage" -> None
  )

  private val testJavaClassfileVersions = List(
    "1.6.0" -> 50,
    "1.8.0" -> 52,
    "11.0.7" -> 54,
    "14.0.2" -> 57,
    "garbage" -> 52
  )

  testJavaVersions.foreach {
    case (javaVersion, expectedJavaVersion) =>
      test(s"Reads java version '$javaVersion' as '$expectedJavaVersion'") {
        withFakeJavaHome(javaVersion) { javahome =>
          val obtainedJavaVersion = JavaHome.javaVersionFromJavaHome(javahome)
          assertEquals(obtainedJavaVersion, expectedJavaVersion)
        }
      }
  }

  testJavaClassfileVersions.foreach {
    case (javaVersion, expectedClassfileVersion) =>
      test(
        s"Expects classfile version '$expectedClassfileVersion' for java version '$javaVersion'"
      ) {
        withFakeJavaHome(javaVersion) { javahome =>
          ClassfileVersion.majorFromJavaHome(javahome)
        }
      }
  }

  private def withFakeJavaHome[T](javaVersion: String)(op: Path => T): T = {
    IOUtil.withTempDirectory { temp =>
      val release = temp.resolve("release")
      Files.write(release, s"""JAVA_VERSION="$javaVersion"""".getBytes)
      op(temp)
    }
  }
}
