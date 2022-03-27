package ch.seidel.kutu.akka

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer, SnapshotSelectionCriteria}
import akka.util.Timeout
import ch.seidel.kutu.Config
import ch.seidel.kutu.data.RegistrationAdmin
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.Core.system
import ch.seidel.kutu.http.JsonSupport
import ch.seidel.kutu.renderer.MailTemplates
import ch.seidel.kutu.view.WettkampfInfo

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

sealed trait RegistrationProtokoll

sealed trait RegistrationAction extends RegistrationProtokoll {
  val wettkampfUUID: String
}

sealed trait RegistrationEvent extends RegistrationProtokoll

case class RegistrationChanged(wettkampfUUID: String) extends RegistrationAction

case class RegistrationResync(wettkampfUUID: String) extends RegistrationAction

case class AskRegistrationSyncActions(wettkampfUUID: String) extends RegistrationAction

case class RegistrationSyncActions(syncActions: List[SyncAction]) extends RegistrationEvent

case class RegistrationActionWithContext(action: RegistrationAction, context: String) extends RegistrationProtokoll

class CompetitionRegistrationClientActor(wettkampfUUID: String) extends PersistentActor with JsonSupport with KutuService {
  def shortName = self.toString().split("/").last.split("#").head + "/" + clientId()

  lazy val l = akka.event.Logging(system, this)

  object log {
    def error(s: String): Unit = l.error(s"[$shortName] $s")

    def error(s: String, ex: Throwable): Unit = l.error(s"[$shortName] $s", ex)

    def warning(s: String): Unit = l.warning(s"[$shortName] $s")

    def info(s: String): Unit = l.info(s"[$shortName] $s")

    def debug(s: String): Unit = l.debug(s"[$shortName] $s")
  }

  object CheckSyncChangedForNotifier

  private val wettkampf = readWettkampf(wettkampfUUID)
  private val wettkampfInfo = WettkampfInfo(wettkampf.toView(readProgramm(wettkampf.programmId)), this)
  private var syncState: RegistrationState = RegistrationState()
  private var syncActions: Option[RegistrationState] = None
  private var syncActionReceivers: List[ActorRef] = List()
  private var clientId: () => String = () => ""
  private val notifierInterval: FiniteDuration = 1.minute
  private var rescheduleSyncNotificationCheck = context.system.scheduler.scheduleOnce(notifierInterval, self, CheckSyncChangedForNotifier)

  override def persistenceId = s"$wettkampfUUID/regs/${Config.appFullVersion}"

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
    rescheduleSyncNotificationCheck.cancel()
    notifyChangesToEMail()
  }

  override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
    log.info(event.toString)
    super.onRecoveryFailure(cause, event)
  }

  val receiveRecover: Receive = {
    //case evt: KutuAppEvent => handleEvent(evt, recoveryMode = true)
    case SnapshotOffer(_, snapshot: RegistrationState) => syncState = snapshot
    case _ =>
  }

  //override def receive = {
  val receiveCommand: Receive = {
    case RegistrationActionWithContext(action, context) =>
      clientId = () => context
      receive(action)
      clientId = () => ""

    case RegistrationResync(_) =>
      retrieveSyncActions(sender())

    case RegistrationChanged(_) => retrieveSyncActions(sender())
    case AskRegistrationSyncActions(_) =>
      if (this.syncActions.nonEmpty)
        sender() ! RegistrationSyncActions(this.syncState.syncActions)
      else
        retrieveSyncActions(sender())
    case a@RegistrationSyncActions(actions) =>
      this.syncState = syncState.resynced(actions, loadAllJudgesOfCompetition(UUID.fromString(wettkampf.uuid.get)).flatMap(_._2).toList)
      this.syncActions = Some(syncState)
      rescheduleSyncActionNotifier()
      if (syncActionReceivers.nonEmpty) {
        syncActionReceivers.foreach(_ ! a)
        syncActionReceivers = List()
      }

    case CheckSyncChangedForNotifier =>
      notifyChangesToEMail()
  }

  private def notifyChangesToEMail(): Unit = {
    if (this.syncState.hasChanges) {
      val regChanges = this.syncState.syncActions
      val judgeChanges = this.syncState.judgeSyncActions
      this.syncState = this.syncState.notified()
      val wk = readWettkampf(wettkampfUUID)
      if (wk.notificationEMail.nonEmpty) {
        KuTuMailerActor.send(
          MailTemplates.createSyncNotificationMail(wk, regChanges, judgeChanges.changed, judgeChanges.removed, judgeChanges.added)
        )
        val criteria = SnapshotSelectionCriteria.Latest
        saveSnapshot(syncState)
        deleteSnapshots(criteria)
      }
    }
  }

  private def retrieveSyncActions(syncActionReceiver: ActorRef) = {
    syncActions = None

    if (syncActionReceivers.nonEmpty) {
      syncActionReceivers = syncActionReceivers :+ syncActionReceiver
    } else {
      log.info("Rebuild Competition SyncActions ...")
      syncActionReceivers = syncActionReceivers :+ syncActionReceiver
      RegistrationAdmin.computeSyncActions(wettkampfInfo, this).andThen {
        case Success(actions) =>
          log.info("Rebuild Competition SyncActions finished")
          self ! RegistrationSyncActions(actions)
        case _ =>
          log.info("Rebuild Competition SyncActions failed")
          self ! RegistrationSyncActions(List.empty)
      }(global)
    }
  }

  private def rescheduleSyncActionNotifier() = {
    this.rescheduleSyncNotificationCheck.cancel()
    this.rescheduleSyncNotificationCheck = context.system.scheduler.scheduleOnce(notifierInterval, self, CheckSyncChangedForNotifier)
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
        prom.completeWith((actorRef ? RegistrationActionWithContext(action, context)).mapTo[RegistrationEvent])
      case Failure(ex) =>
        prom.completeWith((system.actorOf(props(action.wettkampfUUID), name = s"Registration-${action.wettkampfUUID}") ? RegistrationActionWithContext(action, context)).mapTo[RegistrationEvent])
    }
    prom.future
  }
}
