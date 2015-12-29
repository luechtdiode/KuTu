package ch.seidel.kutu.view

import java.text.SimpleDateFormat
import scala.collection.mutable.StringBuilder
import javafx.scene.{ control => jfxsc }
import javafx.collections.{ ObservableList, ListChangeListener }
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scalafx.util.converter.DefaultStringConverter
import scalafx.util.converter.DoubleStringConverter
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.beans.value.ObservableValue
import scalafx.beans.property.ReadOnlyDoubleWrapper
import scalafx.event.ActionEvent
import scalafx.scene.layout.Region
import scalafx.scene.control.Label
import scalafx.scene.layout.VBox
import scalafx.scene.layout.BorderPane
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.StringProperty
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.{ Tab, TabPane }
import scalafx.scene.layout.{ Priority, StackPane }
import scalafx.scene.control.{ TableView, TableColumn }
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.ToolBar
import scalafx.scene.control.Button
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ComboBox
import scalafx.scene.layout.HBox
import scalafx.scene.Group
import scalafx.scene.web.WebView
import java.io.FileOutputStream
import java.awt.Desktop
import java.io.BufferedOutputStream
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import ch.seidel.commons._
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.domain._
import ch.seidel.kutu.data._
import ch.seidel.kutu.renderer.ScoreToHtmlRenderer

class RanglisteTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService with ScoreToHtmlRenderer {
  override val title = wettkampf.easyprint

  /*
         * combo 1. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         * combo 2. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         * combo 3. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         * combo 4. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         */

  //    val daten = service.selectWertungen().groupBy { x =>
  //      x.wettkampf }.map(x => (x._1, x._2.groupBy { x =>
  //        x.wettkampfdisziplin.programm }.map(x => (x._1, x._2.groupBy { x =>
  //          x.athlet }))))

  override def isPopulated = {
    val text = wettkampf.programm.id match {case 20 => "Kategorie" case _ => "Programm"}
    val groupers = List(ByNothing, ByWettkampfArt, ByWettkampfProgramm(text), ByProgramm(text), ByJahrgang, ByGeschlecht, ByVerein, ByRiege, ByDisziplin)
    val gr1Model = ObservableBuffer[GroupBy](groupers)
    val cb1 = new ComboBox[GroupBy] {
      maxWidth = 250
      promptText = "erste Gruppierung..."
      items = gr1Model
    }
    val cb2 =
      new ComboBox[GroupBy] {
        maxWidth = 250
        promptText = "zweite Gruppierung..."
        items = gr1Model
      }
    val cb3 =
      new ComboBox[GroupBy] {
        maxWidth = 250
        promptText = "dritte Gruppierung..."
        items = gr1Model
      }
    val cb4 =
      new ComboBox[GroupBy] {
        maxWidth = 250
        promptText = "vierte Gruppierung..."
        items = gr1Model
      }
    val combs = List(cb1, cb2, cb3, cb4)
    val webView = new WebView

    def buildQuery = {
      groupers.foreach { gr => gr.reset }
      val cblist = combs.filter(cb => !cb.selectionModel.value.isEmpty).map(cb => cb.selectionModel.value.getSelectedItem).filter(x => x != ByNothing)
      if (cblist.isEmpty) {
        ByWettkampfProgramm().groupBy(ByGeschlecht)
      }
      else {
        cblist.foldLeft(cblist.head)((acc, cb) => if (acc != cb) acc.groupBy(cb) else acc)
      }
    }

    def refreshRangliste(query: GroupBy) = {
      val combination = query.select(service.selectWertungen(wettkampfId = Some(wettkampf.id))).toList
      val ret = toHTML(combination)
      webView.engine.loadContent(ret)
      ret
    }

    cb1.selectionModel.value.select(ByWettkampfProgramm(text))
    cb2.selectionModel.value.select(ByGeschlecht)

    combs.foreach{c =>
      c.onAction = handle {
        refreshRangliste(buildQuery)
      }
    }

    val btnSave = new Button {
      text = "Speichern als ..."
      onAction = handle {
        val fileChooser = new FileChooser()
        fileChooser.initialDirectory = new java.io.File(System.getProperty("user.home") + "/documents")
        fileChooser.setTitle("Open Resource File")
        fileChooser.getExtensionFilters().addAll(
                 new ExtensionFilter("Web-Datei", "*.html"),
                 new ExtensionFilter("All Files", "*.*"))
        val selectedFile = fileChooser.showSaveDialog(KuTuApp.getStage())
        if (selectedFile != null) {
          val file = if(!selectedFile.getName.endsWith(".html") && !selectedFile.getName.endsWith(".htm")) {
            new java.io.File(selectedFile.getAbsolutePath + ".html")
          }
          else {
            selectedFile
          }
          val toSave = refreshRangliste(buildQuery).getBytes("UTF-8")
          val os = new BufferedOutputStream(new FileOutputStream(selectedFile))
          os.write(toSave)
          os.flush()
          os.close()
          Desktop.getDesktop().open(selectedFile);
//          new ProcessBuilder().command("explorer.exe", selectedFile.getAbsolutePath).start()
        }

      }
    }
    onSelectionChanged = handle {
      if(selected.value) {
        refreshRangliste(buildQuery)
      }
    }
    content = new BorderPane {
      vgrow = Priority.Always
      hgrow = Priority.Always
      top = new HBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        spacing = 15
        padding = Insets(15)
        children = new Label("Gruppierungen:") +: combs :+ btnSave
      }
      center = webView
    }

    true
  }
}
