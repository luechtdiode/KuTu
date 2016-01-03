package ch.seidel.kutu.view

import java.text.SimpleDateFormat
import java.sql.Date
import javafx.scene.{ control => jfxsc }
import javafx.scene.{control => jfxsc}
import scala.collection.JavaConversions
import scalafx.Includes._
import scalafx.scene.control.Tab
import scalafx.scene.layout._
import scalafx.scene.control.TableColumn._
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control._
import scalafx.util.converter.DefaultStringConverter
import scalafx.event.ActionEvent
import scalafx.application.Platform
import scalafx.scene.input.KeyEvent
import scalafx.scene.input.KeyCode
import scalafx.scene.control.Tab.sfxTab2jfx
import slick.jdbc.StaticQuery.staticQueryToInvoker
import ch.seidel.commons.AutoCommitTextFieldTableCell
import ch.seidel.commons.DisplayablePage
import ch.seidel.commons.TabWithService
import ch.seidel.commons.LazyTabPane
import ch.seidel.kutu.domain._

object TurnerPage {
  var turnerAnalyzers = Map[Long, TurnerAnalyzer]()

  object AthletEditor {
    def apply(init: Athlet) = new AthletEditor(init)
    val coldef = Map(
      "jsid" -> 80,
      "geschlecht" -> 80,
      "name" -> 160,
      "vorname" -> 160,
      "gebdat" -> 80,
      "strasse" -> 180,
      "plz" -> 100,
      "ort" -> 180,
      "activ" -> 100
    )
  }

  class AthletEditor(init: Athlet) {
    val sdf = new SimpleDateFormat("dd.MM.yyyy")
    val jsid = new StringProperty(init.js_id + "")
    val geschlecht = new StringProperty(if(init.geschlecht.toUpperCase.startsWith("M")) "M" else "W")
    val name = new StringProperty(init.name)
    val vorname = new StringProperty(init.vorname)
    val gebdat = new StringProperty(init.gebdat match {case Some(d) => sdf.format(d); case _ => ""})
    val strasse = new StringProperty(init.strasse)
    val plz = new StringProperty(init.plz)
    val ort = new StringProperty(init.ort)
    val activ = new StringProperty(init.activ match {case true => "Aktiv" case _ => "Inaktiv"})

    def reset {
      jsid.value_=(init.js_id + "")
    }

    private def optionOfGebDat: Option[Date] = {
      gebdat.value match {
        case "" => None
        case s: String => Some(new java.sql.Date(sdf.parse(s).getTime()))
      }
    }

    def commit = Athlet(init.id, Integer.valueOf(jsid.value), if(geschlecht.value.toUpperCase.startsWith("M")) "M" else "W", name.value, vorname.value, optionOfGebDat, strasse.value, plz.value, ort.value, init.verein, activ.value.toUpperCase().startsWith("A"))
  }

  def drillDownInAthlet(vverein: Option[Verein], a: Athlet, service: KutuService, tabpane: LazyTabPane) {
    val newtab = new TurnerAnalyzer(vverein, Some(a), None, service) {
      text = a.easyprint + "-Analyse"
      closable = true
      override def onDrillDown(w: WettkampfdisziplinView) {
        drillDownInDisziplin(vverein, w, service, tabpane)
      }

      onClosed = handle {
        turnerAnalyzers = turnerAnalyzers.filter(x => !x._1.equals(a.id))
        tabpane.selectionModel.value.select(0)
      }
    }
    turnerAnalyzers = turnerAnalyzers.updated(a.id, newtab)
    tabpane.refreshTabs()
    tabpane.selectionModel.value.select(newtab)
  }

  def drillDownInDisziplin(vverein: Option[Verein], w: WettkampfdisziplinView, service: KutuService, tabpane: LazyTabPane) {
    val newtab = new TurnerAnalyzer(vverein, None, Some(w), service) {
      text = w.easyprint + "-Analyse"
      closable = true
      override def onDrillDown(a: Athlet) {
        drillDownInAthlet(vverein, a, service, tabpane)
      }
      onClosed = handle {
        turnerAnalyzers = turnerAnalyzers.filter(x => !x._1.equals(w.id * -1))
        tabpane.selectionModel.value.select(0)
      }
    }
    turnerAnalyzers = turnerAnalyzers.updated(w.id * -1, newtab)
    tabpane.refreshTabs()
    tabpane.selectionModel.value.select(newtab)
  }

  class VereinTab(val verein: Verein, override val service: KutuService, val tabpane: LazyTabPane) extends Tab with TabWithService {
    import scala.collection.JavaConversions._

    override def isPopulated: Boolean = {
      val athleten = service.database withSession {implicit session =>
        (service.selectAthletes + " where verein=" +? verein.id + " order by activ desc, name, vorname asc").list.sortBy { a => (a.activ match {case true => "A" case _ => "X"}) + ":" + a.name + ":" + a.vorname }.map{a => AthletEditor(a)}
      }

      val wkModel = ObservableBuffer[AthletEditor](athleten)

      val cols: List[jfxsc.TableColumn[AthletEditor, _]] = classOf[AthletEditor].getDeclaredFields.filter{f =>
          f.getType.equals(classOf[StringProperty])
        }.map { field =>
          field.setAccessible(true)
          val tc: jfxsc.TableColumn[AthletEditor, _] = new TableColumn[AthletEditor, String] {
            text =  field.getName.take(1).toUpperCase() + field.getName.drop(1)
            cellValueFactory = { x =>
              field.get(x.value).asInstanceOf[StringProperty] }
            cellFactory = { _ => new AutoCommitTextFieldTableCell[AthletEditor, String](new DefaultStringConverter()) }
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
    athletenview.selectionModel.value.setCellSelectionEnabled(true)
    athletenview.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(athletenview, false)(ke)
    }

    val addButton = new Button {
      text = "Athlet hinzufügen"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        val ae = new AthletEditor(Athlet(verein))
        wkModel.add(ae)
        athletenview.selectionModel().select(ae)
      }
    }

    val removeButton = new Button {
      text = "Athlet entfernen"
      minWidth = 75
      onAction = (event: ActionEvent) => {
        if (!athletenview.selectionModel().isEmpty) {
          val athlet = athletenview.selectionModel().getSelectedItem
          service.deleteAthlet(athlet.commit.id)
          wkModel.remove(athletenview.selectionModel().getSelectedIndex)
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
    analyzeVereinButton.disable <== when(athletenview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
//    analyzeButton.disable <== when(athletenview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
    removeButton.disable <== when(athletenview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

    val cont = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      center = athletenview
      top = new ToolBar {
        content = List(
          new Label {
            text = s"Verein ${verein.name}"
            maxWidth = Double.MaxValue
            minHeight = Region.USE_PREF_SIZE
            styleClass += "toolbar-header"
          },
          addButton, analyzeVereinButton, /*analyzeButton,*/ removeButton
        )
      }
      //bottom = pagination
    }

      content = cont
      true
    }
  }

  def buildTab(club: Verein, service: KutuService) = {
    def refresher(pane: LazyTabPane) = {
      val retUnsorted = Seq(new VereinTab(club, service, pane) {
        text = verein.name
        closable = false
      }) ++ turnerAnalyzers.values.toSeq ++ Seq(
      new TurnerAnalyzer(Some(club), None, None, service) {
        text = club.easyprint + " Turner-Analyse"
        closable = false
        override def onDrillDown(a: Athlet) {
          drillDownInAthlet(Some(club), a, service, pane)
        }
        override def onDrillDown(w: WettkampfdisziplinView) {
          drillDownInDisziplin(Some(club), w, service, pane)
        }
      },
      new TurnerAnalyzer(None, None, None, service) {
        text = "Turner-Analyse"
        closable = false
        override def onDrillDown(a: Athlet) {
          drillDownInAthlet(None, a, service, pane)
        }
        override def onDrillDown(w: WettkampfdisziplinView) {
          drillDownInDisziplin(None, w, service, pane)
        }
      },
      new TurnerScoreTab(Some(club), service){
        text = club.easyprint + "-übergreifende Turner-Auswertung"
        closable = false
      },
      new TurnerScoreTab(None, service){
        text = "Übergreifende Turner-Auswertung"
        closable = false
      })

      val retSorted = retUnsorted.sortBy(tab =>
        if(tab.isInstanceOf[VereinTab]) {
          0
        }
        else if(tab.isInstanceOf[TurnerAnalyzer]) {
          val ta = tab.asInstanceOf[TurnerAnalyzer]
          ta.athlet match {
            case Some(a) => a.id
            case None => 1000
          }
        }
        else if(tab.isInstanceOf[TurnerScoreTab]) {
          val ts = tab.asInstanceOf[TurnerScoreTab]
          ts.verein match {
            case Some(v) => 1001
            case None => 1002
          }
        }
        else {
          5000
        }
      )
      retSorted
    }
    new TurnerPage( new LazyTabPane(refresher))
  }
}

class TurnerPage(tabPane: LazyTabPane) extends DisplayablePage {

  def getPage = {
    import WettkampfPage._
    TurnerPage.turnerAnalyzers = Map[Long, TurnerAnalyzer]()
    tabPane.init()
    tabPane
  }
}
