package ch.seidel.kutu.view

import java.util.UUID

import ch.seidel.kutu.domain.{AddRegistration, AddVereinAction, Athlet, AthletRegistration, AthletView, EmptyAthletRegistration, MatchCode, MoveRegistration, Registration, RemoveRegistration, SyncAction, WertungView}
import ch.seidel.kutu.http.RegistrationRoutes

import scala.concurrent.Future

object RegistrationAdmin {
  type RegTuple = (Registration, AthletRegistration, Athlet, AthletView)

  def doSyncUnassignedClubRegistrations(wkInfo: WettkampfInfo, service: RegistrationRoutes)(registrations: List[RegTuple]): Vector[SyncAction] = {
    val relevantClubs = registrations.map(_._1).toSet
    val existingPgmAthletes: Map[Long, List[Long]] = registrations.filter(!_._2.isEmptyRegistration).groupBy(_._2.programId).map(group => (group._1 -> group._2.map(_._4.id)))
    val existingAthletes: Set[Long] = registrations.map(_._4.id).filter(_ > 0).toSet

    val addClubActions = registrations.filter(r => r._1.vereinId == None && !r._2.isEmptyRegistration).map(r => AddVereinAction(r._1)).toSet.toVector

    val nonmatching: Map[String, Seq[(Registration, WertungView)]] = service.
      selectWertungen(wkuuid = wkInfo.wettkampf.uuid)
      .map { wertung: WertungView =>
        (relevantClubs.find(club => wertung.athlet.verein.map(_.id).equals(club.vereinId)), wertung)
      }
      .filter {
        _._1 != None
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

    addClubActions ++ removeActions ++ mutationActions
  }

  def computeSyncActions(wkInfo: WettkampfInfo, service: RegistrationRoutes): Future[Vector[SyncAction]] = {
    import scala.concurrent.ExecutionContext.Implicits._
    Future {
      val vereineList = service.selectVereine
      val cache = new java.util.ArrayList[MatchCode]()
      val changelist: List[RegTuple] = for {
        verein <- service.selectRegistrationsOfWettkampf(UUID.fromString(wkInfo.wettkampf.uuid.get))
        athlet <- service.selectAthletRegistrations(verein.id) :+ EmptyAthletRegistration(verein.id)
        resolvedVerein <- vereineList.find(v => v.name.equals(verein.vereinname) && (v.verband.isEmpty || v.verband.get.equals(verein.verband))) match {
          case Some(v) => List(verein.copy(vereinId = Some(v.id)))
          case None => List(verein)
        }
      } yield {
        val parsed = athlet.toAthlet.copy(verein = resolvedVerein.vereinId)
        val candidate = if (athlet.isEmptyRegistration) parsed else service.findAthleteLike(cache)(parsed)
        (resolvedVerein, athlet, parsed, AthletView(
          candidate.id, candidate.js_id,
          candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
          candidate.strasse, candidate.plz, candidate.ort,
          vereineList.find(v => resolvedVerein.vereinId.contains(v.id)), true))
      }
      doSyncUnassignedClubRegistrations(wkInfo, service)(changelist)
    }
  }

  def processSync(wkInfo: WettkampfInfo, service: RegistrationRoutes, selectedAthleten: List[SyncAction]) = {
    val newClubs = for (addVereinAction: AddVereinAction <- selectedAthleten.flatMap {
      case av: AddVereinAction => Some(av)
      case _ => None
    }) yield {
      service.insertVerein(addVereinAction.verein.toVerein)
    }

    for ((progId, athletes) <- selectedAthleten.flatMap {
      case AddRegistration(reg, programId, importathlet, candidateView) =>
        if (candidateView.verein == None) {
          newClubs.find(c => c.name.equals(reg.vereinname) && c.verband.getOrElse("").equals(reg.verband)) match {
            case Some(verein) =>
              Some(mapAddRegistration(service, programId, importathlet, candidateView.copy(verein = Some(verein))))
            case None =>
              None
          }
        } else {
          Some(mapAddRegistration(service, programId, importathlet, candidateView))
        }

      case _ => None
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
  }

  private def mapAddRegistration(service: RegistrationRoutes, programId: Long, importathlet: Athlet, candidateView: AthletView) = {
    val id = if (candidateView.id > 0 &&
      (importathlet.gebdat match {
        case Some(d) =>
          candidateView.gebdat match {
            case Some(cd) => f"${cd}%tF".endsWith("-01-01")
            case _ => true
          }
        case _ => false
      })) {
      val athlet = service.insertAthlete(Athlet(
        id = candidateView.id,
        js_id = candidateView.js_id,
        geschlecht = candidateView.geschlecht,
        name = candidateView.name,
        vorname = candidateView.vorname,
        gebdat = importathlet.gebdat,
        strasse = candidateView.strasse,
        plz = candidateView.plz,
        ort = candidateView.ort,
        verein = candidateView.verein.map(_.id),
        activ = true
      ))
      athlet.id
    }
    else if (candidateView.id > 0) {
      candidateView.id
    }
    else {
      val athlet = service.insertAthlete(Athlet(
        id = 0,
        js_id = candidateView.js_id,
        geschlecht = candidateView.geschlecht,
        name = candidateView.name,
        vorname = candidateView.vorname,
        gebdat = candidateView.gebdat,
        strasse = candidateView.strasse,
        plz = candidateView.plz,
        ort = candidateView.ort,
        verein = candidateView.verein.map(_.id),
        activ = true
      ))
      // TODO update athletregistration with athlet_id or better do this operation remote and download athlet
      athlet.id
    }
    (programId, id)
  }
}
