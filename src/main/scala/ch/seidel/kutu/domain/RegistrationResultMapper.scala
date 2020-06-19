package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait RegistrationResultMapper {
  val getTime = GetResult(r => {
    val t: java.sql.Timestamp = r.<<
    t.getTime()
  })

  implicit val getRegistrationResult = GetResult(r =>
    Registration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r)))
  implicit val getAthletRegistrationResult = GetResult(r =>
    AthletRegistration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getTime(r)))

}