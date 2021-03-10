package com.twitter.classpathverifier.descriptors

sealed trait Type
object Type {
  sealed trait Primitive extends Type
  object Primitive {
    case object Byte extends Primitive
    case object Char extends Primitive
    case object Double extends Primitive
    case object Float extends Primitive
    case object Int extends Primitive
    case object Long extends Primitive
    case object Short extends Primitive
    case object Boolean extends Primitive
    case object Void extends Primitive
  }

  case class Reference(className: String) extends Type
  case class Array(tpe: Type) extends Type

  def pathToName(path: String): String =
    if (path == null) null
    else path.replace('/', '.')

  def nameToPath(name: String): String =
    if (name == null) null
    else name.replace('.', '/')
}
