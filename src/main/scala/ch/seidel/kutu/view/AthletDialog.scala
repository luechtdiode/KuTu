package ch.seidel.kutu.view

import ch.seidel.commons.*
import ch.seidel.kutu.domain.*
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.*

object AthletDialog {
  def apply(service: KutuService, onSelected: Athlet =>Unit) =
    new AthletDialog(Athlet(), "Neu erfassen ...", service, onSelected)
  def apply(init: Athlet, actionTitle: String, service: KutuService, onSelected: Athlet =>Unit) =
    new AthletDialog(init, actionTitle, service, onSelected)
}
class AthletDialog(init: Athlet, actionTitle: String, service: KutuService, onSelected: Athlet =>Unit) {

  private val athletEditor = AthletEditor(init)
  val vereineList: List[Verein] = service.selectVereine
  val vereineMap: Map[Long, Verein] = vereineList.map(v => v.id -> v).toMap
  val vereine: ObservableBuffer[Verein] = ObservableBuffer.from(vereineList)

  private val lblGeschlecht = new Label {
    text = "Geschlecht (M/W)"
  }
  private val cmbGeschlecht = new ComboBox[String] {
    items.value.add("W")
    items.value.add("M")
    value <==> athletEditor.geschlecht
  }

  private val lblVerein = new Label {
    text = "Verein"
  }
  private val cmbVerein = new ComboBox[Verein] {
    items = vereine
  }

  private val txtName = new TextField {
    promptText = "Name"
    text <==> athletEditor.name
  }
  private val txtVorname = new TextField {
    promptText = "Vorname"
    text <==> athletEditor.vorname
  }
  private val txtJahrgang = new TextField {
    promptText = "Geburtsdatum"
    text <==> athletEditor.gebdat
  }
  private val txtPLZ = new TextField {
    promptText = "PLZ"
    text <==> athletEditor.plz
  }
  private val txtOrt = new TextField {
    promptText = "Ort"
    text <==> athletEditor.ort
  }
  private val txtStrasse = new TextField {
    promptText = "Strasse"
    text <==> athletEditor.strasse
  }

  val panel: GridPane = new GridPane() {
    alignment = Pos.Center
    hgrow = Priority.Always
    vgrow = Priority.Always
    minWidth = 350

    hgap = 10
    vgap = 10
//    padding = Insets(5, 5, 5, 5)
    add(lblGeschlecht, 0, 0)
    add(cmbGeschlecht, 1, 0)
    add(lblVerein,     0, 1)
    add(cmbVerein,     1, 1)
    add(txtName,       0, 2, 2, 1)
    add(txtVorname,    0, 3, 2, 1)
    add(txtJahrgang,   0, 4, 2, 1)
    add(txtPLZ,        0, 5, 2, 1)
    add(txtOrt,        0, 6, 2, 1)
    add(txtStrasse,    0, 7, 2, 1)
  }

  val btnOK: Button = new Button("OK") {
    onAction = (event: ActionEvent) => {
      onSelected(
          service.insertAthlete(
              athletEditor.commit.copy(
                  verein = Some(cmbVerein.selectionModel.value.selectedItem.value.id)
              )
          )
      )
    }
  }

  btnOK.disable.value = true

  def changeHandler(): Unit = {
    btnOK.disable.value = !athletEditor.isValid || cmbVerein.selectionModel.value.isEmpty
  }
  cmbVerein.value.onChange(changeHandler())
  cmbGeschlecht.value.onChange(changeHandler())
  txtName.text.onChange(changeHandler())
  txtVorname.text.onChange(changeHandler())
  txtJahrgang.text.onChange(changeHandler())
  txtPLZ.text.onChange(changeHandler())
  txtOrt.text.onChange(changeHandler())
  txtStrasse.text.onChange(changeHandler())

  def execute: ActionEvent => Unit = (event: ActionEvent) => {
    PageDisplayer.showInDialog(actionTitle, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
//          top = filter
          center = panel
//          minWidth = 350
        }
      }
    }, btnOK)(using event)
  }
}