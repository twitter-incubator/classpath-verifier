package com.twitter.classpathverifier.testutil

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

case class Classpath(
    dependencies: List[Path],
    classesDir: Path
) {
  def full: List[Path] = dependencies :+ classesDir
}

object Classpath {
  lazy val bootClasspath: List[Path] =
    sys.props.get("java.home").map(Paths.get(_)) match {
      case None => Nil
      case Some(jre) =>
        val lib = jre.resolve("lib")
        val jars =
          List("charsets", "jce", "jsse", "jvmci-services", "management-agent", "resources", "rt")
        jars
          .map(x => lib.resolve(x + ".jar"))
          .filter(Files.exists(_))
    }

}
