package ch.seidel.kutu.squad

import ch.seidel.kutu._
import ch.seidel.kutu.squad._
import ch.seidel.kutu.domain._

trait Mapper {
        
  protected def buildRiegenIndex(riegen: Seq[RiegeAthletWertungen]) = riegen.flatten.toMap
  
  protected def buildWorkModel(riegen: Seq[RiegeAthletWertungen]): GeraeteRiegen = {
    riegen.map(raw => GeraeteRiege(raw.map(rt => TurnerRiege(rt._1, rt._2.size)).toSet)).toSet
  }
  
  protected def rebuildWertungen(riegen: GeraeteRiegen, index: RiegeAthletWertungen): Seq[RiegeAthletWertungen] = {
    riegen.map(gr => gr.turnerriegen.map(tr => tr.name -> index(tr.name)).toMap).toSeq
  }
  
  protected def rebuildDurchgangWertungen(riegen: Iterable[(String, String, Disziplin, Seq[WertungViewsZuAthletView])]): Map[String, Map[Disziplin, Iterable[(String,Seq[Wertung])]]] = 
    riegen.groupBy{r => r._1}
    .map{rr =>
      val (durchgang, disz) = rr
      println(durchgang)
      (durchgang, disz.groupBy(d => d._3).map{rrr =>
        val (start, athleten) = rrr
        println(start.name)
        (start, athleten.map{a =>
          val (_, riegenname, _, wertungen) = a
          println(riegenname, wertungen.size)
          (riegenname, wertungen.flatMap{wv =>
            wv._2.map(wt =>
              wt.toWertung(riegenname)
            )
          })
        })
      })
    }

}