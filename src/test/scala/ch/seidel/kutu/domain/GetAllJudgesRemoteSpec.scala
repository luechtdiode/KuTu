package ch.seidel.kutu.domain

import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.base.KuTuBaseSpec
import org.apache.pekko.http.scaladsl.model.HttpMethods.GET
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{HttpRequest, StatusCodes}
import spray.json.DefaultJsonProtocol.{listFormat, tuple2Format}

/**
 * Integration tests for the backend route GET /api/registrations/:wkuuid/judges,
 * which is consumed by getAllJudgesRemote in RegistrationRoutes.
 *
 * Scenarios:
 *  - empty    : competition with a registration but no judges → empty list
 *  - 2 entries: 2 registrations each with 1 judge → list of 2 (Registration, List[JudgeRegistration]) pairs
 *  - error    : request without JWT → StatusCodes.Unauthorized
 */
class GetAllJudgesRemoteSpec extends KuTuBaseSpec {

  // One isolated competition per scenario to avoid cross-test state pollution.
  // anzvereine=0 skips athlete/club setup – we only need the competition UUID.
  val wkEmpty: Wettkampf = insertGeTuWettkampf("JudgesRemoteEmpty", 0)
  val wkTwo:   Wettkampf = insertGeTuWettkampf("JudgesRemoteTwo",   0)
  val wkError: Wettkampf = insertGeTuWettkampf("JudgesRemoteError", 0)

  /** Build a valid JWT whose user-identity is the registration id. */
  private def jwtFor(reg: Registration): RawHeader = {
    val claims = setClaims(reg.id.toString, jwtTokenExpiryPeriodInDays)
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))
  }

  /** Create and persist a Registration for the given competition. */
  private def newRegistration(wk: Wettkampf, vereinname: String): Registration =
    createRegistration(NewRegistration(
      wk.id,
      vereinname, s"${vereinname}Verband",
      "Verantwortlich", "Person",
      "+41791234567", s"$vereinname@test.com", "SecretPW1!"))

  /**
   * Persist a JudgeRegistration directly via the DB service (no REST overhead).
   * Uses names proven to pass normalisation in RegistrationRestSpec.
   */
  private def addJudge(reg: Registration, name: String, vorname: String): JudgeRegistration =
    createJudgeRegistration(
      JudgeRegistration(0, reg.id, "M", name, vorname,
        "+41791234567", s"$vorname.$name@test.com", "Comment", 0))

  private def routes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  "GET /api/registrations/:wkuuid/judges" should {

    // ── Scenario 1 : empty ───────────────────────────────────────────────────
    "return an empty list when the competition has registrations but no judges" in {
      val reg = newRegistration(wkEmpty, "EmptyClub")

      HttpRequest(GET, s"/api/registrations/${wkEmpty.uuid.get}/judges")
        .addHeader(jwtFor(reg)) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val result = entityAs[List[(Registration, List[JudgeRegistration])]]
        result shouldBe empty
      }
    }

    // ── Scenario 2 : 2 entries ───────────────────────────────────────────────
    "return 2 entries when 2 registrations each have exactly one judge" in {
      val reg1 = newRegistration(wkTwo, "TwoClub1")
      val reg2 = newRegistration(wkTwo, "TwoClub2")

      // "Schneider"/"Markus" and "Tester"/"Test" are known-good through RegistrationRestSpec
      val judge1 = addJudge(reg1, "Schneider", "Markus")
      val judge2 = addJudge(reg2, "Tester",    "Test")

      HttpRequest(GET, s"/api/registrations/${wkTwo.uuid.get}/judges")
        .addHeader(jwtFor(reg1)) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val result = entityAs[List[(Registration, List[JudgeRegistration])]]

        // one entry per registration that has at least one judge
        result should have size 2

        // each entry carries exactly one judge
        val allJudges = result.flatMap(_._2)
        allJudges should have size 2
        allJudges.map(_.name) should contain(judge1.name)
        allJudges.map(_.name) should contain(judge2.name)
      }
    }

    // ── Scenario 3 : error / unauthorized ──────────────────────────────────
    "return Unauthorized when no JWT header is present" in {
      HttpRequest(GET, s"/api/registrations/${wkError.uuid.get}/judges") ~>
        routes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }
  }
}

