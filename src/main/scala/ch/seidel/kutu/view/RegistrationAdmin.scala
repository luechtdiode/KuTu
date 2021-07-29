package ch.seidel.kutu.view

import ch.seidel.kutu.akka.{AthletIndexActor, AthletLikeFound, FindAthletLike}
import ch.seidel.kutu.domain.{AddRegistration, AddVereinAction, ApproveVereinAction, Athlet, AthletRegistration, AthletView, EmptyAthletRegistration, KutuService, MoveRegistration, Registration, RemoveRegistration, SyncAction, Verein, WertungView}
import ch.seidel.kutu.http.RegistrationRoutes
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object RegistrationAdmin {
  private val logger = LoggerFactory.getLogger(this.getClass)

  type RegTuple = (Registration, AthletRegistration, Athlet, AthletView)

  def doSyncUnassignedClubRegistrations(wkInfo: WettkampfInfo, service: KutuService)(registrations: List[RegTuple]): (Set[Verein], Vector[SyncAction]) = {
    val starttime = System.currentTimeMillis()
    val registrationSet = registrations.map(r => (r._4.verein, r._1)).toSet
    val existingPgmAthletes: Map[Long, List[Long]] = registrations.filter(!_._2.isEmptyRegistration).groupBy(_._2.programId).map(group => (group._1 -> group._2.map(_._4.id)))
    val existingAthletes: Set[Long] = registrations.map(_._4.id).filter(_ > 0).toSet

    val addClubActions = registrations.filter(r => r._4.verein.isEmpty && !r._2.isEmptyRegistration).map(r => AddVereinAction(r._1)).toSet.toVector
    val validatedClubs = registrations.filter(r => r._1.vereinId.isEmpty).flatMap(r => r._4.verein).toSet
    val approvedClubActions = registrations.filter(r => r._1.vereinId.isEmpty && r._4.verein.nonEmpty).map(r => ApproveVereinAction(r._1.copy(vereinId = r._4.verein.map(_.id)))).toSet.toVector
    logger.info(s"start with mapping of ${registrations.size} registrations to sync-actions")
    val nonmatching: Map[String, Seq[(Registration, WertungView)]] = service.
      selectWertungen(wkuuid = wkInfo.wettkampf.uuid)
      .map { wertung: WertungView =>
        (registrationSet.find(club => wertung.athlet.verein.equals(club._1)).map(_._2), wertung)
      }
      .filter {
        _._1.isDefined
      }
      .map(t => (t._1.get, t._2))
      .groupBy { t =>
        val (verein, wertung: WertungView) = t
        if (existingAthletes.contains(wertung.athlet.id)) {
          (existingPgmAthletes.get(wertung.wettkampfdisziplin.programm.id) match {
            case Some(athletList: List[Long]) if athletList.contains(wertung.athlet.id) => "Unverändert"
            case _ => "Umteilen"
          })
        } else {
          "Entfernen"
        }
      }

    val removeActions: Seq[SyncAction] = nonmatching.get("Entfernen") match {
      case Some(list) => list
        .map(t => RemoveRegistration(t._1, t._2.wettkampfdisziplin.programm.id, t._2.athlet.toAthlet, t._2.athlet))
        .toSet.toVector
      case _ => Vector()
    }

    val mutationActions: Seq[SyncAction] = registrations.filter(r => nonmatching.get("Unverändert") match {
      case None => true
      case Some(list) => !list.exists(p => p._2.athlet.id == r._4.id)
    }).flatMap { r =>
      (nonmatching.get("Umteilen") match {
        case Some(list) => list.find(p => p._2.athlet.id == r._4.id) match {
          case Some(t) => Some(MoveRegistration(t._1, t._2.wettkampfdisziplin.programm.id, r._2.programId, r._3, r._4))
          case _ => None
        }
        case _ => None

      }) match {
        case Some(moveRegistration) => Some(moveRegistration)
        case None if (!r._2.isEmptyRegistration) => Some(AddRegistration(r._1, r._2.programId, r._3, r._4))
        case None => None
      }
    }

    val syncActions = addClubActions ++ approvedClubActions ++ removeActions ++ mutationActions
    logger.info(s"mapping of ${registrations.size} registrations to ${syncActions.size} sync-actions in ${System.currentTimeMillis() - starttime}ms")
    (validatedClubs, syncActions)
  }

  def findAthletLike(athlet: Athlet) = {
    Await.result(AthletIndexActor.publish(FindAthletLike(athlet)), Duration.Inf) match {
      case AthletLikeFound(_, found) => found
      case _ => athlet
    }
  }

  def computeSyncActions(wkInfo: WettkampfInfo, service: KutuService): Future[Vector[SyncAction]] = {
    import scala.concurrent.ExecutionContext.Implicits._
    Future {
      logger.info("start computing SyncActions ...")
      val vereineList = service.selectVereine
      val localAthletes: Map[(Long, Long), Athlet] = service.selectAthletes.map(a => ((a.id, a.verein.getOrElse(0L)), a)).toMap

      val start = Instant.now()
      val changelist: List[RegTuple] = for {
        vereinregistration <- service.selectRegistrationsOfWettkampf(UUID.fromString(wkInfo.wettkampf.uuid.get))
        athletRegistration <- service.selectAthletRegistrations(vereinregistration.id) :+ EmptyAthletRegistration(vereinregistration.id)
      } yield {
        //        logger.info(s"start processing Registration ${registration.vereinname}")
        val resolvedVerein = vereineList.find(v => v.name.equals(vereinregistration.vereinname) && (v.verband.isEmpty || v.verband.get.equals(vereinregistration.verband)))
        //        logger.info(s"resolved Verein for Registration ${registration.vereinname}")
        val parsed = athletRegistration.toAthlet.copy(verein = resolvedVerein.map(_.id))
        val candidate: Athlet = if (athletRegistration.isEmptyRegistration) {
          parsed
        } else {
          val key = (parsed.id, parsed.verein.getOrElse(0L))
          if (athletRegistration.isLocalIdentified && localAthletes.contains(key)) {
            localAthletes(key)
          } else {
            findAthletLike(parsed)
          }
        }

        //        logger.info(s"resolved candidate for ${parsed} in ${System.currentTimeMillis() - startime}ms")
        (vereinregistration, athletRegistration, parsed, AthletView(
          candidate.id, candidate.js_id,
          candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
          candidate.strasse, candidate.plz, candidate.ort,
          resolvedVerein, true))
      }
      logger.info(s"Normalize Athlet-List duration: ${java.time.Duration.between(start, Instant.now())}")
      doSyncUnassignedClubRegistrations(wkInfo, service)(changelist)._2
    }
  }

  def processSync(wkInfo: WettkampfInfo, service: RegistrationRoutes, selectedAthleten: List[SyncAction], approvedClubs: Set[Verein]) = {
    val newClubs = for (addVereinAction: AddVereinAction <- selectedAthleten.flatMap {
      case av: AddVereinAction => Some(av)
      case _ => None
    }) yield {
      service.insertVerein(addVereinAction.verein.toVerein)
    }
    val addRegistrations: immutable.Seq[AddRegistration] = selectedAthleten.flatMap {
      case ar@AddRegistration(reg, _, _, candidateView) =>
        if (candidateView.verein.isEmpty) {
          newClubs.find(c => c.name.equals(reg.vereinname) && c.verband.getOrElse("").equals(reg.verband)) match {
            case Some(verein) =>
              val registration = ar.copy(
                verein = ar.verein.copy(vereinId = Some(verein.id)),
                athlet = ar.athlet.copy(verein = Some(verein.id)),
                suggestion = ar.suggestion.withBestMatchingGebDat(ar.athlet.gebdat).copy(verein = Some(verein))
              )
              Some(registration)
            case None =>
              None
          }
        } else {
          Some(ar)
        }

      case _ => None
    }

    for ((progId, athletes) <- addRegistrations.map {
      case AddRegistration(_, programId, _, candidateView) =>
        mapAddRegistration(service, programId, candidateView)
    }.groupBy(_._1).map(x => (x._1, x._2.map(_._2)))) {
      service.assignAthletsToWettkampf(wkInfo.wettkampf.id, Set(progId), athletes.toSet)
    }

    for (moveRegistration: MoveRegistration <- selectedAthleten.flatMap {
      case mr: MoveRegistration => Some(mr)
      case _ => None
    }) {
      service.moveToProgram(wkInfo.wettkampf.id, moveRegistration.toProgramid, moveRegistration.suggestion)
    }

    for (wertungenIds: Set[Long] <- selectedAthleten.flatMap {
      case rr: RemoveRegistration => Some(service.listAthletWertungenZuWettkampf(rr.suggestion.id, wkInfo.wettkampf.id).map(_.id).toSet)
      case _ => None
    }) {
      service.unassignAthletFromWettkampf(wertungenIds)
    }

    val approvedClubRegistrations = selectedAthleten.flatMap {
      case mr: ApproveVereinAction => Some(mr)
      case _ => None
    }
    for {
      registration: ApproveVereinAction <- approvedClubRegistrations
      club: Verein <- approvedClubs.find(c => c.id == registration.verein.vereinId.get)
    } {
      service.joinVereinWithRegistration(wkInfo.wettkampf.toWettkampf, registration.verein, club)
    }

    for {
      club <- approvedClubs
      registration <- addRegistrations.find(r => r.suggestion.verein.equals(Some(club)))
    } {
      service.joinVereinWithRegistration(wkInfo.wettkampf.toWettkampf, registration.verein, club)
    }
  }

  private def mapAddRegistration(service: RegistrationRoutes, programId: Long, candidateView: AthletView) = {
    val id = service.insertAthlete(candidateView.toAthlet).id
    // TODO update athletregistration with athlet_id or better do this operation remote and download athlet
    (programId, id)
  }
}
