package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*

trait GeraeteDistributor {

  protected def distributeToStartgeraete(programm: String, startgeraete: List[Disziplin], alignedriegen: Seq[RiegeAthletWertungen], disziplinGeschlecht: Map[Long, (Int, Int)] = Map.empty): Seq[(String, String, Disziplin, Seq[WertungViewsZuAthletView])] = {
    if alignedriegen.isEmpty || startgeraete.isEmpty then {
      Seq.empty
    } else {
      val numDurchgaenge = math.ceil(alignedriegen.size.toDouble / startgeraete.size).toInt
      val orderedRiegen = if numDurchgaenge <= 1 then alignedriegen else alignedriegen.sortBy(r => -r.sizeOfAll)

      def getGroupGeschlecht(athletes: Seq[WertungViewsZuAthletView]): Option[String] = {
        athletes.headOption.map(_._1.geschlecht)
      }

      def isCompatibleDisziplin(disziplinId: Long, groupGeschlecht: Option[String]): Boolean = {
        disziplinGeschlecht.get(disziplinId) match {
          case Some((masculin, feminim)) =>
            groupGeschlecht match {
              case Some(g) if g.equalsIgnoreCase("M") => masculin == 1
              case Some(g) if g.equalsIgnoreCase("W") => feminim == 1
              case _ => true
            }
          case _ => true
        }
      }

      def findCompatibleDisziplin(groupGeschlecht: Option[String], startIndex: Int): Option[Disziplin] = {
        startgeraete.indices
          .map(i => startgeraete((startIndex + i) % startgeraete.size))
          .find(d => isCompatibleDisziplin(d.id, groupGeschlecht))
      }

      val assigned = orderedRiegen.zipWithIndex.flatMap { r =>
        val (rr, index) = r
        val baseStartgeridx = (index + startgeraete.size) % startgeraete.size
        rr.keys.map { riegenname =>
          val groupGeschlecht = getGroupGeschlecht(rr(riegenname))
          val disziplin = findCompatibleDisziplin(groupGeschlecht, baseStartgeridx)
            .getOrElse(startgeraete(baseStartgeridx))
          (s"$programm (${index / startgeraete.size + 1})", riegenname, disziplin, rr(riegenname))
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

