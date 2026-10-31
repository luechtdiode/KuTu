package ch.seidel.kutu.http

import ch.seidel.kutu.base.KuTuBaseSpec
import org.apache.pekko.http.scaladsl.model.HttpMethods.POST
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import spray.json.*

class AltersklassenRoutesSpec extends KuTuBaseSpec {

  private def routes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private val previewPath = "/api/altersklassen/preview"

  private def previewRequest(formel: String): HttpRequest =
    HttpRequest(
      POST,
      previewPath,
      entity = HttpEntity(ContentTypes.`application/json`, AltersklassenPreviewRequest(formel).toJson.compactPrint)
    )

  private def preview(formel: String): AltersklassenPreviewResponse = {
    var result: AltersklassenPreviewResponse = null
    previewRequest(formel) ~> routes ~> check {
      status should ===(StatusCodes.OK)
      contentType should ===(ContentTypes.`application/json`)
      result = responseAs[String].parseJson.convertTo[AltersklassenPreviewResponse]
    }
    result
  }

  "the altersklassen preview endpoint" should {

    "be usable without a token" in {
      previewRequest("AK6,AK10") ~> routes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "create contiguous classes with an open end" in {
      val result = preview("AK6,AK10")
      result.valid shouldBe true
      result.labels shouldBe List("AK bis 5", "AK 6 bis 9", "AK ab 10")
    }

    "handle ranges and inherited labels" in {
      val result = preview("AK6-8,10,AK12-16/2")
      result.valid shouldBe true
      result.labels shouldBe List(
        "AK bis 5",
        "AK 6 bis 8",
        "AK 9",
        "AK 10 bis 11",
        "AK 12 bis 13",
        "AK 14 bis 15",
        "AK ab 16"
      )
    }

    "include qualifiers in the generated labels" in {
      val result = preview("AK(BS)9-10")
      result.valid shouldBe true
      result.labels shouldBe List("AK(BS) bis 8", "AK(BS) 9 bis 10", "AK(BS) ab 11")
    }

    "ignore tokens that cannot be parsed when other boundaries are valid" in {
      val result = preview("foo,AK6")
      result.valid shouldBe true
      result.labels shouldBe List("AK bis 5", "AK ab 6")
    }

    "treat an empty formula as valid without classes" in {
      val result = preview("")
      result.valid shouldBe true
      result.labels shouldBe empty
    }

    "reject a formula without any usable boundary" in {
      val result = preview("keine-klasse")
      result.valid shouldBe false
      result.labels shouldBe empty
    }

    "reject a malformed body" in {
      HttpRequest(
        POST,
        previewPath,
        entity = HttpEntity(ContentTypes.`application/json`, "{kein json")
      ) ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
      }
    }

    "reject an excessively long formula" in {
      previewRequest("A".repeat(AltersklassenPreview.MaxFormulaLength + 1)) ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
      }
    }

    "not answer on a trailing slash" in {
      previewRequest("AK6").withUri(org.apache.pekko.http.scaladsl.model.Uri(s"$previewPath/")) ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }
  }
}
