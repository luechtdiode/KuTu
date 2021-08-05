package ch.seidel.kutu.akka

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.event.LoggingAdapter
import ch.seidel.kutu.Config
import ch.seidel.kutu.http.Core.system
import courier.Defaults._
import courier._

import scala.concurrent.duration.DurationInt
import scala.util._
import scala.util.control.NonFatal

sealed trait SendMailAction

sealed trait Mail extends SendMailAction {
  val subject: String
  val to: String
}

case class SimpleMail(override val subject: String, messageText: String, override val to: String) extends Mail

case class MultipartMail(override val subject: String, messageText: String, messageHTML: String, override val to: String) extends Mail

case class SendRetry(mail: Mail, retries: Int) extends SendMailAction

class KuTuMailerActor(smtpHost: String, smtpPort: Int, smtpUsername: String, smtpDomain: String, smtpPassword: String)
  extends Actor {
  lazy val l: LoggingAdapter = akka.event.Logging(system, this)

  object log {
    def error(s: String): Unit = l.error(s)

    def error(s: String, ex: Throwable): Unit = l.error(s, ex)

    def warning(s: String): Unit = l.warning(s)

    def info(s: String): Unit = l.info(s)

    def debug(s: String): Unit = l.debug(s)
  }

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error("Error in KuTuMailerActor", e)
      Restart
  }

  private val smtpMailerUser = s"$smtpUsername@$smtpDomain"

  lazy val mailer: Mailer = Mailer(smtpHost, smtpPort)
    .auth(true)
    .as(smtpMailerUser, smtpPassword)
    .ssl(true)()

  override def preStart(): Unit = {
    log.info(s"Start KuTuMailerActor")
    if (!smtpHost.equalsIgnoreCase("undefined") && smtpPort > 0) {
      this.context.become(receiveHot)
    } else {
      log.warning("No smtp environment configured. No mails will be sent!")
    }
  }

  override def postStop(): Unit = {
    log.info(s"Stop KuTuMailerActor")
  }

  override def receive: Receive = {
    case mail: Mail =>
      log.warning(s"smtp environment is not configured. Could not send $mail")

    case _ =>
  }

  def receiveHot: Receive = {
    case mail: Mail =>
      send(mail).onComplete(observeMailComletion(mail, 0))

    case SendRetry(action, retries) => action match {
      case mail: Mail =>
        send(mail).onComplete(observeMailComletion(mail, retries))
    }

    case _ =>
  }

  private def observeMailComletion(mail: Mail, retries: Int): Function1[Try[Unit], _] = {
    val completionObserver: Function1[Try[Unit], _] = {
      case Success(_) =>
        log.info(s"mail ${mail.subject} to ${mail.to} delivered successfully")
      case Failure(e) =>
        if (retries < 3) {
          log.warning(s"mail ${mail.subject} to ${mail.to} delivery failed: " + e.toString)
          this.context.system.scheduler.scheduleOnce((5 * retries) minutes, self, SendRetry(mail, retries + 1))
        } else {
          log.error(s"could not send message ${mail.subject} after 3 retries to ${mail.to}")
        }
    }
    completionObserver
  }

  private def send(mail: Mail) = {
    mail match {
      case SimpleMail(subject, messageText, to) =>
        mailer(
          Envelope.from(smtpMailerUser.addr)
            .to(to.addr)
            .subject(subject)
            .content(Text(messageText))
        )
      case MultipartMail(subject, messageText, messageHTML, to) =>
        mailer(
          Envelope.from(smtpMailerUser.addr)
            .to(to.addr)
            .subject(subject)
            .content(Multipart()
              .text(messageText)
              .html(messageHTML)
            )
        )
    }
  }
}

object KuTuMailerActor {

  def props() = {
    if (Config.config.hasPath("X_SMTP_USERNAME")
      && Config.config.hasPath("X_SMTP_DOMAIN")
      && Config.config.hasPath("X_SMTP_HOST")
      && Config.config.hasPath("X_SMTP_PORT")
      && Config.config.hasPath("X_SMTP_PASSWORD")
    ) {
      Props(classOf[KuTuMailerActor],
        Config.config.getString("X_SMTP_HOST"), Config.config.getInt("X_SMTP_PORT"),
        Config.config.getString("X_SMTP_USERNAME"), Config.config.getString("X_SMTP_DOMAIN"),
        Config.config.getString("X_SMTP_PASSWORD")
      )
    } else {
      Props(classOf[KuTuMailerActor],
        "undefined", 0,
        "undefined", "undefined",
        ""
      )
    }
  }

  private val kutuapMailer: ActorRef = system.actorOf(props(), name = "KutuappMailer")

  def send(mail: Mail): Unit = {
    kutuapMailer ! mail
  }
}