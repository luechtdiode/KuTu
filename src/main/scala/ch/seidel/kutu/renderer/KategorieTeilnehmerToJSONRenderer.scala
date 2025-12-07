package ch.seidel.kutu.renderer

import ch.seidel.kutu.domain.{GeraeteRiege, SimpleDurchgang}
import ch.seidel.kutu.renderer.ServerPrintUtil.*
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.time.LocalDateTime

trait KategorieTeilnehmerToJSONRenderer {
  val logger: Logger = LoggerFactory.getLogger(classOf[KategorieTeilnehmerToJSONRenderer])

  val intro = "{\n"
  val outro = "}"
  import KategorieTeilnehmerToHtmlRenderer.getDurchgangFullName

  private def anmeldeListe(kategorie: String, kandidaten: Seq[Kandidat], dgMapping: Map[String, (SimpleDurchgang, LocalDateTime)]) = {

    val d = kandidaten.map{kandidat =>
      s"""      {
         |        "verein" : "${kandidat.verein}",
         |        "athlet" : "${kandidat.name} ${kandidat.vorname} (${kandidat.jahrgang})",
         |        "athletid" : ${kandidat.id},
         |        "durchgang" : "${kandidat.durchgang}",
         |        "durchgangtitle" : "${getDurchgangFullName(dgMapping, kandidat.durchgang)}",
         |        "start" : "${kandidat.start}",
         |        "team" : "${kandidat.team}"
         |      }""".stripMargin
    }
    val dt = d.mkString("[\n", ",\n", "]\n")
    s"""  {
       |    "programm" : "$kategorie",
       |    "teilnehmer" : $dt
       |  }""".stripMargin
  }


  def riegenToKategorienListeAsJSON(riegen: Seq[GeraeteRiege], logo: File, dgMapping: Seq[(SimpleDurchgang, LocalDateTime)]): String = {
    toJSONasKategorienListe(Kandidaten(riegen), logo, dgMapping)
  }

  private def toJSONasKategorienListe(kandidaten: Seq[Kandidat], logo: File, dgMapping: Seq[(SimpleDurchgang, LocalDateTime)]): String = {
    val logoHtml = if logo.exists() then logo.imageSrcForWebEngine else ""
    val dgmap = dgMapping.map(dg => dg._1.name -> dg).toMap
    val kandidatenPerKategorie = kandidaten.sortBy { k =>
      val krit = f"${escaped(k.verein)}%-40s ${escaped(k.name)}%-40s ${escaped(k.vorname)}%-40s"
      //logger.debug(krit)
      krit
    }.groupBy(k => k.programm)
    val rawpages = for
      kategorie <- kandidatenPerKategorie.keys.toList.sorted
    yield {
      anmeldeListe(kategorie, kandidatenPerKategorie(kategorie), dgmap)
    }

    val pages = rawpages.mkString(s""""logo" : "$logoHtml",
                                     |  "title" : "${kandidaten.head.wettkampfTitel}",
                                     |  "programme" : [\n""".stripMargin, ",\n", "]\n")
    intro + pages + outro
  }
}