package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec

class RiegenServiceSpec extends KuTuBaseSpec {

  private def createAssignedAthleteWithOneWertung(wk: Wettkampf, suffix: String): WertungView = {
    val vereinId = createVerein(s"RiegenServiceClub-$suffix", Some("Spec"))
    val athlet = insertAthlete(Athlet(vereinId).copy(name = s"Athlet-$suffix"))
    val programmId = readWettkampfLeafs(wk.programmId).head.id
    assignAthletsToWettkampf(wk.id, Set(programmId), Set((athlet.id, None)), None)
    selectWertungen(athletId = Some(athlet.id), wettkampfId = Some(wk.id)).head
  }

  "RiegenService" should {
    "keep an existing riege when renaming to the same name" in {
      val wk = insertGeTuWettkampf("RiegenRenameSameNameWK", 2)
      updateOrinsertRiege(RiegeRaw(wk.id, "R-same", Some("DG-1"), Some(1L), RiegeRaw.KIND_STANDARD))

      val renamed = renameRiege(wk.id, "R-same", "R-same")

      renamed.r shouldBe "R-same"
      selectRiegenRaw(wk.id).count(_.r == "R-same") shouldBe 1
    }

    "create a new riege on rename when old name does not exist" in {
      val wk = insertGeTuWettkampf("RiegenRenameCreateWK", 2)

      val renamed = renameRiege(wk.id, "NichtVorhanden", "NeuRiege")

      renamed.r shouldBe "NeuRiege"
      selectRiegenRaw(wk.id).exists(_.r == "NeuRiege") shouldBe true
    }

    "rename existing riege and update both riege and riege2 references in wertungen" in {
      val wk = insertGeTuWettkampf("RiegenRenameExistingWK", 2)
      val wertungView = createAssignedAthleteWithOneWertung(wk, "rename")
      val diszId = wertungView.wettkampfdisziplin.disziplin.id

      updateOrinsertRiege(RiegeRaw(wk.id, "R-alt", Some("DG-1"), Some(diszId), RiegeRaw.KIND_STANDARD))
      updateOrinsertWertung(wertungView.toWertung.copy(riege = Some("R-alt"), riege2 = Some("R-alt")))

      val renamed = renameRiege(wk.id, "R-alt", "R-neu")
      val updatedWertungen = selectWertungen(athletId = Some(wertungView.athlet.id), wettkampfId = Some(wk.id))
      val targeted = updatedWertungen.filter(w => w.riege.contains("R-neu") || w.riege2.contains("R-neu"))

      renamed.r shouldBe "R-neu"
      targeted should not be empty
      targeted.forall(w => w.riege.contains("R-neu") || w.riege2.contains("R-neu")) shouldBe true
      selectRiegenRaw(wk.id).exists(_.r == "R-alt") shouldBe false
    }

    "find and reuse existing matching riege" in {
      val wk = insertGeTuWettkampf("RiegenFindMatchingWK", 2)
      updateOrinsertRiege(RiegeRaw(wk.id, "K2, Verein-1", Some("DG-x"), Some(1L), RiegeRaw.KIND_STANDARD))

      val matchResult = findAndStoreMatchingRiege(
        RiegeRaw(wk.id, "K2, Verein-1", Some("DG-new"), Some(2L), RiegeRaw.KIND_STANDARD)
      )

      matchResult.r shouldBe "K2, Verein-1"
      matchResult.durchgang shouldBe Some("DG-x")
      matchResult.start shouldBe Some(1L)
    }

    "remove unused standard riegen when cleaning" in {
      val wk = insertGeTuWettkampf("RiegenCleanUnusedWK", 2)
      val wertungView = createAssignedAthleteWithOneWertung(wk, "cleanup")

      updateOrinsertRiege(RiegeRaw(wk.id, "UsedR", Some("DG-Used"), Some(1L), RiegeRaw.KIND_STANDARD))
      updateOrinsertRiege(RiegeRaw(wk.id, "UnusedR", Some("DG-Unused"), Some(1L), RiegeRaw.KIND_STANDARD))
      updateOrinsertWertung(wertungView.toWertung.copy(riege = Some("UsedR"), riege2 = None))

      cleanUnusedRiegen(wk.id)

      val remaining = selectRiegenRaw(wk.id).map(_.r).toSet
      remaining.contains("UsedR") shouldBe true
      remaining.contains("UnusedR") shouldBe false
    }

    "not auto-create derived riege2 entries for insertRiegenWertungen when deriveRiege2Barren is disabled" in {
      val wk = insertGeTuWettkampf("RiegenInsertNoDeriveWK", 2)
      val wertungView = createAssignedAthleteWithOneWertung(wk, "insert")

      val mainRiege = s"Main-${System.currentTimeMillis()}"
      val riege2Name = s"Barren-${System.currentTimeMillis()}"
      val throughgang = Some("DG-Insert")
      val withRiege2 = wertungView.toWertung.copy(riege2 = Some(riege2Name))

      insertRiegenWertungen(
        RiegeRaw(wk.id, mainRiege, throughgang, Some(1L), RiegeRaw.KIND_STANDARD),
        Seq(withRiege2),
        deriveRiege2Barren = false
      )

      val storedRiegen = selectRiegenRaw(wk.id).map(_.r).toSet
      storedRiegen.contains(mainRiege) shouldBe true
      storedRiegen.contains(riege2Name) shouldBe false
    }
  }
}

