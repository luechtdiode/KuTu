package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait RegistrationResultMapper {

  implicit val getRegistrationResult = GetResult(r =>
    Registration(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
}