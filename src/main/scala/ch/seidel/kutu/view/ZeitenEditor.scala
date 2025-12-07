package ch.seidel.kutu.view

import ch.seidel.kutu.domain.*
import scalafx.beans.property.*

object ZeitenEditor {
  def apply(init: WettkampfPlanTimeView) = new ZeitenEditor(init)
  val coldef = Map(
    "kategorie" -> 160,
    "disziplin" -> 340,
    "wechsel" -> 80,
    "einturnen" -> 80,
    "uebung" -> 80,
    "wertung" -> 80
  )
}

class ZeitenEditor(init: WettkampfPlanTimeView) {
  val kategorie = new ReadOnlyStringProperty(new StringProperty(init.wettkampfdisziplin.programm.easyprint))
  val disziplin = new ReadOnlyStringProperty(new StringProperty(init.wettkampfdisziplin.disziplin.easyprint))
  val wechsel = new StringProperty(f"${init.wechsel.toDouble / 1000}%1.0f ''")
  val einturnen = new StringProperty(f"${init.einturnen.toDouble / 1000}%1.0f ''")
  val uebung = new StringProperty(f"${init.uebung.toDouble / 1000}%1.0f ''")
  val wertung = new StringProperty(f"${init.wertung.toDouble / 1000}%1.0f ''")

  private def stripNonNumeric(value: String) = value.filter(_.isDigit)
  private def checkValid(value: String): Boolean = {
    try {
      val normalized = stripNonNumeric(value)
      normalized.nonEmpty && Integer.parseInt(normalized) > 0
    } catch {
      case e:NumberFormatException => false
    }
  }

  def isValid: Boolean = {
    checkValid(wechsel.value) &&
    checkValid(einturnen.value) &&
    checkValid(uebung.value) &&
    checkValid(wertung.value)
  }

  def reset(): Unit = {

  }

  def commit: WettkampfPlanTimeView = init.copy(
    wechsel = Integer.parseInt(stripNonNumeric(this.wechsel.value)) * 1000,
    einturnen = Integer.parseInt(stripNonNumeric(this.einturnen.value)) * 1000,
    uebung = Integer.parseInt(stripNonNumeric(this.uebung.value)) * 1000,
    wertung = Integer.parseInt(stripNonNumeric(this.wertung.value)) * 1000
  )
}
