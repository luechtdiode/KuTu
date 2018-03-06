package ch.seidel.kutu.domain

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import org.slf4j.LoggerFactory
import java.sql.Date

import slick.jdbc.GetResult
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import scala.collection.JavaConverters
import java.util.UUID

abstract trait WertungService extends DBService with WertungResultMapper with DisziplinService with RiegenService {
  private val logger = LoggerFactory.getLogger(this.getClass)

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
    Await.result(database.run{(
      sql"""
                    SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wd.masculin, wd.feminim,
                      wk.*,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2
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
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wd.masculin, wd.feminim,
                      wk.*,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2
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
  
  def updateOrinsertWertungenZuWettkampf(wettkampf: Wettkampf, wertungen: Iterable[Wertung]) = {
    val insertWertungenAction = DBIO.sequence(for {
      w <- wertungen
    } yield {
      sqlu"""
                insert into wertung
                (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote, riege, riege2)
                values (${w.athletId}, ${w.wettkampfdisziplinId}, ${w.wettkampfId}, ${w.noteD}, ${w.noteE}, ${w.endnote}, ${w.riege}, ${w.riege2})
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
  
  def updateOrinsertWertungen(wertungen: Iterable[Wertung]) = {
    val process = DBIO.sequence(for {
      w <- wertungen
    } yield {
      sqlu"""
                delete from wertung where
                athlet_Id=${w.athletId} and wettkampfdisziplin_Id=${w.wettkampfdisziplinId} and wettkampf_Id=${w.wettkampfId}
        """>>  
      sqlu"""
                insert into wertung
                (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote, riege, riege2)
                values (${w.athletId}, ${w.wettkampfdisziplinId}, ${w.wettkampfId}, ${w.noteD}, ${w.noteE}, ${w.endnote}, ${w.riege}, ${w.riege2})
        """>>
      sqlu"""   delete from riege
                WHERE wettkampf_id=${w.id} and not exists (
                  SELECT 1 FROM wertung w
                  WHERE w.wettkampf_id=${w.id}
                    and (w.riege=riege.name or w.riege2=riege.name)
                )
        """
      
    })
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
                (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote, riege, riege2)
                values (${w.athletId}, ${w.wettkampfdisziplinId}, ${w.wettkampfId}, ${w.noteD}, ${w.noteE}, ${w.endnote}, ${w.riege}, ${w.riege2})
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
    Await.result(database.run(DBIO.sequence(Seq(
      sqlu"""       UPDATE wertung
                    SET note_d=${w.noteD}, note_e=${w.noteE}, endnote=${w.endnote}, riege=${w.riege}, riege2=${w.riege2}
                    WHERE id=${w.id}
          """,

      sqlu"""       DELETE from riege
                    WHERE wettkampf_id=${w.id} and not exists (
                      SELECT 1 FROM wertung w
                      WHERE w.wettkampf_id=${w.id}
                        and (w.riege=name or w.riege2=name)
                    )
          """
    )).transactionally), Duration.Inf)

    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      //id |id |js_id |geschlecht |name |vorname |gebdat |strasse |plz |ort |verein |activ |id |name |id |programm_id |id |name |kurzbeschreibung |detailbeschreibung |notenfaktor |ord |masculin |feminim |id |datum |titel |programm_id |auszeichnung |difficulty |execution |endnote |riege |
    val wv = Await.result(database.run((sql"""
                    SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wd.masculin, wd.feminim,
                      wk.*,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    WHERE w.id=${w.id}
                    order by wd.programm_id, wd.ord
       """.as[WertungView].head).withPinnedSession), Duration.Inf)
    if(wv.endnote >= 8.7) {
      putWertungToBestenResults(wv)
    }
    wv    
  }

  def updateWertungSimple(w: Wertung, putToBestenresults: Boolean = false) = {
    try {      
      val wv = readWettkampfDisziplinView(w.wettkampfdisziplinId).notenSpez.verifiedAndCalculatedWertung(w)
      val wvId = Await.result(database.run((for {
          updated <- sqlu"""
                    UPDATE wertung
                    SET note_d=${wv.noteD}, note_e=${wv.noteE}, endnote=${wv.endnote}, riege=${wv.riege}, riege2=${wv.riege2}
                    WHERE 
                      athlet_Id=${w.athletId} and wettkampfdisziplin_Id=${w.wettkampfdisziplinId} and wettkampf_Id=${w.wettkampfId}
            """
          wvId <- sql"""
                    SELECT id FROM wertung
                    WHERE 
                      athlet_Id=${w.athletId} and wettkampfdisziplin_Id=${w.wettkampfdisziplinId} and wettkampf_Id=${w.wettkampfId}
          """.as[Long]
        } yield {
          wvId
        }).transactionally
      ), Duration.Inf).head
      // TODO - this feature is not able to serve for multiple competitions at same time
      if(putToBestenresults && wv.endnote >= 8.7) {
        putWertungToBestenResults(getWertung(wvId))
      }
      wv.copy(id = wvId)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def listAthletenWertungenZuProgramm(progids: Seq[Long], wettkampf: Long, riege: String = "%") = {
    Await.result(database.run{
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      (sql"""
                   SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                     wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wd.masculin, wd.feminim,
                     wk.*,
                     w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2
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
                     wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wd.masculin, wd.feminim,
                     wk.*,
                     w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2
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

  private var bestenResults = Map[String,WertungView]()
  private var shouldResetBestenResults = false
  
  def putWertungToBestenResults(wertung: WertungView) {
    if(shouldResetBestenResults) {
      bestenResults = Map[String,WertungView]()
      shouldResetBestenResults = false;
    }
    bestenResults = bestenResults.updated(wertung.athlet.id + ":" + wertung.wettkampfdisziplin.id, wertung)
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
  
  def resetBestenResults {
    shouldResetBestenResults = true;
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
      val einsatz = athletwertungen.head
      val athlet = einsatz.athlet
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
      ,AthletJahrgang(athlet.gebdat).hg
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
              disziplinsZuDurchgangR1.get(d).map(dm => dm.contains(wertung.wettkampfdisziplin.disziplin)).getOrElse(false)
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
              disziplinsZuDurchgangR2.get(d).map(dm => dm.contains(wertung.wettkampfdisziplin.disziplin)).getOrElse(false)
            }
          }
        }
      }.map(_.wettkampfdisziplin.disziplin),
      athletwertungen
      )
    }
  }
}