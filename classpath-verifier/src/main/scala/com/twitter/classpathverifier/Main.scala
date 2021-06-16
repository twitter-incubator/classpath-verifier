package com.twitter.classpathverifier

import java.nio.file.Paths

import com.twitter.classpathverifier.diagnostics.LinkerError
import com.twitter.classpathverifier.linker.Context
import com.twitter.classpathverifier.linker.Linker
import scopt.OParser

object Main {
  private val parser = {
    val builder = OParser.builder[Context]
    def onConfig[T](op: (T, Config) => Config): (T, Context) => Context =
      (value, ctx) => ctx.copy(config = op(value, ctx.config))
    OParser.sequence(
      builder.programName("classpath-verifier"),
      builder.head("classpath-verifier", "0.1"),
      builder
        .opt[String]("classpath")
        .required()
        .unbounded()
        .action(onConfig((v, c) => c.copy(classpath = c.classpath ++ Config.toClasspath(v))))
        .text("The full classpath of the application"),
      builder
        .opt[Reference.Method]('e', "entry")
        .unbounded()
        .action(onConfig((v, c) => c.copy(entrypoints = v :: c.entrypoints)))
        .text("The entrypoints to link from"),
      builder
        .opt[String]('j', "jar")
        .unbounded()
        .action(onConfig((v, c) => c.addJarEntrypoints(Paths.get(v))))
        .text("JARs to fully check"),
      builder
        .opt[String]('m', "main")
        .unbounded()
        .action(onConfig((v, c) => c.addMains(Paths.get(v))))
        .text("JARs whose main methods to check"),
      builder
        .opt[Boolean]('p', "path")
        .action(onConfig((v, c) => c.copy(showPaths = v)))
        .text("Whether to show the path from entrypoint to missing symbol")
    )
  }

  private def process(implicit ctx: Context): Unit = {
    Linker.verify()
    if (ctx.reporter.hasErrors) {
      ctx.reporter.errors.foreach(reportError(_))
      System.exit(1)
    } else {
      System.err.println("Classpath is consistent.")
      System.exit(0)
    }
  }

  private def reportError(error: LinkerError)(implicit ctx: Context): Unit = {
    System.err.println(error.show)
    if (ctx.config.showPaths) {
      error.path.zipWithIndex.foreach {
        case (methodRef, depth) =>
          val prefix = " " * depth * 2
          System.err.println(prefix + methodRef.show)
      }
    }
  }

  def main(args: Array[String]): Unit =
    OParser
      .parse(parser, args, Context.init(Config.empty))
      .foreach(process(_))
}
