package ch.seidel.kutu.domain

import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.base.KuTuBaseSpec
import org.apache.pekko.http.scaladsl.model.HttpMethods.GET
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{HttpRequest, MediaTypes, StatusCodes}

class RegistrationRoutesReadAndMaintenanceScenariosSpec extends KuTuBaseSpec {

  private val wk: Wettkampf = insertGeTuWettkampf("RegistrationRoutesReadMaintenance", 0)

  private def routes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private def jwtFor(userId: String): RawHeader = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))
  }

  private def registrationJwt(reg: Registration): RawHeader = jwtFor(reg.id.toString)

  // Sync-admin endpoints compare authenticated userId against competition UUID string.
  private def syncAdminJwt: RawHeader = jwtFor(wk.uuid.get)

  private def newRegistration(name: String): Registration =
    createRegistration(NewRegistration(
      wk.id,
      name,
      s"$name-Verband",
      "RespName",
      "RespVorname",
      "+41790000000",
      s"$name@test.local",
      "SecretPW1!"
    ))

  private def addJudge(reg: Registration, name: String, vorname: String): JudgeRegistration =
    createJudgeRegistration(JudgeRegistration(0, reg.id, "M", name, vorname, "+41791234567", s"$vorname.$name@test.local", "Comment", 0))

  "registration routes read scenarios" should {

    "return registration detail for owning registration token" in {
      val reg = newRegistration("OwnerReadClub")

      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/${reg.id}")
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
        val loaded = entityAs[Registration]
        loaded.id should ===(reg.id)
      }
    }

    "return judge detail by id" in {
      val reg = newRegistration("JudgeDetailClub")
      val judge = addJudge(reg, "Schneider", "Markus")

      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/${reg.id}/judges/${judge.id}")
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
        val loaded = entityAs[JudgeRegistration]
        loaded.id should ===(judge.id)
      }
    }

    "delete judge via judges endpoint" in {
      val reg = newRegistration("JudgeDeleteClub")
      val judge = addJudge(reg, "Tester", "Delete")

      HttpRequest(org.apache.pekko.http.scaladsl.model.HttpMethods.DELETE, s"/api/registrations/${wk.uuid.get}/${reg.id}/judges/${judge.id}")
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
      }

      selectJudgeRegistrations(reg.id).exists(_.id == judge.id) shouldBe false
    }

    "serve registrations html list" in {
      val reg = newRegistration("HtmlRegClub")

      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}?html") ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
        contentType.mediaType should ===(MediaTypes.`text/html`)
        val html = entityAs[String]
        html.contains(reg.vereinname) shouldBe true
      }
    }

    "serve judges html list when authenticated" in {
      val reg = newRegistration("HtmlJudgeClub")
      val judge = addJudge(reg, "Html", "Judge")

      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/judges?html")
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
        val html = entityAs[String]
        html.contains(judge.name) shouldBe true
      }
    }

  }

  "registration routes maintenance scenarios" should {

    "return OK for regchanged with sync-admin token" in {
      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/regchanged")
        .addHeader(syncAdminJwt) ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "return Conflict for regchanged with registration token" in {
      val reg = newRegistration("RegChangedConflictClub")

      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/regchanged")
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "return fallback message for approvemail without mail parameter" in {
      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/approvemail") ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should include("unable to approve without mail")
      }
    }

    "return OK for refreshsyncs" in {
      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/refreshsyncs") ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "return JSON for syncactions" in {
      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/syncactions") ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
        val json = entityAs[String]
        json.startsWith("[") shouldBe true
      }
    }
  }
}


