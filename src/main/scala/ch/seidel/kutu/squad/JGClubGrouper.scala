package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._

case object JGClubGrouper extends RiegenGrouper {

  override def generateRiegenName(w: WertungView) = groupKey(jgclubGrouper)(w)

  override protected def buildGrouper(riegencnt: Int): (List[WertungView => String], List[WertungView => String]) = {
    (jgclubGrouper, jgclubGrouper)
  }

  val jgclubGrouper: List[WertungView => String] = List(
    x => x.athlet.geschlecht,
    x => (x.athlet.gebdat match {case Some(d) => f"$d%tY"; case _ => ""}),
    x => x.athlet.verein match {case Some(v) => v.easyprint case None => ""}
  )

}