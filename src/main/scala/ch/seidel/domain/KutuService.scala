package ch.seidel.domain

import java.text.SimpleDateFormat
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Session
import scala.slick.jdbc.StaticQuery._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.PositionedResult
import scala.annotation.tailrec
import scala.slick.lifted.Query
import scala.slick.jdbc.StaticQuery
import scala.slick.jdbc.SetParameter

trait KutuService {
  lazy val database = Database.forURL(
    url = "jdbc:mysql://localhost:3306/kutu",
    driver = "com.mysql.jdbc.Driver",
    user = "kutu",
    password = "kutu")

  val sdf = new SimpleDateFormat("dd.MM.yyyy")

  def addFilter[P](query: StaticQuery[P,_], predicate: String, param: Option[P]) = {
    param map {
      case Some(p) => query + predicate + p.toString()
      case _ => query
    }
  }

  implicit def getSQLDate(date: String) = new java.sql.Date(sdf.parse(date).getTime)
  private implicit val getVereinOptionResult = GetResult(r =>  r.nextLongOption() match {
    case Some(id) => Some(Verein(r.<<[Long], r.<<[String]))
    case _        => { r.skip; r.skip; None }
  })
  private implicit val getAthletViewResult = GetResult(r =>
    AthletView(r.<<[Long], r.<<[String], r.<<[String], r.<<[Option[java.sql.Date]], r))
  private implicit val getDisziplinResult = GetResult(r =>
    Disziplin(r.<<[Long], r.<<[String]))
  private implicit val getWettkampfResult = GetResult(r =>
    Wettkampf(r.<<[Long], r.<<[java.sql.Date], r.<<[String], r.<<[Long]))
  private implicit val getWettkampfDisziplinResult = GetResult(r =>
    Wettkampfdisziplin(r.<<[Long], r.<<[Long], r.<<[Long], r.<<[String], None))

  implicit def getWettkampfDisziplinViewResultCached(r: PositionedResult)(implicit session: Session, cache: scala.collection.mutable.Map[Long, ProgrammView]) =
    WettkampfdisziplinView(r.<<[Long], readProgramm(r.<<[Long], cache), r, r.<<[String], None)
  private implicit def getResultWertungView(implicit session: Session, cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WertungView(r.<<[Long], r, r, r, r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal]))
  private implicit def getWettkampfViewResultCached(implicit session: Session, cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long], cache)))
  private implicit def getWettkampfViewResult(implicit session: Session) = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long])))
  private implicit val getProgrammRawResult = GetResult(r =>
    ProgrammRaw(r.<<[Long], r.<<[String], r.<<[Long]))

  def readProgramm(id: Long)(implicit session: Session): ProgrammView = {
    readProgramm(id, scala.collection.mutable.Map[Long, ProgrammView]())
  }
  def readProgramm(id: Long, cache: scala.collection.mutable.Map[Long, ProgrammView])(implicit session: Session): ProgrammView = {

    def wk(pid: Long) = sql"""
                    select * from kutu.programm
                    where id=$pid
                            """.as[ProgrammRaw].build().head
    @tailrec
    def rp(pgm: ProgrammRaw, pgml: List[ProgrammRaw]): List[ProgrammRaw] = {
      if (pgm.parentId > 0l) {
        val wkv = wk(pgm.parentId)
        rp(wkv, pgm :: pgml)
      }
      else {
        pgm :: pgml
      }
    }
    lazy val path = rp(wk(id), List.empty)

    cache.get(id) match {
      case Some(wk) => wk
      case _ =>
        val wk = path.head.buildPathToParent(path.tail)
        cache.put(id, wk)
        wk
    }
  }

  def readWettkampfLeafs(programmid: Long) = {
    database withSession { implicit session =>

      def children(pid: Long) =
        sql"""      select * from kutu.programm
                    where parent_id=$pid
            """.as[ProgrammRaw].build()

      def seek(pid: Long, acc: Seq[ProgrammView]): Seq[ProgrammView] = {
        val ch = children(pid)
        if(ch.isEmpty) {
          acc :+ readProgramm(pid)
        }
        else {
          (for(c <- ch) yield (seek(c.id, acc))).flatten
        }
      }

      seek(programmid, Seq.empty)
    }
  }

  def updateWertung(w: Wertung): WertungView = {
    database withTransaction { implicit session =>
      sqlu"""       UPDATE kutu.wertung
                    SET note_d=${w.noteD}, note_e=${w.noteE}, endnote=${w.endnote}
                    WHERE id=${w.id};
          """.execute

      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      sql"""
                    SELECT w.id, a.*, v.*, wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wk.*, note_d as difficulty, note_e as execution, endnote
                    FROM kutu.wertung w
                    inner join kutu.athlet a on (a.id = w.athlet_id)
                    left outer join kutu.verein v on (a.verein = v.id)
                    inner join kutu.wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join kutu.disziplin d on (d.id = wd.disziplin_id)
                    inner join kutu.programm p on (p.id = wd.programm_id)
                    inner join kutu.wettkampf wk on (wk.id = w.wettkampf_id)
                    WHERE w.id=${w.id}
       """.as[WertungView].build().head
    }
  }

  def listAthletenWertungenZuProgramm(progids: Seq[Long]) = {
    database withSession { implicit session =>
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      sql"""
                    SELECT w.id, a.*, v.*, wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wk.*, note_d as difficulty, note_e as execution, endnote
                    FROM kutu.wertung w
                    inner join kutu.athlet a on (a.id = w.athlet_id)
                    left outer join kutu.verein v on (a.verein = v.id)
                    inner join kutu.wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join kutu.disziplin d on (d.id = wd.disziplin_id)
                    inner join kutu.programm p on (p.id = wd.programm_id)
                    inner join kutu.wettkampf wk on (wk.id = w.wettkampf_id)
                    where wd.programm_id in (#${progids.mkString(",")})
       """.as[WertungView].build()
    }
  }

  def listAthletenZuWettkampf(progids: Seq[Long]) = {
    database withSession { implicit session =>
      sql"""select a.* from kutu.athlet a
                    inner join kutu.wertung w on (a.id = w.athlet_id)
                    inner join kutu.wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    where wd.programm_id in (#${progids.mkString(",")})
         """.as[AthletView].build()
    }
  }

  def createWettkampf(datum: java.sql.Date, titel: String, programmId: Set[Long], withAthlets: Option[(Long, Athlet) => Boolean] = Some({ (_, _) => true })): Wettkampf = {
    database withTransaction { implicit session =>
      val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      val programs = programmId map (p => readProgramm(p, cache))
      val heads = programs map (_.head)
      if (!heads.forall { h => h.id == heads.head.id }) {
        throw new IllegalArgumentException("Programme nicht aus der selben Gruppe können nicht in einen Wettkampf aufgenommen werden")
      }
      sqlu"""
                    insert into kutu.wettkampf
                    (datum, titel, programm_Id)
                    values (${datum}, ${titel}, ${heads.head.id})
          """.execute

      val wk = sql"""
                    select * from kutu.wettkampf
                    where id in (select max(id) from kutu.wettkampf)
                  """.as[Wettkampf].build().head

      assignAthletsToWettkampf(wk.id, programmId, withAthlets)
      wk
    }
  }

  def unassignAthletFromWettkampf(wertungId: Set[Long]) {
    database withTransaction { implicit session: Session =>
      sqlu"""
                    delete from kutu.wertung
                    where id in (#${wertungId.mkString(",")})
              """.execute
    }
  }

  def assignAthletsToWettkampf(wettkampfId: Long, programmId: Set[Long], withAthlets: Option[(Long, Athlet) => Boolean] = Some({ (_, _) => true })) {
    database withSession {implicit session: Session =>
      withAthlets match {
        case Some(f) =>
          for {
            pgm <- programmId
            a <- selectAthletes.build().filter(f(pgm, _))
            wkd <- sql"""
                    select * from kutu.wettkampfdisziplin
                    where programm_Id = $pgm
                    """.as[Wettkampfdisziplin]
          } {
            sqlu"""
                    delete from kutu.wertung where
                    athlet_Id=${a.id} and wettkampfdisziplin_Id=${wkd.id} and wettkampf_Id=${wettkampfId}
              """.execute
            sqlu"""
                    insert into kutu.wertung
                    (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
                    values (${a.id}, ${wkd.id}, ${wettkampfId}, 0, 0, 0)
              """.execute
          }
        case None =>
      }
    }
  }

  def listWettkaempfe = {
    sql"""select * from kutu.wettkampf """.as[Wettkampf]
  }
  def listWettkaempfeView(implicit session: Session) = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    sql"""select * from kutu.wettkampf """.as[WettkampfView].build()
  }

  def readWettkampf(id: Long)(implicit session: Session) = {
    sql"""select * from kutu.wettkampf where id=$id""".as[Wettkampf].build().head
  }

  def selectWertungen(wertungId: Option[Long] = None, athletId: Option[Long] = None, wettkampfId: Option[Long] = None, disziplinId: Option[Long] = None)(implicit session: Session): Seq[WertungView] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    sql"""
                    SELECT w.id, a.*, v.*, wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wk.*, note_d as difficulty, note_e as execution, endnote
                    FROM kutu.wertung w
                    inner join kutu.athlet a on (a.id = w.athlet_id)
                    left outer join kutu.verein v on (a.verein = v.id)
                    inner join kutu.wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join kutu.disziplin d on (d.id = wd.disziplin_id)
                    inner join kutu.programm p on (p.id = wd.programm_id)
                    inner join kutu.wettkampf wk on (wk.id = w.wettkampf_id)
       """.as[WertungView].build() filter { x =>
      lazy val d = (athletId match {
        case None     => true
        case Some(id) => id == x.athlet.id
      })
      lazy val dd = (wettkampfId match {
        case None     => true
        case Some(id) => id == x.wettkampf.id
      })
      lazy val ddd = (disziplinId match {
        case None     => true
        case Some(id) => id == x.wettkampfdisziplin.id
      })
      d && dd && ddd
    }
  }

  def selectAthletes = {
    implicit val getResult = GetResult(r => Athlet(r.<<, r.<<, r.<<, r.<<, r.<<))
    sql"""select * from kutu.athlet""".as[Athlet]
  }

  def selectAthletesView = {
    database withSession { implicit session =>
      sql"""select * from kutu.athlet inner join kutu.verein on (verein.id = athlet.verein)""".as[AthletView].list()
    }
  }

  def insertAthlete(athlete: Athlet) = {
    database withTransaction { implicit session =>
      def getId = sql"""
                    select max(athlet.id) as maxid
                    from kutu.athlet
                    where name=${athlete.name} and vorname=${athlete.vorname} and gebdat=${athlete.gebdat} and verein=${athlete.verein}
           """.as[Long].build().headOption

      if (athlete.id == 0) {
        getId match {
          case Some(id) =>
            sqlu"""
                    replace into kutu.athlet
                    (id, name, vorname, gebdat, verein)
                    values (${id}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.verein})
            """.execute
            athlete.id
          case None => sqlu"""
                    replace into kutu.athlet
                    (name, vorname, gebdat, verein)
                    values (${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.verein})
            """.execute
        }
      }
      else {
        sqlu"""
                    replace into kutu.athlet
                    (id, name, vorname, gebdat, verein)
                    values (${athlete.id}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.verein})
            """.execute
        athlete.id
      }
    }
  }
}