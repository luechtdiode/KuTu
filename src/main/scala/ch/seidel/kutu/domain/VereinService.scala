package ch.seidel.kutu.domain

import org.slf4j.LoggerFactory
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait VereinService extends DBService with VereinResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)
 
  def selectVereine: List[Verein] = {
    Await.result(database.run{
      (sql"""        select id, name, verband from verein order by name""".as[Verein]).withPinnedSession
    }, Duration.Inf).toList
  }
  
  def findVereinLike(verein: Verein): Option[Long] = {
    Await.result(database.run{sql"""
                    select max(verein.id) as maxid
                    from verein
                    where LOWER(name)=${verein.name.toLowerCase()}
         """.as[Long].withPinnedSession
         }, Duration.Inf).toList.headOption
  }
  
  def insertVerein(verein: Verein): Verein = {
    val process = for {
      candidateId <- sql"""
                    select max(verein.id) as maxid
                    from verein
                    where LOWER(name)=${verein.name.toLowerCase()}
         """.as[Long].headOption
    } yield {
      candidateId match {
        case Some(id) if (id > 0) =>
          sql"""
                    select *
                    from verein
                    where id=${id}
             """.as[Verein].map { _
              .filter { v => !v.name.equals(verein.name) || !v.verband.equals(verein.verband) }
              .map { savedverein =>
                sqlu"""
                    update verein
                    set
                      name = ${verein.name}
                    , verband = ${verein.verband}
                    where id=${id}
                     """
              }
          } >>
            sql"""
                    select id from verein where id=${id}
                """.as[Long].head
        case _ =>
          sqlu"""
                    insert into verein  (name, verband) values (${verein.name}, ${verein.verband})
               """ >>
            sql"""
                    select id from verein where id in (select max(id) from verein)
              """.as[Long].head
      }
    }

    Await.result(database.run { (process.flatten.map(Verein(_, verein.name, verein.verband))).transactionally }, Duration.Inf)
  }

  def createVerein(name: String, verband: Option[String]): Long = {
    Await.result(database.run {(
      sqlu"""       insert into verein
                    (name, verband) values (${name}, ${verband})""" >>
        sql"""
                    select id from verein
                    where id in (select max(id) from verein)
         """.as[Long].head).transactionally
    }, Duration.Inf)
  }

  def updateVerein(verein: Verein): Unit = {
    Await.result(database.run {(
      sqlu"""       update verein
                    set name = ${verein.name}, verband = ${verein.verband}
                    where id = ${verein.id}
          """).transactionally
    }, Duration.Inf)
  }
  
  def deleteVerein(vereinid: Long): Unit = {
    Await.result(database.run {(
      sqlu""" update athletregistration set athlet_id = null where vereinregistration_id in (
          select id from vereinregistration where verein_id = $vereinid
        )""" >>
      sqlu""" update vereinregistration set verein_id = null where verein_id = $vereinid""" >>
      sqlu"""       delete from wertung where athlet_id in (select id from athlet where verein=${vereinid})""" >>
      sqlu"""       delete from athlet where verein=${vereinid}""" >>
      sqlu"""       delete from verein where id=${vereinid}""").transactionally
    }, Duration.Inf)
  }

}