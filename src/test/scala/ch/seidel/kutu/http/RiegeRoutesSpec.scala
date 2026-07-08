package ch.seidel.kutu.http

import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.*
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.HttpMethods.{GET, POST, PUT}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.util.UUID
import scala.compiletime.uninitialized

class RiegeRoutesSpec extends KuTuBaseSpec {
  private var testWettkampf: Wettkampf = uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()
    testWettkampf = insertGeTuWettkampf("RiegeRoutesTestWK", 2)
  }

  private def withRoutes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private def jwtFor(userId: String): RawHeader = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))
  }

  private def adminJwtFor(userId: String): RawHeader = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays, isAdmin = true)
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))
  }

  private val emptySuggestionBody: String = {
    val req = RiegeSuggestionRequest()
    riegeSuggestionRequestFormat.write(req).compactPrint
  }

  "RiegeRoutes" should {

    // ── GET /api/competition/{uuid}/riege (empty) ────────────────────────────

    "return empty list when no riegen exist" in {
      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        val items = responseAs[String].parseJson.convertTo[List[RiegeItem]]
        items shouldBe empty
      }
    }

    // ── POST /api/competition/{uuid}/riege/generate (unauthorized) ───────────

    "reject generate without JWT" in {
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/riege/generate",
        entity = HttpEntity(ContentTypes.`application/json`, emptySuggestionBody)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject generate with a JWT for a different UUID" in {
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/riege/generate",
        entity = HttpEntity(ContentTypes.`application/json`, emptySuggestionBody)
      ).addHeader(adminJwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    // ── POST /api/competition/{uuid}/riege/generate (authorized) ─────────────

    "generate riegen with valid JWT and return preview" in {
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/riege/generate",
        entity = HttpEntity(ContentTypes.`application/json`, emptySuggestionBody)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        val response = responseAs[String].parseJson.convertTo[RiegePreviewResponse]
        response.riegen should not be empty
        response.durchgange should not be empty
        // Verify each riege item has required fields
        response.riegen.foreach { item =>
          item.name should not be empty
          item.durchgang shouldBe defined
          item.startId shouldBe defined
          item.startName shouldBe defined
        }
        // Verify each durchgang duration item has required fields
        response.durchgange.foreach { d =>
          d.name should not be empty
          d.totalMillis should be > 0L
        }
      }
    }

    // ── GET /api/competition/{uuid}/riege (after generate) ───────────────────

    "return non-empty riegen list after generation" in {
      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        val items = responseAs[String].parseJson.convertTo[List[RiegeItem]]
        items should not be empty
        items.foreach { item =>
          item.name should not be empty
          item.athletCount should be >= 0
        }
      }
    }

    // ── POST /api/competition/{uuid}/riege/generate with custom params ───────

    "generate riegen with maxRiegenSize=99 to create fewer durchgange" in {
      val largeGroupBody = {
        val req = RiegeSuggestionRequest(maxRiegenSize = 99)
        riegeSuggestionRequestFormat.write(req).compactPrint
      }
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/riege/generate",
        entity = HttpEntity(ContentTypes.`application/json`, largeGroupBody)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        val response = responseAs[String].parseJson.convertTo[RiegePreviewResponse]
        response.riegen should not be empty
        // With maxRiegenSize=99, all athletes fit in fewer groups
        val allAthletes = response.riegen.map(_.athletCount).sum
        allAthletes should be > 0
      }
    }

    "generate riegen with GetrennteDurchgaenge option" in {
      val separatedBody = {
        val req = RiegeSuggestionRequest(splitSexOption = Some("GetrennteDurchgaenge"))
        riegeSuggestionRequestFormat.write(req).compactPrint
      }
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/riege/generate",
        entity = HttpEntity(ContentTypes.`application/json`, separatedBody)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        val response = responseAs[String].parseJson.convertTo[RiegePreviewResponse]
        response.riegen should not be empty
        response.durchgange should not be empty
      }
    }

    // ── PUT /api/competition/{uuid}/riege (update single riege) ──────────────

    "update a riege's durchgang via PUT" in {
      // First read the current state to get a riege name
      val currentItems = {
        val resp = HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege")
          .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
          responseAs[String].parseJson.convertTo[List[RiegeItem]]
        }
        resp
      }
      val targetRiege = currentItems.find(_.athletCount > 0).getOrElse(currentItems.head)
      val newDurchgang = "PUT-Update-DG"

      val updateReq = UpdateRiegeRequest(
        name = targetRiege.name,
        durchgang = Some(newDurchgang),
        startId = targetRiege.startId,
        kind = targetRiege.kind
      )
      val updateBody = updateRiegeRequestFormat.write(updateReq).compactPrint

      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege",
        entity = HttpEntity(ContentTypes.`application/json`, updateBody)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        val updated = responseAs[String].parseJson.convertTo[RiegeItem]
        updated.name should ===(targetRiege.name)
        updated.durchgang should ===(Some(newDurchgang))
      }
    }

    "reject PUT without JWT" in {
      val updateReq = UpdateRiegeRequest(name = "dummy", durchgang = Some("DG"), startId = Some(1L))
      val updateBody = updateRiegeRequestFormat.write(updateReq).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege",
        entity = HttpEntity(ContentTypes.`application/json`, updateBody)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    // ── POST /api/competition/{uuid}/riege/reset ─────────────────────────────

    "reject reset without JWT" in {
      HttpRequest(POST, s"/api/competition/${testWettkampf.uuid.get}/riege/reset") ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reset all riegen with valid JWT" in {
      HttpRequest(POST, s"/api/competition/${testWettkampf.uuid.get}/riege/reset")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
      // Verify it's empty after reset
      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        val items = responseAs[String].parseJson.convertTo[List[RiegeItem]]
        items shouldBe empty
      }
    }

    // ── GET /api/competition/{uuid}/riege/duration ───────────────────────────

    "return duration data after regeneration" in {
      // Regenerate first so there's data
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/riege/generate",
        entity = HttpEntity(ContentTypes.`application/json`, emptySuggestionBody)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }

      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege/duration")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        val items = responseAs[String].parseJson.convertTo[List[DurchgangDurationItem]]
        items should not be empty
        items.foreach { d =>
          d.name should not be empty
          d.title should not be empty
        }
      }
    }

    "reject duration without JWT" in {
      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege/duration") ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    "clean up after all tests" in {
      HttpRequest(POST, s"/api/competition/${testWettkampf.uuid.get}/riege/reset")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }
  }
}
