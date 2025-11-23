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
  cellFactory.value = { (_:Any) => new GeraeteRiegeListCell() }

  var textbuffer = ""

  import ch.seidel.kutu.domain.given_Conversion_String_BigDecimal

  onKeyPressed = event => {
    if event.getText.equalsIgnoreCase("R") then {
      textbuffer = "R"
      if tooltip.value == null then {
        tooltip = new Tooltip()
        tooltip.value.autoHide = false
      }
      tooltip.value.text = textbuffer
      tooltip.value.show(scene.value.getWindow)

    } else if textbuffer.startsWith("R") && event.getText.isDecimalFloat then {
      textbuffer += event.getText
      tooltip.value.text = textbuffer
      tooltip.value.show(scene.value.getWindow)
    }
    if textbuffer.length == 5 then {
      tooltip.value.text = textbuffer
      tooltip.value.show(scene.value.getWindow)
      items.value.find(p => p.sequenceId == textbuffer) match {
        case Some(riege) =>
          textbuffer = ""
          selectionModel.value.select(riege)
          val focusSetter = AutoCommitTextFieldTableCell.selectFirstEditable(tableView)
          Platform.runLater{
              focusSetter()
              if tooltip.value != null then {
                tooltip.value.hide()
                tooltip.value = null
              }
          }

        case _ =>
          textbuffer = ""
          if tooltip.value != null then {
            tooltip.value.hide()
            tooltip.value = null
          }
      }
    }
  }

  focused.onChange({
    if !focused.value then {
      textbuffer = ""
      if tooltip.value != null then {
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
