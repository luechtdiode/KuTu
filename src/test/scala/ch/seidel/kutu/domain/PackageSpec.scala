package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.calc.ScoreCalcTemplateView

import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode
import ch.seidel.kutu.domain.given_Conversion_Double_String
import ch.seidel.kutu.domain.given_Conversion_String_BigDecimal
import ch.seidel.kutu.domain.given_Conversion_String_Double
import ch.seidel.kutu.domain.given_Conversion_String_Int
import ch.seidel.kutu.domain.given_Conversion_String_Long
import ch.seidel.kutu.domain.given_Conversion_LocalDate_Date
import ch.seidel.kutu.domain.given_Conversion_Date_LocalDate

import java.sql.Date

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

  "WertungView.getTeamname" should {
    val teamrule = ""
    val programm = ProgrammView(1L, "Testprogramm", 0, None, 1, 0, 99, "", 1, 0)
    val wettkampf = WettkampfView(1L, None, LocalDate.now(), "Testwettkampf", programm, 0, BigDecimal(0), "", "", "", "", "", teamrule)
    val disziplin = Disziplin(1L, "Testdisziplin")
    val notenSpez = StandardWettkampf(1d)
    val wettkampfdisziplin = WettkampfdisziplinView(1L, programm, disziplin, "", None, notenSpez, 1, 1, 1, 1, 0, 0, 0, 1)
    val verein = Verein(1, "Testverein", Some("Testverband"))
    val athlet = AthletView(1, 0, "M", "Mustermann", "Max", Some(LocalDate.of(2000,1,1)), "", "", "", Some(verein), activ = true)

    "respond verein-name, if teamnumber is 0" in {
      val riege = None
      val team = 0
      val wv = WertungView(1L, athlet, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, riege, None, team, None, None)

      assert(wv.teamName == "Testverein")
    }
    "respond team-name with vereinsname, if teamnumber is > 0" in {
      val riege = None
      val team = 3
      val wv = WertungView(1L, athlet, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, riege, None, team, None, None)

      assert(wv.teamName == "Testverein 3")
    }
    "respond not riege-name, if riege is defined" in {
      val riege = Some("Riegenname")
      val team = 3
      val wv = WertungView(1L, athlet, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, riege, None, team, None, None)

      assert(wv.teamName == "Testverein 3")
    }
    "respond not team-name, if teamnumber is 0 and riege is defined" in {
      val riege = Some("Riegenname")
      val team = 0
      val wv = WertungView(1L, athlet, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, riege, None, team, None, None)

      assert(wv.teamName == "Testverein")
    }
    "respond teamnumber, if no verein is defined" in {
      val team = 3
      val athletNoVerein = athlet.copy(verein = None)
      val wv = WertungView(1L, athletNoVerein, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, team, None, None)

      assert(wv.teamName == "3")
    }
    "respond empty-string, if no verein is defined and teamnumber is 0" in {
      val team = 0
      val athletNoVerein = athlet.copy(verein = None)
      val wv = WertungView(1L, athletNoVerein, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, None, None, team, None, None)

      assert(wv.teamName == "")
    }
    "respond not riege-name, if no verein is defined and riege is defined" in {
      val riege = Some("Riegenname")
      val team = 3
      val athletNoVerein = athlet.copy(verein = None)
      val wv = WertungView(1L, athletNoVerein, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, riege, None, team, None, None)

      assert(wv.teamName == "3")
    }
    "respond not teamnumber, if no verein is defined, teamnumber is 0 and riege is defined" in {
      val riege = Some("Riegenname")
      val team = 0
      val athletNoVerein = athlet.copy(verein = None)
      val wv = WertungView(1L, athletNoVerein, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, riege, None, team, None, None)

      assert(wv.teamName == "")
    }
    "respond empty-string, if no verein is defined, teamnumber is 0 and riege is empty" in {
      val riege = Some("")
      val team = 0
      val athletNoVerein = athlet.copy(verein = None)
      val wv = WertungView(1L, athletNoVerein, wettkampfdisziplin, wettkampf.toWettkampf, None, None, None, riege, None, team, None, None)

      assert(wv.teamName == "")
    }
    "respond effectiv verein-teamname, if verein and teamrule is defined" in {
      val team = 2
      val wettkampfWithTeamrule = wettkampf.copy(teamrule = "VereinGerät(3/4)")
      val wv = WertungView(1L, athlet, wettkampfdisziplin, wettkampfWithTeamrule.toWettkampf, None, None, None, None, None, team, None, None)

      assert(wv.teamName == "Testverein 2")
    }
    "respond virtual teamname if teamnumber is < 0 and teamrule is defined" in {
      val team = -1
      val wettkampfWithTeamrule = wettkampf.copy(teamrule = "VereinGerät(3/4/extrateam1+extrateam2)")
      val wv = WertungView(1L, athlet, wettkampfdisziplin, wettkampfWithTeamrule.toWettkampf, None, None, None, None, None, team, None, None)

      assert(wv.teamName == "extrateam1")
    }
    "respond verband teamname teamrule is defined, but without 'Verein' Context" in {
      val team = 2
      val wettkampfWithTeamrule = wettkampf.copy(teamrule = "VerbandGerät(3/4/extrateam1+extrateam2)")
      val wv = WertungView(1L, athlet, wettkampfdisziplin, wettkampfWithTeamrule.toWettkampf, None, None, None, None, None, team, None, None)

      assert(wv.teamName == "Testverband 2")
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
        Some(testverein), activ = true)), None, Some(MediaAdmin("1", "life-is-life.mp3", "mp3", 0, "", "", 0)))

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
      val ar = AthletRegistration(0, 1, None, "W", " SANER ", " claudia", "2007-10-20", 1L, 1L, None, None, Some(MediaAdmin("1", "life-is-life.mp3", "mp3", 0, "", "", 0)))

      assert(ar.matchesAthlet(AthletView(2, 0, "W", "Saner", "Claudia", Some(str2SQLDate("2007-10-20")), "", "", "", Some(
        Verein(1, "Verein", Some("Verband"))), true).toAthlet).==(true))
    }
    "initial determine sex from name and surname" in {
      val ar = AthletRegistration(0, 1, None, "M", "Meier", "Claudia", "2007-10-20", 1L, 1L, None, None, Some(MediaAdmin("1", "life-is-life.mp3", "mp3", 0, "", "", 0)))

      assert(ar.matchesAthlet(AthletView(2, 0, "W", "Meier", "Claudia", Some(str2SQLDate("2007-10-20")), "", "", "", Some(
        Verein(1, "Verein", Some("Verband"))), true).toAthlet).==(true))
    }
    "initial switch name/vorname if its indicated in the surname-index" in {
      val ar = AthletRegistration(0, 1, None, "M", "Claudia", "Meier", "2007-10-20", 1L, 1L, None, None, Some(MediaAdmin("1", "life-is-life.mp3", "mp3", 0, "", "", 0)))

      assert(ar.matchesAthlet(AthletView(2, 0, "W", "Meier", "Claudia", Some(str2SQLDate("2007-10-20")), "", "", "", Some(
        Verein(1, "Verein", Some("Verband"))), true).toAthlet).==(true))
    }
    "pass all attributes just trimmed if its just an update" in {
      val ar = AthletRegistration(1, 1, Some(2), "W", "Saner ", " Waiata ", "2007-10-20", 1L, 1L, Some(
        AthletView(2, 0, "W", "Saner", "Waiata", Some(str2SQLDate("2007-10-20")), "", "", "", Some(
          Verein(1, "Verein", Some("Verband"))), true)), None, Some(MediaAdmin("1", "life-is-life.mp3", "mp3", 0, "", "", 0)))
      assert(ar.matchesAthlet().==(true))
    }
  }
  "TeamAggreateFun avg" in {
    val results1 = List(
      Resultat(0, 7.00, 0.08),
      Resultat(1, 8.00, 0.12),
      Resultat(2, 8.50, 0.06),
      Resultat(3, 9.50, 0.23)
    )
    val results2 = List(
      Resultat(1, 8.00, 0.10),
      Resultat(1, 8.20, 0.12),
      Resultat(2, 8.50, 0.15),
      Resultat(2, 9.10, 0.20)
    )
    assert(TeamAggreateFun("avg/")(results1).endnote.<(TeamAggreateFun("avg/")(results2).endnote))
    assert(TeamAggreateFun("avg/")(results1).==(Resultat(1.5, 8.25, 0.1225)))
    assert(TeamAggreateFun("avg/")(results2).==(Resultat(1.5, 8.45, 0.1425)))
  }
  "TeamAggreateFun median even" in {
    val results1 = List(
      Resultat(0, 7.30, 0.06),
      Resultat(1, 8.00, 0.08),
      Resultat(2, 8.50, 0.12),
      Resultat(3, 9.50, 0.23)
    )
    assert(TeamAggreateFun("median/")(results1).==(Resultat(1.5, 8.25, 0.10)))
  }
  "TeamAggreateFun median odd" in {
    val results1 = List(
      Resultat(0, 7.00, 0.02),
      Resultat(0, 7.30, 0.06),
      Resultat(1, 8.00, 0.08),
      Resultat(2, 8.50, 0.12),
      Resultat(3, 9.50, 0.23)
    )
    assert(TeamAggreateFun("median/")(results1).==(Resultat(1, 8.00, 0.08)))
  }
  "TeamAggreateFun min" in {
    val results1 = List(
      Resultat(0, 7.00, 0.08),
      Resultat(1, 8.00, 0.12),
      Resultat(2, 8.50, 0.06),
      Resultat(3, 9.50, 0.23)
    )
    val results2 = List(
      Resultat(1, 8.00, 0.10),
      Resultat(1, 8.20, 0.12),
      Resultat(2, 8.50, 0.15),
      Resultat(2, 9.10, 0.20)
    )
    assert(TeamAggreateFun("min/")(results1).endnote.<(TeamAggreateFun("min/")(results2).endnote))
    assert(TeamAggreateFun("min/")(results1).==(Resultat(0, 7.00, 0.06)))
    assert(TeamAggreateFun("min/")(results2).==(Resultat(1, 8.00, 0.10)))
  }
  "TeamAggreateFun max" in {
    val results1 = List(
      Resultat(0, 7.00, 0.08),
      Resultat(1, 8.00, 0.12),
      Resultat(2, 8.50, 0.06),
      Resultat(3, 9.50, 0.23)
    )
    val results2 = List(
      Resultat(1, 8.00, 0.10),
      Resultat(1, 8.20, 0.12),
      Resultat(2, 8.50, 0.15),
      Resultat(2, 9.10, 0.20)
    )
    assert(TeamAggreateFun("max/")(results1).endnote.>(TeamAggreateFun("max/")(results2).endnote))
    assert(TeamAggreateFun("max/")(results1).==(Resultat(3, 9.50, 0.23)))
    assert(TeamAggreateFun("max/")(results2).==(Resultat(2, 9.10, 0.20)))
  }
  "TeamAggreateFun devmin" in {
    val results1 = List(
      Resultat(0, 7.00, 0.08),
      Resultat(1, 8.00, 0.12),
      Resultat(2, 8.50, 0.06),
      Resultat(3, 9.50, 0.23)
    )
    val results2 = List(
      Resultat(1, 8.00, 0.10),
      Resultat(1, 8.20, 0.12),
      Resultat(2, 8.50, 0.15),
      Resultat(2, 9.10, 0.20)
    )
    assert(TeamAggreateFun("devmin/")(results1).endnote.>(TeamAggreateFun("devmin/")(results2).endnote))
    assert(TeamAggreateFun("devmin/")(results1).==(Resultat(1.12, 0.9014, 0.06571720)))
    assert(TeamAggreateFun("devmin/")(results2).==(Resultat(0.50, 0.4153, 0.03766630)))
  }
  "TeamAggreateFun devmin2" in {
    val results1 = List(
      Resultat(0, 7.00, 0.10),
      Resultat(1, 8.00, 0.22),
      Resultat(2, 8.50, 0.21),
      Resultat(3, 9.50, 0.07)
    )
    val results2 = List(
      Resultat(1, 8.00, 0.47),
      Resultat(1, 8.25, 0.42),
      Resultat(2, 8.10, 0.31),
      Resultat(2, 7.90, 0.26),
      Resultat(2, 7.80, 0.76),
      Resultat(2, 8.30, 0.90),
      Resultat(1, 8.10, 0.47),
      Resultat(1, 8.60, 0.42),
      Resultat(2, 8.45, 0.31),
      Resultat(2, 8.10, 0.26),
      Resultat(2, 8.10, 0.76),
      Resultat(2, 8.25, 0.90)
    )
    assert(TeamAggreateFun("devmin/")(results1).==(Resultat(1.12, 0.9014, 0.06595)))
    assert(TeamAggreateFun("devmin/")(results2).noteE.setScale(2, RoundingMode.HALF_DOWN).==(Resultat(0, 0.21, 0).noteE))
  }
}