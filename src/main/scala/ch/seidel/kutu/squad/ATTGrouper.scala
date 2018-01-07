package ch.seidel.kutu.squad

import scala.annotation.tailrec
import ch.seidel.kutu.domain._

case object ATTGrouper extends RiegenGrouper {

  override def generateRiegenName(w: WertungView) = groupKey(atGrouper)(w)
  
  override protected def buildGrouper(riegencnt: Int): (List[WertungView => String], List[WertungView => String]) = {
	  val atGrp = atGrouper.drop(1)++atGrouper.take(1)
    (atGrp, atGrp)
  }

  val atGrouper: List[WertungView => String] = List(
    x => x.wettkampfdisziplin.programm.name.shorten,
    x => x.athlet.geschlecht,
    x => (x.athlet.gebdat match {case Some(d) => f"$d%tY"; case _ => ""}),
    x => x.athlet.verein match {case Some(v) => v.easyprint case None => ""}
  )

}