package ch.seidel.kutu.mail

import ch.seidel.kutu.akka.KuTuMailerActor
import org.jvnet.mock_javamail.{Aliases, Mailbox}
import org.simplejavamail.api.email.Email
import org.simplejavamail.api.mailer.CustomMailer
import org.simplejavamail.api.mailer.config.OperationalConfig

import java.io.IOException
import javax.mail.internet.{MimeMessage, MimeMultipart}
import javax.mail.{Message, MessagingException, Session}

class MockedSMTPMailer extends CustomMailer {

  KuTuMailerActor.setProvider(this)

  def testConnection(var1: OperationalConfig, var2: Session): Unit = {}

  def sendMessage(config: OperationalConfig, session: Session, mail: Email, mimeMessage: MimeMessage): Unit = {
    mail.getRecipients.forEach(recipient => {
      val mailbox = Mailbox.get(Aliases.getInstance.resolve(recipient.getAddress))
      if (mailbox.isError) throw new MessagingException("Simulated error sending message to " + recipient)
      mailbox.add(mimeMessage)
    })
  }

  @throws[MessagingException]
  @throws[IOException]
  def getTextFromMessage(message: Message): String = {
    if (message.isMimeType("text/plain")) {
      message.getContent.toString
    }
    else if (message.isMimeType("multipart/*")) {
      val mimeMultipart = message.getContent.asInstanceOf[MimeMultipart]
      getTextFromMimeMultipart(mimeMultipart)
    } else {
      "unkonwn messagetype " + message.getContentType
    }
  }

  @throws[MessagingException]
  @throws[IOException]
  def getTextFromMimeMultipart(mimeMultipart: MimeMultipart): String = {
    var result = ""
    val count = mimeMultipart.getCount
    for (i <- 0 until count) {
      val bodyPart = mimeMultipart.getBodyPart(i)
      bodyPart.getContent match {
        case multipart: MimeMultipart =>
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
}