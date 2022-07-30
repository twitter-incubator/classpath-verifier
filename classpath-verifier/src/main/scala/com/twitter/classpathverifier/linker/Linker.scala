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

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.config.Config
import com.twitter.classpathverifier.diagnostics.LinkerError
import com.twitter.classpathverifier.diagnostics.MissingMethodError

object Linker {
  def verify(classpath: List[Path], entrypoints: List[Reference.Method]): Seq[LinkerError] = {
    val config = Config.empty.copy(classpath = classpath, entrypoints = entrypoints)
    val ctx = Context.init(config)
    verify(ctx)
    ctx.reporter.errors
  }

  def verify(ctx: Context): Unit = {
    val entrypoints = ctx.config.entrypoints
    Finder(ctx.config.fullClasspath).use { finder =>
      val summarizer = new ClassSummarizer(finder)
      val linker = new Linker(summarizer)
      ctx.config.entrypoints.foreach { entrypoint =>
        linker.verify(entrypoint, ctx)
      }
      ctx.dot.writeGraph()
    }
  }

}

private class Linker(summarizer: Summarizer) {
  private val toCheck = new ConcurrentLinkedQueue[(Dependency, Context)]()
  private val verified = new ConcurrentHashMap[Reference, Unit]()
  private val toCheckInSubclassesOf =
    new ConcurrentHashMap[String, ConcurrentLinkedQueue[(MethodDependency, Context)]]
  private val knownDirectSubclassesOf =
    new ConcurrentHashMap[String, ConcurrentHashMap[String, Unit]]

  def verify(entrypoint: Reference.Method, entryCtx: Context): Unit = {
    toCheck.add(MethodDependency.Static(entrypoint) -> entryCtx)
    val classRef = entrypoint.fullClassName
    val classSummary = summarizer(classRef)(entryCtx)
    registerClassDeps(classSummary)(entryCtx)
    while (!toCheck.isEmpty() || !toCheckInSubclassesOf.isEmpty()) {
      while (!toCheck.isEmpty()) {
        val (current, ctx) = toCheck.poll()
        verify(current)(ctx)
      }
      toCheckInSubclassesOf.forEach((clazz, refs) => {
        val subclasses = knownDirectSubclassesOf.getOrDefault(clazz, new ConcurrentHashMap).keys()
        refs.forEach {
          case (abstractDep, ctx) =>
            while (subclasses.hasMoreElements()) {
              val current = abstractDep.inClass(fullName = subclasses.nextElement())
              verify(current)(ctx)
            }
        }
      })
      toCheckInSubclassesOf.clear()
    }
  }

  private val ctorNames = Set(Constants.InitMethod, Constants.ClassInitMethod)
  private def registerClassDeps(summary: ClassSummary)(implicit ctx: Context): Unit = {
    // any time you use a class, you'd likely need to initialize it
    val ctors = summary.methods.filter { method => ctorNames(method.methodName) }
    ctors.foreach { m =>
      toCheck.add(MethodDependency.Static(m.ref) -> ctx)
    }
  }

  private def registerAsSubclass(summary: ClassSummary): Unit = {
    def register(parent: String, name: String): Unit = {
      val subclassesOfParent =
        knownDirectSubclassesOf.computeIfAbsent(parent, _ => new ConcurrentHashMap)
      subclassesOfParent.put(name, ())
    }
    summary.parent.foreach(register(_, summary.name))
    summary.interfaces.foreach(register(_, summary.name))
  }

  private def verify(dep: Dependency)(implicit ctx: Context): Unit =
    verified.computeIfAbsent(dep.ref, _ => doVerify(dep))

  private def doVerify(dep: Dependency)(implicit ctx: Context): Unit = {
    val classRef = dep.ref.classRef
    val classSummary = summarizer(classRef)
    registerClassDeps(classSummary)
    dep match {
      case dep: MethodDependency => verifyDep(classSummary, dep)
      case dep: ClassDependency =>
        ctx.dot.addDependency(dep.ref)
        ctx.used.addDependency(classSummary)
        classSummary.parent.foreach { parent =>
          val parentReference = Reference.Clazz(parent)
          val dependency = ClassDependency(parentReference)
          toCheck.add(dependency -> ctx)
        }
        classSummary.interfaces.foreach { interface =>
          val interfaceReference = Reference.Clazz(interface)
          val dependency = ClassDependency(interfaceReference)
          toCheck.add(dependency -> ctx)
        }
    }
  }

  private def verifyDep(
      classSummary: ClassSummary,
      dep: MethodDependency
  )(implicit ctx: Context): Unit = {
    ctx.dot.addDependency(dep.ref)
    ctx.used.addDependency(classSummary)
    classSummary.resolveDep(summarizer, dep) match {
      case None if classSummary.kind == ClassKind.Class =>
        ctx.dot.markMissing(dep.ref)
        ctx.reporter.report(MissingMethodError(dep.ref))
      case None =>
        val methodsToCheck =
          toCheckInSubclassesOf.computeIfAbsent(
            dep.ref.fullClassName,
            _ => new ConcurrentLinkedQueue
          )
        methodsToCheck.add(dep -> ctx)
      case Some(meth) =>
        meth.dependencies.foreach(dependency => toCheck.add(dependency -> ctx.in(meth.ref)))
    }
  }

}
