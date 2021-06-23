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

package com.twitter.classpathverifier.io

import scala.collection.mutable.Buffer

class ManagedSuite extends munit.FunSuite {

  managedTest("managed is not opened unless used", Nil) { rec =>
    Managed(rec("hello world"))(_ => ())
  }

  managedTest("managed is closed after use", List("close")) { rec =>
    Managed(())(_ => rec("close")).use(_ => ())
  }

  managedTest(
    "managed is closed after exception",
    List("open", "throw", "close", "caught: oh no")
  ) { rec =>
    try {
      Managed(rec("open"))(_ => rec("close")).use { _ =>
        rec("throw")
        throw new Exception("oh no")
      }
    } catch { case ex: Exception => rec("caught: " + ex.getMessage()) }
  }

  managedTest(
    "managed.map",
    List(1, 2, 3, 4, 5)
  ) { rec =>
    Managed(rec(1))(_ => rec(5)).map(_ => rec(2)).map(_ => rec(3)).use { _ => rec(4) }
  }

  managedTest(
    "managed.map doesn't evaluate",
    List()
  ) { rec =>
    Managed(rec("open"))(_ => rec("close")).map(_ => rec("map"))
  }

  managedTest(
    "managed.map throws exception",
    List("open", "throw", "close", "caught: oh no")
  ) { rec =>
    try {
      Managed(rec("open"))(_ => rec("close"))
        .map { _ =>
          rec("throw")
          throw new Exception("oh no")
        }
        .use { (_: Nothing) => rec("unreachable") }
    } catch { case ex: Exception => rec("caught: " + ex.getMessage) }
  }

  managedTest(
    "managed.flatMap",
    List(1, 2, 3, 4, 5)
  ) { rec =>
    Managed(rec(1))(_ => rec(5)).flatMap(_ => Managed(rec(2))(_ => rec(4))).use(_ => rec(3))
  }

  managedTest(
    "managed.flatMap throws exception",
    List("open", "throw", "close", "caught: oh no")
  ) { rec =>
    try {
      Managed(rec("open"))(_ => rec("close"))
        .flatMap { _ =>
          rec("throw")
          throw new Exception("oh no")
        }
        .use { (_: Nothing) => rec("unreachable") }
    } catch { case ex: Exception => rec("caught: " + ex.getMessage) }
  }

  private def managedTest(name: munit.TestOptions, expectedEvents: List[Any])(
      body: (Any => Unit) => Unit
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      val buffer = Buffer.empty[Any]
      body(ev => buffer += ev)
      assertEquals(buffer.toList, expectedEvents)
    }
  }

}
