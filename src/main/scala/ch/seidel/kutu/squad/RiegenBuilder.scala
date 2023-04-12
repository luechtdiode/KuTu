package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._
import ch.seidel.kutu.squad.RiegenBuilder.selectRiegenGrouper

/** Riegenbuilder:
 *     1. Anzahl Rotationen (min = 1, max = Anzahl Teilnehmer),
 *     2. Anzahl Stationen (min = 1, max = Anzahl Diszipline im Programm),
 *     => ergibt die optimale Anzahl Riegen (Rotationen * Stationen)
 *     3. Gruppiert nach Programm oder Jahrgang (Jahrgang im Athletiktest-Modus),
 *     4. Gruppiert nach Verein oder Jahrgang (Verein im Athletiktest-Modus)
 *     => Verknüpfen der Gruppen auf eine Start-Station/-Rotation
 *     => operation suggestRiegen(WettkampfId, Rotationen/Stationen:List<Integer>): Map<Riegennummer,List<WertungId>>
 *
 *       - Ausgangsgrösse ist die Gesamtteilnehmer-Anzahl und die maximale Gruppengrösse in einer Riege
 *         z.B. 290 Teilnehmer und max 14 Turner in einer Riege
 *
 *       - Die Gruppengrösse in einer Riege sollte sich pro Durchgang einheitlich ergeben (+-3 Tu/Ti)
 *
 *       - Ein Verein darf Rotationen überspannen, innerhalb der Rotation und Kategorie aber nicht
 *         => Dies, weil sonst vom Verein zu viele Betreuer notwendig würden.
 *
 *       - Spezialfälle
 *         - Barren nur für Tu am Ende jedes Durchgangs
 *         - Parallel geführte Kategorien z.B. (K1 & K2), (K3 & K4)
 *         - Gemischt geführte Gruppen z.B. K5 - K7
 *
 *       Somit müsste jede Rotation in sich zunächst stimmen
 */
trait RiegenBuilder {

  def suggestRiegen(rotationstation: Seq[Int], wertungen: Seq[WertungView]): Seq[(String, Seq[Wertung])] = {
    val riegencount = rotationstation.sum
    val riegenmode = wertungen.head.wettkampfdisziplin.programm.riegenmode
    val aks = wertungen.head.wettkampf.altersklassen match {
      case s: String if s.nonEmpty => Some(s)
      case _ => None
    }
    val jaks = wertungen.head.wettkampf.jahrgangsklassen match {
      case s: String if s.nonEmpty => Some(s)
      case _ => None
    }
    selectRiegenGrouper(riegenmode, aks, jaks).suggestRiegen(riegencount, wertungen)
  }
}

object RiegenBuilder {

  def selectRiegenGrouper(riegenmode: Int, altersklassen: Option[String], jahrgangsklassen: Option[String]): RiegenGrouper = {
    riegenmode match {
      case RiegeRaw.RIEGENMODE_BY_JG => ATTGrouper
      case RiegeRaw.RIEGENMODE_BY_JG_VEREIN => JGClubGrouper
      case _ =>
        if (jahrgangsklassen.nonEmpty || altersklassen.nonEmpty) {
          JGClubGrouper
        } else {
          KuTuGeTuGrouper
        }
    }
  }
  def generateRiegenName(w: WertungView) = {
    val riegenmode = w.wettkampfdisziplin.programm.riegenmode
    val aks = w.wettkampf.altersklassen match {
      case s: String if s.nonEmpty => Some(s)
      case _ => None
    }
    val jaks = w.wettkampf.jahrgangsklassen match {
      case s: String if s.nonEmpty => Some(s)
      case _ => None
    }
    selectRiegenGrouper(riegenmode, aks, jaks).generateRiegenName(w)
  }

  def generateRiegen2Name(w: WertungView): Option[String] = {
    w.wettkampf.programmId match {
      case 20 if (w.athlet.geschlecht.equalsIgnoreCase("M")) =>
        Some(s"Barren ${w.wettkampfdisziplin.programm.name}")
      case _ => None
    }
  }

}