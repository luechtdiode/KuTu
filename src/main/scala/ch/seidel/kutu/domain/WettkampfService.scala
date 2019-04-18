package ch.seidel.kutu.domain

import java.util.UUID

import ch.seidel.kutu.akka.{AthletMovedInWettkampf, AthletRemovedFromWettkampf, AthletWertungUpdated}
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.squad.RiegenBuilder
import org.slf4j.LoggerFactory
import slick.jdbc.GetResult
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

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
    else if(pgm.head.id == 20) {
      GeTuWettkampf
    }
    else {
      KuTuWettkampf
    }
  }

  def listRootProgramme(): List[ProgrammView] = {
    val allPgmsQuery = sql"""select * from programm where parent_id is null or parent_id = 0""".as[ProgrammRaw]
        .map{l => l.map(p => p.id -> p).toMap}
        .map{map => map.foldLeft(List[ProgrammView]()){(acc, pgmEntry) =>
          val (id, pgm) = pgmEntry
          if (pgm.parentId > 0) {
            acc :+ pgm.withParent(map(pgm.parentId).toView)
          } else {
            acc :+ pgm.toView            
          }
        }
      }
    Await.result(database.run(allPgmsQuery.withPinnedSession), Duration.Inf)
  }

  def readProgramm(id: Long): ProgrammView = {
    val allPgmsQuery = sql"""select * from programm""".as[ProgrammRaw]
        .map{l => l.map(p => p.id -> p).toMap}
        .map{map => map.foldLeft(List[ProgrammView]()){(acc, pgmEntry) =>
            val (id, pgm) = pgmEntry
            if (pgm.parentId > 0) {
              acc :+ pgm.withParent(map(pgm.parentId).toView)
            } else {
              acc :+ pgm.toView            
            }
          }
          .filter(view => view.id == id)
          .headOption
        }
    Await.result(database.run(allPgmsQuery.withPinnedSession), Duration.Inf).getOrElse(ProgrammView(id, "<unknown>", 0, None, 0, 0, 0))
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
  
  def listWettkaempfeViewAsync = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    database.run{
      sql"""      select * from wettkampf order by datum desc""".as[WettkampfView].withPinnedSession
    }
  }
  
  def listWettkaempfeView = {
    Await.result(listWettkaempfeViewAsync, Duration.Inf)
  }

  def listRiegenZuWettkampf(wettkampf: Long) = {
    Await.result(database.run{(
      sql"""
                  SELECT distinct w.riege, count(distinct w.athlet_id), r.durchgang, d.*
                  FROM wertung w
                  left outer join riege r on (r.name = w.riege and r.wettkampf_id = w.wettkampf_id)
                  left outer join disziplin d on (d.id = r.start)
                  where w.riege not null and w.wettkampf_id = $wettkampf
                  group by w.riege
                  union SELECT distinct w.riege2 as riege, count(distinct w.athlet_id), r.durchgang, d.*
                  FROM wertung w
                  left outer join riege r on (r.name = w.riege2 and r.wettkampf_id = w.wettkampf_id)
                  left outer join disziplin d on (d.id = r.start)
                  where w.riege2 not null and w.wettkampf_id = $wettkampf
                  group by w.riege2
                  union SELECT distinct r.name, 0 as cnt, r.durchgang, d.*
                  FROM riege r
                  inner join disziplin d on (d.id = r.start)
                  where
                    r.wettkampf_id = $wettkampf
                    and not exists (select 1 from wertung w where w.riege = r.name or w.riege2 = r.name and r.wettkampf_id = w.wettkampf_id)
       """.as[(String, Int, Option[String], Option[Disziplin])]).withPinnedSession
    }, Duration.Inf)
  }

  def listAthletenZuWettkampf(progids: Seq[Long]) = {
    Await.result(database.run{(
      sql"""      select a.* from athlet a
                  inner join wertung w on (a.id = w.athlet_id)
                  inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                  where wd.programm_id in (#${progids.mkString(",")})
         """.as[AthletView]).withPinnedSession
    }, Duration.Inf)
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

  def createWettkampf(datum: java.sql.Date, titel: String, programmId: Set[Long], auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, uuidOption: Option[String]): Wettkampf = {
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
    if (!heads.forall { h => h.id == heads.head.id }) {
      throw new IllegalArgumentException("Programme nicht aus der selben Gruppe können nicht in einen Wettkampf aufgenommen werden")
    }
    val process = initialCheck
      .headOption
      .flatMap{
        case Some(cid) if(cid > 0) => 
          sqlu""" delete from riege where wettkampf_id=${cid}""" >>
          sqlu""" delete from wertung where wettkampf_id=${cid}""" >>
          sql"""
                  select * from wettkampf
                  where id=$cid
             """.as[Wettkampf].head
        case _ => 
          sqlu"""
                  insert into wettkampf
                  (datum, titel, programm_Id, auszeichnung, auszeichnungendnote, uuid)
                  values (${datum}, ${titel}, ${heads.head.id}, $auszeichnung, $auszeichnungendnote, $uuid)
              """ >>
          sql"""
                  select * from wettkampf
                  where id in (select max(id) from wettkampf)
             """.as[Wettkampf].head
      }
      
    Await.result(database.run(process.transactionally), Duration.Inf)
  }

  def saveWettkampf(id: Long, datum: java.sql.Date, titel: String, programmId: Set[Long], auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, uuidOption: Option[String]): Wettkampf = {
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
      sqlu"""
                  replace into wettkampf
                  (id, datum, titel, programm_Id, auszeichnung, auszeichnungendnote, uuid)
                  values ($id, $datum, $titel, ${heads.head.id}, $auszeichnung, $auszeichnungendnote, $uuid)
          """ >>
          sql"""
                  select * from wettkampf
                  where id = $id
         """.as[Wettkampf].head
    }
    
    
    Await.result(database.run{(process.flatten).transactionally}, Duration.Inf)
  }
  
  def deleteWettkampf(wettkampfid: Long) {
    Await.result(database.run{(
      sqlu"""      delete from riege where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from wertung where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from wettkampf where id=${wettkampfid}""").transactionally
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

  def unassignAthletFromWettkampf(raw: AthletRemovedFromWettkampf): Unit = {
    val athlet = raw.athlet
    Await.result(database.run{
      sqlu"""      delete from wertung
                   where
                     athlet_id = (select id
                       from athlet
                       where name=${athlet.name}
                         and vorname=${athlet.vorname}
                         and geschlecht=${athlet.geschlecht}
                         and verein=${athlet.verein.get.id})
                     and wettkampf_Id = (select id
                       from wettkampf
                       where uuid=${raw.wettkampfUUID})
              """.transactionally
    }, Duration.Inf)
  }

  def unassignAthletFromWettkampf(wertungId: Set[Long]) {
    val wertung = getWertung(wertungId.head)

    Await.result(database.run{
      sqlu"""
                   delete from wertung
                   where id in (#${wertungId.mkString(",")})
              """.transactionally
    }, Duration.Inf)

    val awu = AthletRemovedFromWettkampf(wertung.athlet, wertung.wettkampf.uuid.get)
    WebSocketClient.publish(awu)
  }

  def assignAthletsToWettkampf(wettkampfId: Long, programmIds: Set[Long], withAthlets: Set[Long] = Set.empty) {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val programs = programmIds map (p => readProgramm(p, cache))
    assignAthletsToWettkampfS(wettkampfId, programs, withAthlets)
  }

//
//  def assignAthletsToWettkampfF(wettkampfId: Long, programs: Set[ProgrammView], withAthlets: Set[Long] = Set.empty) {
//    if (withAthlets.nonEmpty) {
//      val athletsFilter = withAthlets.mkString("(", ",", ")")
//      def assignAction(aid: Long, disciplin: Long) = sqlu"""
//                   delete from wertung where
//                   athlet_Id=${aid} and wettkampfdisziplin_Id=${disciplin} and wettkampf_Id=${wettkampfId}
//               """ >>
//           sqlu"""
//                   insert into wertung
//                   (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
//                   values (${aid}, ${disciplin}, ${wettkampfId}, 0, 0, 0)
//              """
//      val dbActions = sql"""
//                   select id from wettkampfdisziplin
//                   where programm_Id in #${programs.map(_.id).mkString("(", ",", ")")}
//           """.as[Long].
////      .flatMap{disciplines =>
//////        disciplines.flatMap{disciplin =>
//////          ???//withAthlets.flatMap(aid => assignAction(aid, disciplin))
//////        }
////      }
//     Await.result(database.run{dbActions}, Duration.Inf)
//    }
//  }
//  
  def assignAthletsToWettkampfS(wettkampfId: Long, programs: Set[ProgrammView], withAthlets: Set[Long] = Set.empty) {
    if (withAthlets.nonEmpty) {
      val athletsFilter = withAthlets.mkString("(", ",", ")")
      val disciplines = Await.result(database.run{(sql"""
                   select id from wettkampfdisziplin
                   where programm_Id in #${programs.map(_.id).mkString("(", ",", ")")}
           """.as[Long]).withPinnedSession}, Duration.Inf)
           
      withAthlets.foreach(aid => disciplines.foreach{case disciplin =>
        Await.result(database.run{(
          sqlu"""
                   delete from wertung where
                   athlet_Id=${aid} and wettkampfdisziplin_Id=${disciplin} and wettkampf_Id=${wettkampfId}
               """ >>
           sqlu"""
                   insert into wertung
                   (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id)
                   values (${aid}, ${disciplin}, ${wettkampfId})
              """).transactionally
        }, Duration.Inf)
      })           

    }
  }

  def moveToProgram(event: AthletMovedInWettkampf): Unit = {
    val wkIDs = Await.result(database.run{sql"""
                   select disziplin_id, id from wettkampfdisziplin
                   where programm_Id = ${event.pgmId}
                """.as[(Long,Long)].withPinnedSession}, Duration.Inf).toMap
    val athlet = event.athlet
    val wertungen = Await.result(database.run{sql"""
                   select wd.id, w.id from wertung w inner join wettkampfdisziplin wd on (w.wettkampfdisziplin_Id = wd.id)
                   where
                     athlet_id = (select id
                       from athlet
                       where name=${athlet.name}
                         and vorname=${athlet.vorname}
                         and geschlecht=${athlet.geschlecht}
                         and verein=${athlet.verein.get.id})
                     and wettkampf_Id = (select id
                       from wettkampf
                       where uuid=${event.wettkampfUUID})
              """.as[(Long,Long)].transactionally
    }, Duration.Inf)
      .map(t => (t._2, wkIDs(t._1), getWertung(t._2)))
      .map(t => {
        val wkDiszView = readWettkampfDisziplinView(wkIDs(t._3.wettkampfdisziplin.disziplin.id))
        (t._1, t._2, t._3.copy(wettkampfdisziplin = wkDiszView))
      })
      .map(t => (t._1, t._2, generateRiegenName(t._3)))

    Await.result(database.run{
      DBIO.sequence(for(w <- wertungen) yield {
        sqlu"""    UPDATE wertung
                   SET riege=${w._3}
                     , wettkampfdisziplin_Id=${w._2}
                   WHERE id=${w._1}
          """
      }).transactionally
    }, Duration.Inf)
  }

  def moveToProgram(wId: Long, pgmId: Long, athelteView: AthletView) {
    val wkIDs = Await.result(database.run{sql"""
                   select disziplin_id, id from wettkampfdisziplin
                   where programm_Id = ${pgmId}
                """.as[(Long,Long)].withPinnedSession}, Duration.Inf).toMap

    val updates = selectWertungen(athletId = Some(athelteView.id), wettkampfId = Some(wId))
      .map(w => {
        val wkDiszView = readWettkampfDisziplinView(wkIDs(w.wettkampfdisziplin.disziplin.id))
        w.copy(wettkampfdisziplin = wkDiszView)
      })
      .map(w => (w.id, w.wettkampfdisziplin.id,generateRiegenName(w)))

    val wettkampf = Await.result(database.run{(
      DBIO.sequence(for(w <- updates) yield {
        sqlu"""    UPDATE wertung
                   SET riege=${w._3}
                     , wettkampfdisziplin_Id=${w._2}
                   WHERE id=${w._1}
          """}) >>
        sql"""      select * from wettkampf where id=$wId""".as[Wettkampf].head
      ).transactionally
    }, Duration.Inf)

    val awu = AthletMovedInWettkampf(athelteView, wettkampf.uuid.get, pgmId)
    WebSocketClient.publish(awu)
  }
//  def moveToProgram(wId: Long, pgmId: Long, athelteView: AthletView) {
//    val wkIDs = Await.result(database.run{(sql"""
//                   select id from wettkampfdisziplin
//                   where programm_Id = ${pgmId}
//                """.as[Long]).withPinnedSession}, Duration.Inf)
//
//    val wettkampf = Await.result(database.run{(
//      sqlu"""
//                   delete from wertung where
//                   athlet_Id=${athelteView.id} and wettkampf_Id=${wId}
//        """ >>
//      DBIO.sequence(for {wkid <- wkIDs} yield {
//        sqlu"""
//                   insert into wertung
//                   (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
//                   values (${athelteView.id}, ${wkid}, ${wId}, 0, 0, 0)
//          """
//      }) >>
//        sql"""      select * from wettkampf where id=$wId""".as[Wettkampf].head
//      ).transactionally
//    }, Duration.Inf)
//
//    val awu = AthletMovedInWettkampf(athelteView, wettkampf.uuid.get, pgmId)
//    WebSocketClient.publish(awu)
//
//    val wertungen = selectWertungen(athletId = Some(athelteView.id), wettkampfId = Some(wId))
//    Await.result(database.run{
//      DBIO.sequence(for(w <- wertungen) yield {
//        sqlu"""    UPDATE wertung
//                   SET riege=${generateRiegenName(w)}
//                   WHERE id=${w.id}
//          """
//      }).transactionally
//    }, Duration.Inf)
//  }
}