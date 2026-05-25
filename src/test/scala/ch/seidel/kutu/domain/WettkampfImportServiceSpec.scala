package ch.seidel.kutu.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.sql.Date

class WettkampfImportServiceSpec extends AnyWordSpec with Matchers {

  private val testProgramm = ProgrammView(
    id = 10L,
    name = "P-Test",
    aggregate = 0,
    parent = None,
    ord = 1,
    alterVon = 7,
    alterBis = 18,
    uuid = "pgm-uuid",
    riegenmode = 0,
    bestOfCount = 0
  )

  private val testWettkampf = WettkampfView(
    id = 99L,
    uuid = Some("wk-uuid"),
    datum = Date.valueOf("2026-01-01"),
    titel = "WK Test",
    programm = testProgramm,
    auszeichnung = 0,
    auszeichnungendnote = BigDecimal(0),
    notificationEMail = "",
    altersklassen = "",
    jahrgangsklassen = "",
    punktegleichstandsregel = "",
    rotation = "",
    teamrule = ""
  )

  private val service = new WettkampfImportService(null.asInstanceOf[KutuService], testWettkampf)

  "WettkampfImportService.assignSelectedVerein" should {
    "set selected verein on Athlet and AthletView rows" in {
      val verein = Verein(42L, "TV Test", Some("ZH"))
      val row = service.ImportRow(
        progId = 100L,
        athlet = Athlet(verein.id).copy(name = "Muster", vorname = "Max"),
        athletView = AthletView(0, 0, "M", "Muster", "Max", None, "", "", "", None, activ = true),
        oldProg = 0L,
        team = 0
      )

      val assigned = service.assignSelectedVerein(Seq(row), verein)

      assigned should have size 1
      assigned.head.athlet.verein shouldBe Some(42L)
      assigned.head.athletView.verein.map(_.id) shouldBe Some(42L)
      assigned.head.athletView.verein.map(_.name) shouldBe Some("TV Test")
    }
  }

}
