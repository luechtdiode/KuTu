package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait RegistrationResultMapper extends AthletResultMapper {
  val getTime = GetResult(r => {
    val t: java.sql.Timestamp = r.<<
    t.getTime
  })

  implicit val getRegistrationResult: GetResult[Registration] = GetResult(r =>
    Registration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r), r))
  implicit val getAthletRegistrationResult: GetResult[AthletRegistration] = GetResult(r =>
    AthletRegistration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r), r, r.<<))
  implicit val getJudgeRegistrationResult: GetResult[JudgeRegistration] = GetResult(r =>
    JudgeRegistration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r)))
  implicit val getJudgeRegistrationProgramResult: GetResult[JudgeRegistrationProgram] = GetResult(r =>
    JudgeRegistrationProgram(r.<<, r.<<, r.<<, r.<<, r.<<))
  implicit val getJudgeRegistrationProgramItemResult: GetResult[JudgeRegistrationProgramItem] = GetResult(r =>
    JudgeRegistrationProgramItem(r.<<, r.<<, r.<<))

}