package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait WertungsrichterResultMapper extends VereinResultMapper {
      
  implicit val getWertungsrichterResult = GetResult(r =>
    Wertungsrichter(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit val getWertungsrichterViewResult = GetResult(r =>
    //id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
    WertungsrichterView(
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
        
  implicit val getWertungsrichterOptionResult = GetResult(r => r.nextLongOption() match {
    case Some(id) => Some(WertungsrichterView(id, js_id = r.<<,
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