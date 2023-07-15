package ch.seidel.kutu.view

import ch.seidel.kutu.domain.{Disziplin, KutuService, TeamRegel, WettkampfView, WettkampfdisziplinView}

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
}
