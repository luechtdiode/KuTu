package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.{GemischteRiegen, GemischterDurchgang, Verein}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class VereinMergerSpec extends AnyWordSpec with Matchers {

  object Harness extends VereinMerger {
    def substitutesFor(riegeToReplace: GeraeteRiege, zielriege: GeraeteRiege, maxRiegenSize2: Int): Option[GeraeteRiege] =
      findSubstitutesFor(riegeToReplace, zielriege, maxRiegenSize2)

    def spread(startriegen: GeraeteRiegen, splitSex: ch.seidel.kutu.domain.SexDivideRule, targetDiff: Int): GeraeteRiegen =
      spreadEven(startriegen, splitSex, targetDiff)
  }

  private val vereinA = Verein(1L, "TV A", Some("ZH"))
  private val vereinB = Verein(2L, "TV B", Some("ZH"))
  private val vereinC = Verein(3L, "TV C", Some("ZH"))

  private def tr(name: String, verein: Verein, sex: String, size: Int): TurnerRiege =
    TurnerRiege(name, Some(verein), sex, size)

  "VereinMerger" should {
    "not find substitutes when target riege only contains same-club entries" in {
      val toReplace = GeraeteRiege(Set(tr("A-replace", vereinA, "M", 2)))
      val target = GeraeteRiege(Set(tr("A-1", vereinA, "M", 1), tr("A-2", vereinA, "M", 1)))

      Harness.substitutesFor(toReplace, target, maxRiegenSize2 = 4).shouldBe(None)
    }

    "find substitutes from other clubs when they fit into replacement constraints" in {
      val toReplace = GeraeteRiege(Set(tr("A-replace", vereinA, "M", 2)))
      val target = GeraeteRiege(Set(
        tr("B-1", vereinB, "M", 1),
        tr("C-1", vereinC, "M", 1),
        tr("A-keep", vereinA, "M", 1)
      ))

      val substitutes = Harness.substitutesFor(toReplace, target, maxRiegenSize2 = 4)

      substitutes.shouldBe(defined)
      substitutes.get.turnerriegen.flatMap(_.verein).shouldBe(Set(vereinB, vereinC))
      substitutes.get.size.shouldBe(2)
    }

    "keep distribution unchanged when spread is already within target difference" in {
      val grouped = Set(
        GeraeteRiege(Set(tr("A1", vereinA, "M", 3))),
        GeraeteRiege(Set(tr("B1", vereinB, "M", 2)))
      )

      Harness.spread(grouped, GemischteRiegen, targetDiff = 1).shouldBe(grouped)
    }

    "not rebalance across sexes in GemischterDurchgang when smallest and largest groups differ by sex" in {
      val grouped = Set(
        GeraeteRiege(Set(tr("M-big", vereinA, "M", 4), tr("M-small", vereinA, "M", 1))),
        GeraeteRiege(Set(tr("W-small", vereinB, "W", 1)))
      )

      Harness.spread(grouped, GemischterDurchgang, targetDiff = 1).shouldBe(grouped)
    }
  }
}

