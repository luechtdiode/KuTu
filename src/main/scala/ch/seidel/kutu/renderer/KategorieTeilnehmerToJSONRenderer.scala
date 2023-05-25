package ch.seidel.kutu.renderer

import java.io.File
import ch.seidel.kutu.domain.GeraeteRiege
import ch.seidel.kutu.renderer.PrintUtil._
import org.slf4j.LoggerFactory

trait KategorieTeilnehmerToJSONRenderer {
  val logger = LoggerFactory.getLogger(classOf[KategorieTeilnehmerToJSONRenderer])

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
    toJSONasKategorienListe(Kandidaten(riegen), logo)
  }

  def toJSONasKategorienListe(kandidaten: Seq[Kandidat], logo: File): String = {
    val logoHtml = if (logo.exists()) logo.imageSrcForWebEngine else ""
    val kandidatenPerKategorie = kandidaten.sortBy { k =>
      val krit = f"${escaped(k.verein)}%-40s ${escaped(k.name)}%-40s ${escaped(k.vorname)}%-40s"
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