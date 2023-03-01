package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec

import java.time.LocalDate

class PackageSpec extends KuTuBaseSpec {
  "GeTuWettkampf" should {
    val wdg = WettkampfdisziplinView(1, null, null, null, None, null, 1, 1, 1, 2, 0, 0, 10, 1)
    "min" in {
      assert(StandardWettkampf(1d).calcEndnote(0d, -1d, wdg) == 0.00d)
    }
    "max" in {
      assert(StandardWettkampf(1d).calcEndnote(0d, 10.01d, wdg) == 10.00d)
    }
    "scale" in {
      assert(StandardWettkampf(1d).calcEndnote(0d, 8.123d, wdg) == 8.12d)
    }
  }
  "KuTuWettkampf" should {
    val wdk = WettkampfdisziplinView(1, null, null, null, None, null, 1, 1, 1, 3, 1, 0, 30, 1)
    "min" in {
      assert(StandardWettkampf(1d).calcEndnote(0.1d, -1d, wdk) == 0.000d)
    }
    "max" in {
      assert(StandardWettkampf(1d).calcEndnote(0.5d, 30.01d, wdk) == 30.000d)
    }
    "scale" in {
      assert(StandardWettkampf(1d).calcEndnote(1.1d, 8.1234d, wdk) == 9.223d)
    }
  }
  "toDurationFormat" should {
     //Duration(1, TimeUnit.DAYS) + Duration(15, TimeUnit.HOURS)
    "seconds" in {
      val millis = 3 * 1000
      assert("3s" == toDurationFormat(1L, 1L + millis))
    }
    "muinutes and seconds" in {
      val millis = 3 * 1000 + 59 * 60 * 1000
      assert("59m, 3s" == toDurationFormat(1L, 1L + millis))
    }
    "hours, minutes and seconds" in {
      val millis = 3 * 1000 + 59 * 60 * 1000 + 23 * 3600 * 1000
      assert("23h, 59m, 3s" == toDurationFormat(1L, 1L + millis))
    }
    "days, hours, minutes and seconds" in {
      val millis = 3 * 1000 + 59 * 60 * 1000 + 23 * 3600 * 1000  +  1 * 24 * 3600 * 1000
      assert("1d, 23h, 59m, 3s" == toDurationFormat(1L, 1L + millis))
    }
  }
  
  "encapsulated titles" should {
    "match" in {
      val titles = Seq(
          "D1, K1-4 Tu & Ti"
          , "D1.TuTi"
          )
      titles.foreach(t => assert(t.matches(".*[\\s,\\.;!].*")))
      //assert("KH.KD".matches(".*[\\s,\\.;!].*"))
    }
  }

  "toPublicView" should {
    val testverein = Verein(1, "Testverein", Some("Testverband"))
    val vereinRegistration = Registration(
      1, 1L, Some(1L),
      "Testverein", "Testverband", "Max", "Muster", "0791234567", "a@b.com", 2L,
      Some(testverein))

    val athletRegistration = AthletRegistration(1, 1, Some(2), "W", "Saner ", " Waiata ", "2007-10-20", 1L, 1L, Some(
      AthletView(2, 0, "W", "Saner", "Waiata", Some(str2SQLDate("2007-10-20")), "", "", "",
        Some(testverein), activ = true)))

    val athletView = athletRegistration.athlet.get
    val athlet = athletView.toAthlet

    "clear sensitiv vereinRegistration data" in {
      assert(vereinRegistration.toPublicView.mail ==("***"))
      assert(vereinRegistration.toPublicView.mobilephone ==("***"))
    }
    "clear sensitiv athlet data" in {
      assert(athlet.toPublicView.gebdat.map(_.toLocalDate.getDayOfMonth) ==(Some(1)))
      assert(athlet.toPublicView.gebdat.map(_.toLocalDate.getMonthValue) ==(Some(1)))
    }
    "clear sensitiv athletView data" in {
      assert(athletView.toPublicView.gebdat.map(_.toLocalDate.getDayOfMonth) ==(Some(1)))
      assert(athletView.toPublicView.gebdat.map(_.toLocalDate.getMonthValue) ==(Some(1)))
    }
    "clear sensitiv athletRegistration data" in {
      val gebdat = sqlDate2ld(str2SQLDate(athletRegistration.toPublicView.gebdat))
      assert(gebdat.getDayOfMonth ==(1))
      assert(gebdat.getMonthValue ==(1))
    }
  }
  "toAthlet" should {
    "initial trim and capitalize name and surname" in {
      val ar = AthletRegistration(0, 1, None, "W", " SANER ", " claudia", "2007-10-20", 1L, 1L, None)

      assert(ar.matchesAthlet(AthletView(2, 0, "W", "Saner", "Claudia", Some(str2SQLDate("2007-10-20")), "", "", "", Some(
        Verein(1, "Verein", Some("Verband"))), true).toAthlet).==(true))
    }
    "initial determine sex from name and surname" in {
      val ar = AthletRegistration(0, 1, None, "M", "Meier", "Claudia", "2007-10-20", 1L, 1L, None)

      assert(ar.matchesAthlet(AthletView(2, 0, "W", "Meier", "Claudia", Some(str2SQLDate("2007-10-20")), "", "", "", Some(
        Verein(1, "Verein", Some("Verband"))), true).toAthlet).==(true))
    }
    "initial switch name/vorname if its indicated in the surname-index" in {
      val ar = AthletRegistration(0, 1, None, "M", "Claudia", "Meier", "2007-10-20", 1L, 1L, None)

      assert(ar.matchesAthlet(AthletView(2, 0, "W", "Meier", "Claudia", Some(str2SQLDate("2007-10-20")), "", "", "", Some(
        Verein(1, "Verein", Some("Verband"))), true).toAthlet).==(true))
    }
    "pass all attributes just trimmed if its just an update" in {
      val ar = AthletRegistration(1, 1, Some(2), "W", "Saner ", " Waiata ", "2007-10-20", 1L, 1L, Some(
        AthletView(2, 0, "W", "Saner", "Waiata", Some(str2SQLDate("2007-10-20")), "", "", "", Some(
          Verein(1, "Verein", Some("Verband"))), true)))
      assert(ar.matchesAthlet().==(true))
    }
  }
}