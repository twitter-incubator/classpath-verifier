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

import scala.collection.mutable.Buffer

object Parser {
  def parse(descriptor: String): Descriptor = {
    val parser = new Parser(descriptor)
    parser.eat('(')
    val args = Buffer.empty[Type]
    while (parser.current() != ')') {
      args += parser.parseType()
    }
    parser.eat(')')
    val result = parser.parseType()
    Descriptor(args.toList, result)
  }
}

private class Parser(in: String) {
  private var _current: Int = 0
  def current(): Char = in.charAt(_current)

  def eat(c: Char): Unit = {
    assert(next() == c)
  }

  def parseType(): Type =
    (next(): @annotation.switch) match {
      case 'B' => Type.Primitive.Byte
      case 'C' => Type.Primitive.Char
      case 'D' => Type.Primitive.Double
      case 'F' => Type.Primitive.Float
      case 'I' => Type.Primitive.Int
      case 'J' => Type.Primitive.Long
      case 'L' => Type.Reference(parseClassName())
      case 'S' => Type.Primitive.Short
      case 'V' => Type.Primitive.Void
      case 'Z' => Type.Primitive.Boolean
      case '[' => Type.Array(parseType())
    }

  def parseClassName(): String = {
    val buf = new StringBuilder
    while (current() != ';') buf += next();
    eat(';')
    buf.toString()
  }

  private def next(): Char = {
    val char = current()
    _current += 1
    char
  }

}
