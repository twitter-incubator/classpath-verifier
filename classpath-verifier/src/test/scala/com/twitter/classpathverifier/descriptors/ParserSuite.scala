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
