package ch.seidel.kutu.view

import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.KutuService
import ch.seidel.kutu.domain.WertungView
import ch.seidel.kutu.domain.WettkampfView

class RanglisteTab(wettkampf: WettkampfView, override val service: KutuService) extends DefaultRanglisteTab(service) {
  override val title = wettkampf.easyprint
  val programmText = wettkampf.programm.id match {case 20 => "Kategorie" case _ => "Programm"}

  override def groupers: List[FilterBy] = {
    List(ByNothing, ByWettkampfProgramm(programmText), ByProgramm(programmText), ByJahrgang, ByGeschlecht, ByVerband, ByVerein, ByRiege, ByDisziplin)
  }

  override def getData: Seq[WertungView] = service.selectWertungen(wettkampfId = Some(wettkampf.id))

  override def getSaveAsFilenameDefault: FilenameDefault =
    FilenameDefault("Rangliste_" + wettkampf.easyprint.replace(" ", "_") + ".html", new java.io.File(service.homedir + "/" + wettkampf.easyprint.replace(" ", "_")))

  override def isPopulated = {
    val combos = populate(groupers)

    combos(1).selectionModel.value.select(ByWettkampfProgramm(programmText))
    combos(2).selectionModel.value.select(ByGeschlecht)

    true
  }

}
