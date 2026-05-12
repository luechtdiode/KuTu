package ch.seidel.kutu.http

import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.actors.{FinishDurchgang, FinishDurchgangStep, ResetStartDurchgang, StartDurchgang}
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.{ProgrammRaw, Wettkampf}
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.HttpMethods.{DELETE, GET, POST, PUT}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.testkit.RouteTestTimeout
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.io.ByteArrayOutputStream
import java.util.UUID
import scala.compiletime.uninitialized
import scala.concurrent.duration.*

class WettkampfRoutesSpec extends KuTuBaseSpec {
  implicit val routeTestTimeout: RouteTestTimeout = RouteTestTimeout(5.seconds) // or any duration you need
  private var testWettkampf: Wettkampf = uninitialized

  /** Competition whose zip is prepared in beforeAll; the DB entry is deleted before tests run so
   *  we can use the POST upload endpoint to re-import it under the original UUID. */
  private var uploadSourceWettkampf: Wettkampf = uninitialized
  private var uploadZipBytes: Array[Byte] = uninitialized

  /** Competition reserved exclusively for the DELETE happy-path test. */
  private var forDeletionWettkampf: Wettkampf = uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()

    testWettkampf = insertGeTuWettkampf("WkRoutesMainWK", 2)
    makeEinteilung(testWettkampf)

    // ── upload-test setup ──────────────────────────────────────────────────────
    // 1) Create a fresh competition and export it to a byte array.
    uploadSourceWettkampf = insertGeTuWettkampf("WkRoutesUploadSrc", 1)
    val bos = new ByteArrayOutputStream()
    ResourceExchanger.exportWettkampfToStream(uploadSourceWettkampf, bos)
    uploadZipBytes = bos.toByteArray
    // 2) Remove it from the DB so the UUID is "unknown" when the POST route checks.
    deleteWettkampf(uploadSourceWettkampf.id)

    // ── delete-test setup ──────────────────────────────────────────────────────
    forDeletionWettkampf = insertGeTuWettkampf("WkRoutesDeleteWK", 1)
  }

  // ─── helpers ───────────────────────────────────────────────────────────────

  private def withRoutes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private def jwtFor(userId: String): RawHeader = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))
  }

  /** Builds a multipart/form-data entity with a "zip" part, matching the field
   *  name expected by the `fileUpload("zip")` directive in WettkampfRoutes. */
  private def zipEntity(bytes: Array[Byte], filename: String): RequestEntity =
    Multipart.FormData(
      Multipart.FormData.BodyPart.Strict(
        "zip",
        HttpEntity(bytes),
        Map("filename" -> filename)
      )
    ).toEntity

  /** Exports testWettkampf on-the-fly so PUT tests always use up-to-date bytes. */
  private def testWettkampfZipEntity(): RequestEntity = {
    val bos = new ByteArrayOutputStream()
    ResourceExchanger.exportWettkampfToStream(testWettkampf, bos)
    zipEntity(bos.toByteArray, s"${testWettkampf.easyprint}.zip")
  }

  /** Returns the first durchgang name found via the API, or a fallback. */
  private def discoverDurchgang(wk: Wettkampf): String = {
    var name = "Durchgang 1"
    HttpRequest(GET, s"/api/durchgang/${wk.uuid.get}") ~> withRoutes ~> check {
      val list = responseAs[String].parseJson.convertTo[List[String]]
      if list.nonEmpty then name = list.head
    }
    name
  }

  // ─── test body ─────────────────────────────────────────────────────────────

  "WettkampfRoutes" should {

    // ── public listing ────────────────────────────────────────────────────────

    "return all competitions as JSON" in {
      HttpRequest(GET, "/api/competition") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[String] should include("titel")
      }
    }

    "return programmlist as JSON with at least one entry" in {
      HttpRequest(GET, "/api/competition/programmlist") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[String].parseJson.convertTo[List[ProgrammRaw]].nonEmpty shouldBe true
      }
    }

    "return competitions by verein as JSON" in {
      HttpRequest(GET, "/api/competition/byVerein/1") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return empty list for a verein that has no competitions" in {
      HttpRequest(GET, "/api/competition/byVerein/99999") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[String].parseJson.convertTo[List[JsValue]] shouldBe empty
      }
    }

    // ── JWT renewal (isTokenExpired) ──────────────────────────────────────────

    "reject isTokenExpired without JWT" in {
      HttpRequest(GET, "/api/isTokenExpired") ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "renew token on isTokenExpired with a valid JWT" in {
      HttpRequest(GET, "/api/isTokenExpired")
        .addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        header(jwtAuthorizationKey) should not be empty
      }
    }

    // ── GET /api/competition/:wkuuid – download ───────────────────────────────

    "download existing competition as a zip binary" in {
      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType.mediaType should ===(MediaTypes.`application/zip`)
        response.entity.contentLengthOption.getOrElse(0L) should be > 0L
      }
    }

    // ── POST /api/competition/:wkuuid – upload new competition ────────────────

    "reject upload when competition UUID already exists in the database" in {
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}",
        entity = zipEntity(uploadZipBytes, "test.zip")
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
        responseAs[String] should include("mehrfach")
      }
    }

    "upload a new competition and return OK with a JWT secret header" in {
      val uuid = uploadSourceWettkampf.uuid.get
      HttpRequest(
        POST,
        s"/api/competition/$uuid",
        entity = zipEntity(uploadZipBytes, s"$uuid.zip")
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        header(jwtAuthorizationKey) should not be empty
      }
    }

    // ── PUT /api/competition/:wkuuid – update existing competition ────────────

    "reject competition update without a JWT" in {
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}",
        entity = testWettkampfZipEntity()
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject competition update with a JWT for a different UUID" in {
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}",
        entity = testWettkampfZipEntity()
      ).addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "accept competition update with the matching JWT and return OK" in {
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}",
        entity = testWettkampfZipEntity()
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    // ── DELETE /api/competition/:wkuuid ───────────────────────────────────────

    "reject competition deletion without a JWT" in {
      HttpRequest(DELETE, s"/api/competition/${forDeletionWettkampf.uuid.get}") ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject competition deletion with a JWT for a different UUID" in {
      HttpRequest(DELETE, s"/api/competition/${forDeletionWettkampf.uuid.get}")
        .addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "delete a competition with the matching JWT" in {
      HttpRequest(DELETE, s"/api/competition/${forDeletionWettkampf.uuid.get}")
        .addHeader(jwtFor(forDeletionWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    // ── POST /api/competition/:wkuuid/start ───────────────────────────────────

    "reject StartDurchgang without a JWT" in {
      val sd = StartDurchgang(testWettkampf.uuid.get, discoverDurchgang(testWettkampf))
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/start",
        entity = HttpEntity(ContentTypes.`application/json`, startDurchgangFormat.write(sd).compactPrint)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject StartDurchgang with a JWT for a different UUID" in {
      val sd = StartDurchgang(testWettkampf.uuid.get, discoverDurchgang(testWettkampf))
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/start",
        entity = HttpEntity(ContentTypes.`application/json`, startDurchgangFormat.write(sd).compactPrint)
      ).addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "accept StartDurchgang with the matching JWT" in {
      val sd = StartDurchgang(testWettkampf.uuid.get, discoverDurchgang(testWettkampf))
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/start",
        entity = HttpEntity(ContentTypes.`application/json`, startDurchgangFormat.write(sd).compactPrint)
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    // ── POST /api/competition/:wkuuid/reset ───────────────────────────────────

    "reject ResetStartDurchgang without a JWT" in {
      val rsd = ResetStartDurchgang(testWettkampf.uuid.get, discoverDurchgang(testWettkampf))
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/reset",
        entity = HttpEntity(ContentTypes.`application/json`, resetStartDurchgangFormat.write(rsd).compactPrint)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject ResetStartDurchgang with a JWT for a different UUID" in {
      val rsd = ResetStartDurchgang(testWettkampf.uuid.get, discoverDurchgang(testWettkampf))
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/reset",
        entity = HttpEntity(ContentTypes.`application/json`, resetStartDurchgangFormat.write(rsd).compactPrint)
      ).addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "accept ResetStartDurchgang with the matching JWT" in {
      val rsd = ResetStartDurchgang(testWettkampf.uuid.get, discoverDurchgang(testWettkampf))
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/reset",
        entity = HttpEntity(ContentTypes.`application/json`, resetStartDurchgangFormat.write(rsd).compactPrint)
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    // ── POST /api/competition/:wkuuid/stop ────────────────────────────────────

    "reject FinishDurchgang without a JWT" in {
      val fd = FinishDurchgang(testWettkampf.uuid.get, discoverDurchgang(testWettkampf))
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/stop",
        entity = HttpEntity(ContentTypes.`application/json`, finishDurchgangFormat.write(fd).compactPrint)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject FinishDurchgang with a JWT for a different UUID" in {
      val fd = FinishDurchgang(testWettkampf.uuid.get, discoverDurchgang(testWettkampf))
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/stop",
        entity = HttpEntity(ContentTypes.`application/json`, finishDurchgangFormat.write(fd).compactPrint)
      ).addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "accept FinishDurchgang with the matching JWT" in {
      val fd = FinishDurchgang(testWettkampf.uuid.get, discoverDurchgang(testWettkampf))
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/stop",
        entity = HttpEntity(ContentTypes.`application/json`, finishDurchgangFormat.write(fd).compactPrint)
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    // ── POST /api/competition/:wkuuid/finishedStep ────────────────────────────

    "reject FinishDurchgangStep without a JWT" in {
      val fds = FinishDurchgangStep(testWettkampf.uuid.get)
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/finishedStep",
        entity = HttpEntity(ContentTypes.`application/json`, finishDurchgangStepFormat.write(fds).compactPrint)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject FinishDurchgangStep with a JWT for a different UUID" in {
      val fds = FinishDurchgangStep(testWettkampf.uuid.get)
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/finishedStep",
        entity = HttpEntity(ContentTypes.`application/json`, finishDurchgangStepFormat.write(fds).compactPrint)
      ).addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "accept FinishDurchgangStep with the matching JWT" in {
      val fds = FinishDurchgangStep(testWettkampf.uuid.get)
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/finishedStep",
        entity = HttpEntity(ContentTypes.`application/json`, finishDurchgangStepFormat.write(fds).compactPrint)
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }
  }
}


