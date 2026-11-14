package ch.seidel.kutu.actors

import ch.seidel.kutu.domain.*
import ch.seidel.kutu.http.Core.system
import ch.seidel.kutu.http.JsonSupport
import org.apache.pekko.actor.SupervisorStrategy.{Restart, Stop}
import org.apache.pekko.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy, Terminated}
import org.apache.pekko.event.{Logging, LoggingAdapter}
import org.apache.pekko.pattern.{ask, pipe}
import org.apache.pekko.util.Timeout

import java.util
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

sealed trait AthletIndexProtokoll

sealed trait AthletIndexAction extends AthletIndexProtokoll

sealed trait AthletIndexEvent extends AthletIndexProtokoll

case object StopAthletIndex extends AthletIndexAction
case object ResyncIndex extends AthletIndexAction
case class SaveAthlet(athlet: Athlet) extends AthletIndexAction
case class RemoveAthlet(athlet: Athlet) extends AthletIndexAction
case class FindAthletLike(athlet: Athlet) extends AthletIndexAction
case class AthletIndexChanged(athlet: Athlet) extends AthletIndexEvent
case class AthletLikeFound(like: Athlet, athlet: Athlet) extends AthletIndexEvent
private case class AthletIndexResolved(ref: Option[ActorRef])

class AthletIndexActor extends Actor with JsonSupport with KutuService {
  lazy val l: LoggingAdapter = Logging(system, this)

  object log {
    def error(s: String): Unit = l.error(s)

    def error(s: String, ex: Throwable): Unit = l.error(s, ex)

    def warning(s: String): Unit = l.warning(s)

    def info(s: String): Unit = l.info(s)

    def debug(s: String): Unit = l.debug(s)
  }

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error("Error in AthletIndexActor", e)
      Restart
  }

  override def preStart(): Unit = {
    log.info(s"Start AthletIndexActor")
  }

  override def postStop(): Unit = {
    log.info(s"Stop AthletIndexActor")
  }

  private val index = new util.ArrayList[MatchCode]()

  private def invalidateIndex(): Unit = {
    index.clear()
  }

  private def mcOfId(athletId: Long): Option[MatchCode] = {
    val mcOpt = index.stream().filter(mc => mc.id == athletId).findFirst()
    if mcOpt.isEmpty then None else Some(mcOpt.get)
  }

  override def receive: Receive = {
    case ResyncIndex =>
      // Lazy: the index is rebuilt from the DB on the next FindAthletLike.
      invalidateIndex()
      sender() ! AthletLikeFound(Athlet(), Athlet())
      log.info("Athlet Index invalidated")

    case FindAthletLike(athlet) =>
      sender() ! AthletLikeFound(athlet, findAthleteLike(index, exclusive = false)(athlet))
    case SaveAthlet(athlet) => mcOfId(athlet.id) match {
      case None =>
        invalidateIndex()
        sender() ! AthletIndexChanged(athlet)
      case Some(mc) =>
        index.set(
          index.indexOf(mc),
          MatchCode(athlet.id, athlet.name, athlet.vorname, athlet.geschlecht, athlet.gebdat, athlet.verein.getOrElse(0)))
        sender() ! AthletIndexChanged(athlet)
    }
    case RemoveAthlet(athlet) =>
      index.removeIf(mc => mc.id == athlet.id)
      sender() ! AthletIndexChanged(athlet)
  }
}

class AthletIndexActorSupervisor extends Actor with ActorLogging {
  private var athletIndexActor: Option[ActorRef] = None
  private var statshedActions: List[(ActorRef,AthletIndexAction)] = List.empty

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error("Error in AthletIndexActorSupervisor actor.", e)
      Stop
  }

  override def preStart(): Unit = {
    log.info("Starting AthletIndexActorSupervisor")
    super.preStart()

    implicit val timeout: Timeout = Timeout(30000, TimeUnit.MILLISECONDS)
    // Deliver the result as a message so that state changes happen on the actor's thread.
    system.actorSelection(s"user/AthletIndex").resolveOne()
      .map(ref => AthletIndexResolved(Some(ref)))
      .recover { case _ => AthletIndexResolved(None) }
      .pipeTo(self)
  }

  private def onResolved(resolved: Option[ActorRef]): Unit = {
    val ref = resolved.getOrElse(system.actorOf(Props(classOf[AthletIndexActor]), name = s"AthletIndex"))
    athletIndexActor = Some(ref)
    context.watch(ref)
    context.become(receiveCommands)
    log.info(if resolved.isDefined then "AthletIndexActor ready" else "AthletIndexActor started")
    statshedActions.foreach(s => self.tell(s._2, s._1))
    statshedActions = List.empty
  }

  override def postStop(): Unit = {
    super.postStop()
  }

  private def receiveCommands: Actor.Receive = {
    case a: AthletIndexAction =>
      athletIndexActor.foreach(_.forward(a))
    case _ => receive
  }

  override def receive: Actor.Receive = {
    case AthletIndexResolved(resolved) => onResolved(resolved)
    case StopAthletIndex => athletIndexActor.foreach(_.forward(PoisonPill))
      self ! PoisonPill
    case a: AthletIndexAction =>
      statshedActions = statshedActions :+ (sender(), a)
    case Terminated(wettkampfActor) =>
      context.unwatch(wettkampfActor)
  }
}

object AthletIndexActor {

  val supervisor: ActorRef = system.actorOf(Props[AthletIndexActorSupervisor](), name = "AthletIndex-Supervisor")

  def publish(action: AthletIndexAction): Future[AthletIndexEvent] = {
    implicit val timeout: Timeout = Timeout(31000, TimeUnit.MILLISECONDS)

    (supervisor ? action).mapTo[AthletIndexEvent]
  }

  def stopAll(): Unit = {
    supervisor ! StopAthletIndex
  }
}
