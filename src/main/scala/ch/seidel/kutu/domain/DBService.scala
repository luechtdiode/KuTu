package ch.seidel.kutu.domain

import java.io.File
import java.nio.file.{Files, StandardOpenOption}
import java.text.{ParseException, SimpleDateFormat}
import java.util.Properties

import ch.seidel.kutu.Config.{appVersion, userHomePath}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.{Database, DatabaseDef}
import slick.jdbc.SQLiteProfile.api.{AsyncExecutor, DBIO, actionBasedSQLInterpolation, jdbcActionExtensionMethods}

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

object DBService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  lazy private val proplite = {
    val prop = new Properties()
    prop.setProperty("date_string_format", "yyyy-MM-dd")
//    prop.setProperty("connectionPool", "disabled")
    prop.setProperty("keepAliveConnection", "true")
//    prop.setProperty("numberThreads ", "1")
    prop.setProperty("maxConnections ", "256")
    prop.setProperty("maximumPoolSize", "256")
    prop
  }
  lazy private val dbFilename = s"kutu-$appVersion.sqlite"
  lazy private val dbhomedir = if(new File("./db/" + dbFilename).exists()) {
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
  private lazy val dbfile = new File(dbhomedir + "/" + dbFilename)

  private lazy val databaselite = {
    logger.info(s"Using Database at ${dbfile.getAbsolutePath}")
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbfile.getAbsolutePath)
    hikariConfig.setDriverClassName("org.sqlite.JDBC")
    hikariConfig.setDataSourceProperties(proplite)
    hikariConfig.setUsername("kutu")
    hikariConfig.setPassword("kutu")

    val dataSource = new HikariDataSource(hikariConfig)
    Database.forDataSource(dataSource, maxConnections = Some(500), executor = AsyncExecutor("DB-Actions", 500, 10000), keepAliveConnection = true)
//    Database.forURL(
//      url = "jdbc:sqlite:" + dbfile.getAbsolutePath,
//      driver = "org.sqlite.JDBC",
//      prop = proplite,
//      user = "kutu",
//      password = "kutu",
//      executor = AsyncExecutor("DB-Actions", 256, 10000)
//    )
  }
  
  private var database: Option[DatabaseDef] = None  
//  lazy val database = databasemysql
  

  def updateDB(db: DatabaseDef) = {
    val sqlScripts = Seq(
      "SetJournalWAL.sql"
      ,"OptionalWertungen.sql"
      ,"AddRiegenIndicies.sql"
    )

    sqlScripts.filter{ filename =>
      val f = new File(dbhomedir + s"/$appVersion-$filename.log")
      !f.exists()
    }.foreach { filename =>
      val file = getClass.getResourceAsStream("/dbscripts/" + filename)
      val log = try {
        logger.info(s"running sql-script: $filename")

        executeDBScript(Source.fromInputStream(file, "utf-8").getLines(), db)
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
      log
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

  def executeDBScript(script: Iterator[String], db: DatabaseDef) = {
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
    val statements = parse(script)
    val statementActions = statements.map(statement => sqlu"""#$statement""")
    val counters: Seq[Int] = if (statementActions.size == 1) {
      if (statements(0).startsWith("PRAGMA")) {
        Await.result(db.run(statementActions.head), Duration.Inf)
        Seq(1)
      } else {
        Await.result(db.run(DBIO.sequence(statementActions)), Duration.Inf)
      }
    } else {
      Await.result(db.run(DBIO.sequence(statementActions).transactionally), Duration.Inf)
    }

    val log = statements.zip(counters).map(p => s"statements: ${p._1}\n\tupdate-cnt: ${p._2}").mkString("\n")
    log
  }


  def installDB(db: DatabaseDef) = {
    val sqlScripts = Seq(
         "kutu-sqllite-ddl.sql"
        ,"kutu-sqllite-initialdata.sql"
        )

    sqlScripts.map { filename =>
      logger.info(s"running sql-script: $filename")
      val file = getClass.getResourceAsStream("/dbscripts/" + filename)
      executeDBScript(Source.fromInputStream(file, "utf-8").getLines(), db)
    }
  }

  lazy val _startDB = {
    logger.info("starting database ...")
    database = database match {
      case None => 
        if(!dbfile.exists() || dbfile.length() == 0) {
          dbfile.createNewFile()
          installDB(databaselite)
        }
        updateDB(databaselite)
        Some(databaselite)
      case Some(db) => 
        Some(db)
    }
    logger.info("Database initialized")
    true
  }
  
  def startDB(alternativDB: Option[DatabaseDef] = None) = {
    alternativDB match {
      case Some(db) => 
        database = Some(db)
      case None =>
    }
    if (!_startDB) {
      logger.error("Database not initialized!!!")
      System.exit(-1)
    }
    database.get
  }
  
  val sdf = new SimpleDateFormat("dd.MM.yyyy")
  val sdfShort = new SimpleDateFormat("dd.MM.yy")
  val sdfExported = new SimpleDateFormat("yyyy-MM-dd")
  val sdfYear = new SimpleDateFormat("yyyy")

}

trait DBService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  
  def database: DatabaseDef = DBService.startDB()


  implicit def getSQLDate(date: String) = try {
    new java.sql.Date(DBService.sdf.parse(date).getTime)
  }
  catch {
    case d: ParseException => try {
    	new java.sql.Date(DBService.sdfExported.parse(date).getTime)
    }
    catch {
      case dd: ParseException =>
        new java.sql.Date(DBService.sdfShort.parse(date).getTime)
    }
  }
}