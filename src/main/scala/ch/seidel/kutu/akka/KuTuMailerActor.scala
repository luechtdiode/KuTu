package ch.seidel.kutu.akka

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.pattern.ask
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.http.Core.system
import org.simplejavamail.api.mailer.{CustomMailer, Mailer}
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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

case class SendRetry(mail: Mail, retries: Int, sender: ActorRef) extends SendMailAction

class KuTuMailerActor(smtpHost: String, smtpPort: Int, smtpUsername: String, smtpDomain: String, smtpPassword: String, appname: String, customMailer: Option[CustomMailer])
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

  def mailer: Mailer = {
    val builder = MailerBuilder
      .withSMTPServerHost(smtpHost)
      .withSMTPServerPort(smtpPort)
      .withSMTPServerUsername(smtpMailerUser)
      .withSMTPServerPassword(smtpPassword)
      .withTransportStrategy(TransportStrategy.SMTPS)

    customMailer match {
      case None => builder.buildMailer()
      case Some(mailer) => builder.withCustomMailer(mailer).buildMailer()
    }
  }

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
      sender() ! StatusCodes.OK

    case _ =>
  }

  def receiveHot: Receive = {
    case mail: Mail =>
      val completionObserver: Try[String] => Unit = observeMailComletion(mail, 0, sender())
      send(mail).handleAsync {(v,e) =>
        if (e != null) {
          completionObserver(Failure(e))
        } else {
          completionObserver(Success("OK"))
        }
      }

    case SendRetry(action, retries, sender) => action match {
      case mail: Mail =>
        val completionObserver: Try[String] => Unit = observeMailComletion(mail, retries, sender)
        send(mail).handleAsync {(v,e) =>
          if (e != null) {
            completionObserver(Failure(e))
          } else {
            completionObserver(
              Success("OK"))
          }
        }
    }

    case _ =>
  }

  private def observeMailComletion(mail: Mail, retries: Int, sender: ActorRef): Try[String] => Unit = {
    case Success(_) =>
      log.info(s"mail ${mail.subject} to ${mail.to} delivered successfully")
      sender ! StatusCodes.OK
    case Failure(e) =>
      e.printStackTrace()
      if (retries < 3) {
        log.warning(s"mail ${mail.subject} to ${mail.to} delivery failed: " + e.toString)
        this.context.system.scheduler.scheduleOnce((5 * retries + 1) minutes, self, SendRetry(mail, retries + 1, sender))
      } else {
        log.error(s"could not send message ${mail.subject} after 3 retries to ${mail.to}")
        sender ! StatusCodes.ExpectationFailed
      }
  }

  private def send(mail: Mail) = {
    mail match {
      case SimpleMail(subject, messageText, to) =>
        mailer.sendMail(EmailBuilder.startingBlank()
          .from(appname, smtpMailerUser)
          .to(to)
          .withSubject(subject)
          .withPlainText(messageText)
          .buildEmail(), true)
      case MultipartMail(subject, messageText, messageHTML, to) =>
        mailer.sendMail(EmailBuilder.startingBlank()
          .from(appname, smtpMailerUser)
          .to(to)
          .withSubject(subject)
          .withPlainText(messageText)
          .withHTMLText(messageHTML)
          .buildEmail(), true)
    }
  }
}

object KuTuMailerActor {
  private var customMailer: Option[CustomMailer] = None;
  val mailSenderAppName = Config.config.getString("app.smtpsender.appname")

  def props(): Props = {
    if (isSMTPConfigured) {
      Props(classOf[KuTuMailerActor],
        Config.config.getString("X_SMTP_HOST"), Config.config.getInt("X_SMTP_PORT"),
        Config.config.getString("X_SMTP_USERNAME"), Config.config.getString("X_SMTP_DOMAIN"),
        Config.config.getString("X_SMTP_PASSWORD"), mailSenderAppName, customMailer
      )
    } else {
      Props(classOf[KuTuMailerActor],
        "undefined", 0,
        "undefined", "undefined",
        "", "Kutu-App Test",
        customMailer
      )
    }
  }

  def isSMTPConfigured = Config.config.hasPath("X_SMTP_USERNAME") &&
    Config.config.hasPath("X_SMTP_DOMAIN") &&
    Config.config.hasPath("X_SMTP_HOST") &&
    Config.config.hasPath("X_SMTP_PORT") &&
    Config.config.hasPath("X_SMTP_PASSWORD")

  def setProvider(customMailer: CustomMailer): Unit = {
    this.customMailer = Some(customMailer)
  }

  private lazy val kutuapMailer: ActorRef = system.actorOf(props(), name = "KutuappMailer")

  def send(mail: Mail): Future[StatusCode] = {
    implicit lazy val timeout: Timeout = Timeout(30 minutes)
    (kutuapMailer ? mail).asInstanceOf[Future[StatusCode]]
  }
}