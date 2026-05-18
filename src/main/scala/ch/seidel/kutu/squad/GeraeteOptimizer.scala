package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.{GemischteRiegen, GemischterDurchgang, GetrennteDurchgaenge, SexDivideRule}

import scala.collection.mutable

trait GeraeteOptimizer extends Mapper with Stager with VereinMerger with RiegenSplitter {

  protected object BalanceConfig {
    val strictFairnessAthletesPerDurchgang: Int = 40
    val fairnessWeight: Int = 50
    val splitWeight: Int = 30
    val cohesionWeight: Int = 20

    def targetDiff(participants: Int): Int = if participants <= strictFairnessAthletesPerDurchgang then 1 else 2
  }

  protected def splitTurnerRiegenToMaxSize(
      turnerRiegen: Seq[(String, Seq[WertungViewsZuAthletView])],
      maxPerGeraet: Int)
      (using cache: mutable.Map[String, Int]): Seq[(String, Seq[WertungViewsZuAthletView])] = {
    if maxPerGeraet <= 0 then turnerRiegen
    else splitToMaxTurnerCount(turnerRiegen, maxPerGeraet, cache)
  }

  protected def combineToDurchgangSize(
      turnerRiegen: Seq[(String, Seq[WertungViewsZuAthletView])],
      startgeraeteSize: Int,
      maxRiegenSize: Int,
      splitSex: SexDivideRule,
      targetDiff: Int): Seq[RiegeAthletWertungen] = {
    val riegen = turnerRiegen.map(r => Map(r._1 -> r._2))
    val riegenindex = buildRiegenIndex(riegen)
    val workmodel = buildWorkModel(riegen)
    if workmodel.isEmpty then Seq.empty
    else {
      val combined = splitSex match {
        case GemischteRiegen =>
          buildPairs(startgeraeteSize, maxRiegenSize, workmodel, targetDiff, BalanceConfig.fairnessWeight, BalanceConfig.splitWeight, BalanceConfig.cohesionWeight)
        case GemischterDurchgang =>
          val rcm = workmodel.filter(c => c.turnerriegen.head.geschlecht.equalsIgnoreCase("M"))
          val rcw = workmodel.filter(c => c.turnerriegen.head.geschlecht.equalsIgnoreCase("W"))
          val mParticipants = rcm.sizeOfAll
          val wParticipants = rcw.sizeOfAll
          val sumParticipants = math.max(1, mParticipants + wParticipants)

          val maxMGeraete =
            if mParticipants == 0 then 0
            else if wParticipants == 0 then startgeraeteSize
            else math.max(1, math.min(startgeraeteSize - 1, math.round(startgeraeteSize.toDouble * mParticipants / sumParticipants).toInt))
          val maxWGeraete = math.max(0, startgeraeteSize - maxMGeraete)

          val male = if maxMGeraete > 0 then
            buildPairs(maxMGeraete, maxRiegenSize, rcm, targetDiff, BalanceConfig.fairnessWeight, BalanceConfig.splitWeight, BalanceConfig.cohesionWeight)
          else Set.empty[GeraeteRiege]
          val female = if maxWGeraete > 0 then
            buildPairs(maxWGeraete, maxRiegenSize, rcw, targetDiff, BalanceConfig.fairnessWeight, BalanceConfig.splitWeight, BalanceConfig.cohesionWeight)
          else Set.empty[GeraeteRiege]

          male ++ female
        case _ =>
          val rcm = workmodel.filter(c => c.turnerriegen.head.geschlecht.equalsIgnoreCase("M"))
          val rcw = workmodel.filter(c => c.turnerriegen.head.geschlecht.equalsIgnoreCase("W"))
          val maxMGeraete = startgeraeteSize * rcm.size / math.max(1, workmodel.size)
          val maxWGeraete = startgeraeteSize * rcw.size / math.max(1, workmodel.size)
          buildPairs(math.max(1, maxMGeraete), maxRiegenSize, rcm, targetDiff, BalanceConfig.fairnessWeight, BalanceConfig.splitWeight, BalanceConfig.cohesionWeight) ++
            buildPairs(math.max(1, maxWGeraete), maxRiegenSize, rcw, targetDiff, BalanceConfig.fairnessWeight, BalanceConfig.splitWeight, BalanceConfig.cohesionWeight)
      }
      rebuildWertungen(combined, riegenindex)
    }
  }

  protected def handleVereinMerges(
      geraeteRiegen: Seq[RiegeAthletWertungen],
      maxRiegenSize: Int,
      splitSex: SexDivideRule,
      jahrgangGroup: Boolean,
      targetDiff: Int): Seq[RiegeAthletWertungen] = {
    val riegenindex = buildRiegenIndex(geraeteRiegen)
    val workmodel = buildWorkModel(geraeteRiegen)
    val aligned =
      if workmodel.isEmpty || jahrgangGroup then workmodel
      else splitSex match {
        case GemischteRiegen => bringVereineTogether(workmodel, maxRiegenSize, splitSex, targetDiff)
        case GemischterDurchgang => spreadEven(workmodel, splitSex, targetDiff)
        case GetrennteDurchgaenge => bringVereineTogether(workmodel, maxRiegenSize, splitSex, targetDiff)
      }
    rebuildWertungen(aligned, riegenindex)
  }
}

