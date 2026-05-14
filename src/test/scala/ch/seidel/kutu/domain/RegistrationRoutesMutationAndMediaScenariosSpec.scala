package ch.seidel.kutu.domain

import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.base.KuTuBaseSpec
import org.apache.pekko.http.scaladsl.model.HttpMethods.{DELETE, GET, POST, PUT}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, Multipart, StatusCodes}
import org.apache.pekko.util.ByteString
import spray.json.*
import spray.json.DefaultJsonProtocol.listFormat

class RegistrationRoutesMutationAndMediaScenariosSpec extends KuTuBaseSpec {

  private def routes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private def jwtFor(userId: String): RawHeader = {
    val claims = setClaims(userId, jwtTokenExpiryPeriodInDays)
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey))
  }

  private def registrationJwt(reg: Registration): RawHeader = jwtFor(reg.id.toString)

  // Sync-admin endpoints compare authenticated userId against competition UUID string.
  private def syncAdminJwt(wk: Wettkampf): RawHeader = jwtFor(wk.uuid.get)

  private def jsonEntity[T: JsonWriter](value: T): HttpEntity.Strict =
    HttpEntity(ContentTypes.`application/json`, ByteString(value.toJson.compactPrint))

  private def newRegistration(wk: Wettkampf, name: String): Registration =
    createRegistration(NewRegistration(
      wk.id,
      name,
      s"$name-Verband",
      "RespName",
      "RespVorname",
      "+41790000000",
      s"$name@test.local",
      "SecretPW1!"
    ))

  "registration routes mutation scenarios" should {

    "update verein via sync-admin endpoint and reject registration token" in {
      val wk = insertGeTuWettkampf("ScenarioUpdateVerein", 0)
      val reg = newRegistration(wk, "ScenarioVereinClub")
      val vereinId = createVerein("SyncClub", Some("SyncVerband"))
      val updatedVerein = Verein(vereinId, "SyncClubUpdated", Some("SyncVerbandUpdated"))

      HttpRequest(PUT, s"/api/registrations/${wk.uuid.get}/verein", entity = jsonEntity(updatedVerein))
        .addHeader(syncAdminJwt(wk)) ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
      }

      HttpRequest(PUT, s"/api/registrations/${wk.uuid.get}/verein", entity = jsonEntity(updatedVerein.copy(name = "ShouldConflict")))
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "return InternalServerError for sync-admin athletes bulk update on SQLite" in {
      val wk = insertGeTuWettkampf("ScenarioUpdateAthletes", 0)
      val vereinId = createVerein("AthleteSyncClub", Some("AthleteSyncVerband"))
      val athlet = insertAthlete(Athlet(vereinId).copy(name = "OldName", vorname = "OldVorname", geschlecht = "M"))
      val athletView = loadAthleteView(athlet.id)
      val changed = athletView.copy(name = "UpdatedName")

      HttpRequest(PUT, s"/api/registrations/${wk.uuid.get}/athletes", entity = jsonEntity(List(changed)))
        .addHeader(syncAdminJwt(wk)) ~>
        routes ~> check {
        // Route uses SQL syntax unsupported by SQLite test DB in adjustOnlineRegistrations.
        status should ===(StatusCodes.InternalServerError)
      }
    }

    "reject athletes bulk update with registration token" in {
      val wk = insertGeTuWettkampf("ScenarioUpdateAthletesAuth", 0)
      val reg = newRegistration(wk, "AthleteSyncReg")
      val vereinId = createVerein("AthleteSyncClub", Some("AthleteSyncVerband"))
      val athlet = insertAthlete(Athlet(vereinId).copy(name = "OldName", vorname = "OldVorname", geschlecht = "M"))
      val athletView = loadAthleteView(athlet.id)
      val changed = athletView.copy(name = "UpdatedName")

      HttpRequest(PUT, s"/api/registrations/${wk.uuid.get}/athletes", entity = jsonEntity(List(changed)))
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

  }

  "registration routes copy and media scenarios" should {

    "copy athletes and judges from another competition without duplicates" in {
      val sourceWk = insertGeTuWettkampf("ScenarioCopySource", 0)
      val targetWk = insertGeTuWettkampf("ScenarioCopyTarget", 0)

      val sharedVereinId = createVerein("CopyClub", Some("CopyVerband"))

      val sourceReg0 = newRegistration(sourceWk, "CopySourceReg")
      val targetReg0 = newRegistration(targetWk, "CopyTargetReg")
      val sourceReg = updateRegistration(sourceReg0.copy(vereinId = Some(sharedVereinId)))
      val targetReg = updateRegistration(targetReg0.copy(vereinId = Some(sharedVereinId)))

      createAthletRegistration(AthletRegistration(
        0,
        sourceReg.id,
        None,
        "M",
        "Copy",
        "Athlete",
        "2010-05-05",
        20,
        0,
        None,
        None,
        None
      ))
      createJudgeRegistration(JudgeRegistration(
        0,
        sourceReg.id,
        "M",
        "Copy",
        "Judge",
        "+41790000001",
        "copy.judge@test.local",
        "Comment",
        0
      ))

      HttpRequest(PUT, s"/api/registrations/${targetWk.uuid.get}/${targetReg.id}/copyfrom", entity = jsonEntity(sourceWk))
        .addHeader(registrationJwt(targetReg)) ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
      }

      selectAthletRegistrations(targetReg.id).size should ===(1)
      selectJudgeRegistrations(targetReg.id).size should ===(1)

      // Re-run copy to assert duplicate protection in SQL 'not exists' conditions.
      HttpRequest(PUT, s"/api/registrations/${targetWk.uuid.get}/${targetReg.id}/copyfrom", entity = jsonEntity(sourceWk))
        .addHeader(registrationJwt(targetReg)) ~>
        routes ~> check {
        status should ===(StatusCodes.OK)
      }

      selectAthletRegistrations(targetReg.id).size should ===(1)
      selectJudgeRegistrations(targetReg.id).size should ===(1)
    }

    "return conflict on mediafile get/delete when no media is linked" in {
      val wk = insertGeTuWettkampf("ScenarioMediaNoFile", 0)
      val reg = newRegistration(wk, "MediaNoFileClub")
      val athReg = createAthletRegistration(AthletRegistration(
        0,
        reg.id,
        None,
        "M",
        "No",
        "Media",
        "2011-03-03",
        20,
        0,
        None,
        None,
        None
      ))

      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/${reg.id}/athletes/${athReg.id}/mediafile")
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.Conflict)
      }

      HttpRequest(DELETE, s"/api/registrations/${wk.uuid.get}/${reg.id}/athletes/${athReg.id}/mediafile")
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "reject mediafile upload when content type is not mp3" in {
      val wk = insertGeTuWettkampf("ScenarioMediaInvalidType", 0)
      val reg = newRegistration(wk, "MediaInvalidTypeClub")
      val athReg = createAthletRegistration(AthletRegistration(
        0,
        reg.id,
        None,
        "M",
        "Wrong",
        "Type",
        "2012-04-04",
        20,
        0,
        None,
        None,
        None
      ))

      val uploadEntity = Multipart.FormData(
        Multipart.FormData.BodyPart.Strict(
          "mediafile",
          HttpEntity(ContentTypes.`text/plain(UTF-8)`, "not-an-mp3"),
          Map("filename" -> "sample.txt")
        )
      ).toEntity

      HttpRequest(POST, s"/api/registrations/${wk.uuid.get}/${reg.id}/athletes/${athReg.id}/mediafile", entity = uploadEntity)
        .addHeader(registrationJwt(reg)) ~>
        routes ~> check {
        status should ===(StatusCodes.Conflict)
        entityAs[String] should include("Ungültige Audiodatei")
      }
    }
  }
}


