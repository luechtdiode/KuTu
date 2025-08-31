package ch.seidel.kutu.base

import java.sql.Date
import java.util.UUID
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.ApiService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory

import java.time.{Instant, LocalDate}
import java.time.temporal.ChronoUnit

trait KuTuBaseSpec extends AnyWordSpec with Matchers with DBService with KutuService with ApiService with ScalaFutures with ScalatestRouteTest {
  private val logger = LoggerFactory.getLogger(this.getClass)
  DBService.startDB(Some(TestDBService.db))

  def insertGeTuWettkampf(name: String, anzvereine: Int) = {
    val wettkampf = createWettkampf(new Date(System.currentTimeMillis()), name, Set(20L), "testmail@test.com", 3333, 7.5d, Some(UUID.randomUUID().toString), "", "", "", "Kategorie/AlterAufsteigend/Verein/Vorname/Name/Rotierend/AltInvers", "")
    val programme: Seq[ProgrammView] = readWettkampfLeafs(wettkampf.programmId)
    val pgIds = programme.map(_.id)// 20 * 9 * 2 = 360
    val vereine = for (v <- (1 to anzvereine)) yield {
      val vereinID = createVerein(s"Verein-$v", Some(s"Verband-$v"))
      val athleten = for {
        pg <- (1 to pgIds.size) 
        a <- (1 to Math.max(1, 4-pg))
      } yield {
        val athlet = insertAthlete(Athlet(vereinID).copy(name = s"Athlet-$pg-$a"))
        val tuple: (Long, Option[Media]) = (athlet.id, None)
        assignAthletsToWettkampf(wettkampf.id, Set(pgIds(pg-1)), Set(tuple), None)
        athlet
      }
    }
    wettkampf
  }
    def insertTurn10Wettkampf(name: String, anzvereine: Int) = {
    val wettkampf = createWettkampf(new Date(System.currentTimeMillis()), name, Set(211L), "testmail@test.com", 3333, 7.5d, Some(UUID.randomUUID().toString), "7,8,9,11,13,15,17,19", "7,8,9,11,13,15,17,19", "", "", "")
    val programme: Seq[ProgrammView] = readWettkampfLeafs(wettkampf.programmId)
    val pgIds = programme.map(_.id)
    val vereine = for (v <- (1 to anzvereine)) yield {
      val vereinID = createVerein(s"Verein-$v", Some(s"Verband-$v"))
      val athleten = for {
        pg <- (1 to pgIds.size)
        a <- (1 to Math.max(1, 20 / pg))
      } yield {
        val alter = 6 + a * pg
        val gebdat = Date.valueOf(LocalDate.now().minus(alter, ChronoUnit.YEARS))
        val athlet = insertAthlete(Athlet(vereinID).copy(
          name = s"Athlet-$pg-$a",
          gebdat = Some(gebdat)
        ))
        val tuple: (Long, Option[Media]) = (athlet.id, None)
        assignAthletsToWettkampf(wettkampf.id, Set(pgIds(pg-1)), Set(tuple), None)
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