package ch.seidel.kutu.renderer

import ch.seidel.kutu.Config
import ch.seidel.kutu.akka.{Mail, MultipartMail}
import ch.seidel.kutu.domain.{JudgeRegistration, Registration, SyncAction, Wettkampf}
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

  def createSyncNotificationMail(wettkampf: Wettkampf, syncActions: List[SyncAction], changedJudges: List[JudgeRegistration], removedJudges: List[JudgeRegistration], addedJudges: List[JudgeRegistration]): Mail = {
    val logodir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
    val logofile = PrintUtil.locateLogoFile(logodir)
    val logoHtml = if (logofile.exists()) s"""<img class=logo src="${logofile.imageSrcForWebEngine}" title="Logo"/>""" else ""
    MultipartMail("Mutationen bei Wettkampfanmeldungen",
      s"""Salut Wettkampf-Administrator/-In
         |
         |In der Zwischenzeit sind folgende Änderungen bei den Anmeldungen für den Wettkampf
         |** ${wettkampf.easyprint} **
         |nachzuführen:
         |${syncActions.groupBy(_.verein).map{ gr =>
            val (verein: Registration, actions: List[SyncAction]) = gr
            val actionstext = actions.map(action => s"""  ** ${escaped(action.caption)}""").mkString("\n|    ")
            val judgestext = (addedJudges.filter(j => j.vereinregistrationId == verein.id).map{j =>
              s"""  ** Wertungsrichter hinzufügen: ${escaped(j.vorname)} ${escaped(j.name)}, ${escaped(j.mail)}, ${escaped(j.mobilephone)}, ${escaped(j.comment)}"""
            } ++ removedJudges.filter(j => j.vereinregistrationId == verein.id).map{j =>
              s"""  ** Wertungsrichter entfernen: ${escaped(j.vorname)} ${escaped(j.name)}, ${escaped(j.mail)}, ${escaped(j.mobilephone)}, ${escaped(j.comment)}"""
            } ++ changedJudges.filter(j => j.vereinregistrationId == verein.id).map{j =>
              s"""  ** Wertungsrichter ändern: ${escaped(j.vorname)} ${escaped(j.name)}, ${escaped(j.mail)}, ${escaped(j.mobilephone)}, ${escaped(j.comment)}"""
            }).mkString("\n|    ")
        s"""  * ${escaped(verein.vereinname)} (${escaped(verein.respVorname)} ${escaped(verein.respName)})
               |  ${actionstext}
               |  ${judgestext}"""
          }.mkString("\n")}
         |
         |Falls Du Hilfe benötigst, findest Du hier die Anleitung dazu:
         |  https://luechtdiode.gitbook.io/turner-wettkampf-app/v/v2r2/wettkampf-vorbereitung/wettkampf_uebersicht/turneranmeldungen_verarbeiten_online#sync-registrations-1
         |
         |Dieses Mail wird erneut versendet, wenn sich weitere Änderungen ergeben.
         |
         |LG, die Kutuapp
         |
         |PS: Dies ist eine automatisch versendete EMail. Bitte nicht auf diese Mail antworten.""".stripMargin,
      s"""<html>$htmlhead<body>
         |    <div class=textbody>
         |      <div class=headline>
         |        $logoHtml
         |        <div class=title><h4>${escaped(wettkampf.easyprint)}</h4></div>
         |        <div class=subtitle>Mutationen bei Wettkampfanmeldungen</br></div>
         |      </div>
         |      <div class="textblock">
         |        <h4>Salut Wettkampf-Administrator/-In</h4>
         |        <p>
         |          In der Zwischenzeit sind folgende Änderungen bei den Anmeldungen für den Wettkampf
         |          <em>${wettkampf.easyprint}</em> nachzuführen:
         |        </p><ul>
         ${syncActions.groupBy(_.verein).map{ gr =>
                      val (verein: Registration, actions: List[SyncAction]) = gr
                      val actionstext = actions.map(action => s"""<li>${escaped(action.caption)}</li>""").mkString("\n|            ")
                      val judgestext = (addedJudges.filter(j => j.vereinregistrationId == verein.id).map{j =>
                        s"""<li>Wertungsrichter hinzufügen: ${escaped(j.vorname)} ${escaped(j.name)}, ${escaped(j.mail)}, ${escaped(j.mobilephone)}, ${escaped(j.comment)}</li>"""
                      } ++ removedJudges.filter(j => j.vereinregistrationId == verein.id).map{j =>
                        s"""<li>Wertungsrichter entfernen: ${escaped(j.vorname)} ${escaped(j.name)}, ${escaped(j.mail)}, ${escaped(j.mobilephone)}, ${escaped(j.comment)}</li>"""
                      } ++ changedJudges.filter(j => j.vereinregistrationId == verein.id).map{j =>
                        s"""<li>Wertungsrichter ändern: ${escaped(j.vorname)} ${escaped(j.name)}, ${escaped(j.mail)}, ${escaped(j.mobilephone)}, ${escaped(j.comment)}</li>"""
                      }).mkString("\n|            ")
     s"""|          <li>${escaped(verein.vereinname)} (${escaped(verein.respVorname)} ${escaped(verein.respName)})<br>
         |            <ul>
         |              ${actionstext}
         |              ${judgestext}
         |            </ul>
         |          </li>"""
                  }.mkString("\n")}
         |        </ul><p>
         |          Falls Du Hilfe benötigst, findest Du hier die Anleitung dazu:
         |          <a href="https://luechtdiode.gitbook.io/turner-wettkampf-app/v/v2r2/wettkampf-vorbereitung/wettkampf_uebersicht/turneranmeldungen_verarbeiten_online#sync-registrations-1" target="_blank">Online Bedienungsanleitung</a>
         |        </p><p>
         |          Dieses Mail wird erneut versendet, wenn sich weitere Änderungen ergeben.
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
      wettkampf.notificationEMail)
  }

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
