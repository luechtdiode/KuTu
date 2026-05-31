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
    val team: Option[Int] = if r.hasMoreColumns then r.<<? else None
    val mediafile: Option[MediaAdmin] = if r.hasMoreColumns then r.<<? else None
    val reserve: Int = if r.hasMoreColumns then r.<< else 0
    while (r.hasMoreColumns) println(r.nextObject().toString)
    AthletRegistration(id, vereinregistrationId, athletId, geschlecht, name, vorname, gebdat, programId, registrationTime, athlet, team, mediafile, reserve)
  )
  implicit val getJudgeRegistrationResult: GetResult[JudgeRegistration] = GetResult(using r =>
    JudgeRegistration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r)))
  implicit val getJudgeRegistrationProgramResult: GetResult[JudgeRegistrationProgram] = GetResult(using r =>
    JudgeRegistrationProgram(r.<<, r.<<, r.<<, r.<<, r.<<))
  implicit val getJudgeRegistrationProgramItemResult: GetResult[JudgeRegistrationProgramItem] = GetResult(using r =>
    JudgeRegistrationProgramItem(r.<<, r.<<, r.<<))

}