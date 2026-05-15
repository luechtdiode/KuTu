package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.{GemischterDurchgang, SexDivideRule}

import scala.annotation.tailrec

trait VereinMerger {

  protected def bringVereineTogether(startriegen: GeraeteRiegen, maxRiegenSize2: Int, splitSex: SexDivideRule, targetDiff: Int): GeraeteRiegen = {
    @tailrec
    def loop(startriegen: GeraeteRiegen, variantsCache: Set[GeraeteRiegen]): GeraeteRiegen = {
      val optimized = startriegen.flatMap { raw =>
        raw.turnerriegen.map(r => r.verein -> raw)
      }.groupBy(_._1)
        .map(item => item._1 -> item._2.map(_._2))
        .foldLeft(startriegen) { (accStartriegen, item) =>
          val (verein, riegen) = item
          riegen.map(f => (f, f.withVerein(verein))).foldLeft(accStartriegen) { (current, riegen2) =>
            val (geraetRiege, toMove) = riegen2
            val v1 = geraetRiege.countVereine(verein)
            current.find { p =>
              p != geraetRiege && current.contains(geraetRiege) && p.countVereine(verein) >= v1
            } match {
              case Some(zielriege) if (zielriege ++ toMove).size <= maxRiegenSize2 =>
                val gt = geraetRiege -- toMove
                val sg = zielriege ++ toMove
                val base = current - zielriege - geraetRiege + sg
                if gt.nonEmpty then base + gt else base
              case Some(zielriege) =>
                findSubstitutesFor(toMove, zielriege, maxRiegenSize2) match {
                  case Some(substitues) =>
                    val gt = geraetRiege -- toMove ++ substitues
                    val sg = zielriege -- substitues ++ toMove
                    if gt.size > maxRiegenSize2 || sg.size > maxRiegenSize2 || gt.size == 0 then current
                    else current - zielriege - geraetRiege + gt + sg
                  case None => current
                }
              case None => current
            }
          }
        }

      if optimized == startriegen || variantsCache.contains(optimized) then optimized
      else {
        val evenoptimized = spreadEven(optimized, splitSex, targetDiff)
        if evenoptimized == startriegen || variantsCache.contains(evenoptimized) then evenoptimized
        else loop(evenoptimized, variantsCache + optimized + evenoptimized)
      }
    }

    loop(startriegen, Set(startriegen))
  }

  protected def findSubstitutesFor(riegeToReplace: GeraeteRiege, zielriege: GeraeteRiege, maxRiegenSize2: Int): Option[GeraeteRiege] = {
    val candidates = GeraeteRiege(zielriege.turnerriegen
      .filter(tr => riegeToReplace.countVereine(tr.verein).equals(0))
      .groupBy(_.verein)
      .map { case (verein, tr) => (verein, tr.map(_.size).sum, tr) }
      .toList.sortBy(_._2).reverse
      .foldLeft(Seq.empty[TurnerRiege]) { (acc, trt) =>
        val (_, _, trl) = trt
        val candidate = acc ++ trl
        val candidatesSize = candidate.map(_.size).sum
        if candidatesSize <= riegeToReplace.size && zielriege.size - candidatesSize + riegeToReplace.size <= maxRiegenSize2 then candidate
        else acc
      }.toSet)

    if candidates.nonEmpty then Some(candidates) else None
  }

  @tailrec
  protected final def spreadEven(startriegen: GeraeteRiegen, splitSex: SexDivideRule, targetDiff: Int, mustIncreaseQuality: Boolean = false): GeraeteRiegen = {
    type GrpStats = (GeraeteRiege, Int, Int, Int, Option[TurnerRiege])
    if startriegen.isEmpty then startriegen
    else if startriegen.map(_.size).max - startriegen.map(_.size).min <= targetDiff then startriegen
    else {
      val stats: Seq[GrpStats] = startriegen.map { raw =>
        val anzTurner = raw.size
        val abweichung = anzTurner - startriegen.averageSize
        (raw, raw.size, raw.turnerriegen.size, abweichung, raw.smallestDividable)
      }.toSeq.sortBy(_._4).reverse

      val kleinsteGruppe@(geraeteRiegeAusKleinsterGruppe, _, anzGruppenAusKleinsterGruppe, _, _) = stats.last

      def checkSC(p1: GrpStats, p2: GrpStats): Boolean = splitSex match {
        case GemischterDurchgang => p1._1.turnerriegen.head.geschlecht.equals(p2._1.turnerriegen.head.geschlecht)
        case _ => true
      }

      stats.find { groessteGruppe =>
        val (_, anzahlInGruppe, _, _, turnerRiege) = groessteGruppe
        val b11 = turnerRiege.isDefined && groessteGruppe != kleinsteGruppe
        val b12 = checkSC(groessteGruppe, kleinsteGruppe)
        val b2 = anzahlInGruppe > startriegen.averageSize
        val b3 = b11 && turnerRiege.head.size + anzGruppenAusKleinsterGruppe <= startriegen.averageSize
        lazy val v1 = groessteGruppe._1.countVereine(turnerRiege.head.verein)
        lazy val v2 = kleinsteGruppe._1.countVereine(turnerRiege.head.verein)
        lazy val b4 = v1 - turnerRiege.head.size < v2 + turnerRiege.head.size
        lazy val b5 = turnerRiege.head.size + anzGruppenAusKleinsterGruppe <= startriegen.averageSize + (anzGruppenAusKleinsterGruppe / 2)
        lazy val b6 = v1 - turnerRiege.head.size == 0
        lazy val b7 = v2 > 0

        b11 && b12 && ((b2 && b3 && b4) || (b2 && b3 && b5 && b6 && b7))
      } match {
        case Some((geraeteRiege, _, _, _, Some(turnerRiege))) =>
          val gt = geraeteRiege - turnerRiege
          val sg = geraeteRiegeAusKleinsterGruppe + turnerRiege
          val nextCombi = gt + startriegen.filter(sr => sr != geraeteRiege && sr != geraeteRiegeAusKleinsterGruppe) + sg
          if mustIncreaseQuality && nextCombi.quality > startriegen.quality then spreadEven(nextCombi, splitSex, targetDiff, mustIncreaseQuality)
          else startriegen
        case _ =>
          stats.find {
            case groessteGruppe@(_, _, anzGruppenAusGroessterGruppe, _, Some(turnerRiegeAusGroessterGruppe)) =>
              groessteGruppe != kleinsteGruppe &&
                checkSC(groessteGruppe, kleinsteGruppe) &&
                anzGruppenAusGroessterGruppe > startriegen.averageSize &&
                turnerRiegeAusGroessterGruppe.size + anzGruppenAusKleinsterGruppe <= startriegen.averageSize
            case _ => false
          } match {
            case Some((geraeteRiegeAusGroessterGruppe, _, _, _, Some(turnerRiegeAusGroessterGruppe))) =>
              val gt = geraeteRiegeAusGroessterGruppe - turnerRiegeAusGroessterGruppe
              val sg = geraeteRiegeAusKleinsterGruppe + turnerRiegeAusGroessterGruppe
              spreadEven(gt + startriegen.filter(sr => sr != geraeteRiegeAusGroessterGruppe && sr != geraeteRiegeAusKleinsterGruppe) + sg, splitSex, targetDiff, true)
            case _ => startriegen
          }
      }
    }
  }
}