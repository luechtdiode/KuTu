package ch.seidel.kutu.domain

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import ch.seidel.kutu.http.ApiService
import org.scalatest.junit.JUnitRunner
import ch.seidel.kutu.base.KuTuBaseSpec

@RunWith(classOf[JUnitRunner])
class KuTuWettkampfCollectResultsSpec extends KuTuBaseSpec {
  val testwettkampf = insertGeTuWettkampf("TestGetuWK")
  
  "wettkampf" should {

    "return no competitions with empty databse (GET /events)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/api/competition")

      request ~> allroutes(x => x) ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""[]""")
      }
      
    }
  }

}