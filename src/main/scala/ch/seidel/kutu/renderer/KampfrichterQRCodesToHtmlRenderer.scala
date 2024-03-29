package ch.seidel.kutu.renderer

import java.io.File
import java.util.Base64

import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.PrintUtil._
import net.glxn.qrgen.QRCode
import net.glxn.qrgen.image.ImageType
import org.slf4j.LoggerFactory

case class WertungsrichterQRCode(wettkampfTitle: String, durchgangname: String, geraet: String, uri: String, imageData: String)

object WertungsrichterQRCode {
  val logger = LoggerFactory.getLogger(this.getClass)
  val enc = Base64.getUrlEncoder
  
  def toURI(uuid: String, remoteBaseUrl: String, gr: GeraeteRiege) =
    s"$remoteBaseUrl?" + new String(enc.encodeToString((s"c=$uuid&d=${encodeURIComponent(gr.durchgang.get)}&g=${gr.disziplin.get.id}").getBytes))

  def toURI(remoteBaseUrl: String, gr: GeraeteRiege) =
    s"$remoteBaseUrl?" + new String(enc.encodeToString((s"c=${gr.wettkampfUUID}&d=${encodeURIComponent(gr.durchgang.get)}&st=${gr.halt +1}&g=${gr.disziplin.get.id}").getBytes))

  def toMobileConnectData(wettkampf: WettkampfView, baseUrl: String)(gr: GeraeteRiege) =
    WertungsrichterQRCode(wettkampf.titel, gr.durchgang.get, gr.disziplin.get.name, toURI(wettkampf.uuid.get, baseUrl, gr), toQRCodeImage(toURI(wettkampf.uuid.get, baseUrl, gr)))
}

trait WertungsrichterQRCodesToHtmlRenderer {

  val intro = """<html>
    <head>
      <meta charset="UTF-8" />
      <style>
        @media print {
          body { -webkit-print-color-adjust: economy; }
          ul {
            page-break-inside: avoid;
          }
        }
        .qrcodeblatt {
          display: block;
          padding: 15px;
          padding-left: 40px;
          margin-top: 5px;
          margin-left: 5px;
        }
        .headline {
          display: block;
          border: 0px;
          overflow: auto;
        }
        .logo {
          float: left;
          height: 50px;
          border-radius: 5px;
        }
        .durchgang {
          text-align: right;
          float: right;
          font-size: 24px;
          font-weight: 600;
        }
        .geraet {
          text-align: right;
          float: right;
          font-size: 16px;
          font-weight: 600;
        }
        .sf {
          font-size: 9px;
        }
        .showborder {
          margin-top: 8px;
          padding: 5px;
          border: 1px solid black;
          border-radius: 5px;
        }
        .turnerRow {
          border-bottom: 1px solid #ddd;
        }
        .totalRow {
          border-bottom: 1px solid #000;
        }
        .heavyRow {
          font-weight: bolder;
        }
        .totalCol {
          border-left: 1px solid #000;
        }
        .large {
          padding: 8px;
          padding-top: 10px;
          padding-bottom: 10px;
        }
        body {
          font-family: "Arial", "Verdana", sans-serif;
        }
        h1 {
          font-size: 75%;
        }
        table {
          border-collapse:collapse;
          border-spacing:0;
        }
        tr {
          font-size: 14px;
          overflow: hidden;
        }
        td {
          padding: 5px;
        }
        ul {
          margin: 0px;
          padding: 0px;
          border: 0px;
          list-style: none;
          overflow: auto;
        }
        li {
          float: left;
        }
      </style>
    </head>
    <body><ul><li>
  """

  val outro = """
    </li></ul></body>
    </html>
  """

  private def renderedDurchgaenge(geraetCodes: (String, String, Seq[WertungsrichterQRCode]), logo: File) = {
    val logoHtml = if (logo.exists()) s"""<img class=logo src="${logo.imageSrcForWebEngine}" title="Logo"/>""" else ""
    val (wettkampfTitel, geraet, codes) = geraetCodes
    val sorted = codes.sortBy(_.durchgangname)
    val divided = sorted.take(sorted.size / 2).zipAll(sorted.drop(sorted.size / 2), WertungsrichterQRCode("", "", "", "", ""), WertungsrichterQRCode("", "", "", "", ""))
    val d = divided.map{durchgangspalten =>
      val (d1, d2) = durchgangspalten
      s"""<tr class="turnerRow">
            <td class="large"><a href='${d1.uri}'>${escaped(d1.durchgangname)}</a></td><td class="large"><img title='${d1.uri}' width='140px' height='140px' src='${d1.imageData}'></td>
            <td class="totalCol"><a href='${d2.uri}'>${escaped(d2.durchgangname)}</a></td><td class="large"><img title='${d2.uri}' width='140px' height='140px' src='${d2.imageData}'></td>
          </tr>"""
    }.mkString("", "\n", "\n")

    s"""<div class=qrcodeblatt>
      <div class=headline>
        $logoHtml
        <div class=geraet>${geraet}</div></div>
      </div>
      <h1>${escaped(wettkampfTitel)}</h1>
      <div class="showborder">
        <table width="100%">
          <tr class="totalRow heavyRow"><td>Durchgang</td><td>QRCode für Mobile-Connect</td><td class="totalCol">Durchgang</td><td>QRCode für Mobile-Connect</td></tr>
          ${d}
        </table>
      </div>
    </div>
    """
  }

  val fcs = 20

  def toHTML(qrCodes: Seq[WertungsrichterQRCode], logo: File): String = {
    val datenProGeraet = qrCodes.groupBy(_.geraet).map(geraetCodes => {
      val (geraet, codes) = geraetCodes
      (codes.head.wettkampfTitle, geraet, codes)
    })
    val blaetter = datenProGeraet.map(renderedDurchgaenge(_, logo))
    val pages = blaetter.sliding(1, 1).map { _.mkString("</li><li>") }.mkString("</li></ul><ul><li>")
    intro + pages + outro
  }
}