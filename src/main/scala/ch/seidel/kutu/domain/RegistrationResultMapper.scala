package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait RegistrationResultMapper extends AthletResultMapper with MediaResultMapper {
  val getTime = GetResult(r => {
    val t: java.sql.Timestamp = r.<<
    t.getTime
  })

  implicit val getRegistrationResult: GetResult[Registration] = GetResult(r =>
    Registration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r), r))
  implicit val getAthletRegistrationResult: GetResult[AthletRegistration] = GetResult { r =>
    val id: Long = r.<<
    val vereinregistrationId: Long = r.<<
    val athletId: Option[Long] = r.<<?
    val geschlecht: String = r.<<
    val name: String = r.<<
    val vorname: String = r.<<
    val gebdat: String = r.<<
    val programId: Long = r.<<
    val registrationTime: Long = getTime(r)
    val athlet: Option[AthletView] = getAthletOptionResult(r)
    var team: Option[Int] = None
    var mediafile: Option[MediaAdmin] = None
    while(r.hasMoreColumns) {
      r.currentPos match {
        case pos:Int if (pos == 23) =>
          team = r.<<?
        case pos:Int if (pos == 24) =>
          mediafile = r.<<?
        case _ => r.nextObjectOption()
      }
    }
    AthletRegistration(id, vereinregistrationId, athletId, geschlecht, name, vorname, gebdat, programId, registrationTime, athlet, team, mediafile)
  }
  implicit val getJudgeRegistrationResult: GetResult[JudgeRegistration] = GetResult(r =>
    JudgeRegistration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r)))
  implicit val getJudgeRegistrationProgramResult: GetResult[JudgeRegistrationProgram] = GetResult(r =>
    JudgeRegistrationProgram(r.<<, r.<<, r.<<, r.<<, r.<<))
  implicit val getJudgeRegistrationProgramItemResult: GetResult[JudgeRegistrationProgramItem] = GetResult(r =>
    JudgeRegistrationProgramItem(r.<<, r.<<, r.<<))

}