package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait WertungsrichterResultMapper extends VereinResultMapper {
      
  implicit val getWertungsrichterResult: GetResult[Wertungsrichter] = GetResult(using r =>
    Wertungsrichter(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    
  implicit val getWertungsrichterViewResult: GetResult[WertungsrichterView] = GetResult(using r =>
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
        verein = getVereinOptionResult(r)))
        
  implicit val getWertungsrichterOptionResult: GetResult[Option[WertungsrichterView]] = GetResult(using r => r.nextLongOption() match {
    case Some(id) => Some(WertungsrichterView(id, js_id = r.<<,
                                          geschlecht = r.<<,
                                          name = r.<<,
                                          vorname = r.<<,
                                          gebdat = r.<<,
                                          strasse = r.<<,
                                          plz = r.<<,
                                          ort = r.<<,
                                          activ = r.<<,
                                          verein = getVereinOptionResult(r)))
    case _        => {r.skip; r.skip; r.skip; r.skip; r.skip; r.skip; r.skip; r.skip; r.skip; None}
  })
  
}