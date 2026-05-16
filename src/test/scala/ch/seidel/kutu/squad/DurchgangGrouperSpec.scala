package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DurchgangGrouperSpec extends AnyWordSpec with Matchers {

  private val disziplin = Disziplin(1L, "Boden")
  private val dummyWertung = Wertung(1L, 1L, 1L, 1L, "uuid", None, None, None, Some("R1"), None, Some(0), None, None)
  private val data: DurchgangStationZuteilung = Map(disziplin -> List("R1" -> Seq(dummyWertung)))

  "DurchgangGrouper" should {
    "group parallel rounds with disjoint categories under a shared title" in {
      val grouped = DurchgangGrouper.groupDurchgaengeByKategorien(Map(
        "K1 (1)" -> data,
        "K2 (1)" -> data,
        "K1 (2)" -> data,
        "K2 (2)" -> data
      ))

      grouped.find(_.name == "K1 (1)").get.title.shouldBe("Abteilung 1 K1-K2")
      grouped.find(_.name == "K2 (1)").get.title.shouldBe("Abteilung 1 K1-K2")
      grouped.find(_.name == "K1 (2)").get.title.shouldBe("Abteilung 2 K1-K2")
      grouped.find(_.name == "K2 (2)").get.title.shouldBe("Abteilung 2 K1-K2")
    }

    "keep the Durchgang name as title when no grouping is needed" in {
      val grouped = DurchgangGrouper.groupDurchgaengeByKategorien(Map("K1 (1)" -> data))

      grouped should have size 1
      grouped.head.title shouldBe "K1 (1)"
    }

    "support combined categories contained in the Durchgang name" in {
      val grouped = DurchgangGrouper.groupDurchgaengeByKategorien(Map(
        "K1 & K2 (1)" -> data,
        "K3 (1)" -> data
      ))

      grouped.find(_.name == "K1 & K2 (1)").get.title.shouldBe("Abteilung 1 K1-K2-K3")
      grouped.find(_.name == "K3 (1)").get.title.shouldBe("Abteilung 1 K1-K2-K3")
    }

    "expose the grouped output also as a title-name keyed map" in {
      val grouped = DurchgangGrouper.groupDurchgaengeByKategorienAsMap(Map(
        "K1 (1)" -> data,
        "K2 (1)" -> data
      ))

      grouped.keySet shouldBe Set(
        ("Abteilung 1 K1-K2", "K1 (1)"),
        ("Abteilung 1 K1-K2", "K2 (1)")
      )
    }

    "start a new Durchganggruppe when max parallel count is reached" in {
      val grouped = DurchgangGrouper.groupDurchgaengeByKategorien(Map(
        "K1 (1)" -> data,
        "K2 (1)" -> data,
        "K3 (1)" -> data,
        "K4 (1)" -> data
      ), maxParallelProGruppe = 3)

      grouped.find(_.name == "K1 (1)").get.title shouldBe "Abteilung 1 K1-K2-K3"
      grouped.find(_.name == "K2 (1)").get.title shouldBe "Abteilung 1 K1-K2-K3"
      grouped.find(_.name == "K3 (1)").get.title shouldBe "Abteilung 1 K1-K2-K3"
      grouped.find(_.name == "K4 (1)").get.title shouldBe "Abteilung 2 K4"
    }

    "keep unlimited grouping behavior when max parallel count is disabled" in {
      val grouped = DurchgangGrouper.groupDurchgaengeByKategorien(Map(
        "K1 (1)" -> data,
        "K2 (1)" -> data,
        "K3 (1)" -> data,
        "K4 (1)" -> data
      ), maxParallelProGruppe = 0)

      grouped.map(_.title).toSet shouldBe Set("Abteilung 1 K1-K2-K3-K4")
    }
  }
}


