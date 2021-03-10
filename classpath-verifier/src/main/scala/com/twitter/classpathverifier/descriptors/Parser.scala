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
