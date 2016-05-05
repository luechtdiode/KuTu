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

  def updateWertungSimple(w: Wertung) {
    database withTransaction { implicit session =>
      sqlu"""       UPDATE wertung
                    SET note_d=${w.noteD}, note_e=${w.noteE}, endnote=${w.endnote}, riege=${w.riege}, riege2=${w.riege2}
                    WHERE id=${w.id}
          """.execute
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
  def listDisziplinesZuWettkampf(wettkampfId: Long, geschlecht: Option[String] = None): List[Disziplin] = {
    database withSession {implicit session: Session =>
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      val list = sql""" select distinct wd.disziplin_id, d.name
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
             """.as[Disziplin].iterator
      list.toList
    }
  }
  def listWettkampfDisziplines(wettkampfId: Long): List[Wettkampfdisziplin] = {
    database withSession {implicit session: Session =>
      val wettkampf: Wettkampf = readWettkampf(wettkampfId)
      val programme = readWettkampfLeafs(wettkampf.programmId).map(p => p.id).mkString("(", ",", ")")
      val list = sql""" select wd.id, wd.programm_id, wd.disziplin_id, printf('%s (%s)',d.name, p.name) as kurzbeschreibung, wd.ord
             from wettkampfdisziplin wd, disziplin d, programm p
             where
              wd.disziplin_id = d.id
              and wd.programm_id = p.id and
              programm_Id in #$programme
             order by
              wd.ord
                 """.as[(Long, Long, Long, String, Int)].iterator
      list.map{t => Wettkampfdisziplin(t._1, t._2, t._3, t._4, None, 0, t._5, 0, 0) }.toList
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
  @tailrec
  private def splitToRiegenCount[A](sugg: Seq[(String, Seq[A])], riegencnt: Int, cache: scala.collection.mutable.Map[String, Int]): Seq[(String, Seq[A])] = {
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
      splitToRiegenCount(split(ret.head) ++ ret.tail, riegencnt, cache)
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

  def suggestRiegen(wettkampfId: Long, rotationstation: Seq[Int]): Seq[(String, Seq[Wertung])] = {

    val riegencnt = rotationstation.reduce(_+_)
    val cache = scala.collection.mutable.Map[String, Int]()
    val wertungen = selectWertungen(wettkampfId = Some(wettkampfId)).groupBy(w => w.athlet)
    if(wertungen.isEmpty) {
      Seq[(String, Seq[Wertung])]()
    }
    else {

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
          splitToRiegenCount(prep, riegencnt, cache).map(w => (w._1, w._2.flatMap(wv => wv._2.map(wt => wt.toWertung(w._1)))))
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

  def suggestDurchgaenge(wettkampfId: Long, maxRiegenSize: Int = 14, durchgangfilter: Set[String] = Set.empty, programmfilter: Set[Long] = Set.empty, splitSex: SexDivideRule = GemischteRiegen, splitPgm: Boolean = true): Map[String, Map[Disziplin, Iterable[(String,Seq[Wertung])]]] = {
    val cache = scala.collection.mutable.Map[String, Int]()
    val wert = selectWertungen(wettkampfId = Some(wettkampfId)).groupBy(w => w.athlet)
    def listProgramme(x: Map[AthletView, Seq[WertungView]]) = x.map(w => w._2.head.wettkampfdisziplin.programm.name).toSet
    val findDurchgang = selectRiegenRaw(wettkampfId)
      .filter(rr => rr.durchgang.isDefined)
      .map(rr => rr.r -> rr.durchgang.get)
      .toMap

    val filteredWert = wert
    .filter{x =>
      (programmfilter.isEmpty || programmfilter.contains(x._2.head.wettkampfdisziplin.programm.id)) &&
      (durchgangfilter.isEmpty || (x._2.head.riege.forall(r => findDurchgang.get(r).forall(d => durchgangfilter.contains(d)))))
    }
    val programme = listProgramme(filteredWert)
    val progAthlWertungen = filteredWert.groupBy(x => if(splitPgm || durchgangfilter.isEmpty || programme.size == 1) x._2.head.wettkampfdisziplin.programm.name else programme.mkString(" & "))

    val riegencnt = 0 // riegencnt 0 is unlimited
    val disziplinlist = listDisziplinesZuWettkampf(wettkampfId)

    if(wert.isEmpty) {
      Map[String, Map[Disziplin, Iterable[(String,Seq[Wertung])]]]()
    }
    else {
      @tailrec
      def splitToRiegenCount[A](sugg: Seq[(String, Seq[A])], minCount: Int): Seq[(String, Seq[A])] = {
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
//            println(f"key: $key, oldKey1: $oldKey1")
          val key1 = if(key.contains(".")) key else oldKey1 + "." + occurences(oldKey1)
          val key2 = oldKey1 + "." + occurences(oldKey1)
          val splitpos = r.size / 2
          //println(f"key1: $key1, key2: $key2")
          List((key1, oldList.take(splitpos)), (key2, oldList.drop(splitpos)))
        }
        val ret = sugg.sortBy(_._2.size).reverse
        //println((ret.size, riegencnt))
        if(/*ret.size % minCount > 0 ||*/ (ret.size > 0 && ret.head._2.size > maxRiegenSize)) {
          splitToRiegenCount(split(ret.head) ++ ret.tail, minCount)
        }
        else {
          //println(ret.mkString("\n"))
          ret
        }
      }

      def groupWertungen(programm: String, prgWertungen: Map[AthletView, Seq[WertungView]], grp: List[WertungView => String], grpAll: List[WertungView => String], startgeraete: Seq[Disziplin])/*: Seq[(String, Seq[Wertung])]*/ = {
        val sugg = prgWertungen.groupBy(w => groupKey(grp)(w._2.head)).toSeq
        // per groupkey, transform map to seq, sorted by all groupkeys
        val atheltenInRiege = sugg.map{x =>
          (/*grpkey*/  x._1, // Riegenname
           /*values*/  x._2.foldLeft((Seq[(AthletView, Seq[WertungView])](), Set[Long]())){(acc, w) =>
              val (data, seen) = acc
              val (athlet, _ ) = w
              if(seen.contains(athlet.id)) acc else (w +: data, seen + athlet.id)
            }
            ._1.sortBy(w => groupKey(grpAll)(w._2.head)) // Liste der Athleten in der Riege, mit ihren Wertungen
          )
        }
        val riegen = splitToRiegenCount(atheltenInRiege, 0/*, cache*/).map(r => Map(r._1 -> r._2))
        // Maximalausdehnung. Nun die sinnvollen Zusammenlegungen
        type RiegeAthletWertungen = Map[String, Seq[(AthletView, Seq[WertungView])]]

        def durchgangRiegeSize(w: RiegeAthletWertungen) = {
          w.values.foldLeft(0)((acc, item) => acc + item.size)
        }

        def easyPrint(w: RiegeAthletWertungen): String = {
          w.keys.mkString(s"${w.size} Riegen (", ", ", ")")
        }

        @tailrec
        def combineToDurchgangSize(relevantcombis: Seq[RiegeAthletWertungen]): Seq[RiegeAthletWertungen] = {
          def splitAt(splitpoint: Int, sorted: Seq[RiegeAthletWertungen], first: Boolean) = {
            if(splitpoint < 0 || splitpoint >= sorted.size) {
              sorted
            }
            else {
              val headItems = sorted.take(math.max(0, splitpoint))
              val splittedItem = sorted(splitpoint)
              val tailItems = sorted.drop(math.min(sorted.size, splitpoint+1))
              val ret = if(first) {
                val sh1: RiegeAthletWertungen = splittedItem.tail
                val sh2: RiegeAthletWertungen = Map(splittedItem.head._1 -> splittedItem.head._2)
                headItems ++ Seq(sh1, sh2) ++ tailItems
              }
              else {
                val sh1: RiegeAthletWertungen = splittedItem.take(splittedItem.size -1)
                val sh2: RiegeAthletWertungen = Map(splittedItem.last._1 -> splittedItem.last._2)
                headItems ++ Seq(sh1, sh2) ++ tailItems
              }
              if(sorted.map(durchgangRiegeSize).sum != ret.map(durchgangRiegeSize).sum) {
                println(s"splitAt input: ${(sorted).map(x => easyPrint(x)).mkString("", "\n  ", "")}")
                println(s"splitAt output: ${(ret).map(x => easyPrint(x)).mkString("", "\n  ", "")}")
              }
              ret
            }
          }
          def splitEven(unsorted: Seq[RiegeAthletWertungen]) = {
            val sorted = unsorted.sortBy { durchgangRiegeSize }.reverse
            val combis = sorted.filter(x => x.size > 1)
            if(combis.nonEmpty) {
              val splitpoint = sorted.indexOf(combis(combis.size / 2))
              splitAt(splitpoint, sorted, true)
            }
            else {
              sorted
            }
          }
          def splitSmallest(unsorted: Seq[RiegeAthletWertungen]) = {
            val sorted = unsorted.sortBy { durchgangRiegeSize }
            val splitpoint = sorted.lastIndexWhere { x => x.size > 1 }
            val output = splitAt(splitpoint, sorted, false)
            if(unsorted.map(durchgangRiegeSize).sum != output.map(durchgangRiegeSize).sum) {
              println(s"splitSmallest input: ${(unsorted).map(x => easyPrint(x)).mkString("", "\n  ", "")}")
              println(s"splitSmallest output: ${(output).map(x => easyPrint(x)).mkString("", "\n  ", "")}")
            }
            output
          }
          def splitLargest(unsorted: Seq[RiegeAthletWertungen]) = {
            val sorted = unsorted.sortBy { durchgangRiegeSize }.reverse
            val splitpoint = sorted.indexWhere { x => x.size > 1 }
            splitAt(splitpoint, sorted, true)
          }
          def filterM(list: Seq[RiegeAthletWertungen]) = {
            ???
          }
          if(relevantcombis.size > startgeraete.size) {
            // sind mind. startgeraete.size Riegen zu finden, die gepaart die Maxilmalgruppengrösse nicht übersteigen?

            @tailrec
            def buildPairs(requiredPairs: Int, candidate: Seq[RiegeAthletWertungen], acc: Seq[(RiegeAthletWertungen, RiegeAthletWertungen)]): Seq[(RiegeAthletWertungen, RiegeAthletWertungen)] = {
              println(s"    => buildPairs: candidates=${candidate.size}, acc=${acc.size}, requiredPairs=$requiredPairs")
              if(candidate.isEmpty) {
                acc
              }
              else if(requiredPairs <= acc.size) {
                acc
              }
              else if (candidate.size > 1 && (durchgangRiegeSize(candidate.head) + durchgangRiegeSize(candidate.last) <= maxRiegenSize)) {
                // Auf max Riegen-Size zusammenmergen (auffüllen)
                buildPairs(requiredPairs, candidate.tail.take(candidate.size -2), acc :+ (candidate.head, candidate.last))
              }
              else if(candidate.size > 1 && (acc.size < requiredPairs - startgeraete.size || acc.size % startgeraete.size != 0)) {
                buildPairs(requiredPairs, candidate.tail, acc)
              }
              else {
                acc
              }
            }
            val turnerSum = relevantcombis.map(durchgangRiegeSize).sum
            val combisturnerSum = relevantcombis.map(durchgangRiegeSize).sum
            val idealRiegenCnt = (math.floor(turnerSum / maxRiegenSize) + ((turnerSum / maxRiegenSize) % startgeraete.size)).intValue()
            val targetPairCnt = math.max(startgeraete.size, startgeraete.size * math.floor((relevantcombis.size - startgeraete.size) / startgeraete.size).intValue())
            println(s"turnerSum=$turnerSum, idealRiegenCnt=$idealRiegenCnt, targetPairCnt=$targetPairCnt")

            @tailrec
            def nc(candidate: Seq[RiegeAthletWertungen], level: Int): Seq[RiegeAthletWertungen] = {
              val possiblePairs = buildPairs(targetPairCnt, candidate, Seq.empty)
              val stable = candidate.filter(c => !possiblePairs.exists(p => p._1 == c || p._2 == c) )
              val newcombis = stable ++ possiblePairs.map{x => x._1 ++ x._2}
              if(newcombis.map(durchgangRiegeSize).sum != candidate.map(durchgangRiegeSize).sum) {
                println("ALERT")
              }
              if(splitSex == GemischterDurchgang) {
                newcombis
              }
              else {
                val base = candidate.map( durchgangRiegeSize )
                println(s"  => candidates=${candidate.size}, max=${base.max}, min=${base.min}, possiblePairs=${possiblePairs.size}, stable=${stable.size}, newcombis=${newcombis.size}, level=$level")
                if(level == 0 || newcombis.size >= targetPairCnt || (newcombis.size < targetPairCnt && newcombis.size % startgeraete.size == 0)) {
                  newcombis
                }
                else if(newcombis.nonEmpty && newcombis.size < targetPairCnt) {
                  val splitted = (newcombis.size until targetPairCnt).foldLeft(newcombis){(acc, idx) =>
                    if(acc.nonEmpty) splitEven(acc) else acc
                  }
                  splitted
                }
                else {
                  val n = if(level % 3 == 0) splitSmallest(candidate) else splitSmallest(newcombis)
                  nc(n, level-1)
                }
              }
            }
            println(s"relevantcombis=${relevantcombis.size}, fullPairs=$targetPairCnt")
            val newcombis = splitSex match {
              case GemischterDurchgang =>
                val rcm = relevantcombis.filter(c => c.head._2.head._1.geschlecht.equalsIgnoreCase("M"))
                val rcw = relevantcombis.filter(c => c.head._2.head._1.geschlecht.equalsIgnoreCase("W"))
                nc(rcm, rcm.size) ++ nc(rcw, rcw.size)
              case _ =>
                nc(relevantcombis, relevantcombis.size)
            }
            //val newcombis = nc(relevantcombis, relevantcombis.size)// stable ++ possiblePairs.map{x => x._1 ++ x._2}

            if(targetPairCnt > 0 && newcombis.size < relevantcombis.size) {
              println(s"combis merging: ${(newcombis).map(x => easyPrint(x)).mkString("", "\n  ", "")}")
              combineToDurchgangSize(newcombis.sortBy { durchgangRiegeSize }.reverse)
            }
            else if(targetPairCnt > 0 && newcombis.size < relevantcombis.size && ((newcombis.size % startgeraete.size == 0) || (newcombis.size / startgeraete.size >= 1))) {
              println(s"take newcombis : ${newcombis.map(x => easyPrint(x)).mkString("", "\n  ", "")}")
              newcombis
            }
            else {
              println(s"combis elsefall : ${relevantcombis.map(x => easyPrint(x)).mkString("", "\n  ", "")}")
              relevantcombis
            }
          }
          else if(relevantcombis.size < startgeraete.size && relevantcombis.exists { x => x.size > 1 }) {
            combineToDurchgangSize(splitEven(relevantcombis))
          }
          else {
            println(s"combis.size <= ${startgeraete.size} : ${relevantcombis.map(x => easyPrint(x)).mkString("", "\n  ", "")}")
            relevantcombis
          }
        }
        def vereinStats(like: Option[Verein], startriege: RiegeAthletWertungen) = {
          startriege.values.flatMap{x =>
            x.flatMap{xx =>
              xx._1.verein.filter{verein =>
//                println(s"seek $like found $verein")
                like.equals(Some(verein))
              }
            }
          }.size
        }

        @tailrec
        def spreadEven(startriegen: Seq[RiegeAthletWertungen]): Seq[RiegeAthletWertungen] = {
          /*
           * 1. Durchschnittsgrösse ermitteln
           * 2. Grösste Abweichungen ermitteln (kleinste, grösste)
           * 3. davon (teilbare) Gruppen filtern
           * 4. schieben.
           */
          val averageSize = startriegen.map(durchgangRiegeSize).sum / startriegen.size

          def smallestDividable(r: RiegeAthletWertungen) = {
            if(r.size > 1) {
              Some(r.keys.map(x => (x, r(x))).toSeq.sortBy(y => y._2.size).head)
            }
            else {
              None
            }
          }
          val stats = startriegen.map{raw =>
            // Riege, Anz. Gruppen, Anz. Turner, Std.Abweichung, (kleinste Gruppekey, kleinste Gruppe)
            val anzTurner = durchgangRiegeSize(raw)
            val abweichung = anzTurner - averageSize
            (raw, raw.size, anzTurner, abweichung, smallestDividable(raw) )
          }.sortBy(_._4).reverse // Abweichung
          val idxKleinste = stats.size-1
          val kleinsteGruppe = stats(idxKleinste)
          type GrpStats = (RiegeAthletWertungen, Int, Int, Int, Option[(String, Seq[(AthletView, Seq[WertungView])])])
          def checkSC(p1: GrpStats, p2: GrpStats): Boolean = {
            splitSex match {
              case GemischterDurchgang =>
                val ret = p1._1.head._2.head._1.geschlecht.equals(p2._1.head._2.head._1.geschlecht)
                ret
              case _ =>
                true
            }
          }
          stats.find{p =>
            val b11 = p._5 != None && p != kleinsteGruppe
            val b12 = checkSC(p, kleinsteGruppe)
            val b2 = p._3 > averageSize
            val b3 = b11 && p._5.get._2.size + kleinsteGruppe._3 <= averageSize
            lazy val v1 = vereinStats(p._5.get._2.head._1.verein, p._1)
            lazy val v2 = vereinStats(p._5.get._2.head._1.verein, kleinsteGruppe._1)
            lazy val b4 = v1 - p._5.get._2.size < v2 + p._5.get._2.size
            lazy val b5 = {
              p._5.get._2.size + kleinsteGruppe._3 <= averageSize + (kleinsteGruppe._3 / 2)
            }

            b11 && b12 && ((b2 && b3 && b4) || (b2 && b3 && b5 && v1 - p._5.get._2.size == 0 && v2 > 0))
          } match {
            case Some(groessteTeilbare) =>
              val gt = groessteTeilbare._1 - groessteTeilbare._5.get._1
              val sg = kleinsteGruppe._1 ++ Map(groessteTeilbare._5.get._1 -> groessteTeilbare._5.get._2)
              spreadEven(gt +: startriegen.filter(sr => sr != groessteTeilbare._1 && sr != kleinsteGruppe._1) :+ sg)
            case _ => stats.find(p => p != kleinsteGruppe && checkSC(p, kleinsteGruppe) && p._3 > averageSize && p._5 != None && p._5.get._2.size + kleinsteGruppe._3 <= averageSize) match {
              case Some(groessteTeilbare) =>
                val gt = groessteTeilbare._1 - groessteTeilbare._5.get._1
                val sg = kleinsteGruppe._1 ++ Map(groessteTeilbare._5.get._1 -> groessteTeilbare._5.get._2)
                spreadEven(gt +: startriegen.filter(sr => sr != groessteTeilbare._1 && sr != kleinsteGruppe._1) :+ sg)
              case _ => startriegen
            }
          }
        }

        def handleVereinMerges(startriegen: Seq[RiegeAthletWertungen]): Seq[RiegeAthletWertungen] = {
          splitSex match {
            case GemischteRiegen =>
              bringVereineTogether(startriegen)
            case GemischterDurchgang =>
              startriegen
            case GetrennteDurchgaenge =>
              bringVereineTogether(startriegen)
          }
        }

        def bringVereineTogether(startriegen: Seq[RiegeAthletWertungen]): Seq[RiegeAthletWertungen] = {
          val averageSize = startriegen.map(durchgangRiegeSize).sum / startriegen.size
          startriegen.flatMap{raw =>
            raw.values.flatMap{r =>
              r.map{rr =>
                (rr._1.verein -> raw)
              }
            }
          }.groupBy{vereinraw =>
            vereinraw._1
          }.map{vereinraw =>
            vereinraw._1 -> vereinraw._2.map(_._2).toSet
          }.foldLeft(startriegen){(acc, item) =>
            val (verein, riegen) = item
            val ret = riegen.map(f => (f, f.filter(ff => ff._2.exists(p => p._1.verein.equals(verein))).keys.head)).foldLeft(acc.toSet){(accc, riegen2) =>
              val (geraetRiege, filteredRiege) = riegen2
              val toMove = geraetRiege(filteredRiege)
              val v1 = vereinStats(verein, geraetRiege)
              val anzTurner = durchgangRiegeSize(geraetRiege)
              accc.find{p =>
                p != geraetRiege &&
                accc.contains(geraetRiege) &&
                durchgangRiegeSize(p) <= math.min(maxRiegenSize, averageSize + (averageSize * 0.3).intValue()) &&
                vereinStats(verein, p) > v1
              }
              match {
                case Some(zielriege) =>
                  println(s"moving $filteredRiege from ${geraetRiege.keys} to ${zielriege.keys}")
                  val gt = geraetRiege - filteredRiege
                  val sg = zielriege ++ Map(filteredRiege -> toMove)
                  val r1 = accc - zielriege
                  val r2 = r1 + sg
                  val r3 = r2 - geraetRiege
                  val ret = r3 + gt
                  ret
                case _ => accc
              }
            }
            ret.toSeq
          }
        }

        val alignedriegen = if(riegen.isEmpty) riegen else handleVereinMerges(spreadEven(handleVereinMerges(spreadEven(combineToDurchgangSize(riegen)))))

        // Startgeräteverteilung
        alignedriegen.zipWithIndex.flatMap{ r =>
          val (rr, index) = r
          val startgeridx =  (index + startgeraete.size) % startgeraete.size
          rr.keys.map{riegenname =>
            println(s"Durchgang $programm (${index / startgeraete.size + 1}), Start ${startgeraete(startgeridx).easyprint}, ${rr(riegenname).size} Tu/Ti der Riege $riegenname")
            (s"$programm (${index / startgeraete.size + 1})", riegenname, startgeraete(startgeridx), rr(riegenname))
          }
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

      val riegen = progAthlWertungen.flatMap{x =>
        val (programm, wertungen) = x
        wertungen.head._2.head.wettkampfdisziplin.notenSpez match {
          case at: Athletiktest =>
            splitSex match {
              case GemischteRiegen =>
                groupWertungen(programm, wertungen, atGrouper, atGrouper, disziplinlist)
              case GemischterDurchgang =>
                groupWertungen(programm, wertungen, atGrouper, atGrouper, disziplinlist)
              case GetrennteDurchgaenge =>
                val m = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("M"))
                val w = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("W"))
                groupWertungen(programm + "-Tu", m, atGrouper, atGrouper, disziplinlist) ++
                groupWertungen(programm + "-Ti", w, atGrouper, atGrouper, disziplinlist)
            }
          case KuTuWettkampf =>
            splitSex match {
              case GemischteRiegen =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, disziplinlist)
              case GemischterDurchgang =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, disziplinlist)
              case GetrennteDurchgaenge =>
                val m = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("M"))
                val w = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("W"))
                groupWertungen(programm + "-Tu", m, wkFilteredGrouper, wkGrouper, disziplinlist) ++
                groupWertungen(programm + "-Ti", w, wkFilteredGrouper, wkGrouper, disziplinlist)
            }
          case GeTuWettkampf =>
            // Barren wegschneiden (ist kein Startgerät)
            val m = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("M"))
            splitSex match {
              case GemischteRiegen =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, disziplinlist.take(disziplinlist.size -1))
              case GemischterDurchgang =>
                groupWertungen(programm, wertungen, wkFilteredGrouper, wkGrouper, disziplinlist.take(disziplinlist.size -1))
              case GetrennteDurchgaenge =>
                val w = wertungen.filter(w => w._1.geschlecht.equalsIgnoreCase("W"))
                groupWertungen(programm + "-Tu", m, wkFilteredGrouper, wkGrouper, disziplinlist.take(disziplinlist.size -1)) ++
                groupWertungen(programm + "-Ti", w, wkFilteredGrouper, wkGrouper, disziplinlist.take(disziplinlist.size -1))
            }
        }
      }

      val ret: Map[String, Map[Disziplin, Iterable[(String,Seq[Wertung])]]] =
        //.map(w => (w._1, w._2.flatMap(wv => wv._2.map(wt => wt.toWertung(w._1)))))
        riegen.groupBy{r => r._1}
        .map{rr =>
          val (durchgang, disz) = rr
          println(durchgang)
          (durchgang, disz.groupBy(d => d._3).map{rrr =>
            val (start, athleten) = rrr
            println(start.name)
            (start, athleten.map{a =>
              val (_, riegenname, _, wertungen) = a
              println(riegenname, wertungen.size)
              (riegenname, wertungen.flatMap{wv =>
                wv._2.map(wt =>
                  wt.toWertung(riegenname)
                )
              })
            })
          })
        }

      ret
    }
  }

  def deleteRiege(wettkampfid: Long, oldname: String) {
    database withTransaction { implicit session =>
      sqlu"""
                DELETE from riege where name=${oldname.trim} and wettkampf_id=${wettkampfid}
          """.execute

      sqlu"""   UPDATE wertung
                SET riege=''
                WHERE wettkampf_id=${wettkampfid} and riege=${oldname}
          """.execute

      sqlu"""   UPDATE wertung
                SET riege2=''
                WHERE wettkampf_id=${wettkampfid} and riege2=${oldname}
          """.execute
    }
  }

  def renameDurchgang(wettkampfid: Long, oldname: String, newname: String) = {
    database withTransaction { implicit session =>
        sqlu"""
                update riege
                set durchgang=${newname.trim}
                where
                wettkampf_id=${wettkampfid} and durchgang=${oldname}
        """.execute
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

  def cleanAllRiegenDurchgaenge(wettkampfid: Long) {
    database withTransaction { implicit session =>
      sqlu"""
                delete from riege where
                wettkampf_id=${wettkampfid}
        """.execute
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

  def insertRiegenWertungen(riege: RiegeRaw, wertungen: Seq[Wertung]) {
    database withTransaction { implicit session =>
      sqlu"""
                  replace into riege
                  (wettkampf_Id, name, durchgang, start)
                  values (${riege.wettkampfId}, ${riege.r}, ${riege.durchgang}, ${riege.start})
          """.execute
      for(w <- wertungen) {
        sqlu"""     UPDATE wertung
                    SET riege=${riege.r}
                    WHERE id=${w.id}
          """.execute
      }
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