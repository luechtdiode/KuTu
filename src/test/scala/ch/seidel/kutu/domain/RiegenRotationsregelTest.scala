package ch.seidel.kutu.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate
import java.util.UUID

class RiegenRotationsregelTest extends AnyWordSpec with Matchers {

  "Einfach 16+ / Default" in {
    val turnerinA: Kandidat = mockKandidat("Einfach", 2007, 2023, 11, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "K1                            -Aarau                         -0000-Turnerin                      -Almirez                       -Almaz                         ")
  }
  "Einfach 15 / Default" in {
    val turnerinA: Kandidat = mockKandidat("Einfach", 2008, 2023, 11, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "K1                            -Aarau                         -2008-Turnerin                      -Almirez                       -Almaz                         ")
  }
  "Einfach 16+ / AltInvers odd" in {
    val turnerinA: Kandidat = mockKandidat("Einfach/AltInvers", 2008, 2023, 11, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "1K                            -uaraA                         -2008-nirenruT                      -zerimlA                       -zamlA                         ")
  }
  "Einfach 16+ / AltInvers even" in {
    val turnerinA: Kandidat = mockKandidat("Einfach/AltInvers", 2004, 2023, 10, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "K1                            -Aarau                         -0000-Turnerin                      -Almirez                       -Almaz                         ")
  }
  "Rotierend only leads to defautl (Einfach)" in {
    val turnerinA: Kandidat = mockKandidat("Rotierend", 2007, 2023, 11, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "K1                            -Aarau                         -0000-Turnerin                      -Almirez                       -Almaz                         ")
  }
  "AltInvers only leads to defautl (Einfach)" in {
    val turnerinA: Kandidat = mockKandidat("AltInvers", 2007, 2023, 11, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "K1                            -Aarau                         -0000-Turnerin                      -Almirez                       -Almaz                         ")
  }
  "Einfach/Rotierend offset 0" in {
    val turnerinA: Kandidat = mockKandidat("Einfach/Rotierend", 2007, 2023, 4, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "K1                            -AARAU                         -0000-TURNERIN                      -ALMIREZ                       -ALMAZ                         ")
  }
  "Einfach/Rotierend offset 1" in {
    val turnerinA: Kandidat = mockKandidat("Einfach/Rotierend", 2007, 2023, 5, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "L1                            -BBSBV                         -0000-UVSOFSJO                      -BMNJSFA                       -BMNBA                         ")
  }
  "Einfach 15/Rotierend/AltInvers offset 1" in {
    val turnerinA: Kandidat = mockKandidat("Einfach/Rotierend/AltInvers", 2008, 2023, 5, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "1L                            -VBSBB                         -2008-OJSFOSVU                      -AFSJNMB                       -ABNMB                         ")
  }
  "Kategorie/Verein/AlterAufsteigend/Geschlecht/Name/Vorname/Rotierend/AltInvers offset 1" in {
    val turnerinA: Kandidat = mockKandidat("Kategorie/Verein/AlterAufsteigend/Geschlecht/Name/Vorname/Rotierend/AltInvers", 2008, 2023, 5, "TV Aarau", "Almirez", "Almaz", "K1", "Turnerin")
    assert(RiegenRotationsregel(turnerinA.wertungen.head.wettkampf).sort(turnerinA) ==
      "1L                            -VBSBB                         -8002-OJSFOSVU                      -AFSJNMB                       -ABNMB                         ")
  }

  private def mockKandidat(rotation: String, jahrgang: Int, wettkampfjahr: Int, wettkampfTag: Int, verein: String, name: String, vorname: String, kategorie: String, geschlecht: String) = {
    val wk = Wettkampf(1L, None, LocalDate.of(wettkampfjahr, 1, 1).plusDays(wettkampfTag) , "Testwettkampf", 44L, 0, BigDecimal(0d), "", None, None, None, Some(rotation))
    val a = Athlet(1L).copy(name = name, vorname = vorname, gebdat = Some(LocalDate.of(jahrgang, 3, 2))).toAthletView(Some(Verein(1L, verein, Some("Testverband"))))
    val wd = WettkampfdisziplinView(1, ProgrammView(44L, kategorie, 0, None, 1, 0, 100, UUID.randomUUID().toString, 1), Disziplin(1, "Boden"), "", None, StandardWettkampf(1.0), 1, 1, 0, 3, 1, 0, 30, 1)
    val wv = WertungView(wd.id, a, wd, wk, None, None, None, None, None)
    Kandidat("Testwettkampf", geschlecht, kategorie, 1, name, vorname, s"$jahrgang", verein, None, None, Seq(wd.disziplin), Seq.empty, Seq(wv))
  }
}
