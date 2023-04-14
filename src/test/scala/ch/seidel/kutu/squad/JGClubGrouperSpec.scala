package ch.seidel.kutu.squad

import ch.seidel.kutu.data.ByAltersklasse
import ch.seidel.kutu.domain.Altersklasse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JGClubGrouperSpec extends AnyWordSpec with Matchers {

  "extract Altersklasse" in {
    val altersklasse = ByAltersklasse("AK", Altersklasse.parseGrenzen("AK6-10,AK11-20/2,AK25-100/10"))
    println(altersklasse.grenzen)
    assert(altersklasse.grenzen === List(("AK", List(), 6), ("AK", List(), 7), ("AK", List(), 8), ("AK", List(), 9), ("AK", List(), 10),
      ("AK", List(), 11), ("AK", List(), 13), ("AK", List(), 15), ("AK", List(), 17), ("AK", List(), 19), ("AK", List(), 25),
      ("AK", List(), 35), ("AK", List(), 45), ("AK", List(), 55), ("AK", List(), 65), ("AK", List(), 75), ("AK", List(), 85), ("AK", List(), 95))
    )
  }
  "extract complex Altersklasse" in {
    val altersklasse = ByAltersklasse("AK", Altersklasse.parseGrenzen("AK(W+BS)8-17,AK(M+BS)8-18/2,AK(OS)14-18/2"))
    println(altersklasse.grenzen)
    assert(altersklasse.grenzen === List(
      ("AK",List("W", "BS"),8), ("AK",List("M", "BS"),8),
      ("AK",List("W", "BS"),9),
      ("AK",List("W", "BS"),10), ("AK",List("M", "BS"),10),
      ("AK",List("W", "BS"),11), ("AK",List("W", "BS"),12),

      ("AK",List("M", "BS"),12),
      ("AK",List("W", "BS"),13),
      ("AK",List("W", "BS"),14), ("AK",List("M", "BS"),14), ("AK",List("OS"),14),

      ("AK",List("W", "BS"),15), ("AK",List("W", "BS"),16), ("AK",List("M", "BS"),16), ("AK",List("OS"),16),

      ("AK",List("W", "BS"),17),
      ("AK",List("M", "BS"),18), ("AK",List("OS"),18))
    )
  }

}
