package ch.seidel.kutu.view

import scalafx.scene.control.Tab
import ch.seidel.kutu.domain._
import ch.seidel.commons.DisplayablePage
import ch.seidel.commons.LazyTabPane
import ch.seidel.commons.TabWithService
import scalafx.beans.property.BooleanProperty

object WettkampfPage {

  def buildTab(wettkampfmode: BooleanProperty, wettkampf: WettkampfView, service: KutuService) = {
	  lazy val progs = service.readWettkampfLeafs(wettkampf.programm.id)
    lazy val progSites: Seq[Tab] = (progs map {v =>
      new WettkampfWertungTab(wettkampfmode, Some(v), None, wettkampf, service, {
        service.listAthletenWertungenZuProgramm(progs map (p => p.id), wettkampf.id)
        }) {
        text = v.name
        closable = false
      }
    }) :+ new WettkampfWertungTab(wettkampfmode, None, None, wettkampf, service, {
        service.listAthletenWertungenZuProgramm(progs map (p => p.id), wettkampf.id)
        }) {
        text = "Alle"
        closable = false
      }
   
    lazy val ranglisteSite: Seq[Tab] = Seq(
      new RanglisteTab(wettkampf, service) {
        text = "Rangliste"
        closable = false
      }
    )
    def releaser() {
      (progSites).foreach { t => 
        t.asInstanceOf[WettkampfWertungTab].release
      }
      ranglisteSite.foreach{t => 
        t.asInstanceOf[RanglisteTab].release
      }
    }
    def refresher(pane: LazyTabPane) = {
      (progSites).foreach { t => 
        t.asInstanceOf[WettkampfWertungTab].setLazyPane(pane)
//        t.asInstanceOf[WettkampfWertungTab].release
      }
      if(wettkampfmode.value) {
        progSites ++ ranglisteSite
      }
      else {
        progSites ++ Seq[Tab](new RiegenTab(wettkampf, service)) ++ ranglisteSite
      }
    }

    new WettkampfPage( new LazyTabPane(refresher, releaser))
  }
}

class WettkampfPage(tabPane: LazyTabPane)
  extends DisplayablePage {

  def getPage = {
    import WettkampfPage._

    tabPane.init()
    tabPane
  }
  
  override def release() {
    tabPane.release()
  }
}
