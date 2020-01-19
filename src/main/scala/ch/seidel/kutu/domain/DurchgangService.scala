package ch.seidel.kutu.domain

import java.util.UUID

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
                    coalesce((SELECT max(ordinal)+1 FROM durchgang dd WHERE dd.wettkampf_id = zp.wettkampf_id), 0) as ordinal,
                    zp.durchgang as name,
                    zp.durchgang as title,
                    1 as durchgangType,
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

  def updateOrInsertDurchgaenge(durchgaenge: Iterable[Durchgang]) {
    def insertDurchgang(rs: Iterable[Durchgang]) = DBIO.sequence(for {
      durchgang <- rs
    } yield {
      sqlu"""
                insert into durchgang
                (wettkampf_id, title, name, durchgangtype, ordinal, planStartOffset, effectiveStartTime, effectiveEndTime)
                values (${durchgang.wettkampfId}, ${durchgang.title}, ${durchgang.name}, ${durchgang.durchgangtype},
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
      sql"""select d.id, d.wettkampf_id, d.title, d.name, d.durchgangtype, d.ordinal, d.planStartOffset, d.effectiveStartTime, d.effectiveEndTime
             from durchgang d inner join wettkampf wk on d.wettkampf_id = wk.id
             where wk.uuid=${wettkampfUUID.toString}
          """.as[Durchgang]).withPinnedSession
    }
//    database.run{(
//      sql"""select distinct wk.id, r.durchgang
//             from riege r inner join wettkampf wk on r.wettkampf_id = wk.id
//             where wk.uuid=${wettkampfUUID.toString}
//          """.as[Durchgang]).withPinnedSession
//    }
//    database.run{(
//      sql"""select r.wettkampf_id, r.durchgang
//             from durchgangstation r inner join wettkampf wk on r.wettkampf_id = wk.id
//             where wk.uuid=${wettkampfUUID.toString}
//          """.as[Durchgang]).withPinnedSession
//    }
  }
}