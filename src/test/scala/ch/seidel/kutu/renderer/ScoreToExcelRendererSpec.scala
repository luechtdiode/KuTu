package ch.seidel.kutu.renderer

import ch.seidel.kutu.data.{GroupLeaf, GroupNode, TeamSums}
import ch.seidel.kutu.domain.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.ByteArrayInputStream
import java.sql.Date
import java.time.LocalDate

class ScoreToExcelRendererSpec extends AnyWordSpec with Matchers {

  private def createWertung(
      id: Long,
      firstName: String,
      lastName: String,
      endnote: BigDecimal,
      programmName: String = "P1",
      disziplinName: String = "Boden"
  ): WertungView = {
    val programm = ProgrammView(1L, programmName, 0, None, 1, 0, 99, "", 1, 0)
    val wettkampfView = WettkampfView(1L, None, Date.valueOf(LocalDate.now()), "Test-WK", programm, 0, BigDecimal(0), "", "", "", "", "", "")
    val disziplin = Disziplin(1L, disziplinName)
    val notenSpez = StandardWettkampf(1d)
    val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
    val verein = Verein(1L, "TV Test", Some("Verband Test"))
    val athlet = AthletView(id, 0, "M", lastName, firstName, Some(Date.valueOf(LocalDate.of(2010, 1, 1))), "", "", "", Some(verein), activ = true)

    WertungView(
      id = id,
      athlet = athlet,
      wettkampfdisziplin = wettkampfdisziplin,
      wettkampf = wettkampfView.toWettkampf,
      noteD = Some(BigDecimal(0)),
      noteE = Some(endnote),
      endnote = Some(endnote),
      riege = None,
      riege2 = None,
      team = 0,
      mediafile = None,
      variables = None
    )
  }

  private def createTeamLeaf(teamName: String, ruleName: String, memberId: Long, memberScore: BigDecimal): GroupLeaf[Team] = {
    val wertung = createWertung(memberId, s"M$memberId", s"Team$teamName", memberScore, programmName = "P-Team", disziplinName = "Sprung")
    val disziplin = wertung.wettkampfdisziplin.disziplin
    val team = Team(
      name = teamName,
      rulename = ruleName,
      wertungen = List(wertung),
      countingWertungen = Map(disziplin -> List(wertung)),
      relevantWertungen = Map(disziplin -> List(wertung)),
      aggregateFun = Sum
    )
    GroupLeaf(team, List(wertung), diszs = List(disziplin), aggreateFun = Sum)
  }

  "ScoreToExcelRenderer.toExcel" should {
    "export a simple GroupLeaf into a workbook with title, headers and data" in {
      val wertung = createWertung(1L, "Max", "Muster", BigDecimal("12.35"))
      val group = GroupLeaf(GenericGrouper("Einzelwertung"), List(wertung))

      val bytes = new ScoreToExcelRenderer().toExcel(List(group), sortAlphabetically = false, avgOnMultiCompetitions = true)
      bytes.length should be > 0

      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        workbook.getNumberOfSheets shouldBe 1
        val sheet = workbook.getSheetAt(0)

        sheet.getRow(0).getCell(0).getStringCellValue shouldBe "Rangliste"
        sheet.getRow(1).getCell(0).getStringCellValue shouldBe "Rang"
        sheet.getRow(2) should not be null
      }
      finally {
        workbook.close()
      }
    }

    "sanitize and truncate sheet names created from nested GroupNode paths" in {
      val wertung = createWertung(2L, "Lena", "Beispiel", BigDecimal("11.90"))
      val leaf = GroupLeaf(GenericGrouper("Kategorie [A]/B:*? mit sehr sehr langem Namen 1234567890"), List(wertung))
      val node = GroupNode(GenericGrouper("Root/Parent"), List(leaf))

      val bytes = new ScoreToExcelRenderer().toExcel(List(node), sortAlphabetically = false, avgOnMultiCompetitions = true)
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        workbook.getNumberOfSheets shouldBe 1
        val sheetName = workbook.getSheetName(0)
        sheetName.length should be <= 31
        sheetName.exists(ch => "[]:*?/\\".contains(ch)) shouldBe false
      }
      finally {
        workbook.close()
      }
    }

    "provide the same behavior via object API" in {
      val wertung = createWertung(3L, "Noah", "Tester", BigDecimal("10.80"))
      val group = GroupLeaf(GenericGrouper("ObjektAPI"), List(wertung))

      val bytes = ScoreToExcelRenderer.toExcel(List(group), sortAlphabetically = true, avgOnMultiCompetitions = true)
      bytes.length should be > 0

      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        workbook.getNumberOfSheets shouldBe 1
      }
      finally {
        workbook.close()
      }
    }

    "export TeamSums as summary and details sheets per rule" in {
      val teamA = createTeamLeaf("Team A", "Regel 1", 10L, BigDecimal("11.50"))
      val teamB = createTeamLeaf("Team B", "Regel 1", 11L, BigDecimal("11.20"))
      val teamSums = TeamSums(GenericGrouper("Teamwertung"), List(teamA, teamB))

      val bytes = new ScoreToExcelRenderer().toExcel(List(teamSums), sortAlphabetically = false, avgOnMultiCompetitions = true)
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        workbook.getNumberOfSheets shouldBe 2

        val names = (0 until workbook.getNumberOfSheets).map(workbook.getSheetName).toList
        names.exists(_.startsWith("Teamwertung")) shouldBe true
        names.exists(_.startsWith("Details")) shouldBe true

        val detailsSheet = workbook.getSheet(names.find(_.startsWith("Details")).get)
        detailsSheet.getRow(0).getCell(0).getStringCellValue shouldBe "Rangliste"
        detailsSheet.getRow(1).getCell(0).getStringCellValue shouldBe "Rang"
        detailsSheet.getRow(2) should not be null
      }
      finally {
        workbook.close()
      }
    }

    "create separate summary/details sheet pairs for each TeamSums rule group" in {
      val teamRule1 = createTeamLeaf("Team C", "R1", 12L, BigDecimal("10.90"))
      val teamRule2 = createTeamLeaf("Team D", "R2", 13L, BigDecimal("10.70"))
      val teamSums = TeamSums(GenericGrouper("Mehrkampf-Team"), List(teamRule1, teamRule2))

      val bytes = new ScoreToExcelRenderer().toExcel(List(teamSums), sortAlphabetically = false, avgOnMultiCompetitions = true)
      val workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))
      try {
        workbook.getNumberOfSheets shouldBe 4
        val names = (0 until workbook.getNumberOfSheets).map(workbook.getSheetName).toList
        names.count(_.startsWith("Details")) shouldBe 2
        names.exists(_.contains("R1")) shouldBe true
        names.exists(_.contains("R2")) shouldBe true
      }
      finally {
        workbook.close()
      }
    }
  }
}


