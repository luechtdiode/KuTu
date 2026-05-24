package ch.seidel.kutu.domain

import ch.seidel.kutu.data.WettkampfImportSupport.normalizeGender

import java.io.File
import java.net.URI

class WettkampfImportService(service: KutuService, wettkampf: WettkampfView) {
  case class StructuredPreviewOptions(
      removeMissingAthletes: Boolean,
      fixedVerein: Option[Verein],
      genderValueMapping: Map[String, String],
      resolveVereinFromFields: Boolean)

  case class StructuredPreviewContext(
      existingAthlets: Seq[AthletView],
      existingProgramsByAthletEasyprint: Map[String, Long])

  def prepareStructuredPreviewRows(
      rowfields: Seq[Map[String, String]],
      programms: Seq[ProgrammView],
      progrm: Option[ProgrammView],
      options: StructuredPreviewOptions,
      context: StructuredPreviewContext): Seq[(Long, Athlet, AthletView, Long)] = {
    val cache = new java.util.ArrayList[MatchCode]()
    val vereineList = service.selectVereine
    val vereineMap = vereineList.map(v => v.id -> v).toMap

    val importvereine = rowfields
      .map { fields =>
        val importVerein = options.fixedVerein.getOrElse {
          if options.resolveVereinFromFields then {
            val ver = fields.get("VEREIN").map(_.trim).getOrElse("")
            val verb = fields.get("VERBAND").map(_.trim).getOrElse("")
            val rlz = fields.get("RLZ_TZ").map(_.trim).getOrElse("")
            val rlzverb = fields.get("VERBAND_RLZ").map(_.trim).getOrElse("")

            val verein = List(ver, rlz).filter(_.nonEmpty).distinct.mkString(", ")
            val verband = List(verb, rlz, rlzverb).filter(_.nonEmpty).distinct.filter(v => !verein.contains(v)).mkString(", ")
            Verein(0, verein, Some(verband))
          } else Verein(0, "", None)
        }
        val vereinId: Long =
          if importVerein.id > 0L then importVerein.id
          else if importVerein.name.nonEmpty then service.findVereinLike(importVerein).getOrElse(0L)
          else 0L
        (fields, importVerein.copy(id = vereinId))
      }

    val importedRows = importvereine.map { case (fields, verein) =>
      val importGeschlecht = normalizeGender(fields.getOrElse("GESCHLECHT", ""), options.genderValueMapping)
      val gebdat = fields.get("JAHRGANG").map(_.trim).filter(_.nonEmpty).map(year => service.getSQLDate("01.01." + year))
      val parsed = Athlet(
        id = 0,
        js_id = 0,
        geschlecht = importGeschlecht,
        name = fields.getOrElse("NAME", ""),
        vorname = fields.getOrElse("VORNAME", ""),
        gebdat = gebdat,
        strasse = "",
        plz = "",
        ort = "",
        verein = if verein.id > 0L then Some(verein.id) else None,
        activ = true
      )
      val candidate = service.findAthleteLike(cache = cache, exclusive = false)(parsed)
      val progId = resolveProgramId(fields.getOrElse("KATEGORIE", ""), programms, progrm)
      val suggestedVerein = options.fixedVerein
        .orElse(candidate.verein.filter(_ > 0L).flatMap(vereineMap.get))
        .orElse(if verein.id > 0 then vereineMap.get(verein.id) else None)
      val suggestion = AthletView(
        candidate.id, candidate.js_id,
        candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
        candidate.strasse, candidate.plz, candidate.ort,
        suggestedVerein, true)
      val oldProg: Long =
        if options.removeMissingAthletes then context.existingProgramsByAthletEasyprint.getOrElse(suggestion.easyprint, 0L)
        else 0L
      (progId, parsed, suggestion, oldProg)
    }

    val toRemove = if options.removeMissingAthletes then {
      context.existingAthlets
        .filter(a => !importedRows.exists(csv => csv._3.easyprint == a.easyprint))
        .map(a => (0L, a.toAthlet, a, 0L))
        .toList
    } else List.empty

    val changed = if options.removeMissingAthletes then importedRows.filter(item => item._4 != item._1) else importedRows
    changed ++ toRemove
  }

  def assignSelectedVerein(rows: Seq[(Long, Athlet, AthletView, Long)], verein: Verein): Seq[(Long, Athlet, AthletView, Long)] = {
    rows.map { row =>
      (row._1, row._2.copy(verein = Some(verein.id)), row._3.copy(verein = Some(verein)), row._4)
    }
  }

  def prepareFaxeKuTuPdfPreviewRows(filename: URI, progrm: ProgrammView): Seq[(Long, Athlet, AthletView, List[Wertung])] = {
    val scoreImporter = new ScoreImporter()
    val pdfTableData = scoreImporter.extractTableData(new File(filename).toPath)
    preparePdfPreviewRowsFromTableData(pdfTableData, progrm)
  }

  def preparePdfPreviewRowsFromTableData(tableData: Seq[Seq[String]], progrm: ProgrammView): Seq[(Long, Athlet, AthletView, List[Wertung])] = {
    val scoreImporter = new ScoreImporter()
    val rowfields = scoreImporter.mapStructuredRows(tableData)(buildPdfAthletMapper("M"), buildPdfWertungMapper(progrm))

    val cache = new java.util.ArrayList[MatchCode]()
    val vereineList = service.selectVereine
    val vereineMap = vereineList.map(v => v.id -> v).toMap
    val athletSearchFn = service.findAthleteLike(wettkampf = None, cache = cache, exclusive = false, exactVerein = false)

    rowfields.map { row =>
      val (parsed, verein, wertungen) = row
      val candidate = athletSearchFn(parsed)
      val suggestion = AthletView(
        candidate.id, candidate.js_id,
        candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
        candidate.strasse, candidate.plz, candidate.ort,
        candidate.verein.filter(_ > 0L)
          .map(v => vereineMap(v))
          .orElse(service.findVereinLike(verein, exact = false)
            .filter(_ > 0L)
            .map(v => vereineMap(v))
            .orElse(Some(verein))),
        activ = true)
      (progrm.id, parsed, suggestion, wertungen)
    }
  }

  private def buildPdfWertungMapper(progrm: ProgrammView): (String, String, String) => Wertung = {
    val wkdiszs = service.listWettkampfDisziplineViews(wettkampf.toWettkampf)
    (geraet: String, valueD: String, valueE: String) => {
      val dValue = valueD.split(",").map(BigDecimal(_)).sorted.reverse.head
      val wkds = wkdiszs
        .filter(_.programm == progrm)
        .find(_.disziplin.name == geraet)
      val wd: Long = wkds.map(_.id).getOrElse(0L)
      val defaultvariables = wkds.flatMap(wd => wd.notenSpez.template.map(t => t.toView(t.variables)))

      Wertung(
        id = 0,
        athletId = 0,
        wettkampfdisziplinId = wd,
        wettkampfId = wettkampf.id,
        wettkampfUUID = wettkampf.uuid.getOrElse(""),
        noteD = Some(dValue),
        noteE = Some(BigDecimal(valueE) - dValue),
        endnote = Some(BigDecimal(valueE)),
        riege = None,
        riege2 = None,
        team = None,
        mediafile = None,
        variables = defaultvariables
      )
    }
  }

  private def buildPdfAthletMapper(defaultgeschlecht: String): (String, String, String, String) => (Verein, Athlet) = {
    val verbandPartsResolver: Verein => Set[String] = _.verband match {
      case Some(verband) => verband.split(",").toSet + verband
      case None => Set.empty
    }
    val knownVerbandList = service.selectVereine.flatMap(verbandPartsResolver).toSet

    (geschlecht: String, name: String, jahrgang: String, vereinText: String) => {
      val guessedVerband = vereinText.split(" ").last
      val fallbackVerein = Verein(0, vereinText.replace(guessedVerband, "").replace("/", ", ").trim, Some(guessedVerband))
      val v = knownVerbandList
        .filter(verband => vereinText.contains(verband))
        .map { verband =>
          Verein(0, vereinText.replace(verband, "").replace("/", ", ").trim, Some(verband))
        }
        .headOption
        .getOrElse(fallbackVerein)
      val ns = name.split(" ")
      val a = Athlet(
        id = 0,
        js_id = 0,
        geschlecht = if geschlecht.isEmpty then defaultgeschlecht else geschlecht,
        name = ns.last,
        vorname = ns.reverse.tail.reverse.mkString(" ").trim,
        gebdat = Some(service.getSQLDate("01.01." + jahrgang)),
        strasse = "",
        plz = "",
        ort = "",
        verein = service.findVereinLike(v),
        activ = true
      )
      (v, a)
    }
  }

  def applyPdfImportSelection(athletList: Seq[(Long, Athlet, AthletView, List[Wertung])]): Unit = {
    athletList
      .groupBy(_._3.verein)
      .filter(_._1.nonEmpty)
      .map { grp =>
        val v = insertVereinIfMissing(grp._1.get)
        (v.id, grp._2.map { c =>
          val a = c._2.copy(verein = Some(v.id))
          val av: AthletView = c._3.copy(verein = Some(v))
          (c._1, a, av, c._4)
        })
      }
      .foreach { grp =>
        insertAssignments(grp._1, grp._2)
      }
  }

  def applyStructuredImportSelection(
      athletList: Seq[(Long, Athlet, AthletView, Long)],
      wertungIdsByAthletEasyprint: Map[String, Set[Long]]): Unit = {
    insertImportedAthletListToCompetition(athletList)
    athletList
      .filter(_._1 > 0L)
      .filter(_._4 > 0)
      .foreach { grp =>
        service.moveToProgram(wettkampf.id, grp._1, 0, grp._3)
      }
    athletList
      .filter(_._1 == 0L)
      .foreach { grp =>
        service.unassignAthletFromWettkampf(wertungIdsByAthletEasyprint.getOrElse(grp._3.easyprint, Set.empty))
      }
  }


  private def resolveProgramId(programmRef: String, programms: Seq[ProgrammView], fallback: Option[ProgrammView]): Long = {
    val trimmed = Option(programmRef).map(_.trim).getOrElse("")
    if trimmed.isEmpty then fallback.map(_.id).getOrElse(0L)
    else {
      val fromIndex = scala.util.Try(java.lang.Integer.parseInt(trimmed)).toOption.flatMap { idx =>
        if idx > 0 && idx <= programms.length then Some(programms(idx - 1).id) else None
      }
      fromIndex.orElse {
        val normalized = trimmed.replace("-", "").toUpperCase
        programms.find(p => p.name.replace("-", "").toUpperCase == normalized).map(_.id)
      }.orElse(fallback.map(_.id)).getOrElse(0L)
    }
  }

  private def insertVereinIfMissing(verein: Verein): Verein = {
    if verein.id > 0L then verein
    else service.insertVerein(verein)
  }

  private def insertImportedAthletListToCompetition(athletList: Seq[(Long, Athlet, AthletView, Long)]): Unit = {
    athletList
      .filter(_._1 > 0L)
      .filter(_._4 == 0)
      .groupBy(_._3.verein)
      .filter(_._1.nonEmpty)
      .map { grp =>
        val v = insertVereinIfMissing(grp._1.get)
        (v.id, grp._2.map { c =>
          val a = c._2.copy(verein = Some(v.id))
          val av: AthletView = c._3.copy(verein = Some(v))
          (c._1, a, av, List.empty[Wertung])
        })
      }
      .foreach { grp =>
        insertAssignments(grp._1, grp._2)
      }
  }

  private def insertAssignments(vereinId: Long, selectedAthleten: Seq[(Long, Athlet, AthletView, List[Wertung])]): Unit = {
    val clip = selectedAthleten.map { x =>
      val (progrId, importathlet, candidateView, wertungen) = x
      val id = if candidateView.id > 0 &&
        (importathlet.gebdat match {
          case Some(_) =>
            candidateView.gebdat match {
              case Some(cd) => f"$cd%tF".endsWith("-01-01")
              case _ => true
            }
          case _ => false
        }) then {
        service.insertAthlete(Athlet(
          id = candidateView.id,
          js_id = candidateView.js_id,
          geschlecht = candidateView.geschlecht,
          name = candidateView.name,
          vorname = candidateView.vorname,
          gebdat = importathlet.gebdat,
          strasse = candidateView.strasse,
          plz = candidateView.plz,
          ort = candidateView.ort,
          verein = Some(vereinId),
          activ = true
        )).id
      }
      else if candidateView.id > 0 then {
        candidateView.id
      }
      else {
        service.insertAthlete(Athlet(
          id = 0,
          js_id = candidateView.js_id,
          geschlecht = candidateView.geschlecht,
          name = candidateView.name,
          vorname = candidateView.vorname,
          gebdat = candidateView.gebdat,
          strasse = candidateView.strasse,
          plz = candidateView.plz,
          ort = candidateView.ort,
          verein = Some(vereinId),
          activ = true
        )).id
      }
      (progrId, id, wertungen.map(wertung => wertung.copy(athletId = id)))
    }
    if clip.nonEmpty then {
      for (progId, athletes) <- clip.groupBy(_._1).map(x => (x._1, x._2.map(x => (x._2, x._3)))) do {
        if athletes.exists(_._2.isEmpty) then {
          val athletMediaList: Set[(Long, Option[Media])] = athletes.map(s => (s._1, None)).toSet
          service.assignAthletsToWettkampf(wettkampf.id, Set(progId), athletMediaList, None)
        }
        else {
          athletes.flatMap(_._2).foreach(service.updateOrinsertWertung)
        }
      }
    }
  }
}


