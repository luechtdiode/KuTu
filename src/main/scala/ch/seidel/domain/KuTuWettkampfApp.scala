package ch.seidel.domain

import java.text.SimpleDateFormat

import scala.slick.jdbc.GetResult
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Session
import scala.slick.jdbc.StaticQuery._

case class Verein(id: Long, name: String)
case class Athlet(id: Long, name: String, vorname: String, gebdat: Option[java.sql.Date], verein: Option[Long])
case class Disziplin(id: Long, name: String)
case class Programm(id: Long, name: String, parentId: Long)
case class Wettkampf(id: Long, datum: java.sql.Date, titel: String, programmId: Long)
case class Wettkampfdisziplin(id: Long, programmId: Long, disziplinId: Long, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob])
case class Wertung(id: Long, athletId: Long, wettkampfdisziplinId: Long, wettkampfId: Long, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal)

trait KuTuQuery {
  lazy val database = Database.forURL(
      url="jdbc:mysql://localhost:3306/kutu",
      driver = "com.mysql.jdbc.Driver",
      user="kutu",
      password="kutu")

  def createWettkampf(datum: java.sql.Date, titel: String, programmId: Long)(implicit session: Session): Wettkampf = {
    sqlu"""insert into kutu.wettkampf (datum, titel, programm_Id) values (${datum}, ${titel}, ${programmId})""".execute
    implicit val getResult = GetResult(r => Wettkampf(r.<<[Long], r.<<[java.sql.Date], r.<<[String], r.<<[Long]))
    implicit val getResultWKD = GetResult(r => Wettkampfdisziplin(r.<<[Long], r.<<[Long], r.<<[Long], r.<<[String], None))
    val wkid = sql"""select * from kutu.wettkampf where id in (select max(id) from kutu.wettkampf)""".as[Wettkampf].buildColl().head
    for{
      a <- selectAthletes
      wkd <- sql"""select * from kutu.wettkampfdisziplin where programm_Id=${wkid.programmId}""".as[Wettkampfdisziplin]
    }{
      sqlu"""insert into kutu.wertung (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote) values (${a.id}, ${wkd.id}, ${wkid.id}, 0, 0, 0)""".execute
    }
    wkid
  }

  def selectWertungen(athletId:Option[Long] = None, wettkampfId:Option[Long] = None, disziplinId:Option[Long] = None)(implicit session: Session) = {
    /** GetResult implicit for fetching WertungRow objects using plain SQL queries */
    implicit val getResult = GetResult(r => Wertung(r.<<[Long], r.<<[Long], r.<<[Long], r.<<[Long], r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal]))
    sql"""select * from kutu.wertung """.as[Wertung].buildColl.filter{x =>
      lazy val d = (athletId match {
        case None => true
        case Some(id) => id == x.athletId
      })
      lazy val dd = (wettkampfId match {
        case None => true
        case Some(id) => id == x.wettkampfId
      })
      lazy val ddd = (disziplinId match {
        case None => true
        case Some(id) => id == x.wettkampfdisziplinId
      })
      d && dd && ddd
    }
  }

  def selectAthletes(implicit session: Session) = {
    implicit val getResult = GetResult(r => Athlet(r.<<, r.<<, r.<<, r.<<, r.<<))
    sql"""select * from kutu.athlet""".as[Athlet]
  }

  def insertAthlete(athlete: Athlet)(implicit session: Session) = {
    sqlu"""insert into kutu.athlet (name, vorname, gebdat, verein) values (${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.verein})""".execute
    sql"""select max(athlet.id) as maxid from kutu.athlet where true""".as[Long].buildColl().head
  }
}

object KuTuWettkampfApp extends App with KuTuQuery {
  // Open a database connection
  val sdf = new SimpleDateFormat("dd.MM.yyyy")
  implicit def getSQLDate(date: String) = new java.sql.Date(sdf.parse(date).getTime)

  database withSession { implicit session =>
    val verein = 1l
    val programmEP = 12L
    val id = insertAthlete(Athlet(0, "Mandume", "Lucien", Some("15.02.2005"), Some(verein)))
    println(id)

    for(a <- selectAthletes) {
      println(a)
    }

    println(createWettkampf("15.02.2005", "Testwettkampf", programmEP))
    for(w <- selectWertungen(athletId=Some(id))) {
      println(w)
    }
  }
}