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

package com.twitter.classpathverifier.descriptors

import java.nio.file.Path

import com.twitter.classpathverifier.jdk.JavaHome
import com.twitter.classpathverifier.linker.BaseLinkerSuite
import com.twitter.classpathverifier.linker.ClassSummarizer
import com.twitter.classpathverifier.linker.Context
import com.twitter.classpathverifier.linker.Summarizer
import com.twitter.classpathverifier.testutil.Build
import com.twitter.classpathverifier.testutil.TestBuilds

class DescriptorSuite extends BaseLinkerSuite {

  testCompatible("(I)V", "(I)V")
  testCompatible("(Ljava.lang.Object;)V", "(Ljava.util.Map;)V")
  testCompatible("([Ljava.lang.Object;)[Ljava.util.Map;", "([Ljava.util.Map;)[Ljava.lang.Object;")
  testCompatible(
    "(Ltest.ParentInterface;Ltest.MiddleInterface;Ltest.ChildInterface;)Ltest.Child;",
    "(Ltest.Child;Ltest.Child;Ltest.Child;)Ltest.Parent;",
    TestBuilds.inherited
  )
  testIncompatible(
    "(Ltest.ParentInterface;Ltest.MiddleInterface;Ltest.ChildInterface;)Ltest.ChildInterface;",
    "(Ltest.Child;Ltest.Child;Ltest.Child;)Ltest.Child;",
    TestBuilds.inherited
  )

  private implicit val ctx: Context = failOnError

  private def testCompatible(
      declaration: String,
      callSite: String,
      build: Build = TestBuilds.empty
  )(implicit
      loc: munit.Location
  ): Unit =
    test(s"$callSite can be passed to $declaration") {
      val classpath = build.allClasspath.full
      val jreClasses = JavaHome.jreClasspathEntries(JavaHome.javahome())
      withDescriptors(declaration, callSite, jreClasses ::: classpath) {
        (declarationDesc, callSiteDesc, summarizer) =>
          assert(
            callSiteDesc.compatibleWith(summarizer, declarationDesc),
            s"`$callSite` cannot be passed to `$declaration`"
          )
      }
    }

  private def testIncompatible(
      declaration: String,
      callSite: String,
      build: Build = TestBuilds.empty
  )(implicit
      loc: munit.Location
  ): Unit =
    test(s"$callSite cannot be passed to $declaration") {
      val classpath = build.allClasspath.full
      val jreClasses = JavaHome.jreClasspathEntries(JavaHome.javahome())
      withDescriptors(declaration, callSite, jreClasses ::: classpath) {
        (declarationDesc, callSiteDesc, summarize) =>
          assert(
            !callSiteDesc.compatibleWith(summarize, declarationDesc),
            s"`$callSite` can be passed to `$declaration`"
          )
      }
    }

  private def withDescriptors(a: String, b: String, classpath: List[Path])(
      op: (Descriptor, Descriptor, Summarizer) => Unit
  ): Unit = {
    val summarize = new ClassSummarizer(classpath)
    val descA = Parser.parse(a)
    val descB = Parser.parse(b)
    op(descA, descB, summarize)
  }
}
