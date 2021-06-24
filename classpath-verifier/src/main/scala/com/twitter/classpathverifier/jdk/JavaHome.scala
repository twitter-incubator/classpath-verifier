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

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.Collections
import java.util.stream.{ Stream => JStream }

import scala.jdk.CollectionConverters._

object JavaHome {

  private val versionRegex = s"""JAVA_VERSION=["']?(.+?)["']?$$""".r

  def javahome(): Path = Paths.get(sys.props("java.home"))

  def javaVersionFromJavaHome(home: Path): Option[Int] = {
    val release = home.resolve("release")
    if (Files.isReadable(release)) javaVersionFromRelease(release)
    else None
  }

  def jreClasspathEntries(home: Path): List[Path] = {
    val jrtfs = home.resolve("lib").resolve("jrt-fs.jar")
    if (Files.isRegularFile(jrtfs)) classpathEntriesFromJrt(home)
    else classpathEntriesFromJars(home)
  }

  private def javaVersionFromRelease(release: Path): Option[Int] = {
    Files
      .readAllLines(release)
      .asScala
      .find(_.startsWith("JAVA_VERSION="))
      .flatMap(parseJavaVersion)
  }

  private def parseJavaVersion(version: String): Option[Int] =
    version match {
      case versionRegex(javaVersion) =>
        val versionStr =
          if (javaVersion.startsWith("1.")) javaVersion.substring(2, 3)
          else javaVersion.takeWhile(_ != '.')
        try Some(versionStr.toInt)
        catch { case _: NumberFormatException => None }
      case _ =>
        None
    }

  private def classpathEntriesFromJrt(home: Path): List[Path] = {
    val env = Collections.singletonMap("java.home", home.toString)
    val jrtfs = FileSystems.newFileSystem(URI.create("jrt:/"), env)
    val modulesRoot = jrtfs.getPath("modules")
    Files.list(modulesRoot).iterator.asScala.toList.filter(Files.isDirectory(_))
  }

  private def classpathEntriesFromJars(home: Path): List[Path] = {
    def isJar(file: Path, attrs: BasicFileAttributes): Boolean =
      attrs.isRegularFile() && file.getFileName.toString.endsWith(".jar")
    def jarsOf(path: Path): JStream[Path] =
      if (Files.isDirectory(path)) Files.find(path, Int.MaxValue, isJar)
      else JStream.empty()

    JStream
      .concat(
        jarsOf(home.resolve("lib")),
        jarsOf(home.resolve("jre").resolve("lib"))
      )
      .iterator
      .asScala
      .toList
  }
}
