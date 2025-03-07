package ch.seidel.kutu.akka

import org.apache.pekko.actor.SupervisorStrategy.{Restart, Stop}
import org.apache.pekko.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Props, Terminated}
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.Core.system
import ch.seidel.kutu.http.JsonSupport
import org.apache.pekko.event.Logging

import java.util
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

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

class AthletIndexActor extends Actor with JsonSupport with KutuService {
  lazy val l = Logging(system, this)

  object log {
    def error(s: String): Unit = l.error(s)

    def error(s: String, ex: Throwable): Unit = l.error(s, ex)

    def warning(s: String): Unit = l.warning(s)

    def info(s: String): Unit = l.info(s)

    def debug(s: String): Unit = l.debug(s)
  }

  override val supervisorStrategy = OneForOneStrategy() {
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
    if (mcOpt.isEmpty) None else Some(mcOpt.get)
  }

  override def receive = {
    case ResyncIndex =>
      log.info("Resync Athlet Index ...")
      invalidateIndex()
      sender() ! AthletLikeFound(Athlet(), findAthleteLike(index, exclusive = false)(Athlet()))
      log.info("ResyncIndex Athlet Index finished")

    case FindAthletLike(athlet) =>
      sender() ! AthletLikeFound(athlet, findAthleteLike(index, exclusive = false)(athlet))
    case SaveAthlet(athlet) => mcOfId(athlet.id) match {
      case None =>
        invalidateIndex()
        sender() ! AthletIndexChanged(athlet)
      case Some(mc) =>
        index.set(
          index.indexOf(mc),
          MatchCode(athlet.id, athlet.name, athlet.vorname, athlet.gebdat, athlet.verein.getOrElse(0)))
        sender() ! AthletIndexChanged(athlet)
    }
    case RemoveAthlet(athlet) =>
      index.removeIf(mc => mc.id == athlet.id)
      sender() ! AthletIndexChanged(athlet)
  }
}

class AthletIndexActorSupervisor extends Actor with ActorLogging {
  var athletIndexActor: Option[ActorRef] = None
  var statshedActions: List[(ActorRef,AthletIndexAction)] = List.empty

  override val supervisorStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error("Error in AthletIndexActorSupervisor actor.", e)
      Stop
  }

  override def preStart(): Unit = {
    log.info("Starting AthletIndexActorSupervisor")
    super.preStart()

    implicit val timeout = Timeout(30000 milli)
    system.actorSelection(s"user/AthletIndex").resolveOne().onComplete {
      case Success(actorRef) =>
        athletIndexActor = Some(actorRef)
        context.watch(athletIndexActor.get)
        context.become(receiveCommands)
        log.info("AthletIndexActor ready")
        statshedActions.foreach(s => self.tell(s._2, s._1))
      case Failure(ex) =>
        athletIndexActor = Some(system.actorOf(Props(classOf[AthletIndexActor]), name = s"AthletIndex"))
        context.watch(athletIndexActor.get)
        context.become(receiveCommands)
        log.info("AthletIndexActor started")
        statshedActions.foreach(s => self.tell(s._2, s._1))
    }
  }

  override def postStop(): Unit = {
    super.postStop()
  }

  def receiveCommands: Actor.Receive = {
    case a: AthletIndexAction =>
      athletIndexActor.foreach(_.forward(a))
    case _ => receive
  }

  override def receive: Actor.Receive = {
    case a: AthletIndexAction =>
      statshedActions = statshedActions :+ (sender(), a)
    case Terminated(wettkampfActor) =>
      context.unwatch(wettkampfActor)
    case StopAthletIndex => athletIndexActor.foreach(_.forward(PoisonPill))
      self ! PoisonPill
  }
}

object AthletIndexActor {

  val supervisor = system.actorOf(Props[AthletIndexActorSupervisor](), name = "AthletIndex-Supervisor")

  def publish(action: AthletIndexAction): Future[AthletIndexEvent] = {
    implicit val timeout = Timeout(31000 milli)
    (supervisor ? action).mapTo[AthletIndexEvent]
  }

  def stopAll(): Unit = {
    supervisor ! StopAthletIndex
  }
}
