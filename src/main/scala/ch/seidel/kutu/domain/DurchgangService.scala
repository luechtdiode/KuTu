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

trait DurchgangService extends DBService with DurchgangResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def selectDurchgangstationen(wettkampfId: Long) = {
    Await.result(database.run{(
       sql"""select r.wettkampf_id, r.durchgang, r.d_Wertungsrichter1, r.e_Wertungsrichter1, r.d_Wertungsrichter2, r.e_Wertungsrichter2, d.*
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
             left outer join Wertungsrichter d1 on (r.d_Wertungsrichter1 = d.id)
             left outer join Wertungsrichter e1 on (r.e_Wertungsrichter1 = d.id)
             left outer join Wertungsrichter d2 on (r.d_Wertungsrichter2 = d.id)
             left outer join Wertungsrichter e2 on (r.e_Wertungsrichter2 = d.id)
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
}