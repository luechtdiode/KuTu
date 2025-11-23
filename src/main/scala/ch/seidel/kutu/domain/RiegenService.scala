package ch.seidel.kutu.domain

import org.slf4j.LoggerFactory
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait RiegenService extends DBService with RiegenResultMapper {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def renameRiege(wettkampfid: Long, oldname: String, newname: String): Riege = {
    val existing = Await.result(database.run{
        (if newname.trim == oldname then {
          sql"""select r.wettkampf_id, r.name, r.durchgang, r.start, r.kind
             from riege r
             where wettkampf_id=$wettkampfid and name=${oldname}
          """.as[RiegeRaw]
        } else {
          sqlu"""
                DELETE from riege where name=${newname.trim} and wettkampf_id=${wettkampfid}
                """  >>
          sql"""select r.wettkampf_id, r.name, r.durchgang, r.start, r.kind
             from riege r
             where wettkampf_id=$wettkampfid and name=${oldname}
          """.as[RiegeRaw]}).transactionally
    }, Duration.Inf)
    
    Await.result(database.run{
      val riegeModifierAction = if existing.isEmpty then {
        sqlu"""
                insert into riege
                       (name, wettkampf_id)
                VALUES (${newname.trim}, ${wettkampfid})
        """
      }
      else {
        sqlu"""
                  update riege
                  set name=${newname.trim}
                  where
                  wettkampf_id=${wettkampfid} and name=${oldname}
          """
      }
      (riegeModifierAction >>
        sqlu"""   UPDATE wertung
                  SET riege=${newname}
                  WHERE wettkampf_id=${wettkampfid} and riege=${oldname}
            """ >>
        sqlu"""   UPDATE wertung
                  SET riege2=${newname}
                  WHERE wettkampf_id=${wettkampfid} and riege2=${oldname}
            """ >>
        sql"""   select r.name as riegenname, r.durchgang, d.*, r.kind
                 from riege r
                 left outer join disziplin d on (r.start = d.id)
                 where r.wettkampf_id=${wettkampfid} and r.name=${newname}
           """.as[Riege].headOption).transactionally
    }, Duration.Inf).getOrElse(Riege(newname, None, None, 0))
  }

  def cleanAllRiegenDurchgaenge(wettkampfid: Long): Unit = {
    Await.result(database.run{(
      sqlu"""
                delete from durchgang where
                wettkampf_id=${wettkampfid}
        """ >>
      sqlu"""
                delete from durchgangstation where
                wettkampf_id=${wettkampfid}
        """ >>
      sqlu"""
                delete from riege where
                wettkampf_id=${wettkampfid}
        """ >>
      sqlu"""   UPDATE wertung
                SET riege=NULL
                  , riege2=NULL
                WHERE wettkampf_id=${wettkampfid}
          """).transactionally
    }, Duration.Inf)
  }

  def updateOrinsertRiegen(riegen: Iterable[RiegeRaw]): Unit = {
    val riegenList: List[(Long, Iterable[RiegeRaw])] = riegen.groupBy(_.wettkampfId).toList
    def insertRiegen(rs: Iterable[RiegeRaw]): DBIOAction[Iterable[Int], NoStream, Effect] = DBIO.sequence(for
        riege <- rs
      yield {
        updateOrInsertRiegeRawAction(riege)
      })

    val wettkampfList: List[Long] = riegenList.map(_._1).distinct
    val process = DBIO.sequence(for
      (wettkampfid, riegen) <- riegenList
    yield {
      sqlu"""
                delete from riege where
                wettkampf_id=${wettkampfid}
        """>>
      insertRiegen(riegen)
    }) >> DBIO.sequence((for
      wettkampfId <- wettkampfList
    yield {
      updateDurchgaengeAction(wettkampfId)
    }))

    Await.result(database.run{process.transactionally}, Duration.Inf)
  }

  def updateDurchgaenge(wettkampfId: Long): Unit = {
    Await.result(database.run{ updateDurchgaengeAction(wettkampfId).transactionally}, Duration.Inf)
  }

  def updateDurchgaengeAction(wettkampfId: Long): DBIOAction[Int, NoStream, Effect] =
      sqlu"""
                insert into durchgang (wettkampf_id, title, name, durchgangtype, ordinal, planStartOffset)
                SELECT
                    zp.wettkampf_id,
                    zp.durchgang as title,
                    zp.durchgang as name,
                    1 as durchgangType,
                    coalesce((SELECT max(ordinal)+1 FROM durchgang dd WHERE dd.wettkampf_id = zp.wettkampf_id), 0) as ordinal,
                    0 as planStartOffset
                FROM
                    zeitplan zp
                WHERE
                    zp.wettkampf_id = ${wettkampfId}
                    AND NOT EXISTS (SELECT 1 FROM durchgang dd WHERE dd.wettkampf_id = zp.wettkampf_id and dd.name = zp.durchgang)
        """>>
      sqlu"""
                delete from durchgang
                where wettkampf_id = ${wettkampfId}
                and durchgangType = 1
                and not exists (
                  select 1 from riege r where r.durchgang = durchgang.name
                )
            """

  def updateOrInsertRiegeRawAction(riege: RiegeRaw) =
    sqlu"""
                delete from riege where
                wettkampf_id=${riege.wettkampfId} and name=${riege.r}
        """ >>
    sqlu"""
                delete from riege where
                wettkampf_id=${riege.wettkampfId} and kind=1 and start=${riege.start} and durchgang=${riege.durchgang}
        """ >>
    sqlu"""
                insert into riege
                (wettkampf_Id, name, durchgang, start, kind)
                values (${riege.wettkampfId}, ${riege.r}, ${riege.durchgang}, ${riege.start}, ${riege.kind})
        """

  def updateOrinsertRiege(riege: RiegeRaw): Riege = {
    Await.result(database.run{(
      updateOrInsertRiegeRawAction(riege) >>
      updateDurchgaengeAction(riege.wettkampfId) >>
       sql"""select r.name as riegenname, r.durchgang, d.*, r.kind
             from riege r
             left outer join disziplin d on (r.start = d.id)
             where r.wettkampf_id=${riege.wettkampfId} and r.name=${riege.r}
          """.as[Riege]).transactionally
    }, Duration.Inf).head
  }

  def findAndStoreMatchingRiege(riege: RiegeRaw): RiegeRaw = {
    val existingRiegen = selectRiegenRaw(riege.wettkampfId)
    val riegenParts = riege.r.split(",")
    val scoreSchwellwert = math.pow(riegenParts.length -1d, 10d).intValue
    val scoreSchwellwert2 = math.pow(riegenParts.length -2d, 10d).intValue
    val (matchingRiege, matchscore) = existingRiegen.map { er =>
      (er, er.r.split(",").zip(riegenParts).zipWithIndex.map { case (pair, index) =>
        val (existing, newpart) = pair
        if existing.equalsIgnoreCase(newpart) then (riegenParts.length - index) * 10 else 0
      }.sum / 10)
    }.sortBy(t => t._2).reverse.headOption.getOrElse((riege, 0))

    //println(matchscore, scoreSchwellwert, scoreSchwellwert2, matchingRiege)
    if matchscore >= scoreSchwellwert then {
      matchingRiege
    } else if matchscore > scoreSchwellwert2 then {
      updateOrinsertRiege(riege.copy(durchgang = matchingRiege.durchgang, start = matchingRiege.start))
        .toRaw(riege.wettkampfId)
    } else {
      updateOrinsertRiege(riege).toRaw(riege.wettkampfId)
    }
  }

  def cleanUnusedRiegen(wettkampfid: Long): Unit = {
    Await.result(database.run{(
      sqlu"""
                DELETE from riege where wettkampf_id=${wettkampfid} and kind=0 and not exists(
                  select 1 from wertung w where (w.riege = riege.name or w.riege2 = riege.name) and w.wettkampf_id = riege.wettkampf_id
                )
          """ >>
      sqlu"""
              DELETE from durchgang
              where wettkampf_id=${wettkampfid}
              and durchgangtype=1
              and not exists (
                select 1 from riege r
                where r.durchgang = durchgang.name
                and r.wettkampf_id = durchgang.wettkampf_id
              )
            """ >>
      updateDurchgaengeAction(wettkampfid)).transactionally
    }, Duration.Inf)
  }

  def deleteRiege(wettkampfid: Long, oldname: String): Unit = {
    Await.result(database.run{(
      sqlu"""
                DELETE from riege where name=${oldname.trim} and wettkampf_id=${wettkampfid}
          """ >>
      sqlu"""   UPDATE wertung
                SET riege=null
                WHERE wettkampf_id=${wettkampfid} and riege=${oldname}
          """ >>
      sqlu"""   UPDATE wertung
                SET riege2=null
                WHERE wettkampf_id=${wettkampfid} and riege2=${oldname}
          """ >>
      updateDurchgaengeAction(wettkampfid)).transactionally
    }, Duration.Inf)
  }

  def insertRiegenWertungen(riege: RiegeRaw, wertungen: Seq[Wertung]): Unit = {
    val barrenDisziplinId = Some(5L)
    val riege2List = wertungen.flatMap(w => w.riege2)
      .toSet
      .map{(r2: String) =>
        updateOrInsertRiegeRawAction(
          RiegeRaw(riege.wettkampfId, r2, riege.durchgang, barrenDisziplinId, RiegeRaw.KIND_STANDARD))
      }
      .toList
    Await.result(database.run{(
      updateOrInsertRiegeRawAction(riege) >>
      DBIO.sequence(riege2List) >>
      DBIO.sequence(for w <- wertungen yield {
        w.riege2 match {
          case Some(riege2) =>
            sqlu"""     UPDATE wertung
                    SET riege=${riege.r}
                      , riege2=${riege2}
                    WHERE id=${w.id}
          """
          case _ =>
            sqlu"""     UPDATE wertung
                    SET riege=${riege.r}
                    WHERE id=${w.id}
          """
        }
      })).transactionally
    }, Duration.Inf)
  }

  def selectRiegenRaw(wettkampfId: Long) = {
    Await.result(database.run{(
       sql"""select r.wettkampf_id, r.name, r.durchgang, r.start, r.kind
             from riege r
             where wettkampf_id=$wettkampfId
          """.as[RiegeRaw]).withPinnedSession
    }, Duration.Inf).toList
  }

  def selectRiegen(wettkampfId: Long) = {
    Await.result(database.run{(
       sql"""select r.name as riegenname, r.durchgang, d.*, r.kind
             from riege r
             left outer join disziplin d on (r.start = d.id)
             where wettkampf_id=$wettkampfId
          """.as[Riege]).withPinnedSession
    }, Duration.Inf).toList
  }
  
}