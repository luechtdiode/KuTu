package ch.seidel.domain

import java.text.SimpleDateFormat
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Session
import scala.slick.jdbc.StaticQuery._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.PositionedResult
import scala.annotation.tailrec

trait KutuService {
  lazy val database = Database.forURL(
    url = "jdbc:mysql://localhost:3306/kutu",
    driver = "com.mysql.jdbc.Driver",
    user = "kutu",
    password = "kutu")

  val sdf = new SimpleDateFormat("dd.MM.yyyy")
  implicit def getSQLDate(date: String) = new java.sql.Date(sdf.parse(date).getTime)

  def readProgramm(id: Long)(implicit session: Session): ProgrammView = {
    readProgramm(id, scala.collection.mutable.Map[Long, ProgrammView]())
  }
  def readProgramm(id: Long, cache: scala.collection.mutable.Map[Long, ProgrammView])(implicit session: Session): ProgrammView = {
    implicit val getResult = GetResult(r => ProgrammRaw(r.<<[Long], r.<<[String], r.<<[Long]))
    def wk(pid: Long) = sql"""
                              select * from kutu.programm
                              where id=$pid
                            """.as[ProgrammRaw].buildColl().head
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

  def createWettkampf(datum: java.sql.Date, titel: String, programmId: Set[Long], withAthlets: Option[(Long, Athlet) => Boolean] = Some({ (_, _) => true })): Wettkampf = {
    database withTransaction { implicit session =>
      implicit val getResult = GetResult(r => Wettkampf(r.<<[Long], r.<<[java.sql.Date], r.<<[String], r.<<[Long]))
      implicit val getResultWKD = GetResult(r => Wettkampfdisziplin(r.<<[Long], r.<<[Long], r.<<[Long], r.<<[String], None))
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

  def assignAthletsToWettkampf(wettkampfId: Long, programmId: Set[Long], withAthlets: Option[(Long, Athlet) => Boolean] = Some({ (_, _) => true }))(implicit session: Session) {
      implicit val getResultWKD = GetResult(r => Wettkampfdisziplin(r.<<[Long], r.<<[Long], r.<<[Long], r.<<[String], None))
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

  def listWettkaempfe = {
    implicit val getResult = GetResult(r => Wettkampf(r.<<[Long], r.<<[java.sql.Date], r.<<[String], r.<<[Long]))
    sql"""select * from kutu.wettkampf """.as[Wettkampf]
  }
  def listWettkaempfeView(implicit session: Session) = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    implicit val getResult = GetResult(r => WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long], cache)))
    sql"""select * from kutu.wettkampf """.as[WettkampfView].build()
  }

  def readWettkampf(id: Long)(implicit session: Session) = {
    implicit val getResult = GetResult(r => Wettkampf(r.<<[Long], r.<<[java.sql.Date], r.<<[String], r.<<[Long]))
    sql"""select * from kutu.wettkampf where id=$id""".as[Wettkampf].build().head
  }

  def selectWertungen(wertungId: Option[Long] = None, athletId: Option[Long] = None, wettkampfId: Option[Long] = None, disziplinId: Option[Long] = None)(implicit session: Session): Seq[WertungView] = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val filter = List(wertungId, athletId, wettkampfId, disziplinId) map {case None => "%" case Some(id) => id.toString()}
    def resolveVerein(r: PositionedResult): Option[Verein] = r.nextLongOption() match {
      case Some(id) => Some(Verein(r.<<[Long], r.<<[String]))
      case _        => { r.skip; r.skip; None }
    }
    def resolveAthletView(r: PositionedResult) =
      AthletView(r.<<[Long], r.<<[String], r.<<[String], r.<<[Option[java.sql.Date]], resolveVerein(r))

    def resolveDisziplin(r: PositionedResult) =
      Disziplin(r.<<[Long], r.<<[String])

    def resolveWettkampf(r: PositionedResult) =
      Wettkampf(r.<<[Long], r.<<[java.sql.Date], r.<<[String], r.<<[Long])

    def resolveWettkampfDisziplinView(r: PositionedResult) =
      WettkampfdisziplinView(r.<<[Long], readProgramm(r.<<[Long]), resolveDisziplin(r), r.<<[String], None)

    implicit val getResultWertungView = GetResult(r =>
      WertungView(r.<<[Long],
        resolveAthletView(r),
        resolveWettkampfDisziplinView(r),
        resolveWettkampf(r),
        r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal]))

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

  def selectAthletes(implicit session: Session) = {
    implicit val getResult = GetResult(r => Athlet(r.<<, r.<<, r.<<, r.<<, r.<<))
    sql"""select * from kutu.athlet""".as[Athlet]
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