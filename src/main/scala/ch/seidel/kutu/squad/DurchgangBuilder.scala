package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*
import org.slf4j.LoggerFactory

import scala.collection.mutable

case class DurchgangBuilder(service: KutuService) extends Mapper with RiegenSplitter with StartGeraetGrouper {
  private val logger = LoggerFactory.getLogger(classOf[DurchgangBuilder])

  private case class ProgrammKontext(
      programm: String,
      wertungen: Map[AthletView, Seq[WertungView]],
      startgeraete: List[Disziplin],
      splitSex: SexDivideRule,
      grouper: List[WertungView => String],
      fullGrouper: List[WertungView => String],
      jahrgangGroup: Boolean,
      disziplinGeschlecht: Map[Long, (Int, Int)] = Map.empty)

  def suggestDurchgaenge(wettkampfId: Long, maxRiegenSize: Int = 0,
      durchgangfilter: Set[String] = Set.empty, programmfilter: Set[Long] = Set.empty,
      splitSexOption: Option[SexDivideRule] = None, splitPgm: Boolean = true,
      onDisziplinList: Option[Set[Disziplin]] = None,
      separateRiegen2Durchgaenge: Boolean = false): SuggestedDurchgaenge = {

    implicit val cache: mutable.Map[String, Int] = scala.collection.mutable.Map[String, Int]()
    
    val filteredWert = prepareWertungen(wettkampfId) map wertungZuDurchgang(durchgangfilter, makeDurchgangMap(wettkampfId)) filter {x =>
        x._2.nonEmpty &&
        (programmfilter.isEmpty || programmfilter.contains(x._2.head.wettkampfdisziplin.programm.id))
      }
    
    if filteredWert.isEmpty then {
      Map.empty
    }
    else {
      val programme = listProgramme(filteredWert)
      val progAthlWertungen = buildProgrammAthletWertungen(filteredWert, programme, splitPgm)
      val riegen = buildProgrammKontexte(wettkampfId, progAthlWertungen, splitSexOption, onDisziplinList).flatMap {
        case ProgrammKontext(programm, wertungen, startgeraete, splitSex, grouper, fullGrouper, jahrgangGroup, disziplinGeschlecht) =>
          groupWertungen(programm, wertungen, grouper, fullGrouper, startgeraete, maxRiegenSize, splitSex, jahrgangGroup, disziplinGeschlecht)
      }

      val suggested = rebuildDurchgangWertungen(riegen)
      if separateRiegen2Durchgaenge then
        val wettkampfdisziplinIdToKategorieUndStart = filteredWert.values.flatten
          .map(_.wettkampfdisziplin).toSet
          .map(w => w.id -> (w.programm.name, w.disziplin))
          .toMap
        val riege2Assignments = extractRiege2ToDurchgangMapping(suggested, wettkampfdisziplinIdToKategorieUndStart)
        suggested ++ separateRiegen2DurchgaengeFromSuggested(suggested, riege2Assignments)
      else suggested
    }
  }

  def suggestDurchgangGruppen(wettkampfId: Long, maxRiegenSize: Int = 0,
      durchgangfilter: Set[String] = Set.empty, programmfilter: Set[Long] = Set.empty,
      splitSexOption: Option[SexDivideRule] = None, splitPgm: Boolean = true,
      onDisziplinList: Option[Set[Disziplin]] = None,
      maxParallelProGruppe: Int = Int.MaxValue): Seq[SuggestedDurchgang] = {
    val suggested = suggestDurchgaenge(wettkampfId, maxRiegenSize, durchgangfilter, programmfilter, splitSexOption, splitPgm, onDisziplinList)
    DurchgangGrouper.groupDurchgaengeByKategorien(suggested, maxParallelProGruppe)
  }

  private def extractRiege2ToDurchgangMapping(suggested: SuggestedDurchgaenge, wdToKategorieMap: Map[Long, (String, Disziplin)]): Map[String, (String, Disziplin)] = {
    suggested.toSeq.sortBy(_._1).flatMap { case (durchgangName, startMap) =>
      val riege2Names = startMap.values.toSeq
        .flatMap(_.toSeq)
        .flatMap(_._2)
        .flatMap(w => w.riege2.map(r => r -> wdToKategorieMap(w.wettkampfdisziplinId))).toMap

      riege2Names.map(r2 =>
          val (r2Name, (kategorie, start)) = r2
          r2Name -> (s"$kategorie (${start.name})", start)
      )
    }.toMap
  }

  private[squad] def separateRiegen2DurchgaengeFromSuggested(
      suggested: SuggestedDurchgaenge,
      riege2Assignments: Map[String, (String, Disziplin)]): SuggestedDurchgaenge = {

    val grouped = suggested.values.toSeq
      .flatMap(_.toSeq)
      .flatMap { case (disziplin, riegenIter) =>
        riegenIter.toSeq
          .flatMap(_._2)
          .flatMap { wertung =>
            wertung.riege2.flatMap { riege2Name =>
              riege2Assignments.get(riege2Name).map { target =>
                val (targetDurchgang, start) = target
                (targetDurchgang, start, riege2Name, Seq(wertung))
              }
            }
          }
      }

    grouped
      .groupBy(_._1)
      .toSeq
      .sortBy(_._1)
      .map { case (durchgangName, tuples) =>
        val diszMap: DurchgangStationZuteilung = tuples
          .groupBy(_._2)
          .toSeq
          .sortBy(_._1.name)
          .map { case (disziplin, diszTuples) =>
            val byRiege2: Seq[(String, Seq[Wertung])] = diszTuples
              .groupBy(_._3)
              .toSeq
              .sortBy(_._1)
              .map { case (riege2Name, riege2Tuples) =>
                (riege2Name, riege2Tuples.flatMap(_._4))
              }
            disziplin -> byRiege2
          }
          .toMap
        durchgangName -> diszMap
      }
      .toMap
  }



  private def listProgramme(x: Map[AthletView, Seq[WertungView]]) = x.flatMap(w => w._2.map(xx => xx.wettkampfdisziplin.programm.name)).toSet

  private def buildProgrammKontexte(
      wettkampfId: Long,
      progAthlWertungen: Map[String, Map[AthletView, Seq[WertungView]]],
      splitSexOption: Option[SexDivideRule],
      onDisziplinList: Option[Set[Disziplin]]): Seq[ProgrammKontext] = {
    val riegencnt = 0
    val disziplinlist = service.listDisziplinesZuWettkampf(wettkampfId)
    val wkdisziplinlist = service.listWettkampfDisziplines(wettkampfId)
    val filteredDisziplines = disziplinlist.filter(d => onDisziplinList.isEmpty || onDisziplinList.get.contains(d))

    progAthlWertungen.toSeq.flatMap { case (programm, wertungen) =>
      val pgmHead = wertungen.head._2.head.wettkampfdisziplin.programm
      val programmDisziplinen = filteredDisziplines.filter { d =>
        wkdisziplinlist.exists(wd => d.id == wd.disziplinId && wd.programmId == pgmHead.id)
      }
      val wettkampfDisziplinen = wkdisziplinlist.filter(_.programmId == pgmHead.id)
      val startgeraeteMeta = wettkampfDisziplinen.filter(d =>
        (onDisziplinList.isEmpty && d.startgeraet == 1) ||
          (onDisziplinList.nonEmpty && onDisziplinList.get.map(_.id).contains(d.disziplinId)))
      val startgeraete = programmDisziplinen.filter(d => startgeraeteMeta.exists(wd => wd.disziplinId == d.id)).distinct
      val startgeraeteM = startgeraete.filter(d => startgeraeteMeta.find(wd => wd.disziplinId == d.id).exists(_.masculin == 1))
      val startgeraeteW = startgeraete.filter(d => startgeraeteMeta.find(wd => wd.disziplinId == d.id).exists(_.feminim == 1))
      val splitSex = splitSexOption match {
        case None => if startgeraeteM == startgeraeteW then GemischteRiegen else GetrennteDurchgaenge
        case Some(option) => option
      }
      val disziplinGeschlecht = startgeraeteMeta.map(wd => wd.disziplinId -> (wd.masculin, wd.feminim)).toMap
      val wv = wertungen.head._2.head
      val riegenmode = wv.wettkampfdisziplin.programm.riegenmode
      val aks = wv.wettkampf.altersklassen match {
        case Some(s: String) if s.nonEmpty => Some(s)
        case _ => None
      }
      val jaks = wv.wettkampf.jahrgangsklassen match {
        case Some(s: String) if s.nonEmpty => Some(s)
        case _ => None
      }
      val (shortGrouper, fullGrouper, jgGroup) = RiegenBuilder.selectRiegenGrouper(riegenmode, aks, jaks).buildGrouper(riegencnt)
      splitSex match {
        case GemischteRiegen | GemischterDurchgang =>
          Seq(ProgrammKontext(programm, wertungen, startgeraete, splitSex, shortGrouper, fullGrouper, jgGroup, disziplinGeschlecht))
        case GetrennteDurchgaenge =>
          val maenner = wertungen.filter(_._1.geschlecht.equalsIgnoreCase("M"))
          val frauen = wertungen.filter(_._1.geschlecht.equalsIgnoreCase("W"))
          Seq(
            if maenner.nonEmpty then Some(ProgrammKontext(if frauen.nonEmpty then programm + "-Tu" else programm, maenner, startgeraeteM, GetrennteDurchgaenge, shortGrouper, fullGrouper, jgGroup, disziplinGeschlecht)) else None,
            if frauen.nonEmpty then Some(ProgrammKontext(if maenner.nonEmpty then programm + "-Ti" else programm, frauen, startgeraeteW, GetrennteDurchgaenge, shortGrouper, fullGrouper, jgGroup, disziplinGeschlecht)) else None
          ).flatten
      }
    }
  }

  private def makeDurchgangMap(wettkampfId: Long): Map[String, String] = service.selectRiegenRaw(wettkampfId)
      .filter(rr => rr.durchgang.isDefined)
      .map(rr => rr.r -> rr.durchgang.get)
      .toMap
      
  private def containsRiegeInDurchgang(durchgangfilter: Set[String], durchgangMap: Map[String, String])(wertung: WertungView) = 
    (wertung.riege.toSet ++ wertung.riege2.toSet).exists {r => durchgangMap.get(r) match {
        case Some(d) => durchgangfilter.contains(d)
        case _ => false
      }
    }
  
  private def prepareWertungen(wettkampfId: Long) = service.selectWertungen(wettkampfId = Some(wettkampfId)).groupBy(w => w.athlet)
  
  private def wertungZuDurchgang(durchgangfilter: Set[String], durchgangMap: Map[String, String])(x: (AthletView, Seq[WertungView])): (AthletView, Seq[WertungView]) =
    if durchgangfilter.isEmpty then {
      x
    }
    else {
      val mappedWertungen = x._2.filter(containsRiegeInDurchgang(durchgangfilter, durchgangMap))
      (x._1, mappedWertungen)
    }
    
  private def buildProgrammAthletWertungen(filteredWert: Map[AthletView, Seq[WertungView]], programme: Set[String], splitPgm: Boolean) = 
    filteredWert.toSeq.flatMap{aw =>
      val (athlet, wertungen) = aw
      val pgmsPerAthlet = wertungen.groupBy { wg => wg.wettkampfdisziplin.programm }.toSeq.map(w => (athlet, w._1, w._2))
      if pgmsPerAthlet.size > 1 then {
        pgmsPerAthlet.map(pgpa =>
          (pgpa._1, pgpa._2, pgpa._3.map{w =>
            val wkpg = pgpa._2.name.shorten
            if w.riege.getOrElse("").contains(wkpg) then
              w
            else
              w.copy(riege = Some((w.riege.getOrElse("") + wkpg).trim))
          }))
      }
      else {
        pgmsPerAthlet
      }
    }.groupBy{x =>
      if splitPgm then x._2.name
      else         programme.mkString(" & ")
    }.map(x => x._1 -> x._2.map(xx => xx._1 -> xx._3).toMap)
    

    
}

