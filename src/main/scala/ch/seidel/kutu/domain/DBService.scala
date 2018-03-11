package ch.seidel.kutu.domain

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Properties

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

import org.slf4j.LoggerFactory

import ch.seidel.kutu.Config.appVersion
import ch.seidel.kutu.Config.userHomePath
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.SQLiteProfile.api.AsyncExecutor
import slick.jdbc.SQLiteProfile.api.DBIO
import slick.jdbc.SQLiteProfile.api.actionBasedSQLInterpolation
import slick.jdbc.SQLiteProfile.api.jdbcActionExtensionMethods

trait DBService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  
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
  lazy val dbFilename = s"kutu-$appVersion.sqlite"
  lazy val dbhomedir = if(new File("./db/kutu.sqlite").exists()) {
    logger.info("using db at: " + new File("./db/" + dbFilename).getAbsolutePath);
    "./db"
  }
  else if(new File(userHomePath + "/db").exists()) {
    logger.info("using db at: " + userHomePath + "/db");
    userHomePath + "/db"
//    "./db"
  }
  else {
    val f = new File(userHomePath + "/db")
    logger.info("try to create for installing the db: " + f);
    try {
      f.mkdirs();
      logger.info("using db at: " + f);
      userHomePath + "/db"
    } catch {
      case _ : Throwable =>
        val f = new File(".db")
        logger.warn("try to create for installing the db: " + f.getAbsolutePath);
        f.mkdirs();
        f.getPath
    }
  }
  val dbfile = new File(dbhomedir + "/" + dbFilename)

  lazy val databaselite = Database.forURL(
    url = "jdbc:sqlite:" + dbfile.getAbsolutePath,
    driver = "org.sqlite.JDBC",
    prop = proplite,
    user = "kutu",
    password = "kutu",
    executor = AsyncExecutor("DB-Actions", 30, 10000))

  lazy val database = {
    logger.info(s"Using Database at ${dbfile.getAbsolutePath}")
    databaselite
  }
//  lazy val database = databasemysql

  def installDB {
    val sqlScripts = Seq(
         "kutu-sqllite-ddl.sql"
        ,"kutu-sqllite-initialdata.sql"        
        )

    sqlScripts.foreach { filename =>
      logger.info(s"running sql-script: $filename")
      val file = getClass.getResourceAsStream("/dbscripts/" + filename)
      executeDBScript(Source.fromInputStream(file, "utf-8").getLines())
    }
  }

  def updateDB {
    val sqlScripts = Seq(
//        ,"AlterWettkampfUUID.sql"
        )

    sqlScripts.filter{ filename =>
      val f = new File(dbhomedir + s"/$appVersion-$filename.log")
      !f.exists()
    }.foreach { filename =>
      val file = getClass.getResourceAsStream("/dbscripts/" + filename)
      val log = try {
        logger.info(s"running sql-script: $filename")

        executeDBScript(Source.fromInputStream(file, "utf-8").getLines())
      }
      catch {
        case e: Exception => e.getMessage
      }
      
      val fos = Files.newOutputStream(new File(dbhomedir + s"/$appVersion-$filename.log").toPath, StandardOpenOption.CREATE_NEW)
      try {
        fos.write(log.getBytes("utf-8"))
      } finally {
        fos.close
      }
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

  def executeDBScript(script: Iterator[String]) = {
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
    val statements = parse(script).map(statement => sqlu"""#$statement""")
    val counters = Await.result(database.run(DBIO.sequence(statements).transactionally), Duration.Inf)
    statements.zip(counters).map(p => s"statements: ${p._1.statements.mkString("[", ",", "]")}\n\tupdate-cnt: ${p._2}").mkString("\n")
  }


  lazy val startDB = {
    logger.info("starting database ...")
    if(!dbfile.exists() || dbfile.length() == 0) {
      dbfile.createNewFile()
      installDB
    }
    updateDB
    logger.info("Database initialized")
    true
  }
  
  if (!startDB) {
    logger.error("Database not initialized!!!")
    System.exit(-1)
  }
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
  
}