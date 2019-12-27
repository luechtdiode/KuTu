package ch.seidel.kutu.domain

import java.io.File
import java.nio.file.{Files, StandardOpenOption}
import java.text.{ParseException, SimpleDateFormat}
import java.util.Properties

import ch.seidel.kutu.Config
import ch.seidel.kutu.Config.{appVersion, userHomePath}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.{Database, DatabaseDef}
import slick.jdbc.PostgresProfile.api.{DBIO, actionBasedSQLInterpolation, jdbcActionExtensionMethods}

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

object DBService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  lazy private val dbFilename = s"kutu-$appVersion.sqlite"
  lazy private val dbhomedir = if (new File("./db/" + dbFilename).exists()) {
    logger.info("dbhomedir at: " + new File("./db/" + dbFilename).getAbsolutePath);
    "./db"
  }
  else if (new File(userHomePath + "/db").exists()) {
    logger.info("dbhomedir at: " + userHomePath + "/db");
    userHomePath + "/db"
    //    "./db"
  }
  else {
    val f = new File(userHomePath + "/db")
    logger.info("try to create for installing the db: " + f);
    try {
      f.mkdirs();
      logger.info("dbhomedir at: " + f);
      userHomePath + "/db"
    } catch {
      case _: Throwable =>
        val f = new File(".db")
        logger.warn("try to create for installing the db: " + f.getAbsolutePath);
        f.mkdirs();
        f.getPath
    }
  }
  private lazy val dbfile = new File(dbhomedir + "/" + dbFilename)

  private lazy val databaseLite = {
    logger.info(s"Using Database at ${dbfile.getAbsolutePath}")
    if (!dbfile.exists() || dbfile.length() == 0) {
      dbfile.createNewFile()
    }

    val proplite = new Properties()
    proplite.setProperty("date_string_format", "yyyy-MM-dd")
      //    prop.setProperty("connectionPool", "disabled")
      //    prop.setProperty("keepAliveConnection", "true")
      //    prop.setProperty("numberThreads ", "500")
      //    prop.setProperty("maxConnections ", "500")
      //    prop.setProperty("maximumPoolSize", "500")
      //    prop.setProperty("maxThreads", "500")

    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbfile.getAbsolutePath)
    hikariConfig.setDriverClassName("org.sqlite.JDBC")
    hikariConfig.setDataSourceProperties(proplite)
    hikariConfig.setUsername("kutu")
    hikariConfig.setPassword("kutu")
    hikariConfig.setMaximumPoolSize(500)

    val dataSource = new HikariDataSource(hikariConfig)
    import slick.jdbc.SQLiteProfile.api.{AsyncExecutor}
    def createDS = Database.forDataSource(dataSource, maxConnections = Some(500), executor = AsyncExecutor(name = "DB-Actions", minThreads = 500, maxThreads = 500, queueSize = 10000, maxConnections = 500), keepAliveConnection = true)
    var db = createDS
    val sqlScripts = List(
        "kutu-sqllite-ddl.sql"
      , "SetJournalWAL.sql"
      , "kutu-initialdata.sql"
    )
    try {
      installDB(db, sqlScripts)
    } catch {
      case _: DatabaseNotInitializedException =>
        db.close()
        val backupFile = {
          var cnt = 1;
          while(new File(s"${dbfile.getAbsolutePath}.backup-$cnt").exists()) cnt = cnt + 1
          new File(s"${dbfile.getName}.backup-$cnt")
        }
        dbfile.renameTo(backupFile)
        dbfile.createNewFile()
        db = createDS
        installDB(db, sqlScripts)
    }
    db
  }

  private lazy val databaseDef = {
    val postgresDBConfig = "kutudb_pg"
    if (Config.config.hasPath(postgresDBConfig)) try {
      val db = Database.forConfig(postgresDBConfig, Config.config)
      val sqlScripts = List(
          "kutu-pg-ddl.sql"
        , "kutu-initialdata.sql"
      )
      installDB(db, sqlScripts)
      db
    } catch {
      case e: Exception =>
        e.printStackTrace()
        databaseLite
    } else {
      databaseLite
    }
  }

  private var database: Option[DatabaseDef] = None

  def checkMigrationDone(db: DatabaseDef, script: String): Boolean = {
    try {
      0 < Await.result(db.run {
          sql"""
          select count(*) from migrations where name = $script
         """.as[Int].withPinnedSession
      }, Duration.Inf).headOption.getOrElse(0)
    } catch {
      case _: Throwable => false
    }
  }

  def migrationDone(db: DatabaseDef, script: String, log: String): Unit = {
    Await.result(db.run{
      sqlu"""
          insert into migrations (name, result) values ($script, $log)
         """.transactionally
    }, Duration.Inf)
    logger.info(s"... sql-script finished successfully.")
  }

  def parseLine(s: String): IndexedSeq[String] = {
    @tailrec
    def cutFields(s: String, acc: IndexedSeq[String]): IndexedSeq[String] = {
      if (s.isEmpty()) {
        acc
      }
      else if (s.startsWith("\"")) {
        val splitter = s.indexOf("\",", 1)
        if (splitter == -1) {
          val splitter2 = s.indexOf("\"", 1)
          if (splitter2 == 0) {
            acc :+ ""
          }
          else {
            acc :+ s.drop(1).take(splitter2 - 1)
          }
        }
        else if (splitter == 0) {
          cutFields(s.drop(splitter + 2), acc :+ "")
        }
        else {
          cutFields(s.drop(splitter + 2), acc :+ s.drop(1).take(splitter - 1))
        }
      }
      else if (s.startsWith(",")) {
        cutFields(s.drop(1), acc :+ "")
      }
      else {
        val splitter = s.indexOf(",", 1)
        if (splitter == -1) {
          acc :+ s
        }
        else if (splitter == 0) {
          cutFields(s.drop(1), acc :+ "")
        }
        else {
          cutFields(s.drop(splitter + 1), acc :+ s.take(splitter))
        }
      }
    }

    cutFields(s, IndexedSeq[String]())
  }

  def executeDBScript(script: Seq[String], db: DatabaseDef) = {
    def filterCommentLines(line: String) = {
      !line.trim().startsWith("-- ")
    }

    def combineMultilineStatement(acc: List[String], line: String) = {
      if (line.endsWith(";")) {
        acc.updated(acc.size - 1, acc.last + line) :+ ""
      }
      else {
        acc.updated(acc.size - 1, acc.last + line)
      }
    }

    def parse(lines: Seq[String]): List[String] = {
      lines.filter(filterCommentLines).foldLeft(List(""))(combineMultilineStatement)
        .filter(_.trim().length() > 0)
    }

    val statements = parse(script)
    val statementActions = statements.map { statement =>
      sqlu"""#$statement"""
    }

    val counters: Seq[Int] = if (statementActions.size == 1) {
      if (statements(0).startsWith("PRAGMA")) {
        Await.result(db.run(sql"""#${statements(0)}""".as[String]), Duration.Inf)
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


  def installDB(db: DatabaseDef, sqlScripts: List[String]) = {
    sqlScripts.filter{ filename =>
      !checkMigrationDone(db, filename)
    }.map { filename =>
      logger.info(s"running sql-script: $filename ...")
      val file = getClass.getResourceAsStream("/dbscripts/" + filename)
      val sqlscript = Source.fromInputStream(file, "utf-8").getLines().toList
      try {
        migrationDone(db, filename,
          executeDBScript(sqlscript, db))
      }
      catch {
        case e: Exception =>
          val fos = Files.newOutputStream(new File(dbhomedir + s"/$appVersion-$filename.err").toPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
          try {
            fos.write(e.getMessage.getBytes("utf-8"))
            fos.write("\n\nStatement:\n".getBytes("utf-8"));
            fos.write(sqlscript.mkString("\n").getBytes("utf-8"))
            fos.write("\n".getBytes("utf-8"))
          } finally {
            fos.close
          }
          throw new DatabaseNotInitializedException()
      }
    }
  }

  def startDB(alternativDB: Option[DatabaseDef] = None) = {
    alternativDB match {
      case Some(db) =>
        database = Some(db)
      case None =>
        database = Some(database.getOrElse(databaseDef))
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

  lazy val database: DatabaseDef = DBService.startDB()


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