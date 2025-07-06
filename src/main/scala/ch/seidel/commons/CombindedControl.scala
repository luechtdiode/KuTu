package ch.seidel.commons

import ch.seidel.commons.TextFieldWithToolButtonTableCell.defaultStringConverter
import javafx.event.ActionEvent
import javafx.scene.control.Cell
import javafx.scene.input.{KeyCode, KeyEvent}
import javafx.scene.layout.HBox
import javafx.scene.{control => jfxsc}
import javafx.util.{Callback, StringConverter}
import scalafx.Includes._
import scalafx.scene.Node
import scalafx.scene.Node.sfxNode2jfx
import scalafx.scene.control.TextField.sfxTextField2jfx
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}

import scala.language.implicitConversions

case class TextFieldWithToolButton(textField: TextField, button: Button) extends HBox {
  initGraphics()

  private def initGraphics(): Unit = {
    getStyleClass.add("combined-control")
    //button.setFocusTraversable(false)
    setFocusTraversable(true)
    button.setFocusTraversable(true)
    setSpacing(0)
    setFillHeight(false)
    textField.maxWidth = 10000
    textField.onKeyReleased = (ae) => {
      ae.getCode match {
        case c if c.isWhitespaceKey || c.isLetterKey || c.isDigitKey || c.getChar == "-" =>
          if (ae.getText.nonEmpty && !textField.editable.value && button.visible.value) {
            ae.consume()
            button.fire()
          }
        case _ =>
      }
    }
    //getAlignment <== textField.alignment
    //getChildren.setAll(textField, button)
  }

  def switchFormularMode(): Unit = {
    textField.editable = false
    textField.setFocusTraversable(false)
    button.setVisible(true)
    getChildren.setAll(textField, button)
  }

  def switchRawMode(): Unit = {
    textField.editable = true
    textField.setFocusTraversable(true)
    button.setVisible(false)
    getChildren.setAll(textField)
  }
}

object TextFieldWithToolButtonTableCell {
  def defaultStringConverter[T]: StringConverter[T] = new StringConverter[T]() {
    override def toString(t: T): String = if (t == null) null else t.toString
    override def fromString(s: String): T = s.asInstanceOf[T]
  }
  def forTableColumn[S]: Callback[TableColumn[S, String], TableCell[S, String]] = {
    forTableColumn(defaultStringConverter)
  }
  def forTableColumn[S, T](sc: StringConverter[T]): Callback[TableColumn[S, T], TableCell[S, T]] = {
    (tc: TableColumn[S, T]) => new TextFieldWithToolButtonTableCell[S, T](sc)
  }
}

class TextFieldWithToolButtonTableCell[S, T](val sc: StringConverter[T]) extends jfxsc.TableCell[S, T]{
  private var cellEditorWidget: Option[TextFieldWithToolButton] = None
  getStyleClass.add("text-field-table-cell")

  def this() = {
    this(defaultStringConverter)
  }

  override def startEdit(): Unit = {
    super.startEdit()
    if (this.isEditing) {
      cellEditorWidget = cellEditorWidget match {
        case None => Some(CellUtils.createCellEditorWidget(this, sc))
        case tf => tf
      }
      CellUtils.startEdit(this, sc, null, null, this.cellEditorWidget.get)
    }
  }

  override def cancelEdit(): Unit = {
    super.cancelEdit()
    CellUtils.cancelEdit(this, sc, null)
  }

  override def updateItem(item: T, selected: Boolean): Unit = {
    super.updateItem(item, selected)
    CellUtils.updateItem(this, sc, null, null, this.cellEditorWidget)
  }

}


object CellUtils {
  var editIcon: Image = null
  try {
    editIcon = new Image(getClass.getResourceAsStream("/images/inplace-edit.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }

  private def getItemText[T](cell: Cell[T], converter: StringConverter[T]) = if (converter == null) if (cell.getItem == null) ""
  else cell.getItem.toString
  else converter.toString(cell.getItem).trim

  def updateItem[T](cell: Cell[T], converter: StringConverter[T], box: HBox, node: Node, textfield: Option[TextFieldWithToolButton]): Unit = {
    if (cell.isEmpty) {
      cell.setText(null)
      cell.setGraphic(null)
    }
    else {
      if (cell.isEditing) {
        textfield match {
          case Some(tf) =>
            tf.textField.text = getItemText(cell, converter)
            if (node != null) {
              box.getChildren.setAll(node, tf)
              cell.setGraphic(box)
            }
            else {
              cell.setGraphic(tf)
            }
          case None =>
            cell.setText(null)
            cell.setGraphic(null)
        }
        cell.setText(null)
      }
      else {
        cell.setText(getItemText(cell, converter))
        cell.setGraphic(node)
      }
    }
  }

  def startEdit[T](cell: Cell[T], converter: StringConverter[T], box: HBox, node: Node, field: TextFieldWithToolButton): Unit = {
    if (field != null) {
      field.textField.setText(getItemText(cell, converter))
    }
    cell.setText(null)
    if (node != null) {
      box.getChildren.setAll(node, field)
      cell.setGraphic(box)
    }
    else {
      cell.setGraphic(field)
    }
    field.textField.selectAll()
    field.textField.requestFocus()
  }

  def cancelEdit[T](cell: Cell[T], converter: StringConverter[T], node: Node): Unit = {
    cell.setText(getItemText(cell, converter))
    cell.setGraphic(node)
  }

  def createCellEditorWidget[T](cell: Cell[T], converter: StringConverter[T]): TextFieldWithToolButton = {
    val tf: TextField = new TextField()
    tf.text = getItemText(cell, converter)
    tf.setOnAction((event: ActionEvent) => {
      if (converter == null) {
        throw new IllegalStateException("Attempting to convert text input into Object, but provided StringConverter is null. Be sure to set a StringConverter in your cell factory.")
      }
      else {
        cell.commitEdit(converter.fromString(tf.getText))
        event.consume()
      }

    })
    tf.setOnKeyReleased((event: KeyEvent) => {
      if (event.getCode eq KeyCode.ESCAPE) {
        cell.cancelEdit()
        event.consume()
      }

    })
    TextFieldWithToolButton(tf, new Button("..." ,new ImageView { image = editIcon }) {
    })
  }

}
