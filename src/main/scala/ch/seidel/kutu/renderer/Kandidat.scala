package ch.seidel.kutu.renderer

import ch.seidel.kutu.domain.GeraeteRiege

object Kandidaten {
  def apply(riegen: Seq[GeraeteRiege]): Seq[Kandidat] = {
    riegen
      // filter startgeraet
      .filter(riege => riege.halt == 0)
      // filter hauptdurchgang-startgeraet
      .filter(riege => !riege.kandidaten.exists(k => k.einteilung2.exists(d => d.start == riege.disziplin)))
      .flatMap(riege => {
        riege.kandidaten
          .map(kandidat => {
            Kandidat(riege.wettkampfTitel, kandidat.geschlecht, kandidat.programm, kandidat.id, kandidat.name, kandidat.vorname, kandidat.jahrgang, kandidat.verein, "", riege.durchgang.get, riege.disziplin.get.easyprint, Seq.empty)
          })
      })
  }
}
case class Kandidat(wettkampfTitel: String, geschlecht: String, programm: String, id: Long,
                    name: String, vorname: String, jahrgang: String, verein: String,
                    riege: String, durchgang: String, start: String, diszipline: Seq[String])
