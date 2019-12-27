package ch.seidel.kutu.domain

import java.util.Date

import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

@Ignore
class KuTuWettkampfSpec extends AnyWordSpec with Matchers with KutuService {
  print(f"${new Date()}%tF")
//  print(suggestRiegen(7, Seq(6)))
  // Open a database connection

//  database withSession { implicit session =>
//    println(createWettkampf("15.02.2015", "Jugendcup 2015", Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter)))
//    println(createWettkampf("16.01.2015", "Athletiktest NKL Frühling 2015", Set(2,3)))
////    assignAthletsToWettkampf(27L, Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter))
////    assignAthletsToWettkampf(24L, Set(9,10))
//
//    def insertNotenSkala(disziplinid: Long, skala: Map[String, Double]) {
//      for(beschr <- skala.keys.toList.sortBy{skala}.reverse) {
//        sqlu"""
//          INSERT INTO kutu.notenskala
//          (wettkampfdisziplin_id, kurzbeschreibung, punktwert)
//          VALUES(${disziplinid}, ${beschr}, ${skala(beschr)})
//        """.execute
//      }
//    }
//  }
  
  "distribution" should {
    "be equal over all buckets" in {
      val riegen = (1 to 25).toSet
//      val riegen = List(1, 9, 2, 8, 3, 7, 4, 6)
      val expected = List(List(1,9), List(2,8), List(3,7), List(4,6))
      for(i <- (2 to riegen.size)) {
        println(i)
        println(solve(i, riegen).map(_.sum)) // should ===( expected)
      }
    }
  }
  
  def score(geraete: Int, einteilung: Set[Set[Int]]): Long = {
    val sum = einteilung.flatten.sum
    val eqsize = sum / geraete
    einteilung.map(riege => math.abs(riege.sum - eqsize)).foldLeft(((geraete - einteilung.size * 1L), 2L)){(acc, item) =>
      val (accDiff, factor) = acc
      (accDiff + item * factor, factor * 2L)
    }._1.longValue
  }
  
  def mergeCandidates(gerate: Int, targetSize: Int, einteilung: Set[Set[Int]]): Set[(Set[Int], Set[Int])] = {
    val candidates = einteilung.filter(_.sum < targetSize).toList.sortBy(_.sum)
    candidates.reverse.zip(candidates)
      .filter(p => p._1.size + p._2.size <= targetSize)
      .sortBy(p => math.abs(p._1.size + p._2.size - targetSize))
      .map(p => (p._1, p._2)).toSet.take(2)
  }
  
  def solve(geraete: Int, riegen: Set[Int]): Set[Set[Int]] = {
    val sum = riegen.sum
    val eqsize = sum / geraete
    val start = riegen.map(Set(_))
    val initialScore = score(geraete, start)
    println(s"sum: $sum, eqsize: $eqsize")

//    @tailrec
    def _solve(einteilung: Set[Set[Int]], acc: Set[Set[Set[Int]]], lastScore: Long): (Long, Set[Set[Int]]) = {
      mergeCandidates(geraete, eqsize, einteilung)
      .map { case (a, b) =>
        val merged = a ++ b
        val cleaned = einteilung.filterNot(p => p == a || p == b)
        cleaned + merged
      }
      .filterNot(x => acc.contains(x))
      .map{neweinteilung =>
        val newScore = score(geraete, neweinteilung)
        newScore match {
          case 0 => (newScore, neweinteilung)
          case s if(s < lastScore) => _solve(neweinteilung , acc + einteilung, s)
          case _ => (lastScore, einteilung)
        }
      }
      .toList
      .sortBy(_._1)
      .headOption
      .getOrElse((lastScore, einteilung))
    }
    
    _solve(start, Set(), initialScore)._2
  }
}