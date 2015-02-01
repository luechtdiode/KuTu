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

//    insertNotenSkala(8, Map(//ARW rücklings
//        "12 <= 5.0 cm" -> 12d,
//        "11 5.1-6.9cm" -> 11d,
//        "10 7.0-8.9cm" -> 10d,
//        "9 9.0-10.9cm" -> 9d,
//        "8 11.0-12.9cm" -> 8d,
//        "7 13.0-14.9cm" -> 7d,
//        "6 15.0-16.9cm" -> 6d,
//        "5 17.0-18.9cm" -> 5d,
//        "4 19.0-20.9cm" -> 4d,
//        "3 21.0-22.9cm" -> 3d,
//        "2 23.0-24.9cm" -> 2d,
//        "1 25.0-27.0cm" -> 1d,
//        "0 >=27.0cm" -> 0d
//      ))
//    insertNotenSkala(9, Map(//Ein-Ausschultern
//        "12 Unter Schulterbreite" -> 12d,
//        "11 Unter Schulterbreite" -> 11d,
//        "10 In Schulterbreite" -> 10d,
//        "9 In Schulterbreite" -> 9d,
//        "8 Armstellung 30° u. weniger" -> 8d,
//        "7 Armstellung 30° u. weniger" -> 7d,
//        "6 Armstellung 45° u. weniger" -> 6d,
//        "5 Armstellung 45° u. weniger" -> 5d,
//        "4 Armstellung 45°" -> 4d,
//        "3 Armstellung 45°" -> 3d,
//        "2 Armstellung über 45°" -> 2d,
//        "1 Armstellung über 45°" -> 1d
//      ))
//    insertNotenSkala(10, Map(//Brücke
//        "12 Schulter hinter den Händen - Beine u. Arme gestreckt - Beine geschlossen" -> 12d,
//        "11 Schulter hinter den Händen - Beine u. Arme gestreckt - Beine geschlossen" -> 11d,
//        "10 Schulter über den Händen - Beine u. Arme gestreckt - Beine geschlossen" -> 10d,
//        "9 Schulter über den Händen - Beine u. Arme gestreckt - Beine geschlossen" -> 9d,
//        "8 Schulter vor den Händen - Beine u. Arme gestreckt - Beine geschlossen" -> 8d,
//        "7 Schulter vor den Händen - Beine u. Arme gestreckt - Beine geschlossen" -> 7d,
//        "6 Schulter vor den Händen - Knie und/oder Arme leicht gebeugt - Beine leicht offen" -> 6d,
//        "5 Schulter vor den Händen - Knie und/oder Arme leicht gebeugt - Beine leicht offen" -> 5d,
//        "4 Schulter vor den Händen - Knie und/oder Arme gebeugt - Beine offen" -> 4d,
//        "3 Schulter vor den Händen - Knie und/oder Arme gebeugt - Beine offen" -> 3d,
//        "2 Schulter vor den Händen - Knie und/oder Arme stark gebeugt - Beine offen" -> 2d,
//        "1 Schulter vor den Händen - Knie und/oder Arme stark gebeugt - Beine offen" -> 1d
//      ))
//
//    insertNotenSkala(11, Map(//Rumpfbeugen vorwärts
//        "12 Souveräne Bodenberührung des ganzen Brustbeins - Rücken gestreckt" -> 12d,
//        "11 Souveräne Bodenberührung des ganzen Brustbeins - Rücken gestreckt" -> 11d,
//        "10 Flüchtige Bodenberührung des Brustbeins - Rücken gestreckt" -> 10d,
//        "9 Flüchtige Bodenberührung des Brustbeins - Rücken gestreckt" -> 9d,
//        "8 Knapp keine Bodenberührung des Brustbeins - Rücken gestreckt" -> 8d,
//        "7 Knapp keine Bodenberührung des Brustbeins - Rücken gestreckt" -> 7d,
//        "6 Keine Bodenberührung des Brustbeins - Rücken leicht gekrümmt" -> 6d,
//        "5 Keine Bodenberührung des Brustbeins - Rücken leicht gekrümmt" -> 5d,
//        "4 Brustbein eindeutig vom Boden entfernt - Rücken gekrümmt" -> 4d,
//        "3 Brustbein eindeutig vom Boden entfernt - Rücken gekrümmt" -> 3d,
//        "2 Brustbein weit vom Boden entfernt - Rücken stark gekrümmt - Kopf nach vorne geneigt" -> 2d,
//        "1 Brustbein weit vom Boden entfernt - Rücken stark gekrümmt - Kopf nach vorne geneigt" -> 1d
//      ))
//    insertNotenSkala(11, Map(//Querspagat rechts
//        "12 Beine auf einer Linie - Hint. Bein nach unten - Kein oder min. Schrittspalt - Hüfte in 90°-45° zur Beinlinie" -> 12d,
//        "11 Beine auf einer Linie - Hint. Bein nach unten - Kein oder min. Schrittspalt - Hüfte in 90°-45° zur Beinlinie" -> 11d,
//        "10 Beine auf einer Linie - Hint. Bein leicht ausgedreht - Minimaler Schrittspalt - Hüfte in 90°-45° zur Beinlinie" -> 10d,
//        "9 Beine auf einer Linie - Hint. Bein leicht ausgedreht - Minimaler Schrittspalt - Hüfte in 90°-45° zur Beinlinie" -> 9d,
//        "8 Beine beinahe auf einer Linie - Hint. Bein leicht ausgedreht od. leicht gebeugt - Kleiner Schrittspalt - Hüfte in <45° zur Beinlinie" -> 8d,
//        "7 Beine beinahe auf einer Linie - Hint. Bein leicht ausgedreht od. leicht gebeugt - Kleiner Schrittspalt - Hüfte in <45° zur Beinlinie" -> 7d,
//        "6 Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Minimaler Schrittspalt - Hüfte in <45° zur Beinlinie" -> 6d,
//        "5 Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Minimaler Schrittspalt - Hüfte in <45° zur Beinlinie" -> 5d,
//        "4 Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie" -> 4d,
//        "3 Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie" -> 3d,
//        "2 Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und stark gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie" -> 2d,
//        "1 Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und stark gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie" -> 1d
//      ))
//    insertNotenSkala(12, Map(//Querspagat links
//        "Beine auf einer Linie - Hint. Bein nach unten - Kein oder min. Schrittspalt - Hüfte in 90°-45° zur Beinlinie" -> 12d,
//        "Beine auf einer Linie - Hint. Bein nach unten - Kein oder min. Schrittspalt - Hüfte in 90°-45° zur Beinlinie" -> 11d,
//        "Beine auf einer Linie - Hint. Bein leicht ausgedreht - Minimaler Schrittspalt - Hüfte in 90°-45° zur Beinlinie" -> 10d,
//        "Beine auf einer Linie - Hint. Bein leicht ausgedreht - Minimaler Schrittspalt - Hüfte in 90°-45° zur Beinlinie" -> 9d,
//        "Beine beinahe auf einer Linie - Hint. Bein leicht ausgedreht od. leicht gebeugt - Kleiner Schrittspalt - Hüfte in <45° zur Beinlinie" -> 8d,
//        "Beine beinahe auf einer Linie - Hint. Bein leicht ausgedreht od. leicht gebeugt - Kleiner Schrittspalt - Hüfte in <45° zur Beinlinie" -> 7d,
//        "Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Minimaler Schrittspalt - Hüfte in <45° zur Beinlinie" -> 6d,
//        "Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Minimaler Schrittspalt - Hüfte in <45° zur Beinlinie" -> 5d,
//        "Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie" -> 4d,
//        "Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie" -> 3d,
//        "Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und stark gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie" -> 2d,
//        "Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und stark gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie" -> 1d
//      ))
//    insertNotenSkala(13, Map(//Seitspagat
//        "Beine auf einer Linie - Beine sind ausgedreht - Kein Schrittspalt - Rücken gestreckt - Oberkörper aufrecht - Arme in Seithalte" -> 12d,
//        "Beine auf einer Linie - Beine sind ausgedreht - Kein Schrittspalt - Rücken gestreckt - Oberkörper aufrecht - Arme in Seithalte" -> 11d,
//        "Beine auf einer Linie - Beine sind nicht ausgedreht - Kein Schrittspalt - Rücken gestreckt - Oberkörper aufrecht - Arme in Seithalte" -> 10d,
//        "Beine auf einer Linie - Beine sind nicht ausgedreht - Kein Schrittspalt - Rücken gestreckt - Oberkörper aufrecht - Arme in Seithalte" -> 9d,
//        "Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Kleiner Schrittspalt - Rücken gestreckt - Oberkörper neigt nach vorne - Hände stützen ev. auf" -> 8d,
//        "Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Kleiner Schrittspalt - Rücken gestreckt - Oberkörper neigt nach vorne - Hände stützen ev. auf" -> 7d,
//        "Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Kleiner bis mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt nach vorne - Hände stützen ev. auf" -> 6d,
//        "Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Kleiner bis mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt nach vorne - Hände stützen ev. auf" -> 5d,
//        "Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt stark nach vorne - Hände stützen ev. auf" -> 4d,
//        "Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt stark nach vorne - Hände stützen ev. auf" -> 3d,
//        "Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt stark nach vorne - Hände stützen auf" -> 2d,
//        "Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt stark nach vorne - Hände stützen auf" -> 1d
//      ))
    insertNotenSkala(15, Map(//Sprint
        "3.0sek" -> 96d,
        "3.05sek" -> 94d,
        "3.1sek" -> 92d,
        "3.15sek" -> 90d,
        "3.2sek" -> 88d,
        "3.25sek" -> 86d,
        "3.3sek" -> 84d,
        "3.35sek" -> 82d,
        "3.4sek" -> 80d,
        "3.45sek" -> 78d,
        "3.5sek" -> 76d,
        "3.55sek" -> 74d,
        "3.6sek" -> 69d,
        "3.65sek" -> 67d,
        "3.7sek" -> 65d,
        "3.75sek" -> 64d,
        "3.8sek" -> 63d,
        "3.85sek" -> 62d,
        "3.9sek" -> 61d,
        "3.95sek" -> 60d,
        "4.0sek" -> 59d,
        "4.05sek" -> 58d,
        "4.1sek" -> 57d,
        "4.15sek" -> 56d,
        "4.2sek" -> 55d,
        "4.25sek" -> 54d,
        "4.3sek" -> 53d,
        "4.35sek" -> 52d,
        "4.4sek" -> 51d,
        "4.45sek" -> 50d,
        "4.5sek" -> 49d,
        "4.55sek" -> 48d,
        "4.6sek" -> 46d,
        "4.65sek" -> 44d,
        "4.7sek" -> 40d,
        "4.75sek" -> 38d,
        "4.8sek" -> 36d,
        "4.85sek" -> 34d,
        "4.9sek" -> 32d,
        "4.95sek" -> 30d,
        "5.0sek" -> 28d,
        "5.05sek" -> 26d,
        "5.1sek" -> 24d,
        "5.15sek" -> 22d,
        "5.2sek" -> 20d,
        "5.25sek" -> 18d,
        "5.3sek" -> 16d,
        "5.35sek" -> 14d,
        "5.4sek" -> 12d,
        "5.45sek" -> 9d,
        "5.5sek" -> 6d,
        "5.55sek" -> 3d,
        "5.6sek" -> 1d
      ))

  }
}