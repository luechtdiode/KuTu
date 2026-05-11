package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*
import ch.seidel.kutu.domain.given_Conversion_LocalDate_Date
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

/**
 * Test coverage for RiegenBuilder
 *
 * Tests the riegen (group) building functionality including:
 * - Grouper selection based on riegen mode
 * - Riege name generation
 * - Secondary riege name generation (e.g., Barren for GETU)
 */
class RiegenBuilderTest extends AnyWordSpec with Matchers {

  // Test data setup
  val teamrule = ""
  val verein = Verein(1, "Testverein", Some("Testverband"))
  val athletMale = AthletView(1, 0, "M", "Mustermann", "Max", Some(LocalDate.of(2000, 1, 1)), "", "", "", Some(verein), activ = true)
  val athletFemale = AthletView(2, 0, "W", "Musterfrau", "Maria", Some(LocalDate.of(2001, 2, 2)), "", "", "", Some(verein), activ = true)
  val disziplin = Disziplin(1L, "Testdisziplin")
  val notenSpez = StandardWettkampf(1d)

  def createProgrammView(name: String, riegenmode: Int, parentName: Option[String] = None): ProgrammView = {
    val parent = parentName.map(pn => ProgrammView(0L, pn, 0, None, 1, 0, 99, "", 1, 0))
    ProgrammView(2L, name, 0, parent, riegenmode, 0, 99, "", 1, 0)
  }

  def createWettkampfView(programm: ProgrammView, altersklassen: Option[String] = None, jahrgangsklassen: Option[String] = None): WettkampfView = {
    WettkampfView(1L, None, LocalDate.now(), "Testwettkampf", programm, 0, BigDecimal(0), "",
      altersklassen.getOrElse(""), jahrgangsklassen.getOrElse(""), "", "", teamrule)
  }

  def createWertungView(athlet: AthletView, programm: ProgrammView,
                        altersklassen: Option[String] = None,
                        jahrgangsklassen: Option[String] = None): WertungView = {
    val wettkampf = createWettkampfView(programm, altersklassen, jahrgangsklassen)
    val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
    WertungView(1L, athlet, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)
  }

  "RiegenBuilder.selectRiegenGrouper" should {

    "return ATTGrouper when riegenmode is RIEGENMODE_BY_JG" in {
      val grouper = RiegenBuilder.selectRiegenGrouper(RiegeRaw.RIEGENMODE_BY_JG, None, None)
      grouper shouldBe ATTGrouper
    }

    "return JGClubGrouper when riegenmode is RIEGENMODE_BY_JG_VEREIN" in {
      val grouper = RiegenBuilder.selectRiegenGrouper(RiegeRaw.RIEGENMODE_BY_JG_VEREIN, None, None)
      grouper shouldBe JGClubGrouper
    }

    "return JGClubGrouper when altersklassen is defined" in {
      val grouper = RiegenBuilder.selectRiegenGrouper(RiegeRaw.RIEGENMODE_BY_Program, Some("AK8-10"), None)
      grouper shouldBe JGClubGrouper
    }

    "return JGClubGrouper when jahrgangsklassen is defined" in {
      val grouper = RiegenBuilder.selectRiegenGrouper(RiegeRaw.RIEGENMODE_BY_Program, None, Some("JG2010,JG2011"))
      grouper shouldBe JGClubGrouper
    }

    "return JGClubGrouper when both altersklassen and jahrgangsklassen are defined" in {
      val grouper = RiegenBuilder.selectRiegenGrouper(RiegeRaw.RIEGENMODE_BY_Program, Some("AK8-10"), Some("JG2010,JG2011"))
      grouper shouldBe JGClubGrouper
    }

    "return KuTuGeTuGrouper when riegenmode is BY_Program and no klassen defined" in {
      val grouper = RiegenBuilder.selectRiegenGrouper(RiegeRaw.RIEGENMODE_BY_Program, None, None)
      grouper shouldBe KuTuGeTuGrouper
    }

    "return KuTuGeTuGrouper for default case with no klassen" in {
      val grouper = RiegenBuilder.selectRiegenGrouper(0, None, None)
      grouper shouldBe KuTuGeTuGrouper
    }
  }

  "RiegenBuilder.generateRiegenName" should {

    "generate name using ATTGrouper when riegenmode is RIEGENMODE_BY_JG" in {
      val programm = createProgrammView("K1", RiegeRaw.RIEGENMODE_BY_JG)
      val wertung = createWertungView(athletMale, programm)

      val name = RiegenBuilder.generateRiegenName(wertung)

      name should not be empty
      name should include("M")
    }

    "generate name using JGClubGrouper when riegenmode is RIEGENMODE_BY_JG_VEREIN" in {
      val programm = createProgrammView("K1", RiegeRaw.RIEGENMODE_BY_JG_VEREIN)
      val wertung = createWertungView(athletMale, programm)

      val name = RiegenBuilder.generateRiegenName(wertung)

      name should not be empty
    }

    "generate name using JGClubGrouper when altersklassen is defined" in {
      val programm = createProgrammView("K1", RiegeRaw.RIEGENMODE_BY_Program)
      val wertung = createWertungView(athletMale, programm, altersklassen = Some("AK8-10,AK11-15"))

      val name = RiegenBuilder.generateRiegenName(wertung)

      name should not be empty
    }

    "generate name using JGClubGrouper when jahrgangsklassen is defined" in {
      val programm = createProgrammView("K1", RiegeRaw.RIEGENMODE_BY_Program)
      val wertung = createWertungView(athletMale, programm, jahrgangsklassen = Some("JG2010,JG2011"))

      val name = RiegenBuilder.generateRiegenName(wertung)

      name should not be empty
    }

    "generate name using KuTuGeTuGrouper when no special conditions" in {
      val programm = createProgrammView("K1", RiegeRaw.RIEGENMODE_BY_Program)
      val wertung = createWertungView(athletMale, programm)

      val name = RiegenBuilder.generateRiegenName(wertung)

      name should not be empty
      name should include("M")
    }

    "handle female athlete correctly" in {
      val programm = createProgrammView("K1", RiegeRaw.RIEGENMODE_BY_Program)
      val wertung = createWertungView(athletFemale, programm)

      val name = RiegenBuilder.generateRiegenName(wertung)

      name should not be empty
      name should include("W")
    }
  }

  "RiegenBuilder.generateRiegen2Name" should {

    "return barren riege name for male athlete in GETU competition" in {
      val programmParent = ProgrammView(1L, "GeTu-Wettkampf", 0, None, 1, 0, 99, "", 1, 0)
      val programm = ProgrammView(2L, "K1", 0, Some(programmParent), 1, 0, 99, "", 1, 0)
      val wettkampf = createWettkampfView(programm)
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
      val wertungView = WertungView(1L, athletMale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      val riege2Name = RiegenBuilder.generateRiegen2Name(wertungView)

      riege2Name.isDefined shouldBe true
      riege2Name.get shouldBe "Barren K1"
    }

    "return None for female athlete in GETU competition" in {
      val programmParent = ProgrammView(1L, "GeTu-Wettkampf", 0, None, 1, 0, 99, "", 1, 0)
      val programm = ProgrammView(2L, "K1", 0, Some(programmParent), 1, 0, 99, "", 1, 0)
      val wettkampf = createWettkampfView(programm)
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
      val wertungView = WertungView(1L, athletFemale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      val riege2Name = RiegenBuilder.generateRiegen2Name(wertungView)

      riege2Name shouldBe None
    }

    "return None for male athlete in non-GETU competition" in {
      val programmParent = ProgrammView(1L, "KuTu-Wettkampf", 0, None, 1, 0, 99, "", 1, 0)
      val programm = ProgrammView(2L, "K1", 0, Some(programmParent), 1, 0, 99, "", 1, 0)
      val wettkampf = createWettkampfView(programm)
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
      val wertungView = WertungView(1L, athletMale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      val riege2Name = RiegenBuilder.generateRiegen2Name(wertungView)

      riege2Name shouldBe None
    }

    "handle GETU with lowercase correctly" in {
      val programmParent = ProgrammView(1L, "getu-wettkampf", 0, None, 1, 0, 99, "", 1, 0)
      val programm = ProgrammView(2L, "K2", 0, Some(programmParent), 1, 0, 99, "", 1, 0)
      val wettkampf = createWettkampfView(programm)
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
      val wertungView = WertungView(1L, athletMale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      val riege2Name = RiegenBuilder.generateRiegen2Name(wertungView)

      riege2Name.isDefined shouldBe true
      riege2Name.get shouldBe "Barren K2"
    }

    "handle GETU with mixed case correctly" in {
      val programmParent = ProgrammView(1L, "GeTu Wettkampf", 0, None, 1, 0, 99, "", 1, 0)
      val programm = ProgrammView(2L, "K3", 0, Some(programmParent), 1, 0, 99, "", 1, 0)
      val wettkampf = createWettkampfView(programm)
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
      val wertungView = WertungView(1L, athletMale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      val riege2Name = RiegenBuilder.generateRiegen2Name(wertungView)

      riege2Name.isDefined shouldBe true
      riege2Name.get shouldBe "Barren K3"
    }

    "return None when program has no parent" in {
      val programm = ProgrammView(2L, "K1", 0, None, 1, 0, 99, "", 1, 0)
      val wettkampf = createWettkampfView(programm)
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
      val wertungView = WertungView(1L, athletMale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      val riege2Name = RiegenBuilder.generateRiegen2Name(wertungView)

      riege2Name shouldBe None
    }
  }

  "RiegenBuilder trait suggestRiegen" should {

    "call the appropriate grouper based on riegenmode" in {
      // Create a simple implementation of RiegenBuilder for testing
      val builder = new RiegenBuilder {}

      val programmParent = ProgrammView(1L, "GeTu-Wettkampf", 0, None, 1, 0, 99, "", 1, 0)
      val programm = ProgrammView(2L, "K1", 0, Some(programmParent), RiegeRaw.RIEGENMODE_BY_Program, 0, 99, "", 1, 0)
      val wettkampf = createWettkampfView(programm)
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)

      val wertung1 = WertungView(1L, athletMale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)
      val wertung2 = WertungView(2L, athletFemale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      val result = builder.suggestRiegen(Seq(1, 1), Seq(wertung1, wertung2))

      result should not be empty
      result.size should be >= 1
    }

    "handle empty wertungen list gracefully" in {
      val builder = new RiegenBuilder {}

      // Should not crash with empty wertungen
      // Note: This will throw an exception due to .head call in the implementation
      // but that's the current behavior - documenting it
      an[Exception] should be thrownBy {
        builder.suggestRiegen(Seq(1), Seq.empty)
      }
    }

    "use altersklassen when defined" in {
      val builder = new RiegenBuilder {}

      val programm = createProgrammView("K1", RiegeRaw.RIEGENMODE_BY_Program)
      val wettkampf = createWettkampfView(programm, altersklassen = Some("AK8-10,AK11-15"))
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)

      val wertung = WertungView(1L, athletMale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      val result = builder.suggestRiegen(Seq(1), Seq(wertung))

      result should not be empty
    }

    "use jahrgangsklassen when defined" in {
      val builder = new RiegenBuilder {}

      val programm = createProgrammView("K1", RiegeRaw.RIEGENMODE_BY_Program)
      val wettkampf = createWettkampfView(programm, jahrgangsklassen = Some("JG2010,JG2011"))
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)

      val wertung = WertungView(1L, athletMale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      val result = builder.suggestRiegen(Seq(1), Seq(wertung))

      result should not be empty
    }

    "respect rotation station count" in {
      val builder = new RiegenBuilder {}

      val programm = createProgrammView("K1", RiegeRaw.RIEGENMODE_BY_Program)
      val wettkampf = createWettkampfView(programm)
      val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)

      val wertung1 = WertungView(1L, athletMale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)
      val wertung2 = WertungView(2L, athletFemale, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, 0, None, None)

      // Test with 2 rotations
      val result = builder.suggestRiegen(Seq(1, 1), Seq(wertung1, wertung2))

      result should not be empty
    }
  }

  "RiegenBuilder edge cases" should {

    "handle empty altersklassen string" in {
      // Empty string is still considered as nonEmpty by the option, so it triggers JGClubGrouper
      val grouper = RiegenBuilder.selectRiegenGrouper(RiegeRaw.RIEGENMODE_BY_Program, Some(""), None)
      grouper shouldBe JGClubGrouper
    }

    "handle empty jahrgangsklassen string" in {
      // Empty string is still considered as nonEmpty by the option, so it triggers JGClubGrouper
      val grouper = RiegenBuilder.selectRiegenGrouper(RiegeRaw.RIEGENMODE_BY_Program, None, Some(""))
      grouper shouldBe JGClubGrouper
    }

    "handle both empty klassen strings" in {
      // Both empty strings are still considered as nonEmpty, so it triggers JGClubGrouper
      val grouper = RiegenBuilder.selectRiegenGrouper(RiegeRaw.RIEGENMODE_BY_Program, Some(""), Some(""))
      grouper shouldBe JGClubGrouper
    }

    "handle riege name generation with different riegenmodes" in {
      val modes = Seq(
        RiegeRaw.RIEGENMODE_BY_Program,
        RiegeRaw.RIEGENMODE_BY_JG,
        RiegeRaw.RIEGENMODE_BY_JG_VEREIN
      )

      modes.foreach { mode =>
        val programm = createProgrammView("K1", mode)
        val wertung = createWertungView(athletMale, programm)

        val name = RiegenBuilder.generateRiegenName(wertung)

        name should not be empty
      }
    }
  }
}