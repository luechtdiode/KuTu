package ch.seidel.kutu.domain

import java.time.LocalDate
import java.util.UUID
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, RawHeader}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.util.ByteString
import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey, jwtTokenExpiryPeriodInDays}
import ch.seidel.kutu.base.KuTuBaseSpec

class RegistrationSpec extends KuTuBaseSpec {
  val testwettkampf = insertGeTuWettkampf("TestGetuWK", 2)
  val testwettkampf2 = insertGeTuWettkampf("TestGetuWK2", 2)
  val myverysecretpassword = "vkyvf%gMnvXs"
  var registration: Option[Registration] = None
  var athletregistration: Option[AthletRegistration] = None

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
    registration.get
  }

  def createTestAthletRegistration(reg: Registration) = {
    athletregistration = athletregistration match {
      case None =>
        Some(createAthletRegistration(AthletRegistration(0, reg.id, None, "M", "Tester", "Test", "2010-05-05", 20, 0, None)))
      case Some(r) =>
        if (r.vereinregistrationId == reg.id)
        Some(r) else Some(createAthletRegistration(AthletRegistration(0, reg.id, None, "M", "Tester", "Test", "2010-05-05", 20, 0, None)))
    }
    athletregistration.get
  }

  "registration" should {
    "create" in {
      val reg = createTestRegistration
      assert(reg.id > 0L)
      assert(reg.vereinId === None)
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

    "add athlet-registration" in {
      val reg = createTestRegistration
      val athletRegistration: AthletRegistration = createTestAthletRegistration(reg)
      assert(athletRegistration.id > 0L)
      athletRegistration.vereinregistrationId should ===(reg.id)
      athletRegistration.name should ===("Tester")
    }

    "selectAthletRegistration" in {
      val registration = createTestRegistration
      val athletRegistration: AthletRegistration = createTestAthletRegistration(registration)
      val selectedregistration = selectAthletRegistration(athletRegistration.id)
      selectedregistration.id should ===(athletRegistration.id)
    }

    "selectAthletRegistrations" in {
      val registration = createTestRegistration
      val athletRegistration: AthletRegistration = createTestAthletRegistration(registration)
      val registrations = selectAthletRegistrations(registration.id)
      registrations should not be empty
      assert(registrations.exists(r => r.id === athletRegistration.id) === true)
    }

    "delete AthletRegistration" in {
      val registration = createTestRegistration
      val athletRegistration: AthletRegistration = createTestAthletRegistration(registration)
      deleteAthletRegistration(athletRegistration.id)
      val registrations = selectAthletRegistrations(registration.id)
      // registrations shouldBe empty
      assert(registrations.exists(r => r.id === athletRegistration.id) === false)
      athletregistration = None
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

    "auto-approve similar athlet-registration as already registered for earlier competitions" in {
      val reg = createTestRegistration
      updateRegistration(reg.copy(
        vereinId = Some(1L),
        verband = "Verband-1"))
      val athlet1 = createTestAthletRegistration(reg)
      updateAthletRegistration(athlet1.copy(athletId = Some(1L)))

      val secondRegistration = try {
        selectRegistrationsOfWettkampf(UUID.fromString(testwettkampf2.uuid.get)).filter(r => r.vereinId == Some(1L)).head
      } catch {
        case e: Exception =>
        createRegistration(NewRegistration(
          testwettkampf2.id,
          "Verein-1", "Verband-1",
          "TestResponsibleName", "TestResponsibleSurname",
          "0796664420", "a@b.com", myverysecretpassword))
      }
      val athlet2 = createAthletRegistration(
        AthletRegistration(0, secondRegistration.id, None,
          "M", "Tester", "Test", "2010-05-05", 20, 0, None))
      athlet2.athletId should ===(Some(1L))
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
      athletregistration = None
    }

    "delete all from competition" in {
      val registrationToDelete = createTestRegistration
      val idToDelete = registrationToDelete.id
      deleteRegistrations(testwettkampf.uuid.map(UUID.fromString(_)).get)
      val remainingId = selectRegistrations().filter(id => id === idToDelete)
      remainingId.size shouldBe 0
      registration = None
      athletregistration = None
    }
  }

}