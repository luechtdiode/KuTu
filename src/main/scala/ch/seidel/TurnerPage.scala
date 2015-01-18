package ch.seidel

import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.scene.control.{Tab, TabPane}
import scalafx.scene.layout.Priority
import scalafx.scene.control.TableColumn._
import scalafx.beans.property.StringProperty
import ch.seidel.domain._
import ch.seidel.commons.DisplayablePage
import ch.seidel.commons.TabWithService
import ch.seidel.commons.LazyTabPane
import java.text.SimpleDateFormat
import java.sql.Date
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.TableColumn
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.util.converter.DefaultStringConverter
import scala.collection.JavaConversions
import scalafx.scene.control.TableView

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
      "ort" -> 180
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

    def reset {
      jsid.value_=(init.js_id + "")
    }

    private def optionOfGebDat: Option[Date] = {
      gebdat.value match {
        case "" => None
        case s: String => Some(new java.sql.Date(sdf.parse(s).getTime()))
      }
    }

    def commit = Athlet(init.id, Integer.valueOf(jsid.value), geschlecht.value, name.value, vorname.value, optionOfGebDat, strasse.value, plz.value, ort.value, init.verein)
  }

  class VereinTab(val verein: Verein, override val service: KutuService) extends Tab with TabWithService {
    import scala.collection.JavaConversions._

    override def isPopulated: Boolean = {
      val athleten = service.database withSession {implicit session =>
        (service.selectAthletes + " where verein=" +? verein.id).list.map{a => AthletEditor(a)}
      }

      val wkModel = ObservableBuffer[AthletEditor](athleten)

      val cols: List[jfxsc.TableColumn[AthletEditor, _]] = classOf[AthletEditor].getDeclaredFields.filter{f =>
          f.getType.equals(classOf[StringProperty])
        }.map { field =>
          field.setAccessible(true)
          val tc: jfxsc.TableColumn[AthletEditor, _] = new TableColumn[AthletEditor, String] {
            text = field.getName
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

      content = new TableView[AthletEditor](wkModel) {
        columns ++= cols
        id = "athlet-table"
        editable = true
      }
      true
    }
  }

  def buildTab(verein: Verein, service: KutuService) = {
    new TurnerPage( new LazyTabPane(Seq(new VereinTab(verein, service) {
        text = verein.name
        closable = false
      })))
  }
}

class TurnerPage(tabPane: LazyTabPane) extends DisplayablePage {

  def getPage = {
    import WettkampfPage._

    tabPane.init
    tabPane
  }
}
