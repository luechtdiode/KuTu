package ch.seidel.commons

import javafx.{collections => jfxc}
import javafx.{css => jfxcss}
import javafx.scene.control.{cell => jfxscc}
import javafx.{util => jfxu}
import javafx.scene.control.{TextField}

import scalafx.Includes._
import scala.language.implicitConversions
import scalafx.delegate.SFXDelegate
import scalafx.scene.control.cell._
import scalafx.scene.control.{TableCell, TableColumn}
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.event.subscriptions.Subscription
import scalafx.event.Event
import scalafx.event.EventType
import scalafx.util.StringConverter
import scalafx.css.PseudoClass
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableSet
import scalafx.collections.ObservableSet.Change
import scalafx.collections.ObservableSet.Remove

object AutoCommitTextFieldTableCell {
  implicit def sfxAutoCommitTextFieldTableCell2jfx[S, T](cell: AutoCommitTextFieldTableCell[S, T]): jfxscc.TextFieldTableCell[S, T] = if (cell != null) cell.delegate else null
  protected val PSEUDO_CLASS_FOCUSED = PseudoClass("focused")

  def forTableColumn[S](): (TableColumn[S, String] => TableCell[S, String]) =
    (view: TableColumn[S, String]) => jfxscc.TextFieldTableCell.forTableColumn[S]().call(view)

  def forTableColumn[S, T](converter: StringConverter[T]): (TableColumn[S, T] => TableCell[S, T]) =
    (view: TableColumn[S, T]) => jfxscc.TextFieldTableCell.forTableColumn[S, T](converter).call(view)

}

class AutoCommitTextFieldTableCell[S, T](override val delegate: jfxscc.TextFieldTableCell[S, T] = new jfxscc.TextFieldTableCell[S, T])
  extends TableCell[S, T](delegate)
  with ConvertableCell[jfxscc.TextFieldTableCell[S, T], T, T]
  with UpdatableCell[jfxscc.TextFieldTableCell[S, T], T]
  with SFXDelegate[jfxscc.TextFieldTableCell[S, T]] {

  import AutoCommitTextFieldTableCell.PSEUDO_CLASS_FOCUSED

  def this(converter: StringConverter[T]) = {
    this(new jfxscc.TextFieldTableCell[S, T](converter))
  }

  var textField: Option[TextField] = None

  graphic.onChange( textField = if(graphic.value.isInstanceOf[TextField]) Some(graphic.value.asInstanceOf[TextField]) else None )
  editing.onChange( handleEditingState )

  def handleEditingState {
    if(editing.value) {
      connect
    }
  }

  def connect = new Subscription() {

    pseudoClassStates.onChange{(set: ObservableSet[jfxcss.PseudoClass], change: Change[jfxcss.PseudoClass]) =>
      change match {
        case Remove(PSEUDO_CLASS_FOCUSED) if(delegate.isEditing) =>
          val value = textField match {case Some(tf) => tf.text.value case None => "" }
          commitEdit(converter.value.fromString(value))

        case _ =>
      }
    }

    private val fcs = focused.onChange[java.lang.Boolean]{ (value: ObservableValue[scala.Boolean, java.lang.Boolean], oldValue: java.lang.Boolean, newValue: java.lang.Boolean) =>
      if(oldValue == true && newValue == false) {
        cancel
      }
    }

    def cancel() {
      fcs.cancel()
    }
  }
}