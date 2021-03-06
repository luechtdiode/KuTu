package ch.seidel.kutu.akka

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.pattern.ask
import akka.util.Timeout
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.Core.system
import ch.seidel.kutu.http.JsonSupport
import ch.seidel.kutu.view.{RegistrationAdmin, WettkampfInfo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

sealed trait RegistrationProtokoll

sealed trait RegistrationAction extends RegistrationProtokoll {
  val wettkampfUUID: String
}

sealed trait RegistrationEvent extends RegistrationProtokoll

case class RegistrationChanged(wettkampfUUID: String) extends RegistrationAction

case class AskRegistrationSyncActions(wettkampfUUID: String) extends RegistrationAction

case class RegistrationSyncActions(syncActions: Vector[SyncAction]) extends RegistrationEvent

class CompetitionRegistrationClientActor(wettkampfUUID: String) extends Actor with JsonSupport with KutuService {
  def shortName = self.toString().split("/").last.split("#").head + "/" + clientId()

  lazy val l = akka.event.Logging(system, this)

  object log {
    def error(s: String): Unit = l.error(s"[$shortName] $s")

    def error(s: String, ex: Throwable): Unit = l.error(s"[$shortName] $s", ex)

    def warning(s: String): Unit = l.warning(s"[$shortName] $s")

    def info(s: String): Unit = l.info(s"[$shortName] $s")

    def debug(s: String): Unit = l.debug(s"[$shortName] $s")
  }

  private val wettkampf = readWettkampf(wettkampfUUID)
  private val wettkampfInfo = WettkampfInfo(wettkampf.toView(readProgramm(wettkampf.programmId)), this)
  private var syncActions: Vector[SyncAction] = Vector()
  private var syncActionReceivers: List[ActorRef] = List()
  private var clientId: () => String = () => sender().path.toString

  override val supervisorStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error("Error in CompetitionRegistrationClientActor " + wettkampf, e)
      Restart
  }

  override def preStart(): Unit = {
    log.info(s"Starting CompetitionRegistrationClientActor for $wettkampf")
  }

  override def postStop(): Unit = {
    log.info(s"Stop CompetitionRegistrationClientActor for $wettkampf")
  }

  override def receive = {
    case RegistrationChanged(_) => retrieveSyncActions(sender())
    case AskRegistrationSyncActions(_) =>
      if (this.syncActions.nonEmpty)
        sender() ! RegistrationSyncActions(this.syncActions)
      else
        retrieveSyncActions(sender())
    case a@RegistrationSyncActions(actions) =>
      this.syncActions = actions
      if (syncActionReceivers.nonEmpty) {
        syncActionReceivers.foreach(_ ! a)
        syncActionReceivers = List()
      }
  }

  private def retrieveSyncActions(syncActionReceiver: ActorRef) = {
    syncActions = Vector()
    if (syncActionReceivers.nonEmpty) {
      syncActionReceivers = syncActionReceivers :+ syncActionReceiver
    } else {
      syncActionReceivers = syncActionReceivers :+ syncActionReceiver
      RegistrationAdmin.computeSyncActions(wettkampfInfo, this).andThen {
        case Success(actions) =>
          self ! RegistrationSyncActions(actions)
        case _ =>
          self ! RegistrationSyncActions(Vector.empty)
      }(global)
    }
  }
}

object CompetitionRegistrationClientActor {

  private def props(wettkampfUUID: String) = {
    Props(classOf[CompetitionRegistrationClientActor], wettkampfUUID)
  }

  def publish(action: RegistrationAction, context: String): Future[RegistrationEvent] = {
    val prom = Promise[RegistrationEvent]()
    implicit val timeout = Timeout(60000 milli)
    system.actorSelection(s"user/Registration-${action.wettkampfUUID}").resolveOne().onComplete {
      case Success(actorRef) =>
        prom.completeWith((actorRef ? action).mapTo[RegistrationEvent])
      case Failure(ex) =>
        prom.completeWith((system.actorOf(props(action.wettkampfUUID), name = s"Registration-${action.wettkampfUUID}") ? action).mapTo[RegistrationEvent])
    }
    prom.future
  }
}
