package ch.seidel.commons

import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding
import javafx.scene.control.{TextField, cell => jfxscc}
import javafx.scene.{control => jfxsc}
import javafx.util.Callback
import javafx.{css => jfxcss}
import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest
import org.controlsfx.control.textfield.{AutoCompletionBinding, TextFields}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.BooleanProperty.sfxBooleanProperty2jfx
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

import java.util
import java.util.Collection
import scala.jdk.CollectionConverters.IterableHasAsJava
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

  def isEditableColumn[S, T](p: TableColumn[S, T], rowIndex: Int): Boolean = {
    p.isVisible && p.isEditable && p.columns.isEmpty && {
      import javafx.scene.AccessibleAttribute
      val colIndex = p.tableView.value.getVisibleLeafIndex(p)
      val cell = p.tableView.delegate.value.queryAccessibleAttribute(AccessibleAttribute.CELL_AT_ROW_COLUMN, rowIndex, colIndex).asInstanceOf[jfxsc.IndexedCell[T]]
      if (cell != null)
        cell.isEditable
      else
        false
    }
  }

  def getEditableColums[T](tableView: TableView[T], row: Int) =
    tableView.columns.toList.flatMap(p => p +: p.columns.toList).filter(tc => isEditableColumn(tc, row))

  def selectFirstEditable[T](tableView: TableView[T]): () => Unit = {
    val editableColumns = getEditableColums(tableView, 0)
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
    tableView.selectionModel.value.selectedCells.headOption match {
      case Some(selected) =>
        val editableColumns = getEditableColums(tableView, selected.row)
        val remaining = editableColumns.dropWhile(_ != selected.tableColumn)
        val newSelectedRowIdx = if (selected.getRow == tableView.items.value.size() - 1) 0 else selected.getRow + 1
        val editableColumnsNewSeletedRow = getEditableColums(tableView, newSelectedRowIdx)
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
            tableView.selectionModel.value.select(newSelectedRowIdx, editableColumnsNewSeletedRow.head)
            tableView.scrollTo(newSelectedRowIdx)
            tableView.scrollToColumn(editableColumnsNewSeletedRow.head)
          }
        }
        ret
      case _ =>
        () => {}
    }
  }

  def selectPrevEditable[T](tableView: TableView[T]): () => Unit = {
    tableView.selectionModel.value.selectedCells.headOption match {
      case Some(selected) =>
        val editableColumns = getEditableColums(tableView, selected.row)
        val remaining = editableColumns.reverse.dropWhile(_ != selected.tableColumn)
        val newSelectedRowIdx = if (selected.getRow == 0) tableView.items.value.size() - 1 else selected.getRow - 1
        val editableColumnsNewSeletedRow = getEditableColums(tableView, newSelectedRowIdx)
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
            tableView.selectionModel.value.select(newSelectedRowIdx, editableColumnsNewSeletedRow.last)
            tableView.scrollTo(newSelectedRowIdx)
            tableView.scrollToColumn(editableColumnsNewSeletedRow.last)
          }
        }
        ret
      case _ =>
        () => {}
    }
  }

  def selectBelowEditable[T](tableView: TableView[T]): () => Unit = {
    tableView.selectionModel.value.selectedCells.headOption match {
      case Some(selected) =>
        val newSelectedRowIdx = if (selected.getRow == tableView.items.value.size() - 1) 0 else selected.getRow + 1
        val editableColumns = getEditableColums(tableView, newSelectedRowIdx)
        val remaining = if (selected.tableColumn.isEditable) editableColumns.dropWhile(_ != selected.tableColumn) else editableColumns
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
          else if (editableColumns.nonEmpty) {
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
    tableView.selectionModel.value.selectedCells.headOption match {
      case Some(selected) =>
        val newSelectedRowIdx = if (selected.getRow == 0) tableView.items.value.size() - 1 else selected.getRow - 1
        val editableColumns = getEditableColums(tableView, newSelectedRowIdx)
        val remaining = if (selected.tableColumn.isEditable) editableColumns.reverse.dropWhile(_ != selected.tableColumn) else editableColumns
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
          else if (editableColumns.nonEmpty) {
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

      case c if (c.isLetterKey || c.isDigitKey || c.getChar == "-") && tableView.delegate.getEditingCell == null =>
        lastKey = Some(ke.getText)
        tableView.edit(fc.row, tc)

      case _ =>
    }
  }

}

class AutoCommitTextFieldTableCell[S, T](
                                          val cellStateUpdater: scala.Function1[scalafx.scene.control.TableCell[S, T], scala.Unit],
                                          override val delegate: jfxscc.TextFieldTableCell[S, T] = new jfxscc.TextFieldTableCell[S, T],
                                          val suggestListProvider: Option[scala.Function2[S, String, List[T]]])
  extends TableCell[S, T](delegate)
    with ConvertableCell[jfxscc.TextFieldTableCell[S, T], T, T]
    with UpdatableCell[jfxscc.TextFieldTableCell[S, T], T]
    with SFXDelegate[jfxscc.TextFieldTableCell[S, T]] {

  import AutoCommitTextFieldTableCell.PSEUDO_CLASS_FOCUSED


  def this(converter: StringConverter[T], cellStateUpdater: scala.Function1[scalafx.scene.control.TableCell[S, T], scala.Unit]) = {
    this(cellStateUpdater, new jfxscc.TextFieldTableCell[S, T](converter), None)
  }
  def this(converter: StringConverter[T]) = {
    this((tc: TableCell[S, T])=>{}, new jfxscc.TextFieldTableCell[S, T](converter), None)
  }
  def this(converter: StringConverter[T], suggestListProvider: scala.Function2[S, String, List[T]]) = {
    this((tc: TableCell[S, T])=>{}, new jfxscc.TextFieldTableCell[S, T](converter), Some(suggestListProvider))
  }

  var textField: Option[TextField] = None
  val readonlyCellClass = "readonly-cell"

  editable.onChange( adjustEditablStateInStyles() )
  index.onChange( cellStateUpdater(this) )
  selected.onChange(cellStateUpdater(this))
  private def adjustEditablStateInStyles(): Unit = {
    if (editable.value) {
      styleClass.delegate.remove(readonlyCellClass)
    } else {
      styleClass.delegate.add(readonlyCellClass)
    }
  }

  var lastBinding: Option[AutoCompletionBinding[T]] = None
  graphic.onChange({
    textField = graphic.value match {
      case field: TextField =>
        lastBinding.foreach(_.dispose())
        suggestListProvider.foreach(provider => {
          val cb: Callback[AutoCompletionBinding.ISuggestionRequest, util.Collection[T]] = (request: ISuggestionRequest) =>
            provider.apply(delegate.getTableRow.getItem, request.getUserText).asJavaCollection
          val binding = TextFields.bindAutoCompletion(field, cb, converter.value)
          binding.setVisibleRowCount(20)
          binding.setOnAutoCompleted{ evt =>
            commitEdit(evt.getCompletion)
            AutoCommitTextFieldTableCell.setEditMode(false)
          }
          field.widthProperty().onChange {
            binding.setMinWidth(Math.max(binding.getPrefWidth, field.getWidth + 10d))
          }
          binding.setMinWidth(Math.max(binding.getPrefWidth, field.getWidth + 10d))
          lastBinding = Some(binding)
        })
        Some(field)
      case _ => None
    }
    (textField, AutoCommitTextFieldTableCell.lastKey) match {
      case (Some(tf), Some(text)) => tf.setText(text)
        Platform.runLater(() => {
          if (editable.value) {
            tf.editable = true
            tf.deselect()
            tf.end()
          } else {
            tf.editable = false
          }
        })
      case _ =>
    }
  })
  editing.onChange(handleEditingState)

  def handleEditingState: Unit = {
    if (editing.value && editable.value) {
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