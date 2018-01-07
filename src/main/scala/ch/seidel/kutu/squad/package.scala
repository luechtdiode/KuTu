package ch.seidel.kutu

import ch.seidel.kutu.domain._
import scala.annotation.tailrec

package object squad {

  case class PreferredAccumulator(preferred: GeraeteRiege, base: GeraeteRiegen, basepreferred: GeraeteRiege, pairs: Set[(GeraeteRiege, GeraeteRiege)])

  case class TurnerRiege(name: String, size: Int)
  
  case class GeraeteRiege(turnerriegen: Set[TurnerRiege]) {
    lazy val size = turnerriegen.foldLeft(0)((acc, item) => acc + item.size)
    def ++ (other: GeraeteRiege) = GeraeteRiege(turnerriegen ++ other.turnerriegen)
    def ~ (other: GeraeteRiege) = {
      val trs1 = turnerriegen.map(_.name.split(",").last)
      val trs2 = other.turnerriegen.map(_.name.split(",").last)      
      size + other.size - trs1.map(t1 => trs2.count(t2 => t1.equals(t2))).sum
    }
    def groupScore = size - turnerriegen.toList.map(_.name.split(",").last).groupBy(x => x).map(x => x._2.size).sum
    override def toString: String = s"GeraeteRiege($size, ${turnerriegen.mkString("", ", ", ")")}"
  }

  type WertungenZuRiege = (String, Seq[Wertung])
  
  type WertungViewsZuAthletView = (AthletView, Seq[WertungView])
  
  type RiegeAthletWertungen = Map[String, Seq[WertungViewsZuAthletView]]
  implicit class RichRiegeAthletWertungen(w: RiegeAthletWertungen) {
    override def toString = w.keys.mkString(s"${w.size} Riegen (", ", ", ")")
    def durchgangRiegeSize = {
       w.values.foldLeft(0)((acc, item) => acc + item.size)
    }
  }
  
  type GeraeteRiegen = Set[GeraeteRiege]
  implicit class RichGeraeteRiegen(geraeteRiegen: GeraeteRiegen) {
    def sizeOfAll = geraeteRiegen.foldLeft(0)((acc, item) => acc + item.size)
  }
  
  implicit class ShortableString(s: String) {
    def shorten = (" " + s.split(" ").map(_.take(3) + ".").mkString(" "))
  }
  
      
}