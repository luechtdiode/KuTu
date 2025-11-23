package ch.seidel.kutu.squad

import ch.seidel.kutu.data.{ByAltersklasse, ByJahrgangsAltersklasse}
import ch.seidel.kutu.domain.*

case object JGClubGrouper extends RiegenGrouper {

  override def generateRiegenName(w: WertungView): String = groupKey(jgclubGrouper)(w)

  override def buildGrouper(riegencnt: Int): (List[WertungView => String], List[WertungView => String], Boolean) = {
    (jgclubGrouper, jgclubGrouper, true)
  }

  private def extractSexGrouper(w: WertungView): String = if extractJGGrouper(w).contains(w.athlet.geschlecht) then "" else w.athlet.geschlecht

  private def extractProgrammGrouper(w: WertungView): String = if extractJGGrouper(w).contains(w.wettkampfdisziplin.programm.name) then "" else w.wettkampfdisziplin.programm.name

  private def extractJGGrouper(w: WertungView): String = if w.wettkampf.altersklassen.get.nonEmpty then {
    ByAltersklasse("AK", Altersklasse.parseGrenzen(w.wettkampf.altersklassen.get, "AK")).analyze(Seq(w)).head.asInstanceOf[Altersklasse].easyprintShort
  } else if w.wettkampf.jahrgangsklassen.nonEmpty then {
    ByJahrgangsAltersklasse("AK", Altersklasse.parseGrenzen(w.wettkampf.jahrgangsklassen.get, "AK")).analyze(Seq(w)).head.asInstanceOf[Altersklasse].easyprintShort
  } else
    w.athlet.gebdat match {
      case Some(d) => f"$d%tY";
      case _ => ""
    }

  private val jgclubGrouper: List[WertungView => String] = List(
    x => extractSexGrouper(x),
    x => extractProgrammGrouper(x),
    x => extractJGGrouper(x),
    x => x.teamName,
  )
}