package ch.seidel.kutu.domain

import java.time.LocalDate
import java.util.UUID
import ch.seidel.kutu.base.{KuTuBaseSpec, TestDBService}

import scala.annotation.tailrec
import scala.util.matching.Regex

class WettkampfSpec extends KuTuBaseSpec {
  "wettkampf" should {
    "create with disziplin-plan-times" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel", Set(20), "testmail@test.com", 33, 0, None, "7,8,9,11,13,15,17,19", "7,8,9,11,13,15,17,19")
      assert(wettkampf.id > 0L)
      val views = initWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(views.nonEmpty)
    }

    "update" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel2", Set(20), "testmail@test.com", 33, 0, None, "", "")
      val wettkampfsaved = saveWettkampf(wettkampf.id, wettkampf.datum, "neuer titel", Set(wettkampf.programmId), "testmail@test.com", 10000, 7.5, wettkampf.uuid, "7,8,9,11,13,15,17,19", "7,8,9,11,13,15,17,19")
      assert(wettkampfsaved.titel == "neuer titel")
      assert(wettkampfsaved.auszeichnung == 10000)
      assert(wettkampfsaved.altersklassen == "7,8,9,11,13,15,17,19")
      assert(wettkampfsaved.jahrgangsklassen == "7,8,9,11,13,15,17,19")
    }

    "recreate with disziplin-plan-times" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel2", Set(20), "testmail@test.com", 33, 0, None, "", "")
      assert(wettkampf.id > 0L)
      val views = initWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(views.nonEmpty)
      val wettkampf2 = createWettkampf(LocalDate.now(), "titel2", Set(20), "testmail@test.com", 33, 0, wettkampf.uuid, "", "")
      assert(wettkampf2.id == wettkampf.id)
      val views2 = initWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(views2.size == views.size)
    }

    "update disziplin-plan-time" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel2", Set(20), "testmail@test.com", 33, 0, None, "", "")
      assert(wettkampf.id > 0L)
      val views = initWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))

      updateWettkampfPlanTimeView(views(0).toWettkampfPlanTimeRaw.copy(einturnen = 20000))
      val reloaded = loadWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(reloaded(0).einturnen == 20000L)
    }

    "delete all disziplin-plan-time entries when wk is deleted" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel3", Set(20), "testmail@test.com", 33, 0, None, "", "")
      deleteWettkampf(wettkampf.id)
      val reloaded = loadWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(reloaded.isEmpty)
    }

    "create WK Modus with programs and disciplines" in {
      /*
      WettkampfdisziplinView(154,Testprogramm / AK1(von=5 bis=10) (von=5, bis=10),Disziplin(1,Boden),,None,StandardWettkampf(1.0),1,1,0,3,1,0,30,1)
      WettkampfdisziplinView(155,Testprogramm / AK1(von=5 bis=10) (von=5, bis=10),Disziplin(4,Sprung),,None,StandardWettkampf(1.0),1,1,1,3,1,0,30,1)
      WettkampfdisziplinView(156,Testprogramm / AK2(von=11 bis=20) (von=11, bis=20),Disziplin(1,Boden),,None,StandardWettkampf(1.0),1,1,2,3,1,0,30,1)
      WettkampfdisziplinView(157,Testprogramm / AK2(von=11 bis=20) (von=11, bis=20),Disziplin(4,Sprung),,None,StandardWettkampf(1.0),1,1,3,3,1,0,30,1)
      WettkampfdisziplinView(158,Testprogramm / AK3(von=21 bis=50) (von=21, bis=50),Disziplin(1,Boden),,None,StandardWettkampf(1.0),1,1,4,3,1,0,30,1)
      WettkampfdisziplinView(159,Testprogramm / AK3(von=21 bis=50) (von=21, bis=50),Disziplin(4,Sprung),,None,StandardWettkampf(1.0),1,1,5,3,1,0,30,1)
      WettkampfdisziplinView(160,Testprogramm2 / LK1 (von=0, bis=100),Disziplin(30,Ringe),,None,StandardWettkampf(1.0),1,1,0,3,1,0,30,1)
      WettkampfdisziplinView(161,Testprogramm2 / LK1 (von=0, bis=100),Disziplin(5,Barren),,None,StandardWettkampf(1.0),1,1,1,3,1,0,30,1)
      WettkampfdisziplinView(162,Testprogramm2 / LK2 (von=0, bis=100),Disziplin(30,Ringe),,None,StandardWettkampf(1.0),1,1,2,3,1,0,30,1)
      WettkampfdisziplinView(163,Testprogramm2 / LK2 (von=0, bis=100),Disziplin(5,Barren),,None,StandardWettkampf(1.0),1,1,3,3,1,0,30,1)
      WettkampfdisziplinView(164,Testprogramm2 / LK3 (von=0, bis=100),Disziplin(30,Ringe),,None,StandardWettkampf(1.0),1,1,4,3,1,0,30,1)
      WettkampfdisziplinView(165,Testprogramm2 / LK3 (von=0, bis=100),Disziplin(5,Barren),,None,StandardWettkampf(1.0),1,1,5,3,1,0,30,1)
      */
      val tp1 = insertWettkampfProgram("Testprogramm1", 2, 10, 1,
        List("Boden", "Sprung"),
        List("AK1(von=5 bis=10)", "AK2(von=11 bis=20)", "AK3(von=21 bis=50)")
      )
      println(tp1.mkString("\n"))
      assert(tp1.size == 6)
      assert(tp1(2).programm.riegenmode == 2)
      assert(tp1(2).programm.parent.get.riegenmode == 2)
      assert(tp1(2).programm.name == "AK2")
      assert(tp1(2).programm.alterVon == 11)
      assert(tp1(2).programm.alterBis == 20)

      val tp2 = insertWettkampfProgram("Testprogramm2", 1, 10, 0,
        List("Barren", "Ringe"),
        List("LK1", "LK2", "LK3")
      )
      println(tp2.mkString("\n"))
      assert(tp2.size == 6)
      assert(tp2(1).programm.riegenmode == 1)
      assert(tp2(1).programm.parent.get.riegenmode == 1)
      assert(tp2(1).programm.name == "LK1")
      assert(tp2(1).programm.alterVon == 0)
      assert(tp2(1).programm.alterBis == 100)
    }

    "create KuTu TG Allgäu Kür & Pflicht" in {
      val tgam = insertWettkampfProgram(s"KuTu TG Allgäu Kür & Pflicht-Test", 1, 30, 1,
        List("Boden(sex=m)", "Pferd Pauschen(sex=m)", "Ring(sex=m)", "Sprung(sex=m)", "Barren(sex=m)", "Reck(sex=m)"),
        List(
          "Kür/WK I Kür"
          , "Kür/WK II LK1"
          , "Kür/WK III LK1(von=16 bis=17)"
          , "Kür/WK IV LK2(von=14 bis=15)"
          , "Pflicht/WK V Jug(von=14 bis=18)"
          , "Pflicht/WK VI Schüler A(von=12 bis=13)"
          , "Pflicht/WK VII Schüler B(von=10 bis=11)"
          , "Pflicht/WK VIII Schüler C(von=8 bis=9)"
          , "Pflicht/WK IX Schüler D(von=0 bis=7)"
        )
      )
      printNewWettkampfModeInsertStatements(tgam)
    }
    "create KuTuRi TG Allgäu Kür & Pflicht-" in {
      val tgaw = insertWettkampfProgram(s"KuTuRi TG Allgäu Kür & Pflicht-Test", 1, 30, 1,
        List("Sprung(sex=w)", "Stufenbarren(sex=w)", "Balken(sex=w)", "Boden(sex=w)"),
        List(
          "Kür/WK I Kür"
          , "Kür/WK II LK1"
          , "Kür/WK III LK1(von=16 bis=17)"
          , "Kür/WK IV LK2(von=14 bis=15)"
          , "Pflicht/WK V Jug(von=14 bis=18)"
          , "Pflicht/WK VI Schüler A(von=12 bis=13)"
          , "Pflicht/WK VII Schüler B(von=10 bis=11)"
          , "Pflicht/WK VIII Schüler C(von=8 bis=9)"
          , "Pflicht/WK IX Schüler D(von=0 bis=7)"
        )
      )
      printNewWettkampfModeInsertStatements(tgaw)
    }
    "create Turn10" in {
      val turn10v = insertWettkampfProgram(s"Turn10-Verein-Test", 3, 20, 1,
        // Ti: Boden, Balken, Minitramp, Reck/Stufenbarren, Sprung.
        // Tu: Boden, Barren, Minitramp, Reck, Sprung, Pferd, Ringe
        List("Boden", "Barren(sex=m)", "Balken(sex=w)", "Minitramp", "Reck", "Stufenbarren(sex=w)", "Sprung", "Pferd Pauschen(sex=m)", "Ringe(sex=m)"),
        List(
            "BS"
          , "OS"
        )
      )
      printNewWettkampfModeInsertStatements(turn10v)
      val turn10s = insertWettkampfProgram(s"Turn10-Schule-Test", 2, 20, 1,
        // Ti: Boden, Balken, Reck, Sprung.
        // Tu: Boden, Barren, Reck, Sprung.
        List("Boden", "Barren(sex=m)", "Balken(sex=w)", "Reck", "Sprung"),
        List(
            "BS"
          , "OS"
        )
      )
      printNewWettkampfModeInsertStatements(turn10s)
    }
    "create GeTu BLTV" in {
      val btv = insertWettkampfProgram(s"GeTu BLTV-Test", 1, 10,0,
        List("Reck", "Boden", "Schaukelringe", "Sprung", "Barren(sex=m start=0)"),
        List(
          "K1(bis=10)"
          , "K2(bis=12)"
          , "K3(bis=14)"
          , "K4(bis=16)"
          , "K5"
          , "K6"
          , "K7"
          , "KD(von=22)"
          , "KH(von=28)"
        )
      )
      printNewWettkampfModeInsertStatements(btv)
    }

  }

  private def printNewWettkampfModeInsertStatements(tga: List[WettkampfdisziplinView]) = {
    println(s"-- ${tga.head.programm.head.name}")
    val inserts = tga
      .map(wdp => wdp.disziplin)
      .distinct
      .map(d => s"insert into disziplin (id, name) values (${d.id}, '${d.name}') on conflict (id) do nothing;") :::
      ("insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values " +: tga
        .flatMap{wdp =>
          def flatten(pgm: ProgrammView): List[ProgrammView] = {
            if (pgm.parent.isEmpty) {
              List(pgm)
            } else {
              pgm +: flatten(pgm.parent.get)
            }
          }
          flatten(wdp.programm)
        }
        .distinct.sortBy(_.id)
        .map(p => s"(${p.id}, '${p.name}', ${p.aggregate}, ${p.parent.map(_.id.toString).getOrElse("null")}, ${p.ord}, ${p.alterVon}, ${p.alterBis}, '${p.uuid}', ${p.riegenmode})")
        .mkString("", "\n,", " on conflict(id) do update set name=excluded.name, aggregate=excluded.aggregate, parent_id=excluded.parent_id, ord=excluded.ord, alter_von=excluded.alter_von, alter_bis=excluded.alter_bis, riegenmode=excluded.riegenmode;").split("\n").toList) :::
      ("insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values " +: tga
        .map(wdp => s"(${wdp.id}, ${wdp.programm.id}, ${wdp.disziplin.id}, '${wdp.kurzbeschreibung}', ${wdp.detailbeschreibung.map(b => s"'$b'").getOrElse("''")}, ${wdp.notenSpez.calcEndnote(0.000d, 1.000d, wdp)}, ${wdp.masculin}, ${wdp.feminim}, ${wdp.ord}, ${wdp.scale}, ${wdp.dnote}, ${wdp.min}, ${wdp.max}, ${wdp.startgeraet})")
        .mkString("", "\n,", " on conflict(id) do update set masculin=excluded.masculin, feminim=excluded.feminim, ord=excluded.ord, dnote=excluded.dnote, min=excluded.min, max=excluded.max, startgeraet=excluded.startgeraet;").split("\n").toList)

    println(inserts.mkString("\n"))
  }
}