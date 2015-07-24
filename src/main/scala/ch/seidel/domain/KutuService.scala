package ch.seidel.domain

import java.text.SimpleDateFormat
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.JdbcBackend.Session
import scala.slick.jdbc.StaticQuery._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.jdbc.PositionedResult
import scala.annotation.tailrec
import scala.slick.lifted.Query
import scala.slick.jdbc.StaticQuery
import scala.slick.jdbc.SetParameter
import java.util.concurrent.TimeUnit
import java.time.LocalDate
import java.time.temporal.TemporalField
import java.time.Period
import java.io.File
import scala.io.Source
import org.sqlite.SQLiteConfig.Pragma
import org.sqlite.SQLiteConfig.DatePrecision
import java.util.Properties

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

  lazy val databaselite = Database.forURL(
    url = "jdbc:sqlite:./db/kutu.sqlite",
    driver = "org.sqlite.JDBC",
    prop = proplite,
    user = "kutu",
    password = "kutu")

  lazy val database = databaselite
//  lazy val database = databasemysql

  val dbfile = new File("./db/kutu.sqlite")

  def installDB {
    val sqlfile = new File("./Scripts/kutu-sqllite-ddl.sql")
    val sqldatafile = new File("./Scripts/kutu-sqllite-initialdata.sql")
    val sqlwkdatafile = new File("./Scripts/kutu-daten.sql")
    if(sqlfile.exists()) {
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
        parse(Source.fromFile(sqlfile).getLines()).foreach(execStatement)
        parse(Source.fromFile(sqldatafile).getLines()).foreach(execStatement)
//        parse(Source.fromFile(sqlwkdatafile).getLines()).foreach(execStatement)
      }
    }
  }

  if(!dbfile.exists() || dbfile.length() == 0) {
    dbfile.createNewFile()
    installDB
  }
//  if(selectVereine.size == 0) {
//    installDB
//  }

  val sdf = new SimpleDateFormat("dd.MM.yyyy")

  def addFilter[P](query: StaticQuery[P,_], predicate: String, param: Option[P]) = {
    param map {
      case Some(p) => query + predicate + p.toString()
      case _ => query
    }
  }

  implicit def getSQLDate(date: String) = new java.sql.Date(sdf.parse(date).getTime)
  private implicit val getVereinResult = GetResult(r => Verein(r.<<[Long], r.<<[String]))
  private implicit val getVereinOptionResult = GetResult(r =>  r.nextLongOption() match {
    case Some(id) => Some(getVereinResult(r))
    case _        => { r.skip; r.skip; None }
  })
  private implicit val getAthletResult = GetResult(r =>
    Athlet(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
  private implicit val getAthletViewResult = GetResult(r =>
    AthletView(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r))
  private implicit val getDisziplinResult = GetResult(r =>
    Disziplin(r.<<[Long], r.<<[String]))
  private implicit val getWettkampfResult = GetResult(r =>
    Wettkampf(r.<<[Long], r.<<[java.sql.Date], r.<<[String], r.<<[Long], r.<<[Int]))
  private implicit val getWettkampfDisziplinResult = GetResult(r =>
    Wettkampfdisziplin(r.<<[Long], r.<<[Long], r.<<[Long], r.<<[String], r.nextBlobOption(), r.<<, r.<<[Int]))

  implicit def getWettkampfDisziplinViewResultCached(r: PositionedResult)(implicit session: Session, cache: scala.collection.mutable.Map[Long, ProgrammView]) = {
    val id = r.<<[Long]
    val pgm = readProgramm(r.<<[Long], cache)
    WettkampfdisziplinView(id, pgm, r, r.<<[String], r.nextBytesOption(), readNotenModus(id, pgm, r.<<), r.<<)
  }
  private implicit def getResultWertungView(implicit session: Session, cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WertungView(r.<<[Long], r, r, r, r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<[scala.math.BigDecimal], r.<<))
  private implicit def getWettkampfViewResultCached(implicit session: Session, cache: scala.collection.mutable.Map[Long, ProgrammView]) = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long], cache), r.<<[Int]))
  private implicit def getWettkampfViewResult(implicit session: Session) = GetResult(r =>
    WettkampfView(r.<<[Long], r.<<[java.sql.Date], r.<<[String], readProgramm(r.<<[Long]), r.<<[Int]))
  private implicit val getProgrammRawResult = GetResult(r =>
    ProgrammRaw(r.<<[Long], r.<<[String], r.<<[Int], r.<<[Long], r.<<[Int], r.<<[Int], r.<<[Int]))

  def readNotenModus(id: Long, pgm: ProgrammView, notenfaktor: Double)(implicit session: Session): NotenModus = {
    val skala = sql"""
                   select kurzbeschreibung, punktwert from notenskala
                   where wettkampfdisziplin_id=${id}
                   order by punktwert
       """.as[(String,Double)].build()
    if(pgm.head.id == 1) {
      Athletiktest(skala.toMap, notenfaktor)
    }
    else {
      Wettkampf
    }
  }

  def listRootProgramme(): List[ProgrammView] = {
    database withSession { implicit session =>
      sql"""select id from programm where parent_id is null or parent_id = 0""".as[Long].build().foldLeft(List[ProgrammView]()){(acc, pid) =>
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
                            """.as[ProgrammRaw].build().head
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
            """.as[ProgrammRaw].build()

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

  def updateWertung(w: Wertung): WertungView = {
    database withTransaction { implicit session =>
      sqlu"""       UPDATE wertung
                    SET note_d=${w.noteD}, note_e=${w.noteE}, endnote=${w.endnote}, riege=${w.riege}
                    WHERE id=${w.id};
          """.execute

      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      sql"""
                    SELECT w.id, a.*, v.*, wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wk.*, note_d as difficulty, note_e as execution, endnote, riege
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    WHERE w.id=${w.id}
                    order by wd.programm_id, wd.ord
       """.as[WertungView].build().head
    }
  }

  def listAthletenWertungenZuProgramm(progids: Seq[Long], wettkampf: Long, riege: String = "%") = {
    database withSession { implicit session =>
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      sql"""
                   SELECT w.id, a.*, v.*, wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wk.*, note_d as difficulty, note_e as execution, endnote, riege
                   FROM wertung w
                   inner join athlet a on (a.id = w.athlet_id)
                   left outer join verein v on (a.verein = v.id)
                   inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                   inner join disziplin d on (d.id = wd.disziplin_id)
                   inner join programm p on (p.id = wd.programm_id)
                   inner join wettkampf wk on (wk.id = w.wettkampf_id)
                   where wd.programm_id in (#${progids.mkString(",")})
                     and w.wettkampf_id = $wettkampf
                     and ($riege = '%' or w.riege = $riege)
                   order by wd.programm_id, wd.ord
       """.as[WertungView].build()
    }
  }

  def listAthletenWertungenZuRiege(progids: Seq[Long], wettkampf: Long, riege: String) = {
    database withSession { implicit session =>
      implicit val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      sql"""
                   SELECT w.id, a.*, v.*, wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wk.*, note_d as difficulty, note_e as execution, endnote, riege
                   FROM wertung w
                   inner join athlet a on (a.id = w.athlet_id)
                   left outer join verein v on (a.verein = v.id)
                   inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                   inner join disziplin d on (d.id = wd.disziplin_id)
                   inner join programm p on (p.id = wd.programm_id)
                   inner join wettkampf wk on (wk.id = w.wettkampf_id)
                   where wd.programm_id in (#${progids.mkString(",")})
                     and w.riege = $riege
                     and w.wettkampf_id = $wettkampf
                   order by wd.programm_id, wd.ord
       """.as[WertungView].build()
    }
  }

  def listRiegenZuWettkampf(wettkampf: Long) = {
    database withSession { implicit session =>
      sql"""
                   SELECT distinct w.riege
                   FROM wertung w
                   where w.riege not null and w.wettkampf_id = $wettkampf
       """.as[String].build()
    }
  }

  def listAthletenZuWettkampf(progids: Seq[Long]) = {
    database withSession { implicit session =>
      sql"""       select a.* from athlet a
                   inner join wertung w on (a.id = w.athlet_id)
                   inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                   where wd.programm_id in (#${progids.mkString(",")})
         """.as[AthletView].build()
    }
  }

  def createWettkampf(datum: java.sql.Date, titel: String, programmId: Set[Long], withAthlets: Option[(Long, Athlet) => Boolean] = Some({ (_, _) => true })): Wettkampf = {
    database withTransaction { implicit session =>
      val cache = scala.collection.mutable.Map[Long, ProgrammView]()
      val programs = programmId map (p => readProgramm(p, cache))
      val heads = programs map (_.head)
      if (!heads.forall { h => h.id == heads.head.id }) {
        throw new IllegalArgumentException("Programme nicht aus der selben Gruppe können nicht in einen Wettkampf aufgenommen werden")
      }
      sqlu"""
                    insert into wettkampf
                    (datum, titel, programm_Id)
                    values (${datum}, ${titel}, ${heads.head.id})
          """.execute

      val wk = sql"""
                    select * from wettkampf
                    where id in (select max(id) from wettkampf)
                  """.as[Wettkampf].build().head

      assignAthletsToWettkampfS(wk.id, programs, withAthlets, session)
      wk
    }
  }
  def deleteWettkampf(wettkampfid: Long) {
    database withTransaction { implicit session: Session =>
      sqlu"""       delete from wertung where wettkampf_id=${wettkampfid}""".execute
      sqlu"""       delete from wettkampf where id=${wettkampfid}""".execute
    }
  }
  def createVerein(name: String): Long = {
    database withTransaction { implicit session: Session =>
      sqlu"""       insert into verein
                    (name) values (${name})""".execute
      sql"""
                    select id from verein
                    where id in (select max(id) from verein)
         """.as[Long].build().head
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
          a <- selectAthletes.build().filter(altersfilter(pgm, _)).filter{x =>
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

  def listWettkaempfe = {
    sql"""          select * from wettkampf """.as[Wettkampf]
  }
  def listWettkaempfeView(implicit session: Session) = {
    val cache = scala.collection.mutable.Map[Long, ProgrammView]()
    sql"""          select * from wettkampf """.as[WettkampfView].build()
  }

  def readWettkampf(id: Long)(implicit session: Session) = {
    sql"""          select * from wettkampf where id=$id""".as[Wettkampf].build().head
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
                    SELECT w.id, a.*, v.*, wd.id, wd.programm_id, d.*, wd.kurzbeschreibung, wd.detailbeschreibung, wd.notenfaktor, wd.ord, wk.*, note_d as difficulty, note_e as execution, endnote, riege
                    FROM wertung w
                    inner join athlet a on (a.id = w.athlet_id)
                    left outer join verein v on (a.verein = v.id)
                    inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
                    inner join disziplin d on (d.id = wd.disziplin_id)
                    inner join programm p on (p.id = wd.programm_id)
                    inner join wettkampf wk on (wk.id = w.wettkampf_id)
                    #$where
                    order by wd.programm_id, wd.ord
         """.as[WertungView].build()
    }
  }

  def selectAthletes = {
    sql"""          select * from athlet""".as[Athlet]
  }

  def selectAthletesView = {
    database withSession { implicit session =>
      sql"""        select * from athlet inner join verein on (verein.id = athlet.verein) """.as[AthletView].list()
    }
  }

  def selectVereine = {
    database withSession { implicit session =>
      sql"""        select id, name from verein""".as[Verein].list()
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
      def getId = sql"""
                    select max(athlet.id) as maxid
                    from athlet
                    where name=${athlete.name} and vorname=${athlete.vorname} and gebdat=${athlete.gebdat} and verein=${athlete.verein}
           """.as[Long].build().headOption

      if (athlete.id == 0) {
        getId match {
          case Some(id) if(id > 0) =>
            sqlu"""
                    replace into athlet
                    (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein)
                    values (${id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein})
            """.execute
            sql"""select * from athlet where id = ${id}""".as[Athlet].build().head
          case _ => sqlu"""
                    replace into athlet
                    (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein)
                    values (${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein})
            """.execute
            sql"""select * from athlet where id = (select max(athlet.id) from athlet)""".as[Athlet].build().head
        }
      }
      else {
        sqlu"""
                    replace into athlet
                    (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein)
                    values (${athlete.id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein})
            """.execute
        athlete
      }
    }
  }

  def insertOrupdateAthlete(athlete: Athlet) = {
    insertAthlete(athlete)
//    database withTransaction { implicit session =>
//      def getId = sql"""
//                    select max(athlet.id) as maxid
//                    from athlet
//                    where name=${athlete.name} and vorname=${athlete.vorname} and gebdat=${athlete.gebdat} and verein=${athlete.verein}
//           """.as[Long].build().headOption
//
//      if (athlete.id == 0) {
//        val id: Long = getId match {
//          case Some(id) if(id > 0) =>
//            sqlu"""
//                    replace into athlet
//                    (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein)
//                    values (${id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein})
//            """.execute
//            id
//          case _ => sqlu"""
//                    replace into athlet
//                    (js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein)
//                    values (${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein})
//            """.execute
//            getId.get
//        }
//        sql"""select * from athlet where id = ${id}""".as[Athlet].build().headOption.get
//      }
//      else {
//        sqlu"""
//                    replace into athlet
//                    (id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein)
//                    values (${athlete.id}, ${athlete.js_id}, ${athlete.geschlecht}, ${athlete.name}, ${athlete.vorname}, ${athlete.gebdat}, ${athlete.strasse}, ${athlete.plz}, ${athlete.ort}, ${athlete.verein})
//            """.execute
//        athlete
//      }
//    }
  }

  /* Riegenbuilder:
--     1. Anzahl Rotationen (min = 1, max = Anzahl Teilnehmer),
--     2. Anzahl Stationen (min = 1, max = Anzahl Diszipline im Programm),
--     => ergibt die optimale Anzahl Riegen (Rotationen * Stationen)
--     3. Gruppiert nach Programm oder Jahrgang (Jahrgang im Athletiktest-Modus),
--     4. Gruppiert nach Verein oder Jahrgang (Verein im Athletiktest-Modus)
--     => Verknüpfen der Gruppen auf eine Start-Station/-Rotation
--     => operation suggestRiegen(WettkampfId, Rotationen/Stationen:List<Integer>): Map<Riegennummer,List<WertungId>>
*/
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
        }.drop(1)
      }
      @tailrec
      def groupWertungen(grp: List[WertungView => String], grpAll: List[WertungView => String]): Seq[(String, Seq[Wertung])] = {
        val sugg = wertungen.groupBy(w => groupKey(grp)(w._2.head)).toSeq
        if(sugg.size > riegencnt && grp.size > 1) {
          groupWertungen(grp.reverse.tail.reverse, grpAll)
        }
        else {
          val prep = sugg.map(x => (x._1, x._2.foldLeft((Seq[(AthletView, Seq[WertungView])](), Set[Long]())){(acc, w) =>
            if(acc._2.contains(w._1.id)) acc else (w +: acc._1, acc._2 + w._1.id)
          }
          ._1.sortBy(w => groupKey(grpAll)(w._2.head))))
          splitToRiegenCount(prep).map(w => (w._1, w._2.flatMap(wv => wv._2.map(wt => wt.toWertung(w._1)))))
        }
      }
      val wkGrouper: List[WertungView => String] = List(
          x => x.athlet.geschlecht,
          x => x.wettkampfdisziplin.programm.name,
          x => x.athlet.verein match {case Some(v) => v.easyprint case None => ""},
          x => (x.athlet.gebdat match {case Some(d) => f"$d%tY"; case _ => ""})
          );
      val atGrouper: List[WertungView => String] = List(
          x => x.athlet.geschlecht,
          x => (x.athlet.gebdat match {case Some(d) => f"$d%tY"; case _ => ""}),
          x => x.athlet.verein match {case Some(v) => v.easyprint case None => ""}
          );
      if(wertungen.head._2.head.wettkampfdisziplin.notenSpez.isInstanceOf[Athletiktest])
        groupWertungen(atGrouper, atGrouper)
      else
        groupWertungen(wkGrouper, wkGrouper)
    }
  }
}