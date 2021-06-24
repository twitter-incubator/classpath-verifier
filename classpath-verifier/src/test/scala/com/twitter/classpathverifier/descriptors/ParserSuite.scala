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

class ParserSuite extends munit.FunSuite {

  testParser("(I)V", Descriptor(List(Type.Primitive.Int), Type.Primitive.Void))
  testParser(
    "([[II)Z",
    Descriptor(
      List(Type.Array(Type.Array(Type.Primitive.Int)), Type.Primitive.Int),
      Type.Primitive.Boolean
    )
  )
  testParser(
    "([Ljava.lang.String;[[Ljava.lang.String;)Ljava.lang.String;",
    Descriptor(
      List(
        Type.Array(Type.Reference("java.lang.String")),
        Type.Array(Type.Array(Type.Reference("java.lang.String")))
      ),
      Type.Reference("java.lang.String")
    )
  )

  private def testParser(descriptor: String, expected: Descriptor)(implicit
      loc: munit.Location
  ): Unit = {
    test(s"Parse $descriptor") {
      assertEquals(Parser.parse(descriptor), expected)
    }
  }

}
