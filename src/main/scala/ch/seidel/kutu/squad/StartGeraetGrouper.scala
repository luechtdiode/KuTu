package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*
import ch.seidel.kutu.squad
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

trait StartGeraetGrouper extends RiegenSplitter with Stager {
  private val logger = LoggerFactory.getLogger(classOf[StartGeraetGrouper])

  protected object BalanceConfig {
    val strictFairnessAthletesPerDurchgang: Int = 40
    val fairnessWeight: Int = 50
    val splitWeight: Int = 30
    val cohesionWeight: Int = 20

    def targetDiff(participants: Int): Int = if participants <= strictFairnessAthletesPerDurchgang then 1 else 2
  }

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
      val targetDiff = BalanceConfig.targetDiff(athletensum)
      val maxRiegenSize2 = if maxRiegenSize > 0 then maxRiegenSize else math.max(14, math.ceil(1d * athletensum / startgeraeteSize).intValue())
      // Pre-compute eqsize: when rounds > 1 are needed, TurnerRiegen must not exceed eqsize,
      // otherwise mergeCandidates (which is capped at eqsize) cannot fully balance them.
      val (effectiveSplitTarget, _) = computeDimensions(athletensum, startgeraeteSize, maxRiegenSize2)
      val avoidSplitsForSmallDurchgang = athletensum <= startgeraeteSize
      val riegen = (if avoidSplitsForSmallDurchgang then atheltenInRiege
      else splitToMaxTurnerCount(atheltenInRiege, effectiveSplitTarget, cache))
        .map(r => Map(r._1 -> r._2))
      // Maximalausdehnung. Nun die sinnvollen Zusammenlegungen
      val riegenindex = buildRiegenIndex(riegen)
      val workmodel = buildWorkModel(riegen)

      def combineToDurchgangSize(relevantcombis: GeraeteRiegen): GeraeteRiegen = {
        val participants = relevantcombis.sizeOfAll
        val localTargetDiff = BalanceConfig.targetDiff(participants)
        splitSex match {
          case GemischterDurchgang =>
            val rcm = relevantcombis.filter(c => c.turnerriegen.head.geschlecht.equalsIgnoreCase("M"))
            val rcw = relevantcombis.filter(c => c.turnerriegen.head.geschlecht.equalsIgnoreCase("W"))
            val maxMGeraete = startgeraeteSize * rcm.size / relevantcombis.size
            val maxWGeraete = startgeraeteSize * rcw.size / relevantcombis.size
            buildPairs(maxMGeraete, maxRiegenSize2, rcm, localTargetDiff, BalanceConfig.fairnessWeight, BalanceConfig.splitWeight, BalanceConfig.cohesionWeight) ++
              buildPairs(maxWGeraete, maxRiegenSize2, rcw, localTargetDiff, BalanceConfig.fairnessWeight, BalanceConfig.splitWeight, BalanceConfig.cohesionWeight)
          case _ =>
            buildPairs(startgeraeteSize, maxRiegenSize2, relevantcombis, localTargetDiff, BalanceConfig.fairnessWeight, BalanceConfig.splitWeight, BalanceConfig.cohesionWeight)
        }
      }

      def handleVereinMerges(startriegen: GeraeteRiegen): GeraeteRiegen = {
        if jahrgangGroup then {
          startriegen
        }
        else splitSex match {
          case GemischteRiegen => bringVereineTogether(startriegen, maxRiegenSize2, splitSex, targetDiff)
          case GemischterDurchgang => startriegen
          case GetrennteDurchgaenge => bringVereineTogether(startriegen, maxRiegenSize2, splitSex, targetDiff)
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
      val numDurchgaenge = math.ceil(alignedriegen.size.toDouble / startgeraete.size).toInt
      // Sort descending by athlete count so each consecutive chunk of startgeraeteSize entries
      // (= one Durchgang) is homogeneous in size → minimal intra-Durchgang spread.
      val orderedRiegen = if numDurchgaenge <= 1 then alignedriegen
      else alignedriegen.sortBy(r => -r.sizeOfAll)

      val missingStartOffset = math.min(startgeraete.size, orderedRiegen.size)
      val emptyGeraeteRiegen = Range(missingStartOffset, math.max(missingStartOffset, startgeraete.size))
        .map { startgeridx =>
          (s"$programm (1)", s"Leere Riege $programm/${startgeraete(startgeridx).easyprint}", startgeraete(startgeridx), Seq[(AthletView, Seq[WertungView])]())
        }

      orderedRiegen
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


  private def bringVereineTogether(startriegen: GeraeteRiegen, maxRiegenSize2: Int, splitSex: SexDivideRule, targetDiff: Int): GeraeteRiegen = {
    @tailrec
    def _bringVereineTogether(startriegen: GeraeteRiegen, variantsCache: Set[GeraeteRiegen]): GeraeteRiegen = {
      val averageSize = startriegen.averageSize
      val optimized = startriegen.flatMap { raw =>
        raw.turnerriegen.map { r =>
          r.verein -> raw
        }
      }.groupBy { vereinraw =>
        vereinraw._1
      }.map { vereinraw =>
        vereinraw._1 -> vereinraw._2.map(_._2) // (Option[Verein] -> GeraeteRiegen)
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
              p.countVereine(verein) >= v1
          }
          match {
            case Some(zielriege) if (zielriege ++ toMove).size <= maxRiegenSize2 =>
              logger.debug(s"moving $toMove from $geraetRiege to $zielriege")
              val gt = geraetRiege -- toMove
              val sg = zielriege ++ toMove
              val r1 = acccStartriegen - zielriege
              val r2 = r1 + sg
              val r3 = r2 - geraetRiege
              val ret = r3 + gt
              ret
            case Some(zielriege) => findSubstitutesFor(toMove, zielriege, maxRiegenSize2) match {
              case Some(substitues) =>
                val gt = geraetRiege -- toMove ++ substitues
                val sg = zielriege -- substitues ++ toMove
                if gt.size > maxRiegenSize2 || sg.size > maxRiegenSize2 then {
                  acccStartriegen
                } else {
                  logger.debug(s"switching $substitues with toMove between $geraetRiege to $zielriege")
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
        val evenoptimized = spreadEven(optimized, splitSex, targetDiff)
        if evenoptimized == startriegen || variantsCache.contains(evenoptimized) then {
          evenoptimized
        } else {
          _bringVereineTogether(evenoptimized, variantsCache + optimized + evenoptimized)
        }
      }
    }

    _bringVereineTogether(startriegen, Set(startriegen))
  }

  private def findSubstitutesFor(riegeToReplace: squad.GeraeteRiege, zielriege: squad.GeraeteRiege, maxRiegenSize2: Int): Option[squad.GeraeteRiege] = {
    // finde turnriegen möglichst eines Vereins in der zielriege, die nicht den verein von riegeToReplace haben, und deren ersatz durch die turnerriege von riegeToReplace die zielriege nicht über maxRiegenSize2 hinausbringen würde
    val candidates = squad.GeraeteRiege(zielriege.turnerriegen
      .filter{tr => riegeToReplace.countVereine(tr.verein).equals(0)}
      .groupBy(tr => tr.verein)
      .map{case (verein, tr) => (verein, tr.map(_.size).sum, tr)}
      .toList.sortBy(_._2).reverse
      .foldLeft(Seq.empty[TurnerRiege]){(acc, trt) =>
        val (verein, size, trl) = trt
        val candidate = acc ++ trl
        val candidatesSize = candidate.map(_.size).sum
        if candidatesSize <= riegeToReplace.size &&
           zielriege.size - candidatesSize + riegeToReplace.size <= maxRiegenSize2 then {
           candidate
        } else {
          acc
        }
      }.toSet
    )

    if candidates.nonEmpty then {
      Some(candidates)
    } else {
      None
    }
  }

  @tailrec
  private def spreadEven(startriegen: GeraeteRiegen, splitSex: SexDivideRule, targetDiff: Int, mustIncreaseQuality: Boolean = false): GeraeteRiegen = {
    /*
   * 1. Durchschnittsgrösse ermitteln
   * 2. Grösste Abweichungen ermitteln (kleinste, grösste)
   * 3. davon (teilbare) Gruppen filtern
   * 4. schieben.
   */
    type GrpStats = (squad.GeraeteRiege, Int, Int, Int, Option[TurnerRiege])
    if startriegen.isEmpty then {
      startriegen
    }
    else if startriegen.map(_.size).max - startriegen.map(_.size).min <= targetDiff then {
      startriegen
    }
    else {
      val stats: Seq[GrpStats] = startriegen.map { raw =>
        // Riege, Anz. Gruppen, Anz. Turner, Std.Abweichung, (kleinste Gruppekey, kleinste Gruppe)
        val anzTurner = raw.size
        val abweichung = anzTurner - startriegen.averageSize
        (raw, raw.size, raw.turnerriegen.size, abweichung, raw.smallestDividable)
      }.toSeq.sortBy(_._4).reverse // Abweichung

      val kleinsteGruppe@(geraeteRiegeAusKleinsterGruppe, _, anzGruppenAusKleinsterGruppe, _, turnerRiegeAusKleinsterGruppe) = stats.last

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
        val (geraeteRiege, anzahlInGruppe, anzahlGruppen, abweichung, turnerRiege) = groessteGruppe
        val b11 = turnerRiege.isDefined && groessteGruppe != kleinsteGruppe
        val b12 = checkSC(groessteGruppe, kleinsteGruppe)
        val b2 = anzahlInGruppe > startriegen.averageSize
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
            spreadEven(nextCombi, splitSex, targetDiff, mustIncreaseQuality)
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
            spreadEven(gt + startriegen.filter(sr => sr != geraeteRiegeAusGroessterGruppe && sr != geraeteRiegeAusKleinsterGruppe) + sg, splitSex, targetDiff, true)
          case _ => startriegen
        } // inner stats find match
      } // stats find match
    }
  }
}