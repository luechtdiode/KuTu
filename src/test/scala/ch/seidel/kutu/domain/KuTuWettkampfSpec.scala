package ch.seidel.kutu.domain

import java.util.Date
import org.scalatest.WordSpec
import org.scalatest.Matchers

//@RunWith(classOf[JUnitRunner])
class KuTuWettkampfSpec extends WordSpec with Matchers with KutuService {
  print(f"${new Date()}%tF")
//  print(suggestRiegen(7, Seq(6)))
  // Open a database connection

//  database withSession { implicit session =>
//    println(createWettkampf("15.02.2015", "Jugendcup 2015", Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter)))
//    println(createWettkampf("16.01.2015", "Athletiktest NKL Frühling 2015", Set(2,3)))
////    assignAthletsToWettkampf(27L, Set(programmEP, programmEP+1, programmEP+2), Some(mapFilter))
////    assignAthletsToWettkampf(24L, Set(9,10))
//
//    def insertNotenSkala(disziplinid: Long, skala: Map[String, Double]) {
//      for(beschr <- skala.keys.toList.sortBy{skala}.reverse) {
//        sqlu"""
//          INSERT INTO kutu.notenskala
//          (wettkampfdisziplin_id, kurzbeschreibung, punktwert)
//          VALUES(${disziplinid}, ${beschr}, ${skala(beschr)})
//        """.execute
//      }
//    }
//  }
}