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
  override def toString = {
    if value.isInstanceOf[DataObject] then {
      value.asInstanceOf[DataObject].easyprint
    }
    else {
      value.toString
    }
  }
}
