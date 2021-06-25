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

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

import com.twitter.classpathverifier.io.Managed
import com.twitter.classpathverifier.testutil.Build
import com.twitter.classpathverifier.testutil.IOUtil
import com.twitter.classpathverifier.testutil.TestBuilds

class DiscoverySuite extends BaseLinkerSuite {

  test("Can find main from Manifest") {
    withJar(TestBuilds.valid, Some("test.ValidObject")) { jar =>
      val expectedMain = Some(methRef("test.ValidObject"))
      val obtainedMain = Discovery.mainFromManifest(jar)
      assertEquals(obtainedMain, expectedMain)
    }
  }

  test("Can find all mains from jar") {
    withJar(TestBuilds.valid, None) { jar =>
      implicit val ctx = failOnError
      val expectedMains = methRef("test.ValidObject") :: Nil
      val obtainedMains = Discovery.allMains(jar)
      assertEquals(obtainedMains, expectedMains)
    }
  }

  test("Ignores non-static mains") {
    withJar(TestBuilds.arrayClone, None) { jar =>
      implicit val ctx = failOnError
      val expectedMains = Nil
      val obtainedMains = Discovery.allMains(jar)
      assertEquals(obtainedMains, expectedMains)
    }
  }

  test("Can read all references from jar") {
    withJar(TestBuilds.abstractMembers, None) { jar =>
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

  private def withJar[T](build: Build, mainClass: Option[String])(op: Path => T): T =
    IOUtil.withTempFile(suffix = ".jar") { jar =>
      val manifest = new Manifest()
      val attributes = manifest.getMainAttributes()
      attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
      mainClass.foreach(attributes.put(Attributes.Name.MAIN_CLASS, _))
      val stream =
        Files.newOutputStream(jar, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
      Managed(
        new JarOutputStream(
          Files.newOutputStream(
            jar,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
          ),
          manifest
        )
      ).use { jos =>
        build.allClasspath.full.foreach(addClassesToJar(_, jos))
      }

      op(jar)
    }

  private def addClassesToJar(classesDir: Path, jos: JarOutputStream): Unit = {
    Files.walkFileTree(
      classesDir,
      new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          if (file.getFileName.toString.endsWith(".class")) writeFile(classesDir, file, jos)
          FileVisitResult.CONTINUE
        }
      }
    )
  }

  private def writeFile(classesDir: Path, file: Path, jos: JarOutputStream): Unit = {
    val entry = new JarEntry(classesDir.relativize(file).toString)
    jos.putNextEntry(entry)
    Files.copy(file, jos)
    jos.closeEntry()
  }
}
