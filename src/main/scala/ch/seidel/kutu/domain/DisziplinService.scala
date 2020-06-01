package ch.seidel.kutu.domain

import org.slf4j.LoggerFactory
import slick.jdbc.GetResult
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

abstract trait DisziplinService extends DBService with WettkampfResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def readWettkampfLeafs(programmid: Long): Seq[ProgrammView]
  def readWettkampf(id: Long): Wettkampf
  
  def listDisziplinesZuDurchgang(durchgang: Set[String], wettkampf: Long, riege1: Boolean): Map[String, IndexedSeq[Disziplin]] = {
    Await.result(database.run{
      val ret = if (riege1) sql"""
             select distinct d.id, d.name, r.durchgang, wd.ord
             from wettkampfdisziplin wd
             inner join disziplin d on (wd.disziplin_id = d.id)
             inner join wertung w on (w.wettkampfdisziplin_id = wd.id)
             inner join riege r on (
               r.wettkampf_id = $wettkampf
               and r.start = d.id
               and r.durchgang in (#${durchgang.mkString("'","','","'")})
               and r.name = w.riege
               and r.wettkampf_id = w.wettkampf_id
             )
             where
               w.wettkampf_id = $wettkampf
             union all select distinct d.id, d.name, r.durchgang, wd.ord
             from wettkampfdisziplin wd
             inner join disziplin d on (wd.disziplin_id = d.id)
             inner join riege r on (
               r.start = d.id
               and r.durchgang in (#${durchgang.mkString("'", "','", "'")})
             )
             inner join wettkampf wk on (wk.id = r.wettkampf_id)
             inner join programm pg on (pg.parent_id = wk.programm_id and pg.id = wd.programm_id)
             left outer join wertung w on (
               w.wettkampf_id = r.wettkampf_id
               and (w.riege = r.name or w.riege2 = r.name)
             )
             where
               r.wettkampf_id = $wettkampf
               and w.id is null
             order by
               4 --wd.ord
       """.as[(Long, String, String, Int)]
      else sql"""
             select distinct d.id, d.name, r.durchgang, wd.ord
             from wettkampfdisziplin wd
             inner join disziplin d on (wd.disziplin_id = d.id)
             inner join wertung w on (w.wettkampfdisziplin_id = wd.id)
             inner join riege r on (
               r.wettkampf_id = $wettkampf
               and r.start = d.id
               and r.durchgang in (#${durchgang.mkString("'","','","'")})
               and r.name = w.riege2
               and r.wettkampf_id = w.wettkampf_id
             )
             where
               w.wettkampf_id = $wettkampf
             order by
              wd.ord
       """.as[(Long, String, String, Int)]
       ret.withPinnedSession.map{_.map{tupel => (Disziplin(tupel._1, tupel._2), tupel._3)}.groupBy(_._2).map(x => x._1 -> x._2.map(_._1))}
    }, Duration.Inf)
  }

  def listDisziplinIdsZuWettkampf(wettkampfId: Long): List[Long] = {
    Await.result(database.run{
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select id from wettkampfdisziplin where programm_Id in #$programme""".as[Long].withPinnedSession
    }, Duration.Inf).toList
  }
  def listDisziplinZuWettkampf(wettkampf: Wettkampf): Future[Vector[Disziplin]] = {
    database.run{
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select distinct d.id, d.name from disziplin d inner join wettkampfdisziplin wd on d.id = wd.disziplin_id
             where wd.programm_Id in #$programme""".as[Disziplin].withPinnedSession
    }
  }
  
  def listDisziplinesZuProgramm(programmId: Long, geschlecht: Option[String] = None): List[Disziplin] = {
    Await.result(database.run{
      sql""" select distinct d.id, d.name, wd.ord
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id
              #${
                geschlecht match {
                  case Some("M") => "and wd.masculin = 1"
                  case Some("W") => "and wd.feminim = 1"
                  case _ => ""
                }
              }
              and programm_id = $programmId
             order by
              wd.ord
             """.as[Disziplin].withPinnedSession
    }, Duration.Inf).toList
  }
  
  def listDisziplinesZuWettkampf(wettkampfId: Long, geschlecht: Option[String] = None): List[Disziplin] = {
    Await.result(database.run{
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select distinct d.id, d.name, wd.ord
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id
              #${
                geschlecht match {
                  case Some("M") => "and wd.masculin = 1"
                  case Some("W") => "and wd.feminim = 1"
                  case _ => ""
                }
              }
              and programm_id in #$programme
             order by
              wd.ord
             """.as[Disziplin].withPinnedSession
    }, Duration.Inf).toList
  }
  
  def listWettkampfDisziplines(wettkampfId: Long): List[Wettkampfdisziplin] = {
    Await.result(database.run{
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select wd.id, wd.programm_id, wd.disziplin_id, d.name as diszname, p.name as progname, wd.masculin, wd.feminim, wd.ord
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id and
              programm_Id in #$programme
             order by
              wd.ord
         """.as[(Long, Long, Long, String, String, Int, Int, Int)].withPinnedSession
    }, Duration.Inf)//
    .map{t => Wettkampfdisziplin(t._1, t._2, t._3, s"${t._4} (${t._5})", None, 0, t._6, t._7, t._8) }.toList
  }

  implicit def getWettkampfDisziplinViewResult = GetResult{r =>
    val id = r.<<[Long]
    val pgm = readProgramm(r.<<)
    WettkampfdisziplinView(id, pgm, r, r.<<[String], r.nextBytesOption(), readNotenModus(id, pgm, r.<<), r.<<, r.<<, r.<<)
  }

  def listWettkampfDisziplineViews(wettkampf: Wettkampf): List[WettkampfdisziplinView] = {
    Await.result(database.run{
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord
             from wettkampfdisziplin wd, disziplin d, programm p
             where
                  wd.disziplin_id = d.id
              and wd.programm_id = p.id
              and programm_Id in #$programme
             order by
              wd.ord
         """.as[WettkampfdisziplinView].withPinnedSession
    }, Duration.Inf).toList
  }

  def readWettkampfDisziplinView(wettkampfDisziplinId: Long): WettkampfdisziplinView = {
    val wd = Await.result(database.run{
      sql""" select wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord
             from wettkampfdisziplin wd, disziplin d
             where
              wd.disziplin_id = d.id
              and wd.id = $wettkampfDisziplinId
             order by
              wd.ord
         """.as[WettkampfdisziplinView].withPinnedSession
    }, Duration.Inf).toList
    wd.head    
  }
  
}