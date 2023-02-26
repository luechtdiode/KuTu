package ch.seidel.kutu.view

import ch.seidel.kutu.Config._
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import scalafx.beans.property.BooleanProperty

class TurnerScoreTab(wettkampfmode: BooleanProperty, val verein: Option[Verein], override val service: KutuService) extends DefaultRanglisteTab(wettkampfmode, service) {
  override val title = verein match {case Some(v) => v.easyprint case None => "Vereinsübergreifend"}

  override def groupers: List[FilterBy] =
    List(ByNothing(), ByJahr(), ByWettkampf(), ByWettkampfArt(), ByWettkampfProgramm(), ByProgramm(), ByJahrgang(), ByGeschlecht(), ByVerein(), ByVerband(), ByDisziplin(), ByAthlet())

  override def getData: Seq[WertungView] = verein match {
    case Some(v) => service.selectWertungen(vereinId = Some(v.id))
    case None    => service.selectWertungen()
  }

  override def getSaveAsFilenameDefault: FilenameDefault = FilenameDefault("Rangliste_" + encodeFileName(verein.map(_.name.replace(" ", "_")).getOrElse("Alle")), new java.io.File(homedir))

  override def isPopulated = {
    val combos = populate(groupers)
    combos.head.selectionModel.value.select(ByWettkampfProgramm())
    combos.tail.head.selectionModel.value.select(ByGeschlecht())

    true
  }

}
