package ch.seidel.kutu.view

import ch.seidel.kutu.domain._
import scalafx.beans.property.DoubleProperty

case class WertungEditor(init: WertungView) {
	type WertungChangeListener = (WertungEditor) => Unit
  val noteD = DoubleProperty(Double.NaN)
  val noteE = DoubleProperty(Double.NaN)
  val endnote = DoubleProperty(Double.NaN)
  reset
  noteD.onChange {
    listeners.foreach(f => f(this))
  }
  noteE.onChange {
    listeners.foreach(f => f(this))
  }
  endnote.onChange {
    listeners.foreach(f => f(this))
  }

  private def changed(propertyValue: Double, initValue: Option[BigDecimal]): Boolean = {
    if (propertyValue == Double.NaN && initValue.isEmpty) {
      false
    } else if (Some(propertyValue) == initValue.map(_.toDouble)) {
      false
    } else {
      true
    }
  }

  def clearInput(): Unit = {
    noteD.value = Double.NaN
    noteE.value = Double.NaN
    endnote.value = Double.NaN
  }

  def isDirty =
    changed(noteD.value, init.noteD) || changed(noteE.value, init.noteE) || changed(endnote.value, init.endnote)

  var listeners = Set[WertungChangeListener]()
  def addListener(l: WertungChangeListener) {
   listeners += l
  }
  def removeListener(l: WertungChangeListener) {
    listeners -= l
  }
  def reset {
    init.noteD match {
      case Some(d) => noteD.value = d.toDouble case _ => noteD.value = Double.NaN
    }
    init.noteE match {
      case Some(d) => noteE.value = d.toDouble case _ => noteD.value = Double.NaN
    }
    init.endnote match {
      case Some(d) => endnote.value = d.toDouble case _ => noteD.value = Double.NaN
    }
  }
  def toOption(propertyValue: Double): Option[BigDecimal] =
    if (propertyValue.toString == Double.NaN.toString) None
    else Some(scala.math.BigDecimal(propertyValue))

  def toString(propertyValue: Double): String =
    if (propertyValue.toString == Double.NaN.toString) ""
    else propertyValue

  def commit = Wertung(
    init.id, init.athlet.id, init.wettkampfdisziplin.id, init.wettkampf.id, init.wettkampf.uuid.getOrElse(""),
    toOption(noteD.value),
    toOption(noteE.value),
    toOption(endnote.value),
    init.riege,
    init.riege2)
  
  def view =
    init.copy(noteD = toOption(noteD.value), noteE = toOption(noteE.value), endnote = toOption(endnote.value))
}
