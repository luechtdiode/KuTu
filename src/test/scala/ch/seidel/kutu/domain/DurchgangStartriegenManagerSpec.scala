package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.squad.DurchgangGrouper

import scala.compiletime.uninitialized

/**
 * Test coverage for DurchgangManager
 *
 * Tests all operations: rename, merge, move-to-group, ungroup, aggregate,
 * and the recalculateAbteilungTitles logic triggered by moveDurchgangToGroup.
 */
class DurchgangStartriegenManagerSpec extends KuTuBaseSpec {

  var testWettkampf: Wettkampf = uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()
    testWettkampf = insertGeTuWettkampf("TestDurchgangManagerWK", 3)
    makeEinteilung(testWettkampf)
  }

  private def getDurchgaenge(): Seq[SimpleDurchgang] =
    selectSimpleDurchgaenge(testWettkampf.id)

  private def getDurchgangNames(): Seq[String] =
    getDurchgaenge().map(_.name).distinct.sorted

  private def getDurchgangByTitle(title: String): Seq[SimpleDurchgang] =
    getDurchgaenge().filter(_.title == title)

  private def getDurchgangByName(name: String): Option[SimpleDurchgang] =
    getDurchgaenge().find(_.name == name)

  private var callbackCount = 0
  private val manager = new DurchgangStartriegenManager(this, () => callbackCount += 1)

  private def resetCallbackCount(): Unit = callbackCount = 0

  "DurchgangManager" should {

    "renameDurchgang change the name of a single durchgang" in {
      val names = getDurchgangNames()
      names should not be empty
      val originalName = names.head

      manager.renameDurchgang(testWettkampf.id, originalName, "RenamedDG")

      val renamed = getDurchgangByName("RenamedDG")
      renamed shouldBe defined
      renamed.get.name shouldBe "RenamedDG"

      getDurchgangByName(originalName) shouldBe None
    }

    "mergeDurchgaenge merge multiple sources into target" in {
      // First undo the rename from the previous test by recreating einteilung
      makeEinteilung(testWettkampf)

      val names = getDurchgangNames()
      names.length should be >= 2
      val source = names.head
      val target = names(1)

      manager.mergeDurchgaenge(testWettkampf.id, Set(source), target)

      val allNames = getDurchgangNames()
      allNames should not contain source
      allNames should contain(target)
    }

    "mergeDurchgaenge skips when source equals target" in {
      makeEinteilung(testWettkampf)

      val names = getDurchgangNames()
      names should not be empty
      val dg = names.head
      val before = getDurchgaenge().size

      manager.mergeDurchgaenge(testWettkampf.id, Set(dg), dg)

      getDurchgaenge().size shouldBe before
    }

    "moveDurchgangToGroup change the title of selected durchgänge" in {
      makeEinteilung(testWettkampf)

      val names = getDurchgangNames()
      names.length should be >= 2
      val toMove = names.take(2)

      manager.moveDurchgangToGroup(testWettkampf.id, toMove.toSet, "MyGroup")

      toMove.foreach { name =>
        val dg = getDurchgangByName(name)
        dg shouldBe defined
        dg.get.title shouldBe "MyGroup"
      }

      val notMoved = names.drop(2)
      notMoved.foreach { name =>
        val dg = getDurchgangByName(name)
        dg shouldBe defined
        dg.get.title should not be "MyGroup"
      }
    }

    "moveDurchgangToGroup recalculate Abteilung titles when categories change" in {
      makeEinteilung(testWettkampf)

      // First, manually set up two Abteilung groups with known durchgang names
      val names = getDurchgangNames()
      names.length should be >= 4

      // Create Abteilung 1 with first two throughgangs
      manager.aggregateDurchgaenge(testWettkampf.id, Set(names(0), names(1)), "Abteilung 1 (K1, K2)")
      // Create Abteilung 2 with remaining
      val remaining = names.drop(2)
      manager.aggregateDurchgaenge(testWettkampf.id, remaining.toSet, "Abteilung 2 (K3)")

      // Verify setup
      getDurchgangByTitle("Abteilung 1 (K1, K2)").map(_.name).toSet shouldBe Set(names(0), names(1))
      getDurchgangByTitle("Abteilung 2 (K3)").map(_.name).toSet shouldBe remaining.toSet

      // Move one throughgang from Abteilung 2 to Abteilung 1
      val movedDg = remaining.head
      manager.moveDurchgangToGroup(testWettkampf.id, Set(movedDg), "Abteilung 1 (K1, K2)")

      // The moved throughgang should now be in Abteilung 1
      val movedDgAfter = getDurchgangByName(movedDg)
      movedDgAfter shouldBe defined
      movedDgAfter.get.title shouldBe "Abteilung 1 (K1, K2, K3)"

      // All remaining throughgangs still exist
      val remainingAfter = remaining.drop(1)
      remainingAfter.foreach { name =>
        getDurchgangByName(name) shouldBe defined
      }
    }

    "ungroupDurchgaenge set title = name for selected durchgänge" in {
      makeEinteilung(testWettkampf)

      val names = getDurchgangNames()
      names should not be empty
      val toGroup = names.take(2)

      // First group them
      manager.aggregateDurchgaenge(testWettkampf.id, toGroup.toSet, "ToBeUngrouped")

      // Verify grouped
      toGroup.foreach { name =>
        getDurchgangByName(name).get.title shouldBe "ToBeUngrouped"
      }

      // Now ungroup
      manager.ungroupDurchgaenge(testWettkampf.id, toGroup.toSet)

      // Verify ungrouped
      toGroup.foreach { name =>
        val dg = getDurchgangByName(name)
        dg shouldBe defined
        dg.get.title shouldBe dg.get.name
      }
    }

    "aggregateDurchgaenge set a shared title for selected durchgänge" in {
      makeEinteilung(testWettkampf)

      val names = getDurchgangNames()
      names.length should be >= 3
      val toAggregate = names.take(3)

      manager.aggregateDurchgaenge(testWettkampf.id, toAggregate.toSet, "AggregatedGroup")

      toAggregate.foreach { name =>
        val dg = getDurchgangByName(name)
        dg shouldBe defined
        dg.get.title shouldBe "AggregatedGroup"
      }

      // Other throughgangs should not be affected
      val notAggregated = names.drop(3)
      notAggregated.foreach { name =>
        val dg = getDurchgangByName(name)
        dg shouldBe defined
        dg.get.title should not be "AggregatedGroup"
      }
    }

    "moveDurchgangToGroup recalculate category labels when moving into existing Abteilung" in {
      makeEinteilung(testWettkampf)

      val names = getDurchgangNames()
      names.length should be >= 3

      // Set up: Abteilung 1 with first throughgang, Abteilung 2 with second
      manager.aggregateDurchgaenge(testWettkampf.id, Set(names(0)), "Abteilung 1 (K1)")
      manager.aggregateDurchgaenge(testWettkampf.id, Set(names(1)), "Abteilung 2 (K2)")

      // Move throughgang 2 into Abteilung 1
      manager.moveDurchgangToGroup(testWettkampf.id, Set(names(1)), "Abteilung 1 (K1)")

      // The moved throughgang should be in Abteilung 1
      val movedDg = getDurchgangByName(names(1))
      movedDg shouldBe defined
      movedDg.get.title shouldBe "Abteilung 1 (K1, K2)"

      // Abteilung 1 should still exist with both throughgangs
      val abt1 = getDurchgangByTitle("Abteilung 1 (K1, K2)")
      abt1.map(_.name).toSet should contain(names(0))
      abt1.map(_.name).toSet should contain(names(1))
    }

    "moveDurchgangToGroup handle ungrouped throughgangs (title = name)" in {
      makeEinteilung(testWettkampf)

      val names = getDurchgangNames()
      names should not be empty
      val dg = names.head

      // Move an ungrouped throughgang to a new group
      manager.moveDurchgangToGroup(testWettkampf.id, Set(dg), "NewGroup")

      val movedDg = getDurchgangByName(dg)
      movedDg shouldBe defined
      movedDg.get.title shouldBe "NewGroup"
    }

    "getAllStartRiegen return non-empty list with correct fields after einteilung" in {
      makeEinteilung(testWettkampf)

      val riegen = manager.getAllStartRiegen(testWettkampf.id)

      riegen should not be empty
      riegen.foreach { item =>
        item.name should not be empty
        item.durchgang shouldBe defined
        item.startId shouldBe defined
        item.startName shouldBe defined
        item.athletCount should be >= 0
      }

      // At least some riegen should have athletes assigned
      riegen.exists(_.athletCount > 0) shouldBe true
    }

    "getAllStartRiegen return empty list for nonexistent wettkampf" in {
      val riegen = manager.getAllStartRiegen(-999L)
      riegen shouldBe empty
    }

    "setEmptyRiege create an empty riege entry and invoke callback" in {
      makeEinteilung(testWettkampf)

      val disziplinen = listDisziplinesZuWettkampf(testWettkampf.id)
      disziplinen should not be empty
      val startGeraet = disziplinen.head

      val names = getDurchgangNames()
      names should not be empty
      val durchgang = names.head

      resetCallbackCount()
      manager.setEmptyRiege(testWettkampf.id, durchgang, startGeraet)
      callbackCount shouldBe 1

      val riegen = manager.getAllStartRiegen(testWettkampf.id)
      val emptyRiege = riegen.find(r =>
        r.name == s"Leere Riege $durchgang/${startGeraet.name}" &&
        r.kind == RiegeRaw.KIND_EMPTY_RIEGE
      )
      emptyRiege shouldBe defined
      emptyRiege.get.durchgang shouldBe Some(durchgang)
      emptyRiege.get.startId shouldBe Some(startGeraet.id)
    }

    "setEmptyRiege not invoke callback when notify is false" in {
      makeEinteilung(testWettkampf)

      val disziplinen = listDisziplinesZuWettkampf(testWettkampf.id)
      val startGeraet = disziplinen.head
      val durchgang = getDurchgangNames().head

      resetCallbackCount()
      manager.setEmptyRiege(testWettkampf.id, durchgang, startGeraet, notify = false)
      callbackCount shouldBe 0
    }

    "deleteRiegen remove riegen and invoke callback" in {
      makeEinteilung(testWettkampf)

      val disziplinen = listDisziplinesZuWettkampf(testWettkampf.id)
      val startGeraet = disziplinen.head
      val durchgang = getDurchgangNames().head

      // Create an empty riege to delete
      manager.setEmptyRiege(testWettkampf.id, durchgang, startGeraet)
      val emptyName = s"Leere Riege $durchgang/${startGeraet.name}"

      val riegenBefore = manager.getAllStartRiegen(testWettkampf.id)
      riegenBefore.exists(_.name == emptyName) shouldBe true

      resetCallbackCount()
      manager.deleteRiegen(testWettkampf.id, List(emptyName))
      callbackCount shouldBe 1

      val riegenAfter = manager.getAllStartRiegen(testWettkampf.id)
      riegenAfter.exists(_.name == emptyName) shouldBe false
    }

    "deleteRiegen handle empty list gracefully" in {
      makeEinteilung(testWettkampf)

      resetCallbackCount()
      val before = manager.getAllStartRiegen(testWettkampf.id).size
      manager.deleteRiegen(testWettkampf.id, List.empty)
      callbackCount shouldBe 1

      manager.getAllStartRiegen(testWettkampf.id).size shouldBe before
    }

    "updateStartRiege update durchgang of existing riege" in {
      makeEinteilung(testWettkampf)

      val riegen = selectRiegenRaw(testWettkampf.id)
      riegen should not be empty

      val names = getDurchgangNames()
      names.length should be >= 2

      // Pick a riege from the first durchgang
      val riegeToUpdate = riegen.find(r => r.durchgang.contains(names.head)).get
      val newDurchgang = names(1)

      resetCallbackCount()
      val result = manager.updateStartRiege(riegeToUpdate.copy(durchgang = Some(newDurchgang)))
      callbackCount shouldBe 1

      result.name shouldBe riegeToUpdate.r
      result.durchgang shouldBe Some(newDurchgang)
    }

    "updateStartRiege update start geraet of existing riege" in {
      makeEinteilung(testWettkampf)

      val riegen = selectRiegenRaw(testWettkampf.id)
      val disziplinen = listDisziplinesZuWettkampf(testWettkampf.id)
      disziplinen.length should be >= 2

      // Find a riege and change its start geraet
      val riegeToUpdate = riegen.find(r => r.start.contains(disziplinen.head.id)).get
      val newStart = disziplinen(1)

      val result = manager.updateStartRiege(riegeToUpdate.copy(start = Some(newStart.id)))

      result.name shouldBe riegeToUpdate.r
      result.startId shouldBe Some(newStart.id)
      result.startName shouldBe Some(newStart.name)
    }

    "updateStartRiege create empty riege when last riege leaves a durchgang/start slot" in {
      makeEinteilung(testWettkampf)

      val riegen = selectRiegenRaw(testWettkampf.id).filter(_.kind == RiegeRaw.KIND_STANDARD)
      val names = getDurchgangNames()
      names.length should be >= 2

      // Find a durchgang/start combination with exactly one riege
      val grouped = riegen.groupBy(r => (r.durchgang, r.start))
      val singleSlot = grouped.find { case (_, members) => members.size == 1 }

      singleSlot match {
        case Some(((Some(durchgang), Some(startId)), singleRiege)) =>
          val newDurchgang = names.find(_ != durchgang).get
          val riegeToMove = singleRiege.head

          manager.updateStartRiege(riegeToMove.copy(durchgang = Some(newDurchgang)))

          // An empty riege should have been created for the vacated slot
          val allRiegen = manager.getAllStartRiegen(testWettkampf.id)
          val emptyRiegen = allRiegen.filter(r =>
            r.kind == RiegeRaw.KIND_EMPTY_RIEGE &&
            r.durchgang.contains(durchgang) &&
            r.startId.contains(startId)
          )
          emptyRiegen should not be empty

        case _ =>
          // If no single-riege slot exists, just verify updateStartRiege works without error
          val riegeToUpdate = riegen.head
          val newDurchgang = names.find(n => !riegeToUpdate.durchgang.contains(n)).get
          manager.updateStartRiege(riegeToUpdate.copy(durchgang = Some(newDurchgang)))
          succeed
      }
    }

    "callback is invoked for renameDurchgang" in {
      makeEinteilung(testWettkampf)

      val names = getDurchgangNames()
      names should not be empty

      resetCallbackCount()
      manager.renameDurchgang(testWettkampf.id, names.head, "CallbackTestDG")
      callbackCount shouldBe 1
    }
  }
}
