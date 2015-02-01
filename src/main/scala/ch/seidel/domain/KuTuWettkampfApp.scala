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
//    val verein = 1l
//    val programmEP = 12L
//    val id = insertAthlete(
//                  Athlet(2, 0, "M", "Mandume", "Lucien", Some("18.07.2004"), "", "", "", Some(verein)))
//    insertAthlete(Athlet(1, 0, "M", "Weihofen", "Yannik", None, "", "", "", Some(verein)))
//    insertAthlete(Athlet(3, 0, "M", "Antanasio", "Noah", Some("21.03.2007"), "", "", "", Some(verein)))
//    insertAthlete(Athlet(4, 0, "M", "Brentini", "Lino", Some("18.03.2004"), "", "", "", Some(verein)))
//    insertAthlete(Athlet(5, 0, "M", "Schneider", "Catherine", Some("15.02.1965"), "", "", "", None))
//    insertAthlete(Athlet(0, 0, "M", "Botross", "Jakob", Some("11.03.2005"), "", "", "", Some(verein)))
//    insertAthlete(Athlet(0, 0, "M", "Burger", "Noam", Some("27.10.2005"), "", "", "", Some(verein)))
//    insertAthlete(Athlet(0, 0, "M", "Gasio", "Aaron", Some("31.01.2005"), "", "", "", Some(verein)))
//    insertAthlete(Athlet(0, 0, "M", "Kostic", "Alexander", Some("19.05.2005"), "", "", "", Some(verein)))
//    insertAthlete(Athlet(0, 0, "M", "Lüber", "Lukas", Some("21.08.2005"), "", "", "", Some(verein)))
//    insertAthlete(Athlet(0, 0, "F", "Grossman", "Audrey", Some("28.02.2006"), "", "", "", Some(verein)))
//    insertAthlete(Athlet(0, 0, "M", "Mebert", "Lenny", Some("03.03.2007"), "", "", "", Some(verein)))
//
//    val currentscore = Map[Long,Long](
//        1L -> programmEP,
//        2L -> programmEP,
//        3L -> (programmEP + 1L),
//        4L -> (programmEP + 1L),
//        5L -> (programmEP + 2L)
//        )
//    def mapFilter(pgm: Long, a: Athlet): Boolean = currentscore.get(a.id) match{
//      case Some(p) => p.equals(pgm)
//      case _ => false
//    }
//    println(id)
//
//    for(a <- selectAthletes) {
//      println(a)
//    }
//
//    println(createWettkampf("15.02.2015", "Jugendcup 2015", Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter)))
//    println(createWettkampf("16.01.2015", "Athletiktest NKL Frühling 2015", Set(4,5,6,7,8,9)))
//    assignAthletsToWettkampf(27L, Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter))
//    assignAthletsToWettkampf(24L, Set(9,10))

    def insertNotenSkala(disziplinid: Long, skala: Map[String, Double]) {
      for(beschr <- skala.keys.toList.sortBy{skala}.reverse) {
        sqlu"""
          INSERT INTO kutu.notenskala
          (wettkampfdisziplin_id, kurzbeschreibung, punktwert)
          VALUES(${disziplinid}, ${beschr}, ${skala(beschr)})
        """.execute
      }
    }

  }
}