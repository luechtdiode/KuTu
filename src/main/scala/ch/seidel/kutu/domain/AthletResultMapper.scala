package ch.seidel.kutu.domain

import slick.jdbc.GetResult

trait AthletResultMapper extends VereinResultMapper {
  implicit val getAthletResult = GetResult(r =>
    Athlet(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
  implicit val getAthletViewResult = GetResult(r =>
    //id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
    AthletView(
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
        verein = r
    ))

  implicit val getAthletOptionResult = GetResult(r => r.nextLongOption() match {
    case Some(id) => Some(getAthletViewResult(r))
    case _ => { r.skip; r.skip; r.skip; r.skip; r.skip; r.skip; r.skip; r.skip; r.skip; r.skip; None }
  })
}