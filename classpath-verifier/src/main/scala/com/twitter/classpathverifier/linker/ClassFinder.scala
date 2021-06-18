package com.twitter.classpathverifier.linker

import java.io.BufferedInputStream
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

trait Finder {
  def find(name: String): Option[InputStream]
  def allClasses(): List[String]
}

private object Finder {
  def nameToPath(name: String): String =
    Type.nameToPath(name) + ".class"
  def pathToName(name: String): String =
    Type.pathToName(name.stripSuffix(".class"))

  object Empty extends Finder {
    override def find(name: String): Option[InputStream] = None
    override def allClasses(): List[String] = Nil
  }
}

class ClassFinder(classpath: List[Path]) extends Finder {

  private val finders: LazyList[Finder] =
    LazyList.from(classpath).map(open)

  private def open(path: Path): Finder =
    if (Files.isRegularFile(path) && path.toString.endsWith(".jar"))
      new JarClassFinder(new JarFile(path.toFile))
    else if (Files.isDirectory(path))
      new DirectoryClassFinder(path)
    else
      Finder.Empty

  def find(name: String): Option[InputStream] =
    finders.map(_.find(name)).collectFirst {
      case Some(stream) =>
        stream
    }

  override def allClasses(): List[String] = finders.flatMap(_.allClasses()).toList
}

private class JarClassFinder(jar: JarFile) extends Finder {
  def find(name: String): Option[InputStream] = {
    val path = Finder.nameToPath(name)
    Option(jar.getJarEntry(path)).map(jar.getInputStream)
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
  def find(name: String): Option[InputStream] = {
    val path = directory.resolve(Finder.nameToPath(name))
    if (Files.isRegularFile(path))
      Some(new BufferedInputStream(Files.newInputStream(path)))
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
