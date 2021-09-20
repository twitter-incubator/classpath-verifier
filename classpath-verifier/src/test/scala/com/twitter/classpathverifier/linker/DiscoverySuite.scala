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

import com.twitter.classpathverifier.testutil.IOUtil
import com.twitter.classpathverifier.testutil.TestBuilds

class DiscoverySuite extends BaseLinkerSuite {

  test("Can find main from Manifest") {
    IOUtil.withJar(TestBuilds.valid, Some("test.ValidObject"), None) { jar =>
      val expectedMain = Some(methRef("test.ValidObject"))
      val obtainedMain = Discovery.mainFromManifest(jar)
      assertEquals(obtainedMain, expectedMain)
    }
  }

  test("Can find all mains from jar") {
    IOUtil.withJar(TestBuilds.valid, None, None) { jar =>
      implicit val ctx = failOnError
      val expectedMains = methRef("test.ValidObject") :: Nil
      val obtainedMains = Discovery.allMains(jar)
      assertEquals(obtainedMains, expectedMains)
    }
  }

  test("Ignores non-static mains") {
    IOUtil.withJar(TestBuilds.arrayClone, None, None) { jar =>
      implicit val ctx = failOnError
      val expectedMains = Nil
      val obtainedMains = Discovery.allMains(jar)
      assertEquals(obtainedMains, expectedMains)
    }
  }

  test("Can read all references from jar") {
    IOUtil.withJar(TestBuilds.abstractMembers, None, None) { jar =>
      implicit val ctx = failOnError
      val expectedReferences = List(
        methRef("test.MyTrait#foo:(Ltest.MyTrait;)I"),
        methRef("test.MyConcreteClass#foo:(Ltest.MyTrait;)I"),
        methRef("test.MyConcreteClass#<init>:()V"),
        methRef("test.MyAbstractClass#<init>:()V")
      ).sortBy(_.show)
      val obtainedReferences = Discovery.allRefs(jar).sortBy(_.show)
      assertEquals(obtainedReferences, expectedReferences)
    }
  }

  test("Can read classpath from jar") {
    IOUtil.withTempDirectory { temp =>
      val abstractMembersJar = temp.resolve("abstract-members.jar")
      val overrideMembersJar = temp.resolve("override-members.jar")
      IOUtil.buildJarIn(abstractMembersJar, TestBuilds.abstractMembers, None, None)
      IOUtil.buildJarIn(
        overrideMembersJar,
        TestBuilds.overrideMembers,
        None,
        Some("abstract-members.jar" :: Nil)
      )

      val obtainedEntries = Discovery.classpathEntriesFromManifest(overrideMembersJar)
      val expectedEntries = abstractMembersJar.toAbsolutePath() :: Nil
      assertEquals(obtainedEntries, expectedEntries)
    }
  }

  test("Can read classpath from jar with space") {
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

      val obtainedEntries = Discovery.classpathEntriesFromManifest(overrideMembersJar)
      val expectedEntries = abstractMembersJar.toAbsolutePath() :: Nil
      assertEquals(obtainedEntries, expectedEntries)
    }
  }
}
