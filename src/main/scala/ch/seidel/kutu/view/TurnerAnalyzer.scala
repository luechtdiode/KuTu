package ch.seidel.kutu.view

import ch.seidel.commons.TabWithService
import ch.seidel.kutu.domain.*
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.geometry.Bounds
import javafx.scene.chart.XYChart.Data
import javafx.scene.transform.Scale
import javafx.scene.{Group, Node, Parent}
import org.slf4j.{Logger, LoggerFactory}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.geometry.Side
import scalafx.print.PageLayout.sfxPageLayout2jfx
import scalafx.print.PrinterJob.sfxPrinterJob2jfx
import scalafx.print.{PageOrientation, Paper, Printer, PrinterJob}
import scalafx.scene.chart.*
import scalafx.scene.chart.BarChart.sfxBarChart2jfx
import scalafx.scene.chart.XYChart.Series
import scalafx.scene.control.*
import scalafx.scene.effect.Glow
import scalafx.scene.effect.Glow.sfxGlow2jfx
import scalafx.scene.layout.VBox.sfxVBox2jfx
import scalafx.scene.layout.{BorderPane, Priority, VBox}
import scalafx.scene.text.Text
import scalafx.scene.text.Text.sfxText2jfx

import scala.math.BigDecimal.int2bigDecimal

class TurnerAnalyzer(val verein: Option[Verein], val athlet: Option[Athlet], val wettkampfdisziplin: Option[WettkampfdisziplinView], override val service: KutuService) extends Tab with TabWithService {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  
  def onDrillDown(a: Athlet): Unit = {

  }

  def onDrillDown(a: WettkampfdisziplinView): Unit = {

  }

  def print(node: Node): Unit = {
    val printer = Printer.defaultPrinter
    val pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.Portrait, Printer.MarginType.Default)
    val scaleX = pageLayout.getPrintableWidth / node.getBoundsInParent.getWidth
    val scaleY = pageLayout.getPrintableHeight / node.getBoundsInParent.getHeight
    val scale = new Scale(scaleX, scaleY)
    node.getTransforms.add(scale)

    val job = PrinterJob.createPrinterJob
    if job != null then {
      val success = job.printPage(node)
      if success then {
        job.endJob()
      }
      node.getTransforms.remove(scale)
    }
  }
  val glow = new Glow(.8)

  def displayLabelForData[A,B](data: Data[A, B]): Data[A, B] = {
    // val node = data.nodeProperty()
    val (dataText, isLandscape) = if data.getYValue.isInstanceOf[String] then (new Text(s"${data.getXValue}"), true) else (new Text(s"${data.getYValue}"), false)

    data.nodeProperty().addListener(new ChangeListener[Node]() {
      override def changed(ov: ObservableValue[? <: Node], oldNode: Node, node: Node): Unit = {
        if node != null then {
          node.setEffect(null)
          node.onMouseEntered = _ => {
            node.setEffect(glow)
          }
          node.onMouseExited = _ => {
            node.setEffect(null)
          }
          node.onMouseClicked = _ => {
            data.extraValue.value match {
              case a: AthletView => onDrillDown(a.toAthlet)
              case d: WettkampfdisziplinView => onDrillDown(d)
              case _ =>
            }
//            System.out.logger.debug(data.getXValue() + " : " + data.getYValue())
          }

          node.parentProperty().addListener(new ChangeListener[Parent]() {
            override def changed(ov: ObservableValue[? <: Parent], oldParent: Parent, parent: Parent): Unit = {
              val parentGroup = parent.asInstanceOf[Group]
              parentGroup.getChildren.add(dataText)
            }
          })
          node.boundsInParentProperty().addListener(new ChangeListener[Bounds]() {
            override def changed(ov: ObservableValue[? <: Bounds], oldBounds: Bounds, bounds: Bounds): Unit = {
              dataText.setLayoutX(bounds.getMinX)
              dataText.setVisible(false)
              Platform.runLater{
              dataText.setVisible(true)
              if isLandscape then {
                dataText.setLayoutX(
                  Math.min(
                      Math.round(bounds.getWidth + dataText.prefWidth(-1) * 0.5).toDouble,
                      dataText.parent.value.boundsInLocalProperty().get.getWidth - dataText.prefWidth(-1)
                  )
                )
                dataText.setLayoutY(
                  Math.max(
                      Math.round(bounds.getMaxY - bounds.getHeight / 2 + dataText.prefHeight(-1) / 2) - 5d,
                      dataText.parent.value.boundsInLocalProperty().get.getMinY
                  )
                )
              }
              else {
                dataText.setLayoutX(
                  Math.round(
                    bounds.getMinX + bounds.getWidth / 2 - dataText.prefWidth(-1) / 2
                  ).toDouble
                )
                dataText.setLayoutY(
                  Math.max(
                      Math.round(bounds.getMinY - dataText.prefHeight(-1) * 0.5).toDouble,
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

  def toSeries(disciplinename: String, serie: Seq[(String,Object,Number)]): Series[String, Number] = {
    val series = serie.foldLeft(new Series[String, Number]()){(acc, pair) =>
      val d: Data[String,Number] = new Data[String,Number](pair._1, pair._3, pair._2)
      displayLabelForData(d)
      acc.data().add(d)
      acc
    }
    series.name = disciplinename
    series
  }

  def toSeriesq(disciplinename: String, serie: Seq[(String,Object,Number)]): Series[Number, String] = {
    val series = serie.foldLeft(new Series[Number,String]()){(acc, pair) =>
      val d = new Data[Number,String](pair._3, pair._1, pair._2)
      displayLabelForData(d)
      acc.data().add(d)
      acc
    }
    series.name = disciplinename
    series
  }

  override def isPopulated: Boolean = {
    val qry = service.selectWertungen(vereinId = verein.map(_.id), athletId = athlet.map(_.id))
    val charts = new VBox
    for
      (programm, pwertungen) <- qry.filter(x => x.endnote.nonEmpty).groupBy { x => x.wettkampfdisziplin.programm.wettkampfprogramm }.toList.sortBy(x => x._1.ord)
    do
    {
      var hastoadd = false
      val pwg = pwertungen.
        filter{x => wettkampfdisziplin match {case Some(d) => d.disziplin.id == x.wettkampfdisziplin.disziplin.id case None => true}}.
        groupBy { x => x.wettkampf }.
        toSeq.sortBy(x => x._1.datum.getTime)
      // val legend = pwg.map(x => x._1.easyprint)

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
        }) + wettkampfdisziplin.map { " - " + _.disziplin.name }.getOrElse("")
        legendSide = Side.Right
        legendVisible = true
        alternativeRowFillVisible = true
      }
      val withDNotes = pwg.map(x => x._2).flatMap(x => x.filter(x => x.noteD.sum > 0).map(_.wettkampfdisziplin.id)).toSet.nonEmpty
      val elemente = athlet match {
        case None => pwg.map(x => x._2).flatMap(x => x.map(_.athlet.id)).toSet.size
        case _    => pwg.map(x => x._2).flatMap(x => x.map(_.wettkampfdisziplin.id)).toSet.size * (if withDNotes then 2 else 1)
      }
      val chartheight = elemente * pwg.size * 40d
      for
        (wettkampf, awertungen) <- pwg
      do
      {
//        logger.debug(s"für Programm ${programm.easyprint} und Wettkampf ${wettkampf.easyprint}")
        val sumPerDivider = awertungen.
        groupBy { x => athlet match {case None => x.athlet case _ => x.wettkampfdisziplin}}.
        map{x =>
          val resultate = x._2.map(_.resultat)
          (x._1.easyprint.take(30), x._1, resultate.map(x => x.endnote).sum, resultate.map(x => x.noteD).sum)
        }.
        filter(x => x._3 > 1).
        toSeq.sortBy(x => x._3)
        if sumPerDivider.nonEmpty then {
          if withDNotes then {
            lineChart.data().add(toSeriesq("D-Note " +wettkampf.easyprint, sumPerDivider.map(x => (x._1, x._2, x._4))))
            lineChart.data().add(toSeriesq("Endnote " + wettkampf.easyprint, sumPerDivider.map(x => (x._1, x._2, x._3))))
          }
          else {
            lineChart.data().add(toSeriesq(wettkampf.easyprint, sumPerDivider.map(x => (x._1, x._2, x._3))))
          }
          hastoadd = true
        }
        val ph = lineChart.delegate.prefHeight(-1d)
        lineChart.minHeight = math.max(chartheight, ph)
        lineChart.prefHeight = lineChart.minHeight.value
        lineChart.maxHeight = lineChart.minHeight.value
//        logger.debug(s"Wettkampf ${wettkampf.easyprint} berechnete Charthöhe ${chartheight} eff. ZH ${lineChart.minHeight.value} ${elemente}, ${pwg.size}")
      }
      if hastoadd then {
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
          onAction = (_: ActionEvent) => {
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
}