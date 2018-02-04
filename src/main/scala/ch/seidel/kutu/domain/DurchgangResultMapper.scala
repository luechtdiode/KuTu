package ch.seidel.kutu.domain

import slick.jdbc.PositionedResult
import slick.jdbc.GetResult

trait DurchgangResultMapper extends DisziplinResultMapper with KampfrichterResultMapper {

  implicit val getDurchgangStationResult = GetResult(r => Durchgangstation(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getDisziplinResult(r)))
  
  implicit val getDurchgangStationViewResult = GetResult(r => DurchgangstationView(r.<<, r.<<, getKampfrichterOptionResult(r), getKampfrichterOptionResult(r), getKampfrichterOptionResult(r), getKampfrichterOptionResult(r), getDisziplinResult(r)))


}