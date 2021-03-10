package com.twitter.classpathverifier.linker

sealed trait ClassKind
object ClassKind {
  case object Class extends ClassKind
  case object Interface extends ClassKind
  case object AbstractClass extends ClassKind
}
