package ch.seidel.domain

import scala.slick.jdbc.GetResult
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Session
import scala.slick.jdbc.StaticQuery._

object KuTuWettkampfApp extends App with KutuService {
  // Open a database connection

  database withSession { implicit session =>
    val verein = 1l
    val programmEP = 12L
//    val id = insertAthlete(Athlet(2, "Mandume", "Lucien", Some("15.02.2005"), Some(verein)))
//    insertAthlete(Athlet(1, "Weihofen", "Yannik", None, Some(verein)))
//    insertAthlete(Athlet(3, "Antanasio", "Noah", None, Some(verein)))
//    insertAthlete(Athlet(4, "Zgraggen", "Ruedi", Some("15.02.2005"), None))
//    insertAthlete(Athlet(5, "Schneider", "Catherine", Some("15.02.2005"), Some(verein)))
//    insertAthlete(Athlet(0, "Mandume", "Lucien", Some("15.02.2005"), Some(verein)))

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
//    println(createWettkampf("15.02.2005", "Testwettkampf", Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter)))
//    println(createWettkampf("16.02.2004", "Testwettkampf", Set(9,10)))
//    println(createWettkampf("16.02.2005", "Testwettkampf", Set(9,10)))
//    assignAthletsToWettkampf(27L, Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter))
//    assignAthletsToWettkampf(24L, Set(9,10))
    val daten = selectWertungen().groupBy { x =>
      x.wettkampf }.map(x => (x._1, x._2.groupBy { x =>
        x.wettkampfdisziplin.programm }.map(x => (x._1, x._2.groupBy { x =>
          x.athlet }))))

    for{
      w <- daten.keys
    } {
      println(w.datum, w.titel)
      for {
        p <- daten(w)
      } {
        println(s"  ${p._1.toPath}")
        for {
          a <- p._2.keys
        } {
          println(s"    ${a.vorname} ${a.name} (${a.verein.map { _.name }.getOrElse("ohne Verein")})")
          for {
            d <- p._2(a)
          } {
            println(f"    ${d.wettkampfdisziplin.disziplin.name}%70s ${d.noteD}%2.3f ${d.noteE}%2.3f ${d.endnote}%2.3f")
          }
        }
      }
    }

    for(w <- listWettkaempfeView) {
      println(w)
    }
  }
}