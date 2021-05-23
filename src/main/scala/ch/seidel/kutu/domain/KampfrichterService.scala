package ch.seidel.kutu.domain

import org.slf4j.LoggerFactory
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait WertungsrichterService extends DBService with WertungsrichterResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def createOrUpdateWertungsrichter(wertungsrichter: Wertungsrichter): Wertungsrichter = {
    def getId: Option[Long] = wertungsrichter.id match {
      case 0 => wertungsrichter.gebdat match {
        case Some(gebdat) =>
          Await.result(database.run{sql"""
                  select max(Wertungsrichter.id) as maxid
                  from Wertungsrichter
                  where name=${wertungsrichter.name} and vorname=${wertungsrichter.vorname} and gebdat=${gebdat} and verein=${wertungsrichter.verein}
         """.as[Long].headOption}, Duration.Inf)
        case _ =>
          Await.result(database.run{sql"""
                  select max(Wertungsrichter.id) as maxid
                  from Wertungsrichter
                  where name=${wertungsrichter.name} and vorname=${wertungsrichter.vorname} and verein=${wertungsrichter.verein}
         """.as[Long].headOption}, Duration.Inf)
      }
      case id =>Await.result(database.run{sql"""
                  select max(Wertungsrichter.id) as maxid
                  from Wertungsrichter
                  where id=${id}
         """.as[Long].headOption}, Duration.Inf)
    }

    getId match {
      case Some(id) if(id > 0) =>
        Await.result(database.run{
          sqlu"""     delete from Wertungsrichter where id = ${id}""" >>
          sqlu"""
                insert into Wertungsrichter
                (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                values (${id}, ${wertungsrichter.js_id}, ${wertungsrichter.geschlecht}, ${wertungsrichter.name}, ${wertungsrichter.vorname},
                               ${wertungsrichter.gebdat}, ${wertungsrichter.strasse}, ${wertungsrichter.plz}, ${wertungsrichter.ort}, ${wertungsrichter.verein},
                               ${wertungsrichter.activ})
          """ >>
          sql"""select * from Wertungsrichter where id = ${id}""".as[Wertungsrichter].head
        }, Duration.Inf)
      case _ =>
        Await.result(database.run{
          sqlu"""     delete from Wertungsrichter where id = ${wertungsrichter.id}""" >>
          sqlu"""
                insert into Wertungsrichter
                (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                values (${wertungsrichter.js_id}, ${wertungsrichter.geschlecht}, ${wertungsrichter.name}, ${wertungsrichter.vorname}, ${wertungsrichter.gebdat}, ${wertungsrichter.strasse}, ${wertungsrichter.plz}, ${wertungsrichter.ort}, ${wertungsrichter.verein}, ${wertungsrichter.activ})
          """ >>
          sql"""select * from Wertungsrichter where id = (select max(Wertungsrichter.id) from Wertungsrichter)""".as[Wertungsrichter].head
        }, Duration.Inf)
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

  def deleteKamprichter(kamprichterid: Long): Unit = {
    Await.result(database.run{
      sqlu"""       update durchgangstation set d_Wertungsrichter1 = 0 where d_Wertungsrichter1 =${kamprichterid}""" >>
      sqlu"""       update durchgangstation set e_Wertungsrichter1 = 0 where e_Wertungsrichter1 =${kamprichterid}""" >>
      sqlu"""       update durchgangstation set d_Wertungsrichter2 = 0 where d_Wertungsrichter2 =${kamprichterid}""" >>
      sqlu"""       update durchgangstation set e_Wertungsrichter2 = 0 where e_Wertungsrichter2 =${kamprichterid}""" >>
      sqlu"""       delete from kamprichter where id=${kamprichterid}"""
    }, Duration.Inf)
  }
}