package ch.seidel.kutu.renderer

import ch.seidel.kutu.domain.{GeraeteRiege, TeamItem, Wettkampf}

object Kandidaten {
  def apply(riegen: Seq[GeraeteRiege]): Seq[Kandidat] = {
    val wkOption: Option[Wettkampf] = riegen.headOption
      .flatMap(_.kandidaten.headOption)
      .map(_.wertungen.head.wettkampf)
    val virtualTeams = wkOption.toList
      .flatMap(_.extraTeams.zipWithIndex)
      .map(item => TeamItem(item._2 * -1 - 1, item._1))
      .map(vt => vt.index -> vt)
      .toMap
    riegen
      // filter startgeraet
      .filter(riege => riege.halt == 0)
      // filter hauptdurchgang-startgeraet
      .filter(riege => !riege.kandidaten.exists(k => k.einteilung2.exists(d => d.start == riege.disziplin)))
      .flatMap(riege => {
        riege.kandidaten
          .map(kandidat => {
            val team = if (kandidat.wertungen.head.team != 0) virtualTeams.getOrElse(kandidat.wertungen.head.team, TeamItem(kandidat.wertungen.head.team, kandidat.verein)) else TeamItem(0, "")
            Kandidat(riege.wettkampfTitel, kandidat.geschlecht, kandidat.programm, kandidat.id, kandidat.name, kandidat.vorname, kandidat.jahrgang, kandidat.verein, team.itemText, "", riege.durchgang.get, riege.disziplin.get.easyprint, Seq.empty)
          })
      })
  }
}
case class Kandidat(wettkampfTitel: String, geschlecht: String, programm: String, id: Long,
                    name: String, vorname: String, jahrgang: String, verein: String, team: String,
                    riege: String, durchgang: String, start: String, diszipline: Seq[String])
