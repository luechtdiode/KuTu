package ch.seidel

import ch.seidel.commons.DisplayablePage
import ch.seidel.commons.LazyTabPane
import ch.seidel.domain.KutuService
import ch.seidel.domain.WettkampfView
import scalafx.scene.control.Tab

object WettkampfPage {

  def buildTab(wettkampf: WettkampfView, service: KutuService) = {
    val progs = service.readWettkampfLeafs(wettkampf.programm.id)

    val progSites: Seq[Tab] = progs map {v =>
      new WettkampfWertungTab(v, wettkampf, service, {
        service.listAthletenWertungenZuProgramm(progs map (p => p.id))
        }) {
        text = v.name
        closable = false
      }
    }
    val ranglisteSite: Seq[Tab] = Seq(
      new RanglisteTab(wettkampf, service) {
        text = "Rangliste"
        closable = false
      }
    )

    new WettkampfPage( new LazyTabPane(progSites ++ ranglisteSite))
  }
}

class WettkampfPage(tabPane: LazyTabPane)
  extends DisplayablePage {

  def getPage = {
    import WettkampfPage._

    tabPane.init
    tabPane
  }
}
