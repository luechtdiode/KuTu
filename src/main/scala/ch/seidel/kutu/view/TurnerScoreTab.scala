package ch.seidel.kutu.view

import scalafx.Includes._
import scalafx.scene.control.TableColumn._
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.ScoreToHtmlRenderer

class TurnerScoreTab(val verein: Option[Verein], override val service: KutuService) extends DefaultRanglisteTab(service) {
  override val title = verein match {case Some(v) => v.easyprint case None => "Vereinsübergreifend"}

  override def groupers: List[FilterBy] =
    List(ByNothing, ByJahr, ByWettkampfArt, ByWettkampfProgramm(), ByProgramm(), ByJahrgang, ByGeschlecht, ByVerein, ByVerband, ByDisziplin)

  override def getData: Seq[WertungView] = verein match {
    case Some(v) => service.selectWertungen(vereinId = Some(v.id))
    case None    => service.selectWertungen()
  }

  override def getSaveAsFilenameDefault: FilenameDefault = FilenameDefault("", new java.io.File(service.homedir))

  override def isPopulated = {
    val combos = populate(groupers)
    combos(0).selectionModel.value.select(ByWettkampfProgramm())
    combos(1).selectionModel.value.select(ByGeschlecht)

    true
  }

}
