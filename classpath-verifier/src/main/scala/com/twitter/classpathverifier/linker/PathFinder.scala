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

object PathFinder {
  def path(
      summarize: Summarizer,
      from: Reference.Method,
      to: Reference.Method
  )(implicit ctx: Context): Option[List[Reference.Method]] = {
    val finder = new PathFinder(summarize)
    finder.path(to)(ctx.in(from))
  }
}

private class PathFinder(summarize: Summarizer) {

  private val visited = collection.mutable.HashSet.empty[Reference]

  private def path(to: Reference.Method)(implicit ctx: Context): Option[List[Reference.Method]] = {
    if (visited.add(ctx.currentMethod)) {
      if (ctx.currentMethod == to) Some(to :: Nil)
      else {
        val deps = dependencies(ctx.currentMethod)
        deps.foldLeft(Option.empty[List[Reference.Method]]) {
          case (None, d) => path(to)(ctx.in(d))
          case (p, _)    => p
        } match {
          case None    => None
          case Some(p) => Some(ctx.currentMethod :: p)
        }
      }
    } else {
      None
    }
  }

  private def dependencies(ref: Reference.Method)(implicit ctx: Context): List[Reference.Method] = {
    summarize(ref.className)
      .resolveDep(summarize, MethodDependency.Static(ref))
      .getOrElse(MethodSummary.Empty)
      .dependencies
      .map(_.ref)
      .collect { case ref: Reference.Method => ref }
  }

}
