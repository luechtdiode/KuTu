package ch.seidel.kutu.domain

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import ch.seidel.kutu.http.{EnrichedJson, Hashing}
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

  private def removeVereinAndAthletIds(vereinId: Long) = {
    sqlu""" update athletregistration set athlet_id = null where vereinregistration_id in (
          select id from vereinregistration where verein_id = $vereinId
        )""" >>
    sqlu""" update vereinregistration set verein_id = null where verein_id = $vereinId"""
  }

  // AthletRegistration

  def createAthletRegistration(newReg: AthletRegistration): AthletRegistration = {
    val athletId: Option[Long] = if (newReg.athletId.isDefined) {
      newReg.athletId
    } else {
      selectAthletRegistrationsLike(newReg).headOption.flatMap(_.athletId)
    }
    val nomralizedAthlet = newReg.toAthlet

    Await.result(database.run {
      sqlu"""
                  insert into athletregistration
                  (vereinregistration_id, athlet_id, geschlecht, name, vorname, gebdat, program_id, registrationtime)
                  values (${newReg.vereinregistrationId}, ${athletId},
                          ${nomralizedAthlet.geschlecht}, ${nomralizedAthlet.name},
                          ${nomralizedAthlet.vorname}, ${nomralizedAthlet.gebdat},
                          ${newReg.programId},
                          ${Timestamp.valueOf(LocalDateTime.now())})
              """ >>
        sql"""
                  select
                      id, vereinregistration_id,
                      athlet_id, geschlecht, name, vorname, gebdat,
                      program_id, registrationtime
                  from athletregistration
                  where id = (select max(ar.id)
                              from athletregistration ar
                              where ar.vereinregistration_id=${newReg.vereinregistrationId}
                                and ar.geschlecht=${nomralizedAthlet.geschlecht}
                                and ar.name=${nomralizedAthlet.name}
                                and ar.vorname=${nomralizedAthlet.vorname}
                                )
         """.as[AthletRegistration].head.transactionally
    }, Duration.Inf)
  }


  def selectAthletRegistrationsLike(registration: AthletRegistration) = {
    val nomralizedAthlet = registration.toAthlet
    Await.result(database.run {
      sql"""
                  select
                      ar.id, ar.vereinregistration_id,
                      ar.athlet_id, ar.geschlecht, ar.name, ar.vorname, ar.gebdat,
                      ar.program_id, ar.registrationtime
                  from athletregistration ar
                  inner join vereinregistration vr on ar.vereinregistration_id = vr.id
                  where ar.geschlecht=${nomralizedAthlet.geschlecht}
                    and ar.name=${nomralizedAthlet.name}
                    and ar.vorname=${nomralizedAthlet.vorname}
                    and ar.vereinregistration_id <> ${registration.vereinregistrationId}
                    and exists (select 1 from athlet a where a.id = ar.athlet_id and a.verein = vr.verein_id)
           """.as[AthletRegistration]
    }, Duration.Inf).toList
  }

  def updateAthletRegistration(registration: AthletRegistration) = {
    if (registration.id == 0L) {
      throw new IllegalArgumentException("AthletRegistration with id=0 can not be updated")
    }
    val gebdat: java.sql.Date = str2SQLDate(registration.gebdat)
    Await.result(database.run {
      sql"""
              update athletregistration
              set athlet_id=${registration.athletId},
                  name=${registration.name}, vorname=${registration.vorname},
                  gebdat=${gebdat}, geschlecht=${registration.geschlecht},
                  program_id=${registration.programId}
              where id=${registration.id}
     """.as[Long].headOption
    }, Duration.Inf)
    registration
  }

  def selectAthletRegistration(id: Long) = {
    Await.result(database.run {
      sql"""
                  select
                      ar.id, ar.vereinregistration_id,
                      ar.athlet_id, ar.geschlecht, ar.name, ar.vorname, ar.gebdat,
                      ar.program_id, ar.registrationtime
                  from athletregistration ar
                  where ar.id = ${id}
       """.as[AthletRegistration]
    }, Duration.Inf).head
  }

  def selectAthletRegistrations(id: Long) = {
    Await.result(database.run {
      sql"""
                  select
                      ar.id, ar.vereinregistration_id,
                      ar.athlet_id, ar.geschlecht, ar.name, ar.vorname, ar.gebdat,
                      ar.program_id, ar.registrationtime
                  from athletregistration ar
        where ar.vereinregistration_id = $id
        order by ar.registrationtime asc
       """.as[AthletRegistration]
    }, Duration.Inf).toList
  }

  def deleteAthletRegistration(id: Long): Unit = {
    Await.result(database.run {
      sqlu""" delete from athletregistration where id = $id"""
    }, Duration.Inf)
  }

  def removeVereinAthletReferencesRegistration(vereinId: Long): Unit = {
    Await.result(database.run {
      removeVereinAndAthletIds(vereinId)
    }, Duration.Inf)
  }
}