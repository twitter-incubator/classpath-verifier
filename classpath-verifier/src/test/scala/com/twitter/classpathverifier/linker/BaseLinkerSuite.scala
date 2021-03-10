package com.twitter.classpathverifier.linker

import com.twitter.classpathverifier.Config
import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.testutil.FailingReporter

abstract class BaseLinkerSuite extends munit.FunSuite {
  def methDep(r: String): MethodDependency.Static =
    MethodDependency.Static(Reference.Method(r).getOrElse(fail(s"Cannot parse: '$r'")))
  def classDep(name: String): ClassDependency = ClassDependency(classRef(name))
  def classRef(name: String): Reference.Clazz =
    Reference.Clazz(name)

  val failOnError: Context =
    Context.init(Config.empty.copy(reporter = new FailingReporter((msg: String) => fail(msg))))
}
