package ch.seidel.kutu.domain

import slick.jdbc.{GetResult, PositionedResult}

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
    Wettkampf(r.<<, r.nextStringOption(), r.<<[java.sql.Date], r.<<, r.<<, r.<<, r.<<[BigDecimal]))
    
  implicit val getWettkampfDisziplinResult = GetResult(r =>
    Wettkampfdisziplin(r.<<, r.<<, r.<<, r.<<, r.nextBlobOption(), r.<<, r.<<, r.<<, r.<<))

  implicit def getWettkampfDisziplinViewResultCached(r: PositionedResult)(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]) = {
    val id = r.<<[Long]
    val pgm = readProgramm(r.<<[Long], cache)
    WettkampfdisziplinView(id, pgm, r, r.<<, r.nextBytesOption(), readNotenModus(id, pgm, r.<<), r.<<, r.<<, r.<<)
  }
  
  implicit def getWettkampfViewResultCached(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WettkampfView(r.<<, r.nextStringOption(), r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<, cache), r.<<, r.<<[BigDecimal]))
    
  implicit def getWettkampfViewResult = GetResult(r =>
    WettkampfView(r.<<, r.nextStringOption(), r.<<[java.sql.Date], r.<<, readProgramm(r.<<), r.<<, r.<<[BigDecimal]))
    
  implicit val getProgrammRawResult = GetResult(r =>
    ProgrammRaw(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  implicit val getPublishedScoreViewResult = GetResult(r =>
    PublishedScoreView(r.<<, r.<<, r.<<, r.<<))
}