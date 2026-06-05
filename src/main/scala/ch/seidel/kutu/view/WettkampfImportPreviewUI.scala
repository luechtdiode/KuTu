package ch.seidel.kutu.view

import ch.seidel.commons.{DisplayablePage, PageDisplayer}
import ch.seidel.kutu.domain.{Athlet, AthletView, ProgrammView, Verein, Wertung, WettkampfImportService}
import scalafx.Includes.*
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.control.TableColumn.*
import scalafx.scene.layout.*

class WettkampfImportPreviewUI {
  private def buildSearchFilter[T](
      sourceModel: ObservableBuffer[T],
      filteredModel: ObservableBuffer[T],
      table: TableView[T],
      matcher: (T, String) => Boolean): TextField = {
    new TextField() {
      promptText = "Such-Text"
      text.addListener { (_, _, newVal) =>
        val sortOrder = table.sortOrder.toList
        filteredModel.clear()
        val searchQuery = newVal.toUpperCase.split(" ")
        sourceModel.foreach { row =>
          val matches = searchQuery.forall(search => search.isEmpty || matcher(row, search))
          if matches then filteredModel.add(row)
        }
        table.sortOrder.clear()
        table.sortOrder ++= sortOrder
      }
    }
  }

  private def selectionFromTable[T](table: TableView[T]): Seq[T] = {
    table.items.value.zipWithIndex.filter(x => table.selectionModel.value.isSelected(x._2)).map(_._1).toList
  }

  def showPdfImportPreview(
      rows: Seq[(Long, Athlet, AthletView, List[Wertung])],
      onSelected: Seq[(Long, Athlet, AthletView, List[Wertung])] => Unit,
      onAll: Seq[(Long, Athlet, AthletView, List[Wertung])] => Unit)(using event: ActionEvent): Unit = {
    val sourceModel = ObservableBuffer.from(rows)
    val filteredModel = ObservableBuffer.from(sourceModel)
    val athletTable = new TableView[(Long, Athlet, AthletView, List[Wertung])](filteredModel) {
      columns ++= List(
        new TableColumn[(Long, Athlet, AthletView, List[Wertung]), String] {
          text = "Athlet (Name, Vorname, JG)"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "athlet", s"${x.value._2.shortPrint}")
          }
          minWidth = 250
        },
        new TableColumn[(Long, Athlet, AthletView, List[Wertung]), String] {
          text = "Verein"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "verein", s"${x.value._3.verein.get.extendedprint}")
          }
        },
        new TableColumn[(Long, Athlet, AthletView, List[Wertung]), String] {
          text = "Importvorschlag"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "vorschlag", if x.value._3.id > 0 then "existierend" else "neu importieren")
          }
        }
      )
    }
    athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)

    val filter = buildSearchFilter(sourceModel, filteredModel, athletTable, (row, search) => {
      row._2.name.toUpperCase.contains(search) ||
      row._2.vorname.toUpperCase.contains(search) ||
      row._3.verein.exists(_.name.toUpperCase.contains(search))
    })

    PageDisplayer.showInDialog("Aus Rangliste laden ...", new DisplayablePage() {
      override def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          minWidth = 600
          center = new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            top = filter
            center = athletTable
            minWidth = 550
          }
        }
      }
    }, new Button("OK") {
      onAction = _ =>
        if !athletTable.selectionModel().isEmpty then onSelected(selectionFromTable(athletTable))
    }, new Button("OK Alle") {
      onAction = _ => onAll(filteredModel.toList)
    })
  }

  def showCsvImportPreview(
      rows: Seq[ch.seidel.kutu.domain.WettkampfImportService#ImportRow],
      programmColumnCaption: String,
      programms: Seq[ProgrammView],
      onSelected: Seq[ch.seidel.kutu.domain.WettkampfImportService#ImportRow] => Unit,
      onAll: Seq[ch.seidel.kutu.domain.WettkampfImportService#ImportRow] => Unit)(using event: ActionEvent): Unit = {
    type ImportRow = ch.seidel.kutu.domain.WettkampfImportService#ImportRow
    val sourceModel = ObservableBuffer.from(rows)
    val filteredModel = ObservableBuffer.from(sourceModel)
    val athletTable = new TableView[ImportRow](filteredModel) {
      columns ++= List(
        new TableColumn[ImportRow, String] {
          text = "Athlet (Name, Vorname, JG)"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "athlet", s"${x.value.athlet.shortPrint}")
          }
          minWidth = 250
        },
        new TableColumn[ImportRow, String] {
          text = programmColumnCaption
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "programm", {
              programms.find(p => p.id == x.value.progId || p.aggregatorHead.id == x.value.progId) match {
                case Some(programm) => if x.value.oldProg > 0 then "Umteilen auf " + programm.name else programm.name
                case _ => "unbekannt"
              }
            })
          }
        },
        new TableColumn[ImportRow, String] {
          text = "Team"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "team", if x.value.team > 0 then x.value.team.toString else "")
          }
        },
        new TableColumn[ImportRow, String] {
          text = "Import-Vorschlag"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "dbmatch", {
              if x.value.progId == 0L then "wird entfernt"
              else if x.value.athletView.id > 0 then "als " + x.value.athletView.easyprint
              else "wird neu importiert"
            })
          }
        }
      )
    }
    athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)

    val filter = buildSearchFilter(sourceModel, filteredModel, athletTable, (row: ImportRow, search) => {
      row.athlet.name.toUpperCase.contains(search) ||
      row.athlet.vorname.toUpperCase.contains(search) ||
      row.athletView.verein.exists(_.name.toUpperCase.contains(search))
    })

    PageDisplayer.showInDialog("Aus CSV laden ...", new DisplayablePage() {
      override def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          minWidth = 600
          center = new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            top = filter
            center = athletTable
            minWidth = 550
          }
        }
      }
    }, new Button("OK") {
      onAction = _ =>
        if !athletTable.selectionModel().isEmpty then onSelected(selectionFromTable(athletTable))
    }, new Button("OK Alle") {
      onAction = _ => onAll(filteredModel.toList)
    })
  }

  def showCsvImportPreviewWithVerein(
      title: String,
      rows: Seq[ch.seidel.kutu.domain.WettkampfImportService#ImportRow],
      programmColumnCaption: String,
      programms: Seq[ProgrammView],
      vereine: Seq[Verein],
      initialVerein: Option[Verein],
      onSelected: (Seq[ch.seidel.kutu.domain.WettkampfImportService#ImportRow], Verein) => Unit,
      onAll: (Seq[ch.seidel.kutu.domain.WettkampfImportService#ImportRow], Verein) => Unit)(using event: ActionEvent): Unit = {
    type ImportRow = ch.seidel.kutu.domain.WettkampfImportService#ImportRow
    val sourceModel = ObservableBuffer.from(rows)
    val filteredModel = ObservableBuffer.from(sourceModel)
    val athletTable = new TableView[ImportRow](filteredModel) {
      columns ++= List(
        new TableColumn[ImportRow, String] {
          text = "Athlet (Name, Vorname, JG)"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "athlet", s"${x.value.athlet.shortPrint}")
          }
          minWidth = 250
        },
        new TableColumn[ImportRow, String] {
          text = programmColumnCaption
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "programm", {
              programms.find(p => p.id == x.value.progId || p.aggregatorHead.id == x.value.progId) match {
                case Some(programm) => if x.value.oldProg > 0 then "Umteilen auf " + programm.name else programm.name
                case _ => "unbekannt"
              }
            })
          }
        },
        new TableColumn[ImportRow, String] {
          text = "Team"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "team", if x.value.team > 0 then x.value.team.toString else "")
          }
        },
        new TableColumn[ImportRow, String] {
          text = "Import-Vorschlag"
          cellValueFactory = { x =>
            new ReadOnlyStringWrapper(x.value, "dbmatch", {
              if x.value.progId == 0L then "wird entfernt"
              else if x.value.athletView.id > 0 then "als " + x.value.athletView.easyprint
              else "wird neu importiert"
            })
          }
        }
      )
    }
    athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)

    val filter = buildSearchFilter(sourceModel, filteredModel, athletTable, (row: ImportRow, search) => {
      row.athlet.name.toUpperCase.contains(search) ||
      row.athlet.vorname.toUpperCase.contains(search) ||
      row.athletView.verein.exists(_.name.toUpperCase.contains(search))
    })

    val cbVereine = new ComboBox[Verein] {
      items = ObservableBuffer.from(vereine)
    }
    initialVerein.foreach(v => cbVereine.selectionModel.value.select(v))
    if cbVereine.selectionModel.value.getSelectedItem == null && vereine.nonEmpty then cbVereine.selectionModel.value.selectFirst()

    PageDisplayer.showInDialog(title, new DisplayablePage() {
      override def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          minWidth = 600
          top = new HBox {
            prefHeight = 50
            alignment = Pos.BottomRight
            hgrow = Priority.Always
            children = Seq(new Label("Turner/-Innen aus dem Verein  "), cbVereine)
          }
          center = new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            top = filter
            center = athletTable
            minWidth = 550
          }
        }
      }
    }, new Button("OK") {
      disable <== when(cbVereine.selectionModel.value.selectedItemProperty.isNull) choose true otherwise false
      onAction = _ =>
        if !athletTable.selectionModel().isEmpty then onSelected(selectionFromTable(athletTable), cbVereine.selectionModel.value.selectedItem.value)
    }, new Button("OK Alle") {
      disable <== when(cbVereine.selectionModel.value.selectedItemProperty.isNull) choose true otherwise false
      onAction = _ => onAll(filteredModel.toList, cbVereine.selectionModel.value.selectedItem.value)
    })
  }
}


