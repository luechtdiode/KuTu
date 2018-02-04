package ch.seidel.kutu.domain

import slick.jdbc.PositionedResult
import slick.jdbc.GetResult

trait KampfrichterResultMapper extends VereinResultMapper {
      
  implicit val getKampfrichterResult = GetResult(r =>
    Kampfrichter(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit val getKampfrichterViewResult = GetResult(r =>
    //id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
    KampfrichterView(
        id = r.<<,
        js_id = r.<<,
        geschlecht = r.<<,
        name = r.<<,
        vorname = r.<<,
        gebdat = r.<<,
        strasse = r.<<,
        plz = r.<<,
        ort = r.<<,
        activ = r.<<,
        verein = r))
        
  implicit val getKampfrichterOptionResult = GetResult(r => r.nextLongOption() match {
    case Some(id) => Some(KampfrichterView(id, js_id = r.<<,
                                          geschlecht = r.<<,
                                          name = r.<<,
                                          vorname = r.<<,
                                          gebdat = r.<<,
                                          strasse = r.<<,
                                          plz = r.<<,
                                          ort = r.<<,
                                          activ = r.<<,
                                          verein = r))
    case _        => {r.skip; None}
  })
  
}