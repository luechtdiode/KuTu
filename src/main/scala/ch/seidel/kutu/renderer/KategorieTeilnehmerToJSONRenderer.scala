package ch.seidel.kutu.renderer

import java.io.File

import ch.seidel.kutu.domain.{GeraeteRiege}
import ch.seidel.kutu.renderer.PrintUtil._
import org.slf4j.LoggerFactory

trait KategorieTeilnehmerToJSONRenderer {
  val logger = LoggerFactory.getLogger(classOf[KategorieTeilnehmerToJSONRenderer])

  case class Kandidat(wettkampfTitel: String, geschlecht: String, programm: String,
                      id: Long, name: String, vorname: String, jahrgang: String, verein: String,
                      riege: String, durchgang: String, start: String, diszipline: Seq[String])

  val intro = "{\n"
  val outro = "}"

  private def anmeldeListe(kategorie: String, kandidaten: Seq[Kandidat]) = {

    val d = kandidaten.map{kandidat =>
      s"""      {
         |        "verein" : "${kandidat.verein}",
         |        "athlet" : "${kandidat.name} ${kandidat.vorname} (${kandidat.jahrgang})",
         |        "athletid" : ${kandidat.id},
         |        "durchgang" : "${kandidat.durchgang}",
         |        "start" : "${kandidat.start}"
         |      }""".stripMargin
    }
    val dt = d.mkString("[\n", ",\n", "]\n")
    s"""  {
       |    "programm" : "${kategorie}",
       |    "teilnehmer" : $dt
       |  }""".stripMargin
  }

  def riegenToKategorienListeAsJSON(riegen: Seq[GeraeteRiege], logo: File): String = {
    val kandidaten = riegen
        .filter(riege => riege.halt == 0)
        // filter hauptdurchgang-startgeraet
        .filter(riege => !riege.kandidaten.exists(k => k.einteilung2.exists(d => d.start == riege.disziplin)))
        .flatMap(riege => {
          riege.kandidaten
            .map(kandidat => {
            Kandidat(riege.wettkampfTitel, kandidat.geschlecht, kandidat.programm, kandidat.id,
              kandidat.name, kandidat.vorname, kandidat.jahrgang, kandidat.verein, "",
              riege.durchgang.get, riege.disziplin.get.easyprint, Seq.empty)
          })
        })

    toJSONasKategorienListe(kandidaten, logo)
  }

  def toJSONasKategorienListe(kandidaten: Seq[Kandidat], logo: File): String = {
    val logoHtml = if (logo.exists()) logo.imageSrcForWebEngine else ""
    val kandidatenPerKategorie = kandidaten.sortBy { k =>
      val krit = f"${k.verein}%-40s ${k.name}%-40s ${k.vorname}%-40s"
      //logger.debug(krit)
      krit
    }.groupBy(k => k.programm)
    val rawpages = for {
      kategorie <- kandidatenPerKategorie.keys.toList.sorted
    }
    yield {
      anmeldeListe(kategorie, kandidatenPerKategorie(kategorie))
    }

    val pages = rawpages.mkString(s""""logo" : "$logoHtml",
                                     |  "title" : "${kandidaten.head.wettkampfTitel}",
                                     |  "programme" : [\n""".stripMargin, ",\n", "]\n")
    intro + pages + outro
  }
}