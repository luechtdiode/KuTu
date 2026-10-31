package ch.seidel.kutu.squad

import ch.seidel.kutu.data.ByAltersklasse
import ch.seidel.kutu.domain.Altersklasse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JGClubGrouperSpec extends AnyWordSpec with Matchers {

  "extract Altersklasse" in {
    val altersklasse = ByAltersklasse("AK", Altersklasse.parseGrenzen("AK6-10,AK11-20/2,AK25-100/10"))
    println(altersklasse.grenzen)
    assert(altersklasse.grenzen === Seq(
      ("AK", Seq(), 6),
      ("AK", Seq(), 11), ("AK", Seq(), 13), ("AK", Seq(), 15), ("AK", Seq(), 17), ("AK", Seq(), 19),
      ("AK", Seq(), 25), ("AK", Seq(), 35), ("AK", Seq(), 45), ("AK", Seq(), 55), ("AK", Seq(), 65), ("AK", Seq(), 75), ("AK", Seq(), 85), ("AK", Seq(), 95))
    )
  }
  "extract complex Altersklasse" in {
    val altersklasse = ByAltersklasse("AK", Altersklasse.parseGrenzen("AK(W+BS)8-17,AK(M+BS)8-18/2,AK(OS)14-18/2"))
    println(altersklasse.grenzen)
    assert(altersklasse.grenzen === Seq(
      ("AK",Seq("W", "BS"),8),  ("AK",Seq("M", "BS"),8),
                                ("AK",Seq("M", "BS"),10),
                                ("AK",Seq("M", "BS"),12),
                                ("AK",Seq("M", "BS"),14),  ("AK",Seq("OS"),14),
                                ("AK",Seq("M", "BS"),16),  ("AK",Seq("OS"),16),
      ("AK",Seq("W", "BS"),18), ("AK",Seq("M", "BS"),18),  ("AK",Seq("OS"),18))
    )
  }
}
