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

package com.twitter.classpathverifier.testutil

import java.nio.file.Path

import dotty.tools.dotc.ast.Positioned
import dotty.tools.dotc.config.Feature
import dotty.tools.dotc.core.Comments
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.MacroClassLoader
import dotty.tools.dotc.Driver
import dotty.tools.io.AbstractFile
import dotty.tools.io.VirtualFile
import dotty.tools.io.PlainFile.toPlainFile

/**
 * Helper object to compile code snippets to a virtual directory.
 */
object Compiler extends Driver {

  case class CompilationFailed(msg: String) extends Exception(msg)

  override protected def sourcesRequired: Boolean = false

  /**
   * Compiles the given code, passing the given options to the compiler.
   */
  def compile(
      projectName: String,
      sources: List[String],
      outputDir: Path,
      options: Array[String]
  ): Unit = {
    implicit val ctx = initCtx.fresh
    val summary = command.distill(options, ctx.settings)(ctx.settingsState)
    val outputDirectory = AbstractFile.getDirectory(outputDir.toAbsolutePath)
    val newSettings = ctx.settings.outputDir.updateIn(summary.sstate, outputDirectory)
    ctx.setSettings(newSettings)

    Feature.checkExperimentalSettings
    MacroClassLoader.init(ctx)
    Positioned.init

    ctx.setProperty(Comments.ContextDoc, new Comments.ContextDocstrings)

    newCompiler.newRun.compileFromStrings(sources)

    if (ctx.reporter.hasErrors) reportError("Errors found.")
  }

  private def reportError(error: String) = throw CompilationFailed(error)

}
