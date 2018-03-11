package ch.seidel.kutu.domain

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import org.slf4j.LoggerFactory
import java.sql.Date

import slick.jdbc.GetResult
import slick.jdbc.PositionedResult
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import scala.collection.JavaConverters

abstract trait DisziplinService extends DBService with WettkampfResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def readWettkampfLeafs(programmid: Long): Seq[ProgrammView]
  def readWettkampf(id: Long): Wettkampf
  
  def listDisziplinesZuDurchgang(durchgang: Set[String], wettkampf: Long, riege1: Boolean): Map[String, IndexedSeq[Disziplin]] = {
    Await.result(database.run{
      val ret = sql""" select distinct wd.disziplin_id, d.name, r.durchgang
             from wettkampfdisziplin wd
             inner join disziplin d on (wd.disziplin_id = d.id)
             inner join wertung w on (w.wettkampfdisziplin_id = wd.id)
             inner join riege r on (r.wettkampf_id = $wettkampf
                                    and r.start = d.id
                                    and r.durchgang in (#${durchgang.mkString("'","','","'")})
                                    and #${if(riege1) "r.name = w.riege" else "r.name = w.riege2"}
                                    )
             where
               w.wettkampf_id = $wettkampf
             order by
              wd.ord
       """.as[(Long, String, String)]
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
  
  def listDisziplinesZuWettkampf(wettkampfId: Long, geschlecht: Option[String] = None): List[Disziplin] = {
    Await.result(database.run{
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select distinct wd.disziplin_id, d.name
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
      sql""" select wd.id, wd.programm_id, wd.disziplin_id, printf('%s (%s)',d.name, p.name) as kurzbeschreibung, wd.masculin, wd.feminim, wd.ord
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id and
              programm_Id in #$programme
             order by
              wd.ord
         """.as[(Long, Long, Long, String, Int, Int, Int)].withPinnedSession
    }, Duration.Inf)
    .map{t => Wettkampfdisziplin(t._1, t._2, t._3, t._4, None, 0, t._5, t._6, t._7) }.toList
  }
  
  def readWettkampfDisziplinView(wettkampfDisziplinId: Long): WettkampfdisziplinView = {
    
    implicit def getWettkampfDisziplinViewResult = GetResult{r =>
      val id = r.<<[Long]
      val pgm = readProgramm(r.<<)
      WettkampfdisziplinView(id, pgm, r, r.<<[String], r.nextBytesOption(), readNotenModus(id, pgm, r.<<), r.<<, r.<<, r.<<)
    }

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