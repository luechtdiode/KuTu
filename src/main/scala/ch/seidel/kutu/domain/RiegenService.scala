package ch.seidel.kutu.domain

import org.slf4j.LoggerFactory
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait RiegenService extends DBService with RiegenResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def renameRiege(wettkampfid: Long, oldname: String, newname: String): Riege = {
    val existing = Await.result(database.run{
        (if (newname.trim == oldname) {
          sql"""select r.wettkampf_id, r.name, r.durchgang, r.start
             from riege r
             where wettkampf_id=$wettkampfid and name=${oldname}
          """.as[RiegeRaw]
        } else {
          sqlu"""
                DELETE from riege where name=${newname.trim} and wettkampf_id=${wettkampfid}
                """  >>
          sql"""select r.wettkampf_id, r.name, r.durchgang, r.start
             from riege r
             where wettkampf_id=$wettkampfid and name=${oldname}
          """.as[RiegeRaw]}).transactionally
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
    def insertRiegen(rs: Iterable[RiegeRaw]) = DBIO.sequence(for {
        riege <- rs
      } yield {
      sqlu"""
                insert into riege
                (wettkampf_Id, name, durchgang, start)
                values (${riege.wettkampfId}, ${riege.r}, ${riege.durchgang}, ${riege.start})
        """      
      })

    val process = DBIO.sequence(for {
      (wettkampfid, riegen) <- riegen.groupBy(_.wettkampfId).toIterable
    } yield {
      sqlu"""
                delete from riege where
                wettkampf_id=${wettkampfid}
        """>>
      insertRiegen(riegen)
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

  def findAndStoreMatchingRiege(riege: RiegeRaw): RiegeRaw = {
    val existingRiegen = selectRiegenRaw(riege.wettkampfId)
    val riegenParts = riege.r.split(",")

    val (matchingRiege, matchscore) = existingRiegen.map { er =>
      (er, er.r.split(",").zip(riegenParts).filter{ case (existing, newpart) =>
        existing.equalsIgnoreCase(newpart)
      }.length)
    }.sortBy(t => t._2).reverse.headOption.getOrElse((riege, 0))

    if (matchscore > 2) {
      matchingRiege
    } else if (matchscore > 0) {
      updateOrinsertRiege(riege.copy(durchgang = matchingRiege.durchgang, start = matchingRiege.start))
        .toRaw(riege.wettkampfId)
    } else {
      updateOrinsertRiege(riege).toRaw(riege.wettkampfId)
    }
  }

  def cleanUnusedRiegen(wettkampfid: Long): Unit = {
    Await.result(database.run{(
      sqlu"""
                DELETE from riege where wettkampf_id=${wettkampfid} and not exists(
                  select 1 from wertung w where w.riege = riege.name or w.riege2 = riege.name and w.wettkampf_id = riege.wettkampf_id
                )
          """).transactionally
    }, Duration.Inf)
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