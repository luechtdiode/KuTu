package ch.seidel.jwt

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JwtHeaderTest extends AnyWordSpec
  with Matchers {

  "JwtHeader" should {
    "serialize to JSON string correctly" in {
      val header = JwtHeader("HS256", "application/jwt")
      val jsonString = header.asJsonString
      jsonString shouldBe """{"alg":"HS256","cty":"application/jwt","typ":"JWT"}"""
    }

    "deserialize from JSON string correctly" in {
      val jsonString = """{"alg":"RS256","cty":"application/jwt","typ":"JWT"}"""
      val header = JwtHeader.fromJsonString(jsonString)
      header.algorithm shouldBe Some("RS256")
      header.contentType shouldBe Some("application/jwt")
      header.typ shouldBe Some("JWT")
    }

    "handle missing optional fields during deserialization" in {
      val jsonString = """{"alg":"none"}"""
      val header = JwtHeader.fromJsonString(jsonString)
      header.algorithm shouldBe Some("none")
      header.contentType shouldBe None
      header.typ shouldBe None
    }

    "handle empty optional fields during deserialization" in {
      val jsonString = """{"typ":null,"cty":null,"alg":null}"""
      val header = JwtHeader.fromJsonString(jsonString)
      header.algorithm shouldBe None
      header.contentType shouldBe None
      header.typ shouldBe None
    }
  }

}
