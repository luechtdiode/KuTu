package ch.seidel.kutu.data

import ch.seidel.kutu.KuTuApp.cleanUnusedRiegen
import ch.seidel.kutu.actors.{AthletIndexActor, AthletLikeFound, AthletsAddedToWettkampf, FindAthletLike}
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.http.{RegistrationRoutes, WebSocketClient}
import ch.seidel.kutu.squad.RiegenBuilder.{generateRiegen2Name, generateRiegenName}
import ch.seidel.kutu.view.WettkampfInfo
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object RegistrationAdmin {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private type RegTuple = (Registration, AthletRegistration, Athlet, AthletView)

  def doSyncUnassignedClubRegistrations(wkInfo: WettkampfInfo, service: KutuService)(registrations: List[RegTuple]): (Set[Verein], List[SyncAction]) = {
    val starttime = System.currentTimeMillis()
    val registrationSet: Set[(Option[Verein], Registration)] = registrations.map(r => (r._4.verein, r._1)).toSet
    val existingPgmAthletes: Map[Long, Map[Long, AthletRegistration]] = registrations.filter(!_._2.isEmptyRegistration).groupBy(_._2.programId).map(group => group._1 -> group._2.map(t => t._4.id -> t._2).toMap)
    val existingAthletes: Set[Long] = registrations.map(_._4.id).filter(_ > 0).toSet
    val validatedClubs = registrations.filter(r => r._1.vereinId.isEmpty).flatMap(r => r._4.verein).toSet
    val boden = Some(1L)
    val bodenWertungen = service.selectWertungen(disziplinId = boden, wettkampfId = Some(wkInfo.wettkampf.id))
      .map(w => w.athlet.id -> w).toMap

    val isNewVereinFilter: RegTuple => Boolean = r =>
      r._4.verein.isEmpty && !r._2.isEmptyRegistration

    val isApprovedVereinFilter: RegTuple => Boolean = r =>
      r._4.verein.nonEmpty && r._1.vereinId.isEmpty

    val isChangedClubnameFilter: RegTuple => Boolean = r =>
      r._1.vereinId.nonEmpty &&
        !r._2.isEmptyRegistration &&
        r._4.verein.nonEmpty &&
        !(r._1.matchesClubRelation() && r._1.matchesVerein(r._4.verein.get))

    val isChangedAthletnameFilter: RegTuple => Boolean = r =>
      r._1.vereinId.nonEmpty &&
        !r._2.isEmptyRegistration &&
        r._2.athlet.nonEmpty &&
        r._4.verein.nonEmpty &&
        r._1.matchesClubRelation() &&
        !(r._2.matchesAthlet() && r._2.matchesAthlet(r._4.toAthlet))

    val wettkampf = wkInfo.wettkampf
    val isMissingMediaFilter: RegTuple => Boolean = r =>
      r._2.mediafile.nonEmpty && r._2.mediafile.flatMap(m => service.loadMedia(m.id).filter(lm => lm.computeFilePath(wettkampf.toWettkampf).exists())).isEmpty

    val isMediaChangedFilter: RegTuple => Boolean = r => {
      r._2.mediafile.nonEmpty && (bodenWertungen.contains(r._4.id) && !bodenWertungen(r._4.id).mediafile.exists(a => r._2.mediafile.exists(b => a.id.equals(b.id)))) ||
      r._2.mediafile.isEmpty && (bodenWertungen.contains(r._4.id) && bodenWertungen(r._4.id).mediafile.nonEmpty)
    }

    val addClubActions = registrations
      .filter(isNewVereinFilter)
      .map(r => AddVereinAction(r._1)).distinct
    val renameClubActions = registrations
      .filter(isChangedClubnameFilter)
      .map(r => RenameVereinAction(r._1, r._4.verein.get)).distinct
    val renameAthletActions = registrations
      .filter(isChangedAthletnameFilter)
      .map(r => RenameAthletAction(r._1, r._2, r._4.toAthlet, r._3)).distinct
    val approvedClubActions = registrations
      .filter(isApprovedVereinFilter)
      .map(r => ApproveVereinAction(r._1.copy(vereinId = r._4.verein.map(_.id)))).distinct
    val addMediaActions = registrations
      .filter(isMissingMediaFilter)
      .map(r => AddMedia(r._1, r._2))

    logger.info(s"start with mapping of ${registrations.size} registrations to sync-actions")
    val nonMatchinProgramAssignment: Map[String, Seq[(Registration, WertungView)]] = service.
      selectWertungen(wkuuid = wkInfo.wettkampf.uuid)
      .map { (wertung: WertungView) =>
        (registrationSet.find(club => wertung.athlet.verein.equals(club._1)).map(_._2), wertung)
      }
      .filter {
        _._1.isDefined
      }
      .map(t => (t._1.get, t._2))
      .groupBy { t =>
        val (verein, wertung: WertungView) = t
        if existingAthletes.contains(wertung.athlet.id) then {
          existingPgmAthletes.get(wertung.wettkampfdisziplin.programm.id) match {
            case Some(athletList: Map[Long, AthletRegistration]) => athletList.get(wertung.athlet.id) match {
              case Some(athletReistration) =>
                if athletReistration.team.getOrElse(0) == wertung.team then {
                  "Unverändert"
                } else {
                  "Umteilen"
                }
              case None => "Umteilen"
            }
            case _ => "Umteilen"
          }
        } else {
          "Entfernen"
        }
      }

    val changedMedia: Seq[SyncAction] = registrations
      .filter(isMediaChangedFilter)
      .map { rt =>
        val wertung: WertungView = bodenWertungen(rt._4.id)
        UpdateAthletMediaAction(rt._1, rt._2, wertung.toWertung)
      }

    val removeActions: Seq[SyncAction] = nonMatchinProgramAssignment.get("Entfernen") match {
      case Some(list) => list
        .map(t => RemoveRegistration(t._1, t._2.wettkampfdisziplin.programm.id, t._2.athlet.toAthlet, t._2.athlet))
        .toSet.toVector
      case _ => Vector()
    }

    val reAssignProgramActions: Seq[SyncAction] = registrations.filter(r => nonMatchinProgramAssignment.get("Unverändert") match {
      case None => true
      case Some(list) => !list.exists(p => p._2.athlet.id == r._4.id)
    }).flatMap { r =>
      (nonMatchinProgramAssignment.get("Umteilen") match {
        case Some(list) => list.find(p => p._2.athlet.id == r._4.id) match {
          case Some(t) => Some(MoveRegistration(
            t._1,
            t._2.wettkampfdisziplin.programm.id,
            t._2.team,
            r._2.programId,
            r._2.team.getOrElse(0),
            r._3, r._4))
          case _ => None
        }
        case _ => None

      }) match {
        case Some(moveRegistration) =>
          Some(moveRegistration)
        case None if !r._2.isEmptyRegistration =>
          Some(AddRegistration(r._1, r._2.programId, r._3, r._4, r._2.team.getOrElse(0), r._2.mediafile))
        case None => None
      }
    }

    val syncActions = addClubActions ++ approvedClubActions ++ renameClubActions ++ addMediaActions ++ renameAthletActions ++ removeActions ++ reAssignProgramActions ++ changedMedia
    logger.info(s"mapping of ${registrations.size} registrations to ${syncActions.size} sync-actions in ${System.currentTimeMillis() - starttime}ms")
    (validatedClubs, syncActions)
  }

  def findAthletLike(athlet: Athlet): Athlet = {
    Await.result(AthletIndexActor.publish(FindAthletLike(athlet)), Duration.Inf) match {
      case AthletLikeFound(_, found) => found
      case _ => athlet
    }
  }

  def computeSyncActions(wkInfo: WettkampfInfo, service: KutuService): Future[List[SyncAction]] = {
    import scala.concurrent.ExecutionContext.Implicits.*
    Future {
      logger.info("start computing SyncActions ...")
      val vereineList = service.selectVereine
      val localAthletes: Map[(Long, Long), Athlet] = service.selectAthletes.map(a => ((a.id, a.verein.getOrElse(0L)), a)).toMap

      val start = Instant.now()
      val changelist: List[RegTuple] = for
        vereinregistration <- service.selectRegistrationsOfWettkampf(UUID.fromString(wkInfo.wettkampf.uuid.get))
        athletRegistration <- service.selectAthletRegistrations(vereinregistration.id) :+ EmptyAthletRegistration(vereinregistration.id)
      yield {
        //        logger.info(s"start processing Registration ${registration.vereinname}")
        val resolvedVerein = vereineList.find(v => vereinregistration.matchesVerein(v))
        //        logger.info(s"resolved Verein for Registration ${registration.vereinname}")
        val parsed = athletRegistration.toAthlet.copy(verein = resolvedVerein.map(_.id))
        val candidate: Athlet = if athletRegistration.isEmptyRegistration then {
          parsed
        } else {
          val key = (parsed.id, parsed.verein.getOrElse(0L))
          if athletRegistration.isLocalIdentified && localAthletes.contains(key) then {
            localAthletes(key)
          } else {
            findAthletLike(parsed)
          }
        }

        //        logger.info(s"resolved candidate for ${parsed} in ${System.currentTimeMillis() - startime}ms")
        (vereinregistration, athletRegistration, parsed, candidate.toAthletView(resolvedVerein))
      }
      logger.info(s"Normalize Athlet-List duration: ${java.time.Duration.between(start, Instant.now())}")
      doSyncUnassignedClubRegistrations(wkInfo, service)(changelist)._2.map(PublicSyncAction(_))
    }
  }

  def processSync(wkInfo: WettkampfInfo, service: RegistrationRoutes, syncActions: List[SyncAction], approvedClubs: Set[Verein]): List[String] = {
    val newClubs = for (addVereinAction: AddVereinAction <- syncActions.flatMap {
      case av: AddVereinAction => Some(av)
      case _ => None
    }) yield {
      // NOT implicit pushing to ws-client
      service.insertVerein(addVereinAction.verein.toVerein)
    }
    val addRegistrations: immutable.Seq[AddRegistration] = syncActions.flatMap {
      case ar@AddRegistration(reg, _, _, candidateView, _, media) =>
        if candidateView.verein.isEmpty then {
          newClubs.find(c => c.name.equals(reg.vereinname) && c.verband.getOrElse("").equals(reg.verband)) match {
            case Some(verein) =>
              val registration = ar.copy(
                verein = ar.verein.copy(vereinId = Some(verein.id)),
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

    val athleteLocalUpdates = for (renameAthlete: RenameAthletAction <- syncActions.flatMap {
      case mr: RenameAthletAction => Some(mr)
      case _ => None
    }) yield {
      val preparedUpdate = renameAthlete.applyLocalChange
      if renameAthlete.isSexChange then {
        adjustWertungRiegen(wkInfo.wettkampf.toWettkampf, service, preparedUpdate)
      }
      (preparedUpdate.id.toString, preparedUpdate)
    }
    service.insertAthletes(athleteLocalUpdates)
    val athleteRemoteUpdates = for (renameAthlete: RenameAthletAction <- syncActions.flatMap {
      case mr: RenameAthletAction => Some(mr)
      case _ => None
    }) yield {
      renameAthlete.applyRemoteChange
    }
    service.updateRemoteAthletes(wkInfo.wettkampf.toWettkampf, athleteRemoteUpdates)

    for (renameVereinAction: RenameVereinAction <- syncActions.flatMap {
      case mr: RenameVereinAction => Some(mr)
      case _ => None
    }) {
      service.updateVereinRemote(wkInfo.wettkampf.toWettkampf, renameVereinAction.prepareRemoteUpdate)
      service.updateVerein(renameVereinAction.prepareLocalUpdate)
    }

    for (moveRegistration: MoveRegistration <- syncActions.flatMap {
      case mr: MoveRegistration => Some(mr)
      case _ => None
    }) {
      // implicit pushing to ws-client
      service.moveToProgram(wkInfo.wettkampf.id, moveRegistration.toProgramid, moveRegistration.toTeam, moveRegistration.suggestion)
    }

    for (wertungenIds: Set[Long] <- syncActions.flatMap {
      case rr: RemoveRegistration => Some(service.listAthletWertungenZuWettkampf(rr.suggestion.id, wkInfo.wettkampf.id).map(_.id).toSet)
      case _ => None
    }) {
      // implicit pushing to ws-client
      service.unassignAthletFromWettkampf(wertungenIds)
    }

    val approvedClubRegistrations = syncActions.flatMap {
      case mr: ApproveVereinAction => Some(mr)
      case _ => None
    }
    for
      registration: ApproveVereinAction <- approvedClubRegistrations
      club: Verein <- approvedClubs.find(c => c.id == registration.verein.vereinId.get)
    do {
      service.joinVereinWithRegistration(wkInfo.wettkampf.toWettkampf, registration.verein, club)
    }

    for
      club <- approvedClubs
      registration <- addRegistrations.find(r => r.suggestion.verein.contains(club))
    do {
      service.joinVereinWithRegistration(wkInfo.wettkampf.toWettkampf, registration.verein, club)
    }

    service.saveOrUpdateMedias(syncActions.flatMap {
      case ar: AddRegistration => ar.media
      case mr: UpdateAthletMediaAction => mr.athletReg.mediafile
      case am: AddMedia => am.athletReg.mediafile
      case _ => None
    }.distinct)

    val requestMediaList = syncActions.flatMap {
      case am: AddMedia => am.athletReg.mediafile
      case _ => None
    }.distinct
    if requestMediaList.nonEmpty then {
      service.getMediaDownloadRemote(wkInfo.wettkampf.toWettkampf, requestMediaList)
    }

    for (((progId, team), idAndathletes) <- addRegistrations.map {
      case AddRegistration(_, programId, _, candidateView, team, media) =>
        val (pgmId, athlId) = mapAddRegistration(service, programId, candidateView)
        (pgmId, team, media.map(_.toMedia), athlId)
    }.groupBy(t => (t._1, t._2))) {
      // NOT implicit pushing to ws-client
      val athletIds = idAndathletes.map(x => (x._4, x._3)).toSet
      service.assignAthletsToWettkampf(wkInfo.wettkampf.id, Set(progId), athletIds, Some(team))
    }

    for (((progId, team), addActions) <- addRegistrations.groupBy {
      case AddRegistration(_, programId, _, _, team, _) => (programId, team)
    }) {
      WebSocketClient.publish(AthletsAddedToWettkampf(
        addActions.map(aa => (aa.suggestion, aa.media.map(_.toMedia))).toList,
        wkInfo.wettkampf.uuid.get,
        progId, team))
    }

    val updateAthletMediaActions = syncActions.flatMap {
      case mr: UpdateAthletMediaAction => Some(mr)
      case _ => None
    }
    for athletMediaUpdateAction: UpdateAthletMediaAction <- updateAthletMediaActions do {
      // implicit pushing to ws-client
      service.updateWertung(athletMediaUpdateAction.wertung.copy(mediafile = athletMediaUpdateAction.athletReg.mediafile.map(_.toMedia)))
    }
    if updateAthletMediaActions.nonEmpty then {
      service.regchanged(wkInfo.wettkampf.toWettkampf)
    }

    cleanUnusedRiegen(wkInfo.wettkampf.id)

    val einteilungen = service.selectRiegen(wkInfo.wettkampf.id)
    if einteilungen.nonEmpty then {
      val unassignedRiegen = service.listRiegenZuWettkampf(wkInfo.wettkampf.id)
        .filter(r => r._3.isEmpty || r._4.isEmpty)
        .toList
      for r <- unassignedRiegen do {
        service.updateOrinsertRiege(
          RiegeRaw(wettkampfId = wkInfo.wettkampf.id, r = r._1, durchgang = r._3, start = r._4.map(_.id), kind = 0))
      }
      unassignedRiegen.map(r => s"Neue Riege ${r._1}, mit provisorischer Einteilung. Durchgang: ${r._3.getOrElse("nicht zugewiesen")}, Startgerät ${r._4.map(_.name).getOrElse("nicht zugewiesen")}")
    } else {
      List.empty
    }
  }

  def adjustWertungRiegen(wettkampf: Wettkampf, service: KutuService, changedAthlet: Athlet): Unit = {
    val newLocalWertungen = service.selectWertungen(athletId = Some(changedAthlet.id), wettkampfId = Some(wettkampf.id))
      .map(w => w.copy(
        riege = Some(generateRiegenName(
          w.copy(athlet = changedAthlet.toAthletView(w.athlet.verein))
        )),
        riege2 = generateRiegen2Name(
          w.copy(athlet = changedAthlet.toAthletView(w.athlet.verein))
        )
      ).toWertung
      )
    Await.result(service.updateAllWertungenAsync(newLocalWertungen), Duration.Inf)
  }

  private def mapAddRegistration(service: RegistrationRoutes, programId: Long, candidateView: AthletView) = {
    val id = service.insertAthlete(candidateView.toAthlet).id
    (programId, id)
  }
}
