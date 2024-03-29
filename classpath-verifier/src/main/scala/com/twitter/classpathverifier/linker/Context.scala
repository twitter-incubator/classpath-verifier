/*
 * Copyright 2021 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.classpathverifier.linker

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.config.Config
import com.twitter.classpathverifier.diagnostics.Reporter
import com.twitter.classpathverifier.dot.DotBuffer
import com.twitter.classpathverifier.dot.NoDotBuffer

case class Context(
    parent: Context,
    config: Config,
    currentMethod: Reference.Method,
    dot: DotBuffer,
    used: UsedCodeBuffer,
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
  object RootContext
      extends Context(null, Config.empty, Reference.Method.NoMethod, NoDotBuffer, NoUsedCodeBuffer)
  def init(config: Config): Context =
    Context(
      RootContext,
      config,
      Reference.Method.NoMethod,
      DotBuffer(config.dotConfig),
      UsedCodeBuffer(config.usedCodeConfig)
    )
}
