package ch.seidel.kutu.view

import ch.seidel.commons._
import ch.seidel.kutu.domain._
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.event.ActionEvent
import scalafx.scene.Node
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import scalafx.scene.control._
import scalafx.scene.input.{KeyCode, KeyEvent, MouseEvent}
import scalafx.scene.layout._

import java.time.{LocalDate, Period}

class AthletSelectionDialog(actionTitle: String, wettkampfDatum: LocalDate, alterVon: Int, alterBis: Int, sex: Set[String], assignedAthleten: Seq[AthletView], service: KutuService, refreshPaneData: Set[Long]=>Unit) {
  val wkcompareJGMode = wettkampfDatum.getDayOfYear == 1
  def alter(a: AthletView): Int = {
    if (wkcompareJGMode) {
      a.gebdat.map(d => Period.between(LocalDate.of(d.toLocalDate.getYear, 1, 1), wettkampfDatum).getYears).getOrElse(100)
    } else {
      a.gebdat.map(d => Period.between(d.toLocalDate, wettkampfDatum).getYears).getOrElse(100)
    }
  }
  val athletModel = ObservableBuffer.from(
    service.selectAthletesView.filter(a => {
      sex.contains(a.geschlecht) &&
      Range.inclusive(alterVon, alterBis).contains(alter(a))
    }).
    filter { p => assignedAthleten.forall { wp => wp.id != p.id } }.
    sortBy { a => (a.activ match {case true => "A" case _ => "X"}) + ":" + a.name + ":" + a.vorname }
  )

  val filteredModel = ObservableBuffer.from(athletModel)

  val athletTable = new TableView[AthletView](filteredModel) {
    columns ++= List(
      new TableColumn[AthletView, String] {
        text = "Name Vorname Jg"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "athlet", {
            s"${x.value.toAthlet.shortPrint}"
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
            if (x.value.activ) {
              "Aktiv"
            } else {
              "Inaktiv"
            }
          })
        }
        //prefWidth = 150
      }

    )
  }

  athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)

  val btnOK = new Button("OK") {
    onAction = (event: ActionEvent) => {
      if (!athletTable.selectionModel().isEmpty) {
        val selectedAthleten = athletTable.items.value.zipWithIndex.filter {
          x => athletTable.selectionModel.value.isSelected(x._2)
        }.map(x => x._1.id)

        refreshPaneData(selectedAthleten.toSet)
      }
    }
  }

  val btnOKAll = new Button("OK, Alle") {
    onAction = (event: ActionEvent) => {
      if (filteredModel.nonEmpty) refreshPaneData(filteredModel.map(_.id).toSet)
    }
  }

  val btnNew = new Button("Neu erfassen ...") {
    onAction = (event: ActionEvent) => {
      val athlet = AthletDialog(service, (a: Athlet) => {
    	  refreshPaneData(Set(a.id))
      })
      Platform.runLater {
        athlet.execute(event)
      }
    }
  }

  val filter = new TextField() {
    promptText = "Such-Text"
    text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
      val sortOrder = athletTable.sortOrder.toList
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
      btnOKAll.disable.value = filteredModel.isEmpty
    }
  }

  athletTable.selectionModel.value.getSelectedCells.onChange {
    btnOK.disable.value = athletTable.selectionModel().isEmpty
  }
  btnOK.disable.value = true

  athletTable.onKeyPressed = (event: KeyEvent) => {
    event.code match {
      case KeyCode.Enter =>
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