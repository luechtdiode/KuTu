package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait DisziplinResultMapper  {
  def mapBigDecimalOption(rs: _root_.slick.jdbc.PositionedResult): Option[BigDecimal] = {
    val value: Option[String] = rs.<<?
    value.map(BigDecimal(_))
  }
  implicit val getDisziplinResult: GetResult[Disziplin] = GetResult(using r =>
    Disziplin(r.<<[Long], r.<<[String]))
  implicit val getDisziplinOptionResult: GetResult[Option[Disziplin]] = GetResult(using r => r.nextLongOption() match {
    case Some(id) => Some(Disziplin(id, r.<<[String]))
    case _        => r.skip; None
  })
}