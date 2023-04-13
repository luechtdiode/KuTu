package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._

case object ATTGrouper extends RiegenGrouper {

  override def generateRiegenName(w: WertungView) = groupKey(atGrouper)(w)
  
  override def buildGrouper(riegencnt: Int): (List[WertungView => String], List[WertungView => String], Boolean) = {
	  val atGrp = atGrouper.drop(1)++atGrouper.take(1)
    (atGrouper.take(3), atGrouper, true)
  }

  val atGrouper: List[WertungView => String] = List(
    x => x.wettkampfdisziplin.programm.name.shorten,
    x => x.athlet.geschlecht,
    x => (x.athlet.gebdat match {case Some(d) => f"$d%tY"; case _ => ""}),
    x => x.athlet.verein match {case Some(v) => v.easyprint case None => ""}
  )

}