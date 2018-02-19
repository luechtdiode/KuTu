package ch.seidel.kutu.domain

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import org.slf4j.LoggerFactory
import java.sql.Date

import slick.jdbc.GetResult
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import scala.collection.JavaConverters

trait RiegenService extends DBService with RiegenResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def renameRiege(wettkampfid: Long, oldname: String, newname: String): Riege = {
    val existing = Await.result(database.run{
        (sqlu"""
                DELETE from riege where name=${newname.trim} and wettkampf_id=${wettkampfid}
        """ >>
        sql"""select r.wettkampf_id, r.name, r.durchgang, r.start
             from riege r
             where wettkampf_id=$wettkampfid and name=${oldname}
          """.as[RiegeRaw]).transactionally
    }, Duration.Inf)
    
    Await.result(database.run{
      val riegeModifierAction = if(existing.isEmpty) {
        sqlu"""
                insert into riege
                       (name, wettkampf_id)
                VALUES (${newname.trim}, ${wettkampfid})
        """
      }
      else {
        sqlu"""
                  update riege
                  set name=${newname.trim}
                  where
                  wettkampf_id=${wettkampfid} and name=${oldname}
          """
      }
      (riegeModifierAction >>
        sqlu"""   UPDATE wertung
                  SET riege=${newname}
                  WHERE wettkampf_id=${wettkampfid} and riege=${oldname}
            """ >>
        sqlu"""   UPDATE wertung
                  SET riege2=${newname}
                  WHERE wettkampf_id=${wettkampfid} and riege2=${oldname}
            """ >>
        sql"""   select r.name as riegenname, r.durchgang, d.*
                 from riege r
                 left outer join disziplin d on (r.start = d.id)
                 where r.wettkampf_id=${wettkampfid} and r.name=${newname}
           """.as[Riege].headOption).transactionally
    }, Duration.Inf).getOrElse(Riege(newname, None, None))
  }

  def cleanAllRiegenDurchgaenge(wettkampfid: Long) {
    Await.result(database.run{(
      sqlu"""
                delete from riege where
                wettkampf_id=${wettkampfid}
        """ >>
      sqlu"""
                delete from durchgangstation where
                wettkampf_id=${wettkampfid}
        """ >>
      sqlu"""   UPDATE wertung
                SET riege=NULL
                  , riege2=NULL
                WHERE wettkampf_id=${wettkampfid}
          """).transactionally
    }, Duration.Inf)
  }

  def updateOrinsertRiegen(riegen: Iterable[RiegeRaw]) {
    val process = DBIO.sequence(for {
      riege <- riegen
    } yield {
      sqlu"""
                delete from riege where
                wettkampf_id=${riege.wettkampfId} and name=${riege.r}
        """>>  
      sqlu"""
                insert into riege
                (wettkampf_Id, name, durchgang, start)
                values (${riege.wettkampfId}, ${riege.r}, ${riege.durchgang}, ${riege.start})
        """      
    })    
    Await.result(database.run{process.transactionally}, Duration.Inf)
  }
  
  def updateOrinsertRiege(riege: RiegeRaw): Riege = {
    Await.result(database.run{(
      sqlu"""
                delete from riege where
                wettkampf_id=${riege.wettkampfId} and name=${riege.r}
        """ >>
      sqlu"""
                insert into riege
                (wettkampf_Id, name, durchgang, start)
                values (${riege.wettkampfId}, ${riege.r}, ${riege.durchgang}, ${riege.start})
        """ >>
       sql"""select r.name as riegenname, r.durchgang, d.*
             from riege r
             left outer join disziplin d on (r.start = d.id)
             where r.wettkampf_id=${riege.wettkampfId} and r.name=${riege.r}
          """.as[Riege]).transactionally
    }, Duration.Inf).head
  }

  def deleteRiege(wettkampfid: Long, oldname: String) {
    Await.result(database.run{(
      sqlu"""
                DELETE from riege where name=${oldname.trim} and wettkampf_id=${wettkampfid}
          """ >>
      sqlu"""   UPDATE wertung
                SET riege=null
                WHERE wettkampf_id=${wettkampfid} and riege=${oldname}
          """ >>
      sqlu"""   UPDATE wertung
                SET riege2=null
                WHERE wettkampf_id=${wettkampfid} and riege2=${oldname}
          """).transactionally
    }, Duration.Inf)
  }

  def insertRiegenWertungen(riege: RiegeRaw, wertungen: Seq[Wertung]) {
    Await.result(database.run{(
      sqlu"""
                  replace into riege
                  (wettkampf_Id, name, durchgang, start)
                  values (${riege.wettkampfId}, ${riege.r}, ${riege.durchgang}, ${riege.start})
          """ >>
      DBIO.sequence(for(w <- wertungen) yield {
        sqlu"""     UPDATE wertung
                    SET riege=${riege.r}
                    WHERE id=${w.id}
          """
      })).transactionally
    }, Duration.Inf)
  }

  def selectRiegenRaw(wettkampfId: Long) = {
    Await.result(database.run{(
       sql"""select r.wettkampf_id, r.name, r.durchgang, r.start
             from riege r
             where wettkampf_id=$wettkampfId
          """.as[RiegeRaw]).withPinnedSession
    }, Duration.Inf).toList
  }
  
  def selectRiegen(wettkampfId: Long) = {
    Await.result(database.run{(
       sql"""select r.name as riegenname, r.durchgang, d.*
             from riege r
             left outer join disziplin d on (r.start = d.id)
             where wettkampf_id=$wettkampfId
          """.as[Riege]).withPinnedSession
    }, Duration.Inf).toList
  }
  
}