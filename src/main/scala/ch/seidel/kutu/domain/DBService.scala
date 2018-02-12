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
import org.slf4j.LoggerFactory

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
    "./db"
  }
  else {
    val f = new File(System.getProperty("user.home") + "/kutuapp/db")
    logger.debug("try to create for installing the db: " + f);
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
        ,"AlterKampfrichter.sql"
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
  
}