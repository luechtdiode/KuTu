package ch.seidel.kutu.http

import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.actors.{FinishDurchgangStation, WertungContainer}
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.*
import org.apache.pekko.http.scaladsl.model.HttpMethods.{GET, POST, PUT}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.net.URLEncoder
import java.util.UUID
import scala.compiletime.uninitialized

class WertungenRoutesSpec extends KuTuBaseSpec {
  private var testWettkampf: Wettkampf = uninitialized
  private var sampleWertung: Wertung = uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()
    testWettkampf = insertGeTuWettkampf("TestWertungenWK", 2)
    makeEinteilung(testWettkampf)

    sampleWertung = selectWertungen(wettkampfId = Some(testWettkampf.id)).head.toWertung
  }

  private def withRoutes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private def jwtFor(userId: String): RawHeader = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))
  }

  private def wertungEntity(wertung: Wertung): HttpEntity.Strict =
    HttpEntity(ContentTypes.`application/json`, wertungFormat.write(wertung).compactPrint)

  private def encodePathSegment(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

  private def discoverStepContext(): (String, Long, Int, Wertung) = {
    var durchgang: String = ""
    HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}") ~> withRoutes ~> check {
      status should ===(StatusCodes.OK)
      val durchgaenge = responseAs[String].parseJson.convertTo[List[String]].sorted
      durchgaenge.nonEmpty.shouldBe(true)
      durchgang = durchgaenge.head
    }

    val durchgangPath = encodePathSegment(durchgang)
    var geraet: Long = 0L
    HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath") ~> withRoutes ~> check {
      status should ===(StatusCodes.OK)
      val geraete = responseAs[String].parseJson.convertTo[List[Disziplin]].map(_.id).sorted
      geraete.nonEmpty.shouldBe(true)
      geraet = geraete.head
    }

    var step: Int = 1
    HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath/$geraet") ~> withRoutes ~> check {
      status should ===(StatusCodes.OK)
      val steps = responseAs[String].parseJson.convertTo[List[Int]].sorted
      steps.nonEmpty.shouldBe(true)
      step = steps.head
    }

    var wertungForStep: Option[Wertung] = None
    HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath/$geraet/$step") ~> withRoutes ~> check {
      status should ===(StatusCodes.OK)
      val stepWertungen = responseAs[String].parseJson.convertTo[List[WertungContainer]]
      stepWertungen.nonEmpty.shouldBe(true)
      wertungForStep = stepWertungen.map(_.wertung).find(_.wettkampfdisziplinId > 0L).orElse(stepWertungen.headOption.map(_.wertung))
    }
    (durchgangPath, geraet, step, wertungForStep.get)
  }

  "WertungenRoutes" should {
    "return root programme list" in {
      HttpRequest(method = GET, uri = "/api/programm") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[String] should include("name")
      }
    }

    "return athlete wertungen for an existing athlete" in {
      HttpRequest(method = GET, uri = s"/api/athlet/${testWettkampf.uuid.get}/${sampleWertung.athletId}") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[String] should include(sampleWertung.athletId.toString)
      }
    }

    "return durchgang names for an existing competition" in {
      HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return not found for unknown competition on durchgang endpoint" in {
      HttpRequest(method = GET, uri = s"/api/durchgang/${UUID.randomUUID()}") ~> withRoutes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }

    "return geraete list for an existing competition" in {
      HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}/geraete") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return conflict for malformed durchgang segments" in {
      HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}/a/b/c/d") ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "return disziplin list for a specific durchgang (1-segment path)" in {
      val (durchgangPath, _, _, _) = discoverStepContext()
      HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[String].parseJson.convertTo[List[Disziplin]].nonEmpty.shouldBe(true)
      }
    }

    "return step list for a specific durchgang/geraet (2-segment path)" in {
      val (durchgangPath, geraet, _, _) = discoverStepContext()
      HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath/$geraet") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[String].parseJson.convertTo[List[Int]].nonEmpty.shouldBe(true)
      }
    }

    "return step details for a discovered durchgang/geraet/step combination" in {
      val (durchgangPath, geraet, step, _) = discoverStepContext()
      HttpRequest(method = GET, uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath/$geraet/$step") ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[String].parseJson.convertTo[List[WertungContainer]].nonEmpty.shouldBe(true)
      }
    }

    "reject validate without jwt" in {
      HttpRequest(
        method = PUT,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/validate",
        entity = wertungEntity(sampleWertung)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject validate with mismatching jwt" in {
      HttpRequest(
        method = PUT,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/validate",
        entity = wertungEntity(sampleWertung.copy(noteE = Some(BigDecimal(8.5))))
      ).addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "accept validate with matching jwt" in {
      HttpRequest(
        method = PUT,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/validate",
        entity = wertungEntity(sampleWertung.copy(noteE = Some(BigDecimal(8.5))))
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "reject music aquire without jwt" in {
      HttpRequest(
        method = PUT,
        uri = s"/api/music/${testWettkampf.uuid.get}/aquire",
        entity = wertungEntity(sampleWertung)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "accept music aquire with matching jwt" in {
      HttpRequest(
        method = PUT,
        uri = s"/api/music/${testWettkampf.uuid.get}/aquire",
        entity = wertungEntity(sampleWertung)
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "reject music aquire with mismatching jwt" in {
      HttpRequest(
        method = PUT,
        uri = s"/api/music/${testWettkampf.uuid.get}/aquire",
        entity = wertungEntity(sampleWertung)
      ).addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    Seq("release", "start", "stop").foreach { action =>
      s"reject music $action without jwt" in {
        HttpRequest(
          method = PUT,
          uri = s"/api/music/${testWettkampf.uuid.get}/$action",
          entity = wertungEntity(sampleWertung)
        ) ~> withRoutes ~> check {
          status should ===(StatusCodes.Unauthorized)
        }
      }

      s"accept music $action with matching jwt" in {
        HttpRequest(
          method = PUT,
          uri = s"/api/music/${testWettkampf.uuid.get}/$action",
          entity = wertungEntity(sampleWertung)
        ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
          status should ===(StatusCodes.OK)
        }
      }

      s"reject music $action with mismatching jwt" in {
        HttpRequest(
          method = PUT,
          uri = s"/api/music/${testWettkampf.uuid.get}/$action",
          entity = wertungEntity(sampleWertung)
        ).addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
          status should ===(StatusCodes.Unauthorized)
        }
      }
    }

    "reject durchgang step update without jwt" in {
      val (durchgangPath, geraet, step, stepWertung) = discoverStepContext()
      HttpRequest(
        method = PUT,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath/$geraet/$step",
        entity = wertungEntity(stepWertung.copy(noteE = Some(BigDecimal(8.4))))
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject durchgang step update with mismatching jwt" in {
      val (durchgangPath, geraet, step, stepWertung) = discoverStepContext()
      HttpRequest(
        method = PUT,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath/$geraet/$step",
        entity = wertungEntity(stepWertung.copy(noteE = Some(BigDecimal(8.6))))
      ).addHeader(jwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "accept durchgang step update with matching jwt" in {
      val (durchgangPath, geraet, step, stepWertung) = discoverStepContext()
      HttpRequest(
        method = PUT,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath/$geraet/$step",
        entity = wertungEntity(stepWertung.copy(noteE = Some(BigDecimal(8.7))))
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "accept durchgang step update and return json body reflecting domain response" in {
      val (durchgangPath, geraet, step, stepWertung) = discoverStepContext()
      HttpRequest(
        method = PUT,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/$durchgangPath/$geraet/$step",
        entity = wertungEntity(stepWertung.copy(noteE = Some(BigDecimal("7.65"))))
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        val body = responseAs[String]
        // returns either a WertungContainer (if durchgang is open) or a MessageAck (if nicht freigegeben)
        body.nonEmpty.shouldBe(true)
        body should include("\"type\":")
      }
    }

    "reject finish without jwt" in {
      val (durchgangPath, geraet, step, _) = discoverStepContext()
      val fd = FinishDurchgangStation(testWettkampf.uuid.get, durchgangPath, geraet, step)
      HttpRequest(
        method = POST,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/finish",
        entity = HttpEntity(ContentTypes.`application/json`, finishDurchgangStationFormat.write(fd).compactPrint)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "accept finish with matching jwt" in {
      val (durchgangPath, geraet, step, _) = discoverStepContext()
      val fd = FinishDurchgangStation(testWettkampf.uuid.get, durchgangPath, geraet, step)
      HttpRequest(
        method = POST,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/finish",
        entity = HttpEntity(ContentTypes.`application/json`, finishDurchgangStationFormat.write(fd).compactPrint)
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "return conflict for authenticated put with malformed durchgang segments" in {
      HttpRequest(
        method = PUT,
        uri = s"/api/durchgang/${testWettkampf.uuid.get}/malformed/path",
        entity = wertungEntity(sampleWertung)
      ).addHeader(jwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }
  }
}


