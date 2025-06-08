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

  implicit val getWettkampfResult: GetResult[Wettkampf] = GetResult(r =>
    Wettkampf(r.<<, r.nextStringOption(), r.<<[java.sql.Date], r.<<, r.<<, r.<<, r.<<[BigDecimal], r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit val getWettkampfDisziplinResult: GetResult[Wettkampfdisziplin] = GetResult(r =>
    Wettkampfdisziplin(r.<<, r.<<, r.<<, r.<<, r.nextBytesOption(), r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  implicit def getWettkampfDisziplinViewResultCached(r: PositionedResult)(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]): WettkampfdisziplinView = {
    val id = r.<<[Long]
    val pgm = readProgramm(r.<<[Long], cache)
    WettkampfdisziplinView(id, pgm, r, r.<<, r.nextBytesOption(), readNotenModus(id, pgm, r.<<), r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<)
  }
  
  implicit def getWettkampfViewResultCached(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]): GetResult[WettkampfView] = GetResult(r =>
    WettkampfView(r.<<, r.nextStringOption(), r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<, cache), r.<<, r.<<[BigDecimal], r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit def getWettkampfViewResult: GetResult[WettkampfView] = GetResult(r =>
    WettkampfView(r.<<, r.nextStringOption(), r.<<[java.sql.Date], r.<<, readProgramm(r.<<), r.<<, r.<<[BigDecimal], r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit val getProgrammRawResult: GetResult[ProgrammRaw] = GetResult(r =>
    // id: Long, name: String, aggregate: Int, parentId: Long, ord: Int, alterVon: Int, alterBis: Int, uuid: String, riegenmode, bestOfCount
    ProgrammRaw(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  implicit val getPublishedScoreViewResult: GetResult[PublishedScoreView] = GetResult(r =>
    PublishedScoreView(r.<<, r.<<, r.<<, r.<<, r.<<[java.sql.Date], r.<<))

  implicit def getWettkampfPlanTimeView(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]): GetResult[WettkampfPlanTimeView] = GetResult(r =>
    WettkampfPlanTimeView(r.<<, r, r, r.<<, r.<<, r.<<, r.<<))

  implicit def getWettkampfStats: GetResult[WettkampfStats] = GetResult(r => WettkampfStats(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
  implicit def getWettkampfMetaData: GetResult[WettkampfMetaData] = GetResult(r => WettkampfMetaData(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<?, mapBigDecimalOption(r), mapBigDecimalOption(r)))
}