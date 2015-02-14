package ch.seidel.commons

import scalafx.Includes._
import np.com.ngopal.{ control => nfxsc }
import javafx.scene.{control => jfxsc}
import javafx.scene.{text => jfxst}
import scalafx.delegate.SFXDelegate
import scalafx.delegate.AlignmentDelegate
import javafx.{ event => jfxe }
import scalafx.beans.property.IntegerProperty
import scalafx.scene.control.TextInputControl
import scalafx.scene.control.Control
import scalafx.beans.property._

object AutoFillTextBox {
  implicit def sfxTextField2jfx[T](v: AutoFillTextBox[T]) = v.delegate
}

class AutoFillTextBox[T](override val delegate: nfxsc.AutoFillTextBox[T] = new nfxsc.AutoFillTextBox[T])
  extends Control(delegate)
//  with AlignmentDelegate[nfxsc.AutoFillTextBox[T]]
  with SFXDelegate[nfxsc.AutoFillTextBox[T]] {

  /**
   * The action handler associated with this text field, or null if no action handler is assigned.
   */
  def onAction = delegate.getTextbox.onActionProperty
  def onAction_=(v: jfxe.EventHandler[jfxe.ActionEvent]) {
    onAction() = v
  }

  /**
   * The preferred number of text columns.
   */
  def prefColumnCount: IntegerProperty = delegate.getTextbox.prefColumnCountProperty
  def prefColumnCount_=(v: Int) {
    prefColumnCount() = v
  }
  /**
   * Indicates whether this TextInputControl can be edited by the user.
   */
  def editable: BooleanProperty = delegate.getTextbox.editableProperty
  def editable_=(v: Boolean) {
    editable() = v
  }

  /**
   * The default font to use for text in the TextInputControl.
   */
  def font: ObjectProperty[jfxst.Font] = delegate.getTextbox.fontProperty()
  def font_=(v: jfxst.Font) {
    ObjectProperty.fillProperty[jfxst.Font](font, v)
  }

  /**
   * The number of characters in the text input.
   */
  def length: ReadOnlyIntegerProperty = delegate.getTextbox.lengthProperty

  /**
   * Defines the characters in the TextInputControl which are selected
   */
  def selectedText: ReadOnlyStringProperty = delegate.getTextbox.selectedTextProperty

  /**
   * The current selection.
   */
  def selection: ReadOnlyObjectProperty[jfxsc.IndexRange] = delegate.getTextbox.selectionProperty

  /**
   * The textual content of this TextInputControl.
   */
  def text: StringProperty = delegate.getTextbox.textProperty
  def text_=(v: String) {
    text() = v
  }

  /**
   * The prompt text to display in the TextInputControl, or null if no prompt text is displayed.
   * @since 2.2
   */
  def promptText: StringProperty = delegate.getTextbox.promptTextProperty()
  def promptText_=(v: String) {
    promptText() = v
  }

}