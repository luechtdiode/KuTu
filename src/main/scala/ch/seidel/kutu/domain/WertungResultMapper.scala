package ch.seidel.kutu.domain

import ch.seidel.kutu.calc.{ScoreAggregateFn, ScoreCalcTemplate, ScoreCalcTemplateView, TemplateViewJsonReader}
import slick.jdbc.GetResult

trait MediaResultMapper {
  implicit def getMediaOption: GetResult[Option[Media]] = GetResult{ r =>
    val id = r.<<?[Long]
    id match {
      case Some(x) => Some(Media(x, r.<<, r.<<))
      case None => r.skip; r.skip; None
    }
  }
  implicit def getMedia: GetResult[Media] = GetResult{ r => Media(r.<<, r.<<, r.<<)}
}

trait ScoreCalcTemplateResultMapper {
    implicit def getScoreCalcTemplate: GetResult[ScoreCalcTemplate] = GetResult(r =>
        ScoreCalcTemplate(r.<<, r.<<?, r.<<?, r.<<?, r.<<, r.<<, r.<<, ScoreAggregateFn(r.<<?))
    )
    implicit def getScoreCalcTemplateView: GetResult[Option[ScoreCalcTemplateView]] = GetResult(r =>
        TemplateViewJsonReader(r.nextStringOption())
    )
}
/**
 * Wettkampf
 * Programm
 * Disziplin
 */
trait WertungResultMapper extends WettkampfResultMapper with DisziplinResultMapper with AthletResultMapper with ScoreCalcTemplateResultMapper with MediaResultMapper {
    //WertungView(id: Long, athlet: AthletView, wettkampf: Wettkampf, wettkampfdisziplin: WettkampfdisziplinView, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal, riege: Option[String], riege2: Option[String], team: Int, mediafile: Option[Media], variables: Option[ScoreCalcVariable)
    implicit def getResultWertungView(implicit cache: scala.collection.mutable.Map[Long, ProgrammView], cache2: scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]): GetResult[WertungView] = GetResult{ r =>
        val id = r.<<[Long]
        val av: AthletView = r
        val wk: Wettkampf = r
        implicit val wkId: Long = wk.id
        val wd: WettkampfdisziplinView = r
        WertungView(id, av, wd, wk, mapBigDecimalOption(r), mapBigDecimalOption(r), mapBigDecimalOption(r), r.<<?, r.<<?, r.<<, getMediaOption(r), r.<<?)
    }
    implicit def getResultWertung: GetResult[Wertung] = GetResult{
        r => {
            val wettkampfId: Long = r.<<
            val wettkampfdisziplinId: Long = r.<<
            Wertung(r.<<, r.<<, wettkampfdisziplinId, wettkampfId, r.<<, r.<<, r.<<, r.<<, r.<<?, r.<<?, r.<<?, getMediaOption(r), r.<<?)
        }
    }
}