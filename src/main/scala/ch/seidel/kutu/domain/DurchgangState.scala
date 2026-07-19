package ch.seidel.kutu.domain

import scala.collection.immutable

trait DurchgangItem

case class DurchgangState(wettkampfUUID: String, name: String, complete: Boolean, geraeteRiegen: List[GeraeteRiege], durchgang: Durchgang) extends DurchgangItem {
  val started: Long = durchgang.effectiveStartTime.map(_.getTime).getOrElse(0)
  val finished: Long = durchgang.effectiveEndTime.map(_.getTime).getOrElse(0)

  def update(newGeraeteRiegen: List[GeraeteRiege], dg: Durchgang): DurchgangState = {
    if this.durchgang == dg && newGeraeteRiegen.forall(gr => geraeteRiegen.contains(gr)) then this
    else DurchgangState(wettkampfUUID, name, complete, newGeraeteRiegen, dg)
  }

  def ~(other: DurchgangState): Boolean = name == other.name && geraeteRiegen != other.geraeteRiegen

  val updated: Long = System.currentTimeMillis()
  val isRunning: Boolean = started > 0L && finished < started

  def lastResultsURL(baseUrl: String): String =
    s"$baseUrl/last-results?c=$wettkampfUUID&d=${encodeURIParam(name)}&k=gemischt"

  private lazy val statsCompletedBase: immutable.Iterable[(Option[Disziplin], Int, Int, Int, List[(Int, Int, Int, Int)])] = DurchgangState.computeStats(geraeteRiegen)

  private lazy val anzValue: Int = statsCompletedBase.map(_._4).sum

  lazy val anz = s"$anzValue"
  lazy val min = s"${statsCompletedBase.map(_._2).min}%"
  lazy val max = s"${statsCompletedBase.map(_._2).max}%"
  lazy val avg = s"${100 * statsCompletedBase.map(_._3).sum / anzValue}%"

  lazy val percentPerRiegeComplete: Map[Option[Disziplin], (String, String)] = statsCompletedBase
    .map(gr => gr._1 -> (s"${gr._2}%", gr._5.map(grh => s"Station ${grh._1 + 1}: ${grh._2}%").mkString("\n"))).toMap

  lazy val totalTime: Long = if started > 0 then {
    if finished > started then finished - started else updated - started
  } else 0
}

object DurchgangState {
  def computeStats(
    geraeteRiegen: List[GeraeteRiege],
    includePause: Boolean = false
  ): immutable.Iterable[(Option[Disziplin], Int, Int, Int, List[(Int, Int, Int, Int)])] = {
    geraeteRiegen.groupBy(gr => gr.disziplin)
      .filter { d => includePause || !d._1.get.isPause }
      .map { gr =>
        val (disziplin, grd) = gr

        def hasWertungInDisciplin(wertungen: Seq[WertungView]) =
          wertungen.filter(w => disziplin.contains(w.wettkampfdisziplin.disziplin)).exists(_.endnote.nonEmpty)

        val grdStats = grd.groupBy(grds => grds.halt).map { grdsh =>
          val totalCnt = grdsh._2.map(_.kandidaten.size).sum
          val completedCnt = grdsh._2.map(_.kandidaten.count(k => hasWertungInDisciplin(k.wertungen))).sum
          (grdsh._1, 100 * completedCnt / totalCnt, completedCnt, totalCnt)
        }.toList.sortBy(grdsh => grdsh._1)

        val totalCnt = gr._2.map(_.kandidaten.size).sum
        val completedCnt = gr._2.map(_.kandidaten.count(k => hasWertungInDisciplin(k.wertungen))).sum
        // (disziplin, complete%, completeCnt, totalCnt, haltStats(halt, complete%, completeCnt, totalCnt))
        (disziplin, 100 * completedCnt / totalCnt, completedCnt, totalCnt, grdStats)
      }
  }
}
