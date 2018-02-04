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

trait KampfrichterService extends DBService with KampfrichterResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def createOrUpdateKampfrichter(kampfrichter: Kampfrichter): Kampfrichter = {
    def getId: Option[Long] = kampfrichter.gebdat match {
      case Some(gebdat) =>
         Await.result(database.run{sql"""
                  select max(kampfrichter.id) as maxid
                  from kampfrichter
                  where name=${kampfrichter.name} and vorname=${kampfrichter.vorname} and strftime('%Y', gebdat)=strftime('%Y',${gebdat}) and verein=${kampfrichter.verein}
         """.as[Long].headOption}, Duration.Inf)
      case _ =>
         Await.result(database.run{sql"""
                  select max(kampfrichter.id) as maxid
                  from kampfrichter
                  where name=${kampfrichter.name} and vorname=${kampfrichter.vorname} and verein=${kampfrichter.verein}
         """.as[Long].headOption}, Duration.Inf)
    }

    if (kampfrichter.id == 0) {
      getId match {
        case Some(id) if(id > 0) =>
          Await.result(database.run{
            sqlu"""
                  replace into kampfrichter
                  (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${id}, ${kampfrichter.js_id}, ${kampfrichter.geschlecht}, ${kampfrichter.name}, ${kampfrichter.vorname}, 
                                 ${kampfrichter.gebdat}, ${kampfrichter.strasse}, ${kampfrichter.plz}, ${kampfrichter.ort}, ${kampfrichter.verein}, 
                                 ${kampfrichter.activ})
            """ >>
            sql"""select * from kampfrichter where id = ${id}""".as[Kampfrichter].head
          }, Duration.Inf)
        case _ =>
          Await.result(database.run{
            sqlu"""
                  replace into kampfrichter
                  (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${kampfrichter.js_id}, ${kampfrichter.geschlecht}, ${kampfrichter.name}, ${kampfrichter.vorname}, ${kampfrichter.gebdat}, ${kampfrichter.strasse}, ${kampfrichter.plz}, ${kampfrichter.ort}, ${kampfrichter.verein}, ${kampfrichter.activ})
            """ >>
            sql"""select * from kampfrichter where id = (select max(kampfrichter.id) from kampfrichter)""".as[Kampfrichter].head
          }, Duration.Inf)
      }
    }
    else {
      Await.result(database.run{
        sqlu"""
                  replace into kampfrichter
                  (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${kampfrichter.id}, ${kampfrichter.js_id}, ${kampfrichter.geschlecht}, ${kampfrichter.name}, ${kampfrichter.vorname}, ${kampfrichter.gebdat}, ${kampfrichter.strasse}, ${kampfrichter.plz}, ${kampfrichter.ort}, ${kampfrichter.verein}, ${kampfrichter.activ})
          """
      }, Duration.Inf)
      kampfrichter
    }
  }
  
  def selectKampfrichter = {
    sql"""          select * from kampfrichter""".as[Kampfrichter]
  }
  
  def selectKampfrichterOfVerein(id: Long) = {
    Await.result(database.run{
      sql"""        select * from kampfrichter
                    where verein=${id}
                    order by activ desc, name, vorname asc
       """.as[Kampfrichter]
    }, Duration.Inf).toList
  }
  
  /**
   * id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
   */
  def selectKampfrichterView = {
    Await.result(database.run{
      sql"""        select a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.* from kampfrichter a inner join verein v on (v.id = a.verein) order by activ desc, name, vorname asc """.as[KampfrichterView]
    }, Duration.Inf).toList
  }

  def deleteKamprichter(kamprichterid: Long) {
    Await.result(database.run{
      sqlu"""       update durchgangstation set d_kampfrichter1 = 0 where d_kampfrichter1 =${kamprichterid}""" >>
      sqlu"""       update durchgangstation set e_kampfrichter1 = 0 where e_kampfrichter1 =${kamprichterid}""" >>
      sqlu"""       update durchgangstation set d_kampfrichter2 = 0 where d_kampfrichter2 =${kamprichterid}""" >>
      sqlu"""       update durchgangstation set e_kampfrichter2 = 0 where e_kampfrichter2 =${kamprichterid}""" >>
      sqlu"""       delete from kamprichter where id=${kamprichterid}"""
    }, Duration.Inf)
  }
}