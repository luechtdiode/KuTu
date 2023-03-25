package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._
import org.slf4j.LoggerFactory

import scala.collection.mutable

case class DurchgangBuilder(service: KutuService) extends Mapper with RiegenSplitter with StartGeraetGrouper {
  private val logger = LoggerFactory.getLogger(classOf[DurchgangBuilder])
  
  def suggestDurchgaenge(wettkampfId: Long, maxRiegenSize: Int = 14,  
      durchgangfilter: Set[String] = Set.empty, programmfilter: Set[Long] = Set.empty,
      splitSexOption: Option[SexDivideRule] = None, splitPgm: Boolean = true,
      onDisziplinList: Option[Set[Disziplin]] = None): Map[String, Map[Disziplin, Iterable[(String,Seq[Wertung])]]] = {

    implicit val cache: mutable.Map[String, Int] = scala.collection.mutable.Map[String, Int]()
    
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
        val pgmHead = wertungen.head._2.head.wettkampfdisziplin.programm
        val dzlf = dzl.filter{ d =>
          wkdisziplinlist.exists { wd => d.id == wd.disziplinId && wd.programmId == pgmHead.id }
        }
        val wdzlf = wkdisziplinlist.filter{d => d.programmId == pgmHead.id }
        val startgeraete = wdzlf.filter(d => (onDisziplinList.isEmpty && d.startgeraet == 1) || (onDisziplinList.nonEmpty && onDisziplinList.get.map(_.id).contains(d.disziplinId)))
        val dzlff = dzlf.filter(d => startgeraete.exists(wd => wd.disziplinId == d.id)).distinct
        val dzlffm = dzlff.filter(d => startgeraete.find(wd => wd.disziplinId == d.id).exists(p => p.masculin == 1))
        val dzlfff = dzlff.filter(d => startgeraete.find(wd => wd.disziplinId == d.id).exists(p => p.feminim == 1))
        val splitSex = splitSexOption match {
          case None => if (dzlffm == dzlfff) GemischteRiegen else GetrennteDurchgaenge
          case Some(option) => option
        }
        wertungen.head._2.head.wettkampfdisziplin.programm.riegenmode match {
          case RiegeRaw.RIEGENMODE_BY_JG =>
            val atGrouper = ATTGrouper.atGrouper
            val atgr = atGrouper.take(atGrouper.size-1)
            splitSex match {
              case GemischteRiegen =>
                groupWertungen(programm, wertungen, atgr, atGrouper, dzlff, maxRiegenSize, GemischteRiegen, true)
              case GemischterDurchgang =>
                groupWertungen(programm, wertungen, atgr, atGrouper, dzlff, maxRiegenSize, GemischterDurchgang, true)
              case GetrennteDurchgaenge =>
                val m = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("M"))
                val w = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("W"))
                groupWertungen(programm + "-Tu", m, atgr, atGrouper, dzlffm, maxRiegenSize, GetrennteDurchgaenge, true) ++
                  groupWertungen(programm + "-Ti", w, atgr, atGrouper, dzlfff, maxRiegenSize, GetrennteDurchgaenge, true)
            }
          case _ =>
            // Startgeräte selektieren
            splitSex match {
              case GemischteRiegen =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, dzlff, maxRiegenSize, GemischteRiegen, false)
              case GemischterDurchgang =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, dzlff, maxRiegenSize, GemischterDurchgang, false)
              case GetrennteDurchgaenge =>
                val m = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("M"))
                val w = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("W"))
                groupWertungen(programm + "-Tu", m, wkFilteredGrouper, wkGrouper, dzlffm, maxRiegenSize, GetrennteDurchgaenge, false) ++
                  groupWertungen(programm + "-Ti", w, wkFilteredGrouper, wkGrouper, dzlfff, maxRiegenSize, GetrennteDurchgaenge, false)
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