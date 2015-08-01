package ch.seidel

import java.text.SimpleDateFormat
import java.sql.Date
import scala.collection.JavaConversions
import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.scene.control.{Tab, TabPane}
import scalafx.scene.layout._
import scalafx.scene.control.TableColumn._
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control._
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.util.converter.DefaultStringConverter
import ch.seidel.domain._
import ch.seidel.commons.DisplayablePage
import ch.seidel.commons.TabWithService
import ch.seidel.commons.LazyTabPane
import scalafx.event.ActionEvent

object TurnerPage {

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
    val geschlecht = new StringProperty(init.geschlecht)
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

    def commit = Athlet(init.id, Integer.valueOf(jsid.value), geschlecht.value, name.value, vorname.value, optionOfGebDat, strasse.value, plz.value, ort.value, init.verein, activ.value.toUpperCase().startsWith("A"))
  }

  class VereinTab(val verein: Verein, override val service: KutuService) extends Tab with TabWithService {
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
            cellFactory = { _ => new TextFieldTableCell[AthletEditor, String](new DefaultStringConverter()) }
            styleClass += "table-cell-with-value"
            prefWidth = AthletEditor.coldef(field.getName)
            editable = true
            onEditCommit = (evt: CellEditEvent[AthletEditor, String]) => {
              field.get(evt.rowValue).asInstanceOf[StringProperty].value = evt.newValue
              val rowIndex = wkModel.indexOf(evt.rowValue)
              wkModel.update(rowIndex, AthletEditor(service.insertOrupdateAthlete(evt.rowValue.commit)))
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

    val cont = new BorderPane {
      hgrow = Priority.ALWAYS
      vgrow = Priority.ALWAYS
      center = athletenview
      top = new ToolBar {
        content = List(
          new Label {
            text = s"Verein ${verein.name}"
            maxWidth = Double.MaxValue
            minHeight = Region.USE_PREF_SIZE
            styleClass += "toolbar-header"
          },
          addButton, removeButton
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
      Seq(new VereinTab(club, service) {
        text = verein.name
        closable = false
      },
      new TurnerScoreTab(Some(club), service){
        text = club.easyprint + "-übergreifende Turner-Auswertung"
        closable = false
      },
      new TurnerScoreTab(None, service){
        text = "Übergreifende Turner-Auswertung"
        closable = false
      })
    }
    new TurnerPage( new LazyTabPane(refresher))
  }
}

class TurnerPage(tabPane: LazyTabPane) extends DisplayablePage {

  def getPage = {
    import WettkampfPage._

    tabPane.init
    tabPane
  }
}
