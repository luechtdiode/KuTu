package ch.seidel.kutu.view

import scalafx.Includes._
import scalafx.geometry._
import scalafx.scene.control._
import scalafx.scene.layout._


case class AthletHeaderPane(wkview: TableView[IndexedSeq[WertungEditor]]) extends HBox {
  var index = -1
  var selected: IndexedSeq[WertungEditor] = IndexedSeq()

  val lblDisciplin = new Label() {
    styleClass += "toolbar-header"
  }
  val lblAthlet = new Label() {
    styleClass += "toolbar-header"
  }

  wkview.selectionModel.value.selectedItemProperty().onChange(
    (model: scalafx.beans.value.ObservableValue[IndexedSeq[WertungEditor], IndexedSeq[WertungEditor]],
     oldSelection: IndexedSeq[WertungEditor],
     newSelection: IndexedSeq[WertungEditor]) => {
      if (newSelection != null && selected != newSelection) {
        selected = newSelection
        adjust
      }
      else if (newSelection == null) {
        selected = null
        index = -1
        adjust
      }
    })

  wkview.focusModel.value.focusedCell.onChange { (focusModel, oldTablePos, newTablePos) =>
    if (newTablePos != null && selected != null) {
      val column = newTablePos.tableColumn
      val selrow = newTablePos.getRow
      if (column != null && selrow > -1) {
        if (column.isInstanceOf[WKTCAccess]) {
          val selectedIndex = column.asInstanceOf[WKTCAccess].getIndex
          if (selectedIndex > -1 && selectedIndex != index) {
            index = selectedIndex
            adjust
          }
          else if (selectedIndex < 0) {
            index = -1
            adjust
          }
        }
        else {
          index = -1
          adjust
        }
      }
      else {
        index = -1
        adjust
      }
    }
  }

  children = List(lblAthlet, lblDisciplin)
  VBox.setMargin(lblAthlet, Insets(0d, 10d, 0d, 20d))
  VBox.setMargin(lblDisciplin, Insets(0d, 10d, 0d, 40d))

  def adjust {
    if (selected != null && index > -1 && index < selected.size) {
      lblAthlet.text.value = selected(index).init.athlet.easyprint
      lblDisciplin.text.value = " : " + selected(index).init.wettkampfdisziplin.easyprint
    }
    else if (selected != null && selected.size > 0) {
      lblAthlet.text.value = selected(0).init.athlet.easyprint
      lblDisciplin.text.value = " : " + Seq(selected(0).init.riege, selected(0).init.riege2).map(_.getOrElse("")).filter {
        _.length() > 0
      }.mkString(", ")
    }
    else {
      lblAthlet.text.value = ""
      lblDisciplin.text.value = ""
    }
  }
}
