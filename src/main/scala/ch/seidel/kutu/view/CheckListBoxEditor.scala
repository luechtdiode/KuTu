package ch.seidel.kutu.view

import ch.seidel.kutu.domain.*
import scalafx.beans.property.*

case class CheckListBoxEditor[T](value: T, onSelectedChange: Option[(T, Boolean) => Boolean] = None) {
  val selected = BooleanProperty(true)
  if onSelectedChange.isDefined then {
    selected onChange {
      selected.value = onSelectedChange.get(value, selected.value)
    }
  }
  override def toString: String = {
    value match {
      case dataObject: DataObject =>
        dataObject.easyprint
      case _ =>
        value.toString
    }
  }
}
