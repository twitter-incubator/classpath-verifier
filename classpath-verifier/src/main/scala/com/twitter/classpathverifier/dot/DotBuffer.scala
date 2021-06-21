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

package com.twitter.classpathverifier.dot

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.{ Set => JSet }

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.config.DotConfig
import com.twitter.classpathverifier.config.DotConfig.Granularity
import com.twitter.classpathverifier.linker.Context

trait DotBuffer {
  def addDependency(to: Reference)(implicit ctx: Context): Unit
  def markMissing(ref: Reference): Unit
  def writeGraph(): Unit
}

object DotBuffer {
  def apply(config: DotConfig): DotBuffer =
    if (config.output.isDefined) new DefaultDotBuffer(config)
    else NoDotBuffer

  class Buffer[T] {
    private val items = new ConcurrentHashMap[T, JSet[T]]
    def add(key: T, value: T): Unit = {
      if (key != value) {
        val values = items.computeIfAbsent(key, _ => ConcurrentHashMap.newKeySet[T]())
        values.add(value)
      }
    }
    def foreach(op: (T, T) => Unit): Unit =
      items.forEach { (key, values) =>
        values.forEach { value => op(key, value) }
      }
  }
}

object NoDotBuffer extends DotBuffer {
  override def addDependency(to: Reference)(implicit ctx: Context): Unit = ()
  override def markMissing(ref: Reference): Unit = ()
  override def writeGraph(): Unit = ()
}

private class DefaultDotBuffer(config: DotConfig) extends DotBuffer {
  private val granularity = config.granularity
  private val dependencies = new DotBuffer.Buffer[String]
  private val missingRefs: JSet[String] = ConcurrentHashMap.newKeySet[String]
  private val includedPackages: JSet[String] = ConcurrentHashMap.newKeySet[String]
  private val excludedPackages: JSet[String] = ConcurrentHashMap.newKeySet[String]

  override def addDependency(to: Reference)(implicit ctx: Context): Unit = {
    val currentPackage = ctx.currentMethod.packageName
    val included = isIncluded(ctx.currentMethod) && isIncluded(to)

    if (included) {
      granularity match {
        case Granularity.Package =>
          dependencies.add(currentPackage, to.packageName)
        case Granularity.Class =>
          dependencies.add(ctx.currentMethod.fullClassName, to.fullClassName)
        case Granularity.Method =>
          dependencies.add(ctx.currentMethod.show, to.show)
      }
    }
  }
  override def markMissing(ref: Reference): Unit = {
    if (isIncluded(ref)) {
      val key = granularity match {
        case Granularity.Package => ref.packageName
        case Granularity.Class   => ref.fullClassName
        case Granularity.Method  => ref.show
      }
      missingRefs.add(key)
    }
  }
  override def writeGraph(): Unit =
    config.output.foreach { output =>
      val buffer = new StringBuilder()
      buffer.append("digraph {\n")
      missingRefs.forEach { ref =>
        buffer.append(s"""\t"$ref" [fillcolor=red style=filled];\n""")
      }
      dependencies.foreach { (from, to) =>
        buffer.append(s"""\t"$from" -> "$to";\n""")
      }
      buffer.append("}\n")
      Files.write(output, buffer.toString.getBytes)
    }

  private def isIncluded(ref: Reference): Boolean = {
    val currentPackage = ref.packageName
    if (config.packageFilter.isEmpty) true
    else if (includedPackages.contains(currentPackage)) true
    else if (excludedPackages.contains(currentPackage)) false
    else if (
      config.packageFilter.contains(currentPackage) || config.packageFilter
        .exists(x => currentPackage.startsWith(x + "."))
    ) {
      includedPackages.add(currentPackage)
      true
    } else {
      excludedPackages.add(currentPackage)
      false
    }
  }

}
