package ch.seidel.kutu

import ch.seidel.kutu.actors.{AthletIndexActor, KuTuMailerActor, ResyncIndex, SimpleMail}
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.http.{AuthSupport, Core, Hashing, KuTuAppHTTPServer}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object KuTuServer extends App with KuTuAppHTTPServer with AuthSupport with Hashing {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val binding = startServer()

  import Core._

  implicit val executionContext: ExecutionContext = system.dispatcher

  override def shutDown(caller: String): Unit = {
    if (binding != null) {
      logger.info(s"$caller: Server stops ...")
      binding.flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete { done =>
          done.failed.map { ex => log.error(ex, "Failed unbinding") }
        }
    }
    super.shutDown("KuTuServer")
  }

  logger.info("initial cleanup athletes ...")
  val cleanedAthletes = markAthletesInactiveOlderThan(3)
  logger.info("initial cleanup clubs ...")
  val cleanedClubs = cleanUnusedClubs()
  logger.info("initial cleanup media files ...")
  ResourceExchanger.cleanupMediaFiles()

  KuTuMailerActor.send(SimpleMail("Server-Startup Mailsend-Test", s"""
    Mailsender of ${KuTuMailerActor.mailSenderAppName} has been started up.

    $cleanedAthletes have been archived.
    ${cleanedClubs.size} Cleaned Clubs:
    ${cleanedClubs.mkString("\n    ")}
    """, s"${KuTuMailerActor.props().args(2)}@${KuTuMailerActor.props().args(3)}")).onComplete {
    println("test sendmail completed")
    println(_)
  }

  Future {
    logger.info(s"Server started\ntype 'quit' to stop...")
    while (
      StdIn.readLine() match {
        case s: String if (s.endsWith("quit")) =>
          shutDown("KuTuServer")
          false

        case s: String if (s.endsWith("showsecret")) =>
          println(Config.jwtSecretKey)
          true

        case s: String if (s.endsWith("cleanathletes")) =>
          markAthletesInactiveOlderThan(3)
          cleanUnusedClubs()
          println("done")
          true

        case s: String if (s.endsWith("wkmeta")) =>
          addMissingWettkampfMetaData()
          println("done")
          true

        case s: String if (s.endsWith("refresh")) =>
          AthletIndexActor.publish(ResyncIndex)
          println("done")
          true
        case s: String if (s.startsWith("sendmail")) =>
          KuTuMailerActor.send(SimpleMail("Mailsend-Test", s, "btv@interpolar.ch")).onComplete {
            println(_)
          }
          true
        case s: String =>
          println(
            s"""command unknown: '$s'
               |type 'quit' to stop the server
               |     'refresh' to refresh Athlet Matching Index
               |     'showsecret' to print secret to the console
               |     'cleanathletes' to cleanup (move) inactive athletes to inatctiv state
               |     'wkmeta' add missing meta-tuples for each competition
               |""".stripMargin)
          true
        //      case s =>
        //        println(s"cached unknown comand: '$s'")
        case _ =>
          // on linux, readLine doesn't block
          Thread.sleep(5000)
          true
      }
    ) {}
  }
}
