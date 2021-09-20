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

package com.twitter.classpathverifier.config

import java.nio.file.Path
import java.nio.file.Paths

import com.twitter.classpathverifier.linker.BaseLinkerSuite
import com.twitter.classpathverifier.testutil.IOUtil
import com.twitter.classpathverifier.testutil.TestBuilds

class ConfigSuite extends BaseLinkerSuite {
  test("fullClasspath includes jars set in MANIFEST.MF") {
    IOUtil.withTempDirectory { temp =>
      val abstractMembersJar = temp.resolve("hello world/abstract members.jar")
      val overrideMembersJar = temp.resolve("override-members.jar")
      IOUtil.buildJarIn(abstractMembersJar, TestBuilds.abstractMembers, None, None)
      IOUtil.buildJarIn(
        overrideMembersJar,
        TestBuilds.overrideMembers,
        None,
        Some("hello world/abstract members.jar" :: Nil)
      )

      val config = emptyConfig(overrideMembersJar :: Nil)
      val obtainedFullClasspath = config.fullClasspath
      val expectedFullClasspath = List(overrideMembersJar, abstractMembersJar)
      assertEquals(obtainedFullClasspath, expectedFullClasspath)
    }
  }

  test("fullClasspath includes inexistent jars set in MANIFEST.MF") {
    IOUtil.withTempDirectory { temp =>
      val abstractMembersJar = temp.resolve("hello world/abstract members.jar")
      val overrideMembersJar = temp.resolve("override-members.jar")
      val doesntExistJar = temp.resolve("i don't exist.jar")
      IOUtil.buildJarIn(abstractMembersJar, TestBuilds.abstractMembers, None, None)
      IOUtil.buildJarIn(
        overrideMembersJar,
        TestBuilds.overrideMembers,
        None,
        Some("hello world/abstract members.jar" :: doesntExistJar.getFileName.toString :: Nil)
      )

      val config = emptyConfig(overrideMembersJar :: Nil)
      val obtainedFullClasspath = config.fullClasspath
      val expectedFullClasspath = List(overrideMembersJar, abstractMembersJar, doesntExistJar)
      assertEquals(obtainedFullClasspath, expectedFullClasspath)
    }
  }

  test("cyclic jars set in manifest") {
    IOUtil.withTempDirectory { temp =>
      val abstractMembersJar = temp.resolve("abstract members.jar")
      val overrideMembersJar = temp.resolve("override-members.jar")
      IOUtil.buildJarIn(
        abstractMembersJar,
        TestBuilds.abstractMembers,
        None,
        Some("override-members.jar" :: Nil)
      )
      IOUtil.buildJarIn(
        overrideMembersJar,
        TestBuilds.overrideMembers,
        None,
        Some("abstract members.jar" :: Nil)
      )

      val config = emptyConfig(overrideMembersJar :: Nil)
      val obtainedFullClasspath = config.fullClasspath
      val expectedFullClasspath = List(overrideMembersJar, abstractMembersJar)
      assertEquals(obtainedFullClasspath, expectedFullClasspath)
    }
  }

  test("transitive jars set in manifest") {
    IOUtil.withTempDirectory { temp =>
      val emptyBuildJar = temp.resolve("empty build.jar")
      val abstractMembersJar = temp.resolve("abstract members.jar")
      val overrideMembersJar = temp.resolve("override-members.jar")
      IOUtil.buildJarIn(emptyBuildJar, TestBuilds.empty, None, None)
      IOUtil.buildJarIn(
        abstractMembersJar,
        TestBuilds.abstractMembers,
        None,
        Some("empty build.jar" :: Nil)
      )
      IOUtil.buildJarIn(
        overrideMembersJar,
        TestBuilds.overrideMembers,
        None,
        Some("abstract members.jar" :: Nil)
      )

      val config = emptyConfig(overrideMembersJar :: Nil)
      val obtainedFullClasspath = config.fullClasspath
      val expectedFullClasspath = List(overrideMembersJar, abstractMembersJar, emptyBuildJar)
      assertEquals(obtainedFullClasspath, expectedFullClasspath)
    }
  }

  private def emptyConfig(classpath: List[Path]): Config =
    Config.empty.copy(javahome = Paths.get(""), classpath = classpath)

}
