package ch.seidel.kutu.domain

import java.io.File
import java.nio.file.{Files, StandardOpenOption}
import java.text.{ParseException, SimpleDateFormat}
import java.util.Properties
import ch.seidel.kutu.Config
import ch.seidel.kutu.Config.{appVersion, userHomePath}
import ch.seidel.kutu.data.ResourceExchanger
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteConnection
import slick.jdbc
import slick.jdbc.{JdbcBackend, JdbcDataSourceFactory}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api.{DBIO, actionBasedSQLInterpolation, jdbcActionExtensionMethods}

import java.sql.Date
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

object DBService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def buildFilename(version: String) = s"kutu-$version.sqlite"

  lazy private val dbFilename = buildFilename(appVersion)
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

    def createDS(dbfn: String) = {
      import slick.jdbc.SQLiteProfile.api.AsyncExecutor
      val proplite = new Properties()
      proplite.setProperty("date_string_format", "yyyy-MM-dd")
      val config = new HikariConfig()
      config.setJdbcUrl(s"jdbc:sqlite:$dbfn")
      config.setDriverClassName("org.sqlite.JDBC")
      config.setDataSourceProperties(proplite)
      config.setUsername("kutu")
      config.setPassword("kutu")
      //config.setMaximumPoolSize(500)
      val dataSource = new HikariDataSource(config)

      Database.forDataSource(dataSource, maxConnections = Some(10), executor = AsyncExecutor(name = "DB-Actions", minThreads = 10, maxThreads = 10, queueSize = 10000, maxConnections = 10), keepAliveConnection = true)
    }

    val sqlScripts = List(
      "kutu-sqllite-ddl.sql"
      , "SetJournalWAL.sql"
      , "kutu-initialdata.sql"
      , "AddTimeTable-sqllite.sql"
      , "InitTimeTable.sql"
      , "AddDurchgangTable-sqllite.sql"
      , "InitDurchgangTable.sql"
      , "FixEmptyRiegeTimeTableIssue-sqllite.sql"
      , "AddAnmeldungTables-sqllite.sql"
      , "AddAnmeldungTables-u2-sqllite.sql"
      , "AddNotificationMailToWettkampf-sqllite.sql"
      , "AddWKDisziplinMetafields-sqllite.sql"
      , "AddWKTestPgms-sqllite.sql"
      , "AddAltersklassenToWettkampf-sqllite.sql"
      , "AddPunktegleichstandsregelToWettkampf-sqllite.sql"
      , "teamwertung-ddl.sql"
      , "AddTeamToWettkampf-ddl.sql"
      , "tg-allgaeu-wk4ak0-15.sql"
      , "wettkampf-metadata-ddl.sql"
      , "AddWKProgrammBestOfCount-sqllite.sql"
      , "AddScoreTemplateTable-sqllite.sql"
    )

    (!dbfile.exists() || dbfile.length() == 0, Config.importDataFrom) match {
      case (true, Some(version)) =>
        migrateFrom(createDS, version)
      case (true, _) =>
        dbfile.createNewFile()
      case _ => // nothing to do
    }

    var db = createDS(dbfile.getAbsolutePath)
    try {
      val session = db.createSession()
      try {
        NewUUID.install(session.conn.unwrap(classOf[SQLiteConnection]))
      } finally {
        session .close()
      }
      installDB(db, sqlScripts)
    } catch {
      case _: DatabaseNotInitializedException =>
        db.close()
        val backupFile = {
          var cnt = 1;
          while (new File(s"${dbfile.getAbsolutePath}.backup-$cnt").exists()) cnt = cnt + 1
          new File(s"${dbfile.getAbsolutePath}.backup-$cnt")
        }
        dbfile.renameTo(backupFile)
        dbfile.createNewFile()
        db = createDS(dbfile.getAbsolutePath)
        val session = db.createSession()
        try {
          NewUUID.install(session.conn.unwrap(classOf[SQLiteConnection]))
        } finally {
          session.close()
        }
        installDB(db, sqlScripts)
    }
    db
  }

  private def migrateFrom(dsCreate: String => JdbcBackend.Database, version: String): Unit = {
    val preversion = new File(dbhomedir + "/" + buildFilename(version))
    if (preversion.exists()) {
      logger.info(s"Migrating Database from ${preversion.getAbsolutePath}")
      try {
        Files.copy(preversion.toPath, dbfile.toPath)
        val db = dsCreate(dbfile.getAbsolutePath)
        try {
          logger.info(s"applying migration scripts to ${dbfile.getAbsolutePath}")
        } finally {
          db.close()
        }
      } catch {
        case e: Exception =>
          logger.error("Migration failed! Create a new Database instead ...", e)
          dbfile.createNewFile()
      }
    } else {
      dbfile.createNewFile()
    }
  }

  def transferData(source: jdbc.JdbcBackend.Database, target: jdbc.JdbcBackend.Database): Unit = {
    ResourceExchanger.moveAll(source, target)
  }

  private lazy val databaseDef = {
    val dbconfigname_key = "X_DB_CONFIG_NAME"

    def startExternalDB = {
      val dbconfig_key = Config.config.getString(dbconfigname_key)
      logger.info("load db-config with " + dbconfig_key);
      val db = Database.forConfig(dbconfig_key, Config.config)
      logger.info("db-config with " + dbconfig_key + " loaded");
      val sqlScripts = List(
        "kutu-pg-ddl.sql"
        , "kutu-initialdata.sql"
        , "AddTimeTable-pg.sql"
        , "InitTimeTable.sql"
        , "AddDurchgangTable-pg.sql"
        , "InitDurchgangTable.sql"
        , "FixEmptyRiegeTimeTableIssue-pg.sql"
        , "AddAnmeldungTables-pg.sql"
        , "AddAnmeldungTables-u1-pg.sql"
        , "AddAnmeldungTables-u2-pg.sql"
        , "AddNotificationMailToWettkampf-pg.sql"
        , "AddWKDisziplinMetafields-pg.sql"
        , "AddWKTestPgms-pg.sql"
        , "AddAltersklassenToWettkampf-pg.sql"
        , "AddPunktegleichstandsregelToWettkampf-pg.sql"
        , "teamwertung-ddl.sql"
        , "AddTeamToWettkampf-ddl.sql"
        , "tg-allgaeu-wk4ak0-15.sql"
        , "tg-allgaeu-wk4ak0-15.sql"
        , "wettkampf-metadata-ddl.sql"
        , "AddWKProgrammBestOfCount-pg.sql"
        , "AddScoreTemplateTable-pg.sql"
      )
      installDB(db, sqlScripts)
      /*Config.importDataFrom match {
        case Some(version) =>
          logger.info("try to migrate from version " + version);
          val scriptname = s"MigratedFrom-$version"
          if (!checkMigrationDone(db, scriptname)) {
            logger.info("migration from version " + version);
            transferData(databaseLite, db)
            migrationDone(db, scriptname, "from migration")
            logger.info("migration from version " + version + " done");
          }
        case _ =>
      }*/
      logger.info("database initialization ready with config " + dbconfig_key);
      db
    }

    if (Config.config.hasPath(dbconfigname_key) && Config.config.hasPath(Config.config.getString(dbconfigname_key))) try {
      startExternalDB
    } catch {
      case e: Exception =>
        logger.error("Could not initialize database as expected.", e);
        Thread.sleep(30000L)
        try {
          startExternalDB
        } catch {
          case ee: Exception =>
            logger.error("Could not initialize database as expected.", ee);
            Thread.sleep(60000L)
            startExternalDB
        }
    } else {
      logger.info("No dedicated db-config defined. Initialize the fallback with sqlite");
      databaseLite
    }
  }

  private var database: Option[Database] = None

  def checkMigrationDone(db: Database, script: String): Boolean = {
    logger.info(s"checking installation-status for ${script} ...")
    val ret = try {
      0 < Await.result(db.run {
        sql"""
          select count(*) from migrations where name = $script
         """.as[Int].withPinnedSession
      }, Duration.Inf).headOption.getOrElse(0)
    } catch {
      case _: Throwable => false
    }
    if (ret) {
      logger.info(s"the script ${script} is already installed")
    } else {
      logger.info(s"the script ${script} should be executed")
    }
    ret
  }

  def migrationDone(db: Database, script: String, log: String): Unit = {
    Await.result(db.run {
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

  def executeDBScript(script: Seq[String], db: Database) = {
    def filterCommentLines(line: String) = {
      !line.trim().startsWith("--")
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


  def installDB(db: Database, sqlScripts: List[String]) = {
    sqlScripts.filter { filename =>
      !checkMigrationDone(db, filename)
    }.map { filename =>
      logger.info(s"running sql-script: $filename ...")
      val file = getClass.getResourceAsStream("/dbscripts/" + filename)
      if (file == null) {
        println(filename + " not found")
      } else {
        val sqlscript = Source.fromInputStream(file, "utf-8").getLines().toList
        try {
          migrationDone(db, filename,
            executeDBScript(sqlscript, db))
        }
        catch {
          case e: Exception =>
            logger.error("Error on executing database setup script", e);
            val errorfile = new File(dbhomedir + s"/$appVersion-$filename.err")
            errorfile.getParentFile.mkdirs()
            val fos = Files.newOutputStream(errorfile.toPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
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
  }

  def migrateFromPreviousVersion(db: Database) = {
    val sqlScripts = Seq(
      "SetJournalWAL.sql"
      , "AddMigrationTable.sql"
    )

    sqlScripts.filter { filename =>
      !new File(dbhomedir + s"/$appVersion-$filename.log").exists()
    }.foreach { filename =>
      val file = getClass.getResourceAsStream("/dbscripts/" + filename)
      val sqlscript = Source.fromInputStream(file, "utf-8").getLines().toList
      val log = try {
        logger.info(s"running sql-script: $filename")

        executeDBScript(sqlscript, db)
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
          throw e
      }

      val fos = Files.newOutputStream(new File(dbhomedir + s"/$appVersion-$filename.log").toPath, StandardOpenOption.CREATE_NEW)
      try {
        fos.write(log.getBytes("utf-8"))
      } finally {
        fos.close
      }
    }
    sqlScripts
  }

  def startDB(alternativDB: Option[Database] = None) = {
    alternativDB match {
      case Some(db) =>
        database = Some(db)
      case None =>
        database = Some(database.getOrElse(databaseDef))
    }
    database.get
  }


}

trait DBService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def database: Database = DBService.startDB()

  implicit def getSQLDate(date: String): Date = str2SQLDate(date)
}