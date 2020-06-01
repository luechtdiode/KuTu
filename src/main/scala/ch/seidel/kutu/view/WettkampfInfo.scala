package ch.seidel.kutu.view

import ch.seidel.kutu.domain.{KutuService, WettkampfView, Disziplin, WettkampfdisziplinView}

case class WettkampfInfo(wettkampf: WettkampfView, service: KutuService) {
  val wettkampfdisziplinViews: List[WettkampfdisziplinView] = service.listWettkampfDisziplineViews(wettkampf.toWettkampf)
  val disziplinList: List[Disziplin] = wettkampfdisziplinViews.foldLeft(List[Disziplin]()){(acc, dv) =>
    if (!acc.contains(dv.disziplin)) acc :+ dv.disziplin else acc
  }
  val leafprograms = wettkampfdisziplinViews.map(wd => wd.programm)/*.filter(p => p.aggregate == 0)*/.toSet.toList.sortWith((a, b) => a.ord < b.ord)
  val isDNoteUsed = wettkampfdisziplinViews.exists(wd => wd.notenSpez.isDNoteUsed)
  val isAthletikTest = wettkampf.programm.aggregatorHead.id == 1
}
