package ch.seidel.kutu.domain

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

import ch.seidel.kutu.http.Hashing
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait RegistrationService extends DBService with RegistrationResultMapper with Hashing {

  def createRegistration(newReg: NewRegistration): Registration = {
    val similarRegistrations = selectRegistrationsLike(newReg.toRegistration)
    val vereinId = similarRegistrations.headOption.flatMap(_.vereinId)
    Await.result(database.run {
      sqlu"""
                  insert into vereinregistration
                  (wettkampf_id, verein_id, vereinname, verband, responsible_name, responsible_vorname, mobilephone, mail, secrethash, registrationtime)
                  values (${newReg.wettkampfId}, ${vereinId},
                          ${newReg.vereinname}, ${newReg.verband},
                          ${newReg.respName}, ${newReg.respVorname},
                          ${newReg.mobilephone}, ${newReg.mail}, ${hashed(newReg.secret)},
                          ${Timestamp.valueOf(LocalDateTime.now())})
              """ >>
        sql"""
                  select
                      id, wettkampf_id, verein_id, vereinname, verband,
                      responsible_name, responsible_vorname, mobilephone, mail,
                      registrationtime
                  from vereinregistration
                  where id = (select max(vr.id)
                              from vereinregistration vr
                              where vr.wettkampf_id=${newReg.wettkampfId}
                                and vr.responsible_name=${newReg.respName}
                                and vr.responsible_vorname=${newReg.respVorname}
                                and vr.vereinname=${newReg.vereinname})
         """.as[Registration].head.transactionally
    }, Duration.Inf)
  }

  def extractRegistrationId(uuid: String): Option[Long] = {
    try {
      val vereinid: Long = uuid
      Some(vereinid)
    } catch {
      case e: NumberFormatException =>
        val parts = uuid.split(":")
        if (parts.length == 2) {
          Await.result(database.run {
            sql"""     select id
                       from vereinregistration
                       where wettkampf_id in (select id from wettkampf where uuid=${parts(0)})
                         and vereinname=${parts(1)}""".as[Long]
          }, Duration.Inf).toList.headOption
        } else {
          None
        }
    }
  }

  def vereinSecretHashLookup(uuid: String): String = {
    extractRegistrationId(uuid) match {
      case Some(vereinid) =>
        Await.result(database.run {
          sql"""          select secrethash from vereinregistration where id=${vereinid}""".as[String]
        }, Duration.Inf).toList.headOption.getOrElse(hashed(uuid))
      case None => hashed(uuid)
    }
  }

  def updateRegistration(registration: Registration): Registration = {
    if (registration.id == 0L) {
      throw new IllegalArgumentException("Registration with id=0 can not be updated")
    }
    Await.result(database.run {
      sql"""
              update vereinregistration
              set verein_id=${registration.vereinId},
                  vereinname=${registration.vereinname}, verband=${registration.verband},
                  responsible_name=${registration.respName}, responsible_vorname=${registration.respVorname},
                  mobilephone=${registration.mobilephone}, mail=${registration.mail}
              where id=${registration.id}
     """.as[Long].headOption
    }, Duration.Inf)
    registration
  }

  def selectRegistrations() = {
    Await.result(database.run {
      sql"""
        select
                      id, wettkampf_id, verein_id, vereinname, verband,
                      responsible_name, responsible_vorname, mobilephone, mail,
                      registrationtime
        from vereinregistration""".as[Registration]
    }, Duration.Inf).toList
  }

  def selectRegistration(id: Long) = {
    Await.result(database.run {
      sql"""
        select
                     id, wettkampf_id, verein_id, vereinname, verband,
                     responsible_name, responsible_vorname, mobilephone, mail,
                     registrationtime
        from vereinregistration where id=${id}""".as[Registration]
    }, Duration.Inf).head
  }

  def selectRegistrationsLike(registration: Registration) = {
    Await.result(database.run {
      sql"""
        select
                     id, wettkampf_id, verein_id, vereinname, verband,
                     responsible_name, responsible_vorname, mobilephone, mail,
                     registrationtime
        from vereinregistration r
        where r.vereinname=${registration.vereinname}
                     and r.verband=${registration.verband}
                     and r.mail=${registration.mail}
                     and r.wettkampf_id <> ${registration.wettkampfId}
                     and exists (select 1 from verein v where v.id = r.verein_id)
           """.as[Registration]
    }, Duration.Inf).toList
  }

  def selectRegistrationsOfWettkampf(id: UUID) = {
    Await.result(database.run {
      sql"""
        select
                     id, wettkampf_id, verein_id, vereinname, verband,
                     responsible_name, responsible_vorname, mobilephone, mail,
                     registrationtime
        from vereinregistration
        where wettkampf_id in (select id from wettkampf where uuid = ${id.toString})
        order by registrationtime asc
       """.as[Registration]
    }, Duration.Inf).toList
  }

  def deleteRegistrations(wettkampfId: UUID): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    database.run(
      sql"""       select id
                   from vereinregistration
                   where wettkampf_id in (select id from wettkampf where uuid = ${wettkampfId.toString})""".as[Long].flatMap{ v =>
        DBIO.sequence(for(id <- v) yield (deleteRegistrationAction(id)))
      }.transactionally
    )
  }

  def deleteRegistration(registrationId: Long) {
    Await.result(database.run {
      deleteRegistrationAction(registrationId)
    }, Duration.Inf)
  }

  private def deleteRegistrationAction(registrationId: Long) = {
    sqlu"""       delete from judgeregistration_pgm where vereinregistration_id=${registrationId}""" >>
      sqlu"""       delete from judgeregistration where vereinregistration_id=${registrationId}""" >>
      sqlu"""       delete from athletregistration where vereinregistration_id=${registrationId}""" >>
      sqlu"""       delete from vereinregistration where id=${registrationId}"""
  }
}