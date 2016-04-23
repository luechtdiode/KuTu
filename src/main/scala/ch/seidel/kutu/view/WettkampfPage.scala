package ch.seidel.kutu.view

import scalafx.scene.control.Tab
import ch.seidel.kutu.domain._
import ch.seidel.commons.DisplayablePage
import ch.seidel.commons.LazyTabPane
import ch.seidel.commons.TabWithService

object WettkampfPage {

  def buildTab(wettkampfmode: Boolean, wettkampf: WettkampfView, service: KutuService) = {
	  lazy val progs = service.readWettkampfLeafs(wettkampf.programm.id)
    lazy val progSites: Seq[Tab] = progs map {v =>
      new WettkampfWertungTab(wettkampfmode, Some(v), None, wettkampf, service, {
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
      (progSites).foreach { t => t.asInstanceOf[WettkampfWertungTab].setLazyPane(pane)}
      if(wettkampfmode) {
        progSites ++ ranglisteSite
      }
      else {
        progSites ++ Seq[Tab](new RiegenTab(wettkampf, service)) ++ ranglisteSite
      }
    }

    new WettkampfPage( new LazyTabPane(refresher))
  }
}

class WettkampfPage(tabPane: LazyTabPane)
  extends DisplayablePage {

  def getPage = {
    import WettkampfPage._

    tabPane.init()
    tabPane
  }
}
