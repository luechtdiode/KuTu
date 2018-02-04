package ch.seidel.kutu.domain

import slick.jdbc.PositionedResult
import slick.jdbc.GetResult

trait RiegenResultMapper extends DisziplinResultMapper {
      
  implicit val getRiegeRawResult = GetResult(r =>
    RiegeRaw(r.<<, r.<<, r.<<, r.<<))
    
  implicit val getRiegeResult = GetResult(r =>
    Riege(r.<<, r.<<, r))
}