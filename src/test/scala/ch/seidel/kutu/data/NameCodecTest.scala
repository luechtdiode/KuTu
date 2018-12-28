package ch.seidel.kutu.data

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NameCodecTest extends FunSuite {

  test("testEncode") {
    assert(NameCodec.encode("Maier") === NameCodec.encode("Meier"))
    assert(NameCodec.encode("Maier") === NameCodec.encode("Meyer"))
    assert(NameCodec.encode("Sofia") === NameCodec.encode("Sophia"))
  }

}
