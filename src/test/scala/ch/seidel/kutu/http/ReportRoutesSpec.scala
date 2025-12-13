package ch.seidel.kutu.http

import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.{Wettkampf, encodeURIParam}
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, StatusCodes}

import java.util.UUID

/**
 * Test suite for ReportRoutes
 *
 * This demonstrates how to test HTTP routes using ScalatestRouteTest
 */
class ReportRoutesSpec extends KuTuBaseSpec {
  var testWettkampf: Wettkampf = _

  override def beforeAll(): Unit = {
    super.beforeAll()

    // Setup test data
    testWettkampf = insertGeTuWettkampf("TestReportWK", 2)
    makeEinteilung(testWettkampf)
  }

  "ReportRoutes" should {

    "return abused clients list in HTML format" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = "/report/abused"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/html(UTF-8)`)
        // The response should contain HTML
        responseAs[String] should include("<")
      }
    }

    "return 404 for non-existing competition ID" in {
      val nonExistingUUID = UUID.randomUUID()
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/$nonExistingUUID/startlist"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }

    "return startlist as HTML when html parameter is provided" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?html=true"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/html(UTF-8)`)
        // The response should contain HTML
        responseAs[String] should include("<")
      }
    }

    "return startlist as JSON when html parameter is not provided" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "return startlist grouped by verein when gr=verein parameter is provided" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?html=true&gr=verein"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/html(UTF-8)`)
      }
    }

    "return startlist grouped by durchgang when gr=durchgang parameter is provided" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?html=true&gr=durchgang"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/html(UTF-8)`)
      }
    }

    "filter startlist by query parameter" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?q=Verein-1"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        val response = responseAs[String]
        // The response should contain filtered results
        response should include("Verein-1")
      }
    }

    "support combined html and query parameters" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?html=true&q=Athlet"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/html(UTF-8)`)
      }
    }

    "support all parameters combined" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?html=true&q=Verein&gr=verein"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/html(UTF-8)`)
      }
    }
  }

  "filterMatchingCandidatesToQuery" should {
    // Note: This private method can't be tested directly, but we can test it indirectly
    // through the HTTP endpoints as shown above

    "filter by query string through startlist endpoint" in {
      // Test with specific athlete name
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?q=Athlet-1-1"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        val response = responseAs[String]
        response should include("Athlet-1-1")
      }
    }

    "filter by multiple query tokens" in {
      // Test with multiple search terms
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?q=" + encodeURIParam("Verein-1 Athlet-1-1")
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        val response = responseAs[String]
        // Should contain results matching both terms
        response should (include("Verein-1") and include("Athlet-1-1"))
      }
    }
  }
}

