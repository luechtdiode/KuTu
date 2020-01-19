package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait DurchgangResultMapper extends DisziplinResultMapper with WertungsrichterResultMapper {

  //implicit val getDurchgangResultMini = GetResult(r => Durchgang(r.<<, r.<<))
  implicit val getDurchgangResult = GetResult(r => Durchgang(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  implicit val getDurchgangStationResult = GetResult(r => Durchgangstation(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getDisziplinResult(r)))
  
  implicit val getDurchgangStationViewResult = GetResult(r => DurchgangstationView(r.<<, r.<<, getWertungsrichterOptionResult(r), getWertungsrichterOptionResult(r), getWertungsrichterOptionResult(r), getWertungsrichterOptionResult(r), getDisziplinResult(r)))


}