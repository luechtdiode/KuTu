package ch.seidel.kutu.domain

import java.time.LocalDate
import java.util.UUID

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, RawHeader}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.util.ByteString
import authentikat.jwt.JsonWebToken
import ch.seidel.kutu.Config
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.base.KuTuBaseSpec

class RegistrationRestSpec extends KuTuBaseSpec {
  val testwettkampf = insertGeTuWettkampf("TestGetuWK", 2)
  val testwettkampf2 = insertGeTuWettkampf("TestGetuWK2", 2)
  val myverysecretpassword = "vkyvf%gMnvXs"
  var registration: Option[Registration] = None
  var athletregistration: Option[AthletRegistration] = None
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
    val claims = setClaims(registration.get.id.toString, jwtTokenExpiryPeriodInDays)
    registrationJwt = Some(RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, claims, jwtSecretKey)))
    registration.get
  }

  def createTestAthletRegistration(reg: Registration) = {
    athletregistration = athletregistration match {
      case None =>
        Some(createAthletRegistration(AthletRegistration(0, reg.id, None, "M", "Tester", "Test", "2010-05-05", 20, 0)))
      case Some(r) => Some(r)
    }
    athletregistration.get
  }

  "read programmList" in {
    val reg = createTestRegistration
    HttpRequest(method = GET, uri = s"/api/registrations/${testwettkampf.uuid.get}/programmlist") ~>
      allroutes(x => vereinSecretHashLookup(x)) ~> check {
      status should ===(StatusCodes.OK)
      val list = entityAs[List[ProgrammRaw]]
      list.nonEmpty shouldBe true
    }
  }

  "registration" should {
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

    "list Registrations via rest" in {
      val reg = createTestRegistration
      HttpRequest(method = GET, uri = s"/api/registrations/${testwettkampf.uuid.get}") ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        val list = entityAs[List[Registration]]
        list.filter(a => a.id == reg.id).nonEmpty shouldBe true
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

    "read athletList" in {
      val reg = createTestRegistration
      updateRegistration(reg.copy(vereinId = Some(1L)))
      HttpRequest(method = GET, uri = s"/api/registrations/${testwettkampf.uuid.get}/${reg.id}/athletlist")
        .addHeader(registrationJwt.get) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        val list = entityAs[List[AthletView]]
        list.nonEmpty shouldBe true
      }
    }

    "add AthletRegistration via rest" in {
      val reg = createTestRegistration
      val json = athletregistrationFormat.write(
        AthletRegistration(0, reg.id, None, "M", "Tester", "Test", "2020-05-05", 20, 0)
      ).compactPrint
      HttpRequest(method = POST, uri = s"/api/registrations/${testwettkampf.uuid.get}/${reg.id}/athletes", entity = HttpEntity(
        ContentTypes.`application/json`,
        ByteString(json)
      )).addHeader(registrationJwt.get) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        header(Config.jwtAuthorizationKey) should not be empty
        val athletreg = entityAs[AthletRegistration]
        athletreg.vereinregistrationId should ===(reg.id)
      }
    }
    "add AthletRegistration via rest2" in {
      val reg = createTestRegistration
      HttpRequest(method = POST, uri = s"/api/registrations/${testwettkampf.uuid.get}/${reg.id}/athletes", entity = HttpEntity(
        ContentTypes.`application/json`,
             //       {"gebdat":"2020-05-05T02:00:00.000+0200","geschlecht":"M","id":0,"name":"Tester","programId":20,"registrationTime":0,"vereinregistrationId":1,"vorname":"Test"}
        ByteString("""{"id":0,"vereinregistrationId":1,"name":"a","vorname":"b","geschlecht":"W","gebdat":"2003-02-01T00:00:00.000Z","programId":23,"registrationTime":0}""")
      )).addHeader(registrationJwt.get) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        header(Config.jwtAuthorizationKey) should not be empty
        val athletreg = entityAs[AthletRegistration]
        athletreg.vereinregistrationId should ===(reg.id)
      }
    }

    "update AthletRegistration via rest" in {
      val reg = createTestRegistration
      val athletreg = createTestAthletRegistration(reg)
      HttpRequest(method = PUT, uri = s"/api/registrations/${testwettkampf.uuid.get}/${reg.id}/athletes/${athletreg.id}", entity = HttpEntity(
        ContentTypes.`application/json`,
        ByteString(athletregistrationFormat.write(
          athletreg.copy(athletId = Some(1L))
        ).compactPrint)
      )).addHeader(registrationJwt.get) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        header(Config.jwtAuthorizationKey) should not be empty
        val athletreg = entityAs[AthletRegistration]
        athletreg.athletId should ===(Some(1L))
      }
    }

    "list AthletRegistration via rest" in {
      val reg = createTestRegistration
      val athletreg = createTestAthletRegistration(reg)
      HttpRequest(method = GET, uri = s"/api/registrations/${testwettkampf.uuid.get}/${reg.id}/athletes/")
        .addHeader(registrationJwt.get) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
        header(Config.jwtAuthorizationKey) should not be empty
        val list = entityAs[List[AthletRegistration]]
        list.filter(a => a.id == athletreg.id).nonEmpty shouldBe true
      }
    }

    "delete AthletRegistration via rest" in {
      val reg = createTestRegistration
      val athletreg = createTestAthletRegistration(reg)
      HttpRequest(method = DELETE, uri = s"/api/registrations/${testwettkampf.uuid.get}/${reg.id}/athletes/${athletreg.id}")
        .addHeader(registrationJwt.get) ~>
        allroutes(x => vereinSecretHashLookup(x)) ~> check {
        status should ===(StatusCodes.OK)
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
      athletregistration = None
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