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
