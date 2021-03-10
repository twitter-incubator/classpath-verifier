package com.twitter.classpathverifier.linker

import java.nio.file.Path

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.testutil.TestBuilds
import org.objectweb.asm.MethodVisitor

class MethodFinderSuite extends BaseLinkerSuite {
  val classpath: List[Path] = TestBuilds.valid.classpath("root").full
  val finder = new MethodFinder(classpath)

  def findMethod(ref: String): Unit =
    test(s"Method: $ref") {
      assert(
        find(ref).isDefined,
        s"Couldn't find: '$ref' in classpath: ${classpath.mkString(", ")}"
      )
    }

  def find(entryStr: String): Option[MethodVisitor] =
    Reference.Method(entryStr) match {
      case None =>
        fail(s"Couldn't parse: $entryStr")
      case Some(entrypoint) =>
        finder.find(entrypoint)
    }

  findMethod("test.ValidObject")
  findMethod("test.ValidObject#main:([Ljava.lang.String;)V")
  findMethod("cats.data.Const#retag:()Lcats.data.Const;")

}
