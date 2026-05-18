package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*
import ch.seidel.kutu.domain.given_Conversion_LocalDate_Date
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class TurnerRiegenBuilderSpec extends AnyWordSpec with Matchers {

  object Harness extends TurnerRiegenBuilder {
    def build(
        wertungen: Map[AthletView, Seq[WertungView]],
        grp: List[WertungView => String],
        grpAll: List[WertungView => String]): Seq[(String, Seq[WertungViewsZuAthletView])] =
      buildTurnerRiegen(wertungen, grp, grpAll)
  }

  private val vereinA = Verein(1, "TV A", Some("ZH"))
  private val vereinB = Verein(2, "TV B", Some("ZH"))
  private val disziplin = Disziplin(1L, "Boden")
  private val notenSpez = StandardWettkampf(1d)

  private def programm(name: String = "K1", riegenmode: Int = RiegeRaw.RIEGENMODE_BY_Program): ProgrammView =
    ProgrammView(1L, name, 0, None, riegenmode, 0, 99, "", 1, 0)

  private def wettkampf(programm: ProgrammView): Wettkampf =
    WettkampfView(1L, None, LocalDate.now(), "WK", programm, 0, BigDecimal(0), "", "", "", "", "", "").toWettkampf

  private def wertung(athlet: AthletView, programm: ProgrammView = programm(), riege: Option[String] = None): WertungView = {
    val wk = wettkampf(programm)
    val wkd = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
    WertungView(1L + athlet.id, athlet, wkd, wk, None, None, None, riege, None, 0, None, None)
  }

  private def athlet(id: Long, sex: String, verein: Verein, year: Int): AthletView =
    AthletView(id, 0, sex, s"Name$id", s"Vor$id", Some(LocalDate.of(year, 1, 1)), "", "", "", Some(verein), activ = true)

  "TurnerRiegenBuilder" should {
    "group athletes by the provided short grouper" in {
      val a1 = athlet(1, "M", vereinA, 2010)
      val a2 = athlet(2, "M", vereinA, 2011)
      val a3 = athlet(3, "W", vereinB, 2010)

      val grouped = Harness.build(
        Map(
          a1 -> Seq(wertung(a1)),
          a2 -> Seq(wertung(a2)),
          a3 -> Seq(wertung(a3))
        ),
        grp = List(_.athlet.geschlecht),
        grpAll = List(_.athlet.geschlecht, _.athlet.name)
      )

      grouped.map(_._1).toSet shouldBe Set("M", "W")
      grouped.find(_._1 == "M").get._2.map(_._1.id).toSet.shouldBe(Set(1L, 2L))
      grouped.find(_._1 == "W").get._2.map(_._1.id).shouldBe(Seq(3L))
    }

    "keep each athlete only once even if multiple Wertungen are present" in {
      val a1 = athlet(1, "M", vereinA, 2010)
      val p = programm("K2")
      val w1 = wertung(a1, p)
      val w2 = wertung(a1, p).copy(id = 99L, wettkampfdisziplin = WettkampfdisziplinView(2L, p, Disziplin(2L, "Sprung"), "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1))

      val grouped = Harness.build(
        Map(a1 -> Seq(w1, w2)),
        grp = List(_.athlet.geschlecht, _.wettkampfdisziplin.programm.name),
        grpAll = List(_.athlet.geschlecht, _.wettkampfdisziplin.programm.name, _.athlet.name)
      )

      grouped should have size 1
      grouped.head._2 should have size 1
      grouped.head._2.head._2.map(_.id).toSet shouldBe Set(2L, 99L)
    }
  }
}



