package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*

trait GeraeteDistributor {

  protected def distributeToStartgeraete(programm: String, startgeraete: List[Disziplin], alignedriegen: Seq[RiegeAthletWertungen]): Seq[(String, String, Disziplin, Seq[WertungViewsZuAthletView])] = {
    if alignedriegen.isEmpty || startgeraete.isEmpty then {
      Seq.empty
    } else {
      val numDurchgaenge = math.ceil(alignedriegen.size.toDouble / startgeraete.size).toInt
      val orderedRiegen = if numDurchgaenge <= 1 then alignedriegen else alignedriegen.sortBy(r => -r.sizeOfAll)

      val assigned = orderedRiegen.zipWithIndex.flatMap { r =>
        val (rr, index) = r
        val startgeridx = (index + startgeraete.size) % startgeraete.size
        rr.keys.map { riegenname =>
          (s"$programm (${index / startgeraete.size + 1})", riegenname, startgeraete(startgeridx), rr(riegenname))
        }
      }

      val maxRound = math.max(1, math.ceil(orderedRiegen.size.toDouble / startgeraete.size).toInt)
      val expectedDurchgaenge = (1 to maxRound).map(round => s"$programm ($round)")

      val emptyPerDurchgang = expectedDurchgaenge.flatMap { durchgangName =>
        val occupiedStarts = assigned.filter(_._1 == durchgangName).map(_._3.id).toSet
        startgeraete.filterNot(s => occupiedStarts.contains(s.id)).map { start =>
          (durchgangName, s"Leere Riege $programm/${start.easyprint} [$durchgangName]", start, Seq.empty[WertungViewsZuAthletView])
        }
      }

      assigned ++ emptyPerDurchgang
    }
  }
}

