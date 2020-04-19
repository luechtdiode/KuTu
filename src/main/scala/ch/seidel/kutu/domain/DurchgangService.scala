package ch.seidel.kutu.domain

import java.sql.{Date, Timestamp}
import java.util.UUID

import ch.seidel.kutu.akka.{DurchgangFinished, DurchgangStarted}
import org.slf4j.LoggerFactory
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait DurchgangService extends DBService with DurchgangResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def selectDurchgangstationen(wettkampfId: Long) = {
    Await.result(database.run{(
       sql"""select r.wettkampf_id, r.durchgang, r.d_kampfrichter1, r.e_kampfrichter1, r.d_kampfrichter2, r.e_kampfrichter2, d.*
             from durchgangstation r
             inner join disziplin d on (r.start = d.id)
             where wettkampf_id=$wettkampfId
          """.as[Durchgangstation]).withPinnedSession
    }, Duration.Inf).toList
  }
  
  def selectDurchgangstationenView(wettkampfId: Long) = {
    Await.result(database.run{(
       sql"""select r.wettkampf_id, r.durchgang, d1.*, e1.*, d2.*, e2.*, d.*
             from durchgangstation r
             left outer join kampfrichter d1 on (r.d_kampfrichter1 = d.id)
             left outer join kampfrichter e1 on (r.e_kampfrichter1 = d.id)
             left outer join kampfrichter d2 on (r.d_kampfrichter2 = d.id)
             left outer join kampfrichter e2 on (r.e_kampfrichter2 = d.id)
             inner join disziplin d on (r.start = d.id)
             where wettkampf_id=$wettkampfId
          """.as[DurchgangstationView]).withPinnedSession
    }, Duration.Inf).toList
  }
  
  def renameDurchgang(wettkampfid: Long, oldname: String, newname: String) = {
    Await.result(database.run{
        (sqlu"""
                update riege
                set durchgang=${newname.trim}
                where
                wettkampf_id=${wettkampfid} and durchgang=${oldname}
        """ >>  // prevent constraints-violations
          sqlu"""
                delete from durchgang
                where
                wettkampf_id=${wettkampfid} and name=${newname.trim}
        """  >>
          sqlu"""
                update durchgang
                set name=${newname.trim}
                where
                wettkampf_id=${wettkampfid} and name=${oldname}
        """  >>
        sqlu"""
                insert into durchgang (wettkampf_id, title, name, durchgangtype, ordinal, planStartOffset)
                SELECT
                    zp.wettkampf_id,
                    zp.durchgang as title,
                    zp.durchgang as name,
                    1 as durchgangType,
                    coalesce((SELECT max(ordinal)+1 FROM durchgang dd WHERE dd.wettkampf_id = zp.wettkampf_id), 0) as ordinal,
                    0 as planStartOffset
                FROM
                    zeitplan zp
                WHERE
                    zp.wettkampf_id = $wettkampfid
                    AND NOT EXISTS (SELECT 1 FROM durchgang dd WHERE dd.wettkampf_id = zp.wettkampf_id and dd.name = zp.durchgang)
        """ >>  // prevent constraints-violations
        sqlu""" 
                delete from durchgangstation
                where
                wettkampf_id=${wettkampfid} and durchgang=${newname.trim}
        """  >>
        sqlu"""
                update durchgangstation
                set durchgang=${newname.trim}
                where
                wettkampf_id=${wettkampfid} and durchgang=${oldname}
        """).transactionally
    }, Duration.Inf)
  }

  def storeDurchgangStarted(started: DurchgangStarted): Unit = {
    if (started.time > 0) {
      val t = new Timestamp(started.time)
      Await.result(database.run {
        sqlu"""
                update durchgang
                set effectiveStartTime=$t
                where
                wettkampf_id = (select id from wettkampf wk where wk.uuid = ${started.wettkampfUUID})
                and name = ${started.durchgang}
                and (effectiveStartTime is null
                  or effectiveStartTime = ${new Date(0)}
                  or $t < effectiveStartTime)
        """ >>
        sqlu"""
                update durchgang
                set effectiveEndTime=null
                where
                wettkampf_id = (select id from wettkampf wk where wk.uuid = ${started.wettkampfUUID})
                and name = ${started.durchgang}
        """.transactionally
      }, Duration.Inf)
    }
  }

  def storeDurchgangFinished(finished: DurchgangFinished): Unit = {
    if (finished.time > 0) {
      val t = new Timestamp(finished.time)
      Await.result(database.run {
        sqlu"""
                update durchgang
                set effectiveEndTime=$t
                where
                wettkampf_id = (select id from wettkampf wk where wk.uuid = ${finished.wettkampfUUID})
                and name = ${finished.durchgang}
                and (effectiveEndTime is null
                  or $t > effectiveEndTime)
        """.transactionally
      }, Duration.Inf)
    }
  }

  def updateOrInsertDurchgaenge(durchgaenge: Iterable[Durchgang]) {
    def insertDurchgang(rs: Iterable[Durchgang]) = DBIO.sequence(for {
      durchgang <- rs
    } yield {
      sqlu"""
                insert into durchgang
                (wettkampf_id, title, name, durchgangtype, ordinal, planStartOffset, effectiveStartTime, effectiveEndTime)
                values (${durchgang.wettkampfId}, ${durchgang.title}, ${durchgang.name}, ${durchgang.durchgangtype.code},
                ${durchgang.ordinal}, ${durchgang.planStartOffset}, ${durchgang.effectiveStartTime}, ${durchgang.effectiveEndTime})
        """
    })

    val process = DBIO.sequence(for {
      (wettkampfid, planTime) <- durchgaenge.groupBy(_.wettkampfId)
    } yield {
      sqlu"""
                delete from durchgang where
                wettkampf_id=${wettkampfid}
        """>>
        insertDurchgang(planTime)
    })

    Await.result(database.run{process.transactionally}, Duration.Inf)
  }

  def selectDurchgaenge(wettkampfUUID: UUID) = {
    Await.result(selectDurchgaengeAsync(wettkampfUUID), Duration.Inf)
  }

  def selectDurchgaengeAsync(wettkampfUUID: UUID) = {
    database.run{(
      sql"""select
               d.id, d.wettkampf_id, d.title, d.name, d.durchgangtype, d.ordinal,
               d.planStartOffset, d.effectiveStartTime, d.effectiveEndTime,
               zp.einturnen, zp.geraet, zp.total
             from durchgang d
               inner join wettkampf wk on d.wettkampf_id = wk.id
               inner join zeitplan zp on d.wettkampf_id = zp.wettkampf_id and d.name = zp.durchgang
             where wk.uuid=${wettkampfUUID.toString}
          """.as[Durchgang]).withPinnedSession
    }
  }
}