package ch.seidel.kutu.squad

import ch.seidel.kutu.data.ByAltersklasse
import ch.seidel.kutu.domain.Altersklasse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JGClubGrouperSpec extends AnyWordSpec with Matchers {

    "extract Altersklasse" in {
      val altersklasse = ByAltersklasse("AK", Altersklasse.parseGrenzen("AK6-10,AK11-20/2,AK25-100/10"))
      println(altersklasse.grenzen)
      assert(altersklasse.grenzen === List(("AK",6), ("AK",7), ("AK",8), ("AK",9), ("AK",10),
        ("AK",11), ("AK",13), ("AK",15), ("AK",17), ("AK",19),
        ("AK",25), ("AK",35), ("AK",45), ("AK",55), ("AK",65), ("AK",75), ("AK",85), ("AK",95)))
  }

}
