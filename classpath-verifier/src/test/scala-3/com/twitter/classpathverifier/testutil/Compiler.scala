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
