package ch.seidel.kutu.view

import ch.seidel.kutu.domain
import ch.seidel.kutu.domain.{Disziplin, KutuService, TeamRegel, WettkampfView, WettkampfdisziplinView, ld2SQLDate, sdfShort}

import java.time.{LocalDateTime, LocalTime}
import java.util.UUID

case class WettkampfInfo(wettkampf: WettkampfView, service: KutuService) {
  val wettkampfdisziplinViews: List[WettkampfdisziplinView] = service.listWettkampfDisziplineViews(wettkampf.toWettkampf)
  val disziplinList: List[Disziplin] = wettkampfdisziplinViews.foldLeft(List[Disziplin]()) { (acc, dv) =>
    if (!acc.contains(dv.disziplin)) acc :+ dv.disziplin else acc
  }
  val startDisziplinList: List[Disziplin] = wettkampfdisziplinViews.foldLeft(List[Disziplin]()) { (acc, dv) =>
    if (!acc.contains(dv.disziplin) && dv.startgeraet > 0) acc :+ dv.disziplin else acc
  }
  val teamRegel: TeamRegel = TeamRegel(wettkampf.teamrule)
  val isAlterklasse: Boolean = wettkampf.altersklassen.nonEmpty
  val isJGAlterklasse: Boolean = wettkampf.jahrgangsklassen.nonEmpty
  //val pathPrograms = wettkampfdisziplinViews.flatMap(wd => wd.programm.programPath.reverse.tail).distinct.sortBy(_.ord).reverse
  val parentPrograms = wettkampfdisziplinViews.map(wd => wd.programm.parent).filter(_.nonEmpty).map(_.get).distinct.sortBy(_.ord)
  //val parentPrograms = wettkampfdisziplinViews.map(wd => wd.programm.aggregator).distinct.sortBy(_.ord)
  val groupHeadPrograms = wettkampfdisziplinViews.map(wd => wd.programm.groupedHead).distinct.sortBy(_.ord)
  val subHeadPrograms = wettkampfdisziplinViews.map(wd => wd.programm.subHead).filter(_.nonEmpty).map(_.get).distinct.sortBy(_.ord)
  val leafprograms = wettkampfdisziplinViews.map(wd => wd.programm).distinct.sortBy(_.ord)
  val isAggregated = wettkampfdisziplinViews.exists(wd => wd.programm.aggregate != 0)
  val isDNoteUsed = wettkampfdisziplinViews.exists(wd => wd.isDNoteUsed)
  val isAthletikTest = wettkampf.programm.aggregatorHead.id == 1
  val dgEvents: Seq[(domain.Durchgang, LocalDateTime, LocalDateTime)] = service.selectDurchgaenge(UUID.fromString(wettkampf.uuid.get))
    .map(d => (d, d.effectivePlanStart(wettkampf.datum.toLocalDate), d.effectivePlanFinish(wettkampf.datum.toLocalDate)))
  val startDate = ld2SQLDate((LocalDateTime.of(wettkampf.datum.toLocalDate, LocalTime.MIN) +: dgEvents.map(_._2)).distinct.min.toLocalDate)
  val endDate = ld2SQLDate((LocalDateTime.of(wettkampf.datum.toLocalDate, LocalTime.MIN) +: dgEvents.map(_._2)).distinct.max.toLocalDate)
  val wkEventString = if (startDate.equals(endDate))  f"$startDate%td.$startDate%tm.$startDate%ty" else  f"$startDate%td.$startDate%tm.$startDate%ty - $endDate%td.$endDate%tm.$endDate%ty"
}
