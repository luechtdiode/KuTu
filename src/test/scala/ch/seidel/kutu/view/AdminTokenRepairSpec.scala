package ch.seidel.kutu.view

import ch.seidel.jwt.{JsonWebToken, JwtClaimsSet}
import ch.seidel.kutu.Config
import ch.seidel.kutu.domain.CreatorMetaData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.TimeUnit

class AdminTokenRepairSpec extends AnyWordSpec with Matchers {

  private val userKey = "user"
  private val expiredAtKey = "expiredAtKey"
  private val uuid = "b3f0a5f2-6a4a-4f7c-9c1e-2f9b0a1c2d3e"

  private def token(claims: Map[String, String]): String =
    JsonWebToken(Config.jwtHeader, JwtClaimsSet(claims), "irrelevant")

  private def legacyToken: String =
    token(Map(userKey -> uuid, expiredAtKey -> (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(Int.MaxValue.toLong)).toString))

  private def adminToken: String =
    token(Map(
      userKey -> uuid,
      expiredAtKey -> (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(Int.MaxValue.toLong)).toString,
      "admin" -> "true"))

  private def limitedAdminToken: String =
    token(Map(
      userKey -> uuid,
      expiredAtKey -> (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)).toString,
      "admin" -> "true"))

  "localCheck" should {
    "accept an admin token with infinite expiry" in {
      AdminTokenRepair.localCheck(hasAdmin = true, hasInfiniteExpiry = true) shouldBe AdminTokenRepair.LocalCheck.Usable
    }

    "reject a legacy token without admin claim" in {
      AdminTokenRepair.localCheck(hasAdmin = false, hasInfiniteExpiry = true) shouldBe AdminTokenRepair.LocalCheck.NeedsRepair
    }

    "reject a limited admin token" in {
      AdminTokenRepair.localCheck(hasAdmin = true, hasInfiniteExpiry = false) shouldBe AdminTokenRepair.LocalCheck.NeedsRepair
    }
  }

  "localCheckFromToken" should {
    "detect the legacy secret written by older versions" in {
      AdminTokenRepair.localCheckFromToken(Some(legacyToken)) shouldBe AdminTokenRepair.LocalCheck.NeedsRepair
    }

    "detect a current admin secret" in {
      AdminTokenRepair.localCheckFromToken(Some(adminToken)) shouldBe AdminTokenRepair.LocalCheck.Usable
    }

    "require the server round trip for a limited admin token" in {
      AdminTokenRepair.localCheckFromToken(Some(limitedAdminToken)) shouldBe AdminTokenRepair.LocalCheck.NeedsRepair
    }

    "report a missing secret" in {
      AdminTokenRepair.localCheckFromToken(None) shouldBe AdminTokenRepair.LocalCheck.NoSecret
      AdminTokenRepair.localCheckFromToken(Some("   ")) shouldBe AdminTokenRepair.LocalCheck.NoSecret
    }

    "report a non-jwt secret as repair needed so the server decides" in {
      AdminTokenRepair.localCheckFromToken(Some("kein-jwt")) shouldBe AdminTokenRepair.LocalCheck.NeedsRepair
    }
  }

  "decodeClaims" should {
    "read the claims of a token" in {
      AdminTokenRepair.decodeClaims(adminToken).flatMap(_.get("admin")) shouldBe Some("true")
    }

    "return None for a non-jwt" in {
      AdminTokenRepair.decodeClaims("irgendwas") shouldBe None
    }
  }

  "nextStep" should {
    "open the browser on 200" in {
      AdminTokenRepair.nextStep(Some(200), creatorSent = false) shouldBe AdminTokenRepair.NextStep.OpenBrowser
    }

    "ask for creator data on 409 of the first attempt" in {
      AdminTokenRepair.nextStep(Some(409), creatorSent = false) shouldBe AdminTokenRepair.NextStep.CollectCreatorData
    }

    "abort on 409 when creator data was already sent" in {
      val step = AdminTokenRepair.nextStep(Some(409), creatorSent = true)
      step shouldBe a[AdminTokenRepair.NextStep.Abort]
      step.asInstanceOf[AdminTokenRepair.NextStep.Abort].reason should include("bereits andere Veranstalter-Daten")
    }

    "abort on rejected credentials" in {
      Seq(401, 403).foreach { status =>
        val step = AdminTokenRepair.nextStep(Some(status), creatorSent = false)
        step shouldBe a[AdminTokenRepair.NextStep.Abort]
        step.asInstanceOf[AdminTokenRepair.NextStep.Abort].reason should include("Passwort")
      }
    }

    "abort on unknown competition" in {
      val step = AdminTokenRepair.nextStep(Some(404), creatorSent = false)
      step shouldBe a[AdminTokenRepair.NextStep.Abort]
      step.asInstanceOf[AdminTokenRepair.NextStep.Abort].reason should include("existiert")
    }

    "abort when the server is unreachable" in {
      val step = AdminTokenRepair.nextStep(None, creatorSent = false)
      step shouldBe a[AdminTokenRepair.NextStep.Abort]
      step.asInstanceOf[AdminTokenRepair.NextStep.Abort].reason should include("nicht erreichbar")
    }

    "abort on an unexpected status" in {
      val step = AdminTokenRepair.nextStep(Some(500), creatorSent = false)
      step shouldBe a[AdminTokenRepair.NextStep.Abort]
      step.asInstanceOf[AdminTokenRepair.NextStep.Abort].reason should include("500")
    }
  }

  "the terms version" should {
    "be the version expected by the server" in {
      CreatorMetaData.currentTermsVersion shouldBe "1.0"
    }

    "be complete text" in {
      CreatorMetaData.termsText should include("Nutzungsbedingungen")
      CreatorMetaData.termsText should include("Haftungsausschluss")
      CreatorMetaData.termsText should include("24 Stunden")
    }
  }
}
