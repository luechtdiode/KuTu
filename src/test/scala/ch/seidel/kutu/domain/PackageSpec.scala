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
  "GeTuWettkampf" should {
    "min" in {
      assert(GeTuWettkampf.calcEndnote(0d, -1d) == 0.00d)
    }
    "max" in {
      assert(GeTuWettkampf.calcEndnote(0d, 10.01d) == 10.00d)
    }
    "scale" in {
      assert(GeTuWettkampf.calcEndnote(0d, 8.123d) == 8.12d)
    }
  }
  "KuTuWettkampf" should {
    "min" in {
      assert(KuTuWettkampf.calcEndnote(0.1d, -1d) == 0.000d)
    }
    "max" in {
      assert(KuTuWettkampf.calcEndnote(0.5d, 30.01d) == 30.000d)
    }
    "scale" in {
      assert(KuTuWettkampf.calcEndnote(1.1d, 8.1234d) == 9.223d)
    }
  }
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
  
  "encapsulated titles" should {
    "match" in {
      val titles = Seq(
          "D1, K1-4 Tu & Ti"
          , "D1.TuTi"
          )
      titles.foreach(t => assert(t.matches(".*[\\s,\\.;!].*")))
      //assert("KH.KD".matches(".*[\\s,\\.;!].*"))
    }
  }
}