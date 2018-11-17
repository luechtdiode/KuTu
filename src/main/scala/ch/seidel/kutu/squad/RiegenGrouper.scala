package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._

import scala.annotation.tailrec

trait RiegenGrouper extends RiegenSplitter {

  
  def suggestRiegen(riegencnt: Int, wertungen: Seq[WertungView]): Seq[WertungenZuRiege] = {

  	implicit val cache = scala.collection.mutable.Map[String, Int]()
  	val athletGroupedWertungen = wertungen.groupBy(w => w.athlet)
  	if(athletGroupedWertungen.isEmpty) {
  	  Seq[(String, Seq[Wertung])]()
  	}
  	else {
  	  val (grouper, allGrouper) = buildGrouper(riegencnt)
  	  val grouped = groupWertungen(riegencnt, athletGroupedWertungen, grouper, allGrouper)
    	splitToRiegenCount(grouped, riegencnt, cache) map toRiege
    }
  }
  
  protected def toRiege(athletenZuRiegenName: (String, Seq[WertungViewsZuAthletView])): WertungenZuRiege = { 
    val (riegenname, athletenWertungen) = athletenZuRiegenName
    
    val wertungen = athletenWertungen.flatMap{athletenWertung => {
      val (athlet, wertungen) = athletenWertung
      wertungen.map(wt => wt.toWertung(riegenname))
    }}
    
    (riegenname, wertungen)
  }
  
  protected def buildGrouper(riegencnt: Int): (List[WertungView => String], List[WertungView => String]) = ???

  def generateRiegenName(w: WertungView): String = ???

  @tailrec
  private def groupWertungen(riegencnt: Int, athletGroupedWertungen: Map[AthletView, Seq[WertungView]], grp: List[WertungView => String], grpAll: List[WertungView => String])
    (implicit cache: scala.collection.mutable.Map[String, Int]): Seq[(String, Seq[WertungViewsZuAthletView])] = {
    
    val sugg = athletGroupedWertungen.groupBy(w => groupKey(grp)(w._2.head)).toSeq
    if(riegencnt > 0 && sugg.size > riegencnt && grp.size > 1) {
      // too much groups
      // remove last grouper and try again
      groupWertungen(riegencnt, athletGroupedWertungen, grp.reverse.tail.reverse, grpAll)
    }
    else {
      // per groupkey, transform map to seq, sorted by all groupkeys
      sugg.map{x =>
        (/*grpkey*/  x._1,
         /*values*/  x._2.foldLeft((Seq[WertungViewsZuAthletView](), Set[Long]())){(acc, w) =>
            val (data, seen) = acc
            val (athlet, _ ) = w
            if(seen.contains(athlet.id)) acc else (w +: data, seen + athlet.id)
          }
          ._1.sortBy(w => groupKey(grpAll)(w._2.head))
        )
      }
    }
  }
}