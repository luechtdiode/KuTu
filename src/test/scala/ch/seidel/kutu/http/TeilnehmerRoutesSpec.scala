package ch.seidel.kutu.http

import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.*
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.HttpMethods.{GET, PUT}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import spray.json.*

import java.util.UUID
import scala.compiletime.uninitialized

class TeilnehmerRoutesSpec extends KuTuBaseSpec {
  private var testWettkampf: Wettkampf = uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()
    testWettkampf = insertGeTuWettkampf("TeilnehmerRoutesTestWK", 2)
  }

  private def withRoutes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private def adminJwtFor(userId: String): RawHeader = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays, isAdmin = true)
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))
  }

  private def base = s"/api/competition/${testWettkampf.uuid.get}/teilnehmer"

  "TeilnehmerRoutes" should {
    "reject list without JWT" in {
      HttpRequest(GET, base) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject list with JWT of another competition" in {
      HttpRequest(GET, base).addHeader(adminJwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "list participants and update riege2" in {
      val items = HttpRequest(GET, base).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        responseAs[String].parseJson.asInstanceOf[JsArray].elements
      }
      items should not be empty
      val athletId = items.head.asJsObject.fields("athletId").convertTo[Long](using DefaultJsonProtocol.LongJsonFormat)
      HttpRequest(PUT, s"$base/athlet/$athletId",
        entity = HttpEntity(ContentTypes.`application/json`, """{"riege":"R1","riege2":"R2"}"""))
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        val obj = responseAs[String].parseJson.asJsObject
        obj.fields("riege2") should ===(JsString("R2"))
        obj.fields("riege") should ===(JsString("R1"))
      }
    }

    "provide teams and programme, and change team and kategorie" in {
      val admin = adminJwtFor(testWettkampf.uuid.get)
      val items = HttpRequest(GET, base).addHeader(admin) ~> withRoutes ~> check {
        responseAs[String].parseJson.asInstanceOf[JsArray].elements
      }
      val first = items.head.asJsObject
      val athletId = first.fields("athletId").convertTo[Long](using DefaultJsonProtocol.LongJsonFormat)
      HttpRequest(GET, s"$base/athlet/$athletId/teams").addHeader(admin) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        responseAs[String].parseJson.asInstanceOf[JsArray].elements should not be empty
      }
      val pgms = HttpRequest(GET, s"$base/programme").addHeader(admin) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        responseAs[String].parseJson.asInstanceOf[JsArray].elements
      }
      pgms should not be empty
      val pgmId = pgms.last.asJsObject.fields("id").convertTo[Long](using DefaultJsonProtocol.LongJsonFormat)
      HttpRequest(PUT, s"$base/athlet/$athletId",
        entity = HttpEntity(ContentTypes.`application/json`, s"""{"programmId":$pgmId,"team":1}"""))
        .addHeader(admin) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        val obj = responseAs[String].parseJson.asJsObject
        obj.fields("programmId").convertTo[Long](using DefaultJsonProtocol.LongJsonFormat) should ===(pgmId)
        obj.fields("team").convertTo[Int](using DefaultJsonProtocol.IntJsonFormat) should ===(1)
      }
    }

    "export excel and print html" in {
      HttpRequest(GET, s"$base/export").addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        responseAs[Array[Byte]].length should be > 100
      }
      HttpRequest(GET, s"$base/print/notenblatt").addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType.mediaType should ===(MediaTypes.`text/html`)
      }
      HttpRequest(GET, s"$base/print/teilnehmer").addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }
  }
}
