package ch.seidel.kutu.base

import java.io.File
import java.util.Properties
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.SQLiteProfile.api.AsyncExecutor
import ch.seidel.kutu.domain.{DBService, NewUUID}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.sqlite.SQLiteConnection
import slick.jdbc.JdbcBackend

object TestDBService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val proplite = {
    val prop = new Properties()
    prop.setProperty("date_string_format", "yyyy-MM-dd")
    prop
  }

  val db = {
    val dbfile = {
      val tf = File.createTempFile("junit-", ".sqlite")
      tf.deleteOnExit
      tf.getAbsolutePath
    }

    import slick.jdbc.SQLiteProfile.api.AsyncExecutor

    logger.info(s"starting database with $dbfile ...")
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbfile)
    hikariConfig.setDriverClassName("org.sqlite.JDBC")
    hikariConfig.setDataSourceProperties(proplite)
    hikariConfig.setUsername("kutu")
    hikariConfig.setPassword("kutu")

    val dataSource = new HikariDataSource(hikariConfig)
    //val tempDatabase = Database.forDataSource(dataSource, maxConnections = Some(500), executor = AsyncExecutor("DB-Actions", 500, 10000), keepAliveConnection = true)
    val tempDatabase = Database.forDataSource(dataSource, maxConnections = Some(10), executor = AsyncExecutor(name = "DB-Actions", minThreads = 10, maxThreads = 10, queueSize = 10000, maxConnections = 10), keepAliveConnection = true)


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
      //, "AddWKTestPgms-sqllite.sql"
      , "AddAltersklassenToWettkampf-sqllite.sql"
      , "AddPunktegleichstandsregelToWettkampf-sqllite.sql"
      , "teamwertung-ddl.sql"
      , "AddTeamToWettkampf-ddl.sql"
      , "tg-allgaeu-wk4ak0-15.sql"
    )
    installDBFunctions(tempDatabase)

    DBService.installDB(tempDatabase, sqlScripts)
    logger.info("Database initialized")
    tempDatabase
  }

  def installDBFunctions(dbdef: JdbcBackend.Database): Unit = {
    val session = dbdef.createSession()
    try {
      NewUUID.install(session.conn.unwrap(classOf[SQLiteConnection]))
    } finally {
      session.close()
    }
  }
}
