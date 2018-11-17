package ch.seidel.kutu.view

import ch.seidel.kutu.Config._
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.{Durchgang, KutuService, WertungView, WettkampfView}
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault

class RanglisteTab(wettkampf: WettkampfView, override val service: KutuService) extends DefaultRanglisteTab(service) {
  override val title = wettkampf.easyprint
  val programmText = wettkampf.programm.id match {case 20 => "Kategorie" case _ => "Programm"}
  def riegenZuDurchgang: Map[String, Durchgang] = {
    val riegen = service.listRiegenZuWettkampf(wettkampf.id)
    riegen.map(riege => riege._1 -> riege._3.map(riege => Durchgang(0, riege)).getOrElse(Durchgang())).toMap
  }
  override def groupers: List[FilterBy] = {
    List(ByNothing(), ByWettkampfProgramm(programmText), ByProgramm(programmText), ByJahrgang(), ByGeschlecht(), ByVerband(), ByVerein(), ByDurchgang(riegenZuDurchgang), ByRiege(), ByDisziplin())
  }

  override def getData: Seq[WertungView] = service.selectWertungen(wettkampfId = Some(wettkampf.id))

  override def getSaveAsFilenameDefault: FilenameDefault =
    FilenameDefault("Rangliste_" + wettkampf.easyprint.replace(" ", "_") + ".html", new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_")))

  override def isPopulated = {
    val combos = populate(groupers)

    combos(1).selectionModel.value.select(ByWettkampfProgramm(programmText))
    combos(2).selectionModel.value.select(ByGeschlecht())

    true
  }
  
}
