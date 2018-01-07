package ch.seidel.kutu.squad

import scala.annotation.tailrec
import ch.seidel.kutu.domain._

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
    val riegencount = rotationstation.reduce(_+_)
    if(wertungen.head.wettkampfdisziplin.notenSpez.isInstanceOf[Athletiktest]) {
      ATTGrouper.suggestRiegen(riegencount, wertungen)
    } else {
      KuTuGeTuGrouper.suggestRiegen(riegencount, wertungen)
    }
  }
  
  def generateRiegenName(w: WertungView) = {
    w.wettkampfdisziplin.notenSpez match {
      case a: Athletiktest => ATTGrouper.generateRiegenName(w)
      case _ => KuTuGeTuGrouper.generateRiegenName(w)
    }
  }
  
}