package com.twitter.classpathverifier.testutil

import java.nio.file.Path

import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.io.VirtualFile
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global

/**
 * Helper object to compile code snippets to a virtual directory.
 */
object Compiler {

  case class CompilationFailed(msg: String) extends Exception(msg)

  /**
   * Compiles the given code, passing the given options to the compiler.
   */
  def compile(
      projectName: String,
      sources: List[String],
      outputDir: Path,
      options: Array[String]
  ): Unit = {
    val command = new CompilerCommand(options.toList, reportError _)
    command.settings.outputDirs.setSingleOutput(outputDir.toAbsolutePath.toString)

    val global = new Global(command.settings)
    val sourceFiles = sources.zipWithIndex.map {
      case (code, idx) =>
        val file = new VirtualFile(s"src$idx.scala", s"$projectName/src$idx.scala")
        new BatchSourceFile(file, code)
    }
    val run = new global.Run
    run.compileSources(sourceFiles)
    if (global.reporter.hasErrors) {
      reportError("Errors found.")
    }
  }

  private def reportError(error: String) = throw CompilationFailed(error)

}
