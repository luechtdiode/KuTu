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
import ch.seidel.domain._
import ch.seidel.commons._

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
    val dummyTableView = new TableView[GroupRow]()
    val groupers = List(ByNothing, ByWettkampfArt, ByWettkampfProgramm, ByProgramm, ByJahrgang, ByGeschlecht, ByVerein, ByRiege, ByDisziplin)
    val gr1Model = ObservableBuffer[GroupBy](groupers)
    val cb1 = new ComboBox[GroupBy] {
      maxWidth = 200
      promptText = "erste gruppierung..."
      items = gr1Model
    }
    val cb2 =
      new ComboBox[GroupBy] {
        maxWidth = 200
        promptText = "zweite gruppierung..."
        items = gr1Model
      }
    val cb3 =
      new ComboBox[GroupBy] {
        maxWidth = 200
        promptText = "dritte gruppierung..."
        items = gr1Model
      }
    val cb4 =
      new ComboBox[GroupBy] {
        maxWidth = 200
        promptText = "vierte gruppierung..."
        items = gr1Model
      }
    val combs = List(cb1, cb2, cb3, cb4)
    val webView = new WebView

    def buildQuery = {
      groupers.foreach { gr => gr.reset }
      val cblist = combs.filter(cb => !cb.selectionModel.value.isEmpty).map(cb => cb.selectionModel.value.getSelectedItem).filter(x => x != ByNothing)
      if (cblist.isEmpty) {
        ByWettkampfProgramm
      }
      else {
        cblist.foldLeft(cblist.head)((acc, cb) => if (acc != cb) acc.groupBy(cb) else acc)
      }
    }

    def refreshRangliste(query: GroupBy) {
      val combination = query.select(service.selectWertungen(wettkampfId = Some(wettkampf.id))).toList
      webView.engine.loadContent(toHTML(combination))
    }

    combs.foreach{c =>
      c.onAction = handle {
        refreshRangliste(buildQuery)
      }
    }

    val btnRefresh = new Button {
      text = "refresh"
      onAction = handle {
        refreshRangliste(buildQuery)
      }
    }
    onSelectionChanged = handle {
      if(selected.value) {
        refreshRangliste(buildQuery)
      }
    }
    content = new BorderPane {
      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS
      top = new HBox {
        vgrow = Priority.ALWAYS
        hgrow = Priority.ALWAYS
        spacing = 15
        padding = Insets(20)
        content = combs :+ btnRefresh
      }
      center = webView
    }

    true
  }
}
