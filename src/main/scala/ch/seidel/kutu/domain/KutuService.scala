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
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcBackend.Session
import slick.jdbc.StaticQuery._
import slick.jdbc.{GetResult, StaticQuery => Q}
import slick.jdbc.PositionedResult
import slick.lifted.Query
import slick.jdbc.StaticQuery
import slick.jdbc.SetParameter
import org.sqlite.SQLiteConfig.Pragma
import org.sqlite.SQLiteConfig.DatePrecision
import java.util.Arrays.ArrayList
import java.util.Collections
import java.util.ArrayList
import scala.collection.JavaConversions

trait KutuService {

  lazy val databasemysql = Database.forURL(
    url = "jdbc:mysql://localhost:3306/kutu",
//    url = "jdbc:mysql://localhost:36551/kutu",
    driver = "com.mysql.jdbc.Driver",
    user = "kutu",
    password = "kutu")

  lazy val proplite = {
    val prop = new Properties()
    prop.setProperty("date_string_format", "yyyy-MM-dd")
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
    password = "kutu")

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
    database withSession { implicit session =>
      def execStatement(statement: String) {
        println(statement); StaticQuery.updateNA(statement).execute
      }
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
      parse(script).foreach(execStatement)
    }
  }

  if(!dbfile.exists() || dbfile.length() == 0) {
    dbfile.createNewFile()
    installDB
  }
  updateDB

  val sdf = new SimpleDateFormat("dd.MM.yyyy")
  val sdfShort = new SimpleDateFormat("dd.MM.yy")
  val sdfExported = new SimpleDateFormat("yyyy-MM-dd")

  def addFilter[P](query: StaticQuery[P,_], predicate: String, param: Option[P]) = {
    param map {
      case Some(p) => query + predicate + p.toString()
      case _ => query
    }
  }

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

  implicit def getWettkampfDisziplinViewResultCached(r: PositionedResult)(implicit session: Session, cache: scala.collection.mutable.Map[Long, ProgrammView]) = {
    val id = r.<<[Long]
    val pgm = readProgramm(r.<<[Long], cache)
    WettkampfdisziplinView(id, pgm, r, r.<<[String], r.nextBytesOption(), readNotenModus(id, pgm, r.<<), r.<<, r.<<[Int], r.<<[Int])
  }
  private implicit def getResultWertungView(implicit session: Session, cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WertungView(r.<<[Long], r, r, r, r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<, r.<<))
    //WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal, riege: Option[String], riege2: Option[String])
    //WertungView(r.<<[Long], r, r, r, r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<))
  private implicit def getWettkampfViewResultCached(implicit session: Session, cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long], cache), r.<<[Int], r.<<))
  private implicit def getWettkampfViewResult(implicit session: Session) = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long]), r.<<[Int], r.<<))
  private implicit val getProgrammRawResult = GetResult(r =>
    ProgrammRaw(r.<<[Long], r.<<[String], r.<<[Int], r.<<[Long], r.<<[Int], r.<<[Int], r.<<[Int]))

  def readNotenModus(id: Long, pgm: ProgrammView, notenfaktor: Double)(implicit session: Session): NotenModus = {
    val skala = sql"""
                   select kurzbeschreibung, punktwert from notenskala
                   where wettkampfdisziplin_id=${id}
                   order by punktwert
       """.as[(String,Double)].build
    if(pgm.head.id == 1) {
      Athletiktest(skala.toMap, notenfaktor)
    }
    else if(pgm.head.id == 20) {
      GeTuWettkampf
    }
    else {
      KuTuWettkampf
    }
  }

  def listRootProgramme(): List[ProgrammView] = {
    database withSession { implicit session =>
      sql"""select id from programm where parent_id is null or parent_id = 0""".as[Long].build.foldLeft(List[ProgrammView]()){(acc, pid) =>
        acc :+ readProgramm(pid)
      }
    }
  }

  def readProgramm(id: Long)(implicit session: Session): ProgrammView = {
    readProgramm(id, scala.collection.mutable.Map[Long, ProgrammView]())
  }
  def readProgramm(id: Long, cache: scala.collection.mutable.Map[Long, ProgrammView])(implicit session: Session): ProgrammView = {

    def wk(pid: Long) = sql"""
                    select * from programm
                    where id=$pid
                            """.as[ProgrammRaw].build.head
    @tailrec
    def rp(pgm: ProgrammRaw, pgml: List[ProgrammRaw]): List[ProgrammRaw] = {
      if (pgm.parentId > 0l) {
        val wkv = wk(pgm.parentId)
        rp(wkv, pgm :: pgml)
      }
      else {
        pgm :: pgml
      }
    }
    lazy val path = rp(wk(id), List.empty)

    cache.get(id) match {
      case Some(wk) => wk
      case _ =>
        val rev = path
        val wk = rev.foldLeft(rev.head.toView)((path, p) => if(p.id == path.id) path else p.withParent(path))
        cache.put(id, wk)
        wk
    }
  }

  def readWettkampfLeafs(programmid: Long) = {
    database withSession { implicit session =>

      def children(pid: Long) =
        sql"""      select * from programm
                    where parent_id=$pid
            """.as[ProgrammRaw].build

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
  }

  def updateOrinsertWertung(w: Wertung) = {
    database withTransaction { implicit session =>
      sqlu"""
                delete from wertung where
                athlet_Id=${w.athletId} and wettkampfdisziplin_Id=${w.wettkampfdisziplinId} and wettkampf_Id=${w.wettkampfId}
        """.execute
      sqlu"""
                insert into wertung
                (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote, riege, riege2)
                values (${w.athletId}, ${w.wettkampfdisziplinId}, ${w.wettkampfId}, ${w.noteD}, ${w.noteE}, ${w.endnote}, ${w.riege}, ${w.riege2})
        """.execute

      sqlu"""       DELETE from riege
                    WHERE wettkampf_id=${w.id} and not exists (
                      SELECT 1 FROM wertung w
                      WHERE w.wettkampf_id=${w.id}
                        and (w.riege=name or w.riege2=name)
                    )
          """.execute
    }
  }

  def updateWertung(w: Wertung): WertungView = {
    database withTransaction { implicit session =>
      sqlu"""       UPDATE wertung
                    SET note_d=${w.noteD}, note_e=${w.noteE}, endnote=${w.endnote}, riege=${w.riege}, riege2=${w.riege2}
                    WHERE id=${w.id}
          """.execute

      sqlu"""       DELETE from riege
                    WHERE wettkampf_id=${w.id} and not exists (
                      SELECT 1 FROM wertung w
                      WHERE w.wettkampf_id=${w.id}
                        and (w.riege=name or w.riege2=name)
                    )
          """.execute

      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      //id |id |js_id |geschlecht |name |vorname |gebdat |strasse |plz |ort |verein |activ |id |name |id |programm_id |id |name |kurzbeschreibung |detailbeschreibung |notenfaktor |ord |masculin |feminim |id |datum |titel |programm_id |auszeichnung |difficulty |execution |endnote |riege |
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
                    WHERE w.id=${w.id}
                    order by wd.programm_id, wd.ord
       """.as[WertungView].build.head
    }
  }

  def listAthletenWertungenZuProgramm(progids: Seq[Long], wettkampf: Long, riege: String = "%") = {
    database withSession { implicit session =>
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
       """.as[WertungView].build
    }
  }

  def listAthletenWertungenZuRiege(progids: Seq[Long], wettkampf: Long, riege: String) = {
    database withSession { implicit session =>
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      val ret = sql"""
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
                     and (w.riege = $riege or w.riege2 = $riege)
                     and w.wettkampf_id = $wettkampf
                   order by wd.programm_id, wd.ord
       """.as[WertungView].build
       ret
    }
  }

  def listAthletWertungenZuWettkampf(athletId: Long, wettkampf: Long) = {
    database withSession { implicit session =>
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      val ret = sql"""
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
       """.as[WertungView].build
       ret
    }
  }

  def listRiegenZuWettkampf(wettkampf: Long) = {
    database withSession { implicit session =>
      /*
       *  r.name, r.durchgang, d.*
             from riege r
             left outer join disziplin d on (r.start = d.id)
       */
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
       """.as[(String, Int, Option[String], Option[Disziplin])].build
    }
  }

  def listAthletenZuWettkampf(progids: Seq[Long]) = {
    database withSession { implicit session =>
      sql"""       select a.* from athlet a
                   inner join wertung w on (a.id = w.athlet_id)
                   inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                   where wd.programm_id in (#${progids.mkString(",")})
         """.as[AthletView].build
    }
  }

  def createWettkampf(datum: java.sql.Date, titel: String, programmId: Set[Long], auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, withAthlets: Option[(Long, Athlet) => Boolean] = Some({ (_, _) => true })): Wettkampf = {
    database withTransaction { implicit session =>
      val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      val programs = programmId map (p => readProgramm(p, cache))
      val heads = programs map (_.head)
      if (!heads.forall { h => h.id == heads.head.id }) {
        throw new IllegalArgumentException("Programme nicht aus der selben Gruppe können nicht in einen Wettkampf aufgenommen werden")
      }
      val candidateId = sql"""
                    select max(id) as maxid
                    from wettkampf
                    where LOWER(titel)=${titel.toLowerCase()} and programm_id = ${heads.head.id} and datum=$datum
                           """.as[Long].build.headOption
      val wk = candidateId match {
        case Some(cid) if(cid > 0) =>
          sqlu"""   delete from riege where wettkampf_id=${cid}""".execute
          sqlu"""   delete from wertung where wettkampf_id=${cid}""".execute
          sql"""
                    select * from wettkampf
                    where id=$cid
             """.as[Wettkampf].build.head
        case _ =>
          sqlu"""
                    insert into wettkampf
                    (datum, titel, programm_Id, auszeichnung, auszeichnungendnote)
                    values (${datum}, ${titel}, ${heads.head.id}, $auszeichnung, $auszeichnungendnote)
              """.execute
          sql"""
                    select * from wettkampf
                    where id in (select max(id) from wettkampf)
             """.as[Wettkampf].build.head
      }

      assignAthletsToWettkampfS(wk.id, programs, withAthlets, session)
      wk
    }
  }

  def saveWettkampf(id: Long, datum: java.sql.Date, titel: String, programmId: Set[Long], auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal): Wettkampf = {
    database withTransaction { implicit session =>
      val existing = sql"""
                    select * from wettkampf
                    where id = $id
                        """.as[Wettkampf].build.head
      val hasWertungen = sql"""
                    select count(*) from wertung where wettkampf_id=$id""".as[Int].build.head
      val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      val programs = (programmId + existing.programmId) map (p => readProgramm(p, cache))
      val heads = programs map (_.head)
      if (hasWertungen > 0 && !heads.forall { h => h.id == heads.head.id }) {
        throw new IllegalArgumentException("Es kann keine Programmanpassung gemacht werden, wenn bereits Turner zum Wettkampf verknüpft sind.")
      }
      sqlu"""
                    replace into wettkampf
                    (id, datum, titel, programm_Id, auszeichnung, auszeichnungendnote)
                    values ($id, $datum, $titel, ${heads.head.id}, $auszeichnung, $auszeichnungendnote)
          """.execute
      sql"""
                    select * from wettkampf
                    where id = $id
         """.as[Wettkampf].build.head
    }
  }
  def deleteWettkampf(wettkampfid: Long) {
    database withTransaction { implicit session: Session =>
      sqlu"""       delete from riege where wettkampf_id=${wettkampfid}""".execute
      sqlu"""       delete from wertung where wettkampf_id=${wettkampfid}""".execute
      sqlu"""       delete from wettkampf where id=${wettkampfid}""".execute
    }
  }

  def insertVerein(verein: Verein): Verein = {
    database withTransaction { implicit session: Session =>
      val candidateId = sql"""
                select max(verein.id) as maxid
                from verein
                where LOWER(name)=${verein.name.toLowerCase()}
       """.as[Long].build.headOption
           candidateId match {
             case Some(id) if(id > 0) =>
               val savedverein = sql"""
                    select *
                    from verein
                    where id=${id}
           """.as[Verein].build.head
           if(!savedverein.name.equals(verein.name) || !savedverein.verband.equals(verein.verband)) {
             sqlu"""
                  update verein
                  set
                    name = ${verein.name}
                  , verband = ${verein.verband}
                  where id=${id}
                 """.execute

           }
           Verein(id, verein.name, verein.verband)
         case _ =>
           sqlu"""
                insert into verein  (name, verband) values (${verein.name}, ${verein.verband})
               """.execute
           val id = sql"""
                select id from verein where id in (select max(id) from verein)
              """.as[Long].build.head
           Verein(id, verein.name, verein.verband)
       }
    }
  }

  def createVerein(name: String, verband: Option[String]): Long = {
    database withTransaction { implicit session: Session =>
      sqlu"""       insert into verein
                    (name, verband) values (${name}, ${verband})""".execute
      sql"""
                    select id from verein
                    where id in (select max(id) from verein)
         """.as[Long].build.head
    }
  }

  def updateVerein(verein: Verein) {
    database withTransaction { implicit session: Session =>
      sqlu"""       update verein
                    set name = ${verein.name}, verband = ${verein.verband}
                    where id = ${verein.id}
          """.execute
    }
  }
  def deleteVerein(vereinid: Long) {
    database withTransaction { implicit session: Session =>
      sqlu"""       delete from wertung where athlet_id in (select id from athlet where verein=${vereinid})""".execute
      sqlu"""       delete from athlet where verein=${vereinid}""".execute
      sqlu"""       delete from verein where id=${vereinid}""".execute
    }
  }
  def unassignAthletFromWettkampf(wertungId: Set[Long]) {
    database withTransaction { implicit session: Session =>
      sqlu"""
                    delete from wertung
                    where id in (#${wertungId.mkString(",")})
              """.execute
    }
  }

  def assignAthletsToWettkampf(wettkampfId: Long, programmIds: Set[Long], withAthlets: Option[(Long, Athlet) => Boolean] = Some({ (_, _) => true })) {
    database withTransaction {implicit session: Session =>
      val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      val programs = programmIds map (p => readProgramm(p, cache))
      assignAthletsToWettkampfS(wettkampfId, programs, withAthlets, session)
    }
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

  def assignAthletsToWettkampfS(wettkampfId: Long, programs: Set[ProgrammView], withAthlets: Option[(Long, Athlet) => Boolean] = Some({ (_, _) => true }), sess: Session) {
    implicit val session = sess

    withAthlets match {
      case Some(f) =>
        for {
          pgm <- programs
          a <- selectAthletes.build.filter(altersfilter(pgm, _)).filter{x =>
//              println(x)
              f(pgm.id, x)}
          wkid <- sql"""
                    select id from wettkampfdisziplin
                    where programm_Id = ${pgm.id}
                  """.as[Long]
        } {
          sqlu"""
                    delete from wertung where
                    athlet_Id=${a.id} and wettkampfdisziplin_Id=${wkid} and wettkampf_Id=${wettkampfId}
            """.execute
          sqlu"""
                    insert into wertung
                    (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
                    values (${a.id}, ${wkid}, ${wettkampfId}, 0, 0, 0)
            """.execute
        }
      case None =>
    }
  }
  def listDisziplinIdsZuWettkampf(wettkampfId: Long): List[Long] = {
    database withSession {implicit session: Session =>
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      sql""" select id from wettkampfdisziplin where programm_Id in #$programme""".as[Long].build
    }
  }
  def listDisziplinesZuWettkampf(wettkampfId: Long): List[Disziplin] = {
    database withSession {implicit session: Session =>
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      val list = sql""" select wd.disziplin_id, d.name
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id and
              programm_id in #$programme""".as[Disziplin].iterator
      list.toList
    }
  }
  def listWettkampfDisziplines(wettkampfId: Long): List[Wettkampfdisziplin] = {
    database withSession {implicit session: Session =>
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      val list = sql""" select wd.id, wd.programm_id, wd.disziplin_id, printf('%s (%s)',d.name, p.name) as kurzbeschreibung
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id and
              programm_Id in #$programme""".as[(Long, Long, Long, String)].iterator
      list.map{t => Wettkampfdisziplin(t._1, t._2, t._3, t._4, None, 0, 0, 0, 0) }.toList
    }
  }
  def completeDisziplinListOfAthletInWettkampf(wettkampf: Wettkampf, athletId: Long) = {
    val wertungen = listAthletWertungenZuWettkampf(athletId, wettkampf.id)
    val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).toSet
    database withTransaction {implicit session: Session =>
      val pgmwkds = programme.map{x =>
        x -> sql""" select id from wettkampfdisziplin
                    where programm_Id = ${x}
                      and id not in (select wettkampfdisziplin_Id from wertung
                                     where athlet_Id=${athletId}
                                       and wettkampf_Id=${wettkampf.id})""".as[Long].build
      }.toMap
      val completed = programme.filter{p => wertungen.filter{w => p == w.wettkampfdisziplin.programm.id}.nonEmpty}.
      map{pgmwkds}.flatMap{wkds => wkds.map{wkd =>
          sqlu"""
                    delete from wertung where
                    athlet_Id=${athletId} and wettkampfdisziplin_Id=$wkd and wettkampf_Id=${wettkampf.id}
            """.execute
          sqlu"""
                    insert into wertung
                    (athlet_Id, wettkampfdisziplin_Id, wettkampf_Id, note_d, note_e, endnote)
                    values (${athletId}, ${wkd}, ${wettkampf.id}, 0, 0, 0)
            """.execute
          wkd
      }}
      completed
    }
  }
  def listWettkaempfe = {
    sql"""          select * from wettkampf """.as[Wettkampf]
  }
  def listWettkaempfeView(implicit session: Session) = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    sql"""          select * from wettkampf """.as[WettkampfView].build
  }

  def readWettkampf(id: Long)(implicit session: Session) = {
    sql"""          select * from wettkampf where id=$id""".as[Wettkampf].build.head
  }

  def selectWertungen(vereinId: Option[Long] = None, athletId: Option[Long] = None, wettkampfId: Option[Long] = None, disziplinId: Option[Long] = None): Seq[WertungView] = {
    implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    database withSession {implicit session: Session =>
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
         """.as[WertungView].build
    }
  }

  def selectAthletes = {
    sql"""          select * from athlet""".as[Athlet]
  }

  /**
   * id |js_id |geschlecht |name |vorname   |gebdat |strasse |plz |ort |activ |verein |id |name        |
   */
  def selectAthletesView = {
    database withSession { implicit session =>
      sql"""        select a.id, a.js_id, a.geschlecht, a.name, a.vorname, a.gebdat, a.strasse, a.plz, a.ort, a.activ, a.verein, v.* from athlet a inner join verein v on (v.id = a.verein) order by activ desc, name, vorname asc """.as[AthletView].list
    }
  }

  def selectVereine: List[Verein] = {
    database withSession { implicit session =>
      sql"""        select id, name, verband from verein order by name""".as[Verein].list
    }
  }

  def deleteAthlet(id: Long) {
    database withTransaction { implicit session =>
      sqlu"""       delete from wertung
                    where athlet_id=${id}
          """.execute
      sqlu"""
                    delete from athlet where id=${id}
          """.execute
    }
  }
  def insertAthlete(athlete: Athlet) = {
    database withTransaction { implicit session =>
      def getId = athlete.gebdat match {
        case Some(gebdat) =>
           sql"""
                    select max(athlet.id) as maxid
                    from athlet
                    where name=${athlete.name} and vorname=${athlete.vorname} and strftime('%Y', gebdat)=strftime('%Y',${gebdat}) and verein=${athlete.verein}
           """.as[Long].iterator.toStream.headOption
        case _ =>
           sql"""
                    select max(athlet.id) as maxid
                    from athlet
                    where name=${athlete.name} and vorname=${athlete.vorname} and verein=${athlete.verein}
           """.as[Long].iterator.toStream.headOption
      }

      if (athlete.id == 0) {
        getId match {
          case Some(id) if(id > 0) =>
            sqlu"""
                    replace into athlet
                    (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                    values (${id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
            """.execute
            sql"""select * from athlet where id = ${id}""".as[Athlet].iterator.toStream.head
          case _ => sqlu"""
                    replace into athlet
                    (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                    values (${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
            """.execute
            sql"""select * from athlet where id = (select max(athlet.id) from athlet)""".as[Athlet].iterator.toStream.head
        }
      }
      else {
        sqlu"""
                    replace into athlet
                    (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein, activ)
                    values (${athlete.id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein}, ${athlete.activ})
            """.execute
        athlete
      }
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
      val vereinSimilarity = athlet.verein match {
        case Some(vid) => vid == code.verein
        case _ => true
      }
//      print(f" namenSimilarity: $namenSimilarity vorNamenSimilarity: $vorNamenSimilarity jahrgangSimilarity: $jahrgangSimilarity jahrgang: $jahrgang - ${AthletJahrgang(athlet.gebdat).hg}")
      if(vereinSimilarity && preret && jahrgangSimilarity) {
//        println(" factor " + (namenSimilarity + vorNamenSimilarity) * 2)
        (namenSimilarity + vorNamenSimilarity) * 2
      }
      else if(vereinSimilarity && preret) {
//        println(" factor " + (namenSimilarity + vorNamenSimilarity))
        namenSimilarity + vorNamenSimilarity
      }
      else {
//        println(" factor 0")
        0
      }
    }

    database withTransaction { implicit session =>
      val preselect = if(cache.isEmpty()) {
        sql"""
           select id, name, vorname, gebdat, verein
           from athlet
           """.as[(Long, String, String, Option[Date], Long)].iterator.
        map{x =>
          val (id, name, vorname, jahr, verein) = x
          MatchCode(id, name, vorname, AthletJahrgang(jahr).hg, verein)
        }.foreach{ cache.add }
        cache
      }
      else {
        cache
      }
      val presel2 = JavaConversions.collectionAsScalaIterable(preselect).map{matchcode =>
        (matchcode.id, similarAthletFactor(matchcode))
      }.filter(_._2 > 0).toList.sortBy(_._2).reverse
      presel2.headOption.flatMap(k =>
        sql"""select * from athlet where id=${k._1}""".as[Athlet].
        build.headOption
      ).getOrElse(athlet)
    }
  }

  /* Riegenbuilder:
--     1. Anzahl Rotationen (min = 1, max = Anzahl Teilnehmer),
--     2. Anzahl Stationen (min = 1, max = Anzahl Diszipline im Programm),
--     => ergibt die optimale Anzahl Riegen (Rotationen * Stationen)
--     3. Gruppiert nach Programm oder Jahrgang (Jahrgang im Athletiktest-Modus),
--     4. Gruppiert nach Verein oder Jahrgang (Verein im Athletiktest-Modus)
--     => Verknüpfen der Gruppen auf eine Start-Station/-Rotation
--     => operation suggestRiegen(WettkampfId, Rotationen/Stationen:List<Integer>): Map<Riegennummer,List<WertungId>>

       - Ausgangsgrösse ist die Gesamtteilnehmer-Anzahl und die maximale Gruppengrösse in einer Riege
         z.B. 290 Teilnehmer und max 14 Turner in einer Riege

       - Die Gruppengrösse in einer Riege sollte sich pro Durchgang einheitlich ergeben (+-3 Tu/Ti)

       - Ein Verein darf Rotationen überspannen, innerhalb der Rotation und Kategorie aber nicht
         => Dies, weil sonst vom Verein zu viele Betreuer notwendig würden.

       - Spezialfälle
         - Barren nur für Tu am Ende jedes Durchgangs
         - Parallel geführte Kategorien z.B. (K1 & K2), (K3 & K4)
         - Gemischt geführte Gruppen z.B. K5 - K7

       Somit müsste jede Rotation in sich zunächst stimmen
       => Vorgabe von aussen:
       - Rotation[
         - Parallel geführte Gruppe [
           - Gemischt geführte Kategorie [
             - Parallel, gemischt geführte Geräte
           ]
         ]
       ]
       Beispiel GeTu:
       Rotation 1 (Ti,Tu) [                         Gruppen, Kumuliert
         Sub-Rotation 1 [
           Parallel Gruppe [
             Gemischt Gruppe [
               K1 (Ti,Tu) [
                 Reck, Boden, Schaukelringe, Sprung       4,         4
               ]
             ]
             Gemischt Gruppe [
               K2 (Ti,Tu) [
                 Reck, Boden, Schaukelringe, Sprung       4,         8
               ]
             ]
           ]
         ]
         Sub-Rotation 2 [
           Parallel Gruppe [
             Gemischt Gruppe [                            1,         9
               K1 (Tu) [
                 Barren
               ]
             ]
             Gemischt Gruppe [                            1,        10
               K2 (Tu)  [
                 Barren
               ]
             ]
           ]
         ]
       ]

       Rotation 2 (Ti)[
         Parallel Gruppe [
           Gemischt Gruppe [                            4,        13
             K1 [
               Reck, Boden, Schaukelringe, Sprung
             ]
             K2 [
               Reck, Boden, Schaukelringe, Sprung
             ]
           ]
         ]
         Parallel Gruppe [
           Gemischt Gruppe [                            4,        17
             K3 [
               Reck, Boden, Schaukelringe, Sprung
             ]
             K4 [
               Reck, Boden, Schaukelringe, Sprung
             ]
           ]
         ]
       ]
 * * */
  def suggestRiegen(wettkampfId: Long, rotationstation: Seq[Int]): Seq[(String, Seq[Wertung])] = {

    val riegencnt = rotationstation.reduce(_+_)
    val cache = scala.collection.mutable.Map[String, Int]()
    val wertungen = selectWertungen(wettkampfId = Some(wettkampfId)).groupBy(w => w.athlet)
    if(wertungen.isEmpty) {
      Seq[(String, Seq[Wertung])]()
    }
    else {
      @tailrec
      def splitToRiegenCount[A](sugg: Seq[(String, Seq[A])]): Seq[(String, Seq[A])] = {
        //cache.clear
        def split(riege: (String, Seq[A])): Seq[(String, Seq[A])] = {
          val (key, r) = riege
          val oldKey1 = (key + ".").split("\\.").headOption.getOrElse("Riege")
          val oldList = r.toList
          def occurences(key: String) = {
            val cnt = cache.getOrElse(key, 0) + 1
            cache.update(key, cnt)
            //println(f"occurences $key : $cnt")
            f"${cnt}%02d"
          }
//          println(f"key: $key, oldKey1: $oldKey1")
          val key1 = if(key.contains(".")) key else oldKey1 + "." + occurences(oldKey1)
          val key2 = oldKey1 + "." + occurences(oldKey1)
          val splitpos = r.size / 2
          //println(f"key1: $key1, key2: $key2")
          List((key1, oldList.take(splitpos)), (key2, oldList.drop(splitpos)))
        }
        val ret = sugg.sortBy(_._2.size).reverse
        //println((ret.size, riegencnt))
        if(ret.size < riegencnt) {
          splitToRiegenCount(split(ret.head) ++ ret.tail)
        }
        else {
          //println(ret.mkString("\n"))
          ret
        }
      }
      def groupKey(grplst: List[WertungView => String])(wertung: WertungView): String = {
        grplst.foldLeft(""){(acc, f) =>
          acc + "," + f(wertung)
        }.drop(1)// remove leading ","
      }
      @tailrec
      def groupWertungen(grp: List[WertungView => String], grpAll: List[WertungView => String]): Seq[(String, Seq[Wertung])] = {
        val sugg = wertungen.groupBy(w => groupKey(grp)(w._2.head)).toSeq
        if(riegencnt > 0 && sugg.size > riegencnt && grp.size > 1) {
          // too much groups
          // remove last grouper and try again
          groupWertungen(grp.reverse.tail.reverse, grpAll)
        }
        else {
          // per groupkey, transform map to seq, sorted by all groupkeys
          val prep = sugg.map{x =>
            (/*grpkey*/  x._1,
             /*values*/  x._2.foldLeft((Seq[(AthletView, Seq[WertungView])](), Set[Long]())){(acc, w) =>
                val (data, seen) = acc
                val (athlet, _ ) = w
                if(seen.contains(athlet.id)) acc else (w +: data, seen + athlet.id)
              }
              ._1.sortBy(w => groupKey(grpAll)(w._2.head))
            )
          }
          splitToRiegenCount(prep).map(w => (w._1, w._2.flatMap(wv => wv._2.map(wt => wt.toWertung(w._1)))))
        }
      }
      val wkGrouper: List[WertungView => String] = List(
          x => x.athlet.geschlecht,
          x => x.wettkampfdisziplin.programm.name,
          x => x.athlet.verein match {case Some(v) => v.easyprint case None => ""},
          // fallback ... should not happen
          x => (x.athlet.gebdat match {case Some(d) => f"$d%tY"; case _ => ""})
          )
      val wkFilteredGrouper = wkGrouper.take(if(riegencnt == 0) wkGrouper.size-1 else wkGrouper.size)
      val atGrouper: List[WertungView => String] = List(
          x => x.athlet.geschlecht,
          x => (x.athlet.gebdat match {case Some(d) => f"$d%tY"; case _ => ""}),
          x => x.athlet.verein match {case Some(v) => v.easyprint case None => ""}
          );
      if(wertungen.head._2.head.wettkampfdisziplin.notenSpez.isInstanceOf[Athletiktest])
        groupWertungen(atGrouper, atGrouper)
      else
        groupWertungen(wkFilteredGrouper, wkGrouper)
    }
  }

  def renameRiege(wettkampfid: Long, oldname: String, newname: String): Riege = {
    database withTransaction { implicit session =>
      val existing = sql"""select r.wettkampf_id, r.name, r.durchgang, r.start
             from riege r
             where wettkampf_id=$wettkampfid and name=${oldname}
          """.as[RiegeRaw].iterator.toList
        sqlu"""
                DELETE from riege where name=${newname.trim} and wettkampf_id=${wettkampfid}
        """.execute
      if(existing.isEmpty) {
        sqlu"""
                insert into riege
                       (name, wettkampf_id)
                VALUES (${newname.trim}, ${wettkampfid})
        """.execute
      }
      else {
        sqlu"""
                update riege
                set name=${newname.trim}
                where
                wettkampf_id=${wettkampfid} and name=${oldname}
        """.execute
      }

      sqlu"""   UPDATE wertung
                SET riege=${newname}
                WHERE wettkampf_id=${wettkampfid} and riege=${oldname}
          """.execute

      sqlu"""   UPDATE wertung
                SET riege2=${newname}
                WHERE wettkampf_id=${wettkampfid} and riege2=${oldname}
          """.execute

      sql"""   select r.name as riegenname, r.durchgang, d.*
               from riege r
               left outer join disziplin d on (r.start = d.id)
               where r.wettkampf_id=${wettkampfid} and r.name=${newname}
         """.as[Riege].iterator.toList.head
    }
  }

  def updateOrinsertRiege(riege: RiegeRaw): Riege = {
    database withTransaction { implicit session =>
      sqlu"""
                delete from riege where
                wettkampf_id=${riege.wettkampfId} and name=${riege.r}
        """.execute
      sqlu"""
                insert into riege
                (wettkampf_Id, name, durchgang, start)
                values (${riege.wettkampfId}, ${riege.r}, ${riege.durchgang}, ${riege.start})
        """.execute
       sql"""select r.name as riegenname, r.durchgang, d.*
             from riege r
             left outer join disziplin d on (r.start = d.id)
             where r.wettkampf_id=${riege.wettkampfId} and r.name=${riege.r}
          """.as[Riege].iterator.toList.head
    }
  }

  def selectRiegenRaw(wettkampfId: Long) = {
    database withSession { implicit session =>
       sql"""select r.wettkampf_id, r.name, r.durchgang, r.start
             from riege r
             where wettkampf_id=$wettkampfId
          """.as[RiegeRaw].iterator.toList
    }
  }
  def selectRiegen(wettkampfId: Long) = {
    database withSession { implicit session =>
       sql"""select r.name as riegenname, r.durchgang, d.*
             from riege r
             left outer join disziplin d on (r.start = d.id)
             where wettkampf_id=$wettkampfId
          """.as[Riege].iterator.toList
    }
  }
}