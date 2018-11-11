package ch.seidel.kutu.load.competition

import io.gatling.core.Predef.{constantConcurrentUsers, holdFor, _}
import io.gatling.http.Predef._

import scala.concurrent.duration._
//import io.gatling.core.check.extractor.jsonpath.JsonFilter._
//import io.gatling.core.feeder.SourceFeederBuilder

class SimulationBottmingenD1 extends Simulation {
  // mws-01
  //  val jwtToken = "eyJhbGciOiJIUzUxMiIsImN0eSI6ImFwcGxpY2F0aW9uL2pzb24iLCJ0eXAiOiJKV1QifQ.eyJ1c2VyIjoiOTkxMTVkZmItODE5Yi00OGFhLWFkYTUtOTkzN2I4MmVlYmQ0IiwiZXhwaXJlZEF0S2V5IjoxNTQyMDQ4NzY2NjM2fQ.8XYEceYnUIAQVHZVZKP6aF-_7hRNjB7jNFzIGq52CUpmVeBqHmi16W28XQci4tj-IkZmHDqFVzSaW8P3Q3W_vA"

  //kutuapp.shrevic.net
  val jwtToken = "eyJhbGciOiJIUzUxMiIsImN0eSI6ImFwcGxpY2F0aW9uL2pzb24iLCJ0eXAiOiJKV1QifQ.eyJ1c2VyIjoiOTkxMTVkZmItODE5Yi00OGFhLWFkYTUtOTkzN2I4MmVlYmQ0IiwiZXhwaXJlZEF0S2V5IjoxNTQyMDUxMTQxNjQ3fQ.aUPZCrxYryVy0jjACZOng-ONO1RdvRbwZJhDfIBYwx3QyriwnuYocpfBE8bvRh-D5tEqt2XYYgy-rdYMqRtE0A"

  val competition = "99115dfb-819b-48aa-ada5-9937b82eebd4"
  val originBaseUrl = "https://kutuapp.sharevic.net" //,"http://pluto:5757"//, "https://kutuapp.sharevic.net" //,"https://kutuapp.sharevic.net"//,"http://mws-01:5757"//,
  //  val originBaseUrl = "http://mws-01:5757"//, "https://kutuapp.sharevic.net"//,"http://mws-01:5757"//,

  val httpProtocol = http
    .baseUrl(originBaseUrl)
    .wsBaseUrl(originBaseUrl).wsReconnect.wsMaxReconnects(100)
    .inferHtmlResources()
    .doNotTrackHeader("1")
    .userAgentHeader("Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1")
    .headers(Map(
      "Accept" -> "application/json, text/plain, */*",
      "Upgrade-Insecure-Requests" -> "1",
      "Accept-Encoding" -> "gzip, deflate",
      "Accept-Language" -> "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7",
      //      "Connection" -> "keep-alive",
      "Content-Type" -> "application/json",
      "Origin" -> originBaseUrl,
      "x-access-token" -> jwtToken))

  val scnLanding = scenario("Landingpage")
    .exec(http("initial-access")
      .get("/")
      .resources(
        http("get font ionicons").get("/assets/fonts/ionicons.woff2?v=4.2.5"),
        http("get font roboto-regular").get("/assets/fonts/roboto-regular.woff2"),
        http("get font roboto-medium").get("/assets/fonts/roboto-medium.woff2"),
        http("get font roboto-bold").get("/assets/fonts/roboto-bold.woff2"),
        http("get competitions").get("/api/competition"),
        http("check jwt-token expired")
          .options("/api/isTokenExpired")))
    .pause(5 minutes)

  object BrowseResults {
    val encodeInvalidURIRegEx = "[,&.*+?/^${}()|\\[\\]\\\\]".r

    def encodeURIComponent(uri: String) = encodeInvalidURIRegEx.replaceAllIn(uri, "_")

    var durchgangListIdx = Map[String, Int]()
    var geraetListIdx = Map[String, Int]()
    val loadAndSaveDurchgaenge = http("get durchgaenge")
      .get(s"/api/durchgang/$competition")
      .check(
        jsonPath("$").exists,
        jsonPath("$").ofType[Seq[Any]].find.saveAs("durchgaenge"))

    val loadAndSaveGeraete = http("get geraete")
      .get(s"/api/durchgang/$competition/geraete")
      .check(
        jsonPath("$").exists,
        jsonPath("$").ofType[Seq[Any]].find.saveAs("geraete"))

    def chooseDurchgang(session: Session) = {
      val list = session("durchgaenge").as[Seq[Any]].toList
      val listIdx = durchgangListIdx.getOrElse(competition, 0)
      val durchgangOriginal = list(listIdx).toString
      val randomEntry = encodeURIComponent(durchgangOriginal)
      durchgangListIdx = durchgangListIdx.updated(competition, if (listIdx < list.size - 1) listIdx + 1 else 0)
      println(s"random choosed durchgang: $randomEntry")
      session.set("durchgang", randomEntry).set("durchgangOriginal", durchgangOriginal)
    }

    def chooseGeraet(session: Session) = {
      val list = session("geraete").as[Seq[Map[String, Int]]].toList
      val listIdx = geraetListIdx.getOrElse(session("durchgang").as[String], 0)
      val randomEntry = list(listIdx)("id")
      geraetListIdx = geraetListIdx.updated(session("durchgang").as[String], if (listIdx < list.size - 1) listIdx + 1 else 0)
      println(s"random choosed geraet: $randomEntry")
      session.set("geraet", randomEntry.toString)
    }

    val getSteps = {
      exec(http("get steps")
        .get("/api/durchgang/" + competition + "/${durchgang}/${geraet}")
        .check(
          jsonPath("$").exists,
          jsonPath("$").ofType[Seq[Any]].find.saveAs("steps")))
    }

    val diveToWertungen =
      exec(session => chooseDurchgang(session))
        .exec(session => chooseGeraet(session))
        .exec(getSteps)
        .exec(http(s"start durchgang")
          .post(s"/api/competition/$competition/start")
          .body(StringBody(s"""{"type":"StartDurchgangStation","wettkampfUUID":"$competition","durchgang":"${"${durchgangOriginal}"}"}""")))
        .foreach("${steps}", "step") {
          exec(http("get Wertungen")
            .get(s"/api/durchgang/$competition/${"${durchgang}"}/${"${geraet}"}/${"${step}"}")
            .check(
              jsonPath("$").exists,
              jsonPath("$").ofType[Seq[Any]].find.saveAs("wertungen")))
            .pause(2 seconds, 10 seconds)
            .foreach("${wertungen}", "wertung") {
              exec(http("save wertung")
                .put(s"/api/durchgang/$competition/${"${durchgang}"}/${"${geraet}"}/${"${step}"}")
                .body(StringBody("${wertung.wertung.jsonStringify()}"))
                .check(status.is(200))
              )
                .pause(5 seconds, 20 seconds)
              //          .exec(http("finish durchgangstation")
              //            .post(s"/api/durchgang/$competition/${"${durchgang}"}/finish")
              //            .body(StringBody(s"""{"type":"FinishDurchgangStation","wettkampfUUID":"$competition","durchgang:"${"${durchgangOriginal}"}","geraet":${"${geraet}"},"step":${"${step}"}}""")))
            }
        }
  }

  // https://mwclearning.com/?p=1678
  // https://github.com/llatinov/sample-performance-with-gatling/
  // https://automationrhapsody.com/performance-testing-with-gatling-integration-with-maven/
  val scnBrowseResultsPerDurchgangAndGeraet = scenario("BrowseResultsPerDurchgangAndGeraet")
    .exec(BrowseResults.loadAndSaveDurchgaenge)
    .exec(BrowseResults.loadAndSaveGeraete)
    .exec(BrowseResults.diveToWertungen)
  //    .pause(20 seconds, 30 seconds)

  setUp(
    scnLanding
      .inject(
        rampConcurrentUsers(20) to (150) during (2 minutes),
        //        constantConcurrentUsers(150) during(58 minutes),
        constantConcurrentUsers(150) during (4 hours))
      .throttle(
        reachRps(5) in (10 seconds),
        holdFor(4 hours)
      ),
    scnBrowseResultsPerDurchgangAndGeraet
      .inject(
        //        heavisideUsers(70) during (60 seconds)
        //      constantUsersPerSec(1) during (15 seconds) randomized,
        //      rampUsers(100) during (15 seconds),
        //      rampUsersPerSec(2) to 8 during (5 minutes) randomized,
        //        constantConcurrentUsers(12) during (60 minutes),
        rampConcurrentUsers(8) to 12 during (4 hours)
        //constantConcurrentUsers(10) during (4 hours)
        //        constantConcurrentUsers(8) during (50 minutes),
        //        constantConcurrentUsers(12) during (10 minutes),
        //      heavisideUsers(20) during (60 seconds)
      )
  ).protocols(httpProtocol)
}