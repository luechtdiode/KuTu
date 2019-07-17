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


  def selectDurchgaenge(wettkampfUUID: UUID) = {
    Await.result(selectDurchgaengeAsync(wettkampfUUID), Duration.Inf)
  }

  def selectDurchgaengeAsync(wettkampfUUID: UUID) = {
    database.run{(
      sql"""select distinct wk.id, r.durchgang
             from riege r inner join wettkampf wk on r.wettkampf_id = wk.id
             where wk.uuid=${wettkampfUUID.toString}
          """.as[Durchgang]).withPinnedSession
    }
//    database.run{(
//      sql"""select r.wettkampf_id, r.durchgang
//             from durchgangstation r inner join wettkampf wk on r.wettkampf_id = wk.id
//             where wk.uuid=${wettkampfUUID.toString}
//          """.as[Durchgang]).withPinnedSession
//    }
  }
}