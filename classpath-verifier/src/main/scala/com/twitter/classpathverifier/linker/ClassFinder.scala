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

import java.io.BufferedInputStream
import java.io.Closeable
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.JarFile

import scala.collection.compat.immutable.LazyList
import scala.collection.mutable.Buffer

import com.twitter.classpathverifier.descriptors.Type
import com.twitter.classpathverifier.io.Managed

trait Finder extends Closeable {
  def find(name: String): Option[Managed[InputStream]]
  def allClasses(): List[String]
}

object Finder {
  def nameToPath(name: String): String =
    Type.nameToPath(name) + ".class"
  def pathToName(name: String): String =
    Type.pathToName(name.stripSuffix(".class"))

  object Empty extends Finder {
    override def find(name: String): Option[Managed[InputStream]] = None
    override def allClasses(): List[String] = Nil
    override def close(): Unit = ()
  }

  def apply(classpath: List[Path]): Managed[Finder] =
    Managed(new ClassFinder(classpath))
}

private class ClassFinder(classpath: List[Path]) extends Finder {

  private val finders: LazyList[Finder] =
    LazyList.from(classpath).map(open)

  override def close(): Unit = finders.foreach(_.close())

  override def find(name: String): Option[Managed[InputStream]] =
    finders.map(_.find(name)).collectFirst {
      case Some(handle) =>
        handle
    }

  override def allClasses(): List[String] = finders.flatMap(_.allClasses()).toList

  private def open(path: Path): Finder =
    if (Files.isRegularFile(path) && path.toString.endsWith(".jar"))
      new JarClassFinder(new JarFile(path.toFile))
    else if (Files.isDirectory(path))
      new DirectoryClassFinder(path)
    else
      Finder.Empty

}

private class JarClassFinder(jar: JarFile) extends Finder {

  override def close(): Unit = jar.close()

  override def find(name: String): Option[Managed[InputStream]] = {
    val path = Finder.nameToPath(name)
    Option(jar.getJarEntry(path)).map(entry => Managed(jar.getInputStream(entry)))
  }

  override def allClasses(): List[String] = {
    val buffer = Buffer.empty[String]
    val entries = jar.entries()
    while (entries.hasMoreElements()) {
      val entry = entries.nextElement().toString
      if (entry.endsWith(".class")) {
        buffer += Finder.pathToName(entry)
      }
    }
    buffer.toList
  }
}

private class DirectoryClassFinder(directory: Path) extends Finder {

  override def close(): Unit = ()

  override def find(name: String): Option[Managed[InputStream]] = {
    val path = directory.resolve(Finder.nameToPath(name))
    if (Files.isRegularFile(path))
      Some(Managed(new BufferedInputStream(Files.newInputStream(path))))
    else
      None
  }

  override def allClasses(): List[String] = {
    val buffer = Buffer.empty[String]
    Files.walkFileTree(
      directory,
      new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          if (file.getFileName().toString.endsWith(".class")) {
            buffer += Finder.pathToName(directory.relativize(file).toString)
          }
          FileVisitResult.CONTINUE
        }
      }
    )
    buffer.toList
  }
}
