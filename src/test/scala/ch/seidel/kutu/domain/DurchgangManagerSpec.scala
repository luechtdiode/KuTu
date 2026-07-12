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
class DurchgangManagerSpec extends KuTuBaseSpec {

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

  private val manager = new DurchgangManager(this)

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
  }
}
