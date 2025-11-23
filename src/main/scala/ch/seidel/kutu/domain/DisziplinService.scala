package ch.seidel.kutu.domain

import ch.seidel.kutu.calc.ScoreCalcTemplate
import org.slf4j.LoggerFactory
import slick.jdbc.GetResult
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

abstract trait DisziplinService extends DBService with WettkampfResultMapper with DisziplinResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def readWettkampfLeafs(programmid: Long): Seq[ProgrammView]
  def readWettkampf(id: Long): Wettkampf
  
  def listDisziplinesZuDurchgang(durchgang: Set[String], wettkampf: Long, riege1: Boolean): Map[String, IndexedSeq[Disziplin]] = {
    Await.result(database.run{
      val ret = if riege1 then sql"""
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
             where
               r.wettkampf_id = $wettkampf
               and (not exists(select 1 from wertung w where w.wettkampf_id = r.wettkampf_id and (w.riege2 <> ''))
                or not exists(select 1 from wertung w where w.wettkampf_id = r.wettkampf_id and (w.riege = r.name or w.riege2 = r.name)))
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
       ret.withPinnedSession.map{_.map{tupel => (Disziplin(tupel._1, tupel._2), tupel._3)}.distinct.groupBy(_._2).map(x => x._1 -> x._2.map(_._1))}
    }, Duration.Inf)
  }

  def listDisziplinIdsZuWettkampf(wettkampfId: Long): List[Long] = {
    Await.result(database.run{
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select id from wettkampfdisziplin where programm_Id in #$programme""".as[Long].withPinnedSession
    }, Duration.Inf).toList.distinct
  }
  def listDisziplinZuWettkampf(wettkampf: Wettkampf): Future[Vector[Disziplin]] = {
    database.run{
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select distinct d.id, d.name, wd.ord from disziplin d inner join wettkampfdisziplin wd on d.id = wd.disziplin_id
             where wd.programm_Id in #$programme order by wd.ord""".as[Disziplin].withPinnedSession.map(_.distinct)
    }
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
    }, Duration.Inf).toList.distinct
  }
  
  def listWettkampfDisziplines(wettkampfId: Long): List[Wettkampfdisziplin] = {
    Await.result(database.run{
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select wd.id, wd.programm_id, wd.disziplin_id, d.name as diszname, p.name as progname, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id and
              programm_Id in #$programme
             order by
              wd.ord
         """.as[(Long, Long, Long, String, String, Int, Int, Int, Int, Int, Int, Int, Int)].withPinnedSession
    }, Duration.Inf)//
    .map{t => Wettkampfdisziplin(t._1, t._2, t._3, s"${t._4} (${t._5})", None, 0, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13) }.toList
  }

  implicit def getWettkampfDisziplinViewResult(implicit wkId: Long, cache2: scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]): GetResult[WettkampfdisziplinView] = GetResult{ r =>
    val id = r.<<[Long]
    val pgm = readProgramm(r.<<)
    val d: Disziplin = Disziplin(r.<<[Long], r.<<[String])
    WettkampfdisziplinView(id, pgm, d, r.<<[String], r.nextBytesOption(), readNotenModus(wkId, id, d, pgm, r.<<, cache2), r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<)
  }

  def listWettkampfDisziplineViews(wettkampf: Wettkampf): List[WettkampfdisziplinView] = {
    Await.result(database.run{
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      implicit val cache2 = scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]()
      implicit val wkId = wettkampf.id
      sql""" select wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet
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

  def readWettkampfDisziplinView(wkId: Long, wettkampfDisziplinId: Long, cache2: scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]): WettkampfdisziplinView = {
    val wd = Await.result(database.run{
      sql""" select wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet
             from wettkampfdisziplin wd, disziplin d
             where
              wd.disziplin_id = d.id
              and wd.id = $wettkampfDisziplinId
             order by
              wd.ord
         """.as[WettkampfdisziplinView](getWettkampfDisziplinViewResult(wkId, cache2)).withPinnedSession
    }, Duration.Inf).toList
    wd.head    
  }
  
}