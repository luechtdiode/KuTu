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

class RanglisteTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService {
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
    val groupers = List(ByNothing, ByProgramm, ByJahrgang, ByGeschlecht, ByVerein, ByDisziplin)
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
        ByProgramm
      }
      else {
        cblist.foldLeft(cblist.head)((acc, cb) => if (acc != cb) acc.groupBy(cb) else acc)
      }
    }

    def refreshRangliste(query: GroupBy) {
      val combination = query.select(service.selectWertungen().filter(p => p.wettkampf.id == wettkampf.id)).toList
      webView.engine.loadContent(toHTML(combination, 0))
    }

    def toHTML(gs: List[GroupSection], level: Int): String = {
      val gsBlock = new StringBuilder()
      if (level == 0) {
        gsBlock.append(s"""<html><head>
            <style type="text/css">
              body {
                font-family: "Arial", "Verdana", sans-serif;
              }
              table{
                  /*table-layout:fixed;*/
                  border-collapse:collapse;
                  border-spacing:0;
                  border-style:hidden;
              }
              th {
                background-color: rgb(250,250,200);
                font-size: 9px;
              }
              td {
                padding:0.25em;
              }
              td .data {
                text-align: right
              }
              td .valuedata {
                text-align: right
              }
              td .hintdata {
                color: rgb(50,100,150);
                font-size: 9px;
                text-align: right
              }
              col:first-child {
                background: rgb(250, 250, 200, 0.6);
              }
              col:nth-child(4n+6) {
                background: rgba(150, 150, 150, 0.6);
              }
              col:nth-child(4n+4) {
                border-left: 1px solid black;
              }
              tr:nth-child(even) .data {background: rgba(230, 230, 230, 0.6);}
              tr:nth-child(odd) .data {background: rgba(210, 200, 180, 0.6);}
              /*.disziplin {
                -webkit-transform: rotate(90deg);
                -moz-transform: rotate(90deg);
                -o-transform: rotate(90deg);
                writing-mode: lr-tb;
              }*/
            </style>
            </head><body><h1>Rangliste ${wettkampf.easyprint}</h1>\n""")
      }
      for (c <- gs) {
        c match {
          case gl: GroupLeaf =>
            gsBlock.append(s"<h${level + 2}>${gl.groupKey.easyprint}</h${level + 2}>\n<table width='100%'>\n")
            val cols = gl.buildColumns
            cols.foreach { th =>
              if (th.columns.size > 0) {
                cols.foreach { thc =>
                  gsBlock.append(s"<col/>")
                }
              }
              else {
                gsBlock.append(s"<col/>")
              }
            }
            gsBlock.append(s"\n<thead><tr class='head'>\n")
            cols.foreach { th =>
              if (th.columns.size > 0) {
                gsBlock.append(s"<th colspan=${th.columns.size}>${th.getText}</th>")
              }
              else {
                gsBlock.append(s"<th rowspan=2>${th.getText}</th>")
              }
            }
            gsBlock.append(s"</tr><tr>\n")
            cols.foreach { th =>
              if (th.columns.size > 0) {
                th.columns.foreach { th =>
                  gsBlock.append(s"<th>${th.getText}</th>")
                }
              }
            }
            gsBlock.append(s"</tr></thead><tbody>\n")
            gl.getTableData.foreach { row =>
              gsBlock.append(s"<tr class='data'>")
              cols.foreach { col =>
                if (col.columns.size == 0) {
                  val c = col.asInstanceOf[jfxsc.TableColumn[GroupRow, String]]
                  val feature = new CellDataFeatures(dummyTableView, c, row)
                  if (c.getStyleClass.contains("hintdata")) {
                    gsBlock.append(s"<td class='data'><div class='hintdata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                  }
                  else if (c.getStyleClass.contains("data")) {
                    gsBlock.append(s"<td class='data'>${c.getCellValueFactory.apply(feature).getValue}</td>")
                  }
                  else {
                    gsBlock.append(s"<td class='data'><div class='valuedata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                  }
                }
                else {
                  col.columns.foreach { ccol =>
                    val c = ccol.asInstanceOf[jfxsc.TableColumn[GroupRow, String]]
                    val feature = new CellDataFeatures(dummyTableView, c, row)
                    if (c.getStyleClass.contains("hintdata")) {
                      gsBlock.append(s"<td class='data'><div class='hintdata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                    }
                    else if (c.getStyleClass.contains("data")) {
                      gsBlock.append(s"<td class='data'>${c.getCellValueFactory.apply(feature).getValue}</td>")
                    }
                    else {
                      gsBlock.append(s"<td class='data'><div class='valuedata'>${c.getCellValueFactory.apply(feature).getValue}</div></td>")
                    }
                  }
                }
              }
              gsBlock.append(s"</tr>\n")
            }
            gsBlock.append(s"</tbody></table>\n")

          case g: GroupNode => gsBlock.append(s"<h${level + 2}>${g.groupKey.easyprint}</h${level + 2}>\n").append(toHTML(g.next.toList, level + 1))
          case s: GroupSum  => gsBlock.append(s.easyprint)
        }
      }
      gsBlock.append("</body></html>")
      gsBlock.toString()
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
