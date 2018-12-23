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
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class KuTuWettkampfCollectResultsSpec extends KuTuBaseSpec {
  val testwettkampf = insertGeTuWettkampf("TestGetuWK", 4)

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
  
  "wettkampf" should {
//
//    "return inserted competition with (GET /competition)" in {
//      // note that there's no need for the host part in the uri:
//      val request = HttpRequest(uri = "/api/competition")
//
//      request ~> allroutes(x => x) ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and TestGetuWK should be in the list:
//        assert(entityAs[String].contains("\"titel\":\"TestGetuWK\""))
//      }
//
//    }
//
//    "collect results" in {
//      println("start competition ...")
//      CompetitionCoordinatorClientActor.createActorSinkSource("testcase", testwettkampf.uuid.get, None).to(Sink.foreach(msg => println(s"from Sink: $msg")))
//
//      println("read all riegen ...")
//      val riegen = RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(UUID.fromString(testwettkampf.uuid.get)).toList)
//      val durchgaenge = riegen
//        .filter(gr => gr.durchgang.nonEmpty)
//        .map(gr => gr.durchgang.get)
//        .toSet.toList.sorted
//
//      // open all durchgaenge
//      durchgaenge.foreach{d =>
//         println("starting durchgang :" + d)
//         Await.result(CompetitionCoordinatorClientActor.publish(StartDurchgang(testwettkampf.uuid.get, d)), Duration.Inf)
//         println("intermediates:" + Await.result(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(testwettkampf.uuid.get)), Duration.Inf))
//      }
//      val allIntermediates = Await.result(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(testwettkampf.uuid.get)), Duration.Inf).asInstanceOf[ResponseMessage].data
//      val expectedIntermediates = durchgaenge.toSet
//      assert(expectedIntermediates.equals(allIntermediates))
//
//      println("submit new wertungen ...")
//      riegen.groupBy(gr => (gr.durchgang, gr.halt)).map{grd =>
//        val ((durchgang, halt), grs) = grd
//        val durchgangStartTime = System.currentTimeMillis()
//        val step = grs.flatMap{gr =>
//          gr.kandidaten.flatMap{ k =>
//            k.wertungen.map{ wertung =>
//              UpdateAthletWertung(
//                loadAthleteView(k.id),
//                k.wertungen.filter(w => w.id == wertung.id).map(_.toWertung.updatedWertung(wertung.copy(noteE = scala.math.BigDecimal(8.5)).toWertung)).head,
//                testwettkampf.uuid.get,
//                gr.durchgang.get,
//                wertung.wettkampfdisziplin.disziplin.id,
//                0, k.programm)
//            }
//            .map{command => (System.currentTimeMillis(), CompetitionCoordinatorClientActor.publish(command))}
//          }
//        }.toList
//        val results = step.foldLeft(Seq[(KutuAppEvent, Long, Long)]()){(acc, item) => acc :+ (Await.result(item._2,Duration.Inf), item._1, System.currentTimeMillis())}
//        val times = results.map(i => i._3 - i._2)
//        val durchgangTime = System.currentTimeMillis() - durchgangStartTime
//        val summary = s"durchg: $durchgang, halt: $halt, anz: ${results.size}, min: ${times.min}ms max: ${times.max}ms sum: ${times.sum}ms avg: ${times.sum / results.size}ms total: ${durchgangTime}"
//        println(summary)
//        summary
//      }
//
//      // cleanup
//      durchgaenge.foreach{d =>
//         println("finish durchgang :" + d)
//         Await.result(CompetitionCoordinatorClientActor.publish(FinishDurchgang(testwettkampf.uuid.get, d)), Duration.Inf)
//         println("intermediates:" + Await.result(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(testwettkampf.uuid.get)), Duration.Inf))
//      }
//    }
//
    "return scores for intermediate results (GET /scores)" in {
      println("start competition ...")
      CompetitionCoordinatorClientActor.createActorSinkSource("testcase", testwettkampf.uuid.get, None).to(Sink.foreach(msg => println(s"from Sink: $msg")))
  
      println("read all riegen ...")
      val riegen = RiegenBuilder.mapToGeraeteRiegen(getAllKandidatenWertungen(UUID.fromString(testwettkampf.uuid.get)).toList)
      val durchgaenge = riegen
        .filter(gr => gr.durchgang.nonEmpty) 
        .map(gr => gr.durchgang.get)
        .toSet.toList.sorted.reverse.take(2)
        
      // open first 2 durchgaenge
      durchgaenge.foreach{d =>
         println("starting durchgang :" + d)
         Await.result(CompetitionCoordinatorClientActor.publish(StartDurchgang(testwettkampf.uuid.get, d)), Duration.Inf)
         println("intermediates:" + Await.result(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(testwettkampf.uuid.get)), Duration.Inf))
      }
      println("submit new wertungen ...")
      riegen.filter(gr => durchgaenge.exists(gr.durchgang.contains(_))).groupBy(gr => (gr.durchgang, gr.halt)).map{grd =>
        val ((durchgang, halt), grs) = grd
        val durchgangStartTime = System.currentTimeMillis()
        val step = grs.flatMap{gr => 
          gr.kandidaten.flatMap{ k => 
            k.wertungen.map{ wertung => 
              UpdateAthletWertung(
                loadAthleteView(k.id), 
                k.wertungen.filter(w => w.id == wertung.id).map(_.toWertung.updatedWertung(wertung.copy(noteE = scala.math.BigDecimal(8.5)).toWertung)).head, 
                testwettkampf.uuid.get, 
                gr.durchgang.get, 
                wertung.wettkampfdisziplin.disziplin.id, 
                0, k.programm)
            }
            .map{command => (System.currentTimeMillis(), CompetitionCoordinatorClientActor.publish(command))}
          }
        }.toList
        
        step.foldLeft(Seq[(KutuAppEvent, Long, Long)]()){(acc, item) => acc :+ (Await.result(item._2,Duration.Inf), item._1, System.currentTimeMillis())}
      }
      durchgaenge.foreach{d =>
         println("finish durchgang :" + d)
         Await.result(CompetitionCoordinatorClientActor.publish(FinishDurchgang(testwettkampf.uuid.get, d)), Duration.Inf)
         println("intermediates:" + Await.result(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(testwettkampf.uuid.get)), Duration.Inf))
      }
  
      val request = HttpRequest(uri = "/api/scores/" + testwettkampf.uuid.get + "/intermediate")
  
//      request ~> allroutes(x => x) ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and TestGetuWK should not be in the list:
//        durchgaenge.take(2).foreach{d =>
//          assert(!entityAs[String].contains(d))
//        }
//      }
      durchgaenge.foreach{d =>
         println("start durchgang :" + d)
         Await.result(CompetitionCoordinatorClientActor.publish(StartDurchgang(testwettkampf.uuid.get, d)), Duration.Inf)
         println("intermediates:" + Await.result(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(testwettkampf.uuid.get)), Duration.Inf))
      }
//      request ~> allroutes(x => x) ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and open durchgaenge should be in the list:
//        durchgaenge.foreach{d =>
//          assert(entityAs[String].contains(d))
//        }
//      }
      durchgaenge.take(1).foreach{d =>
         println("finish durchgang :" + d)
         Await.result(CompetitionCoordinatorClientActor.publish(FinishDurchgang(testwettkampf.uuid.get, d)), Duration.Inf)
         println("intermediates:" + Await.result(CompetitionCoordinatorClientActor.publish(StartedDurchgaenge(testwettkampf.uuid.get)), Duration.Inf))
      }
      request ~> allroutes(x => x) ~> check {
        status should ===(StatusCodes.OK)
  
        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)
  
        // and finished durchgaenge should not be in the list:
        durchgaenge.take(1).foreach{d =>
          assert(!entityAs[String].contains(d))      
        }
        // and opened durchgaenge should be in the list:
        durchgaenge.drop(1).foreach{d =>
          assert(entityAs[String].contains(d))      
        }
      }
    }   
  }

}