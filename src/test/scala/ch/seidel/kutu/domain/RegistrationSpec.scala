package ch.seidel.kutu.domain

import java.util.UUID

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, RawHeader}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.util.ByteString
import authentikat.jwt.JsonWebToken
import ch.seidel.kutu.Config
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.base.KuTuBaseSpec

class RegistrationSpec extends KuTuBaseSpec {
  val testwettkampf = insertGeTuWettkampf("TestGetuWK", 2)
  val testwettkampf2 = insertGeTuWettkampf("TestGetuWK2", 2)
  val myverysecretpassword = "vkyvf%gMnvXs"
  var registration: Option[Registration] = None
  var registrationJwt: Option[RawHeader] = None

  def createTestRegistration = {
    registration = registration match {
      case None =>
        Some(createRegistration(NewRegistration(
          testwettkampf.id,
          "Verein-1", "Verband-1",
          "TestResponsibleName", "TestResponsibleSurname",
          "0796664420", "a@b.com", myverysecretpassword)))
      case Some(r) => Some(r)
    }
    val claims = setClaims(registration.get.id + "", jwtTokenExpiryPeriodInDays)
    registrationJwt = Some(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey)))
    registration.get
  }

  "registration" should {
    "create" in {
      val reg = createTestRegistration
      assert(reg.id > 0L)
      assert(reg.vereinId === None)
    }

    "create via rest" in {
      HttpRequest(method = POST, uri = "/api/registrations/" + testwettkampf.uuid.get, entity = HttpEntity(
        ContentTypes.`application/json`,
        ByteString(newregistrationFormat.write(NewRegistration(
          testwettkampf.id,
          "Verein-2", "Verband-2",
          "TestResponsibleName", "TestResponsibleSurname",
          "0796664420", "a@b.com", myverysecretpassword)).compactPrint)
      )) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        header(Config.jwtAuthorizationKey) should not be empty
        val reg = entityAs[Registration]
        reg.vereinname should ===("Verein-2")
      }
    }

    "protect by unauthorized login" in {
      val reg = createTestRegistration
      // test login via rest-api
      val unauthorizedRequest = HttpRequest(method = POST, uri = "/api/login", entity = "")
        .addHeader(Authorization(BasicHttpCredentials(reg.id.toString, "wrong password")))
      unauthorizedRequest ~> allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.Unauthorized)
        header(Config.jwtAuthorizationKey) shouldBe empty
      }
    }

    "protect by login with regristration id" in {
      val reg = createTestRegistration
      val request = HttpRequest(method = POST, uri = "/api/login", entity = "")
        .addHeader(Authorization(BasicHttpCredentials(reg.id.toString, myverysecretpassword)))
      request ~> allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        header(Config.jwtAuthorizationKey) should not be empty
      }
    }

    "protect by login with jwt" in {
      val reg = createTestRegistration
      val request = HttpRequest(method = POST, uri = "/api/login", entity = "")
        .addHeader(registrationJwt.get)
      request ~> allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        header(Config.jwtAuthorizationKey) should not be empty
      }
    }

    "protect by login with competition/clubname" in {
      val reg = createTestRegistration
      val username = testwettkampf.uuid.get + ":" + reg.vereinname
      val request2 = HttpRequest(method = POST, uri = "/api/login", entity = "")
        .addHeader(Authorization(BasicHttpCredentials(username, myverysecretpassword)))
      request2 ~> allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        header(Config.jwtAuthorizationKey) should not be empty
      }
    }

    "updateRegistration" in {
      val reg = createTestRegistration
      val updatedRegistration = updateRegistration(reg.copy(
        vereinId = Some(1L),
        verband = "Other Verband"))
      updatedRegistration.id should ===(reg.id)
      updatedRegistration.vereinId should ===(Some(1L))
      updatedRegistration.verband should ===("Other Verband")
    }

    "updateRegistration via rest" in {
      val reg = createTestRegistration
      HttpRequest(method = PUT, uri = "/api/registrations/" + testwettkampf.uuid.get + "/" + reg.id, entity = HttpEntity(
        ContentTypes.`application/json`,
        ByteString(registrationFormat.write(reg.copy(
          verband = "Next Verband")).compactPrint)
      )).addHeader(registrationJwt.get) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        header(Config.jwtAuthorizationKey) should not be empty
        val reg = entityAs[Registration]
        reg.verband should ===("Next Verband")
      }
    }

    "updateRegistration via rest unauthorized" in {
      val reg = createTestRegistration
      HttpRequest(method = PUT, uri = "/api/registrations/" + testwettkampf.uuid.get + "/" + reg.id, entity = HttpEntity(
        ContentTypes.`application/json`,
        ByteString(registrationFormat.write(reg.copy(
          verband = "Next Verband")).compactPrint)
      )) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "auto-approve similar registration as already registered for earlier competitions" in {
      val reg = createTestRegistration
      updateRegistration(reg.copy(
        vereinId = Some(1L),
        verband = "Verband-1"))
      val sedondRegistration = createRegistration(NewRegistration(
        testwettkampf2.id,
        "Verein-1", "Verband-1",
        "TestResponsibleName", "TestResponsibleSurname",
        "0796664420", "a@b.com", myverysecretpassword))
      sedondRegistration.vereinId should ===(Some(1L))
    }

    "selectRegistrations" in {
      val registrationId = createTestRegistration.id
      val registrations = selectRegistrations()
      registrations should not be empty
      assert(registrations.exists(r => r.id === registrationId) === true)
    }

    "selectRegistrations by wettkampf" in {
      val registrationId = createTestRegistration.id
      val registrations = selectRegistrationsOfWettkampf(testwettkampf.uuid.map(UUID.fromString(_)).get)
      registrations should not be empty
      assert(registrations.exists(r => r.id === registrationId) === true)
    }

    "delete" in {
      val registrationToDelete = createTestRegistration
      val idToDelete = registrationToDelete.id
      deleteRegistration(idToDelete)
      val remainingId = selectRegistrations().filter(id => id === idToDelete)
      remainingId.size shouldBe 0
      registration = None
    }

    "delete all from competition" in {
      val registrationToDelete = createTestRegistration
      val idToDelete = registrationToDelete.id
      deleteRegistrations(testwettkampf.uuid.map(UUID.fromString(_)).get)
      val remainingId = selectRegistrations().filter(id => id === idToDelete)
      remainingId.size shouldBe 0
      registration = None
    }

    "delete via rest" in {
      val registrationToDelete = createTestRegistration
      val idToDelete = registrationToDelete.id
      HttpRequest(method = DELETE, uri = "/api/registrations/" + testwettkampf.uuid.get + "/" + idToDelete)
        .addHeader(registrationJwt.get) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~>
        check {
          status should ===(StatusCodes.OK)
        }
      registration = None
    }
    "delete via rest unauthorized" in {
      val registrationToDelete = createTestRegistration
      val idToDelete = registrationToDelete.id
      HttpRequest(method = DELETE, uri = "/api/registrations/" + testwettkampf.uuid.get + "/" + idToDelete) ~>
      allroutes(x => vereinSecretHashLookup(x)) ~>
      check {
        status should ===(StatusCodes.Unauthorized)
      }
    }
  }
}