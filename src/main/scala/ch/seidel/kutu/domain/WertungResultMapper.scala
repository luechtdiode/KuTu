package ch.seidel.kutu.domain

import slick.jdbc.GetResult

/**
 * Wettkampf
 * Programm
 * Disziplin
 */
trait WertungResultMapper extends WettkampfResultMapper with DisziplinResultMapper with AthletResultMapper {
    implicit def getResultWertungView(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]): GetResult[WertungView] = GetResult(r =>
    WertungView(r.<<[Long], r, r, r, r.<<[Option[scala.math.BigDecimal]], r.<<[Option[scala.math.BigDecimal]], r.<<[Option[scala.math.BigDecimal]], r.<<?, r.<<?, r.<<))
    //WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal, riege: Option[String], riege2: Option[String], team: Int)
    implicit def getResultWertung: GetResult[Wertung] = GetResult(r => Wertung(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<?, r.<<?, r.<<))

}