package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class RiegenBuilderTest extends AnyWordSpec
with Matchers {

  "RiegenBuilder.generateRiegen2Name" should {
    val teamrule = ""
    val programmParent = ProgrammView(1L, "GeTu-Wettkampf", 0, None, 1, 0, 99, "", 1, 0)
    val programm = ProgrammView(2L, "K1", 0, Some(programmParent), 1, 0, 99, "", 1, 0)
    val wettkampf = WettkampfView(1L, None, LocalDate.now(), "Testwettkampf", programm, 0, BigDecimal(0), "", "", "", "", "", teamrule)
    val disziplin = Disziplin(1L, "Testdisziplin")
    val notenSpez = StandardWettkampf(1d)
    val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
    val verein = Verein(1, "Testverein", Some("Testverband"))
    val athlet = AthletView(1, 0, "M", "Mustermann", "Max", Some(LocalDate.of(2000, 1, 1)), "", "", "", Some(verein), activ = true)

    "return barren riege if athlet is masculine and wettkampfdisziplin is GETU" in {
      val wertungView = WertungView(1L, athlet, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)
      val riege2Name = RiegenBuilder.generateRiegen2Name(wertungView)
      riege2Name.isDefined shouldBe true
      riege2Name.get shouldBe "Barren K1"
    }
  }
}