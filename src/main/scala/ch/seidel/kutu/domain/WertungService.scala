package ch.seidel.kutu.domain

import java.util.UUID
import java.util.concurrent.TimeUnit

import ch.seidel.kutu.Config
import ch.seidel.kutu.actors.AthletWertungUpdated
import ch.seidel.kutu.http.WebSocketClient
import org.slf4j.LoggerFactory
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object WertungServiceBestenResult {
  private val logger = LoggerFactory.getLogger(this.getClass)
  
  private var bestenResults = Map[String,WertungView]()
  private var shouldResetBestenResults = false
  
  def putWertungToBestenResults(wertung: WertungView): Unit = {
    bestenResults = bestenResults.updated(s"${wertung.athlet.id}:${wertung.wettkampfdisziplin.id}", wertung)
    logger.info(s"actually best-scored: \n${bestenResults.mkString("\n")}")
  }
  
  def getBestenResults = {
    bestenResults
/* Athlet, Disziplin, Wertung (Endnote)
    .map(w =>(w._2.athlet.easyprint, w._2.wettkampfdisziplin.disziplin.name, w._2.endnote))    
    .sortBy(_._3)
 */
    .map(_._2)    
    .toList
  }
  
  def resetBestenResults: Unit = {
    shouldResetBestenResults = true;
  }

  def cleanBestenResults: Unit = {
    if(shouldResetBestenResults) {
      bestenResults = Map[String,WertungView]()
      shouldResetBestenResults = false
    }
  }
}

abstract trait WertungService extends DBService with WertungResultMapper with DisziplinService with RiegenService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  import WertungServiceBestenResult._
  
  def selectWertungen(vereinId: Option[Long] = None, athletId: Option[Long] = None, wettkampfId: Option[Long] = None, disziplinId: Option[Long] = None, wkuuid: Option[String] = None): Seq[WertungView] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val where = "where " + (athletId match {
      case None     => "1=1"
      case Some(id) => s"a.id = $id"
    }) + " and " + (vereinId match {
      case None     => "1=1"
      case Some(id) => s"v.id = $id"
    }) + " and " + (wettkampfId match {
      case None     => "1=1"
      case Some(id) => s"wk.id = $id"
    }) + " and " + (disziplinId match {
      case None     => "1=1"
      case Some(id) => s"d.id = $id"
    }) + " and " + (wkuuid match {
      case None     => "1=1"
      case Some(uuid) => s"wk.uuid = '$uuid'"
    })
    Await.result(database.run {
      (
        sql"""
                    SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet,
                      wk.id, wk.uuid, wk.datum, wk.titel, wk.programm_id, wk.auszeichnung, wk.auszeichnungendnote, wk.notificationEMail, wk.altersklassen, wk.jahrgangsklassen, wk.punktegleichstandsregel, wk.rotation, wk.teamrule,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2, w.team
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)

                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    #$where
                    order by wd.programm_id, wd.ord
         """.as[WertungView]).withPinnedSession
    }, Duration.Inf)
  }
  
  def getWertung(id: Long): WertungView = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    Await.result(database.run{(
      sql"""
                    SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet,
                      wk.id, wk.uuid, wk.datum, wk.titel, wk.programm_id, wk.auszeichnung, wk.auszeichnungendnote, wk.notificationEMail, wk.altersklassen, wk.jahrgangsklassen, wk.punktegleichstandsregel, wk.rotation, wk.teamrule,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2, w.team
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    where w.id = ${id}
                    order by wd.programm_id, wd.ord
         """.as[WertungView]).withPinnedSession
    }, Duration.Inf).head
  }

  def getCurrentWertung(wertung: Wertung): Option[WertungView] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()

    Await.result(database.run{(
      sql"""
                    SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet,
                      wk.id, wk.uuid, wk.datum, wk.titel, wk.programm_id, wk.auszeichnung, wk.auszeichnungendnote, wk.notificationEMail, wk.altersklassen, wk.jahrgangsklassen, wk.punktegleichstandsregel, wk.rotation, wk.teamrule,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2, w.team
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    where a.id = ${wertung.athletId}
                      and wk.uuid = ${wertung.wettkampfUUID}
                      and wd.id = ${wertung.wettkampfdisziplinId}
         """.as[WertungView]).withPinnedSession
    }, Duration.Inf).headOption
  }

  def listScheduledDisziplinIdsZuWettkampf(wettkampfId: Long): List[Long] = {
    Await.result(database.run {
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      val riegen = selectRiegenRaw(wettkampfId)
        .map(r => r.start)
        .filter(_.nonEmpty)
        .map(_.get)
        .filter(_ > 0).toSet
      if (riegen.nonEmpty) {
        sql""" select distinct disziplin_id from wettkampfdisziplin
             where programm_Id in #$programme
               and disziplin_id in #${riegen.mkString("(", ",", ")")}
             """.as[Long].withPinnedSession

      } else {
        sql""" select distinct disziplin_id from wettkampfdisziplin
             where programm_Id in #$programme
             """.as[Long].withPinnedSession
      }
    }, Duration.Inf).toList
  }

  def updateOrinsertWertungenZuWettkampf(wettkampf: Wettkampf, wertungen: Iterable[Wertung]) = {
    val insertWertungenAction = DBIO.sequence(for {
      w <- wertungen
    } yield {
      sqlu"""
                insert into wertung
                (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote, riege, riege2, team)
                values (${w.athletId}, ${w.wettkampfdisziplinId}, ${w.wettkampfId}, ${w.noteD}, ${w.noteE}, ${w.endnote}, ${w.riege}, ${w.riege2}, ${w.team.getOrElse(0)})
        """
    })
    
    val process = DBIO.seq(
      sqlu"""
                delete from wertung where wettkampf_id=${wettkampf.id}
          """>>
      sqlu"""   delete from riege
                WHERE wettkampf_id=${wettkampf.id} and not exists (
                  SELECT 1 FROM wertung w
                  WHERE w.wettkampf_id=${wettkampf.id}
                    and (w.riege=riege.name or w.riege2=riege.name)
                )
          """>>
      insertWertungenAction
    )
      
    Await.result(database.run{process.transactionally}, Duration.Inf)
    wertungen.size
  }

  def updateOrinsertWertung(w: Wertung) = {
    Await.result(database.run(DBIO.sequence(Seq(
      sqlu"""
                delete from wertung where
                athlet_Id=${w.athletId} and wettkampfdisziplin_Id=${w.wettkampfdisziplinId} and wettkampf_Id=${w.wettkampfId}
        """,
        
      sqlu"""
                insert into wertung
                (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote, riege, riege2, team)
                values (${w.athletId}, ${w.wettkampfdisziplinId}, ${w.wettkampfId}, ${w.noteD}, ${w.noteE}, ${w.endnote}, ${w.riege}, ${w.riege2}, ${w.team.getOrElse(0)})
        """,

      sqlu"""   delete from riege
                WHERE wettkampf_id=${w.id} and not exists (
                  SELECT 1 FROM wertung w
                  WHERE w.wettkampf_id=${w.id}
                    and (w.riege=riege.name or w.riege2=riege.name)
                )
        """
    )).transactionally), Duration.Inf)
  }

  def updateWertung(w: Wertung): WertungView = {
    Await.result(updateWertungAsync(w), Duration.Inf)
  }
  
  def updateAllWertungenAsync(ws: Seq[Wertung]): Future[Seq[WertungView]] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    implicit val mapper = getAthletViewResult
    val ret = database.run(DBIO.sequence(for {
      w <- ws
    } yield {
      sqlu"""       UPDATE wertung
                    SET note_d=${w.noteD}, note_e=${w.noteE}, endnote=${w.endnote}, riege=${w.riege}, riege2=${w.riege2}, team=${w.team.getOrElse(0)}
                    WHERE id=${w.id}
          """>>
      sqlu"""       DELETE from riege
                    WHERE wettkampf_id=${w.id} and not exists (
                      SELECT 1 FROM wertung w
                      WHERE w.wettkampf_id=${w.id}
                        and (w.riege=name or w.riege2=name)
                    )
          """>>
      sql"""
                    SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet,
                      wk.id, wk.uuid, wk.datum, wk.titel, wk.programm_id, wk.auszeichnung, wk.auszeichnungendnote, wk.notificationEMail, wk.altersklassen, wk.jahrgangsklassen, wk.punktegleichstandsregel, wk.rotation, wk.teamrule,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2, w.team
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    WHERE w.id=${w.id}
                    order by wd.programm_id, wd.ord
       """.as[WertungView].head
     }).transactionally)
    
    ret
  }
  
  def updateWertungAsync(w: Wertung): Future[WertungView] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    implicit val mapper = getAthletViewResult
    val ret = database.run((
      sqlu"""       UPDATE wertung
                    SET note_d=${w.noteD}, note_e=${w.noteE}, endnote=${w.endnote}, riege=${w.riege}, riege2=${w.riege2}, team=${w.team.getOrElse(0)}
                    WHERE id=${w.id}
          """>>

      sqlu"""       DELETE from riege
                    WHERE wettkampf_id=${w.id} and not exists (
                      SELECT 1 FROM wertung w
                      WHERE w.wettkampf_id=${w.id}
                        and (w.riege=name or w.riege2=name)
                    )
          """>>
      sql"""
                    SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet,
                      wk.id, wk.uuid, wk.datum, wk.titel, wk.programm_id, wk.auszeichnung, wk.auszeichnungendnote, wk.notificationEMail, wk.altersklassen, wk.jahrgangsklassen, wk.punktegleichstandsregel, wk.rotation, wk.teamrule,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2, w.team
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    WHERE w.id=${w.id}
                    order by wd.programm_id, wd.ord
       """.as[WertungView].head).transactionally)
    
    ret.map{wv =>
      cleanBestenResults
      if(wv.endnote.sum >= Config.bestenlisteSchwellwert) {
        putWertungToBestenResults(wv)
      }
      val awu = AthletWertungUpdated(wv.athlet, wv.toWertung, wv.wettkampf.uuid.get, "", wv.wettkampfdisziplin.disziplin.id, wv.wettkampfdisziplin.programm.easyprint)
      WebSocketClient.publish(awu)
      wv
    }
  }

  @throws(classOf[Exception]) // called from mobile-client via coordinator-actor
  def validateWertung(w: Wertung): Wertung = {
    val notenspez = readWettkampfDisziplinView(w.wettkampfdisziplinId)
    val wv = notenspez.verifiedAndCalculatedWertung(w)
    def eq(a: Option[BigDecimal], b: Option[BigDecimal]): Boolean = (a,b) match {
      case (None, Some(v)) => v.compare(BigDecimal(0)) == 0
      case (Some(v), None) => v.compare(BigDecimal(0)) == 0
      case (None, None) => true
      case (a, b) => a.get.compare(b.get) == 0
    }
    if (notenspez.isDNoteUsed && !eq(wv.noteD, w.noteD)) {
      throw new IllegalArgumentException(s"Erfasster D-Wert: ${w.noteDasText}, erlaubter D-Wert: ${wv.noteDasText}")
    }
    if (!eq(wv.noteE, w.noteE)) {
      throw new IllegalArgumentException(s"Erfasster E-Wert: ${w.noteEasText}, erlaubter E-Wert: ${wv.noteEasText}")
    }
    wv
  }

  @throws(classOf[Exception]) // called from mobile-client via coordinator-actor
  def updateWertungSimple(w: Wertung): Wertung = {
    val wv = validateWertung(w);
    Await.result(database.run(DBIO.sequence(Seq(sqlu"""
                  UPDATE wertung
                  SET note_d=${wv.noteD}, note_e=${wv.noteE}, endnote=${wv.endnote}, riege=${wv.riege}, riege2=${wv.riege2}, team=${wv.team.getOrElse(0)}
                  WHERE 
                    athlet_Id=${wv.athletId} and wettkampfdisziplin_Id=${wv.wettkampfdisziplinId} and wettkampf_Id=${wv.wettkampfId}
          """//.transactionally
          ))
    ), Duration(5, TimeUnit.SECONDS))
    wv
  }
  
  @throws(classOf[Exception]) // called from rich-client-app via ResourceExchanger
  def updateWertungWithIDMapping(w: Wertung): Wertung = {
    val wv = readWettkampfDisziplinView(w.wettkampfdisziplinId).verifiedAndCalculatedWertung(w)
    println("single import wertung ...")
    val wvId = Await.result(database.run((for {
        updated <- sqlu"""
                  UPDATE wertung
                  SET note_d=${wv.noteD}, note_e=${wv.noteE}, endnote=${wv.endnote}, riege=${wv.riege}, riege2=${wv.riege2}, team=${wv.team.getOrElse(0)}
                  WHERE
                    athlet_Id=${wv.athletId} and wettkampfdisziplin_Id=${wv.wettkampfdisziplinId} and wettkampf_Id=${wv.wettkampfId}
          """
        wvId <- sql"""
                  SELECT id FROM wertung
                  WHERE
                    athlet_Id=${wv.athletId} and wettkampfdisziplin_Id=${wv.wettkampfdisziplinId} and wettkampf_Id=${wv.wettkampfId}
        """.as[Long]
      } yield {
        wvId
      }).transactionally
    ), Duration.Inf).head
    val result = wv.copy(id = wvId)
    result
  }

  @throws(classOf[Exception]) // called from rich-client-app via ResourceExchanger
  def updateWertungWithIDMapping(ws: Seq[Wertung]): Seq[Wertung] = {
    println("multi import wertung ...")
    val wvs = ws.map(w => readWettkampfDisziplinView(w.wettkampfdisziplinId).verifiedAndCalculatedWertung(w))
    val wvId: Seq[Option[Long]] = Await.result(database.run(DBIO.sequence(for {
      wv <- wvs
    } yield {
      sqlu"""
                  UPDATE wertung
                  SET note_d=${wv.noteD}, note_e=${wv.noteE}, endnote=${wv.endnote}, riege=${wv.riege}, riege2=${wv.riege2}, team=${wv.team.getOrElse(0)}
                  WHERE
                    athlet_Id=${wv.athletId} and wettkampfdisziplin_Id=${wv.wettkampfdisziplinId} and wettkampf_Id=${wv.wettkampfId}
          """>>
      sql"""
                  SELECT id FROM wertung
                  WHERE
                    athlet_Id=${wv.athletId} and wettkampfdisziplin_Id=${wv.wettkampfdisziplinId} and wettkampf_Id=${wv.wettkampfId}
        """.as[Long].headOption
    }).transactionally
    ), Duration.Inf)
    val result = wvs.zip(wvId)
      .filter {
        case (_, Some(_)) => true
        case (w, None) =>
          println(
            s"""
               |Unmatching Wertung!
               |  atheltId:           ${w.athletId}
               |  riege:              ${w.riege}
               |  Wettkampfdisziplin: ${w.wettkampfdisziplinId}
               |  Resultat:           ${w.resultat}
               |""".stripMargin)
          false
        case _ => false
      }
      .map{z => z._1.copy(id = z._2.get)}
    result
  }

  def listAthletenWertungenZuProgramm(progids: Seq[Long], wettkampf: Long, riege: String = "%") = {
    Await.result(database.run{
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      (sql"""
                   SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                     wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet,
                     wk.id, wk.uuid, wk.datum, wk.titel, wk.programm_id, wk.auszeichnung, wk.auszeichnungendnote, wk.notificationEMail, wk.altersklassen, wk.jahrgangsklassen, wk.punktegleichstandsregel, wk.rotation, wk.teamrule,
                     w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2, w.team
                   FROM wertung w
                   inner join athlet a on (a.id = w.athlet_id)
                   left outer join verein v on (a.verein = v.id)
                   inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                   inner join disziplin d on (d.id = wd.disziplin_id)
                   inner join programm p on (p.id = wd.programm_id)
                   inner join wettkampf wk on (wk.id = w.wettkampf_id)
                   where wd.programm_id in (#${progids.mkString(",")})
                     and w.wettkampf_id = $wettkampf
                     and ($riege = '%' or w.riege = $riege or w.riege2 = $riege)
                   order by wd.programm_id, wd.ord
       """.as[WertungView]).withPinnedSession}, Duration.Inf)    
  }

  def listAthletWertungenZuWettkampf(athletId: Long, wettkampf: Long) = {
    Await.result(database.run{
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      (sql"""
                   SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                     wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet,
                     wk.id, wk.uuid, wk.datum, wk.titel, wk.programm_id, wk.auszeichnung, wk.auszeichnungendnote, wk.notificationEMail, wk.altersklassen, wk.jahrgangsklassen, wk.punktegleichstandsregel, wk.rotation, wk.teamrule,
                     w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2, w.team
                   FROM wertung w
                   inner join athlet a on (a.id = w.athlet_id)
                   left outer join verein v on (a.verein = v.id)
                   inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                   inner join disziplin d on (d.id = wd.disziplin_id)
                   inner join programm p on (p.id = wd.programm_id)
                   inner join wettkampf wk on (wk.id = w.wettkampf_id)
                   where w.athlet_id = $athletId
                     and w.wettkampf_id = $wettkampf
                   order by wd.programm_id, wd.ord
       """.as[WertungView]).withPinnedSession
    }, Duration.Inf)
  }

  def getAllKandidatenWertungen(competitionUUId: UUID) = {
    val driver = selectWertungen(wkuuid = Some(competitionUUId.toString())).groupBy { x => x.athlet }.map(_._2).toList
    val competitionId = if (driver.isEmpty || driver.head.isEmpty) {
      0
    } else {
      driver.head.head.wettkampf.id
    }
    
    val programme = driver.flatten.map(x => x.wettkampfdisziplin.programm).foldLeft(Seq[ProgrammView]()){(acc, pgm) =>
      if(!acc.exists { x => x.id == pgm.id }) {
        acc :+ pgm
      }
      else {
        acc
      }
    }
    val riegendurchgaenge = selectRiegen(competitionId).map(r => r.r-> r).toMap
    val rds = riegendurchgaenge.values.map(v => v.durchgang.getOrElse("")).toSet
    val disziplinsZuDurchgangR1 = listDisziplinesZuDurchgang(rds, competitionId, true)
    val disziplinsZuDurchgangR2 = listDisziplinesZuDurchgang(rds, competitionId, false)

    for {
      programm <- programme
      athletwertungen <- driver.map(we => we.filter { x => x.wettkampfdisziplin.programm.id == programm.id})
      if(athletwertungen.nonEmpty)
      einsatz = athletwertungen.head
      athlet = einsatz.athlet
    }
    yield {
      val riegendurchgang1 = riegendurchgaenge.get(einsatz.riege.getOrElse(""))
      val riegendurchgang2 = riegendurchgaenge.get(einsatz.riege2.getOrElse(""))

      Kandidat(
      einsatz.wettkampf.easyprint
      ,athlet.geschlecht match {case "M" => "Turner"  case _ => "Turnerin"}
      ,einsatz.wettkampfdisziplin.programm.easyprint
      ,athlet.id
      ,athlet.name
      ,athlet.vorname
      ,AthletJahrgang(athlet.gebdat).jahrgang
      ,athlet.verein match {case Some(v) => v.easyprint case _ => ""}
      ,riegendurchgang1
      ,riegendurchgang2
      ,athletwertungen.filter{wertung =>
        if(wertung.wettkampfdisziplin.feminim == 0 && !wertung.athlet.geschlecht.equalsIgnoreCase("M")) {
          false
        }
        else if(wertung.wettkampfdisziplin.masculin == 0 && wertung.athlet.geschlecht.equalsIgnoreCase("M")) {
          false
        }
        else {
          riegendurchgang1.forall{x =>
            x.durchgang.nonEmpty &&
            x.durchgang.forall{d =>
              d.nonEmpty &&
              disziplinsZuDurchgangR1.get(d).exists(dm => dm.contains(wertung.wettkampfdisziplin.disziplin))
            }
          }
        }
      }.map(_.wettkampfdisziplin.disziplin)
      ,athletwertungen.filter{wertung =>
        if(wertung.wettkampfdisziplin.feminim == 0 && !wertung.athlet.geschlecht.equalsIgnoreCase("M")) {
          false
        }
        else if(wertung.wettkampfdisziplin.masculin == 0 && wertung.athlet.geschlecht.equalsIgnoreCase("M")) {
          false
        }
        else {
          riegendurchgang2.forall{x =>
            x.durchgang.nonEmpty &&
            x.durchgang.forall{d =>
              d.nonEmpty &&
              disziplinsZuDurchgangR2.get(d).exists(dm => dm.contains(wertung.wettkampfdisziplin.disziplin))
            }
          }
        }
      }.map(_.wettkampfdisziplin.disziplin),
      athletwertungen
      )
    }
  }
}