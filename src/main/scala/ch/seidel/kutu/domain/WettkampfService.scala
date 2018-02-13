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
import ch.seidel.kutu.squad.RiegenBuilder

trait WettkampfService extends DBService 
  with WettkampfResultMapper 
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
      val skalamap = Await.result(database.run(skala), Duration.Inf).toMap
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
    Await.result(database.run(allPgmsQuery), Duration.Inf)
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
          .head
        }
    Await.result(database.run(allPgmsQuery), Duration.Inf)
  }
  
  def readProgramm(id: Long, cache: scala.collection.mutable.Map[Long, ProgrammView]): ProgrammView = {
    cache.getOrElseUpdate(id, readProgramm(id))
  }

  def listWettkaempfe = {
     sql"""       select * from wettkampf """.as[Wettkampf]
  }
  def listWettkaempfeAsync = {
     database.run{ listWettkaempfe }
  }
  
  def listWettkaempfeViewAsync = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    database.run{
      sql"""      select * from wettkampf """.as[WettkampfView]
    }
  }
  
  def listWettkaempfeView = {
    Await.result(listWettkaempfeViewAsync, Duration.Inf)
  }

  def listRiegenZuWettkampf(wettkampf: Long) = {
    Await.result(database.run{
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
       """.as[(String, Int, Option[String], Option[Disziplin])]
    }, Duration.Inf)
  }

  def listAthletenZuWettkampf(progids: Seq[Long]) = {
    Await.result(database.run{
      sql"""      select a.* from athlet a
                  inner join wertung w on (a.id = w.athlet_id)
                  inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                  where wd.programm_id in (#${progids.mkString(",")})
         """.as[AthletView]
    }, Duration.Inf)
  }
  
  def readWettkampfAsync(id: Long) = {
    database.run{
      sql"""      select * from wettkampf where id=$id""".as[Wettkampf].head
    }
  }
  
  def readWettkampf(id: Long) = {
    Await.result(readWettkampfAsync(id), Duration.Inf)
  }
  
  def readWettkampfLeafs(programmid: Long) = {
    def children(pid: Long) = Await.result(database.run{
        sql"""    select * from programm
                  where parent_id=$pid
           """.as[ProgrammRaw]
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

  def createWettkampf(datum: java.sql.Date, titel: String, programmId: Set[Long], auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal): Wettkampf = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val programs = programmId map (p => readProgramm(p, cache))
    val heads = programs map (_.head)
    if (!heads.forall { h => h.id == heads.head.id }) {
      throw new IllegalArgumentException("Programme nicht aus der selben Gruppe können nicht in einen Wettkampf aufgenommen werden")
    }
    val process = 
      sql"""
                  select max(id) as maxid
                  from wettkampf
                  where LOWER(titel)=${titel.toLowerCase()} and programm_id = ${heads.head.id} and datum=$datum
             """.as[Long]
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
                  (datum, titel, programm_Id, auszeichnung, auszeichnungendnote)
                  values (${datum}, ${titel}, ${heads.head.id}, $auszeichnung, $auszeichnungendnote)
              """ >>
          sql"""
                  select * from wettkampf
                  where id in (select max(id) from wettkampf)
             """.as[Wettkampf].head
      }
      
    Await.result(database.run(process), Duration.Inf)
  }

  def saveWettkampf(id: Long, datum: java.sql.Date, titel: String, programmId: Set[Long], auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal): Wettkampf = {
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
                  (id, datum, titel, programm_Id, auszeichnung, auszeichnungendnote)
                  values ($id, $datum, $titel, ${heads.head.id}, $auszeichnung, $auszeichnungendnote)
          """ >>
          sql"""
                  select * from wettkampf
                  where id = $id
         """.as[Wettkampf].head
    }
    
    
    Await.result(database.run{process.flatten}, Duration.Inf)
  }
  
  def deleteWettkampf(wettkampfid: Long) {
    Await.result(database.run{
      sqlu"""      delete from riege where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from wertung where wettkampf_id=${wettkampfid}""" >>
      sqlu"""      delete from wettkampf where id=${wettkampfid}"""
    }, Duration.Inf)
  }
  
  def completeDisziplinListOfAthletInWettkampf(wettkampf: Wettkampf, athletId: Long) = {
    val wertungen = listAthletWertungenZuWettkampf(athletId, wettkampf.id)
    val wpgms = wertungen.map(w => w.wettkampfdisziplin.programm.id)
    val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).filter(id => wpgms.contains(id)).toSet
    
    val pgmwkds: Map[Long,Vector[Long]] = programme.map{x =>
        x -> Await.result(database.run{sql""" select id from wettkampfdisziplin
                    where programm_Id = ${x}
                      and id not in (select wettkampfdisziplin_Id from wertung
                                     where athlet_Id=${athletId}
                                       and wettkampf_Id=${wettkampf.id})""".as[Long]}, Duration.Inf)
    }.toMap
    val completed = programme.
      map{pgmwkds}.flatMap{wkds => wkds.map{wkd =>
        Await.result(database.run{
        sqlu"""
                  insert into wertung
                  (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
                  values (${athletId}, ${wkd}, ${wettkampf.id}, 0, 0, 0)
          """}, Duration.Inf)
        wkd
    }}
    completed    
  }

  def unassignAthletFromWettkampf(wertungId: Set[Long]) {
    Await.result(database.run{
      sqlu"""
                   delete from wertung
                   where id in (#${wertungId.mkString(",")})
              """
    }, Duration.Inf)
  }

  def assignAthletsToWettkampf(wettkampfId: Long, programmIds: Set[Long], withAthlets: Set[Long] = Set.empty) {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val programs = programmIds map (p => readProgramm(p, cache))
    assignAthletsToWettkampfS(wettkampfId, programs, withAthlets)
  }


  def assignAthletsToWettkampfS(wettkampfId: Long, programs: Set[ProgrammView], withAthlets: Set[Long] = Set.empty) {
    if (withAthlets.nonEmpty) {
      val athletsFilter = withAthlets.mkString("(", ",", ")")
      val disciplines = Await.result(database.run{sql"""
                   select id from wettkampfdisziplin
                   where programm_Id in #${programs.map(_.id).mkString("(", ",", ")")}
           """.as[Long]}, Duration.Inf)
           
      withAthlets.foreach(aid => disciplines.foreach{case disciplin =>
        Await.result(database.run{
          sqlu"""
                   delete from wertung where
                   athlet_Id=${aid} and wettkampfdisziplin_Id=${disciplin} and wettkampf_Id=${wettkampfId}
               """ >>
           sqlu"""
                   insert into wertung
                   (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
                   values (${aid}, ${disciplin}, ${wettkampfId}, 0, 0, 0)
              """    
        }, Duration.Inf)
      })           

    }
  }
  
  def moveToProgram(wId: Long, pgmId: Long, aId: Long) {
    val wkIDs = Await.result(database.run{sql"""
                   select id from wettkampfdisziplin
                   where programm_Id = ${pgmId}
                """.as[Long]}, Duration.Inf)
                
    Await.result(database.run{
      sqlu"""
                   delete from wertung where
                   athlet_Id=${aId} and wettkampf_Id=${wId}
        """ >>
      DBIO.sequence(for {wkid <- wkIDs} yield {
        sqlu"""
                   insert into wertung
                   (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
                   values (${aId}, ${wkid}, ${wId}, 0, 0, 0)
          """
      })
    }, Duration.Inf)
    
    val wertungen = selectWertungen(athletId = Some(aId), wettkampfId = Some(wId))
    Await.result(database.run{
      DBIO.sequence(for(w <- wertungen) yield {
        sqlu"""    UPDATE wertung
                   SET riege=${generateRiegenName(w)}
                   WHERE id=${w.id}
          """
      })
    }, Duration.Inf)
  }  
}