package ch.seidel.kutu.view

import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry._
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.Tab
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableView
import scalafx.scene.control.TextField
import scalafx.scene.control.ToolBar
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.Region
import scalafx.scene.layout.VBox
import scalafx.util.converter.DefaultStringConverter
import scalafx.scene.control.ComboBox
import scalafx.scene.input.Clipboard
import scala.io.Source
import java.text.SimpleDateFormat
import scalafx.scene.control.SelectionMode
import scalafx.application.Platform
import java.io.File
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.awt.Desktop
import javafx.scene.{control => jfxsc}
import scala.IndexedSeq
import scalafx.beans.property._
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableView.sfxTableView2jfx
import scalafx.scene.control.cell.CheckBoxTableCell
import scalafx.scene.layout.StackPane
import scalafx.scene.web.WebView
import ch.seidel.commons._
import ch.seidel.kutu.renderer.NotenblattToHtmlRenderer
import ch.seidel.kutu.domain._
import scalafx.scene.control.CheckBox
import scalafx.scene.control.SplitPane
import ch.seidel.kutu.renderer.KategorieTeilnehmerToHtmlRenderer
import scalafx.scene.control.cell.ComboBoxTableCell
import scalafx.util.StringConverter
import ch.seidel.kutu.KuTuApp
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import ch.seidel.kutu.data.ResourceExchanger



class RiegenFilterView(wettkampf: WettkampfView, service: KutuService, refreshPaneData: ()=>Unit, riegenFilterModel: ObservableBuffer[RiegeEditor]) extends TableView[RiegeEditor] {
  val disziplinlist = service.listDisziplinesZuWettkampf(wettkampf.id)
  items = riegenFilterModel
  id = "riege-table"
      editable = true
      columns ++= List(
          new TableColumn[RiegeEditor, Boolean] {
            text = "Filter"
            cellValueFactory = { x => x.value.selected.asInstanceOf[ObservableValue[Boolean,Boolean]] }
            cellFactory = { x => new CheckBoxTableCell[RiegeEditor, Boolean]() }
            editable = true
          }
         ,new TableColumn[RiegeEditor, String] {
            text = "Riege"
            cellValueFactory = { x => x.value.name }
            cellFactory = { _ => new AutoCommitTextFieldTableCell[RiegeEditor, String](new DefaultStringConverter()) }
            editable = true
            onEditCommit = (evt: CellEditEvent[RiegeEditor, String]) => {
              val editor = evt.rowValue
              editor.name.value = evt.newValue
              val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
              service.renameRiege(wettkampf.id, evt.rowValue.initname, evt.newValue)
              refreshPaneData()
              //refreshLazyPane()
              //reloadData()
              evt.tableView.selectionModel.value.select(rowIndex, this)
              evt.tableView.requestFocus()
            }
          }
         ,new TableColumn[RiegeEditor, String] {
            text = "Anz"
            cellValueFactory = { x => x.value.anzkat.asInstanceOf[ObservableValue[String,String]] }
          }
         ,new TableColumn[RiegeEditor, String] {
            text = "Durchgang"
            prefWidth = 150
            cellValueFactory = { x => x.value.durchgang }
            cellFactory = { _ => new AutoCommitTextFieldTableCell[RiegeEditor, String](new DefaultStringConverter()) }
            editable = true
            onEditCommit = (evt: CellEditEvent[RiegeEditor, String]) => {
              val editor = evt.rowValue
              editor.durchgang.value = evt.newValue
              val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
              riegenFilterModel.update(rowIndex, RiegeEditor(
                  evt.rowValue.wettkampfid,
                  evt.rowValue.initanz,
                  evt.rowValue.initviewanz,
                  evt.rowValue.enabled,
                  service.updateOrinsertRiege(evt.rowValue.commit),
                  evt.rowValue.onNameChange,
                  evt.rowValue.onSelectedChange))
              evt.tableView.selectionModel.value.select(rowIndex, this)
              evt.tableView.requestFocus()
            }
          }
         ,new TableColumn[RiegeEditor, Disziplin] {
            text = "Start"
            prefWidth = 150
            val converter = new StringConverter[Disziplin] {
              override def toString(d: Disziplin) = if(d != null) d.easyprint else ""
              override def fromString(s: String) = if(s != null) disziplinlist.find { d => d.name.equals(s) }.getOrElse(null) else null
            }
            val list = ObservableBuffer[Disziplin](disziplinlist)
            cellValueFactory = { x => x.value.start.asInstanceOf[ObservableValue[Disziplin,Disziplin]] }
            cellFactory = { _ => new ComboBoxTableCell[RiegeEditor, Disziplin](converter, list) }
            editable = true
            onEditCommit = (evt: CellEditEvent[RiegeEditor, Disziplin]) => {
              val editor = evt.rowValue
              editor.start.value = evt.newValue
              val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
              riegenFilterModel.update(rowIndex, RiegeEditor(
                  evt.rowValue.wettkampfid,
                  evt.rowValue.initanz,
                  evt.rowValue.initviewanz,
                  evt.rowValue.enabled,
                  service.updateOrinsertRiege(evt.rowValue.commit),
                  evt.rowValue.onNameChange,
                  evt.rowValue.onSelectedChange))
              evt.tableView.selectionModel.value.select(rowIndex, this)
              evt.tableView.requestFocus()
            }
          })
    }

class RiegenTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService {
  val programmText = wettkampf.programm.id match {case 20 => "Kategorie" case _ => "Programm"}

  override def isPopulated = {

    true
  }

}
