package ch.seidel.kutu.renderer

import ch.seidel.kutu.Config

import java.io.File
import ch.seidel.kutu.Config.{homedir, remoteBaseUrl, remoteHostOrigin}
import ch.seidel.kutu.KuTuApp.enc
import ch.seidel.kutu.data.{ByGeschlecht, ByProgramm, GroupSection}
import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.PrintUtil._
import net.glxn.qrgen.QRCode
import net.glxn.qrgen.image.ImageType
import org.slf4j.LoggerFactory

import java.sql.Date
import java.time.{LocalDate, Period}
import scala.collection.immutable._

trait WettkampfOverviewToHtmlRenderer {
  private val logger = LoggerFactory.getLogger(classOf[WettkampfOverviewToHtmlRenderer])
  private val intro2 = """<html><body><ul><li>"""
  private val intro = s"""<html lang="de-CH"><head>
          <meta charset="UTF-8" />
          <style type="text/css">
            @media print {
              body {
                -webkit-print-color-adjust: economy;
              }
              ul {
                page-break-inside: avoid;
              }
            }
            @media only screen {
              body {
                 margin: 15px 15px 15px 20px;
              }
            }
            body {
              font-family: "Arial", "Verdana", sans-serif;
              /*-webkit-print-color-adjust: economy;*/
            }
            h1 {
              font-size: 32px;
            }
            h2 {
              font-size: 15px;
            }
            h3 {
              font-size: 14px;
            }
            h4 {
              font-size: 13px;
            }
            p {
              font-size: 12px;
            }
            table{
              border-collapse:collapse;
              border-spacing:0;
              border: 0px; /*1px solid rgb(50,100,150);*/
              /*border-width: thin;*/
            }
            thead {
              border-bottom: 1px solid gray;
            }
            th {
              background-color: rgb(250,250,200);
              font-size: 10px;
              overflow: hidden;
            }
            td {
              font-size: 10px;
              padding:0.2em;
              overflow: hidden;
              white-space: nowrap;
            }
            td.data {
              //text-align: right
            }
            td.valuedata, td.valuedata.blockstart {
              font-size: 11px;
              text-align: right
            }
            td.link td>a {
              font-size: 12px;

            }
            td.hintdata {
              color: rgb(50,100,150);
              font-size: 9px;
              text-align: right
            }
            tbody tr:not(:last-child) > td {
              border-bottom: solid lightgray;
              border-bottom-width: thin;
            }
            tfoot {
              border-top: 1px solid gray;
            }
            tfoot td, tfoot td.tuti.blockstart {
              background-color: rgb(250,250,200);
              font-size: 10px;
              font-weight: bold;
              border-top: 0px
              border-bottom: 0px
              overflow: hidden;
              text-align: center;
            }
            tr:nth-child(even) {background: rgba(230, 230, 230, 0.6);}
            tr .blockstart:not(:first-child) {
              border-left: 1px solid lightgray;
            }
            li {
              font-size: 12px;
            }
            .headline {
              display: block;
              border: 0px;
              overflow: auto;
            }
            .wordwrapper {
              word-wrap:break-word;
            }
            .logo {
              float: right;
              max-height: 100px;
              border-radius: 5px;
            }
            .qrcode {
              float: right;
              max-height: 200px;
            }
            .showborder {
              padding: 1px;
              border: 1px solid rgb(50,100,150);
              border-radius: 5px;
            }
          </style>
          </head><body><ul><li>
  """

  val outro = """
    </li></ul></body>
    </html>
  """

  private def blatt(wettkampf: WettkampfView, programme: Seq[(String, Int, Int, Int)], vereinRows: List[(String, Map[String, (Int, Int)], Int, Int)], wertungen: List[WertungView], logo: File) = {
    val programHeader1 = programme.map(p => escaped(p._1))
      .mkString("<th class='blockstart' colspan='2'>", "</th><th class='blockstart' colspan='2'>", "</th>")
    val programHeader2 = programme.map(_ => "Ti</th><th>Tu")
      .mkString("<th class='blockstart'>", "</th><th class='blockstart'>", "</th>")
    val rows = vereinRows.map(v =>
      s"""<tr><td class='data'>${escaped(v._1)}</td>${
        programme.map(p =>
          s"""${
            v._2.getOrElse(p._1, (0,0))._2
          }</td><td class='valuedata'>${
            v._2.getOrElse(p._1, (0,0))._1
          }</td>"""
        ).mkString("<td class='valuedata blockstart'>", "</td><td class='valuedata blockstart'>", "</td>")
      }<td class='valuedata blockstart'>${v._4}</td><td class='valuedata'>${v._3}</td><td class='valuedata'>${v._3 + v._4}</td></tr>"""
    ).mkString("")

    val totalDetails = programme.map(p => s"${p._4}</td><td class='valuedata'>${p._3}")
      .mkString("<td class='valuedata blockstart'>", "</td><td class='valuedata blockstart'>", "</td>")

    val totalTuTiDetails = programme.map(p => s"${p._3 + p._4}")
      .mkString("<td class='tuti blockstart' colspan='2'>", "</td><td class='tuti blockstart' colspan='2'>", "</td>")

    val tiSum = programme.map(_._4).sum
    val tuSum = programme.map(_._3).sum
    val totSum = tiSum + tuSum

    val logoHtml = (if (logo.exists) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else s"")
    val hasRemote = wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin) || wettkampf.toWettkampf.hasRemote(homedir, remoteHostOrigin)
    val isLocalServer = Config.isLocalHostServer
    val registrationURL = s"$remoteBaseUrl/registration/${wettkampf.uuid.get}"
    val regQRUrl = toQRCodeImage(registrationURL)

    val startlistURL = s"$remoteBaseUrl/api/report/${wettkampf.uuid.get}/startlist?gr=verein&html"
    val startlistQRUrl = toQRCodeImage(startlistURL)
    val lastResultsURL = s"$remoteBaseUrl/?" + new String(enc.encodeToString((s"last&c=${wettkampf.uuid.get}").getBytes))
    val lastQRUrl = toQRCodeImage(lastResultsURL)

    val auszSchwelle = (if (wettkampf.auszeichnung > 100) {
      wettkampf.auszeichnung / 100d
    } else {
      wettkampf.auszeichnung * 1d
    }) /100
    val auszeichnung = if (wettkampf.auszeichnung > 100) {
      dbl2Str(wettkampf.auszeichnung / 100d) + "%"
    }
    else {
      s"${wettkampf.auszeichnung}%"
    }
    val medallienbedarf = programme.map{p =>
      (p._1, if (p._4 > 0) 1 else 0, if (p._4 > 1) 1 else 0, if (p._4 > 2) 1 else 0, Math.max(Math.floor(p._4*auszSchwelle-3), 0).toInt,
             if (p._3 > 0) 1 else 0, if (p._3 > 1) 1 else 0, if (p._3 > 2) 1 else 0, Math.max(Math.floor(p._3*auszSchwelle-3), 0).toInt)
    }
    val auszHint = if (wettkampf.auszeichnungendnote.compare(BigDecimal.valueOf(0)) != 0)
      s"<em>Auszeichnungs-Mindes-Notenschnitt: ${wettkampf.auszeichnungendnote}</em>"
    else
      s"<em>Auszeichnungs-Schwelle: ${auszeichnung}</em>"

    val medallienHeader1 = medallienbedarf.map(p => p._1)
      .mkString("<th class='blockstart' colspan='2'>", "</th><th class='blockstart' colspan='2'>", "</th>")

    val medallienHeader2 = medallienbedarf.map(p => "Ti</th><th>Tu")
      .mkString("<th class='blockstart'>", "</th><th class='blockstart'>", "</th>")

    val goldDetails = medallienbedarf.map(p => s"${p._2}</td><td class='valuedata'>${p._6}")
      .mkString("<td class='blockstart valuedata'>", "</td><td class='blockstart valuedata'>", "</td>")

    val silverDetails = medallienbedarf.map(p => s"${p._3}</td><td class='valuedata'>${p._7}")
      .mkString("<td class='blockstart valuedata'>", "</td><td class='blockstart valuedata'>", "</td>")

    val bronzeDetails = medallienbedarf.map(p => s"${p._4}</td><td class='valuedata'>${p._8}")
      .mkString("<td class='blockstart valuedata'>", "</td><td class='blockstart valuedata'>", "</td>")

    val auszDetails = medallienbedarf.map(p => s"${p._5}</td><td class='valuedata'>${p._9}")
      .mkString("<td class='blockstart valuedata'>", "</td><td class='blockstart valuedata'>", "</td>")

    val goldSum = medallienbedarf.map(p => p._2 + p._6).sum
    val silverSum = medallienbedarf.map(p => p._3 + p._7).sum
    val bronzeSum = medallienbedarf.map(p => p._4 + p._8).sum
    val auszSum = medallienbedarf.map(p => p._5 + p._9).sum
    val wkAlterklassen = Altersklasse(wettkampf.altersklassen)
    val altersklassen = wkAlterklassen.map(ak => s"<li>${escaped(ak.easyprint)}</li>").mkString("\n")
    val wkJGAltersklassen: Seq[Altersklasse] = Altersklasse(wettkampf.jahrgangsklassen)
    val jgAltersklassen = wkJGAltersklassen.map(ak => s"<li>${escaped(ak.easyprint)}</li>").mkString("\n")

    val teams = TeamRegel(wettkampf.teamrule)
    val teamsBlock = if (teams.teamsAllowed) {
      val groupedWertungen = if (jgAltersklassen.nonEmpty) {
        wertungen.groupBy { w =>
          val programm = w.wettkampfdisziplin.programm
          val gebdat: LocalDate = w.athlet.gebdat.getOrElse(new Date(System.currentTimeMillis())).toLocalDate
          val geschlecht = w.athlet.geschlecht
          val wkd: LocalDate = wettkampf.datum
          val alter = wkd.getYear - gebdat.getYear
          val jgak = Altersklasse(wkJGAltersklassen, alter, geschlecht, programm)
          (programm.easyprint, jgak.easyprint, "")
        }
      }
      else if (altersklassen.nonEmpty) {
        wertungen.groupBy { w =>
          val programm = w.wettkampfdisziplin.programm
          val gebdat: LocalDate = w.athlet.gebdat.getOrElse(new Date(System.currentTimeMillis())).toLocalDate
          val geschlecht = w.athlet.geschlecht
          val wkd: LocalDate = wettkampf.datum
          val alter = Period.between(gebdat, wkd.plusDays(1)).getYears
          val ak = Altersklasse(wkAlterklassen, alter, geschlecht, programm)
          (programm.easyprint, ak.easyprint, "")
        }
      } else {
        wertungen.groupBy(w => (w.wettkampfdisziplin.programm.easyprint, "", w.athlet.geschlecht))
      }
      val groupedTeams = groupedWertungen.toList.flatMap {
        case (group, wertungen) => teams.extractTeams(wertungen).groupBy(_.rulename).map {
          case (rulename, teams) =>
            val (pgm, ak, sex) = group
            (rulename, pgm, ak, sex, teams.size, teams.map(_.name).mkString(", "))
        }
      }.groupBy(_._1).map{
        case (rulename, list) =>
          (rulename, list.sortBy(t => s"${t._1}${t._2}${t._3}"))
      }
      val teamsSections = groupedTeams.keySet.toList.sorted.map {
        case name =>
          s"""<h3>$name</h3><br>
             |<div class="showborder"><table width=100%>
             |<thead>
             |  <tr class='head'><th>Prog./Kat.</th><th class='blockstart'>AK</th><th class='blockstart'>Geschlecht</th><th class='blockstart'>Anzahl Teams</th><th class='blockstart'>Teams</th></tr>
             |</thead><tbody>
             |${
            groupedTeams(name).map {
              case (_, pgm: String, ak: String, sex: String, teamssize: Int, teams: String) =>
                s"<tr><td class='data'>$pgm</td><td class='blockstart data'>$ak</td><td class='blockstart data'>$sex</td><td class='blockstart valuedata'>$teamssize</td><td class='blockstart data'>${teams.replace(",", "<br>")}</td></tr>"
            }.mkString
          }
             |</tbody></table></div>
             |""".stripMargin
      }.mkString("\n")

      s"""<h2>Teams / Mannschaften</h2>
          <p>Die Team-Zuteilungen pro Teilnehmer/-In werden hier im Kontext der eingestellten Team-Regel sichtbar.<br>
          Falls es Gruppen gibt, in denen weniger als 2 Teams aufgeführt sind, kann bei der Ranglisteneinstellung
          über die <em>Filter Alle</em> Funktion Gruppen zusammengefasst werden.</p>
      $teamsSections
      """
    } else {
      ""
    }
    val medalrows = s"""
    <tr><td class='data'>Goldmedallie</td>${goldDetails}<td class='blockstart valuedata'>${goldSum}</td></tr>
    <tr><td class='data'>Silbermedallie</td>${silverDetails}<td class='blockstart valuedata'>${silverSum}</td></tr>
    <tr><td class='data'>Bronzemedallie</td>${bronzeDetails}<td class='blockstart valuedata'>${bronzeSum}</td></tr>
    <tr><td class='data'>Ab 4. Rang</td>${auszDetails}<td class='blockstart valuedata'>${auszSum}</td></tr>
    """

    s"""<div class=blatt>
      <div class=headline>
        $logoHtml
        <h1>Wettkampf-Übersicht</h1><h2>${escaped(wettkampf.easyprint)}</h2></div>
      </div>
      <h2>Anmeldungen</h2>
      <div class=headline>
        ${
        if (!isLocalServer) {
          if (hasRemote)
            s"""<img class=qrcode src="$regQRUrl"/>
                <h3>Wettkampf-Registrierung / Online-Anmeldungen</h3>
                  <p class=wordwrapper>Zum Versenden an die Vereinsverantwortlichen oder für in die Wettkampf-Ausschreibung.<br>
                    <a href="$registrationURL" target="_blank">$registrationURL</a>
                  </p>"""
          else
            s"""<h3>Wettkampf-Registrierung / Online-Anmeldungen</h3>
                <p class=wordwrapper>
                  Der Wettkampf ist nur lokal gespeichert, resp. noch nicht auf den Server ${Config.remoteHost} hochgeladen.</p>
                <p class=wordwrapper>
                  Folgende Online-Funktionen sind nur verfügbar, wenn der Wettkampf hochgeladen wird. <em>(siehe Funktion "Upload")</em>
                  <ul>
                    <li>Anmeldungen über die Vereinsverantwortlichen</li>
                    <li>Online Resultat-Erfassung über die Wertungsrichter</li>
                    <li>Online Rangliste bereitstellen</li>
                  </ul>
                  </p>
                """
          } else ""
        }
        ${
        if (!isLocalServer) {
          s"""<h3>EMail des Wettkampf-Administrators</h3>
            <p>An diese EMail Adresse werden Notifikations-Meldungen versendet, sobald sich an den Anmeldungen Mutationen ergeben.<br>
              ${if (wettkampf.notificationEMail.nonEmpty) s"""<a href="mailto://${escaped(wettkampf.notificationEMail)}" target="_blank">${escaped(wettkampf.notificationEMail)}</a>""" else "<strong>Keine EMail hinterlegt!</strong>"}
          """
        } else ""
        }
        </p>
        ${if (altersklassen.nonEmpty)
          s"""<h2>Altersklassen</h2>
          Alter am Wettkampf - Tag: ${escaped(wettkampf.altersklassen)} <br>
          <ul>${altersklassen}
          </ul>"""
          else if (jgAltersklassen.nonEmpty)
          s"""<h2>Altersklassen</h2>
          Alter im Wettkampf - Jahr: ${escaped(wettkampf.jahrgangsklassen)} <br>
          <ul>${jgAltersklassen}
          </ul>"""
          else ""
        }
        <h3>Zusammenstellung der Anmeldungen</h3>
      </div>
      <div class="showborder">
        <table width="100%">
          <thead>
            <tr class='head'><th>&nbsp;</th>${programHeader1}<th class='blockstart' colspan="3">Total</th></tr>
            <tr class='head'><th>Verein</th>${programHeader2}<th class='blockstart'>Ti</th><th>Tu</th><th>Total</th></tr>
          </thead>
          <tbody>
          ${rows}
          </tbody>
          <tfoot>
          <tr><td class="data">Total</td>${totalDetails}<td class='valuedata blockstart'>${tiSum}</td><td class="valuedata">${tuSum}</td><td class="valuedata">${totSum}</td></tr>
          <tr><td class="data">Total Ti & Tu</td>${totalTuTiDetails}<td class='tuti blockstart' colspan='3'>&nbsp;</td></tr>
          </tfoot>
        </table>
      </div>
      $teamsBlock
      <h2>Medallien-Bedarf</h2>
        ${auszHint}
        <div class="showborder">
        <table width="100%">
          <thead>
            <tr class='head'><th>&nbsp;</th>${medallienHeader1}<th class='blockstart'>&nbsp;</th></tr>
            <tr class='head'><th>Auszeichnung</th>${medallienHeader2}<th class='blockstart'>Total</th></tr>
          </thead>
            <tbody>
            $medalrows
            </tbody>
        </table><br>
      </div>
      <em>(Ohne Reserven)</em>
      ${ if (hasRemote)
        s"""
        <h2 id="usefullinks">Weitere nützliche Links</h2>
        <div class=headline>
          <img class=qrcode src="$startlistQRUrl"/>
          <h3>Online Liste der Teilnehmer/-Innen</h3><p class=wordwrapper>
          Liste aller angemeldeten Teilnehmer/-Innen mit ihrer Starteinteilung.<br>
          <a href="$startlistURL" target="_blank">$startlistURL</a>
          </p>
        </div>
        <div class=headline>
          <img class=qrcode src="$lastQRUrl"/>
          <h3>Online Wettkampfresultate</h3><p class=wordwrapper>Direkter Link in die Online App, wo die letzten Resultate publiziert werden.<br>
          <a href="$lastResultsURL" target="_blank">$lastResultsURL</a>
        </div>"""
      }
      </div>
    """
  }

  val nextSite = "</li></ul><ul><li>\n"
  
  val pageIntro = "<table width='100%'><tr><td>"
  val pageOutro = "</td></tr></table>"
  def toHTML(wettkampf: WettkampfView, stats: List[OverviewStatTuple], wertungen: List[WertungView], logo: File): String = {
    val programme: Seq[(String, Int, Int, Int)] = stats
      .groupBy(t => (t._2, t._3))
      .map(t => (t._1._1, t._1._2, t._2.map(_._4).sum, t._2.map(_._5).sum))
      .toList.sortBy(_._2)
    val vereinRows: List[(String, Map[String, (Int, Int)], Int, Int)] = stats
      .groupBy(_._1)
      .view.mapValues(_.groupBy(_._2).view.mapValues(stats => (stats.head._4, stats.head._5)))
      .map(t => (t._1, t._2.toMap, t._2.values.map(_._1).sum, t._2.values.map(_._2).sum))
      .toList
      .sortBy(_._1)

    intro + blatt(wettkampf, programme, vereinRows, wertungen, logo) + outro
  }

}