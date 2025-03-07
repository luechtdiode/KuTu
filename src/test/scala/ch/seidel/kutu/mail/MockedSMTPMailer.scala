package ch.seidel.kutu.mail

import ch.seidel.kutu.actors.KuTuMailerActor
import jakarta.mail
import jakarta.mail.{MessagingException, internet}
import org.simplejavamail.api.email.Email
import org.simplejavamail.api.mailer.CustomMailer
import org.simplejavamail.api.mailer.config.OperationalConfig

import java.io.IOException

class MockedSMTPMailer extends CustomMailer {

  KuTuMailerActor.setProvider(this)

  @throws[IOException]
  def getTextFromMessage(message: internet.MimeMessage): String = {
    if (message.isMimeType("text/plain")) {
      message.getContent.toString
    }
    else if (message.isMimeType("multipart/*")) {
      val mimeMultipart = message.getContent.asInstanceOf[internet.MimeMultipart]
      getTextFromMimeMultipart(mimeMultipart)
    } else {
      "unkonwn messagetype " + message.getContentType
    }
  }

  @throws[IOException]
  def getTextFromMimeMultipart(mimeMultipart: internet.MimeMultipart): String = {
    var result = ""
    val count = mimeMultipart.getCount
    for (i <- 0 until count) {
      val bodyPart = mimeMultipart.getBodyPart(i)
      bodyPart.getContent match {
        case multipart: internet.MimeMultipart =>
          result = result + getTextFromMimeMultipart(multipart)
        case _ if (bodyPart.isMimeType("text/plain")) =>
          result = result + "\n" + bodyPart.getContent
        case _ if (bodyPart.isMimeType("text/html")) =>
          val html = bodyPart.getContent.asInstanceOf[String]
          result = result + "\n" + html
      }
    }
    result
  }

  override def testConnection(operationalConfig: OperationalConfig, session: mail.Session): Unit = {}

  override def sendMessage(operationalConfig: OperationalConfig, session: mail.Session, email: Email, mimeMessage: internet.MimeMessage): Unit = {
    email.getRecipients.forEach(recipient => {
      val mailbox = Mailbox.get(recipient.getAddress)
      if (mailbox.isError) throw new MessagingException("Simulated error sending message to " + recipient)
      mailbox.add(mimeMessage)
    })
  }
}