package ch.seidel.kutu.domain

import ch.seidel.kutu.actors.{AthletIndexActor, RemoveAthlet, SaveAthlet}
import ch.seidel.kutu.data.{CaseObjectMetaUtil, Surname}
import org.slf4j.LoggerFactory
import slick.jdbc.SQLiteProfile.api._

import java.sql.Date
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.CollectionHasAsScala

trait AthletService extends DBService with AthletResultMapper with VereinService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def selectAthletesAction = {
    sql"""          select * from athlet""".as[Athlet]
  }

  def selectAthletes: List[Athlet] = {
    Await.result(database.run {
      sql"""        select * from athlet
                    order by activ desc, name, vorname asc
       """.as[Athlet].withPinnedSession
    }, Duration.Inf).toList
  }

  def selectAthletesOfVerein(id: Long): List[Athlet] = {
    Await.result(database.run {
      sql"""        select * from athlet
                    where verein=${id}
                    order by activ desc, name, vorname asc
       """.as[Athlet].withPinnedSession
    }, Duration.Inf).toList
  }

  /**
    * id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
    */
  def selectAthletesView: List[AthletView] = {
    Await.result(database.run {
      sql"""        select a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein,
                            v.* from athlet a inner join verein v on (v.id = a.verein) 
                     order by a.activ desc, a.name, a.vorname asc
          """.as[AthletView]
        .withPinnedSession
    }, Duration.Inf).toList
  }

  /**
    * id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
    */
  def selectAthletesView(verein: Verein): List[AthletView] = {
    Await.result(database.run {
      sql"""        select a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein,
                            v.* from athlet a inner join verein v on (v.id = a.verein)
                    where v.id = ${verein.id} or (v.name = ${verein.name} and v.verband = ${verein.verband})
                    order by a.activ desc, a.name, a.vorname asc
          """.as[AthletView]
        .withPinnedSession
    }, Duration.Inf).toList
  }

  def loadAthleteView(athletId: Long): AthletView = {
    Await.result(database.run {
      sql"""        select a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein,
                            v.* from athlet a inner join verein v on (v.id = a.verein) 
                     where a.id = $athletId
                     order by a.activ desc, a.name, a.vorname asc
          """.as[AthletView].head
        .withPinnedSession
    }, Duration.Inf)
  }

  def publishChanged(athlet: Athlet) = AthletIndexActor.publish(SaveAthlet(athlet))
  def publishRemoved(athlet: Athlet) = AthletIndexActor.publish(RemoveAthlet(athlet))

  def mergeAthletes(idToDelete: Long, idToKeep: Long): Unit = {
    val toDelete = loadAthlet(idToDelete)
    Await.result(database.run {
      (
        sqlu"""       update wertung
                    set athlet_id=${idToKeep}
                    where athlet_id=${idToDelete}
          """ >>
        sqlu"""       update athletregistration
                    set athlet_id=${idToKeep}
                    where athlet_id=${idToDelete}
          """ >>
        sqlu"""
                    delete from athlet where id=${idToDelete}
          """).transactionally
    }, Duration.Inf)
    toDelete.foreach(publishRemoved)
  }

  def deleteAthlet(id: Long): Unit = {
    val toDelete = loadAthlet(id)
    Await.result(database.run {
      (
        sqlu"""       delete from wertung
                    where athlet_id=${id}
          """ >>
        sqlu"""
                    delete from athlet where id=${id}
          """).transactionally
    }, Duration.Inf)
    toDelete.foreach(publishRemoved)
  }

  def insertAthletes(athletes: Iterable[(String, Athlet)]): Iterable[(String, Athlet)] = {
    val process = athletes.map(a => insertAthlete2(a))
    Await.result(database.run {
      DBIO.sequence(process).transactionally
    }, Duration.Inf)
  }

  private def insertAthlete2(importAthlet: (String, Athlet)) = {
    val (csvid, athlete) = importAthlet

    def getId = athlete.id match {
      case 0 => athlete.gebdat match {
        case Some(gebdat) =>
          sql"""
                  select max(athlet.id) as maxid
                  from athlet
                  where name=${athlete.name} and vorname=${athlete.vorname} and gebdat=${gebdat} and verein=${athlete.verein}
         """.as[Long].headOption
        case _ =>
          sql"""
                  select max(athlet.id) as maxid
                  from athlet
                  where name=${athlete.name} and vorname=${athlete.vorname} and verein=${athlete.verein}
         """.as[Long].headOption
      }

      case id =>
        sql"""
                  select max(athlet.id) as maxid
                  from athlet
                  where id=${id}
         """.as[Long].headOption
    }


    getId.flatMap {
      case Some(athletId) if (athletId > 0) =>
        sqlu"""
                update athlet
                set js_id=${athlete.js_id},
                    geschlecht = ${athlete.geschlecht},
                    name = ${athlete.name},
                    vorname = ${athlete.vorname},
                    gebdat = ${athlete.gebdat},
                    strasse = ${athlete.strasse},
                    plz = ${athlete.plz},
                    ort = ${athlete.ort},
                    verein = ${athlete.verein},
                    activ = ${athlete.activ}
                where id=${athletId}
          """ >>
          sql"""select * from athlet where id = ${athletId}""".as[Athlet].head

      case _ =>
        sqlu"""
                insert into athlet
                (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                values (${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
          """ >>
          sql"""select * from athlet where id = (select max(athlet.id) from athlet)""".as[Athlet].head
    }
    .map { a: Athlet =>
      publishChanged(a)
      (csvid, a)
    }
  }

  def insertAthlete(athlete: Athlet): Athlet = {
    val process = Seq(("", athlete)).map(a => insertAthlete2(a))
    Await.result(database.run {
      DBIO.sequence(process).transactionally
    }, Duration.Inf).map(x => x._2).head

    // TODO send message to replicat the new fact
    //    val awu = AthletUpdated(wv.athlet, wv.toWertung, wv.wettkampf.uuid.get, "", wv.wettkampfdisziplin.disziplin.id, wv.wettkampfdisziplin.programm.easyprint)
    //    WebSocketClient.publish(awu)
  }

  def startsSameInPercent(text1: String, text2: String) = 100 * text1.zip(text2).foldLeft((true, 0)) { (acc, pair) =>
    val same = pair._1 == pair._2
    (acc._1 && same, acc._2 + (if (acc._1 && same) 1 else 0))
  }._2 / math.max(text1.length, text2.length)

  def findAthleteLike(cache: java.util.Collection[MatchCode] = new java.util.ArrayList[MatchCode], wettkampf: Option[Long] = None, exclusive: Boolean)(athlet: Athlet): Athlet = {
    val bmname = MatchCode.encode(athlet.name)
    val bmvorname = MatchCode.encode(athlet.vorname)

    def similarAthletFactor(code: MatchCode) = {
      val maxthresholdCharCount = 8

      def calcThreshold(text1: String, text2: String) =
        80 - 60 / maxthresholdCharCount * math.min(maxthresholdCharCount, math.max(0, maxthresholdCharCount - math.max(text1.length, text2.length)))

      val encodedNamen = code.encodedNamen
      val namenSimilarity = MatchCode.similarFactor(code.name, athlet.name, calcThreshold(athlet.name, code.name)) + encodedNamen.find(bmname.contains(_)).map(_ => 100).getOrElse(
        bmname.find(encodedNamen.contains(_)).map(_ => 100).getOrElse(0)
      )
      val encodedVorNamen = code.encodedVorNamen
      val vorNamenSimilarity = MatchCode.similarFactor(code.vorname, athlet.vorname, calcThreshold(athlet.vorname, code.vorname)) + startsSameInPercent(code.vorname, athlet.vorname) + encodedVorNamen.find(bmvorname.contains(_)).map(_ => 100).getOrElse(
        bmvorname.find(encodedVorNamen.contains(_)).map(_ => 100).getOrElse(0)
      )
      val gebdatSimilarity = athlet.gebdat.isEmpty || code.gebdat.equals(athlet.gebdat)
      val jahrgangSimilarity = athlet.gebdat.isEmpty || code.jahrgang.equals(AthletJahrgang(athlet.gebdat).jahrgang)
      val preret = namenSimilarity > 140 && vorNamenSimilarity > 140
      val preret2 = namenSimilarity > 50 && vorNamenSimilarity > 25 && (namenSimilarity + vorNamenSimilarity) > 200 && (math.max(namenSimilarity, vorNamenSimilarity) > 140)
      val vereinSimilarity = athlet.verein match {
        case Some(vid) => vid == code.verein
        case _ => false
      }
      //      if (code.name.equals(athlet.name)) {
      //      print(athlet.easyprint, this)
      //      }
      if (vereinSimilarity && preret && gebdatSimilarity) {
        (namenSimilarity + vorNamenSimilarity) * 3
      }
      else if (vereinSimilarity && preret && jahrgangSimilarity) {
        (namenSimilarity + vorNamenSimilarity) * 2
      }
      else if (vereinSimilarity && (preret || (preret2 && gebdatSimilarity))) {
        namenSimilarity + vorNamenSimilarity
      }
      else {
        0
      }
    }

    val preselect = if (cache.isEmpty) {
      Await.result(database.run {
        (wettkampf match {
          case None => sql"""
             select id, name, vorname, gebdat, verein
             from athlet
           """
          case Some(wkid) => sql"""
             select id, name, vorname, gebdat, verein
             from athlet a
             where exists (select w.id from wertung w where w.wettkampf_id = $wkid and w.athlet_id = a.id)
           """
        }).as[(Long, String, String, Option[Date], Long)].withPinnedSession
      }, Duration.Inf).
        flatMap { x =>
          val (id, name, vorname, gebdat, verein) = x
          val mc1 = MatchCode(id, name, vorname, gebdat, verein)
          if (Surname.isSurname(mc1.name).isDefined) {
            List(mc1, mc1.swappednames)
          } else {
            List(mc1)
          }

        }.foreach {
        cache.add
      }
      cache
    }
    else {
      cache
    }
    val presel2 = preselect.asScala.filter(mc => !exclusive || mc.id != athlet.id).map { matchcode =>
      (matchcode.id, similarAthletFactor(matchcode))
    }.filter(p => p._2 > 0).toList.sortBy(_._2).reverse
    presel2.headOption.flatMap(k => loadAthlet(k._1)).getOrElse {
      if (!athlet.equals(Athlet())) {
        logger.warn("Athlet local not found! " + athlet.extendedprint)
      }
      athlet
    }
  }

  def loadAthlet(key: Long): Option[Athlet] = {
    Await.result(database.run {
      sql"""select * from athlet where id=${key}""".as[Athlet]
        .headOption
        .withPinnedSession
    }, Duration.Inf)
  }

  def findDuplicates(): List[(AthletView, AthletView, AthletView)] = {
    val likeFinder = findAthleteLike(cache = new java.util.ArrayList[MatchCode], exclusive = true) _
    for {
      athleteView <- selectAthletesView
      athlete = athleteView.toAthlet
      like = likeFinder(athlete)
      if athleteView.id != like.id
    } yield {
      val tupel = List(athleteView, loadAthleteView(like.id)).sortWith { (a, b) =>
        if (a.gebdat.map(_.toLocalDate.getDayOfMonth).getOrElse(0) > b.gebdat.map(_.toLocalDate.getDayOfMonth).getOrElse(0)) true
        else {
          val asp = Athlet.mapSexPrediction(a.toAthlet)
          val bsp = Athlet.mapSexPrediction(b.toAthlet)
          if (asp == a.geschlecht && bsp != b.geschlecht) true
          else if (bsp == b.geschlecht && asp != a.geschlecht) false
          else if (a.id - b.id > 0) true
          else false
        }
      }
      (tupel(0), tupel(1), CaseObjectMetaUtil.mergeMissingProperties(tupel(0), tupel(1)))
    }
  }

  def markAthletesInactiveOlderThan(nYears: Int): Int = {
    val d = LocalDate.now().minus(nYears, ChronoUnit.YEARS)
    logger.info(s"searching for athletes not active since last $nYears years (last event before $d) ...")
    try {
      val inactivList = Await.result(database.run {
        sql"""
        select a.id, a.name, a.vorname, max(wk.datum) from athlet a
              inner join verein v on v.id = a.verein
              inner join wertung w on a.id = w.athlet_id
              inner join wettkampf wk on w.wettkampf_id = wk.id
        where a.activ = true
        group by a.id
        having max(wk.datum) < ${Date.valueOf(d)}
       """.as[(Int, String, String, Date)]
      }, Duration.Inf).toList
      if (inactivList.length > 0) {
        logger.info("setting the following list of athlets inactiv:")
        logger.info(inactivList.mkString("(", "\n", ")"))
        val length = Await.result(database.run {
          sqlu"""
          update athlet
          set activ = false
          where activ = true
            and (id in (#${inactivList.filter(_._4.compareTo(d) < 0).map(_._1).mkString(",")})
                 or not exists (select 1 from wertung w where w.athlet_id = athlet.id)
                )
         """
        }, Duration.Inf)
        logger.info(s"inactivated athletes: $length")
        length
      } else {
        0
      }
    } catch {
      case e: Exception => e.printStackTrace()
        0
    }
  }

  def cleanUnusedClubs(): Set[Verein] = {
    val affectedClubs = Await.result(database.run {
      sql"""
          select * from verein
          where not exists (
            select distinct 1 from athlet a
                inner join wertung w on (a.id = w.athlet_id)
                where verein.id = a.verein
          )
          and not exists (
            select distinct 1 from vereinregistration vr
			      inner join wettkampf wk on (vr.wettkampf_id = wk.id)
            where verein.id = verein_id
			        and wk.datum >= current_date
          )          """.as[Verein]
    }, Duration.Inf).toSet
    for(verein <- affectedClubs) {
      deleteVerein(verein.id)
    }
    affectedClubs
  }
  def addMissingWettkampfMetaData(): Unit = {
    Await.result(database.run {
      sqlu"""
          insert into wettkampfmetadata
          (uuid, wettkampf_id)
          select uuid, id from wettkampf wk where wk.uuid <> ''
          on conflict(wettkampf_id) do nothing;
          """
    }, Duration.Inf)
  }
}