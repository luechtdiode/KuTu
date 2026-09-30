package ch.seidel.kutu.http

import ch.seidel.kutu.Config.jwtAuthorizationKey
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.{Terms, TermsInfo}
import org.apache.pekko.http.scaladsl.model.HttpMethods.GET
import org.apache.pekko.http.scaladsl.model.headers.`Cache-Control`
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpRequest, StatusCodes}
import org.scalatest.OptionValues
import spray.json.*

class TermsRoutesSpec extends KuTuBaseSpec with OptionValues {

  private def routes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private val termsPath = "/api/terms"

  "the public terms endpoint" should {

    "serve the terms without a token" in {
      HttpRequest(GET, termsPath) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[String].parseJson.convertTo[TermsInfo].version shouldBe Terms.version
      }
    }

    "serve exactly what the shared terms document contains" in {
      HttpRequest(GET, termsPath) ~> routes ~> check {
        val info = responseAs[String].parseJson.convertTo[TermsInfo]
        info.version shouldBe Terms.version
        info.stand shouldBe Terms.stand
        info.title shouldBe Terms.title
        info.blocks.map(block => (block.kind, block.text)) shouldBe
          Terms.blocks.map(block => (block.kind.toString, block.text))
      }
    }

    "serve a renderable document with a title, sections and paragraphs" in {
      HttpRequest(GET, termsPath) ~> routes ~> check {
        val info = responseAs[String].parseJson.convertTo[TermsInfo]
        info.title should include("Nutzungsbedingungen")
        info.blocks.count(_.kind == "Title") shouldBe 1
        info.blocks.count(_.kind == "Heading") should be >= 1
        info.blocks.count(_.kind == "Paragraph") should be >= 1
        info.blocks.map(_.text).mkString(" ") should include("Haftungsausschluss")
      }
    }

    "not leak markdown markup" in {
      HttpRequest(GET, termsPath) ~> routes ~> check {
        val info = responseAs[String].parseJson.convertTo[TermsInfo]
        info.blocks.map(_.text).mkString(" ") should not include "#"
      }
    }

    "be cacheable by the browser" in {
      HttpRequest(GET, termsPath) ~> routes ~> check {
        header[`Cache-Control`].value.toString should include("public")
      }
    }

    "serve a client with a stale token from localStorage" in {
      // Der Web-Client hängt den x-access-token an jede Anfrage, auch an die öffentliche Seite.
      // Ein unlesbarer Token darf dort keinen Serverfehler auslösen.
      HttpRequest(GET, termsPath).addHeader(RawHeader(jwtAuthorizationKey, "null")) ~> routes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "serve a client with an unreadable token" in {
      HttpRequest(GET, termsPath).addHeader(RawHeader(jwtAuthorizationKey, "a.b.c")) ~> routes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "not answer on a trailing slash or sub path" in {
      HttpRequest(GET, s"$termsPath/") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }
  }
}
