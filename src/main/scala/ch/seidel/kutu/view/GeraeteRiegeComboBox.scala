package ch.seidel.kutu.view

import ch.seidel.commons._
import ch.seidel.kutu.domain._
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control._


class GeraeteRiegeComboBox[T](tableView: TableView[T]) extends ComboBox[GeraeteRiege]() {
  id = "cmbDurchgaenge"

  promptText = "Durchgang-Filter"
  buttonCell = new GeraeteRiegeListCell()
  cellFactory.value = { _:Any => new GeraeteRiegeListCell() }

  var textbuffer = ""

  onKeyPressed = event => {
    if (event.getText.equalsIgnoreCase("R")) {
      textbuffer = "R"
      if (tooltip.value == null) {
        tooltip = new Tooltip()
        tooltip.value.autoHide = false
      }
      tooltip.value.text = textbuffer
      tooltip.value.show(scene.value.getWindow)

    } else if (textbuffer.startsWith("R") && event.getText.isDecimalFloat) {
      textbuffer += event.getText
      tooltip.value.text = textbuffer
      tooltip.value.show(scene.value.getWindow)
    }
    if (textbuffer.length == 5) {
      tooltip.value.text = textbuffer
      tooltip.value.show(scene.value.getWindow)
      items.value.find(p => p.sequenceId == textbuffer) match {
        case Some(riege) =>
          textbuffer = ""
          selectionModel.value.select(riege)
          val focusSetter = AutoCommitTextFieldTableCell.selectFirstEditable(tableView)
          Platform.runLater{
              focusSetter()
              if (tooltip.value != null) {
                tooltip.value.hide()
                tooltip.value = null
              }
          }

        case _ =>
          textbuffer = ""
          if (tooltip.value != null) {
            tooltip.value.hide()
            tooltip.value = null
          }
      }
    }
  }

  focused.onChange({
    if (!focused.value) {
      textbuffer = ""
      if (tooltip.value != null) {
        tooltip.value.hide()
        tooltip.value = null
      }
      val focusSetter = AutoCommitTextFieldTableCell.selectFirstEditable(tableView)
      Platform.runLater{
        focusSetter()
      }
    }
  })
}
