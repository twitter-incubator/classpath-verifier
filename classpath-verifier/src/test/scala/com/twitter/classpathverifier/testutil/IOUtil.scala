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

package com.twitter.classpathverifier.testutil

import java.io.IOException
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

object IOUtil {

  def withTempFile[T](suffix: String)(op: Path => T): T = {
    val temp = Files.createTempFile(null, suffix)
    try op(temp)
    finally Files.delete(temp)
  }

  def withTempDirectory[T](op: Path => T): T = {
    val temp = Files.createTempDirectory(null)
    try op(temp)
    finally deleteRecursively(temp)
  }

  def buildJarIn(
      jar: Path,
      build: Build,
      mainClass: Option[String],
      classpath: Option[List[String]]
  ): Unit = {
    val manifest = new Manifest()
    val attributes = manifest.getMainAttributes()
    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    mainClass.foreach(attributes.put(Attributes.Name.MAIN_CLASS, _))
    classpath.foreach { cp =>
      val rootPath = jar.getParent()
      val root = rootPath.toUri
      val urls = cp.map(entry => root.relativize(rootPath.resolve(entry).toUri))
      attributes.put(Attributes.Name.CLASS_PATH, urls.mkString(" "))
    }
    Files.createDirectories(jar.getParent())
    val stream =
      Files.newOutputStream(
        jar,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
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
  }

  def withJar[T](build: Build, mainClass: Option[String], classpath: Option[List[String]])(
      op: Path => T
  ): T =
    IOUtil.withTempFile(suffix = ".jar") { jar =>
      buildJarIn(jar, build, mainClass, classpath)
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

  private def deleteRecursively(directory: Path): Unit =
    Files.walkFileTree(
      directory,
      new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, ex: IOException): FileVisitResult = {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      }
    )

}
