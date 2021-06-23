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
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.{ Stream => JStream }

import scala.jdk.CollectionConverters._

import com.twitter.classpathverifier.io.Managed

object JavaHome {

  private val versionRegex = s"""JAVA_VERSION=["']?(.+?)["']?$$""".r
  private val jreClasspathCache = new ConcurrentHashMap[Path, List[Path]]()

  def javahome(): Path = Paths.get(sys.props("java.home"))

  def javaVersionFromJavaHome(home: Path): Option[Int] = {
    val release = home.resolve("release")
    if (Files.isReadable(release)) javaVersionFromRelease(release)
    else None
  }

  def jreClasspathEntries(home: Path): List[Path] =
    jreClasspathCache.computeIfAbsent(
      home,
      _ => {
        val jrtfs = home.resolve("lib").resolve("jrt-fs.jar")
        if (Files.isRegularFile(jrtfs)) classpathEntriesFromJrt(home)
        else classpathEntriesFromJars(home)
      }
    )

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

  /*
   * This code is known to leak some memory (it's not about closing the classloader and
   * filesystem), see:
   *   - https://bugs.openjdk.java.net/browse/JDK-8260621
   *   - https://stackoverflow.com/a/68083960/2466911 for a workaround
   *
   * This is fixed in JDK 17. We mitigate the issue by caching the classpath entries for each
   * java home. This shouldn't become an issue unless we load thousands of JRT images in a single
   * run. Then, a potential solution is to copy all the classfiles from the jrtfs to a known
   * location, and then use these classfiles.
   */
  private def classpathEntriesFromJrt(home: Path): List[Path] = {
    val env = Collections.singletonMap("java.home", home.toString)
    val jrtfsJar = Array(home.resolve("lib").resolve("jrt-fs.jar").toUri.toURL)
    val classloader = new URLClassLoader(jrtfsJar)
    val jrtfs = FileSystems.newFileSystem(URI.create("jrt:/"), env, classloader)
    val modulesRoot = jrtfs.getPath("modules")
    Managed(Files.list(modulesRoot)).use(_.iterator.asScala.toList.filter(Files.isDirectory(_)))
  }

  private def classpathEntriesFromJars(home: Path): List[Path] = {
    def isJar(file: Path, attrs: BasicFileAttributes): Boolean =
      attrs.isRegularFile() && file.getFileName.toString.endsWith(".jar")
    def jarsOf(path: Path): Managed[JStream[Path]] =
      Managed {
        if (Files.isDirectory(path)) Files.find(path, Int.MaxValue, isJar)
        else JStream.empty()
      }

    val entries =
      for {
        lib <- jarsOf(home.resolve("lib"))
        jreLib <- jarsOf(home.resolve("jre").resolve("lib"))
      } yield JStream.concat(lib, jreLib)

    entries.use(_.iterator.asScala.toList)
  }
}
