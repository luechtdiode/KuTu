package ch.seidel.commons

import javafx.{collections => jfxc}
import javafx.scene.{ control => jfxsc }
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
import scalafx.scene.control._
import scalafx.application.Platform

object AutoCommitTextFieldTableCell {
  implicit def sfxAutoCommitTextFieldTableCell2jfx[S, T](cell: AutoCommitTextFieldTableCell[S, T]): jfxscc.TextFieldTableCell[S, T] = if (cell != null) cell.delegate else null
  protected val PSEUDO_CLASS_FOCUSED = PseudoClass("focused")

  def forTableColumn[S](): (TableColumn[S, String] => TableCell[S, String]) =
    (view: TableColumn[S, String]) => jfxscc.TextFieldTableCell.forTableColumn[S]().call(view)

  def forTableColumn[S, T](converter: StringConverter[T]): (TableColumn[S, T] => TableCell[S, T]) =
    (view: TableColumn[S, T]) => jfxscc.TextFieldTableCell.forTableColumn[S, T](converter).call(view)

  def handleDefaultEditingKeyEvents[A, B](tableView: TableView[A], double: Boolean, filterText: TextField)(ke: KeyEvent) = {
    val fc = tableView.focusModel.value.focusedCell.value
    val tc = fc.tableColumn.asInstanceOf[jfxsc.TableColumn[A,B]]

    val selectionstore = tableView.selectionModel.value.getSelectedCells
    def extractCoords = for(ts <- selectionstore) yield {
      if(ts.getColumn > -1 && ts.getTableColumn.getParentColumn != null)
        //(ts.getRow, wkview.columns.indexOf(ts.getTableColumn.getParentColumn))
        (ts.getRow, Seq(tableView.columns.indexOf(ts.getTableColumn.getParentColumn), ts.getColumn))
      else
        (ts.getRow, Seq(ts.getColumn))
    }
    val coords = extractCoords
    def restoreSelection = if(!coords.equals(extractCoords)) try {
      for(ts <- coords) {
        if(ts._2.size > 1) {
          val toSelectParent = tableView.columns(ts._2.head);
          val firstVisible = toSelectParent.getColumns.get(ts._2.tail.head)//toSelectParent.columns(0)
          tableView.selectionModel.value.select(ts._1, firstVisible)
          tableView.scrollToColumn(firstVisible)
        }
        else {
          tableView.selectionModel.value.select(ts._1, tableView.columns(ts._2.head))
          tableView.scrollToColumn(tableView.columns(ts._2.head))
        }
        tableView.scrollTo(ts._1)

      }
    }
    catch {
      case e: Exception =>
    }
    ke.code match {
      case KeyCode.F if(ke.controlDown) =>
        filterText.requestFocus()
        ke.consume()

      case KeyCode.TAB if(!ke.controlDown) =>

        val action = new Runnable() {
          override def run = {
            if(ke.shiftDown)
              tableView.selectionModel.value.selectPrevious()
            else
              tableView.selectionModel.value.selectNext()
          }
        }
        val wasEditing = tableView.delegate.getEditingCell() != null
        ke.consume()
        action.run()
        if(wasEditing) {
          Platform.runLater(action)
        }

      case KeyCode.ENTER /*if(ke.controlDown || wkview.delegate.getEditingCell() != null)*/ =>
        val action = new Runnable() {
          override def run = {
            if(ke.shiftDown) {
              val index = tableView.selectionModel.value.getSelectedIndex
              if(index == 0) {
                if(index == tableView.items.value.size()-1) {
                  tableView.selectionModel.value.selectNext()
                }
                else {
                  tableView.selectionModel.value.selectLast()
                }
              }
              else {
                tableView.selectionModel.value.selectAboveCell()
              }
            }
            else {
              val index = tableView.selectionModel.value.getSelectedIndex
              if(index == tableView.items.value.size()-1) {
                if(index == 0) {
                  tableView.selectionModel.value.selectNext()
                }
                else {
                  tableView.selectionModel.value.selectFirst()
                }
              }
              else  {
                tableView.selectionModel.value.selectBelowCell()
              }
            }
          }
        }
        val wasEditing = tableView.delegate.getEditingCell() != null
        action.run()
        if(wasEditing) {
          ke.consume()
          Platform.runLater(action)
        }
        else {
          ke.consume()
        }

      case KeyCode.DELETE if(tableView.delegate.getEditingCell() == null) =>
        tableView.edit(fc.row, tc)

      // Paste via CTRL+V or SHIFT+INSERT
      case c if(ke.shiftDown && c == KeyCode.INSERT) || (ke.controlDown && ke.text.equals("v")) =>
        tableView.edit(fc.row, tc)

      case c if((c.isLetterKey || c.isDigitKey) && tableView.editingCell.value == null) =>
        tableView.edit(fc.row, tc)

      case _ =>
    }
  }

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