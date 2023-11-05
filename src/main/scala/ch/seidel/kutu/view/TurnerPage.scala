package ch.seidel.kutu.view

import ch.seidel.commons._
import ch.seidel.kutu.domain._
import javafx.scene.{control => jfxsc}
import scalafx.Includes._
import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.Tab.sfxTab2jfx
import scalafx.scene.control.TableColumn._
import scalafx.scene.control._
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout._
import scalafx.util.converter.DefaultStringConverter

object TurnerPage {
  var turnerAnalyzers: Map[Long, TurnerAnalyzer] = Map[Long, TurnerAnalyzer]()

  def drillDownInAthlet(vverein: Option[Verein], a: Athlet, service: KutuService, tabpane: LazyTabPane): Unit = {
    val newtab = new TurnerAnalyzer(vverein, Some(a), None, service) {
      text = a.easyprint + "-Analyse"
      closable = true
      override def onDrillDown(w: WettkampfdisziplinView): Unit = {
        drillDownInDisziplin(vverein, w, this.service, tabpane)
      }

      onClosed = _ => {
        turnerAnalyzers = turnerAnalyzers.filter(x => !x._1.equals(a.id))
        tabpane.selectionModel.value.select(0)
      }
    }
    turnerAnalyzers = turnerAnalyzers.updated(a.id, newtab)
    tabpane.refreshTabs()
    tabpane.selectionModel.value.select(newtab)
  }

  def drillDownInDisziplin(vverein: Option[Verein], w: WettkampfdisziplinView, service: KutuService, tabpane: LazyTabPane): Unit = {
    val newtab = new TurnerAnalyzer(vverein, None, Some(w), service) {
      text = w.easyprint + "-Analyse"
      closable = true
      override def onDrillDown(a: Athlet): Unit = {
        drillDownInAthlet(vverein, a, this.service, tabpane)
      }
      onClosed = _ => {
        turnerAnalyzers = turnerAnalyzers.filter(x => !x._1.equals(w.id * -1))
        tabpane.selectionModel.value.select(0)
      }
    }
    turnerAnalyzers = turnerAnalyzers.updated(w.id * -1, newtab)
    tabpane.refreshTabs()
    tabpane.selectionModel.value.select(newtab)
  }

  class VereinTab(val verein: Verein, override val service: KutuService, val tabpane: LazyTabPane) extends Tab with TabWithService {

    override def isPopulated: Boolean = {
      val athleten = service.selectAthletesOfVerein(verein.id)
        .sortBy { a => (if (a.activ) {
          "A"
        } else {
          "X"
        }) + ":" + a.name + ":" + a.vorname }.map{a => AthletEditor(a)}

      val wkModel = ObservableBuffer.from(athleten)

      val cols: List[jfxsc.TableColumn[AthletEditor, _]] = classOf[AthletEditor].getDeclaredFields.filter{f =>
          f.getType.equals(classOf[StringProperty])
        }.map { field =>
          field.setAccessible(true)
          val tc: jfxsc.TableColumn[AthletEditor, _] = new TableColumn[AthletEditor, String] {
            text =  field.getName.take(1).toUpperCase() + field.getName.drop(1)
            cellValueFactory = { x =>
              field.get(x.value).asInstanceOf[StringProperty] }
            cellFactory.value = { _:Any => new AutoCommitTextFieldTableCell[AthletEditor, String](new DefaultStringConverter()) }
            styleClass += "table-cell-with-value"
            prefWidth = AthletEditor.coldef(field.getName)
            editable = true
            onEditCommit = (evt: CellEditEvent[AthletEditor, String]) => {
              field.get(evt.rowValue).asInstanceOf[StringProperty].value = evt.newValue
              val rowIndex = wkModel.indexOf(evt.rowValue)
              wkModel.update(rowIndex, AthletEditor(service.insertAthlete(evt.rowValue.commit)))
              evt.tableView.selectionModel.value.select(rowIndex, this)
              evt.tableView.requestFocus()
            }
          }
          tc
        }.toList

    val athletenview = new TableView[AthletEditor](wkModel) {
      columns ++= cols
      id = "athlet-table"
      editable = true
    }

    var lastFilter: String = ""

    def updateFilteredList(newVal: String): Unit = {
      //if(!newVal.equalsIgnoreCase(lastFilter)) {
        lastFilter = newVal
        val sortOrder = athletenview.sortOrder.toList
        wkModel.clear()
        val searchQuery = newVal.toUpperCase().split(" ")
        for{athlet <- athleten} {
          val matches =
            searchQuery.forall{search =>
            if(search.isEmpty || athlet.name.value.toUpperCase().contains(search)) {
              true
            }
            else if(athlet.vorname.value.toUpperCase().contains(search)) {
              true
            }
            else {
              false
            }
          }

          if(matches) {
            wkModel += athlet
          }
        }
        athletenview.sortOrder.clear()
        val restored = athletenview.sortOrder ++= sortOrder
      //}
  	}
    val txtUserFilter = new TextField() {
      promptText = "Athlet-Filter"
      text.addListener{ (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
        if(!lastFilter.equalsIgnoreCase(newVal)) {
          updateFilteredList(newVal)
        }
      }
    }
    athletenview.selectionModel.value.setCellSelectionEnabled(true)
    athletenview.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(athletenview, double = false, txtUserFilter)(ke)
    }

    val addButton = new Button {
      text = "Athlet hinzufügen"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        val ae = new AthletEditor(Athlet(verein))
        wkModel.insert(0, ae)
        athletenview.requestFocus()
        athletenview.selectionModel.value.select(0, athletenview.columns.head)
        athletenview.scrollToColumn(athletenview.columns.head)
//        athletenview.selectionModel().select(ae)
      }
    }

    val removeButton = new Button {
      text = "Athlet entfernen"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        if (!athletenview.selectionModel().isEmpty) {
          val athlet = athletenview.selectionModel().getSelectedItem
          implicit val impevent = event
          PageDisplayer.showInDialog(text.value, new DisplayablePage() {
            def getPage: Node = {
              new HBox {
                prefHeight = 50
                alignment = Pos.BottomRight
                hgrow = Priority.Always
                children = Seq(
                    new Label(
                        s"Soll '${athlet.commit.easyprint}' wirklich mitsamt seinen Wettkampfresultaten gelöscht werden?"))
              }
            }
          }, new Button("OK") {
            onAction = (event: ActionEvent) => {
              service.deleteAthlet(athlet.commit.id)
              wkModel.remove(athletenview.selectionModel().getSelectedIndex)
            }
          })
        }
      }
    }

    val analyzeVereinButton = new Button {
      text = "Athlet im Verein Analysieren"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        if (!athletenview.selectionModel().isEmpty) {
          val a = athletenview.selectionModel().getSelectedItem.commit
          drillDownInAthlet(Some(verein), a, service, tabpane)
        }
      }
    }
//    val analyzeButton = new Button {
//      text = "Athlet Analysieren"
//      minWidth = 75
//      onAction = (event: ActionEvent) => {
//        if (!athletenview.selectionModel().isEmpty) {
//          val a = athletenview.selectionModel().getSelectedItem.commit
//          drillDownInAthlet(None, a, service, tabpane)
//        }
//      }
//    }
    analyzeVereinButton.disable <== when(athletenview.selectionModel.value.selectedItemProperty.isNull) choose true otherwise false
//    analyzeButton.disable <== when(athletenview.selectionModel.value.selectedItemProperty.isNull choose true otherwise false
    removeButton.disable <== when(athletenview.selectionModel.value.selectedItemProperty.isNull) choose true otherwise false

    val cont = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      top = new ToolBar {
        content = List(
          new Label {
            text = s"Verein ${verein.name} "
            maxWidth = Double.MaxValue
            minHeight = Region.USE_PREF_SIZE
            styleClass += "toolbar-header"
          },
          addButton, analyzeVereinButton, /*analyzeButton,*/ removeButton, txtUserFilter
        )
      }
      center = athletenview
      //bottom = pagination
    }

      content = cont
      true
    }
  }

  def buildTab(wettkampfmode: BooleanProperty, club: Verein, service: KutuService) = {
    def refresher(pane: LazyTabPane) = {
      val retUnsorted = Seq(new VereinTab(club, service, pane) {
        text = verein.name
        closable = false
      }) ++ turnerAnalyzers.values.toSeq ++ Seq(
      new TurnerAnalyzer(Some(club), None, None, service) {
        text = club.easyprint + " Turner-Analyse"
        closable = false
        override def onDrillDown(a: Athlet): Unit = {
          drillDownInAthlet(Some(club), a, this.service, pane)
        }
        override def onDrillDown(w: WettkampfdisziplinView): Unit = {
          drillDownInDisziplin(Some(club), w, this.service, pane)
        }
      },
      new TurnerAnalyzer(None, None, None, service) {
        text = "Turner-Analyse"
        closable = false
        override def onDrillDown(a: Athlet): Unit = {
          drillDownInAthlet(None, a, this.service, pane)
        }
        override def onDrillDown(w: WettkampfdisziplinView): Unit = {
          drillDownInDisziplin(None, w, this.service, pane)
        }
      },
      new TurnerScoreTab(wettkampfmode, Some(club), service){
        text = club.easyprint + "-übergreifende Turner-Auswertung"
        closable = false
      },
      new TurnerScoreTab(wettkampfmode, None, service){
        text = "Übergreifende Turner-Auswertung"
        closable = false
      })

      val retSorted = retUnsorted.sortBy {
        case _: VereinTab =>
          0
        case ta: TurnerAnalyzer =>
          ta.athlet match {
            case Some(a) => a.id
            case None => 1000
          }
        case ts: TurnerScoreTab =>
          ts.verein match {
            case Some(v) => 1001
            case None => 1002
          }
        case _ =>
          5000
      }
      retSorted
    }
    new TurnerPage( new LazyTabPane(refresher, () => {}))
  }
}

class TurnerPage(tabPane: LazyTabPane) extends DisplayablePage {

  def getPage = {
    TurnerPage.turnerAnalyzers = Map[Long, TurnerAnalyzer]()
    tabPane.init()
    tabPane
  }
}
