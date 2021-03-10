package com.twitter.classpathverifier.linker

import com.twitter.classpathverifier.Reference

sealed trait Dependency {
  def ref: Reference
}

case class ClassDependency(ref: Reference.Clazz) extends Dependency

sealed trait MethodDependency extends Dependency {
  type Dep <: Dependency
  def ref: Reference.Method
  def inClass(className: String): Dep
}

object MethodDependency {

  case class Static(ref: Reference.Method) extends MethodDependency {
    type Dep = Static
    def dynamic: Dynamic = Dynamic(ref)
    override def inClass(className: String): Dep = copy(ref = ref.copy(className = className))
  }
  case class Dynamic(ref: Reference.Method) extends MethodDependency {
    type Dep = Dynamic
    def static: Static = Static(ref)
    override def inClass(className: String): Dep = copy(ref = ref.copy(className = className))
  }
}
