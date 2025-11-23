package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait DurchgangResultMapper extends DisziplinResultMapper with WertungsrichterResultMapper {

  //implicit val getDurchgangResultMini = GetResult(r => Durchgang(r.<<, r.<<))
  implicit val getSimpleDurchgangResult: GetResult[SimpleDurchgang] = GetResult(using r => SimpleDurchgang(r.<<, r.<<, r.<<, r.<<, DurchgangType(r.<<), r.<<, r.<<, r.<<, r.<<))
  implicit val getDurchgangResult: GetResult[Durchgang] = GetResult(using r => Durchgang(r.<<, r.<<, r.<<, r.<<, DurchgangType(r.<<), r.<<, r.<<,
    r.<<, r.<<,
    r.<<, r.<<, r.<<))

  implicit val getDurchgangStationResult: GetResult[Durchgangstation] = GetResult(using r => Durchgangstation(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, getDisziplinResult(r)))
  
  implicit val getDurchgangStationViewResult: GetResult[DurchgangstationView] = GetResult(using r => DurchgangstationView(r.<<, r.<<, getWertungsrichterOptionResult(r), getWertungsrichterOptionResult(r), getWertungsrichterOptionResult(r), getWertungsrichterOptionResult(r), getDisziplinResult(r)))


}