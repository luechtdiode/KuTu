package ch.seidel.commons

import javafx.scene.control.{TextField, cell => jfxscc}
import javafx.scene.{control => jfxsc}
import javafx.{css => jfxcss}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableSet.{Change, Remove}
import scalafx.collections.{ObservableBuffer, ObservableSet}
import scalafx.css.PseudoClass
import scalafx.delegate.SFXDelegate
import scalafx.event.subscriptions.Subscription
import scalafx.scene.control.cell._
import scalafx.scene.control._
import scalafx.scene.input.{Clipboard, KeyCode, KeyEvent}
import scalafx.util.StringConverter

import scala.language.implicitConversions

object AutoCommitTextFieldTableCell {
  implicit def sfxAutoCommitTextFieldTableCell2jfx[S, T](cell: AutoCommitTextFieldTableCell[S, T]): jfxscc.TextFieldTableCell[S, T] = if (cell != null) cell.delegate else null

  protected val PSEUDO_CLASS_FOCUSED = PseudoClass("focused")
  var lastKey: Option[String] = None
  val editmode = BooleanProperty(false)
  var editModeTerminationListener: List[() => Unit] = List.empty
  editmode.onChange {
    if (!editmode.value) {
      println(s"executing ${editModeTerminationListener.size} waiting tasks ...")
      editModeTerminationListener.foreach(task => task())
      editModeTerminationListener = List.empty
      println(s"all tasks completed")
    }
  }

  def doWhenEditmodeEnds(task: () => Unit): Unit = {
    if (!editmode.value) {
      println("not in editmode ... execute update-tasks immediately")
      task()
    } else {
      println("in editmode ... execute update-tasks later")
      editModeTerminationListener = editModeTerminationListener :+ task
    }
  }

  protected def setEditMode(flag: Boolean): Unit = {
    editmode.setValue(flag)
  }

  def forTableColumn[S](): (TableColumn[S, String] => TableCell[S, String]) =
    (view: TableColumn[S, String]) => jfxscc.TextFieldTableCell.forTableColumn[S]().call(view)

  def forTableColumn[S, T](converter: StringConverter[T]): (TableColumn[S, T] => TableCell[S, T]) =
    (view: TableColumn[S, T]) => jfxscc.TextFieldTableCell.forTableColumn[S, T](converter).call(view)

  def isEditableColumn(p: TableColumn[_, _]) = p.isVisible() && p.isEditable() && p.columns.size == 0

  def getEditableColums[T](tableView: TableView[T]) =
    tableView.columns.toList.flatMap(p => p +: p.columns.toList).filter(isEditableColumn(_))

  def selectFirstEditable[T](tableView: TableView[T]): () => Unit = {
    val editableColumns = getEditableColums(tableView)
    val ret = () => {
      if (editableColumns.nonEmpty) {
        val nextEditable = editableColumns.head
        tableView.selectionModel.value.select(0, nextEditable)
        tableView.scrollToColumn(nextEditable)
      }
      tableView.requestFocus()
    }
    ret
  }

  def selectNextEditable[T](tableView: TableView[T]): () => Unit = {
    val editableColumns = getEditableColums(tableView)
    tableView.selectionModel.value.selectedCells.headOption match {
      case Some(selected) =>
        val remaining = editableColumns.dropWhile(_ != selected.tableColumn)
        val newSelectedRowIdx = if (selected.getRow == tableView.items.value.size() - 1) 0 else selected.getRow + 1
        val ret = () => {
          if (remaining.size > 1) {
            val nextEditable = remaining.drop(1).head
            tableView.selectionModel.value.select(selected.getRow, nextEditable)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(nextEditable)
          }
          else if (editableColumns.size > 1 && tableView.items.value.size == 1) {
            val nextEditable = editableColumns.head
            tableView.selectionModel.value.select(selected.getRow, nextEditable)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(nextEditable)
          }
          else if (editableColumns.size == 1 && !editableColumns.head.equals(selected.tableColumn)) {
            tableView.selectionModel.value.select(selected.getRow, editableColumns.head)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(editableColumns.head)
          }
          else {
            tableView.selectionModel.value.select(newSelectedRowIdx, editableColumns.head)
            tableView.scrollTo(newSelectedRowIdx)
            tableView.scrollToColumn(editableColumns.head)
          }
        }
        ret
      case _ =>
        () => {}
    }
  }

  def selectPrevEditable[T](tableView: TableView[T]): () => Unit = {
    val editableColumns = getEditableColums(tableView)
    tableView.selectionModel.value.selectedCells.headOption match {
      case Some(selected) =>
        val remaining = editableColumns.reverse.dropWhile(_ != selected.tableColumn)
        val newSelectedRowIdx = if (selected.getRow == 0) tableView.items.value.size() - 1 else selected.getRow - 1
        val ret = () => {
          if (remaining.size > 1) {
            val nextEditable = remaining.drop(1).head
            tableView.selectionModel.value.select(selected.getRow, nextEditable)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(nextEditable)
          }
          else if (editableColumns.size > 1 && tableView.items.value.size == 1) {
            val nextEditable = editableColumns.last
            tableView.selectionModel.value.select(selected.getRow, nextEditable)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(nextEditable)
          }
          else if (editableColumns.size == 1 && !editableColumns.head.equals(selected.tableColumn)) {
            tableView.selectionModel.value.select(selected.getRow, editableColumns.head)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(editableColumns.head)
          }
          else {
            tableView.selectionModel.value.select(newSelectedRowIdx, editableColumns.last)
            tableView.scrollTo(newSelectedRowIdx)
            tableView.scrollToColumn(editableColumns.last)
          }
        }
        ret
      case _ =>
        () => {}
    }
  }

  def selectBelowEditable[T](tableView: TableView[T]): () => Unit = {
    val editableColumns = getEditableColums(tableView)
    tableView.selectionModel.value.selectedCells.headOption match {
      case Some(selected) =>
        val remaining = if (selected.tableColumn.isEditable) editableColumns.dropWhile(_ != selected.tableColumn) else editableColumns
        val newSelectedRowIdx = if (selected.getRow == tableView.items.value.size() - 1) 0 else selected.getRow + 1
        val movedDown = selected.getRow < newSelectedRowIdx
        val ret = () => {
          if (remaining.size == 1) {
            val nextEditable = if (!movedDown) editableColumns.head else remaining.head
            tableView.selectionModel.value.select(newSelectedRowIdx, nextEditable)
            tableView.scrollToColumn(nextEditable)
          }
          else if (remaining.size > 1) {
            val nextEditable = if (!movedDown) remaining.drop(1).head else remaining.head
            tableView.selectionModel.value.select(newSelectedRowIdx, nextEditable)
            tableView.scrollToColumn(nextEditable)
          }
          else {
            tableView.selectionModel.value.select(newSelectedRowIdx, editableColumns.head)
            tableView.scrollToColumn(editableColumns.head)
          }
          tableView.scrollTo(newSelectedRowIdx)
        }
        ret
      case _ =>
        () => {}
    }
  }

  def selectAboveEditable[T](tableView: TableView[T]): () => Unit = {
    val editableColumns = getEditableColums(tableView)
    tableView.selectionModel.value.selectedCells.headOption match {
      case Some(selected) =>

        val remaining = if (selected.tableColumn.isEditable) editableColumns.reverse.dropWhile(_ != selected.tableColumn) else editableColumns
        val newSelectedRowIdx = if (selected.getRow == 0) tableView.items.value.size() - 1 else selected.getRow - 1
        val movedUp = selected.getRow > newSelectedRowIdx
        val ret = () => {
          if (remaining.size == 1) {
            val nextEditable = if (!movedUp) editableColumns.last else remaining.head
            tableView.selectionModel.value.select(newSelectedRowIdx, nextEditable)
            tableView.scrollToColumn(nextEditable)
          }
          else if (remaining.size > 1) {
            val nextEditable = if (!movedUp) remaining.drop(1).head else remaining.head
            tableView.selectionModel.value.select(newSelectedRowIdx, nextEditable)
            tableView.scrollToColumn(nextEditable)
          }
          else {
            tableView.selectionModel.value.select(newSelectedRowIdx, editableColumns.last)
            tableView.scrollToColumn(editableColumns.head)
          }
          tableView.scrollTo(newSelectedRowIdx)
        }
        ret
      case _ =>
        () => {}
    }
  }

  def handleDefaultEditingKeyEvents[A, B](tableView: TableView[A], double: Boolean, filterText: TextField)(ke: KeyEvent): Unit = {

    val fc = tableView.focusModel.value.focusedCell.value
    val tc = fc.tableColumn.asInstanceOf[jfxsc.TableColumn[A, B]]

    val selectionstore = tableView.selectionModel.value.getSelectedCells

    def extractCoords: ObservableBuffer[(Int, Seq[Int])] = for (ts <- selectionstore) yield {
      if (ts.getColumn > -1 && ts.getTableColumn.getParentColumn != null)
        (ts.getRow, Seq(tableView.columns.indexOf(ts.getTableColumn.getParentColumn), ts.getColumn))
      else
        (ts.getRow, Seq(ts.getColumn))
    }

    val coords = extractCoords

    def restoreSelection(): Unit = if (!coords.equals(extractCoords)) try {
      for (ts <- coords) {
        if (ts._2.size > 1) {
          val toSelectParent = tableView.columns(ts._2.head)
          val firstVisible = toSelectParent.getColumns.get(ts._2.tail.head) //toSelectParent.columns(0)
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

    lastKey = None
    ke.code match {
      case KeyCode.F if ke.controlDown =>
        if (filterText != null) filterText.requestFocus()
        ke.consume()

      case KeyCode.Tab if !ke.controlDown =>
        val toSelectNextOp = selectNextEditable(tableView)
        val toSelectPrevOp = selectPrevEditable(tableView)
        val action = new Runnable() {
          override def run(): Unit = {
            if (ke.shiftDown)
              toSelectPrevOp()
            else {
              toSelectNextOp()
            }
          }
        }
        val wasEditing = tableView.delegate.getEditingCell != null
        ke.consume()
        action.run()
        if (wasEditing) {
          Platform.runLater(action)
        }

      case KeyCode.Enter =>
        val toSelectAboveOp = selectAboveEditable(tableView)
        val toSelectBelowOp = selectBelowEditable(tableView)
        val action = new Runnable() {
          override def run(): Unit = {
            if (ke.shiftDown) {
              toSelectAboveOp()
            }
            else {
              toSelectBelowOp()
            }
          }
        }
        val wasEditing = tableView.delegate.getEditingCell != null
        action.run()
        if (wasEditing) {
          ke.consume()
          Platform.runLater(action)
        }
        else {
          ke.consume()
        }

      case KeyCode.Delete if tableView.delegate.getEditingCell == null =>
        tableView.edit(fc.row, tc)

      // Paste via CTRL+V or SHIFT+INSERT
      case c if (ke.shiftDown && c == KeyCode.Insert) || (ke.controlDown && ke.text.equals("v")) =>
        lastKey = Some(Clipboard.systemClipboard.string)
        tableView.edit(fc.row, tc)

      case c if (c.isLetterKey || c.isDigitKey) && tableView.editingCell.value == null =>
        lastKey = Some(ke.getText)
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

  graphic.onChange({
    textField = graphic.value match {
      case field: TextField => Some(field)
      case _ => None
    }
    (textField, AutoCommitTextFieldTableCell.lastKey) match {
      case (Some(tf), Some(text)) => tf.setText(text)
        Platform.runLater(() => {
          tf.deselect()
          tf.end()
        })
      case _ =>
    }
  })
  editing.onChange(handleEditingState)

  def handleEditingState: Unit = {
    if (editing.value) {
      connect
    }
  }

  def connect: Subscription = new Subscription() {
    AutoCommitTextFieldTableCell.setEditMode(true)
    pseudoClassStates.onChange { (set: ObservableSet[jfxcss.PseudoClass], change: Change[jfxcss.PseudoClass]) =>
      change match {
        case Remove(PSEUDO_CLASS_FOCUSED) if delegate.isEditing || AutoCommitTextFieldTableCell.editmode.value =>
          val value = textField match {
            case Some(tf) => tf.text.value
            case None => ""
          }
          commitEdit(converter.value.fromString(value))
          AutoCommitTextFieldTableCell.setEditMode(false)

        case _ =>
      }
    }

    private val fcs = focused.onChange[java.lang.Boolean] { (_: ObservableValue[scala.Boolean, java.lang.Boolean], oldValue: java.lang.Boolean, newValue: java.lang.Boolean) =>
      if (oldValue == true && newValue == false) {
        cancel()
      }
    }

    def cancel(): Unit = {
      AutoCommitTextFieldTableCell.setEditMode(false)
      fcs.cancel()
      //      println(textField, "canceled")
    }
  }
}