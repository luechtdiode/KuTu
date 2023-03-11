package ch.seidel.kutu.domain

import java.time.LocalDate
import java.util.UUID

import ch.seidel.kutu.base.KuTuBaseSpec

class WettkampfSpec extends KuTuBaseSpec {
  "wettkampf" should {
    "create with disziplin-plan-times" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel", Set(20), "testmail@test.com", 33, 0, None)
      assert(wettkampf.id > 0L)
      val views = initWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(views.nonEmpty)
    }

    "update" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel2", Set(20), "testmail@test.com", 33, 0, None)
      val wettkampfsaved = saveWettkampf(wettkampf.id, wettkampf.datum, "neuer titel", Set(wettkampf.programmId), "testmail@test.com", 10000, 7.5, wettkampf.uuid)
      assert(wettkampfsaved.titel == "neuer titel")
      assert(wettkampfsaved.auszeichnung == 10000)
    }

    "recreate with disziplin-plan-times" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel2", Set(20), "testmail@test.com", 33, 0, None)
      assert(wettkampf.id > 0L)
      val views = initWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(views.nonEmpty)
      val wettkampf2 = createWettkampf(LocalDate.now(), "titel2", Set(20), "testmail@test.com", 33, 0, wettkampf.uuid)
      assert(wettkampf2.id == wettkampf.id)
      val views2 = initWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(views2.size == views.size)
    }

    "update disziplin-plan-time" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel2", Set(20), "testmail@test.com", 33, 0, None)
      assert(wettkampf.id > 0L)
      val views = initWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))

      updateWettkampfPlanTimeView(views(0).toWettkampfPlanTimeRaw.copy(einturnen = 20000))
      val reloaded = loadWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(reloaded(0).einturnen == 20000L)
    }

    "delete all disziplin-plan-time entries when wk is deleted" in {
      val wettkampf = createWettkampf(LocalDate.now(), "titel3", Set(20), "testmail@test.com", 33, 0, None)
      deleteWettkampf(wettkampf.id)
      val reloaded = loadWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      assert(reloaded.isEmpty)
    }

    "create WK Modus with programs and disciplines" in {

      println(insertWettkampfProgram("Testprogramm", List("Boden", "Sprung"), List("LK1", "LK2", "LK3")).mkString("\n"))
    }
  }
}