package ch.seidel.kutu.domain

import slick.jdbc.PositionedResult
import slick.jdbc.GetResult

/**
 * Wettkampf
 * Programm
 * Disziplin
 */
abstract trait WettkampfResultMapper extends DisziplinResultMapper {
  def readNotenModus(id: Long, pgm: ProgrammView, notenfaktor: Double): NotenModus
  def readProgramm(id: Long, cache: scala.collection.mutable.Map[Long, ProgrammView]): ProgrammView
  def readProgramm(id: Long): ProgrammView
  
  implicit val getWettkampfResult = GetResult(r =>
    Wettkampf(r.<<[Long], r.<<[java.sql.Date], r.<<[String], r.<<[Long], r.<<[Int], r.<<[BigDecimal], r.nextStringOption()))
    
  implicit val getWettkampfDisziplinResult = GetResult(r =>
    Wettkampfdisziplin(r.<<[Long], r.<<[Long], r.<<[Long], r.<<[String], r.nextBlobOption(), r.<<, r.<<[Int], r.<<[Int], r.<<[Int]))

  implicit def getWettkampfDisziplinViewResultCached(r: PositionedResult)(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]) = {
    val id = r.<<[Long]
    val pgm = readProgramm(r.<<[Long], cache)
    WettkampfdisziplinView(id, pgm, r, r.<<[String], r.nextBytesOption(), readNotenModus(id, pgm, r.<<), r.<<, r.<<[Int], r.<<[Int])
  }
  
  implicit def getWettkampfViewResultCached(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long], cache), r.<<[Int], r.<<[BigDecimal], r.nextStringOption()))
    
  implicit def getWettkampfViewResult = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long]), r.<<[Int], r.<<[BigDecimal], r.nextStringOption()))
    
  implicit val getProgrammRawResult = GetResult(r =>
    ProgrammRaw(r.<<[Long], r.<<[String], r.<<[Int], r.<<[Long], r.<<[Int], r.<<[Int], r.<<[Int]))
  
}