package ch.seidel.kutu.renderer

import ch.seidel.kutu.Config
import ch.seidel.kutu.akka.{Mail, MultipartMail}
import ch.seidel.kutu.domain.{Registration, Wettkampf}
import ch.seidel.kutu.renderer.PrintUtil._

object MailTemplates {
  val htmlhead =
    s"""    <head>
       |      <meta charset="UTF-8" />
       |      <style>
       |        .textbody {
       |          max-width: 50em;
       |          display: block;
       |          padding: 10px;
       |          padding-left: 50px;
       |          margin-top: 5px;
       |          margin-left: 5px;
       |        }
       |        .headline {
       |          display: block;
       |          border: 0px;
       |          overflow: auto;
       |        }
       |        .logo {
       |          float: right;
       |          height: 100px;
       |          border-radius: 5px;
       |        }
       |        .catchme {
       |          padding: 20px;
       |          margin: auto;
       |          text-align: center;
       |          //max-width: 80%;
       |          word-wrap:break-word;
       |          border: 1px solid blue;
       |          border-radius: 5px;
       |        }
       |        .title {
       |          float: left;
       |        }
       |        .subtitle {
       |          float: left;
       |          font-size: 24px;
       |          font-weight: 600;
       |        }
       |        .textblock {
       |          padding-top: 20px;
       |        }
       |        body {
       |          font-family: "Arial", "Verdana", sans-serif;
       |        }
       |      </style>
       |    </head>""".stripMargin

  def createPasswordResetMail(wettkampf: Wettkampf, registration: Registration, link: String): Mail = {
    val logodir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
    val logofile = PrintUtil.locateLogoFile(logodir)
    val logoHtml = if (logofile.exists()) s"""<img class=logo src="${logofile.imageSrcForWebEngine}" title="Logo"/>""" else ""
    val imageData = toQRCodeImage(link)
    MultipartMail("Kutuapp Passwort-Reset",
      s"""Hallo ${registration.respVorname}
         |
         |Jemand hat die Passwort-Reset Funktion bei deiner Wettkampf-Registrierung angewendet.
         |Falls das nicht du warst, kannst du dieses Mail ignorieren.
         |
         |Mit dem folgenden Link kann in den nächsten 24h das Login
         | * für den Verein ${registration.vereinname} (${registration.verband})
         | * zur Wettkampf-Anmeldung '${wettkampf.easyprint}'
         |gemacht werden.
         |
         |${link}
         |
         |Hiermit kannst du das Formular für deine Vereinsregistrierung öffnen und das Passwort neu setzen.
         |
         |LG, die Kutuapp
         |
         |PS: Dies ist eine automatisch versendete EMail. Bitte nicht auf diese Mail antworten.""".stripMargin,
      s"""<html>$htmlhead<body>
         |    <div class=textbody>
         |      <div class=headline>
         |        $logoHtml
         |        <div class=title><h4>${escaped(wettkampf.easyprint)}</h4></div>
         |        <div class=subtitle>Login-Problem bei Wettkampf-Registrierung</br></div>
         |      </div>
         |      <div class="textblock">
         |        <h4>Hallo ${escaped(registration.respVorname)}</h4>
         |        <p>
         |          Jemand hat die Passwort-Reset Funktion bei deiner Wettkampf-Registrierung angewendet.
         |          Falls das nicht du warst, kannst du dieses Mail ignorieren.
         |        </p><p>
         |          Mit dem folgenden Link kann in den nächsten 24h das Login
         |          <ul>
         |            <li>für den Verein <b>${escaped(registration.vereinname)} (</b><em>${escaped(registration.verband)}</em><b>)</b></li>
         |            <li>zur Wettkampf-Anmeldung <b>${escaped(wettkampf.easyprint)}</b> </li>
         |          </ul>
         |          gemacht werden.
         |        </p><div class="catchme">
         |          <a href='${link}'>
         |            <h2>Link mit Berechtigung auf Online-Registrierung</h2>
         |            <img title='${link}' width='300px' height='300px' src='${imageData}'>
         |          </a><br>
         |          <a href='${link}'> ${link}</a>
         |        </div><p>
         |          Hiermit kannst du das Formular für deine Vereinsregistrierung öffnen und das Passwort neu setzen.
         |        </p><p>
         |          LG, die KuTu-App
         |        </p>
         |        <hr>
         |        <p>
         |           <b>PS:</b> <em>Dies ist eine automatisch versendete EMail. Bitte nicht auf diese Mail antworten.</em>
         |        </p>
         |      </div>
         |    </div>
         |</body></html>""".stripMargin,
      registration.mail)
  }
}
