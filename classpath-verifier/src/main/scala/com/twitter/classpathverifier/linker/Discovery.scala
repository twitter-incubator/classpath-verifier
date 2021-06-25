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

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarInputStream

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.io.Managed

object Discovery {

  def mainFromManifest(jar: Path): Option[Reference.Method] =
    Managed(new JarInputStream(Files.newInputStream(jar))).use { stream =>
      for {
        manifest <- Option(stream.getManifest())
        attributes = manifest.getMainAttributes()
        main <- Option(attributes.getValue(Attributes.Name.MAIN_CLASS))
      } yield Reference.Method(main, Constants.MainMethodName, Constants.MainMethodDescriptor)
    }

  def allMains(jar: Path)(implicit ctx: Context): List[Reference.Method] =
    for {
      method <- allMethods(jar)
      if isMain(method)
    } yield method.ref

  def allRefs(jar: Path)(implicit ctx: Context): List[Reference.Method] =
    for {
      method <- allMethods(jar)
    } yield method.ref

  private def isMain(summary: MethodSummary): Boolean =
    summary.methodName == Constants.MainMethodName &&
      summary.descriptor == Constants.MainMethodDescriptor &&
      summary.isStatic

  private def allMethods(jar: Path)(implicit ctx: Context): List[MethodSummary] =
    for {
      summary <- Summarizer.summarizeJar(jar)(ctx)
      method <- summary.methods
    } yield method
}
