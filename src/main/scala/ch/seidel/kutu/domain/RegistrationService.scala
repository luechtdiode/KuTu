package ch.seidel.kutu.domain

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

import ch.seidel.kutu.http.Hashing
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

trait RegistrationService extends DBService with RegistrationResultMapper with Hashing {

  def createRegistration(newReg: NewRegistration): Registration = {
    val similarRegistrations = selectRegistrationsLike(newReg.toRegistration)
      .filter{(reg: Registration) =>
        val oldHash = vereinSecretHashLookup(s"${reg.id}")
        val newHash = matchHashed(oldHash)(newReg.secret)
        oldHash.equals(newHash)
      }
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

  def resetRegistrationPW(resetPW: RegistrationResetPW): Registration = {
    if (resetPW.id == 0L) {
      throw new IllegalArgumentException("Registration with id=0 can not be updated")
    }
    Await.result(database.run {
      sqlu"""
              update vereinregistration
              set secrethash=${hashed(resetPW.secret)}
              where id=${resetPW.id}
      """ >>
      sql"""
              select
              id, wettkampf_id, verein_id, vereinname, verband,
              responsible_name, responsible_vorname, mobilephone, mail,
              registrationtime
              from vereinregistration where id=${resetPW.id}
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

  def deleteRegistration(registrationId: Long): Unit = {
    Await.result(database.run {
      deleteRegistrationAction(registrationId)
    }, Duration.Inf)
  }

  def copyClubRegsFromCompetition(wettkampfCopyFrom: String, registrationId: Long): Unit = {
    Await.result(database.run {
      sqlu"""
              insert into athletregistration (vereinregistration_id, athlet_id, geschlecht, name, vorname, gebdat, program_id, registrationtime)
              select distinct
                #$registrationId as vereinregistration_id,
                a.id,
                a.geschlecht,
                a.name,
                a.vorname,
                a.gebdat,
                wkd.programm_id,
                current_timestamp as registrationtime
              from athlet a
              inner join wertung w on (w.athlet_id = a.id)
              inner join wettkampfdisziplin wkd on (w.wettkampfdisziplin_id = wkd.id)
              inner join vereinregistration vrnow on (vrnow.verein_id = a.verein)
              inner join wettkampf wkthen on (w.wettkampf_id = wkthen.id)
              where vrnow.id = $registrationId
                  and wkthen.uuid = $wettkampfCopyFrom
                  and not exists (
                    select 1
                    from athletregistration arex
                    where arex.vereinregistration_id = $registrationId
                      and arex.name = a.name
                      and arex.vorname = a.vorname
                      and arex.gebdat = a.gebdat
                      and arex.geschlecht = a.geschlecht
                  )
          """ >>
        sqlu"""
              insert into judgeregistration (vereinregistration_id, geschlecht, name, vorname, mobilephone, mail,comment, registrationtime)
              select
                #$registrationId as vereinregistration_id,
                ar.geschlecht,
                ar.name,
                ar.vorname,
                ar.mobilephone,
                ar.mail,
                ar.comment,
                current_timestamp as registrationtime
              from judgeregistration ar
              where ar.vereinregistration_id = (
                select distinct vrthen.id
                from vereinregistration vrthen
                inner join vereinregistration vrnow on (vrthen.verein_id = vrnow.verein_id)
                inner join wettkampf wkthen on (vrthen.wettkampf_id = wkthen.id)
                inner join wettkampf wknow on (vrnow.wettkampf_id = wknow.id)
                where wkthen.uuid = $wettkampfCopyFrom
                  and vrthen.id <> $registrationId
                  and vrnow.id = $registrationId
              )
              and not exists (
                select 1
                from judgeregistration arex
                where arex.vereinregistration_id = $registrationId
                  and arex.name = ar.name
                  and arex.vorname = ar.vorname
                  and arex.geschlecht = ar.geschlecht
              )
          """

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


  // JudgeRegistration

  def createJudgeRegistration(newReg: JudgeRegistration): JudgeRegistration = {
    val nomralizedJudge = newReg.normalized

    Await.result(database.run {
      sqlu"""
                  insert into judgeregistration
                  (vereinregistration_id, geschlecht, name, vorname, mobilephone, mail, comment, registrationtime)
                  values (${newReg.vereinregistrationId},
                          ${nomralizedJudge.geschlecht}, ${nomralizedJudge.name},
                          ${nomralizedJudge.vorname}, ${nomralizedJudge.mobilephone}, ${nomralizedJudge.mail}, ${nomralizedJudge.comment},
                          ${Timestamp.valueOf(LocalDateTime.now())})
              """ >>
        sql"""
                  select
                      id, vereinregistration_id,
                      geschlecht, name, vorname, mobilephone, mail, comment,
                      registrationtime
                  from judgeregistration
                  where id = (select max(ar.id)
                              from judgeregistration ar
                              where ar.vereinregistration_id=${newReg.vereinregistrationId}
                                and ar.geschlecht=${nomralizedJudge.geschlecht}
                                and ar.name=${nomralizedJudge.name}
                                and ar.vorname=${nomralizedJudge.vorname}
                                )
         """.as[JudgeRegistration].head.transactionally
    }, Duration.Inf)
  }
//
//
//  def selectJudgeRegistrationLike(registration: JudgeRegistration) = {
//    val nomralizedJudge = registration.toWertungsrichter
//    Await.result(database.run {
//      sql"""
//                  select
//                      ar.id, ar.vereinregistration_id,
//                      ar.geschlecht, ar.name, ar.vorname, ar.mobliephone, ar.mail,
//                      ar.registrationtime
//                  from judgeregistration ar
//                  inner join vereinregistration vr on ar.vereinregistration_id = vr.id
//                  where ar.geschlecht=${nomralizedJudge.geschlecht}
//                    and ar.name=${nomralizedJudge.name}
//                    and ar.vorname=${nomralizedJudge.vorname}
//                    and ar.vereinregistration_id <> ${registration.vereinregistrationId}
//                    and exists (select 1 from kampfrichter a where a.id = ar.judge_id and a.verein = vr.verein_id)
//           """.as[JudgeRegistration]
//    }, Duration.Inf).toList
//  }

  def updateJudgeRegistration(registration: JudgeRegistration) = {
    if (registration.id == 0L) {
      throw new IllegalArgumentException("JudgeRegistration with id=0 can not be updated")
    }
    registration.validate()
    Await.result(database.run {
      sql"""
              update judgeregistration
              set name=${registration.name}, vorname=${registration.vorname}, geschlecht=${registration.geschlecht},
                  mobilephone=${registration.mobilephone}, mail=${registration.mail}, comment=${registration.comment}
              where id=${registration.id}
     """.as[Long].headOption
    }, Duration.Inf)
    registration
  }

  def selectJudgeRegistration(id: Long) = {
    Await.result(database.run {
      sql"""
                  select
                      ar.id, ar.vereinregistration_id,
                      ar.geschlecht, ar.name, ar.vorname,
                      ar.mobilephone, ar.mail, ar.comment,
                      ar.registrationtime
                  from judgeregistration ar
                  where ar.id = ${id}
       """.as[JudgeRegistration]
    }, Duration.Inf).head
  }

  def selectJudgeRegistrations(id: Long) = {
    Await.result(database.run {
      sql"""
                  select
                      ar.id, ar.vereinregistration_id,
                      ar.geschlecht, ar.name, ar.vorname,
                      ar.mobilephone, ar.mail, ar.comment,
                      ar.registrationtime
                  from judgeregistration ar
        where ar.vereinregistration_id = $id
        order by ar.registrationtime asc
       """.as[JudgeRegistration]
    }, Duration.Inf).toList
  }

  def deleteJudgeRegistration(id: Long): Unit = {
    Await.result(database.run {
      sqlu""" delete from judgeregistration_pgm where judgeregistration_id = $id""" >>
      sqlu""" delete from judgeregistration where id = $id"""
    }, Duration.Inf)
  }

  def listJudgeRegistrationProgramItems(programme: Seq[Long]): Future[Vector[JudgeRegistrationProgramItem]] = {
    // program: String, disziplin: String, disziplinId: Long
    database.run{
      val pgms = programme.mkString("(", ",", ")")
      sql""" select distinct p.name, d.name, wd.id, p.ord, wd.ord
             from disziplin d
             inner join wettkampfdisziplin wd on d.id = wd.disziplin_id
             inner join programm p on p.id = wd.programm_id
             where wd.programm_id in #$pgms
             order by p.ord, wd.ord""".as[JudgeRegistrationProgramItem]
    }
  }

  def listJudgePgmRegistrations(judgeId: Long) = {
    Await.result(database.run {
      sql"""
                  select
                      ar.id, ar.vereinregistration_id,
                      ar.judgeregistration_id,
                      ar.wettkampfdisziplin_id,
                      ar.comment
                  from judgeregistration_pgm ar
        where ar.judgeregistration_id = $judgeId
        order by ar.wettkampfdisziplin_id asc
       """.as[JudgeRegistrationProgram]
    }, Duration.Inf).toList
  }

  def saveJudgePgmRegistrations(judgeId: Long, wettkampfDisziplinIds: List[Long]) = {
    val vereinregistrationId = selectJudgeRegistration(judgeId).id
    Await.result(database.run {
      sqlu""" delete from judgeregistration_pgm where id = $judgeId""" >>
        DBIO.sequence(for{wkid <- wettkampfDisziplinIds} yield
          sqlu"""
                  insert into judgeregistration_pgm
                  (vereinregistration_id, judgeregistration_id, wettkampfdisziplin_id, comment)
                  values (${vereinregistrationId}, $judgeId}, ${wkid}, ${""})
              """) >>
          sql"""  select id, vereinregistration_id, judgeregistration_id, wettkampfdisziplin_id, comment
                  from judgeregistration_pgm
                  where judgeregistration_id=$judgeId
             """.as[JudgeRegistrationProgram].head.transactionally
    }, Duration.Inf)
  }

  def loadAllJudgesOfCompetition(wettkampf: UUID): Map[Registration,List[JudgeRegistration]] = {
    (for {
      reg <- selectRegistrationsOfWettkampf(wettkampf)
      judge <- selectJudgeRegistrations(reg.id)
    } yield {
      (reg, judge)
    }).foldLeft(Map[Registration,List[JudgeRegistration]]()) {(acc, entry) =>
      val registrations = acc.getOrElse(entry._1, List[JudgeRegistration]())
      acc + (entry._1 -> (registrations :+ entry._2))
    }
  }

  def deleteJudgePgmRegistration(id: Long): Unit = {
    Await.result(database.run {
      sqlu""" delete from judgeregistration_pgm where id = $id"""
    }, Duration.Inf)
  }
}