package ch.seidel

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
import scalafx.stage.FileChooser
import ch.seidel.domain._
import ch.seidel.commons._
import scalafx.stage.FileChooser.ExtensionFilter
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.awt.Desktop

class TurnerScoreTab(val verein: Option[Verein], override val service: KutuService) extends Tab with TabWithService  with ScoreToHtmlRenderer {
  override val title = verein match {case Some(v) => v.easyprint case None => "Vereinsübergreifend"}

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
    val groupers = List(ByNothing, ByJahr, ByWettkampfArt, ByWettkampfProgramm(), ByProgramm(), ByJahrgang, ByGeschlecht, ByVerein, ByDisziplin)
    val gr1Model = ObservableBuffer[GroupBy](groupers)
    val f1Model = ObservableBuffer[DataObject]()
    val f2Model = ObservableBuffer[DataObject]()
    val f3Model = ObservableBuffer[DataObject]()
    val f4Model = ObservableBuffer[DataObject]()
    val cb1 = new ComboBox[GroupBy] {
      maxWidth = 250
      promptText = "erste Gruppierung..."
      items = gr1Model
    }
    val fcb1 = new ComboBox[DataObject] {
      maxWidth = 250
      promptText = "Filter..."
      items = f1Model
    }
    val cb2 =
      new ComboBox[GroupBy] {
        maxWidth = 250
        promptText = "zweite Gruppierung..."
        items = gr1Model
      }
    val fcb2 = new ComboBox[DataObject] {
      maxWidth = 250
      promptText = "Filter..."
      items = f2Model
    }
    val cb3 =
      new ComboBox[GroupBy] {
        maxWidth = 250
        promptText = "dritte Gruppierung..."
        items = gr1Model
      }
    val fcb3 = new ComboBox[DataObject] {
      maxWidth = 250
      promptText = "Filter..."
      items = f3Model
    }
    val cb4 =
      new ComboBox[GroupBy] {
        maxWidth = 250
        promptText = "vierte Gruppierung..."
        items = gr1Model
      }
    val fcb4 = new ComboBox[DataObject] {
      maxWidth = 250
      promptText = "Filter..."
      items = f4Model
    }
    val combs = List(cb1, cb2, cb3, cb4)
    //val filters = List(fcb1, fcb2, fcb3, fcb4)
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
      val qry = verein match {
        case Some(v) => service.selectWertungen(vereinId = Some(v.id))
        case None    => service.selectWertungen()
      }
      val combination = query.select(qry).toList
      //query.asInstanceOf[FilterBy].adjustFilter(filters)
      val ret = toHTML(combination)
      webView.engine.loadContent(ret)
      ret
    }

    cb1.selectionModel.value.select(ByWettkampfProgramm())
    cb2.selectionModel.value.select(ByGeschlecht)

    combs.foreach{c =>
      c.onAction = handle {
        refreshRangliste(buildQuery)
      }
    }
//    filters.foreach{c =>
//      c.onAction = handle {
//        refreshRangliste(buildQuery)
//      }
//    }

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
//        children = new Label("Gruppierungen:") +: combs.zip(filters).map(x => new VBox {
//          vgrow = Priority.Always
//          hgrow = Priority.Always
//          spacing = 5
//          padding = Insets(5)
//          children = List(x._1, x._2)
//        }) :+ btnSave
        children = combs :+ btnSave
      }
      center = webView
    }

    true
  }

}
