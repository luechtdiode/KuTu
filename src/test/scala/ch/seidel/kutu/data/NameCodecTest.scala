package ch.seidel.kutu.data

import org.scalatest.funsuite.AnyFunSuite

class NameCodecTest extends AnyFunSuite {

  test("testEncode") {
    assert(NameCodec.encode("Maier") === NameCodec.encode("Meier"))
    assert(NameCodec.encode("Maier") === NameCodec.encode("Meyer"))
    assert(NameCodec.encode("Sofia") === NameCodec.encode("Sophia"))
  }

}
