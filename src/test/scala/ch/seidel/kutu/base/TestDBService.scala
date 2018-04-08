package ch.seidel.kutu.base

import java.io.File
import java.util.Properties
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.SQLiteProfile.api.AsyncExecutor
import ch.seidel.kutu.domain.DBService

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
    val tempDatabase = Database.forURL(
        //url = "jdbc:sqlite:file:kutu?mode=memory&cache=shared",
        url = "jdbc:sqlite:" + dbfile,
        driver = "org.sqlite.JDBC",
        prop = proplite,
        user = "kutu",
        password = "kutu",
        executor = AsyncExecutor("DB-Actions", 10, 10000)
        )
    DBService.installDB(tempDatabase)
    DBService.updateDB(tempDatabase)
    logger.info("Database initialized")
    tempDatabase
  }  
}
