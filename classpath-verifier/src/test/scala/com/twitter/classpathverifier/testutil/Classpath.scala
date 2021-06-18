package com.twitter.classpathverifier.testutil

import java.nio.file.Path

case class Classpath(
    dependencies: List[Path],
    classesDir: Path
) {
  def full: List[Path] = dependencies :+ classesDir
}
