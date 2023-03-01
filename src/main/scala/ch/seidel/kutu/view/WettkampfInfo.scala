package ch.seidel.kutu.view

import ch.seidel.kutu.domain.{Disziplin, KutuService, WettkampfView, WettkampfdisziplinView}

case class WettkampfInfo(wettkampf: WettkampfView, service: KutuService) {
  val wettkampfdisziplinViews: List[WettkampfdisziplinView] = service.listWettkampfDisziplineViews(wettkampf.toWettkampf)
  val disziplinList: List[Disziplin] = wettkampfdisziplinViews.foldLeft(List[Disziplin]()) { (acc, dv) =>
    if (!acc.contains(dv.disziplin)) acc :+ dv.disziplin else acc
  }
//  val rootprograms = wettkampfdisziplinViews.map(wd => wd.programm.parent).filter(_.nonEmpty).map(_.get).toSet.toList.sortWith((a, b) => a.ord < b.ord)
  val leafprograms = wettkampfdisziplinViews.map(wd => wd.programm).toSet.toList.sortWith((a, b) => a.ord < b.ord)
  val isDNoteUsed = wettkampfdisziplinViews.exists(wd => wd.isDNoteUsed)
  val isAthletikTest = wettkampf.programm.aggregatorHead.id == 1
}
