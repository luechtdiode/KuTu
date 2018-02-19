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
import java.time.Period
import java.time.LocalDate

trait AthletService extends DBService with AthletResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def selectAthletes = {
    sql"""          select * from athlet""".as[Athlet]
  }
  
  def selectAthletesOfVerein(id: Long) = {
    Await.result(database.run{
      sql"""        select * from athlet
                    where verein=${id}
                    order by activ desc, name, vorname asc
       """.as[Athlet]
    }, Duration.Inf).toList
  }
  
  /**
   * id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
   */
  def selectAthletesView = {
    Await.result(database.run{
      sql"""        select a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.* from athlet a inner join verein v on (v.id = a.verein) order by activ desc, name, vorname asc """.as[AthletView]
    }, Duration.Inf).toList
  }
  


  def deleteAthlet(id: Long) {
    Await.result(database.run{
      sqlu"""       delete from wertung
                    where athlet_id=${id}
          """ >>
      sqlu"""
                    delete from athlet where id=${id}
          """
    }, Duration.Inf)
  }
  
  def insertAthletes(athletes: Iterable[(String,Athlet)]): Iterable[(String,Athlet)] = {
    val process = athletes.map(a => insertAthlete2(a))
    Await.result(database.run{
      DBIO.sequence(process)
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
      getId.flatMap(id => id match {
        case Some(id) if(id > 0) =>
            sqlu"""
                  replace into athlet
                  (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
            """ >>
            sql"""select * from athlet where id = ${id}""".as[Athlet].head
          
        case _ =>
            sqlu"""
                  replace into athlet
                  (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
            """ >>
            sql"""select * from athlet where id = (select max(athlet.id) from athlet)""".as[Athlet].head
      })
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
      DBIO.sequence(process)
    }, Duration.Inf).map(x => x._2).head
  }

  def findAthleteLike(cache: java.util.List[MatchCode] = java.util.Collections.emptyList[MatchCode])(athlet: Athlet) = {
    val bmname = MatchCode.encode(athlet.name)
    val bmvorname = MatchCode.encode(athlet.vorname)
    def similarAthletFactor(code: MatchCode) = {
//      print(athlet.easyprint, name, vorname, jahrgang)
      val encodedNamen = code.encodedNamen
      val namenSimilarity = MatchCode.similarFactor(code.name, athlet.name) + (100 * encodedNamen.filter(bmname.contains(_)).toList.size / encodedNamen.size)
      val encodedVorNamen = code.encodedVorNamen
      val vorNamenSimilarity = MatchCode.similarFactor(code.vorname, athlet.vorname) + (100 * encodedVorNamen.filter(bmvorname.contains(_)).toList.size / encodedVorNamen.size)
      val jahrgangSimilarity = code.jahrgang.equals(AthletJahrgang(athlet.gebdat).hg)
      val preret = namenSimilarity > 140 && vorNamenSimilarity > 140
      val preret2 = (namenSimilarity + vorNamenSimilarity) > 220 && (math.max(namenSimilarity, vorNamenSimilarity) > 140)
      val vereinSimilarity = athlet.verein match {
        case Some(vid) => vid == code.verein
        case _ => true
      }
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
         """.as[(Long, String, String, Option[Date], Long)]
      }, Duration.Inf).
      map{x =>
        val (id, name, vorname, jahr, verein) = x
        MatchCode(id, name, vorname, AthletJahrgang(jahr).hg, verein)
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
      Await.result(database.run{sql"""select * from athlet where id=${k._1}""".as[Athlet].
      headOption}, Duration.Inf)
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