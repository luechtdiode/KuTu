package ch.seidel.kutu.view

import ch.seidel.kutu.domain._
import scalafx.beans.property._

case class CheckListBoxEditor[T](value: T, onSelectedChange: Option[(T, Boolean) => Boolean] = None) {
  val selected = BooleanProperty(true)
  if(onSelectedChange.isDefined) {
    selected onChange {
      selected.value = onSelectedChange.get(value, selected.value)
    }
  }
  override def toString = {
    if(value.isInstanceOf[DataObject]) {
      value.asInstanceOf[DataObject].easyprint
    }
    else {
      value.toString
    }
  }
}
