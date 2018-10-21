package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait VereinResultMapper {
  implicit val getVereinResult = GetResult(r => Verein(r.<<[Long], r.<<[String], r.<<))
  implicit val getVereinOptionResult = GetResult(r => r.nextLongOption() match {
    case Some(id) => Some(getVereinResult(r))
    case _ => { r.skip; r.skip; None }
  })
}