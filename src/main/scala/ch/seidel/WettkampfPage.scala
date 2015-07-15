package ch.seidel

import ch.seidel.commons.DisplayablePage
import ch.seidel.commons.LazyTabPane
import ch.seidel.domain.KutuService
import ch.seidel.domain.WettkampfView
import scalafx.scene.control.Tab

object WettkampfPage {

  def buildTab(wettkampf: WettkampfView, service: KutuService) = {
	  lazy val progs = service.readWettkampfLeafs(wettkampf.programm.id)
    lazy val progSites: Seq[Tab] = progs map {v =>
      new WettkampfWertungTab(Some(v), None, wettkampf, service, {
        service.listAthletenWertungenZuProgramm(progs map (p => p.id), wettkampf.id)
        }) {
        text = v.name
        closable = false
      }
    }
    lazy val ranglisteSite: Seq[Tab] = Seq(
      new RanglisteTab(wettkampf, service) {
        text = "Rangliste"
        closable = false
      }
    )
    def refresher(pane: LazyTabPane) = {
      val riegen = service.listRiegenZuWettkampf(wettkampf.id).sorted
      val riegenSites: Seq[Tab] = riegen map {v =>
        new WettkampfWertungTab(None, Some(v), wettkampf, service, {
          service.listAthletenWertungenZuRiege(progs map (p => p.id), wettkampf.id, v)
          }) {
          text = v
          closable = false
        }
      }
      (riegenSites ++ progSites).foreach { t => t.asInstanceOf[WettkampfWertungTab].setLazyPane(pane)}
      riegenSites ++ progSites ++ ranglisteSite
    }

    new WettkampfPage( new LazyTabPane(refresher))
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
