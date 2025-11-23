package ch.seidel.kutu.domain

import ch.seidel.kutu.calc.ScoreCalcTemplate
import slick.jdbc.{GetResult, PositionedResult}

/**
 * Wettkampf
 * Programm
 * Disziplin
 */
trait WettkampfResultMapper extends DisziplinResultMapper {
  def readNotenModus(wkId: Long, id: Long, disz: Disziplin, pgm: ProgrammView, notenfaktor: Double, cache: scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]): NotenModus
  def readProgramm(id: Long, cache: scala.collection.mutable.Map[Long, ProgrammView]): ProgrammView
  def readProgramm(id: Long): ProgrammView

  implicit val getWettkampfResult: GetResult[Wettkampf] = GetResult(using r =>
    Wettkampf(r.<<, r.nextStringOption(), r.<<[java.sql.Date], r.<<, r.<<, r.<<, r.<<[BigDecimal], r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit val getWettkampfDisziplinResult: GetResult[Wettkampfdisziplin] = GetResult(using r =>
    Wettkampfdisziplin(r.<<, r.<<, r.<<, r.<<, r.nextBytesOption(), r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  implicit def getWettkampfDisziplinViewResultCached(r: PositionedResult)(implicit wkid: Long, cache: scala.collection.mutable.Map[Long, ProgrammView], cache2: scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]): WettkampfdisziplinView = {
    val id = r.<<[Long]
    val pgm = readProgramm(r.<<[Long], cache)
    val disz: Disziplin = Disziplin(r.<<[Long], r.<<[String])
    WettkampfdisziplinView(id, pgm, disz, r.<<, r.nextBytesOption(), readNotenModus(wkid, id, disz, pgm, r.<<, cache2), r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<)
  }
  
  implicit def getWettkampfViewResultCached(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]): GetResult[WettkampfView] = GetResult(using r =>
    WettkampfView(r.<<, r.nextStringOption(), r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<, cache), r.<<, r.<<[BigDecimal], r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit def getWettkampfViewResult: GetResult[WettkampfView] = GetResult(using r =>
    WettkampfView(r.<<, r.nextStringOption(), r.<<[java.sql.Date], r.<<, readProgramm(r.<<), r.<<, r.<<[BigDecimal], r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit val getProgrammRawResult: GetResult[ProgrammRaw] = GetResult(using r =>
    // id: Long, name: String, aggregate: Int, parentId: Long, ord: Int, alterVon: Int, alterBis: Int, uuid: String, riegenmode, bestOfCount
    ProgrammRaw(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  implicit val getPublishedScoreViewResult: GetResult[PublishedScoreView] = GetResult(using r =>
    PublishedScoreView(r.<<, r.<<, r.<<, r.<<, r.<<[java.sql.Date], r.<<))

  implicit def getWettkampfPlanTimeView(implicit wkId: Long, cache: scala.collection.mutable.Map[Long, ProgrammView], cache2: scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]): GetResult[WettkampfPlanTimeView] = GetResult(using r => {
    val id: Long = r.<<
    val wkd: WettkampfdisziplinView = r
    val wk: Wettkampf = getWettkampfResult(r)
    WettkampfPlanTimeView(id, wk, wkd, r.<<, r.<<, r.<<, r.<<)
  })

  implicit def getWettkampfStats: GetResult[WettkampfStats] = GetResult(using r => WettkampfStats(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
  implicit def getWettkampfMetaData: GetResult[WettkampfMetaData] = GetResult(using r => WettkampfMetaData(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<?, mapBigDecimalOption(r), mapBigDecimalOption(r)))
}