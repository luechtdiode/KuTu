package ch.seidel.kutu.view

import ch.seidel.commons._
import ch.seidel.kutu.domain._
import scalafx.Includes._
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.event.ActionEvent
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import scalafx.scene.layout._

class AthletDialog(actionTitle: String, service: KutuService, onSelected: (Athlet)=>Unit) {

  val btnOK = new Button("OK") {
    onAction = (event: ActionEvent) => {
      onSelected(null)
    }
  }

  btnOK.disable.value = true

  def execute = (event: ActionEvent) => {
    implicit val impevent = event

    PageDisplayer.showInDialog(actionTitle, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
//          top = filter
//          center = athletTable
          minWidth = 350
        }
      }
    }, btnOK)
  }
}