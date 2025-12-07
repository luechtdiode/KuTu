package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait RegistrationResultMapper extends AthletResultMapper with MediaResultMapper with VereinResultMapper {

  implicit val getRegistrationResult: GetResult[Registration] = GetResult(using r =>
    Registration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r), r.<<?[Verein]))
  implicit val getAthletRegistrationResult: GetResult[AthletRegistration] = GetResult(using r =>
    val id: Long = r.<<
    val vereinregistrationId: Long = r.<<
    val athletId: Option[Long] = r.<<?
    val geschlecht: String = r.<<
    val name: String = r.<<
    val vorname: String = r.<<
    val gebdat: String = dateToExportedStr(r.<<)
    val programId: Long = r.<<
    val registrationTime: Long = getTime(r)
    val athlet: Option[AthletView] = getAthletOptionResult(r)
    var team: Option[Int] = None
    var mediafile: Option[MediaAdmin] = None
    while r.hasMoreColumns do {
      r.currentPos match {
        case pos:Int if pos == 23 =>
          team = r.<<?
        case pos:Int if pos == 24 =>
          mediafile = r.<<?
        case _ => r.nextObjectOption()
      }
    }
    AthletRegistration(id, vereinregistrationId, athletId, geschlecht, name, vorname, gebdat, programId, registrationTime, athlet, team, mediafile)
  )
  implicit val getJudgeRegistrationResult: GetResult[JudgeRegistration] = GetResult(using r =>
    JudgeRegistration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r)))
  implicit val getJudgeRegistrationProgramResult: GetResult[JudgeRegistrationProgram] = GetResult(using r =>
    JudgeRegistrationProgram(r.<<, r.<<, r.<<, r.<<, r.<<))
  implicit val getJudgeRegistrationProgramItemResult: GetResult[JudgeRegistrationProgramItem] = GetResult(using r =>
    JudgeRegistrationProgramItem(r.<<, r.<<, r.<<))

}