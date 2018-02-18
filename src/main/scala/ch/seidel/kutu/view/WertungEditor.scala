package ch.seidel.kutu.view

import ch.seidel.kutu.domain._
import scalafx.beans.property.DoubleProperty

case class WertungEditor(init: WertungView) {
	type WertungChangeListener = (WertungEditor) => Unit
  val noteD = DoubleProperty(init.noteD.toDouble)
  val noteE = DoubleProperty(init.noteE.toDouble)
  val endnote = DoubleProperty(init.endnote.toDouble)
  noteD.onChange {
    listeners.foreach(f => f(this))
  }
  noteE.onChange {
    listeners.foreach(f => f(this))
  }
  endnote.onChange {
    listeners.foreach(f => f(this))
  }
  def isDirty = noteD.value != init.noteD || noteE.value != init.noteE || endnote.value != init.endnote
  var listeners = Set[WertungChangeListener]()
  def addListener(l: WertungChangeListener) {
   listeners += l
  }
  def removeListener(l: WertungChangeListener) {
    listeners -= l
  }
  def reset {
    noteD.value = init.noteD
    noteE.value = init.noteE
    endnote.value = init.endnote
  }
  def commit = Wertung(
    init.id, init.athlet.id, init.wettkampfdisziplin.id, init.wettkampf.id, init.wettkampf.uuid.getOrElse(""),
    scala.math.BigDecimal(noteD.value),
    scala.math.BigDecimal(noteE.value),
    scala.math.BigDecimal(endnote.value),
    init.riege,
    init.riege2)
  
  def view = init.copy(noteD = noteD.value, noteE = noteE.value, endnote = endnote.value)    
}
