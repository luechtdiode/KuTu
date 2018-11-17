package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._
import org.slf4j.LoggerFactory

case class DurchgangBuilder(service: KutuService) extends Mapper with RiegenSplitter with StartGeraetGrouper {
  private val logger = LoggerFactory.getLogger(classOf[DurchgangBuilder])
  
  def suggestDurchgaenge(wettkampfId: Long, maxRiegenSize: Int = 14,  
      durchgangfilter: Set[String] = Set.empty, programmfilter: Set[Long] = Set.empty,
      splitSex: SexDivideRule = GemischteRiegen, splitPgm: Boolean = true,
      onDisziplinList: Option[Set[Disziplin]] = None): Map[String, Map[Disziplin, Iterable[(String,Seq[Wertung])]]] = {

    implicit val cache = scala.collection.mutable.Map[String, Int]()
    
    val filteredWert = prepareWertungen(wettkampfId) map wertungZuDurchgang(durchgangfilter, makeDurchgangMap(wettkampfId)) filter {x =>
        x._2.nonEmpty &&
        (programmfilter.isEmpty || programmfilter.contains(x._2.head.wettkampfdisziplin.programm.id))
      }
    
    if(filteredWert.isEmpty) {
      Map[String, Map[Disziplin, Iterable[(String,Seq[Wertung])]]]()
    }
    else {
      val programme = listProgramme(filteredWert)
      val progAthlWertungen = buildProgrammAthletWertungen(filteredWert, programme, splitPgm || durchgangfilter.isEmpty)
      val riegencnt = 0 // riegencnt 0 is unlimited
      val disziplinlist = service.listDisziplinesZuWettkampf(wettkampfId)
      val wkdisziplinlist = service.listWettkampfDisziplines(wettkampfId)

      val dzl = disziplinlist.filter(d => onDisziplinList.isEmpty || onDisziplinList.get.contains(d))
      val wkGrouper = KuTuGeTuGrouper.wkGrouper
      val wkFilteredGrouper = wkGrouper.take(if(riegencnt == 0) wkGrouper.size-1 else wkGrouper.size)
      if(progAthlWertungen.keys.size > 1) {
        val toDebug = (progAthlWertungen.keys.size, progAthlWertungen.keys.map(k => (progAthlWertungen(k).size, progAthlWertungen(k).map(w => w._2.size).sum))).toString
        logger.debug(toDebug)
      }
      val riegen = progAthlWertungen.flatMap{x =>
        val (programm, wertungen) = x
        val dzlf = dzl.filter{d =>
          val pgm = wertungen.head._2.head.wettkampfdisziplin.programm
          wkdisziplinlist.exists { wd => d.id == wd.disziplinId && wd.programmId == pgm.id }
        }

        wertungen.head._2.head.wettkampfdisziplin.notenSpez match {
          case at: Athletiktest =>
            val atGrouper = ATTGrouper.atGrouper
            val atgr = atGrouper.take(atGrouper.size-1)
            splitSex match {
              case GemischteRiegen =>
                groupWertungen(programm, wertungen, atgr, atGrouper, dzlf, maxRiegenSize, splitSex, true)
              case GemischterDurchgang =>
                groupWertungen(programm, wertungen, atgr, atGrouper, dzlf, maxRiegenSize, splitSex, true)
              case GetrennteDurchgaenge =>
                val m = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("M"))
                val w = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("W"))
                groupWertungen(programm + "-Tu", m, atgr, atGrouper, dzlf, maxRiegenSize, splitSex, true) ++
                groupWertungen(programm + "-Ti", w, atgr, atGrouper, dzlf, maxRiegenSize, splitSex, true)
            }
          case KuTuWettkampf =>
            splitSex match {
              case GemischteRiegen =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, dzlf, maxRiegenSize, splitSex, false)
              case GemischterDurchgang =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, dzlf, maxRiegenSize, splitSex, false)
              case GetrennteDurchgaenge =>
                val m = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("M"))
                val w = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("W"))
                groupWertungen(programm + "-Tu", m, wkFilteredGrouper, wkGrouper, dzlf, maxRiegenSize, splitSex, false) ++
                groupWertungen(programm + "-Ti", w, wkFilteredGrouper, wkGrouper, dzlf, maxRiegenSize, splitSex, false)
            }
          case GeTuWettkampf =>
            // Barren wegschneiden (ist kein Startgerät)
            val dzlff = dzlf.filter(d => (onDisziplinList.isEmpty && d.id != 5) || (onDisziplinList.nonEmpty && onDisziplinList.get.contains(d)))
            val wkGrouper = KuTuGeTuGrouper.wkGrouper
            splitSex match {
              case GemischteRiegen =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, dzlff, maxRiegenSize, splitSex, false)
              case GemischterDurchgang =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, dzlff, maxRiegenSize, splitSex, false)
              case GetrennteDurchgaenge =>
                val m = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("M"))
                val w = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("W"))
                groupWertungen(programm + "-Tu", m, wkFilteredGrouper, wkGrouper, dzlff, maxRiegenSize, splitSex, false) ++
                groupWertungen(programm + "-Ti", w, wkFilteredGrouper, wkGrouper, dzlff, maxRiegenSize, splitSex, false)
            }
        }
      }

      rebuildDurchgangWertungen(riegen)
    }
  }


  
  private def listProgramme(x: Map[AthletView, Seq[WertungView]]) = x.flatMap(w => w._2.map(xx => xx.wettkampfdisziplin.programm.name)).toSet
  
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
    if(durchgangfilter.isEmpty) {
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
      if(pgmsPerAthlet.size > 1) {
        pgmsPerAthlet.map(pgpa =>
          (pgpa._1, pgpa._2, pgpa._3.map{w =>
            val wkpg = pgpa._2.name.shorten
            if(w.riege.getOrElse("").contains(wkpg))
              w
            else
              w.copy(riege = Some((w.riege.getOrElse("") + wkpg).trim))
          }))
      }
      else {
        pgmsPerAthlet
      }
    }.groupBy{x =>
      if(splitPgm) x._2.name
      else         programme.mkString(" & ")
    }.map(x => x._1 -> x._2.map(xx => xx._1 -> xx._3).toMap)
    

    
}