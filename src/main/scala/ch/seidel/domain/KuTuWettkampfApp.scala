package ch.seidel.domain

import scala.slick.jdbc.GetResult
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Session
import scala.slick.jdbc.StaticQuery._
import javax.swing.JFrame
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JScrollPane
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene._
import scalafx.scene.control._
import scalafx.scene.control.TableColumn._
import scalafx.scene.layout._
import scalafx.scene.paint._
import scalafx.scene.paint.Color._
import scalafx.scene.web._
import scalafx.collections._
import scalafx.geometry._
import scalafx.beans.property.StringProperty
import scalafx.beans.property.ReadOnlyStringWrapper
import java.sql.Date
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.cell.TextFieldTableCell

object KuTuWettkampfApp extends JFXApp with KutuService {
  // Open a database connection

  database withSession { implicit session =>
    val verein = 1l
    val programmEP = 12L
    val id = insertAthlete(Athlet(2, "Mandume", "Lucien", Some("15.02.2005"), Some(verein)))
    insertAthlete(Athlet(1, "Weihofen", "Yannik", None, Some(verein)))
    insertAthlete(Athlet(3, "Antanasio", "Noah", None, Some(verein)))
    insertAthlete(Athlet(4, "Zgraggen", "Ruedi", Some("15.02.2005"), None))
    insertAthlete(Athlet(5, "Schneider", "Catherine", Some("15.02.2005"), Some(verein)))
    insertAthlete(Athlet(0, "Mandume", "Lucien", Some("15.02.2005"), Some(verein)))

    val currentscore = Map[Long,Long](
        1L -> programmEP,
        2L -> programmEP,
        3L -> (programmEP + 1L),
        4L -> (programmEP + 1L),
        5L -> (programmEP + 2L)
        )
    def mapFilter(pgm: Long, a: Athlet): Boolean = currentscore.get(a.id) match{
      case Some(p) => p.equals(pgm)
      case _ => false
    }
//    println(id)
//
//    for(a <- selectAthletes) {
//      println(a)
//    }
//
    println(createWettkampf("15.02.2015", "Jugendcup 2015", Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter)))
    println(createWettkampf("16.01.2015", "Athletiktest NKL Frühling 2015", Set(9,10)))
//    assignAthletsToWettkampf(27L, Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter))
//    assignAthletsToWettkampf(24L, Set(9,10))
    val daten = selectWertungen().groupBy { x =>
      x.wettkampf }.map(x => (x._1, x._2.groupBy { x =>
        x.wettkampfdisziplin.programm }.map(x => (x._1, x._2.groupBy { x =>
          x.athlet }))))

    val content = new StringBuffer()
    content.append("<html><body>")
    for {
      w <- daten.keys
    } {
//      println(w.datum, w.titel)
      for {
        p <- daten(w)
      } {
//        println(s"  ${p._1.toPath}")
        content.append(s"<h1>${p._1.toPath}</h1>").append("\n")
        for {
          a <- p._2.keys
        } {
//          println(s"    ${a.vorname} ${a.name} (${a.verein.map { _.name }.getOrElse("ohne Verein")})")
          content.append(s"<h2>${a.vorname} ${a.name} (${a.verein.map { _.name }.getOrElse("ohne Verein")})</h2>").append("\n").append("<table>")
          for {
            d <- p._2(a)
          } {
//            println(f"    ${d.wettkampfdisziplin.disziplin.name}%70s ${d.noteD}%2.3f ${d.noteE}%2.3f ${d.endnote}%2.3f")
            content.append(f"<tr><td>${d.wettkampfdisziplin.disziplin.name}%70s</td><td>${d.noteD}%2.3f</td><td>${d.noteE}%2.3f</td><td>${d.endnote}%2.3f</td></tr>").append("\n")
          }
          content.append("</table>")
        }
      }
    }
    content.append("</body></html>")
    val report = content.toString()

    println(content)

    val browser = new WebView {
      hgrow = Priority.ALWAYS
      vgrow = Priority.ALWAYS
      onAlert = (e: WebEvent[_]) => println("onAlert: " + e)
      onStatusChanged = (e: WebEvent[_]) => println("onStatusChanged: " + e)
      onResized = (e: WebEvent[_]) => println("onResized: " + e)
      onVisibilityChanged = (e: WebEvent[_]) => println("onVisibilityChanged: " + e)
    }
    val engine = browser.engine
    val wkModel = ObservableBuffer[WettkampfView](listWettkaempfeView)

    val wkview = new TableView[WettkampfView](wkModel) {
        columns ++= List(
          new TableColumn[WettkampfView, String] {
            text = "Datum"
            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "datum", sdf.format(x.value.datum)) }
            prefWidth = 80
            //onContextMenuRequested = handle { wkview.selectionModel}
          },
          new TableColumn[WettkampfView, String] {
            text = "Wettkampf"
            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "titel", x.value.titel) }
            prefWidth = 200
          }
        )
    }

    stage = new PrimaryStage {
      title = "KuTu App"
      scene = new Scene {
        content = new HBox {
          padding = Insets(20)
          content = Seq(
            new VBox{
              fill = new LinearGradient(
                endX = 0,
                stops = Stops(PALEGREEN, SEAGREEN)
              )
              content = wkview
            },
            browser
          )
        }
      }
    }
//    val frame = new JFrame("KuTu App")
//    frame.getContentPane.setLayout(new BorderLayout(5,5))
//    val resultpanel = new JLabel();
//    resultpanel.setText(content.toString())
//    frame.getContentPane.add(new JScrollPane(resultpanel));
//    frame.pack()
//    frame.setVisible(true)
//
//    for(w <- listWettkaempfeView) {
//      println(w)
//    }
  }
}