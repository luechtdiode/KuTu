package ch.seidel

import scala.collection.JavaConversions
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.scene.control._

import scalafx.scene.chart._
import javafx.scene.chart.XYChart.Data
import scalafx.scene.chart.XYChart.Series
import scalafx.scene.control.Tab
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.HBox
import scalafx.geometry.Insets
import scalafx.scene.layout.VBox
import scalafx.scene.control.ScrollPane
import scalafx.geometry.Side
import scalafx.scene.chart.LineChart
import scalafx.collections.ObservableArray
import scalafx.collections.ObservableBuffer
import ch.seidel.commons.TabWithService
import ch.seidel.domain._
import scalafx.scene.text.Text
import javafx.beans.value.ChangeListener
import javafx.scene.Parent
import javafx.beans.value.ObservableValue
import javafx.scene.Group
import javafx.geometry.Bounds
import javafx.scene.Node
import scalafx.application.Platform
import javafx.scene.transform.Scale
import scalafx.print.Printer
import scalafx.print.Paper
import scalafx.print.PageOrientation
import scalafx.print.PrinterJob
import scalafx.event.ActionEvent

class TurnerAnalyzer(val verein: Option[Verein], val athlet: Option[Athlet], override val service: KutuService) extends Tab with TabWithService {
  def print(node: Node) {
    val printer = Printer.defaultPrinter;
    val pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.Portrait, Printer.MarginType.Default);
    val scaleX = pageLayout.getPrintableWidth() / node.getBoundsInParent().getWidth();
    val scaleY = pageLayout.getPrintableHeight() / node.getBoundsInParent().getHeight();
    val scale = new Scale(scaleX, scaleY)
    node.getTransforms().add(scale);

    val job = PrinterJob.createPrinterJob;
    if (job != null) {
      val success = job.printPage(node);
      if (success) {
        job.endJob();
      }
      node.getTransforms().remove(scale)
    }
  }

  def displayLabelForData[A,B](data: Data[A, B]) = {
    val node = data.nodeProperty()
    val (dataText, isLandscape) = if(data.getYValue().isInstanceOf[String]) (new Text(data.getXValue() + ""), true) else (new Text(data.getYValue() + ""), false)

    data.nodeProperty().addListener(new ChangeListener[Node]() {
      override def changed(ov: ObservableValue[_<: Node], oldNode: Node, node: Node) {
        if(node != null) {
         node.parentProperty().addListener(new ChangeListener[Parent]() {
            override def changed(ov: ObservableValue[_<: Parent], oldParent: Parent, parent: Parent) {
              val parentGroup = parent.asInstanceOf[Group]
              parentGroup.getChildren().add(dataText)
            }
          })

          node.boundsInParentProperty().addListener(new ChangeListener[Bounds]() {
            override def changed(ov: ObservableValue[_<: Bounds], oldBounds: Bounds, bounds: Bounds) {
              dataText.setLayoutX(bounds.getMinX)
              dataText.setVisible(false)
              Platform.runLater{
              dataText.setVisible(true)
              if(isLandscape) {
                dataText.setLayoutX(
                  Math.min(
                      Math.round(bounds.getWidth + dataText.prefWidth(-1) * 0.5),
                      dataText.parent.value.boundsInLocalProperty().get.getWidth - dataText.prefWidth(-1)
                  )
                )
                dataText.setLayoutY(
                  Math.max(
                      Math.round(bounds.getMaxY() - bounds.getHeight() / 2 + dataText.prefHeight(-1) / 2) - 5d,
                      dataText.parent.value.boundsInLocalProperty().get.getMinY
                  )
                )
              }
              else {
                dataText.setLayoutX(
                  Math.round(
                    bounds.getMinX() + bounds.getWidth() / 2 - dataText.prefWidth(-1) / 2
                  )
                )
                dataText.setLayoutY(
                  Math.max(
                      Math.round(bounds.getMinY() - dataText.prefHeight(-1) * 0.5),
                      dataText.parent.value.boundsInLocalProperty().get.getMinY
                  )
                )
              }
            }
            }
          })
        }
      }
    })

    data
  }

  def toSeries(disciplinename: String, serie: Seq[(String,Number)]) = {
    val series = serie.foldLeft(new Series[String, Number]()){(acc, pair) =>
      val d: Data[String,Number] = new Data[String,Number](pair._1, pair._2)
      displayLabelForData(d)
      acc.data.get.add(d)
      acc
    }
    series.name = disciplinename
    series
  }

  def toSeriesq(disciplinename: String, serie: Seq[(String,Number)]) = {
    val series = serie.foldLeft(new Series[Number,String]()){(acc, pair) =>
      val d = new Data[Number,String](pair._2, pair._1)
      displayLabelForData(d)
      acc.data.get.add(d)
      acc
    }
    series.name = disciplinename
    series
  }

  override def isPopulated = {
    val qry = service.selectWertungen(vereinId = verein.map(_.id), athletId = athlet.map(_.id))
    val charts = new VBox
    for {
      (programm, pwertungen) <- qry.filter(x => x.endnote > 0).groupBy { x => x.wettkampfdisziplin.programm.wettkampfprogramm }.toList.sortBy(x => x._1.ord)
    }
    {
      var hastoadd = false
      val pwg = pwertungen.groupBy { x => x.wettkampf }.toSeq.sortBy(x => x._1.datum.getTime)
      val legend = pwg.map(x => x._1.easyprint)

      val xAxis = new CategoryAxis() {
        tickLabelRotation = 360d
      }
      val yAxis = new NumberAxis() {
        forceZeroInRange = false
      }
      val lineChart = new BarChart[Number,String](yAxis, xAxis) {
        title = programm.easyprint + (athlet match {
          case None    => ""
          case Some(a) => " - " + a.easyprint
        })
        legendSide = Side.RIGHT
        legendVisible = true
        alternativeRowFillVisible = true
      }
      val elemente = athlet match {
        case None => pwg.map(x => x._2).flatMap(x => x.map(_.athlet.id)).toSet.size
        case _    => pwg.map(x => x._2).flatMap(x => x.map(_.wettkampfdisziplin.id)).toSet.size
      }
      val chartheight = elemente * pwg.size * 40d
      for{
        (wettkampf, awertungen) <- pwg
      }
      {
//        println(s"für Programm ${programm.easyprint} und Wettkampf ${wettkampf.easyprint}")
        val sumPerDivider = awertungen.
        groupBy { x => athlet match {case None => x.athlet case _ => x.wettkampfdisziplin}}.
        map(x => (x._1.easyprint.take(30), x._2.map { x => x.endnote }.sum)).
//        filter(x => x._2 > 1).
        toSeq
        if(sumPerDivider.filter(x => x._2 > 1).nonEmpty) {
          lineChart.data.get.add(toSeriesq(wettkampf.easyprint, sumPerDivider.filter(x => x._2 > 1)))
          hastoadd = true
        }
        val ph = lineChart.delegate.prefHeight(-1d)
        lineChart.minHeight = math.max(chartheight, ph)
        lineChart.prefHeight = lineChart.minHeight.value
        lineChart.maxHeight = lineChart.minHeight.value
//        println(s"Wettkampf ${wettkampf.easyprint} berechnete Charthöhe ${chartheight} eff. ZH ${lineChart.minHeight.value} ${elemente}, ${pwg.size}")
      }
      if(hastoadd) {
        charts.getChildren.add(lineChart)
      }
    }

    content = new BorderPane {
      vgrow = Priority.Always
      hgrow = Priority.Always
      top = new ToolBar {
        content = List(new Button {
          text = "Drucken"
          minWidth = 75
          onAction = (event: ActionEvent) => {
            print(charts)
          }
        }
        )
      }
      center = new ScrollPane {
        fitToWidth = true
        fitToHeight = true
        content = charts
      }
      //charts
    }
    true
  }

  def isPopulatedBar = {
    val qry = verein match {
      case Some(v) => service.selectWertungen(vereinId = Some(v.id))
      case None    => service.selectWertungen()
    }
    val charts = new VBox
    for {
      (programm, pwertungen) <- qry.filter(x => x.endnote > 0).groupBy { x => x.wettkampfdisziplin.programm.wettkampfprogramm }.toList.sortBy(x => x._1.ord)
    }
    {
      var hastoadd = false
      val pwg = pwertungen.groupBy { x => x.wettkampf }.toSeq.sortBy(x => x._1.datum.getTime)
      val legend = pwg.map(x => x._1.easyprint)

      val xAxis = new CategoryAxis() {
        tickLabelRotation = 360d
      }
      val yAxis = new NumberAxis() {
        forceZeroInRange = false
      }
      val lineChart = new BarChart[String,Number](xAxis,yAxis) {
        title = programm.easyprint
        minHeight = 300d
      }

      for{
        (wettkampf, awertungen) <- pwg
      }
      {
        val sumPerAthlet = awertungen.
        groupBy { x => x.athlet }.
        map(x => (x._1.easyprint, x._2.map { x => x.endnote }.sum)).
        toSeq
        if(sumPerAthlet.filter(x => x._2 > 1).nonEmpty) {
          lineChart.data.get.add(toSeries(wettkampf.easyprint, sumPerAthlet))
          println(wettkampf.easyprint, sumPerAthlet)
          hastoadd = true
        }
      }
      if(hastoadd) {
        lineChart.legendSide = Side.RIGHT
        lineChart.legendVisible = true
        charts.getChildren.add(lineChart)
      }
    }

    content = new BorderPane {
      vgrow = Priority.Always
      hgrow = Priority.Always
      top = new ToolBar {
        content = List(new Button {
          text = "Drucken"
          minWidth = 75
          onAction = (event: ActionEvent) => {
            print(charts)
          }
        }
        )
      }
      center = new ScrollPane {
        fitToWidth = true
        fitToHeight = true
        content = charts
      }
      //charts
    }
    true
  }

  def isPopulatedLine = {
    val qry = verein match {
      case Some(v) => service.selectWertungen(vereinId = Some(v.id))
      case None    => service.selectWertungen()
    }
    val charts = new VBox
    for {
      (programm, pwertungen) <- qry.groupBy { x => x.wettkampfdisziplin.programm.head }
    }
    {
      val xAxis = new CategoryAxis()
      val yAxis = new NumberAxis()
//      xAxis.setLabel("Wettkampf")
      val lineChart = new LineChart[String,Number](xAxis,yAxis)
      lineChart.setTitle(programm.easyprint)
      var hastoadd = false
      for{
        (athlet, awertungen) <- pwertungen.groupBy { x => x.athlet }
      }
      {
        val sumPerWettkampf = awertungen.
        groupBy { x => x.wettkampf }.
        map(x => (x._1, x._2.map { x => x.endnote }.sum)).
        toSeq.sortBy(x => x._1.datum.getTime).
        map(x => (x._1.datum.toString, x._2))
        if(sumPerWettkampf.filter(x => x._2 > 0).nonEmpty) {
          lineChart.data.get.add(toSeries(athlet.easyprint, sumPerWettkampf))
          hastoadd = true
        }
      }
      if(hastoadd) {
        lineChart.legendSide = Side.RIGHT
        lineChart.legendVisible = true
        charts.getChildren.add(lineChart)
      }
    }

    content = new BorderPane {
      vgrow = Priority.Always
      hgrow = Priority.Always
      top = new ToolBar {
        content = List(new Button {
          text = "Drucken"
          minWidth = 75
          onAction = (event: ActionEvent) => {
            print(charts)
          }
        }
        )
      }
      center = new ScrollPane {
        content = charts
      }
    }
    true
  }
}