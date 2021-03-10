package com.twitter.classpathverifier

import com.twitter.classpathverifier.linker.Constants
import scopt.Read

sealed trait Reference {
  def className: String
  def classRef: Reference.Clazz
  def show: String
}

object Reference {

  case class Clazz(className: String) extends Reference {
    def classRef: Clazz = this
    def show: String = className
  }

  case class Method(
      className: String,
      methodName: String,
      descriptor: String
  ) extends Reference {
    def classRef: Reference.Clazz = Clazz(className)
    def show: String = s"$className#$methodName:$descriptor"
  }

  object Method {

    private val regex = "^([^#]*)(?:#([^:]+)(?::(.+))?)?$".r

    implicit val referenceReader: Read[Method] =
      Read.reads(apply(_) match {
        case Some(methodReference) =>
          methodReference
        case None =>
          throw new IllegalArgumentException("Not a valid method reference")
      })

    def apply(spec: String): Option[Method] =
      spec match {
        case regex(c, m, s) =>
          Some(
            Method(
              c,
              Option(m).getOrElse(Constants.MainMethodName),
              Option(s).getOrElse(Constants.MainMethodDescriptor)
            )
          )
        case _ =>
          None
      }

    val NoMethod: Method = Method("<no class>", "<no method>", "")
  }
}
