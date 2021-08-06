package ch.seidel.kutu.renderer

import ch.seidel.kutu.akka.{Mail, SimpleMail}
import ch.seidel.kutu.domain.{Registration, Wettkampf}

object MailTemplates {

  def createPasswordResetMail(wettkampf: Wettkampf, registration: Registration, link: String): Mail = {
    SimpleMail(
      "Kutuapp Passwort-Reset",
      s"""  Hallo ${registration.respVorname}
         |  Mit dem folgenden Link kann in den nächsten 24h das Login
         |    für den Verein ${registration.vereinname} (${registration.verband})
         |    zur Wettkampf-Anmeldung '${ wettkampf.easyprint}' gemacht werden.
         |
         |  ${link}
         |
         |  Hiermit kannst du im Formular für die Vereinsregistrierung das Passwort neu setzen.
         |
         |  LG, die Kutuapp
         |
         |  PS: Dies ist eine automatisch versendete EMail. Bitte nicht auf diese Mail antworten.""".stripMargin,
      registration.mail)
  }
}
