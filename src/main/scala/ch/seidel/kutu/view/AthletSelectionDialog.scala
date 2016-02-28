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
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.input.MouseEvent

class AthletSelectionDialog(actionTitle: String, progrm: ProgrammView, assignedAthleten: Seq[AthletView], service: KutuService, refreshPaneData: ((Long, Athlet)=>Boolean)=>Unit) {

  val athletModel = ObservableBuffer[AthletView](
    service.selectAthletesView.filter(service.altersfilter(progrm, _)).
    filter { p => /*p.activ &&*/ assignedAthleten.forall { wp => wp.id != p.id } }.
    sortBy { a => (a.activ match {case true => "A" case _ => "X"}) + ":" + a.name + ":" + a.vorname }
  )

  val filteredModel = ObservableBuffer[AthletView](athletModel)

  val athletTable = new TableView[AthletView](filteredModel) {
    columns ++= List(
      new TableColumn[AthletView, String] {
        text = "Name Vorname"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "athlet", {
            s"${x.value.name} ${x.value.vorname}"
          })
        }
        //prefWidth = 150
      },
      new TableColumn[AthletView, String] {
        text = "Verein"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "verein", {
            s"${x.value.verein.map { _.name }.getOrElse("ohne Verein")}"
          })
        }
        //prefWidth = 150
      },
      new TableColumn[AthletView, String] {
        text = "Status"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "status", {
            x.value.activ match {case true => "Aktiv" case _ => "Inaktiv"}
          })
        }
        //prefWidth = 150
      }

    )
  }

  athletTable.selectionModel.value.setSelectionMode(SelectionMode.MULTIPLE)

  val filter = new TextField() {
    promptText = "Such-Text"
    text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
      val sortOrder = athletTable.sortOrder.toList;
      filteredModel.clear()
      val searchQuery = newVal.toUpperCase().split(" ")
      for{athlet <- athletModel
      } {
        val matches = searchQuery.forall{search =>
          if(search.isEmpty() || athlet.name.toUpperCase().contains(search)) {
            true
          }
          else if(athlet.vorname.toUpperCase().contains(search)) {
            true
          }
          else if(athlet.verein match {case Some(v) => v.name.toUpperCase().contains(search) case None => false}) {
            true
          }
          else {
            false
          }
        }

        if(matches) {
          filteredModel.add(athlet)
        }
      }
      athletTable.sortOrder.clear()
      val restored = athletTable.sortOrder ++= sortOrder
      btnOKAll.disable.value = filteredModel.size == 0
    }
  }

  val btnOK = new Button("OK") {
    onAction = (event: ActionEvent) => {
      if (!athletTable.selectionModel().isEmpty) {
        val selectedAthleten = athletTable.items.value.zipWithIndex.filter {
          x => athletTable.selectionModel.value.isSelected(x._2)
        }.map(x => x._1.id)

        def filter(progId: Long, a: Athlet): Boolean = selectedAthleten.contains(a.id)
        refreshPaneData(filter)
      }
    }
  }

  val btnOKAll = new Button("OK, Alle") {
    onAction = (event: ActionEvent) => {
      if (!filteredModel.isEmpty) {
        val athlet = athletTable.selectionModel().getSelectedItem
        def filter(progId: Long, a: Athlet): Boolean = filteredModel.exists { x => x.id == a.id }
        refreshPaneData(filter)
      }
    }
  }

  val btnNew = new Button("Neu erfassen ...") {
    onAction = (event: ActionEvent) => {
      val athlet = athletTable.selectionModel().getSelectedItem
      def filter(progId: Long, a: Athlet): Boolean = filteredModel.exists { x => x.id == a.id }
      refreshPaneData(filter)
    }
  }

  athletTable.selectionModel.value.getSelectedCells.onChange {
    btnOK.disable.value = athletTable.selectionModel().isEmpty
  }
  btnOK.disable.value = true

  athletTable.onKeyPressed = (event: KeyEvent) => {
    event.code match {
      case KeyCode.ENTER =>
        if(!btnOK.disabled.value) {
          btnOK.fire()
        }
      case _ =>
    }
  }

  athletTable.onMouseClicked = (event: MouseEvent) => {
    if(event.clickCount == 2) {
      if(!btnOK.disabled.value) {
        btnOK.fire()
      }
    }
  }

  def execute = (event: ActionEvent) => {
    implicit val impevent = event

    PageDisplayer.showInDialog(actionTitle, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          top = filter
          center = athletTable
          minWidth = 350
        }
      }
    }, btnOK , btnOKAll, btnNew)
  }
}