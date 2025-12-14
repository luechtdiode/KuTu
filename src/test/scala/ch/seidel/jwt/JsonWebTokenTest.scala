package ch.seidel.jwt

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JsonWebTokenTest extends AnyWordSpec
  with Matchers {

  "JsonWebToken" should {
    val key = "your-256-bit-secret"
    val expectedJWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.GuoUe6tw79bJlbU1HU0ADX0pr0u2kf3r_4OdrDufSfQ"

    "create JWT correctly" in {
      val jwt = expectedJWT
      jwt match {
        case JsonWebToken(header, claims, signature) =>
          val signingInput = jwt.split("\\.").take(2).mkString(".")
          val expectedSignatureBytes = JsonWebSignature("HS256", signingInput, key)
          val expectedSignature = java.util.Base64.getUrlEncoder.withoutPadding().encodeToString(expectedSignatureBytes)
          signature shouldBe expectedSignature

          header.algorithm shouldBe Some("HS256")
          claims match {
            case claimsJValue: JwtClaimsSetJValue =>
              val claimsMapTry = claimsJValue.asSimpleMap
              claimsMapTry.isSuccess shouldBe true
              val claimsMap = claimsMapTry.get
              claimsMap("sub") shouldBe "1234567890"
              claimsMap("name") shouldBe "John Doe"
              claimsMap("iat") shouldBe "1516239022"
          }
      }
    }

    "unapply fails with incomplete JWT" in {
      val jwt = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ"
      an[MatchError] should be thrownBy {
        jwt match {
          case JsonWebToken(header, claims, signature) =>
            fail("Unapply should have failed for invalid JWT")
        }
      }
    }

    "unapply fails with empty header JWT" in {
      val jwt = ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.GuoUe6tw79bJlbU1HU0ADX0pr0u2kf3r_4OdrDufSfQ"
      an[MatchError] should be thrownBy {
        jwt match {
          case JsonWebToken(header, claims, signature) =>
            fail("Unapply should have failed for invalid JWT")
        }
      }
    }

    "unapply fails with empty claims JWT" in {
      val jwt = "eyJhbGciOiJIUzM4NCJ9..GuoUe6tw79bJlbU1HU0ADX0pr0u2kf3r_4OdrDufSfQ"
      an[MatchError] should be thrownBy {
        jwt match {
          case JsonWebToken(header, claims, signature) =>
            fail("Unapply should have failed for invalid JWT")
        }
      }
    }

    "validate JWT correctly" in {
      JsonWebToken.validate(expectedJWT, key) shouldBe true
    }

    "invalidate JWT correctly" in {
      JsonWebToken.validate(expectedJWT, key+"xxx") shouldBe false

      an[IllegalArgumentException] should be thrownBy {
        JsonWebToken.validate(expectedJWT, null)
      }

      an[UnsupportedOperationException] should be thrownBy {
        JsonWebToken.validate("invalid.jwt.token", key)
      }
    }
  }
}
