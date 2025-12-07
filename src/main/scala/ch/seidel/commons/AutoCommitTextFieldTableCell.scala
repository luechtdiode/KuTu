package ch.seidel.commons

import javafx.css as jfxcss
import javafx.scene.control as jfxsc
import javafx.scene.control.{TextField, cell as jfxscc}
import javafx.util.Callback
import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest
import org.controlsfx.control.textfield.{AutoCompletionBinding, TextFields}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.BooleanProperty.sfxBooleanProperty2jfx
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableSet.{Change, Remove}
import scalafx.collections.{ObservableBuffer, ObservableSet}
import scalafx.css.PseudoClass
import scalafx.delegate.SFXDelegate
import scalafx.event.ActionEvent
import scalafx.event.subscriptions.Subscription
import scalafx.scene.control.*
import scalafx.scene.control.cell.*
import scalafx.scene.input.*
import scalafx.util.StringConverter

import java.util
import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.language.implicitConversions

object AutoCommitTextFieldTableCell {
  implicit def sfxAutoCommitTextFieldTableCell2jfx[S, T](cell: AutoCommitTextFieldTableCell[S, T]): TextFieldWithToolButtonTableCell[S, T] = if cell != null then cell.delegate.asInstanceOf[TextFieldWithToolButtonTableCell[S, T]] else null

  protected val PSEUDO_CLASS_FOCUSED = PseudoClass("focused")
  var lastKey: Option[String] = None
  val editmode = BooleanProperty(false)
  var editModeTerminationListener: List[() => Unit] = List.empty
  editmode.onChange {
    if !editmode.value then {
      println(s"executing ${editModeTerminationListener.size} waiting tasks ...")
      editModeTerminationListener.foreach(task => task())
      editModeTerminationListener = List.empty
      println(s"all tasks completed")
    }
  }

  def doWhenEditmodeEnds(task: () => Unit): Unit = {
    if !editmode.value then {
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
      if cell != null then
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
      if editableColumns.nonEmpty then {
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
        val newSelectedRowIdx = if selected.getRow == tableView.items.value.size() - 1 then 0 else selected.getRow + 1
        val editableColumnsNewSeletedRow = getEditableColums(tableView, newSelectedRowIdx)
        val ret = () => {
          if remaining.size > 1 then {
            val nextEditable = remaining.drop(1).head
            tableView.selectionModel.value.select(selected.getRow, nextEditable)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(nextEditable)
          }
          else if editableColumns.size > 1 && tableView.items.value.size == 1 then {
            val nextEditable = editableColumns.head
            tableView.selectionModel.value.select(selected.getRow, nextEditable)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(nextEditable)
          }
          else if editableColumns.size == 1 && !editableColumns.head.equals(selected.tableColumn) then {
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
        val newSelectedRowIdx = if selected.getRow == 0 then tableView.items.value.size() - 1 else selected.getRow - 1
        val editableColumnsNewSeletedRow = getEditableColums(tableView, newSelectedRowIdx)
        val ret = () => {
          if remaining.size > 1 then {
            val nextEditable = remaining.drop(1).head
            tableView.selectionModel.value.select(selected.getRow, nextEditable)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(nextEditable)
          }
          else if editableColumns.size > 1 && tableView.items.value.size == 1 then {
            val nextEditable = editableColumns.last
            tableView.selectionModel.value.select(selected.getRow, nextEditable)
            tableView.scrollTo(selected.getRow)
            tableView.scrollToColumn(nextEditable)
          }
          else if editableColumns.size == 1 && !editableColumns.head.equals(selected.tableColumn) then {
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
        val newSelectedRowIdx = if selected.getRow == tableView.items.value.size() - 1 then 0 else selected.getRow + 1
        val editableColumns = getEditableColums(tableView, newSelectedRowIdx)
        val remaining = if selected.tableColumn.isEditable then editableColumns.dropWhile(_ != selected.tableColumn) else editableColumns
        val movedDown = selected.getRow < newSelectedRowIdx
        val ret = () => {
          if remaining.size == 1 then {
            val nextEditable = if !movedDown then editableColumns.head else remaining.head
            tableView.selectionModel.value.select(newSelectedRowIdx, nextEditable)
            tableView.scrollToColumn(nextEditable)
          }
          else if remaining.size > 1 then {
            val nextEditable = if !movedDown then remaining.drop(1).head else remaining.head
            tableView.selectionModel.value.select(newSelectedRowIdx, nextEditable)
            tableView.scrollToColumn(nextEditable)
          }
          else if editableColumns.nonEmpty then {
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
        val newSelectedRowIdx = if selected.getRow == 0 then tableView.items.value.size() - 1 else selected.getRow - 1
        val editableColumns = getEditableColums(tableView, newSelectedRowIdx)
        val remaining = if selected.tableColumn.isEditable then editableColumns.reverse.dropWhile(_ != selected.tableColumn) else editableColumns
        val movedUp = selected.getRow > newSelectedRowIdx
        val ret = () => {
          if remaining.size == 1 then {
            val nextEditable = if !movedUp then editableColumns.last else remaining.head
            tableView.selectionModel.value.select(newSelectedRowIdx, nextEditable)
            tableView.scrollToColumn(nextEditable)
          }
          else if remaining.size > 1 then {
            val nextEditable = if !movedUp then remaining.drop(1).head else remaining.head
            tableView.selectionModel.value.select(newSelectedRowIdx, nextEditable)
            tableView.scrollToColumn(nextEditable)
          }
          else if editableColumns.nonEmpty then {
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

  def handleDefaultEditingMouseEvents[A, B](tableView: TableView[A], double: Boolean, filterText: TextField)(ke: MouseEvent): Unit = {
    if tableView.editable.value then {
      val fc = tableView.focusModel.value.focusedCell.value
      val tc = fc.tableColumn.asInstanceOf[jfxsc.TableColumn[A, B]]
      if ke.isPopupTrigger || ke.clickCount == 2 then {
        ke.consume()
        lastKey = None
        tableView.edit(fc.row, tc)
      }
    }
  }

  def handleDefaultEditingKeyEvents[A, B](tableView: TableView[A], double: Boolean, filterText: TextField)(ke: KeyEvent): Unit = {

    val fc = tableView.focusModel.value.focusedCell.value
    val tc = fc.tableColumn.asInstanceOf[jfxsc.TableColumn[A, B]]

    val selectionstore = tableView.selectionModel.value.getSelectedCells

    def extractCoords: ObservableBuffer[(Int, Seq[Int])] = for ts <- selectionstore yield {
      if ts.getColumn > -1 && ts.getTableColumn.getParentColumn != null then
        (ts.getRow, Seq(tableView.columns.indexOf(ts.getTableColumn.getParentColumn), ts.getColumn))
      else
        (ts.getRow, Seq(ts.getColumn))
    }

    val coords = extractCoords

    def restoreSelection(): Unit = if !coords.equals(extractCoords) then try {
      for ts <- coords do {
        if ts._2.size > 1 then {
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
        if filterText != null then filterText.requestFocus()
        ke.consume()

      case KeyCode.Tab if !ke.controlDown =>
        val toSelectNextOp = selectNextEditable(tableView)
        val toSelectPrevOp = selectPrevEditable(tableView)
        val action = new Runnable() {
          override def run(): Unit = {
            if ke.shiftDown then
              toSelectPrevOp()
            else {
              toSelectNextOp()
            }
          }
        }
        val wasEditing = tableView.delegate.getEditingCell != null
        ke.consume()
        action.run()
        if wasEditing then {
          Platform.runLater(action)
        }

      case KeyCode.Enter =>
        val toSelectAboveOp = selectAboveEditable(tableView)
        val toSelectBelowOp = selectBelowEditable(tableView)
        val action = new Runnable() {
          override def run(): Unit = {
            if ke.shiftDown then {
              toSelectAboveOp()
            }
            else {
              toSelectBelowOp()
            }
          }
        }
        val wasEditing = tableView.delegate.getEditingCell != null
        lastKey = None
        action.run()
        if wasEditing then {
          ke.consume()
          Platform.runLater(action)
        }
        else {
          ke.consume()
        }

      case KeyCode.Delete if tableView.delegate.getEditingCell == null =>
        lastKey = Some("@DELETE@")
        tableView.edit(fc.row, tc)

      // Paste via CTRL+V or SHIFT+INSERT
      case c if (ke.shiftDown && c == KeyCode.Insert) || (ke.controlDown && ke.text.equals("v")) =>
        lastKey = Some(Clipboard.systemClipboard.string)
        tableView.edit(fc.row, tc)

      case c if ke.controlDown && ke.text.toLowerCase.equals("c") =>
        ke.consume()
        val clipboard = Clipboard.systemClipboard
        val content = new ClipboardContent
        content.putString(s"${tc.getCellObservableValue(fc.row).getValue}")
        clipboard.setContent(content)

      case c if !ke.controlDown && (c.isLetterKey || c.isDigitKey || ke.text.toLowerCase.equals("-")) && tableView.delegate.getEditingCell == null =>
        lastKey = Some(ke.getText)
        tableView.edit(fc.row, tc)

      case c if (c.isWhitespaceKey) && tableView.delegate.getEditingCell == null =>
        ke.consume()
        lastKey = Some("")
        tableView.edit(fc.row, tc)

      case _ =>
    }
  }

}

trait FormularAction[S, T] {
  def fire(cell: TextFieldWithToolButtonTableCell[S, T],actionEvent: ActionEvent): Unit
  def hasFormular(cell: TextFieldWithToolButtonTableCell[S, T]): Boolean
  def clear(cell: TextFieldWithToolButtonTableCell[S, T],actionEvent: ActionEvent): Unit
}

class AutoCommitTextFieldTableCell[S, T](
                                          val cellStateUpdater: scala.Function1[scalafx.scene.control.TableCell[S, T], scala.Unit],
                                          override val delegate: TextFieldWithToolButtonTableCell[S, T] = new TextFieldWithToolButtonTableCell[S, T],
                                          val suggestListProvider: Option[scala.Function2[S, String, List[T]]],
                                          val formularAction: Option[FormularAction[S,T]] = None
                                        )
  extends TableCell[S, T](delegate)
    with UpdatableCell[TextFieldWithToolButtonTableCell[S, T], T]
    with SFXDelegate[TextFieldWithToolButtonTableCell[S, T]] {

  import AutoCommitTextFieldTableCell.PSEUDO_CLASS_FOCUSED

  def this(converter: StringConverter[T], cellStateUpdater: scala.Function1[scalafx.scene.control.TableCell[S, T], scala.Unit]) = {
    this(cellStateUpdater, new TextFieldWithToolButtonTableCell[S, T](converter), None)
  }
  def this(converter: StringConverter[T], cellStateUpdater: scala.Function1[scalafx.scene.control.TableCell[S, T], scala.Unit], formularAction: FormularAction[S,T]) = {
    this(cellStateUpdater, new TextFieldWithToolButtonTableCell[S, T](converter), None, Some(formularAction))
  }
  def this(converter: StringConverter[T]) = {
    this((tc: TableCell[S, T])=>{}, new TextFieldWithToolButtonTableCell[S, T](converter), None)
  }
  def this(converter: StringConverter[T], suggestListProvider: scala.Function2[S, String, List[T]]) = {
    this((tc: TableCell[S, T])=>{}, new TextFieldWithToolButtonTableCell[S, T](converter), Some(suggestListProvider))
  }

  var textField: Option[TextFieldWithToolButton] = None
  val readonlyCellClass = "readonly-cell"

  editable.onChange( adjustEditablStateInStyles() )
  index.onChange( cellStateUpdater(this) )
  selected.onChange(cellStateUpdater(this))
  private def adjustEditablStateInStyles(): Unit = {
    if editable.value then {
      styleClass.delegate.remove(readonlyCellClass)
    } else {
      styleClass.delegate.add(readonlyCellClass)
    }
  }

  var lastBinding: Option[AutoCompletionBinding[T]] = None
  graphic.onChange({
    textField = graphic.value match {
      case tf@TextFieldWithToolButton(field, button) =>
        lastBinding.foreach(_.dispose())
        formularAction match {
          case Some(a) =>
            if a.hasFormular(this) then {
              tf.switchFormularMode()
              button.onAction = (ae: ActionEvent) => {
                a.fire(this, ae)
                cancelEdit()
              }
            } else {
              tf.switchRawMode()
            }
          case None =>
            tf.switchRawMode()
        }
        suggestListProvider.foreach(provider => {
          val cb: Callback[AutoCompletionBinding.ISuggestionRequest, util.Collection[T]] = (request: ISuggestionRequest) =>
            provider.apply(delegate.getTableRow.getItem, request.getUserText).asJavaCollection
          val binding = TextFields.bindAutoCompletion(field, cb, delegate.sc)
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
        Some(tf)
      case _ => None
    }
    (textField, AutoCommitTextFieldTableCell.lastKey) match {
      case (Some(TextFieldWithToolButton(tf,b)), Some(t)) =>
        val isDelete = "@DELETE@".equals(t)
        AutoCommitTextFieldTableCell.lastKey = None
        val text = if isDelete then "" else t
        if editable.value && !b.visible.value then {
          tf.setText(text)
        }
        Platform.runLater(() => {
          formularAction match {
            case Some(a) if (a.hasFormular(this)) =>
              val ae = new ActionEvent(this, b)
              if isDelete then {
                a.clear(this, ae)
                AutoCommitTextFieldTableCell.setEditMode(false)
                cancelEdit()
              } else {
                a.fire(this, ae)
                AutoCommitTextFieldTableCell.setEditMode(false)
                cancelEdit()
              }
            case _ =>
              if text.trim.isEmpty then tf.selectAll() else {
                tf.deselect()
                tf.end()
              }
          }
        })
      case (Some(TextFieldWithToolButton(tf,b)), None) =>
        Platform.runLater(() => {
          formularAction match {
            case Some(a) if (a.hasFormular(this)) =>
              val ae = new ActionEvent(this, b)
              a.fire(this, ae)
              AutoCommitTextFieldTableCell.setEditMode(false)
              cancelEdit()
            case _ =>
              tf.selectAll()
              //tf.deselect()
              //tf.end()
          }
        })
      case _ =>
    }
  })
  editing.onChange(handleEditingState)

  var connected: Option[Subscription] = None
  def handleEditingState: Unit = {
    if editing.value && editable.value then {
      connected = Some(connect)
    } else if connected.nonEmpty then {
      connected.foreach(_.cancel())
      connected = None
    }
  }

  def connect: Subscription = new Subscription() {
    AutoCommitTextFieldTableCell.setEditMode(true)

    pseudoClassStates.onChange { (set: ObservableSet[jfxcss.PseudoClass], change: Change[jfxcss.PseudoClass]) =>
      change match {
        case Remove(PSEUDO_CLASS_FOCUSED) if delegate.isEditing || AutoCommitTextFieldTableCell.editmode.value =>
          textField match {
            case Some(TextFieldWithToolButton(tf,b)) =>
              if !b.visible.value then commitEdit(delegate.sc.fromString(tf.text.value))
              AutoCommitTextFieldTableCell.setEditMode(false)

            case None =>
              AutoCommitTextFieldTableCell.setEditMode(false)
          }

        case _ =>
      }
    }

    private val fcs = focused.onChange[java.lang.Boolean] { (_: ObservableValue[scala.Boolean, java.lang.Boolean], oldValue: java.lang.Boolean, newValue: java.lang.Boolean) =>
      if oldValue == true && newValue == false then {
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