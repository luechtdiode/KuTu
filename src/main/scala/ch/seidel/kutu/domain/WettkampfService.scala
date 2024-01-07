package ch.seidel.kutu.domain

import ch.seidel.kutu.akka.{AthletMovedInWettkampf, AthletRemovedFromWettkampf, DurchgangChanged, ScoresPublished}
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.squad.RiegenBuilder
import ch.seidel.kutu.squad.RiegenBuilder.{generateRiegen2Name, generateRiegenName}
import org.slf4j.LoggerFactory
import slick.jdbc.PositionedResult

import java.sql.Date
import java.util.UUID
import scala.util.matching.Regex
//import slick.jdbc.SQLiteProfile.api._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait WettkampfService extends DBService 
  with WettkampfResultMapper
  with WertungResultMapper
  with WertungService
  with RiegenService 
  with RiegenBuilder {
  
  private val logger = LoggerFactory.getLogger(this.getClass)

  def readNotenModus(id: Long, pgm: ProgrammView, notenfaktor: Double): NotenModus = {
    if(pgm.head.id == 1) {
      val skala = sql"""
                   select kurzbeschreibung, punktwert from notenskala
                   where wettkampfdisziplin_id=${id}
                   order by punktwert
        """.as[(String,Double)].withPinnedSession      
      val skalamap = Await.result(database.run(skala.withPinnedSession), Duration.Inf).toMap
      Athletiktest(skalamap, notenfaktor)
    }
    else if (pgm.toPath.contains("Turn10")) {
      // Turn10 special score-labels
      StandardWettkampf(notenfaktor, "A", "B")
    }
    else {
      StandardWettkampf(notenfaktor)
    }
  }

  def listRootProgramme(): List[ProgrammView] = {
    Await.result(listRootProgrammeAsync, Duration.Inf)
  }

  def listRootProgrammeAsync: Future[List[ProgrammView]] = {
    val allPgmsQuery =
      sql"""select
            id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode
            from programm where parent_id is null or parent_id = 0""".as[ProgrammRaw]
        .map { l => l.map(p => p.id -> p).toMap }
        .map { map =>
          map.foldLeft(List[ProgrammView]()) { (acc, pgmEntry) =>
            val (id, pgm) = pgmEntry
            if (pgm.parentId > 0) {
              acc :+ pgm.withParent(map(pgm.parentId).toView)
            } else {
              acc :+ pgm.toView
            }
          }
        }
    database.run(allPgmsQuery.withPinnedSession)
  }

  def initPlanZeitenActions(wettkampfUUID: UUID) =
      sqlu"""
                    DELETE FROM wettkampf_plan_zeiten where wettkampf_id = (
                    select id from wettkampf wk where wk.uuid= ${wettkampfUUID.toString})""" >>
      sqlu"""       INSERT INTO wettkampf_plan_zeiten
                        (wettkampfdisziplin_id, wettkampf_id, wechsel, einturnen, uebung, wertung)
                    SELECT
                        wkd.id as wettkampfdisziplin_id,
                        wk.id as wettkampf_id,
                        30000 as wechsel,
                        30000 as eintrunen,
                        case
                            when pd.name in ('K1', 'K2', 'K3', 'K4') then 40000
                            when pd.name like 'P%' then 60000
                            else 50000
                        end as uebung,
                        case
                            when pd.name in ('K1', 'K2', 'K3', 'K4') then 40000
                            when pd.name like 'P%' then 60000
                            else 50000
                        end as wertung
                    FROM
                        wettkampf wk
                        inner join programm p on (wk.programm_id in (p.id, p.parent_id))
                        inner join programm pd on (p.id = pd.parent_id)
                        inner join wettkampfdisziplin wkd on (pd.id = wkd.programm_id)
                        inner join disziplin d on (d.id = wkd.disziplin_id)
                     where wk.uuid = ${wettkampfUUID.toString}
      """

  def selectPlanZeitenAction(wettkampfUUID: UUID) =
    sql"""
                    select
                        wpt.id,
                        wk.id, wk.uuid, wk.datum, wk.titel, wk.programm_id, wk.auszeichnung, wk.auszeichnungendnote, wk.notificationEMail, wk.altersklassen, wk.jahrgangsklassen, wk.punktegleichstandsregel, wk.rotation, wk.teamrule, wd.id, wd.programm_id,
                        d.*,
                        wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.masculin, wd.feminim, wd.ord, wd.scale, wd.dnote, wd.min, wd.max, wd.startgeraet,
                        wpt.wechsel, wpt.einturnen, wpt.uebung, wpt.wertung
                    from wettkampf_plan_zeiten wpt
                        inner join wettkampf wk on (wk.id = wpt.wettkampf_id)
                        inner join wettkampfdisziplin wd on (wd.id = wpt.wettkampfdisziplin_id)
                        inner join disziplin d on (wd.disziplin_id = d.id)
                    where wk.uuid = ${wettkampfUUID.toString}
      """

  def initWettkampfDisziplinTimes(wettkampfUUID: UUID): List[WettkampfPlanTimeView] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    Await.result(database.run(
      initPlanZeitenActions(wettkampfUUID) >>
      selectPlanZeitenAction(wettkampfUUID)
      .as[WettkampfPlanTimeView].withPinnedSession).map(_.toList), Duration.Inf)
  }

  def loadWettkampfDisziplinTimes(wettkampfUUID: UUID): List[WettkampfPlanTimeView] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    Await.result(database.run(
      selectPlanZeitenAction(wettkampfUUID)
      .as[WettkampfPlanTimeView].withPinnedSession).map(_.toList), Duration.Inf)
  }

  def updateWettkampfPlanTimeView(planTime: WettkampfPlanTimeRaw): Unit = {
    Await.result(database.run(
      sqlu"""
                    update wettkampf_plan_zeiten
                        set wechsel = ${planTime.wechsel},
                            einturnen = ${planTime.einturnen},
                            uebung = ${planTime.uebung},
                            wertung = ${planTime.wertung}
                    where
                        wettkampf_id = ${planTime.wettkampfId}
                        and wettkampfdisziplin_id = ${planTime.wettkampfDisziplinId}
      """), Duration.Inf)
  }

  def updateOrInsertPlanTimes(planTimes: Iterable[WettkampfPlanTimeRaw]): Unit = {
    def insertPlanTime(rs: Iterable[WettkampfPlanTimeRaw]) = DBIO.sequence(for {
      planTime <- rs
    } yield {
      sqlu"""
                insert into wettkampf_plan_zeiten
                (wettkampf_Id, wettkampfdisziplin_id, wechsel, einturnen, uebung, wertung)
                values (${planTime.wettkampfId}, ${planTime.wettkampfDisziplinId}, ${planTime.wechsel}, ${planTime.einturnen},
                ${planTime.uebung}, ${planTime.wertung})
        """
    })

    val process = DBIO.sequence(for {
      (wettkampfid, planTime) <- planTimes.groupBy(_.wettkampfId)
    } yield {
      sqlu"""
                delete from wettkampf_plan_zeiten where
                wettkampf_id=${wettkampfid}
        """>>
        insertPlanTime(planTime)
    })

    Await.result(database.run{process.transactionally}, Duration.Inf)
  }

  def listOverviewStats(wettkampfUUID: UUID): List[OverviewStatTuple] = Await.result(
    database.run {
      sql"""
           with s as (
                select distinct v.name as verein, p.name as programm, p.ord as ord, a.geschlecht, a.id
                from verein v
                inner join athlet a on a.verein = v.id
                inner join wertung w on w.athlet_id = a.id
                inner join wettkampf wk on wk.id = w.wettkampf_id
                inner join wettkampfdisziplin wd on wd.id = w.wettkampfdisziplin_id
                inner join programm p on wd.programm_id = p.id
                where wk.uuid = ${wettkampfUUID.toString}
                )
           select verein, programm, ord,
                count(case geschlecht when 'M' then 1 else null end) as m,
                count(case geschlecht when 'W' then 1 else null end) as w
           from s
           group by verein, programm, ord
           order by verein, ord
         """.as[OverviewStatTuple].withPinnedSession
        .map(_.toList)
    }, Duration.Inf)
  def listAKOverviewFacts(wettkampfUUID: UUID): List[(String,ProgrammView,Int,String,Date)] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()

    def getResultMapper(r: PositionedResult)(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]) = {
      val verein = r.<<[String]
      val pgm = readProgramm(r.<<[Long], cache)
      (verein,pgm,r.<<[Int],r.<<[String],r.<<[Date],r.<<[Long])
    }
    Await.result(
      database.run {
        sql"""
          select distinct v.name as verein, p.id, p.ord as ord, a.geschlecht, a.gebdat, a.id
          from verein v
          inner join athlet a on a.verein = v.id
          inner join wertung w on w.athlet_id = a.id
          inner join wettkampf wk on wk.id = w.wettkampf_id
          inner join wettkampfdisziplin wd on wd.id = w.wettkampfdisziplin_id
          inner join programm p on wd.programm_id = p.id
          where wk.uuid = ${wettkampfUUID.toString} and a.gebdat is not null
         """.as[(String,ProgrammView,Int,String,Date,Long)](getResultMapper).withPinnedSession
          .map(_.toList.map(x => (x._1,x._2,x._3,x._4,x._5)))
      }, Duration.Inf)
  }

  def listPublishedScores(wettkampfUUID: UUID): Future[List[PublishedScoreView]] = {
    database.run(sql"""select sc.id, sc.title, sc.query, sc.published, sc.published_date, wk.*
           from published_scores sc
           inner join wettkampf wk on wk.id = sc.wettkampf_id
           where wk.uuid = ${wettkampfUUID.toString}
         """.as[PublishedScoreView].withPinnedSession).map(_.toList)
  }

  def savePublishedScore(wettkampfId: Long, title: String, query: String, published: Boolean, propagate: Boolean) = {
    val scoreId = UUID.randomUUID().toString
    val process: DBIOAction[PublishedScoreView, NoStream, Effect] =
      sqlu"""
                    delete from published_scores where title = $title and wettkampf_id = $wettkampfId""" >>
      sqlu"""
                    insert into published_scores
                    (id, title, query, wettkampf_id, published) values ($scoreId, $title, $query, $wettkampfId, $published)""" >>
      sql"""
                    select sc.id, sc.title, sc.query, sc.published, sc.published_date, wk.*
                    from published_scores sc
                    inner join wettkampf wk on wk.id = sc.wettkampf_id
                    where sc.id = $scoreId
         """.as[PublishedScoreView].head

    val publishedScoreView = Await.result(database.run(process.transactionally), Duration.Inf)
    if (propagate) {
      val ps = ScoresPublished(publishedScoreView.id, publishedScoreView.title, publishedScoreView.query, publishedScoreView.published, publishedScoreView.wettkampf.uuid.get)
      WebSocketClient.publish(ps)
    }
    publishedScoreView
  }

  private def insertPublishedScores(scs: Iterable[PublishedScoreRaw]) = DBIO.sequence(for {
    score <- scs
  } yield {
    sqlu"""
                delete from published_scores where
                wettkampf_id=${score.wettkampfId} and (id=${score.id} or title=${score.title})
        """>>
    sqlu"""     insert into published_scores (id, title, query, wettkampf_id, published, published_date)
                values
                (${score.id}, ${score.title}, ${score.query}, ${score.wettkampfId}, ${score.published}, ${score.publishedDate})""">>
    sql"""
                select sc.id as sc_id, sc.title, sc.query, sc.published, sc.published_date, wk.*
                from published_scores sc
                inner join wettkampf wk on wk.id = sc.wettkampf_id
                where sc.id = ${score.id} and wettkampf_id=${score.wettkampfId}
       """.as[PublishedScoreView].head
  })

  def updatePublishedScore(wettkampfId: Long, scoreId: String, title: String, query: String, published: Boolean, propagate: Boolean) = {
    val publishedScoreView: PublishedScoreView = Await.result(database.run(
      insertPublishedScores(Seq(PublishedScoreRaw(scoreId, title, query, published, new Date(System.currentTimeMillis()), wettkampfId)))
        .transactionally), Duration.Inf).head

    if (propagate) {
      val ps = ScoresPublished(publishedScoreView.id, publishedScoreView.title, publishedScoreView.query, publishedScoreView.published, publishedScoreView.wettkampf.uuid.get)
      WebSocketClient.publish(ps)
    }
    publishedScoreView
  }

  def updateOrinsertScoreDefs(scores: Iterable[PublishedScoreRaw]): Unit = {
    val process = DBIO.sequence(for {
      (wettkampfid, scores) <- scores.groupBy(_.wettkampfId)
    } yield {
      sqlu"""
                delete from published_scores where
                wettkampf_id=${wettkampfid}
        """>>
        insertPublishedScores(scores)
    })

    Await.result(database.run{process.transactionally}, Duration.Inf)
  }

  def readProgramm(id: Long): ProgrammView = {
    val allPgmsQuery = sql"""select id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode from programm""".as[ProgrammRaw]
      .map { l => l.map(p => p.id -> p).toMap }
      .map { map =>
        def resolve(id: Long): ProgrammView = {
          val item = map(id)
          val parent = item.parentId
          if (parent > 0) {
            item.withParent(resolve(parent))
          } else {
            item.toView
          }
        }

        resolve(id)
      }
    Await.result(database.run(allPgmsQuery.withPinnedSession), Duration.Inf)
  }
  
  def readProgramm(id: Long, cache: scala.collection.mutable.Map[Long, ProgrammView]): ProgrammView = {
    cache.getOrElseUpdate(id, readProgramm(id))
  }

  def listWettkaempfe = {
     sql"""       select * from wettkampf order by datum desc""".as[Wettkampf]
  }
  def listWettkaempfeAsync = {
     database.run{ listWettkaempfe.withPinnedSession }
  }

  def listWettkaempfeByVereinId(vereinId: Long) = {
     sql"""       select wk.* from wettkampf wk where exists (
                    select 1 from wertung wr, athlet a where wr.wettkampf_id = wk.id and wr.athlet_id = a.id and a.verein = $vereinId
                  )
                  order by wk.datum desc""".as[Wettkampf]
  }
  def listWettkaempfeByVereinIdAsync(vereinId: Long) = {
     database.run{ listWettkaempfeByVereinId(vereinId).withPinnedSession }
  }

  def listWettkaempfeViewAsync = {
    database.run{
      sql"""      select * from wettkampf order by datum desc""".as[WettkampfView].withPinnedSession
    }
  }
  
  def listWettkaempfeView = {
    Await.result(listWettkaempfeViewAsync, Duration.Inf)
  }

  def listRiegen2ToRiegenMapZuWettkampf(wettkampf: Long) = {
    Await.result(database.run{(
      sql"""
                  SELECT distinct w.riege2, w.riege
                  FROM wertung w
                  where w.riege is not null and w.riege2 is not null and w.wettkampf_id = $wettkampf
       """.as[(String, String)]).withPinnedSession
    }, Duration.Inf).groupBy(_._1).map(x => (x._1, x._2.map(_._2).toList))
  }

  def listRiegenZuWettkampf(wettkampf: Long) = {
    Await.result(database.run{(
      sql"""
                  SELECT distinct w.riege, count(distinct w.athlet_id), r.durchgang, d.*
                  FROM wertung w
                  left outer join riege r on (r.name = w.riege and r.wettkampf_id = w.wettkampf_id)
                  left outer join disziplin d on (d.id = r.start)
                  where w.riege is not null and w.wettkampf_id = $wettkampf
                  group by w.riege, r.durchgang, d.id
                  union SELECT distinct w.riege2 as riege, count(distinct w.athlet_id), r.durchgang, d.*
                  FROM wertung w
                  left outer join riege r on (r.name = w.riege2 and r.wettkampf_id = w.wettkampf_id)
                  left outer join disziplin d on (d.id = r.start)
                  where w.riege2 is not null and w.wettkampf_id = $wettkampf
                  group by w.riege2, r.durchgang, d.id
                  union SELECT distinct r.name, 0 as cnt, r.durchgang, d.*
                  FROM riege r
                  inner join disziplin d on (d.id = r.start)
                  where
                    r.wettkampf_id = $wettkampf
                    and not exists (select 1 from wertung w where w.riege = r.name or w.riege2 = r.name and r.wettkampf_id = w.wettkampf_id)
       """.as[(String, Int, Option[String], Option[Disziplin])]).withPinnedSession
    }, Duration.Inf)
  }

  def wettkampfExists(uuid: String): Boolean = {
    Await.result(wettkampfExistsAsync(uuid), Duration.Inf)
  }
  def wettkampfExistsAsync(uuid: String) = {
    database.run{
      (sql"""      select count(*) from wettkampf where uuid=$uuid""".as[Int].head.map(_ > 0)).withPinnedSession
    }
  } 
  def readWettkampfAsync(uuid: String) = {
    database.run{
      (sql"""      select * from wettkampf where uuid=$uuid""".as[Wettkampf].head).withPinnedSession
    }
  }  
    
  def readWettkampf(uuid: String) = {
    Await.result(readWettkampfAsync(uuid), Duration.Inf)
  }
  
  def readWettkampfAsync(id: Long) = {
    database.run{
      (sql"""      select * from wettkampf where id=$id""".as[Wettkampf].head).withPinnedSession
    }
  }
  
  def readWettkampf(id: Long) = {
    Await.result(readWettkampfAsync(id), Duration.Inf)
  }
  
  def readWettkampfLeafs(programmid: Long): Seq[ProgrammView] = {
    def children(pid: Long) = Await.result(database.run{
        (sql"""    select * from programm
                  where parent_id=$pid
           """.as[ProgrammRaw]).withPinnedSession
      }, Duration.Inf)

    def seek(pid: Long, acc: Seq[ProgrammView]): Seq[ProgrammView] = {
      val ch = children(pid)
      if(ch.isEmpty) {
        acc :+ readProgramm(pid)
      }
      else {
        (for(c <- ch) yield (seek(c.id, acc))).flatten
      }
    }
    seek(programmid, Seq.empty)
  }

  def createWettkampf(datum: java.sql.Date, titel: String, programmId: Set[Long], notificationEMail: String, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, uuidOption: Option[String], altersklassen: String, jahrgangsklassen: String, punktegleichstandsregel: String, rotation: String, teamrule: String): Wettkampf = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val programs = programmId map (p => readProgramm(p, cache))
    val heads = programs map (_.head)
    val uuid = uuidOption.getOrElse(UUID.randomUUID().toString())
    val initialCheck = uuidOption match {
      case Some(suuid) => sql"""
                  select max(id) as maxid
                  from wettkampf
                  where uuid=$suuid
             """.as[Long]
      case _ => sql"""
                  select max(id) as maxid
                  from wettkampf
                  where LOWER(titel)=${titel.toLowerCase()} and programm_id = ${heads.head.id} and datum=$datum
             """.as[Long]
    }
    val dublicateCheck = uuidOption match {
      case Some(suuid) => sql"""
                  select count(id) as wkcount
                  from wettkampf
                  where LOWER(titel)=${titel.toLowerCase()} and programm_id = ${heads.head.id} and datum=$datum
                    and uuid is not null
                    and uuid<>$suuid
             """.as[Long]
      case _ => sql"""
                  select 0 as wkcount
                  from wettkampf
                  where LOWER(titel)=${titel.toLowerCase()} and programm_id = ${heads.head.id} and datum=$datum
             """.as[Long]
    }
    if (!heads.forall { h => h.id == heads.head.id }) {
      throw new IllegalArgumentException("Programme nicht aus der selben Gruppe können nicht in einen Wettkampf aufgenommen werden")
    }
    val process = initialCheck
      .headOption
      .flatMap{
        case Some(cid) if(cid > 0) =>
          deleteWettkampfRelationActions(cid) >>
          sqlu"""
                update wettkampf
                set datum=$datum, titel=$titel, programm_Id=${heads.head.id},
                    notificationEMail=$notificationEMail,
                    auszeichnung=$auszeichnung, auszeichnungendnote=$auszeichnungendnote,
                    altersklassen=$altersklassen, jahrgangsklassen=$jahrgangsklassen,
                    punktegleichstandsregel=$punktegleichstandsregel,
                    rotation=$rotation,
                    teamrule=$teamrule
                where id=$cid and uuid=$uuid
            """ >>
          initPlanZeitenActions(UUID.fromString(uuid)) >>
          sql"""
                  select * from wettkampf
                  where id=$cid
             """.as[Wettkampf].head
        case _ => 
          sqlu"""
                  insert into wettkampf
                  (datum, titel, programm_Id, notificationEMail, auszeichnung, auszeichnungendnote, punktegleichstandsregel, altersklassen, jahrgangsklassen, rotation, teamrule, uuid)
                  values (${datum}, ${titel}, ${heads.head.id}, $notificationEMail, $auszeichnung, $auszeichnungendnote, $punktegleichstandsregel, $altersklassen, $jahrgangsklassen, $rotation, $teamrule, $uuid)
              """ >>
          initPlanZeitenActions(UUID.fromString(uuid)) >>
          sql"""
                  select * from wettkampf
                  where id in (select max(id) from wettkampf)
             """.as[Wettkampf].head
      }
      
    Await.result(database.run(process.transactionally), Duration.Inf)
  }

  def createUUIDForWettkampf(id: Long): Wettkampf = {
    val uuid = UUID.randomUUID()
    Await.result(database.run {
      sqlu"""     update wettkampf
                  set uuid=${uuid.toString}
                  where id=$id
          """ >>
      sql"""      select * from wettkampf
                  where id = $id
         """.as[Wettkampf].head.transactionally
    }, Duration.Inf)
  }

  def saveWettkampf(id: Long, datum: java.sql.Date, titel: String, programmId: Set[Long], notificationEMail: String, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, uuidOption: Option[String], altersklassen: String, jahrgangsklassen: String, punktegleichstandsregel: String, rotation: String, teamrule: String): Wettkampf = {
    val uuid = uuidOption.getOrElse(UUID.randomUUID().toString())
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val process = for {
        existing <- sql"""
                  select * from wettkampf
                  where id = $id
                      """.as[Wettkampf]
        hasWertungen <- sql"""
                  select count(*) from wertung where wettkampf_id=$id""".as[Int]
    } yield {
      val programs = (programmId + existing.head.programmId) map (p => readProgramm(p, cache))
      val heads = programs map (_.head)
      if (hasWertungen.head > 0 && !heads.forall { h => h.id == heads.head.id }) {
        throw new IllegalArgumentException("Es kann keine Programmanpassung gemacht werden, wenn bereits Turner zum Wettkampf verknüpft sind.")
      }
      sqlu"""     update wettkampf
                  set datum=$datum,
                      titel=$titel,
                      programm_Id=${heads.head.id},
                      notificationEMail=$notificationEMail,
                      auszeichnung=$auszeichnung,
                      auszeichnungendnote=$auszeichnungendnote,
                      altersklassen=$altersklassen, jahrgangsklassen=$jahrgangsklassen,
                      punktegleichstandsregel=$punktegleichstandsregel,
                      rotation=$rotation,
                      teamrule=$teamrule,
                      uuid=$uuid
                  where id=$id
          """ >>
       sql"""     select * from wettkampf
                  where id = $id
          """.as[Wettkampf].head
    }
    Await.result(database.run{(process.flatten).transactionally}, Duration.Inf)
  }

  def deleteWettkampfRelationActions(wettkampfid: Long) = {
      sqlu"""      delete from published_scores where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from durchgangstation where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from durchgang where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from wettkampf_plan_zeiten where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from riege where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from wertung where wettkampf_id=${wettkampfid}"""
  }

  def deleteWettkampfActions(wettkampfid: Long) = {
    deleteWettkampfRelationActions(wettkampfid) >>
      sqlu"""      delete from athletregistration where vereinregistration_id in (select id from vereinregistration where wettkampf_id=${wettkampfid})""" >>
      sqlu"""      delete from judgeregistration_pgm where vereinregistration_id in (select id from vereinregistration where wettkampf_id=${wettkampfid})""" >>
      sqlu"""      delete from judgeregistration where vereinregistration_id in (select id from vereinregistration where wettkampf_id=${wettkampfid})""" >>
      sqlu"""      delete from vereinregistration where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from wettkampf where id=${wettkampfid}"""
  }

  def deleteWettkampf(wettkampfid: Long): Unit = {
    Await.result(database.run{
      deleteWettkampfActions(wettkampfid).transactionally
    }, Duration.Inf)
  }
//  
//  def completeDisziplinListOfAthletInWettkampf(wettkampf: Wettkampf, athletId: Long) = {
//    val wertungen = listAthletWertungenZuWettkampf(athletId, wettkampf.id)
//    val wpgms = wertungen.map(w => w.wettkampfdisziplin.programm.id)
//    val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).filter(id => wpgms.contains(id)).toSet
//    
//    val pgmwkds: Map[Long,Vector[Long]] = programme.map{x =>
//        x -> Await.result(database.run{sql""" select id from wettkampfdisziplin
//                    where programm_Id = ${x}
//                      and id not in (select wettkampfdisziplin_Id from wertung
//                                     where athlet_Id=${athletId}
//                                       and wettkampf_Id=${wettkampf.id})""".as[Long]}, Duration.Inf)
//    }.toMap
//    val completed = programme.
//      map{pgmwkds}.flatMap{wkds => wkds.map{wkd =>
//        Await.result(database.run{
//        sqlu"""
//                  insert into wertung
//                  (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
//                  values (${athletId}, ${wkd}, ${wettkampf.id}, 0, 0, 0)
//          """}, Duration.Inf)
//        wkd
//    }}
//    completed    
//  }

  def unassignAthletFromWettkampf(raw: AthletRemovedFromWettkampf) = {
    val athlet = raw.athlet
    val durchgaenge: Set[String] = Await.result(database.run{
      sql"""      select distinct durchgang from riege r inner join wertung w on (
                    w.riege = r.name or w.riege2 = r.name
                  )
                  where
                     athlet_id = ${athlet.id}
                     and w.wettkampf_Id = (select id
                       from wettkampf
                       where uuid=${raw.wettkampfUUID})
              """.as[(String)].transactionally
    }, Duration.Inf).toSet

    Await.result(database.run{
      sqlu"""      delete from wertung
                   where
                     athlet_id = ${athlet.id}
                     and wettkampf_Id = (select id
                       from wettkampf
                       where uuid=${raw.wettkampfUUID})
              """.transactionally
    }, Duration.Inf)

    durchgaenge
  }

  def unassignAthletFromWettkampf(wertungId: Set[Long]): Unit = {
    val wertung = getWertung(wertungId.head)
    val durchgaenge: Set[String] = Await.result(database.run{
      sql"""      select distinct durchgang from riege r inner join wertung w on (
                    w.riege = r.name or w.riege2 = r.name
                  )
                  where
                     w.id in (#${wertungId.mkString(",")})
              """.as[(String)].transactionally
    }, Duration.Inf).toSet

    Await.result(database.run{
      sqlu"""
                   delete from wertung
                   where id in (#${wertungId.mkString(",")})
              """.transactionally
    }, Duration.Inf)

    val awu = AthletRemovedFromWettkampf(wertung.athlet, wertung.wettkampf.uuid.get)
    WebSocketClient.publish(awu)
    for(durchgang <- durchgaenge) {
      WebSocketClient.publish(DurchgangChanged(durchgang, wertung.wettkampf.uuid.get, wertung.athlet))
    }
  }

  def assignAthletsToWettkampf(wettkampfId: Long, programmIds: Set[Long], withAthlets: Set[Long] = Set.empty, team: Option[Int]): Unit = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val programs = programmIds map (p => readProgramm(p, cache))
    assignAthletsToWettkampfS(wettkampfId, programs, withAthlets, team)
  }

  def assignAthletsToWettkampfS(wettkampfId: Long, programs: Set[ProgrammView], withAthlets: Set[Long] = Set.empty, team: Option[Int]): Unit = {
    if (withAthlets.nonEmpty) {
      val disciplines = Await.result(database.run{(sql"""
                   select id from wettkampfdisziplin
                   where programm_Id in #${programs.map(_.id).mkString("(", ",", ")")}
           """.as[Long]).withPinnedSession}, Duration.Inf)
           
      withAthlets.foreach{aid =>
        disciplines.foreach{case disciplin =>
          Await.result(database.run{(
            sqlu"""
                     delete from wertung where
                     athlet_Id=${aid} and wettkampfdisziplin_Id=${disciplin} and wettkampf_Id=${wettkampfId}
                 """ >>
             sqlu"""
                     insert into wertung
                     (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, team)
                     values (${aid}, ${disciplin}, ${wettkampfId}, ${team.getOrElse(0)})
                """).transactionally
          }, Duration.Inf)
        }
      }
      val wertungen = Await.result(database.run{sql"""
                   select wd.id, w.id from wertung w inner join wettkampfdisziplin wd on (w.wettkampfdisziplin_Id = wd.id)
                   where
                     athlet_Id in (#${withAthlets.mkString(",")})
                     and wettkampf_Id = $wettkampfId
              """.as[(Long,Long)].transactionally
      }, Duration.Inf)
        .map(t => {
          val wertung = getWertung(t._2).copy(wettkampfdisziplin = readWettkampfDisziplinView(t._1))
          (t._2,
            generateRiegenName(wertung),
            generateRiegen2Name(wertung),
          )
        })

      Await.result(database.run{
        DBIO.sequence(for(w <- wertungen) yield {
          val (wertungId, riegeText1, riegeText2) = w
          riegeText2 match {
            case Some(riegeText2) =>
              sqlu"""    UPDATE wertung
                   SET riege=${riegeText1}
                     , riege2=${riegeText2}
                   WHERE id=${wertungId}
              """
            case _ =>
              sqlu"""    UPDATE wertung
                   SET riege=${riegeText1}
                   WHERE id=${wertungId}
              """
          }
        }).transactionally
      }, Duration.Inf)
    }
  }

  def moveToProgram(event: AthletMovedInWettkampf) = {
    val wettkampf = readWettkampf(event.wettkampfUUID)
    val wkdIDs = Await.result(database.run{sql"""
                   select disziplin_id, id from wettkampfdisziplin
                   where programm_Id = ${event.pgmId}
                """.as[(Long,Long)].withPinnedSession}, Duration.Inf).toMap
    val athlet = event.athlet
    val existingriegen = selectRiegenRaw(wettkampf.id)
    val wertungen: Seq[(Long, Long, String, String, Option[String], String, Int, Int)] = Await.result(database.run{sql"""
                   select wd.disziplin_id, w.id, w.riege, w.riege2, w.team from wertung w inner join wettkampfdisziplin wd on (w.wettkampfdisziplin_Id = wd.id)
                   where
                     athlet_id = ${athlet.id}
                     and wettkampf_Id = ${wettkampf.id}
              """.as[(Long,Long, String, String, Int)].transactionally
    }, Duration.Inf)
      .map{t =>
        val (wkdId, wertungId, oldRiegenName, oldRiegen2Name, oldTeam) = t
        val newWettkampfDisziplinId = wkdIDs(wkdId)
        val wkDiszView = readWettkampfDisziplinView(newWettkampfDisziplinId)
        val wertung = getWertung(wertungId)
        val newWertung = wertung.copy(wettkampfdisziplin = wkDiszView, team = event.team)
        (newWertung.id, newWertung.wettkampfdisziplin.id,
          generateRiegenName(newWertung), oldRiegenName,
          generateRiegen2Name(newWertung), oldRiegen2Name,
          newWertung.team, oldTeam
        )
      }

    val riegenset: Map[String,String] = wertungen
      .map(w => (w._4, w._3)).toSet.map { w: (String, String) =>
      val (oldRiege, newRiege) = w
      findMatchingOldNewRiegen(existingriegen, oldRiege, newRiege)
    }.toMap

    val riegenset2: Map[String,String] = wertungen
      .filter(w => w._5.nonEmpty)
      .map(w => (w._6, w._5.get)).toSet.map { w: (String, String) =>
      val (oldRiege2, newRiege2) = w
      findMatchingOldNewRiegen(existingriegen, oldRiege2, newRiege2)
    }.toMap

    val process = for (w <- wertungen) yield {
      val riegeText: String = riegenset.getOrElse(w._4, w._3)
      w._5 match {
        case Some(oldRiegeText2) if (riegenset2.contains(w._6)) =>
          val riegeText2: String = riegenset2.getOrElse(w._6, oldRiegeText2)
          sqlu"""    UPDATE wertung
                   SET riege=${riegeText}
                     , riege2=${riegeText2}
                     , wettkampfdisziplin_Id=${w._2}
                     , team=${w._7}
                   WHERE id=${w._1}
          """
        case _ =>
          sqlu"""    UPDATE wertung
                   SET riege=${riegeText}
                     , wettkampfdisziplin_Id=${w._2}
                     , team=${w._7}
                   WHERE id=${w._1}
          """
      }
    }

    Await.result(database.run{
      DBIO.sequence(process).transactionally
    }, Duration.Inf)

    cleanUnusedRiegen(wettkampf.id)

    Await.result(database.run{
      DBIO.sequence(for(w <- riegenset.toVector) yield {
        sql"""    select distinct durchgang from riege
                  where wettkampf_id = (select id
                    from wettkampf
                    where uuid=${event.wettkampfUUID})
                  and name in ('#${w._1}', '#${w._2}')
        """.as[String]
      }).transactionally
    }, Duration.Inf).flatten
  }

  private def findMatchingOldNewRiegen(existingriegen: List[RiegeRaw], oldRiege: String, newRiege: String): (String, String) = {
    existingriegen.find { r => oldRiege.equalsIgnoreCase(r.r) } match {
      case Some(matchingRiege) =>
        existingriegen.find { r => newRiege.equalsIgnoreCase(r.r) } match {
          case Some(matchingRiege) =>
            (oldRiege, matchingRiege.r)
          case _ => (oldRiege, findAndStoreMatchingRiege(matchingRiege.copy(r = newRiege)).r)
        }

      case _ => (oldRiege, newRiege)
    }
  }

  def moveToProgram(wId: Long, pgmId: Long, team: Int, athelteView: AthletView): Unit = {
    val wettkampf = readWettkampf(wId)
    val movedInWettkampf = AthletMovedInWettkampf(athelteView, wettkampf.uuid.getOrElse(""), pgmId, team)
    val durchgaenge = moveToProgram(movedInWettkampf)

    WebSocketClient.publish(movedInWettkampf)

    for(durchgang <- durchgaenge) {
      WebSocketClient.publish(DurchgangChanged(durchgang, wettkampf.uuid.get, athelteView))
    }
  }

  def insertWettkampfProgram(rootprogram: String, riegenmode: Int, maxScore: Int, dnoteUsed: Int, disziplinlist: List[String], programlist: List[String]): List[WettkampfdisziplinView] = {
    val programme = Await.result(database.run{(sql"""
                   select * from programm
           """.as[ProgrammRaw]).withPinnedSession}, Duration.Inf)

    if (programme.exists(p => p.name.equalsIgnoreCase(rootprogram))) {
      // alternative: return existing associated WettkampfdisziplinViews
      throw new RuntimeException(s"Name des Rootprogrammes ist nicht eindeutig: $rootprogram")
    }
    val wkdisciplines = Await.result(database.run{(sql"""
                   select * from wettkampfdisziplin
           """.as[Wettkampfdisziplin]).withPinnedSession}, Duration.Inf)
    val disciplines = Await.result(database.run{(sql"""
                   select * from disziplin
           """.as[Disziplin]).withPinnedSession}, Duration.Inf)
    val nextWKDiszId: Long = wkdisciplines.map(_.id).max + 1L
    val pgmIdRoot: Long = wkdisciplines.map(_.programmId).max + 1L
    val nextPgmId: Long = pgmIdRoot + 1L

    val nameMatcher: Regex = "(?iumU)^([\\w\\h\\s\\d]+[^\\h\\s\\(])".r
    val alterVonMatcher: Regex = "(?iumU)\\(.*von=([\\d]{1,2}).*\\)$".r
    val alterBisMatcher: Regex = "(?iumU)\\(.*bis=([\\d]{1,2}).*\\)$".r
    val sexMatcher: Regex = "(?iumU)\\(.*sex=([mMwW]{1,2}).*\\)$".r
    val startMatcher: Regex = "(?iumU)\\(.*start=([01jJnNyYwWtTfF]{1}).*\\)$".r
    val sexMapping = (for {
      w <- disziplinlist
      name = nameMatcher.findFirstMatchIn(w).map(md => md.group(1)).mkString
    } yield {
      (name->sexMatcher.findFirstMatchIn(w).map(md => md.group(1)).mkString.toUpperCase)
    }).toMap
    val startMapping = (for {
      w <- disziplinlist
      name = nameMatcher.findFirstMatchIn(w).map(md => md.group(1)).mkString
    } yield {
      (name->startMatcher.findFirstMatchIn(w).map(md => md.group(1)).mkString.toUpperCase)
    }).toMap

    val diszInserts = for {
      w <- disziplinlist
      name = nameMatcher.findFirstMatchIn(w).map(md => md.group(1)).mkString
      if !disciplines.exists(_.name.equalsIgnoreCase(name))
    } yield {

      sqlu"""    INSERT INTO disziplin
                    (name)
                    VALUES
                      ($name)
          """ >>
      sql"""
               SELECT * from disziplin where name=$name
          """.as[Disziplin]
    }
    val insertedDiszList = (Await.result(database.run{
      DBIO.sequence(diszInserts).transactionally
    }, Duration.Inf).flatten ++ disziplinlist.flatMap{w =>
      val name = nameMatcher.findFirstMatchIn(w).map(md => md.group(1)).mkString
      disciplines.filter(d => d.name.equalsIgnoreCase(name))
    }).sortBy(d => disziplinlist.indexWhere(dl => dl.startsWith(d.name)))

    val rootPgmInsert = sqlu"""
            INSERT INTO programm
                  (id, name, aggregate, ord, riegenmode, uuid)
                  VALUES
                    ($pgmIdRoot, $rootprogram, 0, $pgmIdRoot, $riegenmode, ${UUID.randomUUID().toString})
          """  >>
      sql"""
                 SELECT * from programm where id=$pgmIdRoot
               """.as[ProgrammRaw]
    val aggregate = if (programlist.exists(_.contains("/"))) 1 else 0
    val items = programlist.flatMap(path => path.split("/")).distinct.zipWithIndex
    val parentItemsMap = items.flatMap{item =>
      val pgm=item._1
      programlist
        .find(path => path.contains(s"/$pgm"))
        .flatMap(path => path.split("/")
          .reverse
          .dropWhile(item => !item.equals(pgm))
          .tail.headOption)
        .flatMap(parentItem => items.find(it => it._1.equals(parentItem)))
        .map(parent => pgm -> (pgmIdRoot + 1L + parent._2).longValue())
    }.toMap
    val pgmInserts = rootPgmInsert +: (for {
      w <- items
    } yield {
      val (pgm, idx) = w
      val pgmId: Long = nextPgmId + idx
      val parentId: Long = parentItemsMap.getOrElse(pgm, pgmIdRoot)
      val name=nameMatcher.findFirstMatchIn(pgm).map(md => md.group(1)).mkString
      val von=alterVonMatcher.findFirstMatchIn(pgm).map(md => md.group(1).intValue).getOrElse(0)
      val bis=alterBisMatcher.findFirstMatchIn(pgm).map(md => md.group(1).intValue).getOrElse(100)
      sqlu"""    INSERT INTO programm
                    (id, parent_id, name, aggregate, ord, riegenmode, alter_von, alter_bis, uuid)
                    VALUES
                      ($pgmId, $parentId, $name, $aggregate, $pgmId, $riegenmode, $von, $bis, ${UUID.randomUUID().toString})
          """ >>
        sql"""
               SELECT * from programm where id=$pgmId
          """.as[ProgrammRaw]
    })

    val insertedPgmList = Await.result(database.run{
      DBIO.sequence(pgmInserts).transactionally
    }, Duration.Inf).flatten

    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val wkDiszsInserts = (for {
      pgm <- insertedPgmList
      if pgm.parentId > 0 && !insertedPgmList.exists(p => p.parentId == pgm.id)
      disz <- insertedDiszList
    } yield {
      (pgm, disz)
    }).zipWithIndex.map {
      case ((p, d), i) =>
        val id = i + nextWKDiszId
        val m = sexMapping.get(d.name) match {
          case Some(s) if s.contains("M") => 1
          case Some(s) if s.isEmpty => 1
          case _ => 0
        }
        val f = sexMapping.get(d.name) match {
          case Some(s) if s.contains("W") => 1
          case Some(s) if s.isEmpty => 1
          case _ => 0
        }
        val start = startMapping.get(d.name) match {
          case Some(s) if s.matches("[0nNfF]{1}") => 0
          case _ => 1
        }
        sqlu"""    INSERT INTO wettkampfdisziplin
                 (id, programm_id, disziplin_id, notenfaktor, ord, masculin, feminim, dnote, max, startgeraet)
                 VALUES
                 ($id, ${p.id}, ${d.id}, 1.000, $i, $m, $f, $dnoteUsed, $maxScore, $start)
          """ >>
          sql"""
               SELECT * from wettkampfdisziplin where id=$id
          """.as[Wettkampfdisziplin]
    }
    Await.result(database.run{
      DBIO.sequence(wkDiszsInserts).transactionally
    }, Duration.Inf).flatten.map{
      case w: Wettkampfdisziplin =>
        WettkampfdisziplinView(
          w.id,
          readProgramm(w.programmId, cache),
          insertedDiszList.find(d => d.id == w.disziplinId).get,
          w.kurzbeschreibung,
          w.detailbeschreibung,
          StandardWettkampf(1d),
          w.masculin, w.feminim, w.ord, w.scale, w.dnote, w.min, w.max, w.startgeraet
        )
    }
  }
}