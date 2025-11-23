package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._
import ch.seidel.kutu.squad
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.immutable

trait StartGeraetGrouper extends RiegenSplitter with Stager {
  private val logger = LoggerFactory.getLogger(classOf[StartGeraetGrouper])

  def groupWertungen(programm: String, wertungen: Map[AthletView, Seq[WertungView]],
                     grp: List[WertungView => String], grpAll: List[WertungView => String],
                     startgeraete: List[Disziplin], maxRiegenSize: Int, splitSex: SexDivideRule, jahrgangGroup: Boolean)
                    (implicit cache: scala.collection.mutable.Map[String, Int]): Seq[(String, String, Disziplin, Seq[(AthletView, Seq[WertungView])])] = {

    val startgeraeteSize = startgeraete.size
    if startgeraeteSize == 0 then {
      Seq.empty
    } else {
      // per groupkey, transform map to seq, sorted by all groupkeys
      val atheltenInRiege = wertungen.groupBy(w => groupKey(grp)(w._2.head)).toSeq.map { x =>
        ( /*grpkey*/ x._1, // Riegenname
          /*values*/ x._2.foldLeft((Seq[(AthletView, Seq[WertungView])](), Set[Long]())) { (acc, w) =>
          val (data, seen) = acc
          val (athlet, _) = w
          if seen.contains(athlet.id) then acc else (w +: data, seen + athlet.id)
        }
          ._1.sortBy(w => groupKey(grpAll)(w._2.head)) // Liste der Athleten in der Riege, mit ihren Wertungen
        )
      }

      val athletensum = atheltenInRiege.flatMap {
        _._2.map(aw => aw._1.id)
      }.toSet.size
      val maxRiegenSize2 = if maxRiegenSize > 0 then maxRiegenSize else math.max(14, math.ceil(1d * athletensum / startgeraeteSize).intValue())
      val riegen = splitToMaxTurnerCount(atheltenInRiege, maxRiegenSize2, cache).map(r => Map(r._1 -> r._2))
      // Maximalausdehnung. Nun die sinnvollen Zusammenlegungen
      val riegenindex = buildRiegenIndex(riegen)
      val workmodel = buildWorkModel(riegen)

      def combineToDurchgangSize(relevantcombis: GeraeteRiegen): GeraeteRiegen = {
        splitSex match {
          case GemischterDurchgang =>
            val rcm = relevantcombis.filter(c => c.turnerriegen.head.geschlecht.equalsIgnoreCase("M"))
            val rcw = relevantcombis.filter(c => c.turnerriegen.head.geschlecht.equalsIgnoreCase("W"))
            val maxMGeraete = startgeraeteSize * rcm.size / relevantcombis.size
            val maxWGeraete = startgeraeteSize * rcw.size / relevantcombis.size
            buildPairs(maxMGeraete, maxRiegenSize2, rcm) ++ buildPairs(maxWGeraete, maxRiegenSize2, rcw)
          case _ =>
            buildPairs(startgeraeteSize, maxRiegenSize2, relevantcombis)
        }
      }

      def handleVereinMerges(startriegen: GeraeteRiegen): GeraeteRiegen = {
        if jahrgangGroup then {
          startriegen
        }
        else splitSex match {
          case GemischteRiegen => bringVereineTogether(startriegen, maxRiegenSize2, splitSex)
          case GemischterDurchgang => startriegen
          case GetrennteDurchgaenge => bringVereineTogether(startriegen, maxRiegenSize2, splitSex)
        }
      }

      val alignedriegen = if workmodel.isEmpty then workmodel else handleVereinMerges(combineToDurchgangSize(workmodel))

      // Startgeräteverteilung
      distributeToStartgeraete(programm, startgeraete, maxRiegenSize, rebuildWertungen(alignedriegen, riegenindex))
    }
  }

  private def distributeToStartgeraete(programm: String, startgeraete: List[Disziplin], maxRiegenSize: Int, alignedriegen: Seq[RiegeAthletWertungen]): Seq[(String, String, Disziplin, Seq[(AthletView, Seq[WertungView])])] = {
    if alignedriegen.isEmpty then {
      Seq.empty
    } else {
      val missingStartOffset = math.min(startgeraete.size, alignedriegen.size)
      val emptyGeraeteRiegen = Range(missingStartOffset, math.max(missingStartOffset, startgeraete.size))
        .map { startgeridx =>
          (s"$programm (1)", s"Leere Riege ${programm}/${startgeraete(startgeridx).easyprint}", startgeraete(startgeridx), Seq[(AthletView, Seq[WertungView])]())
        }

      alignedriegen
        .zipWithIndex.flatMap { r =>
        val (rr, index) = r
        val startgeridx = (index + startgeraete.size) % startgeraete.size
        rr.keys.map { riegenname =>
          logger.debug(s"Durchgang $programm (${index / startgeraete.size + 1}), Start ${startgeraete(startgeridx).easyprint}, ${rr(riegenname).size} Tu/Ti der Riege $riegenname")
          (s"$programm (${if maxRiegenSize > 0 then index / startgeraete.size + 1 else 1})", riegenname, startgeraete(startgeridx), rr(riegenname))
        }
      } ++ emptyGeraeteRiegen
    }
  }


  private def bringVereineTogether(startriegen: GeraeteRiegen, maxRiegenSize2: Int, splitSex: SexDivideRule): GeraeteRiegen = {
    @tailrec
    def _bringVereineTogether(startriegen: GeraeteRiegen, variantsCache: Set[GeraeteRiegen]): GeraeteRiegen = {
      val averageSize = startriegen.averageSize
      val optimized = startriegen.flatMap { raw =>
        raw.turnerriegen.map { r =>
          (r.verein -> raw)
        }
      }.groupBy { vereinraw =>
        vereinraw._1
      }.map { vereinraw =>
        vereinraw._1 -> vereinraw._2.map(_._2).toSet // (Option[Verein] -> GeraeteRiegen)
      }.foldLeft(startriegen) { (accStartriegen, item) =>
        val (verein, riegen) = item
        val ret = riegen.map(f => (f, f.withVerein(verein))).foldLeft(accStartriegen) { (acccStartriegen, riegen2) =>
          val (geraetRiege, toMove) = riegen2
          //            val vereinTurnerRiege = Map(filteredRiege -> toMove)
          val v1 = geraetRiege.countVereine(verein)
          val anzTurner = geraetRiege.size
          acccStartriegen.find { p =>
            p != geraetRiege &&
              acccStartriegen.contains(geraetRiege) &&
              p.countVereine(verein) > v1
          }
          match {
            case Some(zielriege) if ((zielriege ++ toMove).size <= maxRiegenSize2) =>
              logger.debug(s"moving $toMove from ${geraetRiege} to ${zielriege}")
              val gt = geraetRiege -- toMove
              val sg = zielriege ++ toMove
              val r1 = acccStartriegen - zielriege
              val r2 = r1 + sg
              val r3 = r2 - geraetRiege
              val ret = r3 + gt
              ret
            case Some(zielriege) => findSubstitutesFor(toMove, zielriege) match {
              case Some(substitues) =>
                val gt = geraetRiege -- toMove ++ substitues
                val sg = zielriege -- substitues ++ toMove
                if gt.size > maxRiegenSize2 || sg.size > maxRiegenSize2 then {
                  acccStartriegen
                } else {
                  logger.debug(s"switching ${substitues} with toMove between ${geraetRiege} to ${zielriege}")
                  acccStartriegen - zielriege - geraetRiege + gt + sg
                }
              case None => acccStartriegen
            }
            case None => acccStartriegen
          }
        }
        ret
      }
      if optimized == startriegen || variantsCache.contains(optimized) then {
        optimized
      } else {
        val evenoptimized = spreadEven(optimized, splitSex)
        if evenoptimized == startriegen || variantsCache.contains(evenoptimized) then {
          evenoptimized
        } else {
          _bringVereineTogether(evenoptimized, variantsCache + optimized + evenoptimized)
        }
      }
    }

    _bringVereineTogether(startriegen, Set(startriegen))
  }

  private def findSubstitutesFor(riegeToReplace: squad.GeraeteRiege, zielriege: squad.GeraeteRiege): Option[squad.GeraeteRiege] = {
    val replaceCnt = riegeToReplace.size
    val reducedZielriege = zielriege -- riegeToReplace

    val candidates = reducedZielriege.turnerriegen.toSeq.sortBy(_.size).reverse.foldLeft(squad.GeraeteRiege()) { (acc, candidate) =>
      val grouped = acc + candidate
      if grouped.size <= replaceCnt then {
        grouped
      } else {
        acc
      }
    }
    if candidates.nonEmpty then {
      Some(candidates)
    } else {
      None
    }
  }

  @tailrec
  private def spreadEven(startriegen: GeraeteRiegen, splitSex: SexDivideRule, mustIncreaseQuality: Boolean = false): GeraeteRiegen = {
    /*
   * 1. Durchschnittsgrösse ermitteln
   * 2. Grösste Abweichungen ermitteln (kleinste, grösste)
   * 3. davon (teilbare) Gruppen filtern
   * 4. schieben.
   */
    val stats = startriegen.map { raw =>
      // Riege, Anz. Gruppen, Anz. Turner, Std.Abweichung, (kleinste Gruppekey, kleinste Gruppe)
      val anzTurner = raw.size
      val abweichung = anzTurner - startriegen.averageSize
      (raw, raw.size, anzTurner, abweichung, raw.smallestDividable)
    }.toSeq.sortBy(_._4).reverse // Abweichung

    val kleinsteGruppe@(geraeteRiegeAusKleinsterGruppe, _, anzGruppenAusKleinsterGruppe, _, turnerRiegeAusKleinsterGruppe) = stats.last
    type GrpStats = (squad.GeraeteRiege, Int, Int, Int, Option[TurnerRiege])

    def checkSC(p1: GrpStats, p2: GrpStats): Boolean = {
      splitSex match {
        case GemischterDurchgang =>
          val ret = p1._1.turnerriegen.head.geschlecht.equals(p2._1.turnerriegen.head.geschlecht)
          ret
        case _ =>
          true
      }
    }

    stats.find { groessteGruppe =>
      val (geraeteRiege, _, anzGruppenAusGroessterGruppe, _, turnerRiege) = groessteGruppe
      val b11 = turnerRiege.isDefined && groessteGruppe != kleinsteGruppe
      val b12 = checkSC(groessteGruppe, kleinsteGruppe)
      val b2 = anzGruppenAusGroessterGruppe > startriegen.averageSize
      val b3 = b11 && turnerRiege.size + anzGruppenAusKleinsterGruppe <= startriegen.averageSize
      lazy val v1 = geraeteRiege.countVereine(turnerRiege.head.verein)
      lazy val v2 = kleinsteGruppe._1.countVereine(turnerRiege.head.verein)
      lazy val b4 = v1 - turnerRiege.size < v2 + turnerRiege.size
      lazy val b5 = turnerRiege.size + anzGruppenAusKleinsterGruppe <= startriegen.averageSize + (anzGruppenAusKleinsterGruppe / 2)
      lazy val b6 = v1 - turnerRiege.size == 0
      lazy val b7 = v2 > 0

      b11 && b12 && ((b2 && b3 && b4) || (b2 && b3 && b5 && b6 && b7))
    } match {
      case Some(groessteTeilbare@(geraeteRiege, _, _, _, Some(turnerRiege))) =>
        val gt = geraeteRiege - turnerRiege
        val sg = geraeteRiegeAusKleinsterGruppe + turnerRiege
        val nextCombi = gt + startriegen.filter(sr => sr != geraeteRiege && sr != geraeteRiegeAusKleinsterGruppe) + sg
        if mustIncreaseQuality && nextCombi.quality > startriegen.quality then {
          spreadEven(nextCombi, splitSex, mustIncreaseQuality)
        } else {
          startriegen
        }
      case _ => stats.find {
        case groessteGruppe@(geraeteRiegeAusGroessterGruppe, _, anzGruppenAusGroessterGruppe, _, Some(turnerRiegeAusGroessterGruppe)) =>
          groessteGruppe != kleinsteGruppe &&
            checkSC(groessteGruppe, kleinsteGruppe) &&
            anzGruppenAusGroessterGruppe > startriegen.averageSize &&
            turnerRiegeAusGroessterGruppe.size + anzGruppenAusKleinsterGruppe <= startriegen.averageSize
        case _ => false
      } match {
        case Some(groessteGruppe@(geraeteRiegeAusGroessterGruppe, _, _, _, Some(turnerRiegeAusGroessterGruppe))) =>
          val gt = geraeteRiegeAusGroessterGruppe - turnerRiegeAusGroessterGruppe
          val sg = geraeteRiegeAusKleinsterGruppe + turnerRiegeAusGroessterGruppe
          spreadEven(gt + startriegen.filter(sr => sr != geraeteRiegeAusGroessterGruppe && sr != geraeteRiegeAusKleinsterGruppe) + sg, splitSex, true)
        case _ => startriegen
      } // inner stats find match
    } // stats find match
  }
}