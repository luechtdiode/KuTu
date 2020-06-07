package ch.seidel.kutu.base

import java.io.File
import java.util.Properties

import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.SQLiteProfile.api.AsyncExecutor
import ch.seidel.kutu.domain.DBService
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

object TestDBService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val proplite = {
    val prop = new Properties()
    prop.setProperty("date_string_format", "yyyy-MM-dd")
    prop.setProperty("connectionPool", "disabled")
    prop.setProperty("keepAliveConnection", "true")
    prop
  }
    

  val db = {
    val dbfile = {
      val tf = File.createTempFile("junit-", ".sqlite")
      tf.deleteOnExit
      tf.getAbsolutePath
    }
    
    logger.info(s"starting database with $dbfile ...")
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbfile)
    hikariConfig.setDriverClassName("org.sqlite.JDBC")
    hikariConfig.setDataSourceProperties(proplite)
    hikariConfig.setUsername("kutu")
    hikariConfig.setPassword("kutu")

    val dataSource = new HikariDataSource(hikariConfig)
    val tempDatabase = Database.forDataSource(dataSource, maxConnections = Some(500), executor = AsyncExecutor("DB-Actions", 500, 10000), keepAliveConnection = true)

//    val tempDatabase = Database.forURL(
//        //url = "jdbc:sqlite:file:kutu?mode=memory&cache=shared",
//        url = "jdbc:sqlite:" + dbfile,
//        driver = "org.sqlite.JDBC",
//        prop = proplite,
//        user = "kutu",
//        password = "kutu",
//        executor = AsyncExecutor("DB-Actions", 500, 10000)
//        )
    val sqlScripts = List(
        "kutu-sqllite-ddl.sql"
      , "SetJournalWAL.sql"
      , "kutu-initialdata.sql"
      , "AddTimeTable-sqllite.sql"
      , "InitTimeTable.sql"
      , "AddDurchgangTable-sqllite.sql"
      , "InitDurchgangTable.sql"
      , "FixEmptyRiegeTimeTableIssue-sqllite.sql"
    )
    DBService.installDB(tempDatabase, sqlScripts)
    logger.info("Database initialized")
    tempDatabase
  }  
}
