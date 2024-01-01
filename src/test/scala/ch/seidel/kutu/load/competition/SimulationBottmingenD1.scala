package ch.seidel.kutu.load.competition

import io.gatling.core.Predef.{constantConcurrentUsers, exec, _}
import io.gatling.http.Predef._

import scala.concurrent.duration._

class SimulationBottmingenD1 extends Simulation {
  // mws-01
  val jwtToken = "eyJhbGciOiJIUzUxMiIsImN0eSI6ImFwcGxpY2F0aW9uL2pzb24iLCJ0eXAiOiJKV1QifQ.eyJ1c2VyIjoiNzJmODI0MjMtNjVkYS00NWM5LTk5YTctYjNmMTk3MTJmNjI4IiwiZXhwaXJlZEF0S2V5IjoxNjg3MTI0NzU5NjkwfQ.IjzouXrDuDRsvhafPSCgcGAXlINZhAzgoieQ0Obamb0b8oG_R-6T6wMCtOUV8o84xmh5D7Cpv3_Alyiv-bj1fQ"
  val competition = "72f82423-65da-45c9-99a7-b3f19712f628"

  val originBaseUrl = "https://kutuapp-test.sharevic.net"//,
  //val originBaseUrl = "http://mars.stargate:30134" //https://test-kutuapp.sharevic.net" //,"http://pluto:5757"//, "https://kutuapp.sharevic.net" //,"https://kutuapp.sharevic.net"//,"http://mws-01:5757"//,
  // val originBaseUrl = "http://mws-01:5757"//, "https://kutuapp.sharevic.net"//,"http://mws-01:5757"//,
  //val originBaseUrl = "https://kutuapp.sharevic.net"

  val httpProtocol = http
    .baseUrl(originBaseUrl)
    .wsBaseUrl(originBaseUrl.replace("http", "ws")).wsReconnect.wsMaxReconnects(10)
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
      val list = session("durchgaenge").as[Vector[Any]]
      val listIdx = durchgangListIdx.getOrElse(competition, 0)
      val durchgangOriginal = list(listIdx).toString
      val randomEntry = encodeURIComponent(durchgangOriginal)
      durchgangListIdx = durchgangListIdx.updated(competition, if (listIdx < list.size - 1) listIdx + 1 else 0)
      println(s"random choosed durchgang: $randomEntry")
      session.set("durchgang", randomEntry).set("durchgangOriginal", durchgangOriginal)
    }

    def chooseGeraet(session: Session) = {
      val list = session("geraete").as[Vector[Map[String, Int]]].toList
      val listIdx = geraetListIdx.getOrElse(session("durchgang").as[String], 0)
      val randomEntry = list(listIdx)("id")
      geraetListIdx = geraetListIdx.updated(session("durchgang").as[String], if (listIdx < list.size - 1) listIdx + 1 else 0)
      println(s"random choosed geraet: $randomEntry")
      session.set("geraet", randomEntry.toString)
    }

    val connectWSUserToDurchgang = ws("openSocketDG")
        .wsName("${sessionDgUser}")
        .connect("/api/durchgang/" + competition + "/${durchgang}/ws?clientid=${sessionUserId}")
        .onConnected(
          exec(ws("sendMessage").wsName("${sessionDgUser}")
            .sendText("keepAlive")
            .await(5 seconds)(ws.checkTextMessage("check1")
              .check(regex("Connection established(.*)").saveAs("wsgreetingmessage"))
              .silent)
          )
        )
    val connectWSUserToAll = ws("openSocketAll")
        .wsName("${sessionDgUser}-all")
        .connect("/api/durchgang/" + competition + "/all/ws?clientid=${sessionUserId}")
        .onConnected(
          exec(ws("sendMessage").wsName("${sessionDgUser}-all")
            .sendText("keepAlive")
            .await(5 seconds)(ws.checkTextMessage("check1")
              .check(regex("Connection established(.*)").saveAs("wsgreetingmessage"))
              .silent)
          )
        )

    val closeWSUserFromDurchgang = ws("closeConnectionDG").wsName("${sessionDgUser}").close
    val closeWSUserFromAll = ws("closeConnectionAll").wsName("${sessionDgUser}-all").close

    val getSteps = {
      exec(http("get steps")
        .get("/api/durchgang/" + competition + "/${durchgang}/${geraet}")
        .check(
          jsonPath("$").exists,
          jsonPath("$").ofType[Seq[Any]].find.saveAs("steps")))
    }

    def chooseDurchgangWSConnection(session: Session) = {
      val dg = session("durchgang").as[String]
      val sessionDgUser = s"${dg}-${session.userId}"
      println(s"ws connection-key: $sessionDgUser")
      session
        .set("sessionDgUser", sessionDgUser)
        .set("sessionUserId", session.userId)
    }

    val commonDGInitializer = exec(session => {
      val dgSession = chooseDurchgang(session)
      val dgwsSession = chooseDurchgangWSConnection(dgSession)
      val gearSession = chooseGeraet(dgwsSession)
      gearSession
    })

    val diveToWertungen = commonDGInitializer
      .exec(connectWSUserToAll)
      .exec(getSteps)
      .foreach("${steps}", "step") {
        exec(http("get Wertungen")
          .get(s"/api/durchgang/$competition/${"${durchgang}"}/${"${geraet}"}/${"${step}"}")
          .check(
            jsonPath("$").exists,
            jsonPath("$").ofType[Seq[Any]].find))
          //.pause(5 minutes, 10 minutes)
          .rendezVous(10)
      }
      // implicit closed by session ending .exec(closeWSUserFromAll)
      // implicit closed by session ending .exec(closeWSUserFromDurchgang)

    val collectWertungen = commonDGInitializer
        .exec(http(s"start durchgang")
          .post(s"/api/competition/$competition/start")
          .body(StringBody(s"""{"type":"StartDurchgangStation","wettkampfUUID":"$competition","durchgang":"${"${durchgangOriginal}"}"}""")))
        .exec(connectWSUserToDurchgang)
        .exec(getSteps)
        .foreach("${steps}", "step") {
          exec(http("get Wertungen")
            .get(s"/api/durchgang/$competition/${"${durchgang}"}/${"${geraet}"}/${"${step}"}")
            .check(
              jsonPath("$").exists,
              jsonPath("$").ofType[Seq[Any]].find.saveAs("wertungen")))
            .pause(1 minutes, 3 minutes)
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
            .rendezVous(12)
            .exec(http("finish step")
              .post(s"/api/competition/$competition/finishedStep")
              .body(StringBody(s"""{"type":"FinishDurchgangStep","wettkampfUUID":"$competition"}"""))
              .silent
            )
        }
        .pause(20 seconds, 30 seconds)
        // implicit closed by session ending .exec(closeWSUserFromDurchgang)
  }

  val scnVisitor = scenario("Visitor")
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
    .exec(BrowseResults.loadAndSaveDurchgaenge)
    .exec(BrowseResults.loadAndSaveGeraete)
    .exec(BrowseResults.diveToWertungen)
    .exec(BrowseResults.closeWSUserFromAll)


  // https://mwclearning.com/?p=1678
  // https://github.com/llatinov/sample-performance-with-gatling/
  // https://automationrhapsody.com/performance-testing-with-gatling-integration-with-maven/
  val scnJudge = scenario("Judge")
    .exec(BrowseResults.loadAndSaveDurchgaenge)
    .exec(BrowseResults.loadAndSaveGeraete)
    .exec(BrowseResults.collectWertungen)
    .exec(BrowseResults.closeWSUserFromDurchgang)

  setUp(
    scnVisitor
      .inject(
        rampConcurrentUsers(100) to (500) during (10 minutes),
        constantConcurrentUsers(500) during(40 minutes),
        rampConcurrentUsers(500) to (300) during (10 minutes),
//        constantConcurrentUsers(150) during (10 hours)
        )
      .throttle(
        reachRps(30) in (10 seconds),
        holdFor(1 hours)
      )
      ,
    scnJudge
      .inject(
        //        heavisideUsers(70) during (60 seconds)
        //      constantUsersPerSec(1) during (15 seconds) randomized,
        //      rampUsers(100) during (15 seconds),
        //      rampUsersPerSec(2) to 8 during (5 minutes) randomized,
//                constantConcurrentUsers(12) during (60 minutes),
        rampConcurrentUsers(4) to 64 during (20 minutes)
        , rampConcurrentUsers(64) to 12 during (20 minutes)
        , constantConcurrentUsers(12) during (20 minutes)
        //        constantConcurrentUsers(8) during (50 minutes),
        //        constantConcurrentUsers(12) during (10 minutes),
        //        heavisideUsers(20) during (60 seconds)
      )
      .throttle(
        reachRps(5) in (3 minutes),
        holdFor(1 hour)
      )
  ).protocols(httpProtocol)
}