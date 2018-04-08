package ch.seidel.kutu.base

import org.scalatest.WordSpec
import org.scalatest.Matchers
import ch.seidel.kutu.domain._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory
import ch.seidel.kutu.http.ApiService
import org.scalactic.source.Position.apply
import akka.http.scaladsl.testkit.ScalatestRouteTest
import java.util.UUID
import java.sql.Date

trait KuTuBaseSpec extends WordSpec with Matchers with DBService with KutuService with ApiService with ScalaFutures with ScalatestRouteTest {
  private val logger = LoggerFactory.getLogger(this.getClass)
  DBService.startDB(Some(TestDBService.db))

  def insertGeTuWettkampf(name: String, anzvereine: Int) = {
    val wettkampf = createWettkampf(new Date(System.currentTimeMillis()), name, Set(20L), 3333, 7.5d, Some(UUID.randomUUID().toString))
    val programme: Seq[ProgrammView] = readWettkampfLeafs(wettkampf.programmId)
    val pgIds = programme.map(_.id)// 20 * 9 * 2 = 360
    val vereine = for (v <- (1 to anzvereine)) yield {
      val vereinID = createVerein(s"Verein-$v", Some(s"Verband-$v"))
      val athleten = for {
        pg <- (1 to pgIds.size) 
        a <- (1 to 1)
      } yield {
        val athlet = insertAthlete(Athlet(vereinID).copy(name = s"Athlet-$pg-$a"))
        assignAthletsToWettkampf(wettkampf.id, Set(pgIds(pg-1)), Set(athlet.id))
        athlet
      }
    }
    wettkampf
  }
  
  "application" should {
    "initialize new database" in {
      TestDBService.db
    }
  }
  
  
}