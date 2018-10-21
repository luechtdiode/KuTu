package ch.seidel.kutu.domain

import org.slf4j.LoggerFactory
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait WertungsrichterService extends DBService with WertungsrichterResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def createOrUpdateWertungsrichter(Wertungsrichter: Wertungsrichter): Wertungsrichter = {
    def getId: Option[Long] = Wertungsrichter.gebdat match {
      case Some(gebdat) =>
         Await.result(database.run{sql"""
                  select max(Wertungsrichter.id) as maxid
                  from Wertungsrichter
                  where name=${Wertungsrichter.name} and vorname=${Wertungsrichter.vorname} and strftime('%Y', gebdat)=strftime('%Y',${gebdat}) and verein=${Wertungsrichter.verein}
         """.as[Long].headOption}, Duration.Inf)
      case _ =>
         Await.result(database.run{sql"""
                  select max(Wertungsrichter.id) as maxid
                  from Wertungsrichter
                  where name=${Wertungsrichter.name} and vorname=${Wertungsrichter.vorname} and verein=${Wertungsrichter.verein}
         """.as[Long].headOption}, Duration.Inf)
    }

    if (Wertungsrichter.id == 0) {
      getId match {
        case Some(id) if(id > 0) =>
          Await.result(database.run{
            sqlu"""
                  replace into Wertungsrichter
                  (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${id}, ${Wertungsrichter.js_id}, ${Wertungsrichter.geschlecht}, ${Wertungsrichter.name}, ${Wertungsrichter.vorname}, 
                                 ${Wertungsrichter.gebdat}, ${Wertungsrichter.strasse}, ${Wertungsrichter.plz}, ${Wertungsrichter.ort}, ${Wertungsrichter.verein}, 
                                 ${Wertungsrichter.activ})
            """ >>
            sql"""select * from Wertungsrichter where id = ${id}""".as[Wertungsrichter].head
          }, Duration.Inf)
        case _ =>
          Await.result(database.run{
            sqlu"""
                  replace into Wertungsrichter
                  (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${Wertungsrichter.js_id}, ${Wertungsrichter.geschlecht}, ${Wertungsrichter.name}, ${Wertungsrichter.vorname}, ${Wertungsrichter.gebdat}, ${Wertungsrichter.strasse}, ${Wertungsrichter.plz}, ${Wertungsrichter.ort}, ${Wertungsrichter.verein}, ${Wertungsrichter.activ})
            """ >>
            sql"""select * from Wertungsrichter where id = (select max(Wertungsrichter.id) from Wertungsrichter)""".as[Wertungsrichter].head
          }, Duration.Inf)
      }
    }
    else {
      Await.result(database.run{
        sqlu"""
                  replace into Wertungsrichter
                  (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${Wertungsrichter.id}, ${Wertungsrichter.js_id}, ${Wertungsrichter.geschlecht}, ${Wertungsrichter.name}, ${Wertungsrichter.vorname}, ${Wertungsrichter.gebdat}, ${Wertungsrichter.strasse}, ${Wertungsrichter.plz}, ${Wertungsrichter.ort}, ${Wertungsrichter.verein}, ${Wertungsrichter.activ})
          """
      }, Duration.Inf)
      Wertungsrichter
    }
  }
  
  def selectWertungsrichter = {
    sql"""          select * from Wertungsrichter""".as[Wertungsrichter]
  }
  
  def selectWertungsrichterOfVerein(id: Long) = {
    Await.result(database.run{
      sql"""        select * from Wertungsrichter
                    where verein=${id}
                    order by activ desc, name, vorname asc
       """.as[Wertungsrichter]
    }, Duration.Inf).toList
  }
  
  /**
   * id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
   */
  def selectWertungsrichterView = {
    Await.result(database.run{
      sql"""        select a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.* from Wertungsrichter a inner join verein v on (v.id = a.verein) order by activ desc, name, vorname asc """.as[WertungsrichterView]
    }, Duration.Inf).toList
  }

  def deleteKamprichter(kamprichterid: Long) {
    Await.result(database.run{
      sqlu"""       update durchgangstation set d_Wertungsrichter1 = 0 where d_Wertungsrichter1 =${kamprichterid}""" >>
      sqlu"""       update durchgangstation set e_Wertungsrichter1 = 0 where e_Wertungsrichter1 =${kamprichterid}""" >>
      sqlu"""       update durchgangstation set d_Wertungsrichter2 = 0 where d_Wertungsrichter2 =${kamprichterid}""" >>
      sqlu"""       update durchgangstation set e_Wertungsrichter2 = 0 where e_Wertungsrichter2 =${kamprichterid}""" >>
      sqlu"""       delete from kamprichter where id=${kamprichterid}"""
    }, Duration.Inf)
  }
}