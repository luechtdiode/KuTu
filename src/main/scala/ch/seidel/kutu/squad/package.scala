package ch.seidel.kutu

import ch.seidel.kutu.domain.*

import scala.math.abs

package object squad {

  case class PreferredAccumulator(preferred: GeraeteRiege, base: GeraeteRiegen, basepreferred: GeraeteRiege, pairs: Set[(GeraeteRiege, GeraeteRiege)])

  case class TurnerRiege(name: String, verein: Option[Verein], geschlecht: String, size: Int)
  
  object GeraeteRiege {
    def apply(): GeraeteRiege = GeraeteRiege(Set.empty)
  }
  case class GeraeteRiege(turnerriegen: Set[TurnerRiege]) {
    lazy val size: Int = turnerriegen.foldLeft(0)((acc, item) => acc + item.size)
    lazy val smallestDividable: Option[TurnerRiege] = {
      if turnerriegen.size > 1 then Some(turnerriegen.toSeq.minBy(_.size))
      else                      None
    }
    def ++ (other: GeraeteRiege) = GeraeteRiege(turnerriegen ++ other.turnerriegen)
    def -- (other: GeraeteRiege) = GeraeteRiege(turnerriegen -- other.turnerriegen)
    def ~ (other: GeraeteRiege): Int = {
      val trs1 = turnerriegen.map(_.verein)
      val trs2 = other.turnerriegen.map(_.verein)      
      size + other.size - trs1.map(t1 => trs2.count(t2 => t1.equals(t2))).sum
    }
    def - (turnerRiege: TurnerRiege): GeraeteRiege = copy(turnerriegen - turnerRiege)
    def + (turnerRiege: TurnerRiege): GeraeteRiege = copy(turnerriegen + turnerRiege)
    def + (geraeteRiegen: GeraeteRiegen): GeraeteRiegen = geraeteRiegen + this
    def countVereine(like: Option[Verein]): Int = {
      turnerriegen.count{tr =>
        like.equals(tr.verein)
      }
    }
    def withVerein(like: Option[Verein]) = GeraeteRiege(turnerriegen.filter(tv => like.equals(tv.verein)))
    def nonEmpty: Boolean = turnerriegen.nonEmpty
    override def toString: String = s"GeraeteRiege($size, ${turnerriegen.mkString("", ", ", ")")}"
  }

  type WertungenZuRiege = (String, Seq[Wertung])
  
  type WertungViewsZuAthletView = (AthletView, Seq[WertungView])
  
  type RiegeAthletWertungen = Map[String, Seq[WertungViewsZuAthletView]]
  implicit class RichRiegeAthletWertungen(w: RiegeAthletWertungen) {
    override def toString: String = w.keys.mkString(s"${w.size} Riegen (", ", ", ")")
    lazy val sizeOfAll: Int = w.values.foldLeft(0)((acc, item) => acc + item.size)
  }
  
  type GeraeteRiegen = Set[GeraeteRiege]
  implicit class RichGeraeteRiegen(geraeteRiegen: GeraeteRiegen) {
    lazy val sizeOfAll: Int = geraeteRiegen.foldLeft(0)((acc, item) => acc + item.size)
    lazy val averageSize: Int = sizeOfAll / geraeteRiegen.size
    lazy val quality: Int = sizeOfAll*sizeOfAll - geraeteRiegen.foldLeft(0)((acc, item) => acc + abs(item.size - averageSize))
    def + (geraeteRiege: GeraeteRiege): GeraeteRiegen = geraeteRiegen + geraeteRiege
  }
  
  implicit class ShortableString(s: String) {
    lazy val shorten: String = " " + s.split(" ").map(_.take(3) + ".").mkString(" ")
  }
  
      
}