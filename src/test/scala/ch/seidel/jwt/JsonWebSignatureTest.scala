package ch.seidel.jwt

import ch.seidel.jwt.JsonWebSignature.HexToString
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JsonWebSignatureTest extends AnyWordSpec
  with Matchers {

  "HexToString" should {
    "map" in {
      HexToString.converter(Array[Byte](0x01, 0x02, 0x0A, 0x0F)) shouldBe "01020a0f"
    }
  }

  "apply HS256" should {
    "generate correct hash" in {
      val key = "your-256-bit-secret"
      val data = JsonWebToken(
        JwtHeader(Some("HS256"), None, None),
        JwtClaimsSet(Map(
          "sub" -> "1234567890",
          "name" -> "John Doe",
          "iat" -> 1516239022
        )),
        key
      )
      val expectedHash = "r2a2d6hnW2SORr0f93BVJyFDw3aWl_78qedbVcIvq0c"
      val hashBytes = JsonWebSignature("HS256", data, key)
      val hashString = java.util.Base64.getUrlEncoder.withoutPadding().encodeToString(hashBytes)
      hashString shouldBe expectedHash
    }
  }

  "apply HS384" should {
    "generate correct hash" in {
      val key = "your-384-bit-secret"
      val data = JsonWebToken(
        JwtHeader(Some("HS384"), None, None),
        JwtClaimsSet(Map(
          "sub" -> "1234567890",
          "name" -> "John Doe",
          "iat" -> 1516239022
        )),
        key
      )
      val expectedHash = "IsP3oXBCz5j-yzlUSFvUG5y2cEsfwSRt6nfRQG9ANNd7Cz7Oa_kI56APAYN1kyxL"
      val hashBytes = JsonWebSignature("HS384", data, key)
      val hashString = java.util.Base64.getUrlEncoder.withoutPadding().encodeToString(hashBytes)
      hashString shouldBe expectedHash
    }
  }

  "apply HS512" should {
    "generate correct hash" in {
      val key = "your-512-bit-secret"
      val data = JsonWebToken(
        JwtHeader(Some("HS512"), None, None),
        JwtClaimsSet(Map(
          "sub" -> "1234567890",
          "name" -> "John Doe",
          "iat" -> 1516239022
        )),
        key
      )
      val expectedHash = "yfzYaOIjAsbJHdhPhhYO+XM+KB02OscLLQQrL098zciz1LnsEAw0gljRlVoOL6zeD9dNDK+j7xHA9Yd2rNsbFQ=="
      val hashBytes = JsonWebSignature("HS512", data, key)
      val hashString = java.util.Base64.getEncoder.encodeToString(hashBytes)
      hashString shouldBe expectedHash
    }
  }

  "apply none" should {
    "throw UnsupportedOperationException" in {
      val data = "any-data"
      an[UnsupportedOperationException] should be thrownBy {
        JsonWebSignature("none", data, null)
      }
    }
  }

  "apply unknown algorithm" should {
    "throw UnsupportedOperationException" in {
      val data = "any-data"
      val key = "any-key"
      an[UnsupportedOperationException] should be thrownBy {
        JsonWebSignature("unknown-algo", data, key)
      }
    }
  }
}
