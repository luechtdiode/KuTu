package ch.seidel.kutu.view

import ch.seidel.kutu.data.ByDisziplin
import ch.seidel.kutu.data.ByGeschlecht
import ch.seidel.kutu.data.ByJahrgang
import ch.seidel.kutu.data.ByNothing
import ch.seidel.kutu.data.ByProgramm
import ch.seidel.kutu.data.ByRiege
import ch.seidel.kutu.data.ByVerein
import ch.seidel.kutu.data.ByWettkampfArt
import ch.seidel.kutu.data.ByWettkampfProgramm
import ch.seidel.kutu.data.FilterBy
import ch.seidel.kutu.domain.KutuService
import ch.seidel.kutu.domain.WertungView
import ch.seidel.kutu.domain.WettkampfView

class RanglisteTab(wettkampf: WettkampfView, override val service: KutuService) extends DefaultRanglisteTab(service) {
  override val title = wettkampf.easyprint

  override def groupers(text: String): List[FilterBy] =
    List(ByNothing, ByWettkampfArt, ByWettkampfProgramm(text), ByProgramm(text), ByJahrgang, ByGeschlecht, ByVerein, ByRiege, ByDisziplin)

  override def getData: Seq[WertungView] = service.selectWertungen(wettkampfId = Some(wettkampf.id))

  override def getSaveAsFilenameDefault: FilenameDefault =
    FilenameDefault("Rangliste_" + wettkampf.easyprint.replace(" ", "_") + ".html", new java.io.File(service.homedir + "/" + wettkampf.easyprint.replace(" ", "_")))

  override def isPopulated = {
    val text = wettkampf.programm.id match {case 20 => "Kategorie" case _ => "Programm"}
    val combos = populate(groupers(text))

    combos(0).selectionModel.value.select(ByWettkampfProgramm(text))
    combos(1).selectionModel.value.select(ByGeschlecht)

    true
  }

}
