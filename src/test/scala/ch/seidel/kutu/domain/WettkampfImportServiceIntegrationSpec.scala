package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec

class WettkampfImportServiceIntegrationSpec extends KuTuBaseSpec {

  "WettkampfImportService.prepareStructuredPreviewRows" should {
    "build preview rows and include removal entries when configured" in {
      val wettkampf = insertGeTuWettkampf(s"ImportService-${System.currentTimeMillis()}", anzvereine = 1)
      val programms = readWettkampfLeafs(wettkampf.programmId)
      val wettkampfView = wettkampf.toView(programms.head)
      val importService = new WettkampfImportService(this, wettkampfView)

      val rowfields = Seq(
        Map(
          "NAME" -> "Neu",
          "VORNAME" -> "Max",
          "JAHRGANG" -> "2012",
          "KATEGORIE" -> "",
          "VERBAND" -> "",
          "VEREIN" -> "",
          "RLZ_TZ" -> "",
          "VERBAND_RLZ" -> "",
          "GESCHLECHT" -> "F"
        )
      )

      val existingAthlets = Seq(
        AthletView(
          id = 0,
          js_id = 0,
          geschlecht = "M",
          name = "Alt",
          vorname = "Athlet",
          gebdat = None,
          strasse = "",
          plz = "",
          ort = "",
          verein = None,
          activ = true
        )
      )

      val result = importService.prepareStructuredPreviewRows(
        rowfields = rowfields,
        programms = programms,
        progrm = Some(programms.head),
        options = importService.StructuredPreviewOptions(
          removeMissingAthletes = true,
          fixedVerein = None,
          genderValueMapping = Map("F" -> "W"),
          resolveVereinFromFields = true
        ),
        context = importService.StructuredPreviewContext(
          existingAthlets = existingAthlets,
          existingProgramsByAthletEasyprint = Map.empty,
          existingTeamsByAthletEasyprint = Map.empty
        )
      )

      val imported = result.find(_._2.name == "Neu").getOrElse(fail("Expected imported row for athlete Neu"))
      imported._1 shouldBe programms.head.id
      imported._2.geschlecht shouldBe "W"

      result.exists(r => r._1 == 0L && r._3.name == "Alt" && r._3.vorname == "Athlet") shouldBe true
    }

    "not create removal rows when removeMissingAthletes is disabled" in {
      val wettkampf = insertGeTuWettkampf(s"ImportServiceNoRemove-${System.currentTimeMillis()}", anzvereine = 1)
      val programms = readWettkampfLeafs(wettkampf.programmId)
      val wettkampfView = wettkampf.toView(programms.head)
      val importService = new WettkampfImportService(this, wettkampfView)

      val result = importService.prepareStructuredPreviewRows(
        rowfields = Seq(
          Map(
            "NAME" -> "Neu",
            "VORNAME" -> "OhneRemove",
            "JAHRGANG" -> "2013",
            "KATEGORIE" -> "",
            "VERBAND" -> "",
            "VEREIN" -> "",
            "RLZ_TZ" -> "",
            "VERBAND_RLZ" -> "",
            "GESCHLECHT" -> "M"
          )
        ),
        programms = programms,
        progrm = Some(programms.head),
        options = importService.StructuredPreviewOptions(
          removeMissingAthletes = false,
          fixedVerein = None,
          genderValueMapping = Map("M" -> "M"),
          resolveVereinFromFields = true
        ),
        context = importService.StructuredPreviewContext(
          existingAthlets = Seq(AthletView(0, 0, "M", "Alt", "Athlet", None, "", "", "", None, activ = true)),
          existingProgramsByAthletEasyprint = Map.empty,
          existingTeamsByAthletEasyprint = Map.empty
        )
      )

      result.exists(_._1 == 0L) shouldBe false
      result.exists(_._2.vorname == "OhneRemove") shouldBe true
    }

    "prefer fixedVerein when provided" in {
      val wettkampf = insertGeTuWettkampf(s"ImportServiceFixedVerein-${System.currentTimeMillis()}", anzvereine = 1)
      val programms = readWettkampfLeafs(wettkampf.programmId)
      val wettkampfView = wettkampf.toView(programms.head)
      val importService = new WettkampfImportService(this, wettkampfView)
      val fixed = Verein(77L, "TV Fixed", Some("ZH"))

      val result = importService.prepareStructuredPreviewRows(
        rowfields = Seq(
          Map(
            "NAME" -> "Neu",
            "VORNAME" -> "Fixed",
            "JAHRGANG" -> "2014",
            "KATEGORIE" -> "",
            "VERBAND" -> "Irrelevant",
            "VEREIN" -> "Wird ignoriert",
            "RLZ_TZ" -> "",
            "VERBAND_RLZ" -> "",
            "GESCHLECHT" -> "M"
          )
        ),
        programms = programms,
        progrm = Some(programms.head),
        options = importService.StructuredPreviewOptions(
          removeMissingAthletes = false,
          fixedVerein = Some(fixed),
          genderValueMapping = Map("M" -> "M"),
          resolveVereinFromFields = false
        ),
        context = importService.StructuredPreviewContext(Seq.empty, Map.empty, Map.empty)
      )

      result should have size 1
      result.head._2.verein shouldBe Some(77L)
      result.head._3.verein.map(_.id) shouldBe Some(77L)
      result.head._3.verein.map(_.name) shouldBe Some("TV Fixed")
    }

    "drop unchanged rows when old and new program are identical" in {
      val wettkampf = insertGeTuWettkampf(s"ImportServiceUnchanged-${System.currentTimeMillis()}", anzvereine = 1)
      val programms = readWettkampfLeafs(wettkampf.programmId)
      val wettkampfView = wettkampf.toView(programms.head)
      val importService = new WettkampfImportService(this, wettkampfView)

      val expectedAthlet = AthletView(
        id = 0,
        js_id = 0,
        geschlecht = "M",
        name = "Neu",
        vorname = "GleichesProgramm",
        gebdat = Some(getSQLDate("01.01.2015")),
        strasse = "",
        plz = "",
        ort = "",
        verein = None,
        activ = true
      )

      val result = importService.prepareStructuredPreviewRows(
        rowfields = Seq(
          Map(
            "NAME" -> "Neu",
            "VORNAME" -> "GleichesProgramm",
            "JAHRGANG" -> "2015",
            "KATEGORIE" -> "",
            "VERBAND" -> "",
            "VEREIN" -> "",
            "RLZ_TZ" -> "",
            "VERBAND_RLZ" -> "",
            "GESCHLECHT" -> "M"
          )
        ),
        programms = programms,
        progrm = Some(programms.head),
        options = importService.StructuredPreviewOptions(
          removeMissingAthletes = true,
          fixedVerein = None,
          genderValueMapping = Map("M" -> "M"),
          resolveVereinFromFields = false
        ),
        context = importService.StructuredPreviewContext(
          existingAthlets = Seq(expectedAthlet),
          existingProgramsByAthletEasyprint = Map(expectedAthlet.easyprint -> programms.head.id),
          existingTeamsByAthletEasyprint = Map.empty
        )
      )

      result.shouldBe(Seq.empty)
    }

    "keep rows when only team assignment changes" in {
      val wettkampf = insertGeTuWettkampf(s"ImportServiceTeamChanged-${System.currentTimeMillis()}", anzvereine = 1)
      val programms = readWettkampfLeafs(wettkampf.programmId)
      val wettkampfView = wettkampf.toView(programms.head)
      val importService = new WettkampfImportService(this, wettkampfView)

      val expectedAthlet = AthletView(
        id = 0,
        js_id = 0,
        geschlecht = "M",
        name = "Neu",
        vorname = "Teamwechsel",
        gebdat = Some(getSQLDate("01.01.2015")),
        strasse = "",
        plz = "",
        ort = "",
        verein = None,
        activ = true
      )

      val result = importService.prepareStructuredPreviewRows(
        rowfields = Seq(
          Map(
            "NAME" -> "Neu",
            "VORNAME" -> "Teamwechsel",
            "JAHRGANG" -> "2015",
            "KATEGORIE" -> "",
            "TEAM" -> "2",
            "RESERVE" -> "0",
            "VERBAND" -> "",
            "VEREIN" -> "",
            "RLZ_TZ" -> "",
            "VERBAND_RLZ" -> "",
            "GESCHLECHT" -> "M"
          )
        ),
        programms = programms,
        progrm = Some(programms.head),
        options = importService.StructuredPreviewOptions(
          removeMissingAthletes = true,
          fixedVerein = None,
          genderValueMapping = Map("M" -> "M"),
          resolveVereinFromFields = false
        ),
        context = importService.StructuredPreviewContext(
          existingAthlets = Seq(expectedAthlet),
          existingProgramsByAthletEasyprint = Map(expectedAthlet.easyprint -> programms.head.id),
          existingTeamsByAthletEasyprint = Map(expectedAthlet.easyprint -> (1,0))
        )
      )

      result should have size 1
      result.head._1 shouldBe programms.head.id
      result.head._5 shouldBe 2
    }
  }

  "WettkampfImportService.preparePdfPreviewRowsFromTableData" should {
    "map parsed score-table rows to preview rows with six wertungen" in {
      val wettkampf = insertGeTuWettkampf(s"ImportServicePdf-${System.currentTimeMillis()}", anzvereine = 1)
      val programms = readWettkampfLeafs(wettkampf.programmId)
      val wettkampfView = wettkampf.toView(programms.head)
      val importService = new WettkampfImportService(this, wettkampfView)

      val tableData = Seq(
        Seq("1.", "Muster Max", "2010", "TV Test ZH", "4.20", "13.500", "4.40", "4.10", "13.800", "4.00", "13.000", "4.30", "4.50", "14.000", "3.90", "12.800", "3.80", "12.900", "4.90", "13.000")
      )

      val previewRows = importService.preparePdfPreviewRowsFromTableData(tableData, programms.head)

      previewRows should have size 1
      val row = previewRows.head
      row._1 shouldBe programms.head.id
      row._2.name shouldBe "Max"
      row._2.vorname shouldBe "Muster"
      row._3.verein should not be empty
      row._4 should have size 6
      row._4.foreach(_.wettkampfId shouldBe wettkampf.id)
      row._4.foreach(_.wettkampfUUID shouldBe wettkampfView.uuid.get)
    }
  }

  "WettkampfImportService.applyStructuredImportSelection" should {
    "persist insert, move and remove changes" in {
      val wettkampf = insertGeTuWettkampf(s"ImportServiceApplyStructured-${System.currentTimeMillis()}", anzvereine = 1)
      val programms = readWettkampfLeafs(wettkampf.programmId)
      val wettkampfView = wettkampf.toView(programms.head)
      val importService = new WettkampfImportService(this, wettkampfView)

      val vereinId = createVerein("TV Structured", Some("ZH"))
      val verein = selectVereine.find(_.id == vereinId).getOrElse(fail("Expected test verein"))

      val athleteToMove = insertAthlete(Athlet(vereinId).copy(name = "Move", vorname = "Athlet"))
      val athleteToRemove = insertAthlete(Athlet(vereinId).copy(name = "Remove", vorname = "Athlet"))

      assignAthletsToWettkampf(wettkampf.id, Set(programms.head.id), Set((athleteToMove.id, None)), None)
      assignAthletsToWettkampf(wettkampf.id, Set(programms.head.id), Set((athleteToRemove.id, None)), None)

      val before = selectWertungen(wettkampfId = Some(wettkampf.id))
      val moveView = before.find(w => w.athlet.id == athleteToMove.id).map(_.athlet).getOrElse(fail("Expected move athlete in wettkampf"))
      val removeView = before.find(w => w.athlet.id == athleteToRemove.id).map(_.athlet).getOrElse(fail("Expected remove athlete in wettkampf"))
      val removeIds = before.filter(w => w.athlet.id == athleteToRemove.id).map(_.id).toSet

      val rows = Seq(
        importService.ImportRow(
          progId = programms.head.id,
          athlet = Athlet(0L).copy(name = "Insert", vorname = "Athlet", verein = Some(vereinId)),
          athletView = AthletView(0, 0, "M", "Insert", "Athlet", None, "", "", "", Some(verein), activ = true),
          oldProg = 0L,
          team = 3,
          reserve = 0
        ),
        importService.ImportRow(
          progId = programms.last.id,
          athlet = moveView.toAthlet,
          athletView = moveView,
          oldProg = programms.head.id,
          team = 2,
          reserve = 0
        ),
        importService.ImportRow(
          progId = 0L,
          athlet = removeView.toAthlet,
          athletView = removeView,
          oldProg = 0L,
          team = 0,
          reserve = 0
        )
      )

      importService.applyStructuredImportSelection(rows, Map(removeView.easyprint -> removeIds))

      val after = selectWertungen(wettkampfId = Some(wettkampf.id))

      after.exists(w => w.athlet.name == "Insert" && w.athlet.vorname == "Athlet" && w.wettkampfdisziplin.programm.id == programms.head.id) shouldBe true
      after.exists(w => w.athlet.name == "Insert" && w.athlet.vorname == "Athlet" && w.team == 3) shouldBe true
      after.exists(w => w.athlet.id == athleteToMove.id && w.wettkampfdisziplin.programm.id == programms.last.id) shouldBe true
      after.exists(w => w.athlet.id == athleteToMove.id && w.wettkampfdisziplin.programm.id == programms.head.id) shouldBe false
      after.exists(w => w.athlet.id == athleteToRemove.id) shouldBe false
    }
  }

  "WettkampfImportService.applyPdfImportSelection" should {
    "persist imported wertungen for a newly created athlete" in {
      val wettkampf = insertGeTuWettkampf(s"ImportServiceApplyPdf-${System.currentTimeMillis()}", anzvereine = 1)
      val programms = readWettkampfLeafs(wettkampf.programmId)
      val wettkampfView = wettkampf.toView(programms.head)
      val importService = new WettkampfImportService(this, wettkampfView)

      val vereinId = createVerein("TV PDF", Some("ZH"))
      val verein = selectVereine.find(_.id == vereinId).getOrElse(fail("Expected test verein"))
      val wkdisziplin = listWettkampfDisziplineViews(wettkampf).find(_.programm.id == programms.head.id).getOrElse(fail("Expected disziplin for programm"))

      val importedAthlet = Athlet(0L).copy(
        name = "Pdf",
        vorname = "Athlet",
        gebdat = Some(getSQLDate("01.01.2011")),
        verein = Some(vereinId)
      )

      val candidateView = AthletView(
        id = 0,
        js_id = 0,
        geschlecht = "M",
        name = "Pdf",
        vorname = "Athlet",
        gebdat = Some(getSQLDate("01.01.2011")),
        strasse = "",
        plz = "",
        ort = "",
        verein = Some(verein),
        activ = true
      )

      val importedWertung = Wertung(
        id = 0,
        athletId = 0,
        wettkampfdisziplinId = wkdisziplin.id,
        wettkampfId = wettkampf.id,
        wettkampfUUID = wettkampfView.uuid.getOrElse(""),
        noteD = Some(BigDecimal("4.20")),
        noteE = Some(BigDecimal("9.30")),
        endnote = Some(BigDecimal("13.50")),
        riege = None,
        riege2 = None,
        team = Some(0),
        mediafile = None,
        variables = None
      )

      importService.applyPdfImportSelection(Seq((programms.head.id, importedAthlet, candidateView, List(importedWertung))))

      val after = selectWertungen(wettkampfId = Some(wettkampf.id))
      after.exists { w =>
        w.athlet.name == "Pdf" &&
          w.athlet.vorname == "Athlet" &&
          w.wettkampfdisziplin.id == wkdisziplin.id &&
          w.noteD.contains(BigDecimal("4.20")) &&
          w.endnote.contains(BigDecimal("13.50"))
      } shouldBe true
    }
  }
}
