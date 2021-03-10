package com.twitter.classpathverifier.linker

import com.twitter.classpathverifier.Config
import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.diagnostics.Reporter

case class Context(
    parent: Context,
    config: Config,
    currentMethod: Reference.Method
) {
  def in(ref: Reference.Method): Context =
    copy(parent = this, currentMethod = ref)

  def path: List[Reference.Method] =
    parent match {
      case Context.RootContext => Nil
      case other               => currentMethod :: other.path
    }

  def reporter: Reporter = config.reporter
}

object Context {

  object RootContext extends Context(null, Config.empty, Reference.Method.NoMethod)
  def init(config: Config): Context = Context(RootContext, config, Reference.Method.NoMethod)
}
