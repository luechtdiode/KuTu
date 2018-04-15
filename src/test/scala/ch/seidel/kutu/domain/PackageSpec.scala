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
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class PackageSpec extends KuTuBaseSpec {
  "toDurationFormat" should {
     //Duration(1, TimeUnit.DAYS) + Duration(15, TimeUnit.HOURS)
    "seconds" in {
      val millis = 3 * 1000
      assert("3s" == toDurationFormat(1L, 1L + millis))
    }
    "muinutes and seconds" in {
      val millis = 3 * 1000 + 59 * 60 * 1000
      assert("59m, 3s" == toDurationFormat(1L, 1L + millis))
    }
    "hours, minutes and seconds" in {
      val millis = 3 * 1000 + 59 * 60 * 1000 + 23 * 3600 * 1000
      assert("23h, 59m, 3s" == toDurationFormat(1L, 1L + millis))
    }
    "days, hours, minutes and seconds" in {
      val millis = 3 * 1000 + 59 * 60 * 1000 + 23 * 3600 * 1000  +  1 * 24 * 3600 * 1000
      assert("1d, 23h, 59m, 3s" == toDurationFormat(1L, 1L + millis))
    }
  }  
}