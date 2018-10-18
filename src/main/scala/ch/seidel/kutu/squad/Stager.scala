package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._
import scala.annotation.tailrec
import ch.seidel.kutu.data._
import org.slf4j.LoggerFactory

trait Stager extends Mapper {
  private val logger = LoggerFactory.getLogger(classOf[Stager])
//  def solve(geraete: Int, maxGeraeteRiegenSize: Int, riegen: Seq[RiegeAthletWertungen]): Seq[RiegeAthletWertungen] = {
//    val riegenindex = buildRiegenIndex(riegen)
//    val workmodel = buildWorkModel(riegen)
//    val suggest = buildPairs(geraete, maxGeraeteRiegenSize, workmodel)
//    rebuildWertungen(suggest, riegenindex)
//  }

  def buildPairs(geraeteOriginal: Int, maxGeraeteRiegenSize: Int, geraeteriegen: GeraeteRiegen): GeraeteRiegen = {
    val sum = geraeteriegen.sizeOfAll
    val (eqsize, rounds) = computeDimensions(sum, geraeteOriginal, maxGeraeteRiegenSize, 1)
    val geraete = rounds * geraeteOriginal
    logger.debug(s"sum: $sum, eqsize: $eqsize, geraete: $geraete, gerateOriginal: $geraeteOriginal")

    @tailrec
    def _buildPairs(acc: Set[(Long, GeraeteRiegen)], nextCandidates: Stream[GeraeteRiegen]): (Long, GeraeteRiegen) = {
      def finish(finalAcc: Set[(Long, GeraeteRiegen)]): (Long, GeraeteRiegen) = {
        val sorted = finalAcc.toList.sortBy(_._1)
//        logger.debug("finishing with", sorted.take(3).zipWithIndex.mkString("\n(\n\t", "\n\t", "\n)"))
        sorted.headOption match {
          case Some(optimum) => optimum
          case _ => (0, Set.empty)
        }
      }
      if(nextCandidates.isEmpty) {
        finish(acc)
      }
      else {
        val withNewCandidates = nextCandidates #::: (for{
          neweinteilung <- mergeCandidates(geraete, eqsize, nextCandidates.head, Set.empty)
          s = score(geraete, eqsize, neweinteilung)
          if(neweinteilung.size >= geraete && !acc.contains((s, neweinteilung))) 
        } yield {
          neweinteilung
        }).toStream
        
        withNewCandidates match {
          case candidate #:: tail if(tail.isEmpty) => 
//            logger.debug("found end of stream")
            val sc = (score(geraete, eqsize, candidate), candidate)
            finish(acc + sc)
          case candidate #:: tail => score(geraete, eqsize, candidate) match {
            case 0 => 
//              logger.debug("found top scorer")
              (score(geraete, eqsize, candidate), candidate)
            case s =>
//              logger.debug("should seek further")
              val sc = (s, candidate)
              _buildPairs(acc + sc, tail)
          }
        }
      }
    }
    val (rank, pairs) = _buildPairs(Set(), Stream(geraeteriegen))
    //logger.debug(rank, pairs.mkString("\n ", "\n ", ""))
    pairs
  }
  protected def score(geraete: Int, eqsize: Int, einteilung: GeraeteRiegen): Long = {
    einteilung.map(riege => math.abs(riege.size - eqsize)).sum + math.abs(geraete - einteilung.size) * einteilung.size * 10L
  }
  
  @tailrec
  private def mergeCandidates(geraete: Int, targetGeraeteRiegeSize: Int, einteilung: GeraeteRiegen, acc: Set[GeraeteRiegen], preferredRiege: Option[PreferredAccumulator] = None): Set[GeraeteRiegen] = {
    val actualGeraeteRiegen = einteilung.size
    val candidates = einteilung.filter(_.size < targetGeraeteRiegeSize).toList.sortBy(_.size).reverse
    val finishedRiegen = actualGeraeteRiegen - candidates.size
    val keepUnmerged = geraete - finishedRiegen
    if (keepUnmerged == 0) {
      acc
    } else {
      lazy val min = candidates.minBy(_.size).size
      lazy val max = candidates.maxBy(_.size).size
      lazy val median = ((min + max) / 2 + targetGeraeteRiegeSize / 2) / 2
      
      val smallerCandidates = if (preferredRiege.nonEmpty) 
          candidates
          .filter(_.size + preferredRiege.get.preferred.size <= targetGeraeteRiegeSize)         
        else if(keepUnmerged == 1) candidates
        else
           candidates
           .dropWhile(_.size > median)
         
      val biggerCandidates =  if (preferredRiege.nonEmpty) 
          preferredRiege.map(_.preferred).toList
        else if(keepUnmerged == 1) candidates
        else
          candidates
          .takeWhile(_.size >= median)
        
      val validPairs = smallerCandidates
        .flatMap{candidateSmaller => biggerCandidates.map(candidateBigger => (candidateSmaller, candidateBigger))}
        .filter{case (candidateSmaller, candidateBigger) => 
          candidateSmaller != candidateBigger &&
          candidateSmaller.size + candidateBigger.size <= targetGeraeteRiegeSize
        }
        .filter(pair => preferredRiege match {
          case None => true 
          case Some(pr) => !pr.pairs.contains(pair)
        })
        .sortBy(p => ((targetGeraeteRiegeSize - p._1.size - p._2.size) * 10 + p._1 ~ p._2))
        
      def merge(s: GeraeteRiege, b: GeraeteRiege): (GeraeteRiege, GeraeteRiegen) = {
        val merged = s ++ b
        val cleaned = einteilung.filterNot(p => p == s || p == b)
        (merged, cleaned + merged)
      }
      validPairs.headOption match {
        case pairOption @ Some((s, b)) if(s.size + b.size - targetGeraeteRiegeSize == 0) =>
          val pair = pairOption.get
          val (s, b) = pair
          val (merged, newEinteilung) = merge(s, b)
          val pr = preferredRiege.map(p => p.copy(preferred = p.basepreferred, pairs = p.pairs + pair)).orElse(Some(PreferredAccumulator(merged, einteilung, b, Set(pair))))
          mergeCandidates(geraete, targetGeraeteRiegeSize, pr.get.base, acc + newEinteilung, pr)
        case Some(pair) =>
          val (s, b) = pair
          val (merged, newEinteilung) = merge(s, b)
          if(keepUnmerged == 1) {
//            logger.debug("staging not optimal")
            acc + newEinteilung
          } else {
            val pr = preferredRiege.map(p => p.copy(preferred = merged, pairs = p.pairs + pair)).orElse(Some(PreferredAccumulator(merged, einteilung, b, Set(pair))))
            mergeCandidates(geraete, targetGeraeteRiegeSize, newEinteilung, acc, pr)
          }
        case None => acc
      }
    }
  }
  
  protected def computeDimensions(sumOfAll: Int, geraete: Int, maxGeraeteRiegenSize: Int, level: Int): (Int, Int) = {
    val eqsize = (1d * sumOfAll / (geraete * level) + 0.9d).intValue()
    if (eqsize <= maxGeraeteRiegenSize) (eqsize, level) else computeDimensions(sumOfAll, geraete, maxGeraeteRiegenSize, level +1)
  }
  
}