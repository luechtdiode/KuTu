package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.{Disziplin, GemischteRiegen, GemischterDurchgang, GetrennteDurchgaenge, SexDivideRule}

import scala.collection.mutable

trait GeraeteRiegenBuilder extends GeraeteOptimizer with GeraeteDistributor {

  protected def buildGeraeteRiegen(
      programm: String,
      startgeraete: List[Disziplin],
      turnerRiegen: Seq[(String, Seq[WertungViewsZuAthletView])],
      maxRiegenSize: Int,
      splitSex: SexDivideRule,
      jahrgangGroup: Boolean)
      (using cache: mutable.Map[String, Int]): Seq[(String, String, Disziplin, Seq[WertungViewsZuAthletView])] = {
    if maxRiegenSize <= 0 && splitSex == GemischteRiegen then {
      val packed = packUnlimitedGemischte(turnerRiegen, startgeraete.size)
      return distributeToStartgeraete(programm, startgeraete, packed)
    }

    val athletensum = turnerRiegen.flatMap(_._2.map(_._1.id)).toSet.size
    if athletensum == 0 then return Seq.empty
    val targetDiff = BalanceConfig.targetDiff(athletensum)
    val maxRiegenSize2 = if maxRiegenSize > 0 then maxRiegenSize else athletensum
    val (effectiveSplitTarget, plannedRounds) =
      if maxRiegenSize > 0 then computeDimensions(athletensum, startgeraete.size, maxRiegenSize2)
      else (athletensum, 1)
    val splitTurnerRiegen = if maxRiegenSize > 0 then splitTurnerRiegenToMaxSize(turnerRiegen, effectiveSplitTarget) else turnerRiegen
    val combined = combineToDurchgangSize(splitTurnerRiegen, startgeraete.size, maxRiegenSize2, splitSex, targetDiff)
    val aligned = if combined.isEmpty then combined else handleVereinMerges(combined, maxRiegenSize2, splitSex, jahrgangGroup, targetDiff)
    val targetGroupCount = math.max(1, plannedRounds * startgeraete.size)
    val compacted = compactToTargetGroupCount(aligned, targetGroupCount, maxRiegenSize2, splitSex)
    val rebalanced =
      if maxRiegenSize <= 0 && splitSex == GemischteRiegen then rebalanceGemischte(compacted, targetDiff)
      else compacted
    val normalized =
      if maxRiegenSize <= 0 && splitSex == GetrennteDurchgaenge then normalizeToSingleDurchgang(rebalanced, startgeraete.size)
      else rebalanced
    distributeToStartgeraete(programm, startgeraete, normalized)
  }

  private def packUnlimitedGemischte(
      turnerRiegen: Seq[(String, Seq[WertungViewsZuAthletView])],
      startgeraeteSize: Int): Seq[RiegeAthletWertungen] = {
    if turnerRiegen.isEmpty || startgeraeteSize <= 0 then Seq.empty
    else {
      val bins = Vector.fill(startgeraeteSize)(Map.empty[String, Seq[WertungViewsZuAthletView]])
      turnerRiegen.sortBy(_._2.size).reverse.foldLeft(bins) { (acc, next) =>
        val minIndex = acc.zipWithIndex.minBy { case (m, idx) => (m.sizeOfAll, idx) }._2
        val (name, athletes) = next
        acc.updated(minIndex, acc(minIndex) + (name -> athletes))
      }.filter(_.nonEmpty)
    }
  }

  private def rebalanceGemischte(riegen: Seq[RiegeAthletWertungen], targetDiff: Int): Seq[RiegeAthletWertungen] = {
    if riegen.isEmpty then riegen
    else {
      val idx = buildRiegenIndex(riegen)
      val balanced = spreadEven(buildWorkModel(riegen), GemischteRiegen, targetDiff, mustIncreaseQuality = true)
      rebuildWertungen(balanced, idx)
    }
  }

  private def compactToTargetGroupCount(
      riegen: Seq[RiegeAthletWertungen],
      targetGroupCount: Int,
      maxRiegenSize: Int,
      splitSex: SexDivideRule): Seq[RiegeAthletWertungen] = {
    if riegen.size <= targetGroupCount || targetGroupCount <= 0 then riegen
    else {
      val riegenIndex = buildRiegenIndex(riegen)
      val desiredGroupSize = math.ceil(riegen.map(_.sizeOfAll).sum.toDouble / targetGroupCount).toInt

      @scala.annotation.tailrec
      def loop(work: GeraeteRiegen): GeraeteRiegen = {
        if work.size <= targetGroupCount then work
        else {
          val candidates = work.toSeq.combinations(2).flatMap {
            case Seq(a, b) if canMerge(a, b, maxRiegenSize, splitSex) =>
              val mergedSize = a.size + b.size
              val balancePenalty = math.abs(desiredGroupSize - mergedSize)
              Some((a, b, balancePenalty, a ~ b))
            case _ => None
          }.toSeq.sortBy(t => (t._3, t._4))

          candidates.headOption match {
            case Some((a, b, _, _)) => loop((work - a - b) + (a ++ b))
            case None => work
          }
        }
      }

      rebuildWertungen(loop(buildWorkModel(riegen)), riegenIndex)
    }
  }

  private def canMerge(a: GeraeteRiege, b: GeraeteRiege, maxRiegenSize: Int, splitSex: SexDivideRule): Boolean = {
    val withinCapacity = a.size + b.size <= maxRiegenSize
    if !withinCapacity then false
    else splitSex match {
      case GemischterDurchgang =>
        val g1 = a.turnerriegen.headOption.map(_.geschlecht)
        val g2 = b.turnerriegen.headOption.map(_.geschlecht)
        g1 == g2
      case _ => true
    }
  }

  private def normalizeToSingleDurchgang(riegen: Seq[RiegeAthletWertungen], startgeraeteSize: Int): Seq[RiegeAthletWertungen] = {
    if riegen.size <= startgeraeteSize || startgeraeteSize <= 0 then riegen
    else {
      val bins = Vector.fill(startgeraeteSize)(Map.empty[String, Seq[WertungViewsZuAthletView]])
      riegen.sortBy(_.sizeOfAll).reverse.foldLeft(bins) { (acc, nextRiege) =>
        val minIndex = acc.zipWithIndex.minBy(_._1.sizeOfAll)._2
        acc.updated(minIndex, acc(minIndex) ++ nextRiege)
      }.filter(_.nonEmpty)
    }
  }
}
