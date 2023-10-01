package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._
import ch.seidel.kutu.view.WettkampfInfo

case object KuTuGeTuGrouper extends RiegenGrouper {

  override def generateRiegenName(w: WertungView) = groupKey(wkGrouper.take(wkGrouper.size-1))(w)

  override def buildGrouper(riegencnt: Int): (List[WertungView => String], List[WertungView => String], Boolean) = {
	  val wkFilteredGrouper = wkGrouper.take(if(riegencnt == 0) wkGrouper.size-1 else wkGrouper.size)
    (wkFilteredGrouper, wkGrouper, false)
  }

  val wkGrouper: List[WertungView => String] = List(
    x => x.athlet.geschlecht,
    x => x.wettkampfdisziplin.programm.name,
    x => x.athlet.verein match {
      case Some(v) =>
        if (x.team == 0) v.easyprint
        else if (x.team < 0 && x.wettkampf.extraTeams.size > x.team * -1 -1) {
          s"${x.wettkampf.extraTeams(x.team * -1 -1)}"
        }
        else s"${v.easyprint} ${x.team}"
      case None => ""
    },
    // fallback ... should not happen
    x => (x.athlet.gebdat match {case Some(d) => f"$d%tY"; case _ => ""})
  )

}