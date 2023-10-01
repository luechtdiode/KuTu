package ch.seidel.kutu.squad

import ch.seidel.kutu.data.{ByAltersklasse, ByJahrgangsAltersklasse}
import ch.seidel.kutu.domain._

case object JGClubGrouper extends RiegenGrouper {

  override def generateRiegenName(w: WertungView) = groupKey(jgclubGrouper)(w)

  override def buildGrouper(riegencnt: Int): (List[WertungView => String], List[WertungView => String], Boolean) = {
    (jgclubGrouper, jgclubGrouper, true)
  }

  def extractSexGrouper(w: WertungView): String = if (extractJGGrouper(w).contains(w.athlet.geschlecht)) "" else w.athlet.geschlecht

  def extractProgrammGrouper(w: WertungView): String = if (extractJGGrouper(w).contains(w.wettkampfdisziplin.programm.name)) "" else w.wettkampfdisziplin.programm.name

  def extractJGGrouper(w: WertungView): String = if (w.wettkampf.altersklassen.get.nonEmpty) {
    ByAltersklasse("AK", Altersklasse.parseGrenzen(w.wettkampf.altersklassen.get, "AK")).analyze(Seq(w)).head.asInstanceOf[Altersklasse].easyprintShort
  } else if (w.wettkampf.jahrgangsklassen.nonEmpty) {
    ByJahrgangsAltersklasse("AK", Altersklasse.parseGrenzen(w.wettkampf.jahrgangsklassen.get, "AK")).analyze(Seq(w)).head.asInstanceOf[Altersklasse].easyprintShort
  } else
    (w.athlet.gebdat match {
      case Some(d) => f"$d%tY";
      case _ => ""
    })

  val jgclubGrouper: List[WertungView => String] = List(
    x => extractSexGrouper(x),
    x => extractProgrammGrouper(x),
    x => extractJGGrouper(x),
    x => x.athlet.verein match {
      case Some(v) =>
        if (x.team == 0) v.easyprint
        else if (x.team < 0 && x.wettkampf.extraTeams.size > x.team * -1 -1) {
          s"${x.wettkampf.extraTeams(x.team * -1 -1)}"
        }
        else s"${v.easyprint} ${x.team}"
      case None => ""
    },
  )
}