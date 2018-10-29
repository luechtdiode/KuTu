package ch.seidel.kutu.view

import ch.seidel.commons.{DisplayablePage, LazyTabPane}
import ch.seidel.kutu.domain._
import scalafx.beans.property.BooleanProperty
import scalafx.scene.control.Tab

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
    lazy val networkSite: Seq[Tab] = Seq(
        new NetworkTab(wettkampf, service)
      )
        
    def releaser() {
      (progSites).foreach { t => 
        t.asInstanceOf[WettkampfWertungTab].release
      }
      ranglisteSite.foreach{t => 
        t.asInstanceOf[RanglisteTab].release
      }
      networkSite.foreach{t => 
        t.asInstanceOf[NetworkTab].release
      }
    }
    
    def refresher(pane: LazyTabPane) = {
      (progSites).foreach { t => 
        t.asInstanceOf[WettkampfWertungTab].setLazyPane(pane)
//        t.asInstanceOf[WettkampfWertungTab].release
      }
      if(wettkampfmode.value) {
        progSites ++ networkSite ++ ranglisteSite
      }
      else {
        progSites ++ Seq[Tab](new RiegenTab(wettkampf, service)) ++ networkSite ++ ranglisteSite
      }
    }

    new WettkampfPage( new LazyTabPane(refresher, () => releaser()))
  }
}

class WettkampfPage(tabPane: LazyTabPane)
  extends DisplayablePage {

  def getPage = {

    tabPane.init()
    tabPane
  }
  
  override def release() {
    tabPane.release()
  }
}
