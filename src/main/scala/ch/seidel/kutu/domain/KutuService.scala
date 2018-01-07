package ch.seidel.kutu.domain

import java.text.SimpleDateFormat
import java.text.ParseException
import java.time.LocalDate
import java.time.temporal.TemporalField
import java.time.Period
import java.sql.Date
import java.util.concurrent.TimeUnit
import java.util.Properties
import java.io.File

import scala.io.Source
import scala.annotation.tailrec
import scala.collection.JavaConversions
import scala.concurrent.ExecutionContext.Implicits.global

import slick.jdbc.JdbcBackend.Database
//import slick.jdbc.JdbcBackend.Session
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import slick.jdbc.PositionedResult
import slick.jdbc.GetResult
//import slick.jdbc.SetParameter
//import slick.lifted.Query

import org.sqlite.SQLiteConfig.Pragma
import org.sqlite.SQLiteConfig.DatePrecision

import java.util.Arrays.ArrayList
import java.util.Collections
import java.util.ArrayList
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.collection.JavaConverters
import ch.seidel.kutu.squad._

trait KutuService extends RiegenBuilder {

  lazy val databasemysql = Database.forURL(
    url = "jdbc:mysql://localhost:3306/kutu",
//    url = "jdbc:mysql://localhost:36551/kutu",
    driver = "com.mysql.jdbc.Driver",
    user = "kutu",
    password = "kutu")

  lazy val proplite = {
    val prop = new Properties()
    prop.setProperty("date_string_format", "yyyy-MM-dd")
    prop.setProperty("connectionPool", "disabled")
    prop.setProperty("keepAliveConnection", "true")
    prop
  }
  lazy val homedir = if(new File("./data").exists()) {
    "./data"
  }
  else if(new File(System.getProperty("user.home") + "/kutuapp/data").exists()) {
    System.getProperty("user.home") + "/kutuapp/data"
  }
  else {
    val f = new File(System.getProperty("user.home") + "/kutuapp/data")
    f.mkdirs();
    System.getProperty("user.home") + "/kutuapp/data"
  }
  lazy val dbhomedir = if(new File("./db/kutu.sqlite").exists()) {
    "./db"
  }
  else if(new File(System.getProperty("user.home") + "/kutuapp/db").exists()) {
    System.getProperty("user.home") + "/kutuapp/db"
  }
  else {
    val f = new File(System.getProperty("user.home") + "/kutuapp/db")
    println("try to create for installing the db: " + f);
    f.mkdirs();
    System.getProperty("user.home") + "/kutuapp/db"
  }
  val dbfile = new File(dbhomedir + "/kutu.sqlite")

  lazy val databaselite = Database.forURL(
    url = "jdbc:sqlite:" + dbfile.getAbsolutePath,
    driver = "org.sqlite.JDBC",
    prop = proplite,
    user = "kutu",
    password = "kutu",
    executor = AsyncExecutor("DB-Actions", 30, 10000))

  lazy val database = databaselite
//  lazy val database = databasemysql

  def installDB {
    val sqlScripts = Seq(
         "kutu-sqllite-ddl.sql"
        ,"kutu-sqllite-initialdata.sql"
        )

    sqlScripts.foreach { filename =>
      val file = getClass.getResourceAsStream("/dbscripts/" + filename)
      executeDBScript(Source.fromInputStream(file, "utf-8").getLines())
    }
  }

  def updateDB {
    val sqlScripts = Seq(
         "UpdateGeTuReihenfolge.sql"
        ,"AlterGeschlechtFelder.sql"
        //,"CreateIndicies.sql"
        ,"AlterAlternativeRiegeFelder.sql"
        ,"AlterRiegeVerbandAuszNote.sql"
        ,"UpdateGeTuK7.sql"
        ,"UpdateGeTuK7Ord.sql"
        ,"AddGeTuDamenHerrenKategorie.sql"
        ,"UpdateGeTuK7DamenOhneBarren.sql"
        )

    sqlScripts.filter{ filename =>
      val f = new File(dbhomedir + "/" + filename)
      !f.exists()
    }.foreach { filename =>
      val file = getClass.getResourceAsStream("/dbscripts/" + filename)
      try {
        executeDBScript(Source.fromInputStream(file, "utf-8").getLines())
      }
      catch {
        case e: Exception => e.printStackTrace()
      }
      val quitfile = new File(dbhomedir + "/" + filename)
      quitfile.createNewFile();
    }
  }

  def parseLine(s: String): IndexedSeq[String] = {
    @tailrec
    def cutFields(s: String, acc: IndexedSeq[String]): IndexedSeq[String] = {
      if(s.isEmpty()) {
        acc
      }
      else if(s.startsWith("\"")) {
        val splitter = s.indexOf("\",", 1)
        if(splitter == -1) {
          val splitter2 = s.indexOf("\"", 1)
          if(splitter2 == 0) {
            acc :+ ""
          }
          else {
            acc :+ s.drop(1).take(splitter2-1)
          }
        }
        else if(splitter == 0) {
          cutFields(s.drop(splitter+2), acc :+ "")
        }
        else {
          cutFields(s.drop(splitter+2), acc :+ s.drop(1).take(splitter-1))
        }
      }
      else if(s.startsWith(",")) {
        cutFields(s.drop(1), acc :+ "")
      }
      else {
        val splitter = s.indexOf(",", 1)
        if(splitter == -1) {
          acc :+ s
        }
        else if(splitter == 0){
          cutFields(s.drop(1), acc :+ "")
        }
        else {
          cutFields(s.drop(splitter+1), acc :+ s.take(splitter))
        }
      }
    }
    cutFields(s, IndexedSeq[String]())
  }

  def executeDBScript(script: Iterator[String]) {
    def filterCommentLines(line: String) = {
      !line.trim().startsWith("-- ")
    }
    def combineMultilineStatement(acc: List[String], line: String) = {
      if(line.endsWith(";")) {
        acc.updated(acc.size -1, acc.last + line) :+ ""
      }
      else {
        acc.updated(acc.size -1, acc.last + line)
      }
    }
    def parse(lines: Iterator[String]): List[String] = {
      lines.filter(filterCommentLines).foldLeft(List(""))(combineMultilineStatement).filter(_.trim().length() > 0)
    }
    
    database.run(DBIO.sequence(parse(script).map(statement => sqlu"""#$statement""")))
  }

  if(!dbfile.exists() || dbfile.length() == 0) {
    dbfile.createNewFile()
    installDB
  }
  updateDB

  val sdf = new SimpleDateFormat("dd.MM.yyyy")
  val sdfShort = new SimpleDateFormat("dd.MM.yy")
  val sdfExported = new SimpleDateFormat("yyyy-MM-dd")

  implicit def getSQLDate(date: String) = try {
    new java.sql.Date(sdf.parse(date).getTime)
  }
  catch {
    case d: ParseException => try {
    	new java.sql.Date(sdfExported.parse(date).getTime)
    }
    catch {
      case dd: ParseException =>
        new java.sql.Date(sdfShort.parse(date).getTime)
    }
  }

  private implicit val getVereinResult = GetResult(r => Verein(r.<<[Long], r.<<[String], r.<<))
  private implicit val getVereinOptionResult = GetResult(r =>  r.nextLongOption() match {
    case Some(id) => Some(getVereinResult(r))
    case _        => { r.skip; r.skip; None }
  })
  private implicit val getAthletResult = GetResult(r =>
    Athlet(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
  private implicit val getAthletViewResult = GetResult(r =>
    //id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
    AthletView(
        id = r.<<,
        js_id = r.<<,
        geschlecht = r.<<,
        name = r.<<,
        vorname = r.<<,
        gebdat = r.<<,
        strasse = r.<<,
        plz = r.<<,
        ort = r.<<,
        activ = r.<<,
        verein = r))
  private implicit val getDisziplinResult = GetResult(r =>
    Disziplin(r.<<[Long], r.<<[String]))
  private implicit val getDisziplinOptionResult = GetResult(r => r.nextLongOption() match {
    case Some(id) => Some(Disziplin(id, r.<<[String]))
    case _        => {r.skip; None}
  })
  private implicit val getRiegeRawResult = GetResult(r =>
    RiegeRaw(r.<<, r.<<, r.<<, r.<<))
  private implicit val getRiegeResult = GetResult(r =>
    Riege(r.<<, r.<<, r))
  private implicit val getWettkampfResult = GetResult(r =>
    Wettkampf(r.<<[Long], r.<<[java.sql.Date], r.<<[String], r.<<[Long], r.<<[Int], r.<<))
  private implicit val getWettkampfDisziplinResult = GetResult(r =>
    Wettkampfdisziplin(r.<<[Long], r.<<[Long], r.<<[Long], r.<<[String], r.nextBlobOption(), r.<<, r.<<[Int], r.<<[Int], r.<<[Int]))

  implicit def getWettkampfDisziplinViewResultCached(r: PositionedResult)(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]) = {
    val id = r.<<[Long]
    val pgm = readProgramm(r.<<[Long], cache)
    WettkampfdisziplinView(id, pgm, r, r.<<[String], r.nextBytesOption(), readNotenModus(id, pgm, r.<<), r.<<, r.<<[Int], r.<<[Int])
  }
  private implicit def getResultWertungView(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WertungView(r.<<[Long], r, r, r, r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<, r.<<))
    //WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal, riege: Option[String], riege2: Option[String])
    //WertungView(r.<<[Long], r, r, r, r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<))
  private implicit def getWettkampfViewResultCached(implicit cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long], cache), r.<<[Int], r.<<))
  private implicit def getWettkampfViewResult = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long]), r.<<[Int], r.<<))
  private implicit val getProgrammRawResult = GetResult(r =>
    ProgrammRaw(r.<<[Long], r.<<[String], r.<<[Int], r.<<[Long], r.<<[Int], r.<<[Int], r.<<[Int]))

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

  def updateOrinsertWertung(w: Wertung) = {
    database.run(DBIO.sequence(Seq(
      sqlu"""
                delete from wertung where
                athlet_Id=${w.athletId} and wettkampfdisziplin_Id=${w.wettkampfdisziplinId} and wettkampf_Id=${w.wettkampfId}
        """,
        
      sqlu"""
                insert into wertung
                (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote, riege, riege2)
                values (${w.athletId}, ${w.wettkampfdisziplinId}, ${w.wettkampfId}, ${w.noteD}, ${w.noteE}, ${w.endnote}, ${w.riege}, ${w.riege2})
        """,

      sqlu"""   delete from riege
                WHERE wettkampf_id=${w.id} and not exists (
                  SELECT 1 FROM wertung w
                  WHERE w.wettkampf_id=${w.id}
                    and (w.riege=name or w.riege2=name)
                )
        """
    )))
  }

  def updateWertung(w: Wertung): WertungView = {
    Await.result(database.run(DBIO.sequence(Seq(
      sqlu"""       UPDATE wertung
                    SET note_d=${w.noteD}, note_e=${w.noteE}, endnote=${w.endnote}, riege=${w.riege}, riege2=${w.riege2}
                    WHERE id=${w.id}
          """,

      sqlu"""       DELETE from riege
                    WHERE wettkampf_id=${w.id} and not exists (
                      SELECT 1 FROM wertung w
                      WHERE w.wettkampf_id=${w.id}
                        and (w.riege=name or w.riege2=name)
                    )
          """
    ))), Duration.Inf)

    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      //id |id |js_id |geschlecht |name |vorname |gebdat |strasse |plz |ort |verein |activ |id |name |id |programm_id |id |name |kurzbeschreibung |detailbeschreibung |notenfaktor |ord |masculin |feminim |id |datum |titel |programm_id |auszeichnung |difficulty |execution |endnote |riege |
    val wv = Await.result(database.run(sql"""
                    SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wd.masculin, wd.feminim,
                      wk.*,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    WHERE w.id=${w.id}
                    order by wd.programm_id, wd.ord
       """.as[WertungView].head.withPinnedSession), Duration.Inf)
    if(wv.endnote >= 8.7) {
      putWertungToBestenResults(wv)
    }
    wv    
  }

  def updateWertungSimple(w: Wertung) {
    Await.result(database.run(
      sqlu"""       UPDATE wertung
                    SET note_d=${w.noteD}, note_e=${w.noteE}, endnote=${w.endnote}, riege=${w.riege}, riege2=${w.riege2}
                    WHERE id=${w.id}
          """
    ), Duration.Inf)
  }

  def listAthletenWertungenZuProgramm(progids: Seq[Long], wettkampf: Long, riege: String = "%") = {
    Await.result(database.run{
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      sql"""
                   SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                     wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wd.masculin, wd.feminim,
                     wk.*,
                     w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2
                   FROM wertung w
                   inner join athlet a on (a.id = w.athlet_id)
                   left outer join verein v on (a.verein = v.id)
                   inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                   inner join disziplin d on (d.id = wd.disziplin_id)
                   inner join programm p on (p.id = wd.programm_id)
                   inner join wettkampf wk on (wk.id = w.wettkampf_id)
                   where wd.programm_id in (#${progids.mkString(",")})
                     and w.wettkampf_id = $wettkampf
                     and ($riege = '%' or w.riege = $riege or w.riege2 = $riege)
                   order by wd.programm_id, wd.ord
       """.as[WertungView].withPinnedSession}, Duration.Inf)    
  }

  def listDisziplinesZuDurchgang(durchgang: Set[String], wettkampf: Long, riege1: Boolean): Map[String, IndexedSeq[Disziplin]] = {
    Await.result(database.run{
      val ret = sql""" select distinct wd.disziplin_id, d.name, r.durchgang
             from wettkampfdisziplin wd
             inner join disziplin d on (wd.disziplin_id = d.id)
             inner join wertung w on (w.wettkampfdisziplin_id = wd.id)
             inner join riege r on (r.wettkampf_id = $wettkampf
                                    and r.start = d.id
                                    and r.durchgang in (#${durchgang.mkString("'","','","'")})
                                    and #${if(riege1) "r.name = w.riege" else "r.name = w.riege2"}
                                    )
             where
               w.wettkampf_id = $wettkampf
             order by
              wd.ord
       """.as[(Long, String, String)]
       ret.map{_.map{
         tupel =>
           (Disziplin(tupel._1, tupel._2), tupel._3)
         }.groupBy(_._2).map(x => x._1 -> x._2.map(_._1))
       }
    }, Duration.Inf)
  }

  def listAthletWertungenZuWettkampf(athletId: Long, wettkampf: Long) = {
    Await.result(database.run{
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      sql"""
                   SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                     wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wd.masculin, wd.feminim,
                     wk.*,
                     w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2
                   FROM wertung w
                   inner join athlet a on (a.id = w.athlet_id)
                   left outer join verein v on (a.verein = v.id)
                   inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                   inner join disziplin d on (d.id = wd.disziplin_id)
                   inner join programm p on (p.id = wd.programm_id)
                   inner join wettkampf wk on (wk.id = w.wettkampf_id)
                   where w.athlet_id = $athletId
                     and w.wettkampf_id = $wettkampf
                   order by wd.programm_id, wd.ord
       """.as[WertungView].withPinnedSession
    }, Duration.Inf)
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
      sql"""       select a.* from athlet a
                   inner join wertung w on (a.id = w.athlet_id)
                   inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                   where wd.programm_id in (#${progids.mkString(",")})
         """.as[AthletView]
    }, Duration.Inf)
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
          sqlu"""   delete from riege where wettkampf_id=${cid}""" >>
          sqlu"""   delete from wertung where wettkampf_id=${cid}""" >>
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
      sqlu"""       delete from riege where wettkampf_id=${wettkampfid}""" >>
      sqlu"""       delete from wertung where wettkampf_id=${wettkampfid}""" >>
      sqlu"""       delete from wettkampf where id=${wettkampfid}"""
    }, Duration.Inf)
  }

  def insertVerein(verein: Verein): Verein = {
    val process = for {
      candidateId <- sql"""
              select max(verein.id) as maxid
              from verein
              where LOWER(name)=${verein.name.toLowerCase()}
         """.as[Long].headOption
     } yield {
       candidateId match {
         case Some(id) if(id > 0) =>
           sql"""
                select *
                from verein
                where id=${id}
             """.as[Verein].map{vereine => vereine
               .filter{v => !v.name.equals(verein.name) || !v.verband.equals(verein.verband)}
               .map{savedverein =>
                 sqlu"""
                    update verein
                    set
                      name = ${verein.name}
                    , verband = ${verein.verband}
                    where id=${id}
                     """
               }
             } >> 
             sql"""
                select id from verein where id=${id}
                """.as[Long].head
         case _ =>
           sqlu"""
                insert into verein  (name, verband) values (${verein.name}, ${verein.verband})
               """ >> 
           sql"""
                select id from verein where id in (select max(id) from verein)
              """.as[Long].head
       }
     }
     
     Await.result(database.run{process.flatten.map(Verein(_, verein.name, verein.verband))}, Duration.Inf)
  }

  def createVerein(name: String, verband: Option[String]): Long = {
    Await.result(database.run{
      sqlu"""       insert into verein
                    (name, verband) values (${name}, ${verband})""" >>
      sql"""
                    select id from verein
                    where id in (select max(id) from verein)
         """.as[Long].head
    }, Duration.Inf)
  }

  def updateVerein(verein: Verein) {
     Await.result(database.run{
      sqlu"""       update verein
                    set name = ${verein.name}, verband = ${verein.verband}
                    where id = ${verein.id}
          """
    }, Duration.Inf)
  }
  def deleteVerein(vereinid: Long) {
    Await.result(database.run{
      sqlu"""       delete from wertung where athlet_id in (select id from athlet where verein=${vereinid})""" >>
      sqlu"""       delete from athlet where verein=${vereinid}""" >>
      sqlu"""       delete from verein where id=${vereinid}"""
    }, Duration.Inf)
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
  
  def listDisziplinIdsZuWettkampf(wettkampfId: Long): List[Long] = {
    Await.result(database.run{
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select id from wettkampfdisziplin where programm_Id in #$programme""".as[Long]
    }, Duration.Inf).toList
  }
  
  def listDisziplinesZuWettkampf(wettkampfId: Long, geschlecht: Option[String] = None): List[Disziplin] = {
    Await.result(database.run{
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select distinct wd.disziplin_id, d.name
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id
              #${
                geschlecht match {
                  case Some("M") => "and wd.masculin = 1"
                  case Some("W") => "and wd.feminim = 1"
                  case _ => ""
                }
              }
              and programm_id in #$programme
             order by
              wd.ord
             """.as[Disziplin]      
    }, Duration.Inf).toList
  }
  
  def listWettkampfDisziplines(wettkampfId: Long): List[Wettkampfdisziplin] = {
    Await.result(database.run{
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select wd.id, wd.programm_id, wd.disziplin_id, printf('%s (%s)',d.name, p.name) as kurzbeschreibung, wd.ord
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id and
              programm_Id in #$programme
             order by
              wd.ord
         """.as[(Long, Long, Long, String, Int)]
    }, Duration.Inf)
    .map{t => Wettkampfdisziplin(t._1, t._2, t._3, t._4, None, 0, t._5, 0, 0) }.toList
  }
  
  def completeDisziplinListOfAthletInWettkampf(wettkampf: Wettkampf, athletId: Long) = {
    val wertungen = listAthletWertungenZuWettkampf(athletId, wettkampf.id)
    val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).toSet
    
    val pgmwkds: Map[Long,Vector[Long]] = programme.map{x =>
        x -> Await.result(database.run{sql""" select id from wettkampfdisziplin
                    where programm_Id = ${x}
                      and id not in (select wettkampfdisziplin_Id from wertung
                                     where athlet_Id=${athletId}
                                       and wettkampf_Id=${wettkampf.id})""".as[Long]}, Duration.Inf)
    }.toMap
    val completed = programme.filter{p => wertungen.filter{w => p == w.wettkampfdisziplin.programm.id}.nonEmpty}.
      map{pgmwkds}.flatMap{wkds => wkds.map{wkd =>
        Await.result(database.run{sqlu"""
                  delete from wertung where
                  athlet_Id=${athletId} and wettkampfdisziplin_Id=$wkd and wettkampf_Id=${wettkampf.id}
          """ >>
        sqlu"""
                  insert into wertung
                  (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
                  values (${athletId}, ${wkd}, ${wettkampf.id}, 0, 0, 0)
          """}, Duration.Inf)
        wkd
    }}
    completed    
  }
  
  def listWettkaempfe = {
    sql"""          select * from wettkampf """.as[Wettkampf]
  }
  def listWettkaempfeView = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    Await.result(database.run{
      sql"""          select * from wettkampf """.as[WettkampfView]
    }, Duration.Inf)
  }

  def readWettkampf(id: Long) = {
    Await.result(database.run{
      sql"""          select * from wettkampf where id=$id""".as[Wettkampf].head
    }, Duration.Inf)
  }

  def selectWertungen(vereinId: Option[Long] = None, athletId: Option[Long] = None, wettkampfId: Option[Long] = None, disziplinId: Option[Long] = None): Seq[WertungView] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    val where = "where " + (athletId match {
      case None     => "1=1"
      case Some(id) => s"a.id = $id"
    }) + " and " + (vereinId match {
      case None     => "1=1"
      case Some(id) => s"v.id = $id"
    }) + " and " + (wettkampfId match {
      case None     => "1=1"
      case Some(id) => s"wk.id = $id"
    }) + " and " + (disziplinId match {
      case None     => "1=1"
      case Some(id) => s"d.id = $id"
    })
    Await.result(database.run{
      sql"""
                    SELECT w.id, a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.*,
                      wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wd.masculin, wd.feminim,
                      wk.*,
                      w.note_d as difficulty, w.note_e as execution, w.endnote, w.riege, w.riege2
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    #$where
                    order by wd.programm_id, wd.ord
         """.as[WertungView]
    }, Duration.Inf)
  }

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

  def selectVereine: List[Verein] = {
    Await.result(database.run{
      sql"""        select id, name, verband from verein order by name""".as[Verein]
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
  
  def insertAthlete(athlete: Athlet) = {    
    def getId: Option[Long] = athlete.gebdat match {
      case Some(gebdat) =>
         Await.result(database.run{sql"""
                  select max(athlet.id) as maxid
                  from athlet
                  where name=${athlete.name} and vorname=${athlete.vorname} and strftime('%Y', gebdat)=strftime('%Y',${gebdat}) and verein=${athlete.verein}
         """.as[Long].headOption}, Duration.Inf)
      case _ =>
         Await.result(database.run{sql"""
                  select max(athlet.id) as maxid
                  from athlet
                  where name=${athlete.name} and vorname=${athlete.vorname} and verein=${athlete.verein}
         """.as[Long].headOption}, Duration.Inf)
    }

    if (athlete.id == 0) {
      getId match {
        case Some(id) if(id > 0) =>
          Await.result(database.run{
            sqlu"""
                  replace into athlet
                  (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
            """ >>
            sql"""select * from athlet where id = ${id}""".as[Athlet].head
          }, Duration.Inf)
        case _ =>
          Await.result(database.run{
            sqlu"""
                  replace into athlet
                  (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
            """ >>
            sql"""select * from athlet where id = (select max(athlet.id) from athlet)""".as[Athlet].head
          }, Duration.Inf)
      }
    }
    else {
      Await.result(database.run{
        sqlu"""
                  replace into athlet
                  (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                  values (${athlete.id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
          """
      }, Duration.Inf)
      athlete
    }
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
//        println(" factor " + (namenSimilarity + vorNamenSimilarity) * 2)
        (namenSimilarity + vorNamenSimilarity) * 2
      }
      else if(vereinSimilarity && (preret || (preret2 && jahrgangSimilarity))) {
//        println(" factor " + (namenSimilarity + vorNamenSimilarity))
        namenSimilarity + vorNamenSimilarity
      }
      else {
//        println(" factor 0")
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

  def deleteRiege(wettkampfid: Long, oldname: String) {
    Await.result(database.run{
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
          """
    }, Duration.Inf)
  }

  def renameDurchgang(wettkampfid: Long, oldname: String, newname: String) = {
    Await.result(database.run{
        sqlu"""
                update riege
                set durchgang=${newname.trim}
                where
                wettkampf_id=${wettkampfid} and durchgang=${oldname}
        """
    }, Duration.Inf)
  }

  def renameRiege(wettkampfid: Long, oldname: String, newname: String): Riege = {
    val existing = Await.result(database.run{
        sqlu"""
                DELETE from riege where name=${newname.trim} and wettkampf_id=${wettkampfid}
        """ >>
        sql"""select r.wettkampf_id, r.name, r.durchgang, r.start
             from riege r
             where wettkampf_id=$wettkampfid and name=${oldname}
          """.as[RiegeRaw]
    }, Duration.Inf)
    
    Await.result(database.run{
      val riegeModifierAction = if(existing.isEmpty) {
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
      riegeModifierAction >>
        sqlu"""   UPDATE wertung
                  SET riege=${newname}
                  WHERE wettkampf_id=${wettkampfid} and riege=${oldname}
            """ >>
        sqlu"""   UPDATE wertung
                  SET riege2=${newname}
                  WHERE wettkampf_id=${wettkampfid} and riege2=${oldname}
            """ >>
        sql"""   select r.name as riegenname, r.durchgang, d.*
                 from riege r
                 left outer join disziplin d on (r.start = d.id)
                 where r.wettkampf_id=${wettkampfid} and r.name=${newname}
           """.as[Riege].headOption
    }, Duration.Inf).getOrElse(Riege(newname, None, None))
  }

  def cleanAllRiegenDurchgaenge(wettkampfid: Long) {
    Await.result(database.run{
      sqlu"""
                delete from riege where
                wettkampf_id=${wettkampfid}
        """ >>
      sqlu"""   UPDATE wertung
                SET riege=NULL
                  , riege2=NULL
                WHERE wettkampf_id=${wettkampfid}
          """
    }, Duration.Inf)
  }

  def updateOrinsertRiege(riege: RiegeRaw): Riege = {
    Await.result(database.run{
      sqlu"""
                delete from riege where
                wettkampf_id=${riege.wettkampfId} and name=${riege.r}
        """ >>
      sqlu"""
                insert into riege
                (wettkampf_Id, name, durchgang, start)
                values (${riege.wettkampfId}, ${riege.r}, ${riege.durchgang}, ${riege.start})
        """ >>
       sql"""select r.name as riegenname, r.durchgang, d.*
             from riege r
             left outer join disziplin d on (r.start = d.id)
             where r.wettkampf_id=${riege.wettkampfId} and r.name=${riege.r}
          """.as[Riege]
    }, Duration.Inf).head
  }

  def insertRiegenWertungen(riege: RiegeRaw, wertungen: Seq[Wertung]) {
    Await.result(database.run{
      sqlu"""
                  replace into riege
                  (wettkampf_Id, name, durchgang, start)
                  values (${riege.wettkampfId}, ${riege.r}, ${riege.durchgang}, ${riege.start})
          """ >>
      DBIO.sequence(for(w <- wertungen) yield {
        sqlu"""     UPDATE wertung
                    SET riege=${riege.r}
                    WHERE id=${w.id}
          """
      })
    }, Duration.Inf)
  }

  def selectRiegenRaw(wettkampfId: Long) = {
    Await.result(database.run{
       sql"""select r.wettkampf_id, r.name, r.durchgang, r.start
             from riege r
             where wettkampf_id=$wettkampfId
          """.as[RiegeRaw]
    }, Duration.Inf).toList
  }
  def selectRiegen(wettkampfId: Long) = {
    Await.result(database.run{
       sql"""select r.name as riegenname, r.durchgang, d.*
             from riege r
             left outer join disziplin d on (r.start = d.id)
             where wettkampf_id=$wettkampfId
          """.as[Riege]
    }, Duration.Inf).toList
  }

  def moveToProgram(wId: Long, pgmId: Long, aId: Long) {
    Await.result(database.run{
      sqlu"""
                delete from wertung where
                athlet_Id=${aId} and wettkampf_Id=${wId}
        """ >>
      DBIO.seq((for {
        wkid <- sql"""
                  select id from wettkampfdisziplin
                  where programm_Id = ${pgmId}
                """.as[Long].head
      } yield {
        sqlu"""
                  insert into wertung
                  (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
                  values (${aId}, ${wkid}, ${wId}, 0, 0, 0)
          """
      }).flatten)
    }, Duration.Inf)
    
    val wertungen = selectWertungen(athletId = Some(aId), wettkampfId = Some(wId))
    Await.result(database.run{
      DBIO.sequence(for(w <- wertungen) yield {
        sqlu"""     UPDATE wertung
                    SET riege=${generateRiegenName(w)}
                    WHERE id=${w.id}
          """
      })
    }, Duration.Inf)
  }
  
  private var bestenResults = Map[String,WertungView]()
  private var shouldResetBestenResults = false
  
  def putWertungToBestenResults(wertung: WertungView) {
    if(shouldResetBestenResults) {
      bestenResults = Map[String,WertungView]()
      shouldResetBestenResults = false;
    }
    bestenResults = bestenResults.updated(wertung.athlet.id + ":" + wertung.wettkampfdisziplin.id, wertung)
  }
  
  def getBestenResults = {
    bestenResults
/* Athlet, Disziplin, Wertung (Endnote)
    .map(w =>(w._2.athlet.easyprint, w._2.wettkampfdisziplin.disziplin.name, w._2.endnote))    
    .sortBy(_._3)
 */
    .map(_._2)    
    .toList
  }
  
  def resetBestenResults {
    shouldResetBestenResults = true;
  }
}