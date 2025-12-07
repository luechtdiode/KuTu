package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*
import ch.seidel.kutu.squad
import ch.seidel.kutu.squad.RiegenBuilder.generateRiegen2Name
import org.slf4j.LoggerFactory

trait Mapper {
  private val logger = LoggerFactory.getLogger(classOf[Mapper])

  protected def buildRiegenIndex(riegen: Seq[RiegeAthletWertungen]): Map[String, Seq[(AthletView, Seq[WertungView])]] = riegen.flatten.toMap

  protected def buildWorkModel(riegen: Seq[RiegeAthletWertungen]): GeraeteRiegen = {
    riegen.map(raw => {
      val turnerriegen = raw.map(rt => TurnerRiege(
        rt._1,
        rt._2.headOption.flatMap(_._1.verein),
        rt._2.headOption.map(_._1.geschlecht).get,
        rt._2.size)).toSet[TurnerRiege]
      squad.GeraeteRiege(turnerriegen)
    }
    ).toSet
  }

  protected def rebuildWertungen(riegen: GeraeteRiegen, index: RiegeAthletWertungen): Seq[RiegeAthletWertungen] = {
    riegen.map(gr => gr.turnerriegen.map(tr => tr.name -> index(tr.name)).toMap).toSeq
  }

  protected def rebuildDurchgangWertungen(riegen: Iterable[(String, String, Disziplin, Seq[WertungViewsZuAthletView])]): Map[String, Map[Disziplin, Iterable[(String, Seq[Wertung])]]] =
    riegen.groupBy { r => r._1 }
      .map { rr =>
        val (durchgang, disz) = rr
        logger.debug(durchgang)
        (durchgang, disz.groupBy(d => d._3).map { rrr =>
          val (start, athleten) = rrr
          logger.debug(start.name)
          (start, athleten.map { a =>
            val (_, riegenname, _, wertungen) = a
            logger.debug(riegenname, wertungen.size)
            (riegenname, wertungen.flatMap { wv =>
              wv._2.map(wt =>
                wt.toWertung(riegenname, generateRiegen2Name(wt))
              )
            })
          })
        })
      }

}