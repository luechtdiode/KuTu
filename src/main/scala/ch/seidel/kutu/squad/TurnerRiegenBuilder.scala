package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*

trait TurnerRiegenBuilder extends RiegenSplitter {

  protected def buildTurnerRiegen(
      wertungen: Map[AthletView, Seq[WertungView]],
      grp: List[WertungView => String],
      grpAll: List[WertungView => String]): Seq[(String, Seq[WertungViewsZuAthletView])] = {
    wertungen.groupBy(w => groupKey(grp)(w._2.head)).toSeq.map { x =>
      (
        x._1,
        x._2.foldLeft((Seq.empty[WertungViewsZuAthletView], Set.empty[Long])) { (acc, w) =>
          val (data, seen) = acc
          val (athlet, _) = w
          if seen.contains(athlet.id) then acc else (w +: data, seen + athlet.id)
        }._1.sortBy(w => groupKey(grpAll)(w._2.head))
      )
    }
  }
}

