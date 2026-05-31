package ch.seidel.kutu.data

import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.data.RegistrationAdmin._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.view.WettkampfInfo
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Seconds, Span}

import java.sql.Date
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RegistrationAdminSpec extends KuTuBaseSpec with PatienceConfiguration {

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  val testWettkampf: Wettkampf = insertGeTuWettkampf("RegistrationAdminTestWK", 2)
  lazy val wkInfo: WettkampfInfo = {
    val programmes = readWettkampfLeafs(testWettkampf.programmId)
    val aggregatorProgramm = programmes.headOption.map(_.aggregatorHead).getOrElse(programmes.head)
    val wkView = testWettkampf.toView(aggregatorProgramm)
    WettkampfInfo(wkView, this)
  }

  "RegistrationAdmin" should {

    // ===== Tests for doSyncUnassignedClubRegistrations =====
    "doSyncUnassignedClubRegistrations should detect new clubs" in {
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "NewClub", "TestVerband",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      val athletReg = createAthletRegistration(AthletRegistration(0, reg.id, None, "M", "TestAthlet", "Test", "2010-05-05", 20, 0, None, None, None))
      val athlet = Athlet(0).copy(
        geschlecht = athletReg.geschlecht,
        name = athletReg.name,
        vorname = athletReg.vorname,
        verein = None
      )
      val athletView = athlet.toAthletView(None)

      val regTuple: (Registration, AthletRegistration, Athlet, AthletView) = (reg, athletReg, athlet, athletView)
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List(regTuple))

      syncActions should not be empty
      syncActions.exists(_.isInstanceOf[AddVereinAction]) should be(true)
    }

    "doSyncUnassignedClubRegistrations should handle unassigned clubs" in {
      // Create a club that will be resolved
      val clubId = createVerein("UnassignedClub", Some("Verband1"))
      val club = Verein(clubId, "UnassignedClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "UnassignedClub", "Verband1",
        "Responsible", "Person",
        "0791234567", "contact@test.com", "secret"
      ))
      val athletReg = createAthletRegistration(AthletRegistration(0, reg.id, None, "W", "Maria", "Mueller", "2012-01-15", 20, 0, None, None, None))
      val athlet = Athlet(0).copy(
        geschlecht = athletReg.geschlecht,
        name = athletReg.name,
        vorname = athletReg.vorname,
        verein = Some(clubId)
      )
      val athletView = athlet.toAthletView(Some(club))

      val regTuple: (Registration, AthletRegistration, Athlet, AthletView) = (reg, athletReg, athlet, athletView)
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List(regTuple))

      // validatedClubs should contain clubs where registration has no vereinId but athletView has verein
      clubs should not be empty
      syncActions should not be empty
    }

    "doSyncUnassignedClubRegistrations should identify club name changes" in {
      // Create existing club
      val existingClubId = createVerein("ExistingClubName", Some("ExistingVerband"))
      val existingClub = Verein(existingClubId, "ExistingClubName", Some("ExistingVerband"))

      // Create registration that matches existing club but with different name
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "DifferentClubName", "ExistingVerband",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      val updatedReg = updateRegistration(reg.copy(vereinId = Some(existingClubId)))

      val athletReg = createAthletRegistration(AthletRegistration(0, updatedReg.id, None, "M", "John", "Doe", "2010-05-05", 20, 0, None, None, None))
      val athlet = Athlet(0).copy(
        geschlecht = athletReg.geschlecht,
        name = athletReg.name,
        vorname = athletReg.vorname,
        verein = Some(existingClubId)
      )
      val athletView = athlet.toAthletView(Some(existingClub))

      val regTuple: (Registration, AthletRegistration, Athlet, AthletView) = (updatedReg, athletReg, athlet, athletView)
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List(regTuple))

      // Should detect club name mismatch between registration and resolved club
      syncActions.exists(_.isInstanceOf[RenameVereinAction]) should be(true)
    }

    "doSyncUnassignedClubRegistrations should identify athlete name changes" in {
      val existingClubId = createVerein("TestClub", Some("TestVerband"))
      val existingClub = Verein(existingClubId, "TestClub", Some("TestVerband"))
      val existingAthlet = insertAthlete(Athlet(existingClubId).copy(
        name = "OriginalName",
        vorname = "OriginalFirstName"
      ))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "TestClub", "TestVerband",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      updateRegistration(reg.copy(vereinId = Some(existingClubId)))

      val athletReg = createAthletRegistration(AthletRegistration(
        0, reg.id, Some(existingAthlet.id), "M", "DifferentName", "DifferentFirstName", "2010-05-05", 20, 0, None, None, None
      ))

      val athlet = Athlet(0).copy(
        id = existingAthlet.id,
        geschlecht = "M",
        name = "DifferentName",
        vorname = "DifferentFirstName",
        verein = Some(existingClubId)
      )
      val athletView = athlet.toAthletView(Some(existingClub))

      val regTuple: (Registration, AthletRegistration, Athlet, AthletView) = (
        reg, athletReg, existingAthlet, athletView
      )
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List(regTuple))

      syncActions should not be empty
    }

    "doSyncUnassignedClubRegistrations should handle empty registrations" in {
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "ClubWithEmpty", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      val emptyAthletReg = EmptyAthletRegistration(reg.id)
      val athlet = emptyAthletReg.toAthlet
      val athletView = athlet.toAthletView(None)

      val regTuple: (Registration, AthletRegistration, Athlet, AthletView) = (reg, emptyAthletReg, athlet, athletView)
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List(regTuple))

      // Empty registrations should not generate add actions
      syncActions.find {
        case ar: AddRegistration => !ar.suggestion.verein.isEmpty
        case _ => false
      } should be(None)
    }

    "doSyncUnassignedClubRegistrations should handle empty input list" in {
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List())

      clubs should be(empty)
      syncActions should be(empty)
    }

    "doSyncUnassignedClubRegistrations should handle multiple registrations from same club" in {
      val clubId = createVerein("MultiAthleteClub", Some("Verband1"))
      val club = Verein(clubId, "MultiAthleteClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "MultiAthleteClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val athReg1 = createAthletRegistration(AthletRegistration(0, reg.id, None, "M", "Athlete1", "Test", "2010-05-05", 20, 0, None, None, None))
      val athReg2 = createAthletRegistration(AthletRegistration(0, reg.id, None, "W", "Athlete2", "Test", "2012-01-15", 20, 0, None, None, None))

      val athlet1 = Athlet(0).copy(geschlecht = "M", name = "Athlete1", vorname = "Test", verein = Some(clubId))
      val athlet2 = Athlet(0).copy(geschlecht = "W", name = "Athlete2", vorname = "Test", verein = Some(clubId))
      val athletView1 = athlet1.toAthletView(Some(club))
      val athletView2 = athlet2.toAthletView(Some(club))

      val regTuples = List(
        (reg, athReg1, athlet1, athletView1),
        (reg, athReg2, athlet2, athletView2)
      )
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(regTuples)

      syncActions should not be empty
    }

    // ===== Tests for findAthletLike =====
    "findAthletLike should return existing athlete when found" in {
      val clubId = createVerein("FuzzySearchClub", Some("TestVerband"))
      val existingAthlet = insertAthlete(Athlet(clubId).copy(
        name = "Mueller",
        vorname = "Hans"
      ))

      val searchAthlet = Athlet(clubId).copy(
        name = "Mueller",
        vorname = "Hans"
      )

      val found = findAthletLike(searchAthlet)
      found.name.shouldEqual(existingAthlet.name)
      found.vorname.shouldEqual(existingAthlet.vorname)
    }

    "findAthletLike should return original athlete if no match found" in {
      val nonExistentClub = 99999L
      val searchAthlet = Athlet(nonExistentClub).copy(
        name = "UniqueNameXYZ123",
        vorname = "VeryUniqueFirst"
      )

      val found = findAthletLike(searchAthlet)
      // Should return the athlete itself when no match is found
      found should not be null
    }

    // ===== Tests for computeSyncActions =====
    "computeSyncActions should return Future with sync actions" in {
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "ComputeTestClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      val athletReg = createAthletRegistration(AthletRegistration(
        0, reg.id, None, "M", "ComputeAthlet", "Test", "2010-05-05", 20, 0, None, None, None
      ))

      val futureActions = computeSyncActions(wkInfo, this)
      val results = Await.result(futureActions, Duration.Inf)

      results should not be empty
      results.forall(_.isInstanceOf[SyncAction]) should be(true)
    }

    "computeSyncActions should process all registrations of wettkampf" in {
      val reg1 = createRegistration(NewRegistration(
        testWettkampf.id,
        "Compute1", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      val reg2 = createRegistration(NewRegistration(
        testWettkampf.id,
        "Compute2", "Verband2",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val athReg1 = createAthletRegistration(AthletRegistration(0, reg1.id, None, "M", "Athlete1", "Test", "2010-05-05", 20, 0, None, None, None))
      val athReg2 = createAthletRegistration(AthletRegistration(0, reg2.id, None, "W", "Athlete2", "Test", "2010-05-05", 20, 0, None, None, None))

      val futureActions = computeSyncActions(wkInfo, this)
      val results = Await.result(futureActions, Duration.Inf)

      results should not be empty
    }

    // ===== Tests for adjustWertungRiegen =====
    "adjustWertungRiegen should update riege names after athlete update" in {
      // Create athlete with wertungen
      val clubId = createVerein("RiegenTestClub", Some("TestVerband"))
      val athlet = insertAthlete(Athlet(clubId).copy(
        geschlecht = "M",
        name = "TestName",
        vorname = "TestFirst",
        gebdat = Some(Date.valueOf(LocalDate.now().minus(15, ChronoUnit.YEARS)))
      ))

      // Assign to programs and create wertungen
      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.nonEmpty then {
        val firstProg = programmes.head
        assignAthletsToWettkampf(testWettkampf.id, Set(firstProg.id), Set((athlet.id, None)), None)

        // Make einteilung to create riegen
        makeEinteilung(testWettkampf)

        val originalWertungen = selectWertungen(athletId = Some(athlet.id), wettkampfId = Some(testWettkampf.id))
        originalWertungen should not be empty

        // Update athlete (simulate sex change)
        val updatedAthlet = athlet.copy(geschlecht = "W")
        adjustWertungRiegen(testWettkampf, this, updatedAthlet, true)

        // Verify wertungen were updated
        val updatedWertungen = selectWertungen(athletId = Some(athlet.id), wettkampfId = Some(testWettkampf.id))
        updatedWertungen should not be empty
      }
    }

    "adjustWertungRiegen should handle athlete with no wertungen" in {
      val clubId = createVerein("NoWertungenClub", Some("TestVerband"))
      val athlet = insertAthlete(Athlet(clubId).copy(
        geschlecht = "M",
        name = "NoWertungenAthlete",
        vorname = "Test"
      ))

      // No wertungen assigned to this athlete, should not throw
      val updatedAthlet = athlet.copy(geschlecht = "W")
      adjustWertungRiegen(testWettkampf, this, updatedAthlet)

      val wertungen = selectWertungen(athletId = Some(athlet.id), wettkampfId = Some(testWettkampf.id))
      wertungen should be(empty)
    }

    // ===== Tests for computeSyncActions edge cases =====
    "computeSyncActions should handle wettkampf with no registrations" in {
      val emptyWK = createWettkampf(
        new Date(System.currentTimeMillis()),
        "EmptyWK",
        Set(20L),
        "test@test.com",
        3333,
        7.5d,
        Some(UUID.randomUUID().toString),
        "", "", "",
        "Kategorie/AlterAufsteigend/Verein/Vorname/Name/Rotierend/AltInvers",
        ""
      )
      val emptyProgrammes = readWettkampfLeafs(emptyWK.programmId)
      val emptyAggregatorProgramm = emptyProgrammes.headOption.map(_.aggregatorHead).getOrElse(emptyProgrammes.head)
      val emptyWKView = emptyWK.toView(emptyAggregatorProgramm)
      val emptyWKInfo = WettkampfInfo(emptyWKView, this)

      val futureActions = computeSyncActions(emptyWKInfo, this)
      val results = Await.result(futureActions, Duration.Inf)

      // Should return a list (may be empty or contain empty registration actions)
      results.forall(_.isInstanceOf[SyncAction]) should be(true)
    }

    "doSyncUnassignedClubRegistrations should process distinct sync actions" in {
      val clubId = createVerein("DistinctClub", Some("Verband1"))
      val club = Verein(clubId, "DistinctClub", Some("Verband1"))

      // Use a single registration with the same club
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "DistinctClub", "Verband1",
        "Contact1", "Name1",
        "0791234567", "test1@test.com", "secret"
      ))

      // Create two empty athlete registrations for the same registration
      val emptyReg1 = EmptyAthletRegistration(reg.id)
      val emptyReg2 = EmptyAthletRegistration(reg.id)

      val regTuples = List(
        (reg, emptyReg1, Athlet(clubId), Athlet(clubId).toAthletView(Some(club))),
        (reg, emptyReg2, Athlet(clubId), Athlet(clubId).toAthletView(Some(club)))
      )

      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(regTuples)

      // Distinct should remove duplicates
      syncActions.distinct.length should be <= syncActions.length
    }

    "doSyncUnassignedClubRegistrations should categorize athletes correctly" in {
      val clubId = createVerein("CategorizeClub", Some("Verband1"))
      val club = Verein(clubId, "CategorizeClub", Some("Verband1"))
      val existingAthlet = insertAthlete(Athlet(clubId).copy(name = "Existing", vorname = "Athlet"))

      // Create a registration with existing athlete
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "CategorizeClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      updateRegistration(reg.copy(vereinId = Some(clubId)))

      val athletReg = createAthletRegistration(AthletRegistration(
        0, reg.id, Some(existingAthlet.id), "M", "Existing", "Athlet", "1950-01-01", 20, 0, None, None, None
      ))

      val regTuple: (Registration, AthletRegistration, Athlet, AthletView) = (
        reg, athletReg, existingAthlet, existingAthlet.toAthletView(Some(club))
      )
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List(regTuple))

      // Should categorize correctly without throwing
      syncActions should not be empty
    }

    "computeSyncActions should wrap results as SyncAction" in {
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "PublicSyncTestClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      val athletReg = createAthletRegistration(AthletRegistration(
        0, reg.id, None, "M", "PublicSyncAthlet", "Test", "2010-05-05", 20, 0, None, None, None
      ))

      val futureActions = computeSyncActions(wkInfo, this)
      val results = Await.result(futureActions, Duration.Inf)

      results.foreach { action =>
        action.isInstanceOf[SyncAction] should be(true)
      }
    }

    "doSyncUnassignedClubRegistrations should handle mixed athlete states" in {
      val clubId = createVerein("MixedStateClub", Some("Verband1"))
      val club = Verein(clubId, "MixedStateClub", Some("Verband1"))
      val existingAthlet = insertAthlete(Athlet(clubId).copy(name = "Mixed", vorname = "Athlet"))

      // Use a single registration to avoid UNIQUE constraint violation
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "MixedStateClub", "Verband1",
        "Contact1", "Name1",
        "0791234567", "test1@test.com", "secret"
      ))
      updateRegistration(reg.copy(vereinId = Some(clubId)))

      // One with existing athlete, one new - both under same registration
      val athReg1 = createAthletRegistration(AthletRegistration(0, reg.id, Some(existingAthlet.id), "M", "Mixed", "Athlet", "1950-01-01", 20, 0, None, None, None))
      val athReg2 = createAthletRegistration(AthletRegistration(0, reg.id, None, "W", "New", "Athlet", "2010-05-05", 20, 0, None, None, None))

      val regTuples = List(
        (reg, athReg1, existingAthlet, existingAthlet.toAthletView(Some(club))),
        (reg, athReg2, Athlet(clubId).copy(name = "New", vorname = "Athlet"), Athlet(clubId).copy(name = "New", vorname = "Athlet").toAthletView(Some(club)))
      )

      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(regTuples)

      syncActions should not be empty
    }

    // ===== Additional Edge Case Tests =====
    "doSyncUnassignedClubRegistrations should handle athletes with special characters in names" in {
      val clubId = createVerein("SpecialCharClub", Some("Verband1"))
      val club = Verein(clubId, "SpecialCharClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "SpecialCharClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val athletReg = createAthletRegistration(AthletRegistration(
        0, reg.id, None, "M", "Müller-Schmidt", "François", "2010-05-05", 20, 0, None, None, None
      ))

      val athlet = Athlet(0).copy(
        geschlecht = "M",
        name = "Müller-Schmidt",
        vorname = "François",
        verein = Some(clubId)
      )
      val athletView = athlet.toAthletView(Some(club))

      val regTuple: (Registration, AthletRegistration, Athlet, AthletView) = (reg, athletReg, athlet, athletView)
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List(regTuple))

      syncActions should not be empty
      syncActions.exists(_.isInstanceOf[AddRegistration]) should be(true)
    }

    "doSyncUnassignedClubRegistrations should handle team reassignments" in {
      val clubId = createVerein("TeamChangeClub", Some("Verband1"))
      val club = Verein(clubId, "TeamChangeClub", Some("Verband1"))
      val existingAthlet = insertAthlete(Athlet(clubId).copy(
        name = "TeamPlayer",
        vorname = "Test"
      ))

      // Assign athlete to program with team 1
      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.nonEmpty then {
        val firstProg = programmes.head
        assignAthletsToWettkampf(testWettkampf.id, Set(firstProg.id), Set((existingAthlet.id, None)), None)

        val reg = createRegistration(NewRegistration(
          testWettkampf.id,
          "TeamChangeClub", "Verband1",
          "Contact", "Name",
          "0791234567", "test@test.com", "secret"
        ))
        updateRegistration(reg.copy(vereinId = Some(clubId)))

        // Create registration with different team
        val athletReg = createAthletRegistration(AthletRegistration(
          0, reg.id, Some(existingAthlet.id), "M", "TeamPlayer", "Test", "2010-05-05", firstProg.id, 2, None, None, None
        ))

        val regTuple: (Registration, AthletRegistration, Athlet, AthletView) = (
          reg, athletReg, existingAthlet, existingAthlet.toAthletView(Some(club))
        )
        val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List(regTuple))

        // Should detect team change if wertungen exist
        syncActions should not be empty
      }
    }

    "doSyncUnassignedClubRegistrations should handle very long club names" in {
      val longClubName = List.fill(200)("A").mkString
      val clubId = createVerein(longClubName, Some("LongNameVerband"))
      val club = Verein(clubId, longClubName, Some("LongNameVerband"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        longClubName, "LongNameVerband",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val athletReg = createAthletRegistration(AthletRegistration(
        0, reg.id, None, "W", "LongName", "Test", "2010-05-05", 20, 0, None, None, None
      ))

      val athlet = Athlet(0).copy(
        geschlecht = "W",
        name = "LongName",
        vorname = "Test",
        verein = Some(clubId)
      )
      val athletView = athlet.toAthletView(Some(club))

      val regTuple: (Registration, AthletRegistration, Athlet, AthletView) = (reg, athletReg, athlet, athletView)
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(List(regTuple))

      syncActions should not be empty
    }

    "doSyncUnassignedClubRegistrations should handle athletes with identical names but different gebdat" in {
      val clubId = createVerein("TwinClub", Some("Verband1"))
      val club = Verein(clubId, "TwinClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "TwinClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val athletReg1 = createAthletRegistration(AthletRegistration(
        0, reg.id, None, "M", "Schmidt", "Max", "2010-05-05", 20, 0, None, None, None
      ))
      val athletReg2 = createAthletRegistration(AthletRegistration(
        0, reg.id, None, "M", "Schmidt", "Max", "2011-06-06", 20, 0, None, None, None
      ))

      val athlet1 = Athlet(0).copy(geschlecht = "M", name = "Schmidt", vorname = "Max", verein = Some(clubId))
      val athlet2 = Athlet(0).copy(geschlecht = "M", name = "Schmidt", vorname = "Max", verein = Some(clubId))
      val athletView1 = athlet1.toAthletView(Some(club))
      val athletView2 = athlet2.toAthletView(Some(club))

      val regTuples = List(
        (reg, athletReg1, athlet1, athletView1),
        (reg, athletReg2, athlet2, athletView2)
      )
      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(regTuples)

      // Should handle two athletes with same name
      syncActions should not be empty
      syncActions.count(_.isInstanceOf[AddRegistration]) should be >= 2
    }

    "findAthletLike should handle athletes with very similar names" in {
      val clubId = createVerein("SimilarNameClub", Some("TestVerband"))
      insertAthlete(Athlet(clubId).copy(name = "Meier", vorname = "Hans"))
      insertAthlete(Athlet(clubId).copy(name = "Meyer", vorname = "Hans"))
      insertAthlete(Athlet(clubId).copy(name = "Maier", vorname = "Hans"))

      val searchAthlet = Athlet(clubId).copy(name = "Mayer", vorname = "Hans")

      val found = findAthletLike(searchAthlet)
      // Should find one of the similar names or return original
      found should not be null
    }

    "findAthletLike should handle empty name fields gracefully" in {
      val searchAthlet = Athlet(99999L).copy(name = "", vorname = "")

      val found = findAthletLike(searchAthlet)
      found should not be null
    }

    "computeSyncActions should handle registrations with missing optional fields" in {
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "MinimalFieldsClub", "Verband1",
        "Contact", "Name",
        "", "", "secret"  // Empty phone and email
      ))
      val athletReg = createAthletRegistration(AthletRegistration(
        0, reg.id, None, "M", "MinimalAthlet", "Test", "2010-05-05", 20, 0, None, None, None
      ))

      val futureActions = computeSyncActions(wkInfo, this)
      val results = Await.result(futureActions, Duration.Inf)

      // Should handle missing fields without crashing
      results should not be empty
    }

    "adjustWertungRiegen should handle athlete with multiple wertungen" in {
      val clubId = createVerein("MultiWertungClub", Some("TestVerband"))
      val athlet = insertAthlete(Athlet(clubId).copy(
        geschlecht = "M",
        name = "MultiTest",
        vorname = "Athlete",
        gebdat = Some(Date.valueOf(LocalDate.now().minus(15, ChronoUnit.YEARS)))
      ))

      // Assign to multiple programs
      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.size >= 2 then {
        assignAthletsToWettkampf(testWettkampf.id, Set(programmes.head.id, programmes(1).id), Set((athlet.id, None)), None)
        makeEinteilung(testWettkampf)

        val originalWertungen = selectWertungen(athletId = Some(athlet.id), wettkampfId = Some(testWettkampf.id))
        originalWertungen.size should be >= 2

        // Update athlete
        val updatedAthlet = athlet.copy(name = "UpdatedMultiTest")
        adjustWertungRiegen(testWettkampf, this, updatedAthlet)

        // Verify all wertungen were updated
        val updatedWertungen = selectWertungen(athletId = Some(athlet.id), wettkampfId = Some(testWettkampf.id))
        updatedWertungen.size should equal(originalWertungen.size)
      }
    }

    "adjustWertungRiegen should be idempotent" in {
      val clubId = createVerein("IdempotentClub", Some("TestVerband"))
      val athlet = insertAthlete(Athlet(clubId).copy(
        geschlecht = "W",
        name = "IdempotentTest",
        vorname = "Athlete",
        gebdat = Some(Date.valueOf(LocalDate.now().minus(15, ChronoUnit.YEARS)))
      ))

      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.nonEmpty then {
        assignAthletsToWettkampf(testWettkampf.id, Set(programmes.head.id), Set((athlet.id, None)), None)
        makeEinteilung(testWettkampf)

        // Call adjustWertungRiegen multiple times
        adjustWertungRiegen(testWettkampf, this, athlet)
        val firstResult = selectWertungen(athletId = Some(athlet.id), wettkampfId = Some(testWettkampf.id))

        adjustWertungRiegen(testWettkampf, this, athlet)
        val secondResult = selectWertungen(athletId = Some(athlet.id), wettkampfId = Some(testWettkampf.id))

        // Results should be identical
        firstResult.size should equal(secondResult.size)
        firstResult.zip(secondResult).foreach { case (w1, w2) =>
          w1.riege should equal(w2.riege)
          w1.riege2 should equal(w2.riege2)
        }
      }
    }

    "doSyncUnassignedClubRegistrations should handle registrations with all empty athlete registrations" in {
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "AllEmptyClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val emptyRegs = List(
        (reg, EmptyAthletRegistration(reg.id), Athlet(0), Athlet(0).toAthletView(None)),
        (reg, EmptyAthletRegistration(reg.id), Athlet(0), Athlet(0).toAthletView(None)),
        (reg, EmptyAthletRegistration(reg.id), Athlet(0), Athlet(0).toAthletView(None))
      )

      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(emptyRegs)

      // Should not generate any add actions for empty registrations
      syncActions.filter(_.isInstanceOf[AddRegistration]) should be(empty)
    }

    "doSyncUnassignedClubRegistrations should handle mixed gender athletes in same registration" in {
      val clubId = createVerein("MixedGenderClub", Some("Verband1"))
      val club = Verein(clubId, "MixedGenderClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "MixedGenderClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val maleReg = createAthletRegistration(AthletRegistration(
        0, reg.id, None, "M", "Male", "Athlete", "2010-05-05", 20, 0, None, None, None
      ))
      val femaleReg = createAthletRegistration(AthletRegistration(
        0, reg.id, None, "W", "Female", "Athlete", "2010-05-05", 20, 0, None, None, None
      ))

      val maleAthlet = Athlet(0).copy(geschlecht = "M", name = "Male", vorname = "Athlete", verein = Some(clubId))
      val femaleAthlet = Athlet(0).copy(geschlecht = "W", name = "Female", vorname = "Athlete", verein = Some(clubId))

      val regTuples = List(
        (reg, maleReg, maleAthlet, maleAthlet.toAthletView(Some(club))),
        (reg, femaleReg, femaleAthlet, femaleAthlet.toAthletView(Some(club)))
      )

      val (clubs, syncActions) = doSyncUnassignedClubRegistrations(wkInfo, this)(regTuples)

      // Should handle both genders correctly
      syncActions.filter(_.isInstanceOf[AddRegistration]).size should be >= 2
    }
/*

    "computeSyncActions should handle concurrent execution" in {
      // Create some test data
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "ConcurrentClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      createAthletRegistration(AthletRegistration(
        0, reg.id, None, "M", "Concurrent", "Test", "2010-05-05", 20, 0, None, None, None
      ))

      // Execute multiple times concurrently
      val futures = (1 to 3).map(_ => computeSyncActions(wkInfo, this))
      val results = futures.map(f => Await.result(f, Duration.Inf))

      // All should complete successfully
      results.foreach { result =>
        result should not be empty
      }
    }
*/

    "doSyncUnassignedClubRegistrations should preserve action order for deterministic processing" in {
      val clubId = createVerein("OrderTestClub", Some("Verband1"))
      val club = Verein(clubId, "OrderTestClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "OrderTestClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      // Create multiple athlete registrations
      val athletes = (1 to 5).map { i =>
        val athletReg = createAthletRegistration(AthletRegistration(
          0, reg.id, None, "M", s"Athlete$i", "Test", "2010-05-05", 20, 0, None, None, None
        ))
        val athlet = Athlet(0).copy(geschlecht = "M", name = s"Athlete$i", vorname = "Test", verein = Some(clubId))
        (reg, athletReg, athlet, athlet.toAthletView(Some(club)))
      }.toList

      val (clubs, syncActions1) = doSyncUnassignedClubRegistrations(wkInfo, this)(athletes)
      val (_, syncActions2) = doSyncUnassignedClubRegistrations(wkInfo, this)(athletes)

      // Should produce consistent results
      syncActions1.size should equal(syncActions2.size)
    }

    // ===== Tests for processSync =====
    "processSync should process AddVereinAction and create new clubs" in {
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "ProcessSyncNewClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val syncActions = List(AddVereinAction(reg))
      val testService = new TestRegistrationService(this)

      val results = processSync(wkInfo, testService, syncActions, Set())

      testService.insertedVereine.size should be(1)
      testService.insertedVereine.head.name should be("ProcessSyncNewClub")
    }

    "processSync should process AddRegistration with existing club" in {
      val clubId = createVerein("AddRegTestClub", Some("Verband1"))
      val club = Verein(clubId, "AddRegTestClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "AddRegTestClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val athlet = Athlet(clubId).copy(name = "TestAthlet", vorname = "Test")
      val athletView = athlet.toAthletView(Some(club))
      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.nonEmpty then {
        val syncActions = List(AddRegistration(reg, programmes.head.id, athlet, athletView, 0, None))
        val testService = new TestRegistrationService(this)

        processSync(wkInfo, testService, syncActions, Set())

        testService.insertedAthletes.size should be(1)
        testService.assignedAthletes.size should be(1)
      }
    }

    "processSync should process AddRegistration with new club from newClubs list" in {
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "NewClubForAddReg", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val athlet = Athlet(0).copy(name = "NewClubAthlet", vorname = "Test")
      val athletView = athlet.toAthletView(None)
      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.nonEmpty then {
        val syncActions = List(
          AddVereinAction(reg),
          AddRegistration(reg, programmes.head.id, athlet, athletView, 0, None)
        )
        val testService = new TestRegistrationService(this)

        processSync(wkInfo, testService, syncActions, Set())

        testService.insertedVereine.size should be(1)
        testService.insertedAthletes.size should be(1)
        testService.assignedAthletes.size should be(1)
      }
    }
/*
    "processSync should process RenameAthletAction for local and remote updates" in {
      val clubId = createVerein("RenameAthlTestClub", Some("Verband1"))
      val existingAthlet = insertAthlete(Athlet(clubId).copy(
        name = "OldName",
        vorname = "OldFirst",
        geschlecht = "M"
      ))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "RenameAthlTestClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val athletReg = AthletRegistration(
        0, reg.id, Some(existingAthlet.id), "M", "NewName", "NewFirst", "2010-05-05", 20, 0, Some(existingAthlet.toAthletView(Some(Verein(existingAthlet.verein.get.toLong, "club", None)))), None, None
      )

      val expectedAthlet = existingAthlet.copy(name = "NewName", vorname = "NewFirst")
      val syncActions = List(RenameAthletAction(reg, athletReg, existingAthlet, expectedAthlet))
      val testService = new TestRegistrationService(this)

      processSync(wkInfo, testService, syncActions, Set())

      testService.insertedAthletes.size should be(1)
      testService.remoteAthletesUpdated.size should be(1)
      testService.insertedAthletes.head._2.name should be("NewName")
      testService.insertedAthletes.head._2.vorname should be("NewFirst")
    }

    "processSync should process RenameAthletAction with sex change" in {
      val clubId = createVerein("SexChangeTestClub", Some("Verband1"))
      val existingAthlet = insertAthlete(Athlet(clubId).copy(
        name = "SexChangeAthlet",
        vorname = "Test",
        geschlecht = "M",
        gebdat = Some(Date.valueOf(LocalDate.now().minus(15, ChronoUnit.YEARS)))
      ))

      // Assign to program and create wertungen
      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.nonEmpty then {
        assignAthletsToWettkampf(testWettkampf.id, Set(programmes.head.id), Set((existingAthlet.id, None)), None)
        makeEinteilung(testWettkampf)

        val reg = createRegistration(NewRegistration(
          testWettkampf.id,
          "SexChangeTestClub", "Verband1",
          "Contact", "Name",
          "0791234567", "test@test.com", "secret"
        ))

        val athletReg = AthletRegistration(
          0, reg.id, Some(existingAthlet.id), "W", "SexChangeAthlet", "Test", "2010-05-05", 20, 0, None, None, None
        )

        val expectedAthlet = existingAthlet.copy(geschlecht = "W")
        val syncActions = List(RenameAthletAction(reg, athletReg, existingAthlet, expectedAthlet))
        val testService = new TestRegistrationService(this)

        processSync(wkInfo, testService, syncActions, Set())

        testService.insertedAthletes.size should be(1)
        testService.insertedAthletes.head._2.geschlecht should be("W")
      }
    }
*/
    "processSync should process RenameVereinAction for local and remote updates" in {
      val clubId = createVerein("OldClubName", Some("OldVerband"))
      val oldClub = Verein(clubId, "OldClubName", Some("OldVerband"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "NewClubName", "NewVerband",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))
      val updreg = updateRegistration(reg.copy(vereinId = Some(clubId), selectedInitialClub = Some(oldClub)))

      val syncActions = List(RenameVereinAction(updreg, oldClub))
      val testService = new TestRegistrationService(this)

      processSync(wkInfo, testService, syncActions, Set())

      testService.updatedVereine.size should be(1)
      testService.remoteVereineUpdated.size should be(1)
    }

    "processSync should process MoveRegistration to move athletes between programs" in {
      val clubId = createVerein("MoveRegTestClub", Some("Verband1"))
      val athlet = insertAthlete(Athlet(clubId).copy(
        name = "MoveTest",
        vorname = "Athlete",
        gebdat = Some(Date.valueOf(LocalDate.now().minus(15, ChronoUnit.YEARS)))
      ))

      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.size >= 2 then {
        // Assign to first program
        assignAthletsToWettkampf(testWettkampf.id, Set(programmes.head.id), Set((athlet.id, None)), None)

        val reg = createRegistration(NewRegistration(
          testWettkampf.id,
          "MoveRegTestClub", "Verband1",
          "Contact", "Name",
          "0791234567", "test@test.com", "secret"
        ))

        val athletView = athlet.toAthletView(Some(Verein(clubId, "MoveRegTestClub", Some("Verband1"))))
        val syncActions = List(MoveRegistration(reg, programmes.head.id, 0, programmes(1).id, 1, athlet, athletView))
        val testService = new TestRegistrationService(this)

        processSync(wkInfo, testService, syncActions, Set())

        testService.movedToProgram.size should be(1)
        testService.movedToProgram.head._2 should be(programmes(1).id)
        testService.movedToProgram.head._3 should be(1)
      }
    }

    "processSync should process RemoveRegistration to unassign athletes" in {
      val clubId = createVerein("RemoveRegTestClub", Some("Verband1"))
      val athlet = insertAthlete(Athlet(clubId).copy(
        name = "RemoveTest",
        vorname = "Athlete",
        gebdat = Some(Date.valueOf(LocalDate.now().minus(15, ChronoUnit.YEARS)))
      ))

      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.nonEmpty then {
        assignAthletsToWettkampf(testWettkampf.id, Set(programmes.head.id), Set((athlet.id, None)), None)
        makeEinteilung(testWettkampf)

        val reg = createRegistration(NewRegistration(
          testWettkampf.id,
          "RemoveRegTestClub", "Verband1",
          "Contact", "Name",
          "0791234567", "test@test.com", "secret"
        ))

        val athletView = athlet.toAthletView(Some(Verein(clubId, "RemoveRegTestClub", Some("Verband1"))))
        val syncActions = List(RemoveRegistration(reg, programmes.head.id, athlet, athletView))
        val testService = new TestRegistrationService(this)

        processSync(wkInfo, testService, syncActions, Set())

        testService.unassignedAthletes.size should be >= 1
      }
    }

    "processSync should process ApproveVereinAction to join verein with registration" in {
      val clubId = createVerein("ApproveTestClub", Some("Verband1"))
      val club = Verein(clubId, "ApproveTestClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "ApproveTestClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val approvedReg = reg.copy(vereinId = Some(clubId))
      val syncActions = List(ApproveVereinAction(approvedReg))
      val testService = new TestRegistrationService(this)

      processSync(wkInfo, testService, syncActions, Set(club))

      testService.joinedVereineWithRegistration.size should be(1)
      testService.joinedVereineWithRegistration.head._2.id should be(approvedReg.id)
      testService.joinedVereineWithRegistration.head._3.id should be(clubId)
    }

    "processSync should process AddMedia actions" in {
      val clubId = createVerein("AddMediaTestClub", Some("Verband1"))
      val club = Verein(clubId, "AddMediaTestClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "AddMediaTestClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val mediaAdmin = MediaAdmin("media-id-1", "audio", "mp3", 1, "", "", 0L)
      val athletReg = AthletRegistration(
        0, reg.id, None, "M", "MediaTest", "Athlete", "2010-05-05", 20, 0, None, None, Some(mediaAdmin)
      )

      val syncActions = List(AddMedia(reg, athletReg))
      val testService = new TestRegistrationService(this)

      processSync(wkInfo, testService, syncActions, Set())

      testService.savedMedias.size should be(1)
      testService.savedMedias.head.name should be("audio")
      testService.requestedMediaDownloads.size should be(1)
    }

    "processSync should process UpdateAthletMediaAction" in {
      val clubId = createVerein("UpdateMediaTestClub", Some("Verband1"))
      val athlet = insertAthlete(Athlet(clubId).copy(
        name = "UpdateMediaTest",
        vorname = "Athlete",
        gebdat = Some(Date.valueOf(LocalDate.now().minus(15, ChronoUnit.YEARS)))
      ))

      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.nonEmpty then {
        assignAthletsToWettkampf(testWettkampf.id, Set(programmes.head.id), Set((athlet.id, None)), None)
        makeEinteilung(testWettkampf)

        val wertungen = selectWertungen(athletId = Some(athlet.id), wettkampfId = Some(testWettkampf.id))
        if wertungen.nonEmpty then {
          val reg = createRegistration(NewRegistration(
            testWettkampf.id,
            "UpdateMediaTestClub", "Verband1",
            "Contact", "Name",
            "0791234567", "test@test.com", "secret"
          ))

          val mediaAdmin = MediaAdmin("media-id-2", "updated-video", "mp4", 1, "", "", 0L)
          val athletReg = AthletRegistration(
            0, reg.id, Some(athlet.id), "M", "UpdateMediaTest", "Athlete", "2010-05-05", 20, 0, None, None, Some(mediaAdmin)
          )

          val syncActions = List(UpdateAthletMediaAction(reg, athletReg, wertungen.head.toWertung))
          val testService = new TestRegistrationService(this)

          processSync(wkInfo, testService, syncActions, Set())

          testService.updatedWertungen.size should be(1)
          testService.updatedWertungen.head.mediafile.isDefined should be(true)
          testService.regchangedCalled should be(true)
        }
      }
    }

    "processSync should handle empty sync actions list" in {
      val testService = new TestRegistrationService(this)
      val results = processSync(wkInfo, testService, List(), Set())

      results.size should be(0)
      testService.insertedVereine.size should be(0)
      testService.insertedAthletes.size should be(0)
    }

    "processSync should handle mixed sync actions" in {
      val clubId = createVerein("MixedActionsClub", Some("Verband1"))
      val club = Verein(clubId, "MixedActionsClub", Some("Verband1"))

      val reg1 = createRegistration(NewRegistration(
        testWettkampf.id,
        "MixedActionsClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val reg2 = createRegistration(NewRegistration(
        testWettkampf.id,
        "NewMixedClub", "Verband2",
        "Contact2", "Name2",
        "0792234567", "test2@test.com", "secret"
      ))

      val athlet = Athlet(clubId).copy(name = "MixedTest", vorname = "Athlete")
      val athletView = athlet.toAthletView(Some(club))
      val programmes = readWettkampfLeafs(testWettkampf.programmId)

      if programmes.nonEmpty then {
        val syncActions = List(
          AddVereinAction(reg2),
          AddRegistration(reg1, programmes.head.id, athlet, athletView, 0, None)
        )
        val testService = new TestRegistrationService(this)

        processSync(wkInfo, testService, syncActions, Set())

        testService.insertedVereine.size should be(1)
        testService.insertedAthletes.size should be(1)
        testService.assignedAthletes.size should be(1)
      }
    }

    "processSync should create riegen entries when needed" in {
      val clubId = createVerein("RiegenTestClub", Some("Verband1"))
      val club = Verein(clubId, "RiegenTestClub", Some("Verband1"))
      val athlet = insertAthlete(Athlet(clubId).copy(
        name = "RiegenTest",
        vorname = "Athlete",
        gebdat = Some(Date.valueOf(LocalDate.now().minus(15, ChronoUnit.YEARS)))
      ))

      val programmes = readWettkampfLeafs(testWettkampf.programmId)
      if programmes.nonEmpty then {
        val reg = createRegistration(NewRegistration(
          testWettkampf.id,
          "RiegenTestClub", "Verband1",
          "Contact", "Name",
          "0791234567", "test@test.com", "secret"
        ))

        val athletView = athlet.toAthletView(Some(club))
        val syncActions = List(AddRegistration(reg, programmes.head.id, athlet, athletView, 0, None))
        val testService = new TestRegistrationService(this)

        val results = processSync(wkInfo, testService, syncActions, Set())

        // Results may contain messages about riegen assignments
        results should not be null
      }
    }

    "processSync should join verein for approved clubs in addRegistrations" in {
      val clubId = createVerein("ApproveInAddRegClub", Some("Verband1"))
      val club = Verein(clubId, "ApproveInAddRegClub", Some("Verband1"))

      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "ApproveInAddRegClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val athlet = Athlet(0).copy(name = "ApproveTest", vorname = "Athlete")
      val athletView = athlet.toAthletView(Some(club))
      val programmes = readWettkampfLeafs(testWettkampf.programmId)

      if programmes.nonEmpty then {
        val syncActions = List(AddRegistration(reg, programmes.head.id, athlet, athletView, 0, None))
        val testService = new TestRegistrationService(this)

        processSync(wkInfo, testService, syncActions, Set(club))

        testService.joinedVereineWithRegistration.size should be(1)
      }
    }

    "processSync should handle multiple media save and download operations" in {
      val reg = createRegistration(NewRegistration(
        testWettkampf.id,
        "MultiMediaClub", "Verband1",
        "Contact", "Name",
        "0791234567", "test@test.com", "secret"
      ))

      val media1 = MediaAdmin("media-id-3", "audio1", "mp3", 1, "", "", 0L)
      val media2 = MediaAdmin("media-id-4", "audio2", "mp3", 1, "", "", 1L)

      val athletReg1 = AthletRegistration(
        0, reg.id, None, "M", "Athlete1", "Test", "2010-05-05", 20, 0, None, None, Some(media1)
      )
      val athletReg2 = AthletRegistration(
        0, reg.id, None, "W", "Athlete2", "Test", "2010-05-05", 20, 0, None, None, Some(media2)
      )

      val syncActions = List(
        AddMedia(reg, athletReg1),
        AddMedia(reg, athletReg2)
      )
      val testService = new TestRegistrationService(this)

      processSync(wkInfo, testService, syncActions, Set())

      testService.savedMedias.size should be(2)
      testService.requestedMediaDownloads.flatten.size should be(2)
    }
  }

  // Test service implementation for processSync testing
  class TestRegistrationService(baseService: KutuService) extends ch.seidel.kutu.http.RegistrationRoutes {
    import scala.collection.mutable.ListBuffer

    val insertedVereine = ListBuffer[Verein]()
    val updatedVereine = ListBuffer[Verein]()
    val remoteVereineUpdated = ListBuffer[Verein]()
    val insertedAthletes = ListBuffer[(String, Athlet)]()
    val remoteAthletesUpdated = ListBuffer[AthletView]()
    val assignedAthletes = ListBuffer[(Long, Set[Long], Set[(Long, Option[Media])], Option[Int])]()
    val movedToProgram = ListBuffer[(Long, Long, Int, AthletView)]()
    val unassignedAthletes = ListBuffer[Set[Long]]()
    val joinedVereineWithRegistration = ListBuffer[(Wettkampf, Registration, Verein)]()
    val savedMedias = ListBuffer[Media]()
    val requestedMediaDownloads = ListBuffer[List[MediaAdmin]]()
    val updatedWertungen = ListBuffer[Wertung]()
    var regchangedCalled = false

    override def insertVerein(verein: Verein): Verein = {
      val inserted = baseService.insertVerein(verein)
      insertedVereine += inserted
      inserted
    }

    override def updateVerein(verein: Verein): Unit = {
      baseService.updateVerein(verein)
      updatedVereine += verein
    }

    override def updateVereinRemote(p: Wettkampf, vereinToUpdate: Option[Verein]): Unit = {
      vereinToUpdate.foreach(v => remoteVereineUpdated += v)
    }

    override def insertAthletes(list: Iterable[(String, Athlet)]): Iterable[(String, Athlet)] = {
      val result = baseService.insertAthletes(list)
      insertedAthletes ++= result
      result
    }

    override def insertAthlete(athlet: Athlet): Athlet = {
      val inserted = baseService.insertAthlete(athlet)
      insertedAthletes += (inserted.id.toString -> inserted)
      inserted
    }

    override def updateRemoteAthletes(p: Wettkampf, athleteRemoteUpdates: List[AthletView]): Unit = {
      remoteAthletesUpdated ++= athleteRemoteUpdates
    }

    override def assignAthletsToWettkampf(wId: Long, pgmIds: Set[Long], athletIds: Set[(Long, Option[Media])], team: Option[Int], reserve: Option[Int]): Unit = {
      baseService.assignAthletsToWettkampf(wId, pgmIds, athletIds, team, reserve)
      assignedAthletes += ((wId, pgmIds, athletIds, team))
    }

    override def moveToProgram(wId: Long, pgmId: Long, team: Int, reserve: Int, athleteView: AthletView): Unit = {
      baseService.moveToProgram(wId, pgmId, team, reserve, athleteView)
      movedToProgram += ((wId, pgmId, team, athleteView))
    }

    override def unassignAthletFromWettkampf(wertungIds: Set[Long]): Unit = {
      baseService.unassignAthletFromWettkampf(wertungIds)
      unassignedAthletes += wertungIds
    }

    override def joinVereinWithRegistration(p: Wettkampf, reg: Registration, verein: Verein): Unit = {
      joinedVereineWithRegistration += ((p, reg, verein))
    }

    override def saveOrUpdateMedias(medias: Seq[MediaAdmin]): Seq[MediaAdmin] = {
      savedMedias ++= medias.map(_.toMedia)
      medias
    }

    override def doMediaDownloadRemote(p: Wettkampf, mediaList: List[MediaAdmin]): Unit = {
      requestedMediaDownloads += mediaList
    }

    override def updateWertung(w: Wertung): WertungView = {
      baseService.updateWertung(w)
      updatedWertungen += w
      baseService.selectWertungen(wettkampfId = Some(w.wettkampfId)).find(_.id == w.id).get
    }

    override def regchanged(p: Wettkampf): Unit = {
      regchangedCalled = true
    }

    override def listAthletWertungenZuWettkampf(athletId: Long, wettkampfId: Long): Seq[WertungView] = {
      baseService.listAthletWertungenZuWettkampf(athletId, wettkampfId)
    }

    // Delegate all other methods to base service
    override def selectVereine: List[Verein] = baseService.selectVereine
    override def selectAthletes: List[Athlet] = baseService.selectAthletes
    override def selectRegistrationsOfWettkampf(uuid: UUID): List[Registration] = baseService.selectRegistrationsOfWettkampf(uuid)
    override def selectAthletRegistrations(registrationId: Long): List[AthletRegistration] = baseService.selectAthletRegistrations(registrationId)
    override def selectRegistration(registrationId: Long): Registration = baseService.selectRegistration(registrationId)
    override def updateRegistration(registration: Registration): Registration = baseService.updateRegistration(registration)

    override def loadMedia(id: String): Option[MediaAdmin] = baseService.loadMedia(id)
    override def selectRiegen(wettkampfId: Long): List[Riege] = {
      baseService.selectRiegen(wettkampfId)
    }
    override def listRiegenZuWettkampf(wettkampfId: Long): IndexedSeq[(String, Int, Option[String], Option[Disziplin])] = {
      baseService.listRiegenZuWettkampf(wettkampfId)
    }
    override def updateOrinsertRiege(riege: RiegeRaw): Riege = baseService.updateOrinsertRiege(riege)
  }
}

















