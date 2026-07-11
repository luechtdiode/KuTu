package ch.seidel.kutu.http

import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.*
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.HttpMethods.{GET, POST, PUT}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.util.UUID
import scala.compiletime.uninitialized

class DurchgangRoutesSpec extends KuTuBaseSpec {
  private var testWettkampf: Wettkampf = uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()
    testWettkampf = insertGeTuWettkampf("DurchgangRoutesTestWK", 2)
    makeEinteilung(testWettkampf)
  }

  private def withRoutes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private def adminJwtFor(userId: String): RawHeader = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays, isAdmin = true)
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))
  }

  private val emptySuggestionBody: String = {
    val req = RiegeSuggestionRequest()
    riegeSuggestionRequestFormat.write(req).compactPrint
  }

  /** Returns all simple durchgang names (via the duration endpoint). */
  private def discoverDurchgaenge: Seq[String] = {
    var result = Seq.empty[String]
    HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege/duration")
      .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
      val items = responseAs[String].parseJson.convertTo[List[DurchgangDurationItem]]
      result = items.map(_.name)
    }
    result
  }

  "DurchgangRoutes" should {

    // ── GET /api/competition/{uuid}/riege/duration (initial state) ──────────

    "return duration data after einteilung" in {
      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege/duration")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        val items = responseAs[String].parseJson.convertTo[List[DurchgangDurationItem]]
        items should not be empty
      }
    }

    // ── PUT /api/competition/{uuid}/riege/renamedg ──────────────────────────

    "reject renamedg without JWT" in {
      val dg = discoverDurchgaenge.headOption.getOrElse("Durchgang 1")
      val req = UpdateDurchgangRequest(oldTitle = dg, newTitle = "RenamedDG")
      val body = updateDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/renamedg",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject renamedg with a JWT for a different UUID" in {
      val dg = discoverDurchgaenge.headOption.getOrElse("Durchgang 1")
      val req = UpdateDurchgangRequest(oldTitle = dg, newTitle = "RenamedDG")
      val body = updateDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/renamedg",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ).addHeader(adminJwtFor(UUID.randomUUID().toString)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "rename a durchgang with valid JWT" in {
      val dg = discoverDurchgaenge.headOption.getOrElse("Durchgang 1")
      val req = UpdateDurchgangRequest(oldTitle = dg, newTitle = "RenamedDG")
      val body = updateDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/renamedg",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        val resp = responseAs[String].parseJson.asJsObject()
        resp.fields("status") should ===(JsString("ok"))
      }
      // Verify: the old name should no longer appear in duration list
      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege/duration")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        val items = responseAs[String].parseJson.convertTo[List[DurchgangDurationItem]]
        items.map(_.name) should not contain dg
        items.map(_.name) should contain("RenamedDG")
      }
    }

    // ── PUT /api/competition/{uuid}/riege/mergedg ───────────────────────────

    "reject mergedg without JWT" in {
      val dgs = discoverDurchgaenge
      val req = MergeDurchgangRequest(durchgangNames = dgs.take(2).toSet, targetName = "MergedDG")
      val body = mergeDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/mergedg",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "merge two durchgange with valid JWT" in {
      // First regenerate to have a clean state
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/aggregate",
        entity = HttpEntity(ContentTypes.`application/json`,
          groupDurchgangRequestFormat.write(GroupDurchgangRequest(Set("MergedDG"), "Abteilung Test")).compactPrint)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {}

      val dgs = discoverDurchgaenge
      val toMerge = dgs.filterNot(_.startsWith("Abteilung")).take(2)
      toMerge.size should be >= 1

      val req = MergeDurchgangRequest(durchgangNames = toMerge.toSet, targetName = "MergedDG")
      val body = mergeDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/mergedg",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    // ── PUT /api/competition/{uuid}/riege/aggregate ─────────────────────────

    "reject aggregate without JWT" in {
      val req = GroupDurchgangRequest(durchgangNames = Set("Dg1", "Dg2"), groupTitle = "Abteilung 1 (Tu)")
      val body = groupDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/aggregate",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "aggregate two durchgange into a group with valid JWT" in {
      // Regenerate to have clean state
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/riege/generate",
        entity = HttpEntity(ContentTypes.`application/json`, emptySuggestionBody)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {}

      val dgs = discoverDurchgaenge
      val toAggregate = dgs.filterNot(_.startsWith("Abteilung")).take(2)
      toAggregate.size should be >= 1

      val req = GroupDurchgangRequest(durchgangNames = toAggregate.toSet, groupTitle = "Abteilung New (Tu)")
      val body = groupDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/aggregate",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
      // Verify: the group title should appear in the duration list as title
      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege/duration")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        val items = responseAs[String].parseJson.convertTo[List[DurchgangDurationItem]]
        items.map(_.title) should contain("Abteilung New (Tu)")
      }
    }

    // ── PUT /api/competition/{uuid}/riege/ungroup ───────────────────────────

    "reject ungroup without JWT" in {
      val req = UngroupDurchgangRequest(durchgangNames = Set("Abteilung 1 (Tu)"))
      val body = ungroupDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/ungroup",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "ungroup a grouped durchgang with valid JWT" in {
      // Regenerate to have clean state, then aggregate
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/riege/generate",
        entity = HttpEntity(ContentTypes.`application/json`, emptySuggestionBody)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {}

      val dgs = discoverDurchgaenge
      val toAggregate = dgs.filterNot(_.startsWith("Abteilung")).take(2)
      if toAggregate.nonEmpty then {
        // First aggregate them
        val aggReq = GroupDurchgangRequest(durchgangNames = toAggregate.toSet, groupTitle = "Abteilung ToUngroup (Tu)")
        HttpRequest(
          PUT,
          s"/api/competition/${testWettkampf.uuid.get}/riege/aggregate",
          entity = HttpEntity(ContentTypes.`application/json`,
            groupDurchgangRequestFormat.write(aggReq).compactPrint)
        ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
          status should ===(StatusCodes.OK)
        }

        // Now ungroup
        val ungroupReq = UngroupDurchgangRequest(durchgangNames = Set("Abteilung ToUngroup (Tu)"))
        HttpRequest(
          PUT,
          s"/api/competition/${testWettkampf.uuid.get}/riege/ungroup",
          entity = HttpEntity(ContentTypes.`application/json`,
            ungroupDurchgangRequestFormat.write(ungroupReq).compactPrint)
        ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
          status should ===(StatusCodes.OK)
        }
      }
    }

    // ── PUT /api/competition/{uuid}/riege/movegroup ─────────────────────────

    "reject movegroup without JWT" in {
      val req = GroupDurchgangRequest(durchgangNames = Set("Dg1"), groupTitle = "Abteilung 2 (Tu)")
      val body = groupDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/movegroup",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "move a durchgang to a group with valid JWT" in {
      // Regenerate to have clean state
      HttpRequest(
        POST,
        s"/api/competition/${testWettkampf.uuid.get}/riege/generate",
        entity = HttpEntity(ContentTypes.`application/json`, emptySuggestionBody)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {}

      val dgs = discoverDurchgaenge
      val plainDg = dgs.find(!_.startsWith("Abteilung")).getOrElse(dgs.head)
      val req = GroupDurchgangRequest(durchgangNames = Set(plainDg), groupTitle = "Abteilung Target (Tu)")
      val body = groupDurchgangRequestFormat.write(req).compactPrint
      HttpRequest(
        PUT,
        s"/api/competition/${testWettkampf.uuid.get}/riege/movegroup",
        entity = HttpEntity(ContentTypes.`application/json`, body)
      ).addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
      // Verify: the group title should now appear in the duration list as title
      HttpRequest(GET, s"/api/competition/${testWettkampf.uuid.get}/riege/duration")
        .addHeader(adminJwtFor(testWettkampf.uuid.get)) ~> withRoutes ~> check {
        val items = responseAs[String].parseJson.convertTo[List[DurchgangDurationItem]]
        items.map(_.title) should contain("Abteilung Target (Tu)")
      }
    }
  }
}
