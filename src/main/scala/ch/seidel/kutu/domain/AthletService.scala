package ch.seidel.kutu.domain

import java.sql.Date
import java.time.{LocalDate, Period}

import org.slf4j.LoggerFactory
import slick.jdbc.SQLiteProfile.api._

import scala.collection.JavaConverters
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait AthletService extends DBService with AthletResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def selectAthletes = {
    sql"""          select * from athlet""".as[Athlet]
  }
  
  def selectAthletesOfVerein(id: Long) = {
    Await.result(database.run{(
      sql"""        select * from athlet
                    where verein=${id}
                    order by activ desc, name, vorname asc
       """.as[Athlet]).withPinnedSession
    }, Duration.Inf).toList
  }
  
  /**
   * id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
   */
  def selectAthletesView = {
    Await.result(database.run{
      (sql"""        select a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, 
                            v.* from athlet a inner join verein v on (v.id = a.verein) 
                     order by activ desc, name, vorname asc 
          """.as[AthletView]
      ).withPinnedSession
    }, Duration.Inf).toList
  }
  
  def loadAthleteView(athletId: Long) = {
    Await.result(database.run{
      (sql"""        select a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, 
                            v.* from athlet a inner join verein v on (v.id = a.verein) 
                     where a.id = $athletId 
                     order by activ desc, name, vorname asc
          """.as[AthletView].head
      ).withPinnedSession
    }, Duration.Inf)
  }

  def deleteAthlet(id: Long) {
    Await.result(database.run{(
      sqlu"""       delete from wertung
                    where athlet_id=${id}
          """ >>
      sqlu"""
                    delete from athlet where id=${id}
          """).transactionally
    }, Duration.Inf)
  }
  
  def insertAthletes(athletes: Iterable[(String,Athlet)]): Iterable[(String,Athlet)] = {
    val process = athletes.map(a => insertAthlete2(a))
    Await.result(database.run{
      DBIO.sequence(process).transactionally
    }, Duration.Inf)
  }
  
  private def insertAthlete2(importAthlet: (String,Athlet)) = {
    val (id, athlete) = importAthlet
    def getId = athlete.gebdat match {
      case Some(gebdat) =>
         sql"""
                  select max(athlet.id) as maxid
                  from athlet
                  where name=${athlete.name} and vorname=${athlete.vorname} and strftime('%Y', gebdat)=strftime('%Y',${gebdat}) and verein=${athlete.verein}
         """.as[Long].headOption
      case _ =>
         sql"""
                  select max(athlet.id) as maxid
                  from athlet
                  where name=${athlete.name} and vorname=${athlete.vorname} and verein=${athlete.verein}
         """.as[Long].headOption
    }
    (if (athlete.id == 0) {
      getId.flatMap {
        case Some(athletId) if (athletId > 0) =>
          sqlu"""
                  replace into athlet
                  (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${athletId}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
            """ >>
            sql"""select * from athlet where id = ${athletId}""".as[Athlet].head

        case _ =>
          sqlu"""
                  replace into athlet
                  (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
            """ >>
            sql"""select * from athlet where id = (select max(athlet.id) from athlet)""".as[Athlet].head
      }
    } else {
        sqlu"""
                  replace into athlet
                  (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${athlete.id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
          """.map(_ => athlete)
    })
    .map(a => (id, a))
  }

  def insertAthlete(athlete: Athlet): Athlet = {   
    val process = Seq(("", athlete)).map(a => insertAthlete2(a))
    Await.result(database.run{
      DBIO.sequence(process).transactionally
    }, Duration.Inf).map(x => x._2).head
  }

  def findAthleteLike(cache: java.util.List[MatchCode] = new java.util.ArrayList[MatchCode])(athlet: Athlet) = {
    val bmname = MatchCode.encode(athlet.name)
    val bmvorname = MatchCode.encode(athlet.vorname)
    def similarAthletFactor(code: MatchCode) = {
      val encodedNamen = code.encodedNamen
      val namenSimilarity = MatchCode.similarFactor(code.name, athlet.name) + (100 * encodedNamen.filter(bmname.contains(_)).toList.size / encodedNamen.size)
      val encodedVorNamen = code.encodedVorNamen
      val vorNamenSimilarity = MatchCode.similarFactor(code.vorname, athlet.vorname) + (100 * encodedVorNamen.filter(bmvorname.contains(_)).toList.size / encodedVorNamen.size)
      val jahrgangSimilarity = code.jahrgang.equals(AthletJahrgang(athlet.gebdat).jahrgang)
      val preret = namenSimilarity > 140 && vorNamenSimilarity > 140
      val preret2 = (namenSimilarity + vorNamenSimilarity) > 220 && (math.max(namenSimilarity, vorNamenSimilarity) > 140)
      val vereinSimilarity = athlet.verein match {
        case Some(vid) => vid == code.verein
        case _ => true
      }
//      if (code.name.equals(athlet.name)) {
//      print(athlet.easyprint, this)
//      }
      if(vereinSimilarity && preret && jahrgangSimilarity) {
//        logger.debug(" factor " + (namenSimilarity + vorNamenSimilarity) * 2)
        (namenSimilarity + vorNamenSimilarity) * 2
      }
      else if(vereinSimilarity && (preret || (preret2 && jahrgangSimilarity))) {
//        logger.debug(" factor " + (namenSimilarity + vorNamenSimilarity))
        namenSimilarity + vorNamenSimilarity
      }
      else {
//        logger.debug(" factor 0")
        0
      }
    }
    
    val preselect = if(cache.isEmpty()) {
      Await.result(database.run{sql"""
         select id, name, vorname, gebdat, verein
         from athlet
         """.as[(Long, String, String, Option[Date], Long)].withPinnedSession
      }, Duration.Inf).
      map{x =>
        val (id, name, vorname, jahr, verein) = x
        MatchCode(id, name, vorname, AthletJahrgang(jahr).jahrgang, verein)
      }.foreach{ cache.add }
      cache
    }
    else {
      cache
    }
    val presel2 = JavaConverters.asScalaBuffer(preselect).map{matchcode =>
      (matchcode.id, similarAthletFactor(matchcode))
    }.filter(_._2 > 0).toList.sortBy(_._2).reverse
    presel2.headOption.flatMap(k =>
      Await.result(database.run{(sql"""select * from athlet where id=${k._1}""".as[Athlet].
      headOption).withPinnedSession}, Duration.Inf)
    ).getOrElse(athlet)
  }

  def altersfilter(pgm: ProgrammView, a: Athlet): Boolean = {
    val alter = a.gebdat match {
      case Some(d) => Period.between(d.toLocalDate, LocalDate.now).getYears
      case None    => 7
    }
    pgm.alterVon <= alter && pgm.alterBis >= alter
  }

  def altersfilter(pgm: ProgrammView, a: AthletView): Boolean = {
    val alter = a.gebdat match {
      case Some(d) => Period.between(d.toLocalDate, LocalDate.now).getYears
      case None    => 7
    }
    pgm.alterVon <= alter && pgm.alterBis >= alter
  }
}