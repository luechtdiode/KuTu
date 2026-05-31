package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec

import java.util.UUID

class RegistrationSpec extends KuTuBaseSpec {
  val testwettkampf = insertGeTuWettkampf("TestGetuWK", 2)
  val testwettkampf2 = insertGeTuWettkampf("TestGetuWK2", 2)
  val myverysecretpassword = "vkyvf%gMnvXs"
  var registration: Option[Registration] = None

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

  def createTestAthletRegistration(reg: Registration, withMedia: Option[MediaAdmin] = Some(MediaAdmin("1", "life-is-life.mp3", "mp3", 0, "", "", 0)), team: Option[Int] = None, reserve: Int = 0) = {
    val teamText = team match {
      case Some(n) => s"-Team$n"
      case None => ""
    }
    createAthletRegistration(AthletRegistration(0, reg.id, None, "M", "Tester", s"Test$teamText", "2010-05-05", 20, 0, None, team, withMedia, reserve = reserve))
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
      athletRegistration.reserve should ===(0)
    }

    "add athlet-registration with team and reserve" in {
      val reg = createTestRegistration
      val athletRegistration: AthletRegistration = createTestAthletRegistration(reg, team=Some(1), reserve = 1)
      assert(athletRegistration.id > 0L)
      athletRegistration.vereinregistrationId should ===(reg.id)
      athletRegistration.name should ===("Tester")
      athletRegistration.team should ===(Some(1))
      athletRegistration.reserve should ===(1)
    }

    "selectAthletRegistration" in {
      val registration = createTestRegistration
      val athletRegistration: AthletRegistration = createTestAthletRegistration(registration)
      val selectedregistration = selectAthletRegistration(athletRegistration.id)
      selectedregistration.id should ===(athletRegistration.id)
      selectedregistration.reserve should ===(0)
    }

    "selectAthletRegistrations with team" in {
      val registration = createTestRegistration
      val athletRegistration: AthletRegistration = createTestAthletRegistration(registration, team=Some(1), reserve = 1)
      val registrations = selectAthletRegistrations(registration.id)
      registrations should not be empty
      assert(registrations.exists(r => r.id === athletRegistration.id) === true)
      assert(registrations.exists(r => r.id === athletRegistration.id && r.reserve == 1) === true)
    }

    "delete AthletRegistration" in {
      val registration = createTestRegistration
      val athletRegistration: AthletRegistration = createTestAthletRegistration(registration, reserve = 1)
      deleteAthletRegistration(athletRegistration.id)
      val registrations = selectAthletRegistrations(registration.id)
      // registrations shouldBe empty
      assert(registrations.exists(r => r.id === athletRegistration.id) === false)
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
      val athlet1 = createTestAthletRegistration(reg, reserve = 1)
      updateAthletRegistration(athlet1.copy(athletId = Some(1L), reserve = 2))

      val secondRegistration = try {
        selectRegistrationsOfWettkampf(UUID.fromString(testwettkampf2.uuid.get)).filter(r => r.vereinId.contains(1L)).head
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
          "M", "Tester", "Test", "2010-05-05", 20, 0, None, None, Some(MediaAdmin("1", "life-is-life.mp3", "mp3", 0, "", "", 0))))
      athlet2.athletId should ===(Some(1L))

      val updated = updateAthletRegistration(athlet2.copy(reserve = 3)).get
      updated.reserve should ===(3)
    }

    "selectRegistrations" in {
      val registrationId = createTestRegistration.id
      val registrations = selectRegistrations()
      registrations should not be empty
      assert(registrations.exists(r => r.id === registrationId) === true)
    }

    "selectRegistrations by wettkampf" in {
      val registrationId = createTestRegistration.id
      val registrations = selectRegistrationsOfWettkampf(testwettkampf.uuid.map(UUID.fromString).get)
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
      deleteRegistrations(testwettkampf.uuid.map(UUID.fromString).get)
      val remainingId = selectRegistrations().filter(id => id === idToDelete)
      remainingId.size shouldBe 0
      registration = None
    }
  }

}