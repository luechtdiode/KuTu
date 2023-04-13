package ch.seidel.kutu.squad

import ch.seidel.kutu.data.{ByAltersklasse, ByJahrgangsAltersklasse}
import ch.seidel.kutu.domain._

case object JGClubGrouper extends RiegenGrouper {

  override def generateRiegenName(w: WertungView) = groupKey(jgclubGrouper)(w)

  override def buildGrouper(riegencnt: Int): (List[WertungView => String], List[WertungView => String], Boolean) = {
    (jgclubGrouper, jgclubGrouper, true)
  }

  def extractJGGrouper(w: WertungView): String = if (w.wettkampf.altersklassen.nonEmpty) {
    val value = ByAltersklasse("AK", Altersklasse.parseGrenzen(w.wettkampf.altersklassen, "AK")).analyze(Seq(w))
    value.head.easyprint
  } else if (w.wettkampf.jahrgangsklassen.nonEmpty) {
    ByAltersklasse("AK", Altersklasse.parseGrenzen(w.wettkampf.jahrgangsklassen, "AK")).analyze(Seq(w)).head.easyprint
  }
  else
    (w.athlet.gebdat match {case Some(d) => f"$d%tY"; case _ => ""})

  val jgclubGrouper: List[WertungView => String] = List(
    x => x.athlet.geschlecht,
    x => extractJGGrouper(x),
    x => x.wettkampfdisziplin.programm.name,
    x => x.athlet.verein match {case Some(v) => v.easyprint case None => ""}
  )
}