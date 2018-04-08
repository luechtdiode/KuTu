package ch.seidel.kutu.domain

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import ch.seidel.kutu.http.ApiService
import ch.seidel.kutu.akka._
import org.scalatest.junit.JUnitRunner
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.renderer.RiegenBuilder
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.stream.scaladsl.Sink
import ch.seidel.kutu.squad.DurchgangBuilder
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class KuTuWettkampfCollectResultsSpec extends KuTuBaseSpec {
  val testwettkampf = insertGeTuWettkampf("TestGetuWK")
  
  "wettkampf" should {

    "return inserted competition with (GET /competition)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/api/competition")

      request ~> allroutes(x => x) ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and TestGetuWK should be in the list:
        assert(entityAs[String].contains("\"titel\":\"TestGetuWK\""))
      }
      
    }
    
    "collect results" in {
      println("suggest riegen ...")
      val riegenzuteilungen = DurchgangBuilder(this).suggestDurchgaenge(testwettkampf.id, 0)
      cleanAllRiegenDurchgaenge(testwettkampf.id)
      println("save suggested riegen ...")
		  for{
		    durchgang <- riegenzuteilungen.keys
			  (start, riegen) <- riegenzuteilungen(durchgang)
			  (riege, wertungen) <- riegen
		  } {
			  insertRiegenWertungen(RiegeRaw(
			    wettkampfId = testwettkampf.id,
			    r = riege,
			    durchgang = Some(durchgang),
			    start = Some(start.id)
			  ), wertungen)
		  }
		  
      println("start competition ...")
      CompetitionCoordinatorClientActor.createActorSinkSource("testcase", testwettkampf.uuid.get, None).to(Sink.foreach(msg => println(s"from Sink: $msg")))

      println("read all riegen ...")
      val riegen = RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(UUID.fromString(testwettkampf.uuid.get)).toList)
      val durchgaenge = riegen
        .filter(gr => gr.durchgang.nonEmpty) 
        .map(gr => gr.durchgang.get)
        .toSet.toList.sorted
      durchgaenge.foreach{d =>
         Await.result(CompetitionCoordinatorClientActor.publish(StartDurchgang(testwettkampf.uuid.get, d)), Duration.Inf)
      }
      println("submit new wertungen ...")
      riegen.groupBy(gr => (gr.durchgang, gr.halt)).map{grd =>
        val ((durchgang, halt), grs) = grd
        val step = grs.flatMap{gr => 
          gr.kandidaten.flatMap{ k => 
            k.wertungen.map{ wertung => 
              val command = UpdateAthletWertung(
                              loadAthleteView(k.id), 
                              k.wertungen.filter(w => w.id == wertung.id).map(_.toWertung.updatedWertung(wertung.copy(noteE = scala.math.BigDecimal(8.5)).toWertung)).head, 
                              testwettkampf.uuid.get, 
                              gr.durchgang.get, 
                              wertung.wettkampfdisziplin.disziplin.id, 
                              0)
              CompetitionCoordinatorClientActor.publish(command)
            }
          }
        }.toList
        s"durchg: $durchgang, halt: $halt, anz: ${step.foldLeft(Seq[KutuAppEvent]()){(acc, item) => acc :+ Await.result(item,Duration.Inf)}.size}"
      }.foreach(evt => println("Received: " + evt))
    }
  }

}