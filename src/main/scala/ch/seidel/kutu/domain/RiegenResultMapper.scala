package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait RiegenResultMapper extends DisziplinResultMapper {
      
  implicit val getRiegeRawResult: GetResult[RiegeRaw] = GetResult(r =>
    RiegeRaw(r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit val getRiegeResult: GetResult[Riege] = GetResult(r =>
    Riege(r.<<, r.<<, r, r.<<))
}