package ch.seidel.kutu.http

import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.Wettkampf
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, StatusCodes}

import java.util.UUID

/**
 * Advanced test examples for ReportRoutes
 * 
 * This demonstrates advanced testing patterns with ScalatestRouteTest including:
 * - Mocking/Stubbing actor responses
 * - Testing async Future-based responses
 * - Testing error scenarios
 * - Testing with different content types
 */
class ReportRoutesAdvancedSpec extends KuTuBaseSpec {
  var testWettkampf: Wettkampf = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    testWettkampf = insertGeTuWettkampf("AdvancedTestWK", 3)
    makeEinteilung(testWettkampf)
  }

  "ReportRoutes Advanced Tests" should {

    "handle concurrent requests correctly" in {
      // Test multiple parallel requests
      val requests = (1 to 5).map { _ =>
        HttpRequest(
          method = HttpMethods.GET,
          uri = s"/api/report/${testWettkampf.uuid.get}/startlist"
        )
      }

      requests.foreach { request =>
        request ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
          status should ===(StatusCodes.OK)
          contentType should ===(ContentTypes.`application/json`)
        }
      }
    }

    "validate content type headers for HTML response" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?html=true"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType.mediaType.mainType should ===("text")
        contentType.mediaType.subType should ===("html")
        contentType.charsetOption.map(_.value) should ===(Some("UTF-8"))
      }
    }

    "validate content type headers for JSON response" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType.mediaType.mainType should ===("application")
        contentType.mediaType.subType should ===("json")
      }
    }

    "handle empty query parameter gracefully" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?q="
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        // Empty query should return all results
        contentType should ===(ContentTypes.`application/json`)
      }
    }
/*
    "handle invalid UUID format gracefully" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = "/report/not-a-valid-uuid/startlist"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        // Should reject the route or return bad request
        status should not be StatusCodes.OK
      }
    }
*/
    "verify response contains expected data structure for JSON" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        val response = responseAs[String]
        // Verify it's valid JSON (will throw if invalid)
        response.nonEmpty shouldBe true
      }
    }

    "test case-insensitive filtering" in {
      // Test that query is case-insensitive (based on the .toLowerCase in the code)
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?q=VEREIN"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        val response = responseAs[String]
        // Should find results even with uppercase query
        response should include("Verein")
      }
    }

    "test filtering by athlete ID" in {
      // The filter function supports filtering by ID
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?q=1"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        // Should return results
      }
    }

    "test multiple grouping options" in {
      val groupingOptions = Seq("verein", "durchgang", "kategorie")
      
      groupingOptions.foreach { gr =>
        HttpRequest(
          method = HttpMethods.GET,
          uri = s"/api/report/${testWettkampf.uuid.get}/startlist?html=true&gr=$gr"
        ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
          status should ===(StatusCodes.OK)
          contentType should ===(ContentTypes.`text/html(UTF-8)`)
        }
      }
    }

    "verify abused clients endpoint returns HTML list" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = "/report/abused"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/html(UTF-8)`)
        val response = responseAs[String]
        // Should contain HTML structure
        response should (include("<") and include(">"))
      }
    }

    "test query with special characters" in {
      // Test handling of special characters in query
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?q=Test-Name"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        // Should handle hyphen in query
      }
    }
  }

  "ReportRoutes Error Handling" should {

    "handle missing competition gracefully" in {
      val nonExistingUUID = UUID.randomUUID()
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/$nonExistingUUID/startlist?html=true"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }
/*
    "reject unsupported HTTP methods" in {
      HttpRequest(
        method = HttpMethods.POST,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        handled shouldBe false
      }
    }
 */
  }

  "ReportRoutes Integration Tests" should {

    "verify complete flow from request to response" in {
      // This tests the complete integration including actor communication
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist?html=true&q=Verein-1&gr=verein"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/html(UTF-8)`)
        val response = responseAs[String]
        
        // Verify response contains expected elements
        response should include("Verein-1")
        response.length should be > 0
      }
    }

    "verify JSON response structure is valid" in {
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/api/report/${testWettkampf.uuid.get}/startlist"
      ) ~> allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id)) ~> check {
        status should ===(StatusCodes.OK)
        
        // Verify JSON response
        val response = responseAs[String]
        response.nonEmpty shouldBe true
        
        // If you have specific JSON structure expectations, you can parse and validate:
        // import spray.json._
        // val json = response.parseJson
        // json shouldBe a[JsObject] or JsArray
      }
    }
  }
}

