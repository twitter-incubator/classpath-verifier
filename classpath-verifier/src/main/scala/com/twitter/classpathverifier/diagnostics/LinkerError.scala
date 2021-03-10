package com.twitter.classpathverifier.diagnostics

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.linker.Context

sealed trait LinkerError {
  def path: List[Reference.Method]
  def reference: Reference
  def show: String
}

private object LinkerError {
  def caller(path: List[Reference.Method]): String =
    path match {
      case Nil         => "<unknown>"
      case caller :: _ => caller.show
    }
}

case class MissingClassError(reference: Reference.Clazz, path: List[Reference.Method])
    extends LinkerError {
  override def show: String =
    s"class '${reference.show}' is missing (called from '${LinkerError.caller(path)}')"
}

object MissingClassError {
  def apply(reference: Reference.Clazz)(implicit ctx: Context): MissingClassError =
    MissingClassError(reference, ctx.path)
}

case class MissingMethodError(reference: Reference.Method, path: List[Reference.Method])
    extends LinkerError {
  override def show: String =
    s"method '${reference.show}' is missing (called from '${LinkerError.caller(path)}"
}

object MissingMethodError {
  def apply(reference: Reference.Method)(implicit ctx: Context): MissingMethodError =
    MissingMethodError(reference, ctx.path)
}
