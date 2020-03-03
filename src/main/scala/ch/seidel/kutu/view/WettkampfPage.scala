package ch.seidel.kutu.view

import ch.seidel.commons.{DisplayablePage, LazyTabPane}
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.domain._
import scalafx.beans.property.BooleanProperty
import scalafx.scene.control.Tab
import scalafx.beans.binding.Bindings._
import scalafx.event.subscriptions.Subscription

object WettkampfPage {

  def buildTab(wettkampfmode: BooleanProperty, wettkampf: WettkampfView, service: KutuService) = {
	  lazy val progs = service.readWettkampfLeafs(wettkampf.programm.id)
    val overview = new WettkampfOverviewTab(wettkampf, service)
    lazy val alleWertungenTab = new WettkampfWertungTab(wettkampfmode, None, None, wettkampf, service, {
      service.listAthletenWertungenZuProgramm(progs map (p => p.id), wettkampf.id)
    }) {
      text <== when(wettkampfmode) choose "Alle Wertungen" otherwise "Alle"
      closable = false
    }

    lazy val progSites: Seq[Tab] = (progs map { v =>
      new WettkampfWertungTab(wettkampfmode, Some(v), None, wettkampf, service, {
        service.listAthletenWertungenZuProgramm(progs map (p => p.id), wettkampf.id)
        }) {
        text = v.name
        closable = false
      }
    }) :+ alleWertungenTab
   
    lazy val ranglisteSite: Seq[Tab] = Seq(
      new RanglisteTab(wettkampfmode, wettkampf, service) {
        text = "Rangliste"
        closable = false
      }
    )
    lazy val networkSite: Seq[Tab] = Seq(
        new NetworkTab(wettkampfmode, wettkampf, service)
      )
    lazy val riegenSite: Seq[Tab] = Seq(new RiegenTab(wettkampf, service))
    var subscription: Option[Subscription] = None

    def releaser() {
      subscription match {
        case Some(s) => s.cancel()
        case _ =>
      }
      overview.release
      (progSites).foreach { t => 
        t.asInstanceOf[WettkampfWertungTab].release
      }
      ranglisteSite.foreach{t => 
        t.asInstanceOf[RanglisteTab].release
      }
      riegenSite.foreach{t =>
        t.asInstanceOf[RiegenTab].release
      }
      networkSite.foreach{t => 
        t.asInstanceOf[NetworkTab].release
      }
    }
    
    def refresher(pane: LazyTabPane): Seq[Tab] = {
      overview.setLazyPane(pane)
      (progSites).foreach { t => 
        t.asInstanceOf[WettkampfWertungTab].setLazyPane(pane)
      }
      networkSite.foreach {
        t => t.asInstanceOf[NetworkTab].setLazyPane(pane)
      }
      if(wettkampfmode.value) {
        Seq[Tab](overview) ++
                networkSite ++ Seq[Tab](alleWertungenTab) ++ ranglisteSite
      }
      else {
        Seq[Tab](overview) ++
                progSites ++ riegenSite ++ networkSite ++ ranglisteSite
      }
    }

    val lazyPane = new LazyTabPane(refresher, () => releaser())
    subscription = Some(wettkampfmode.onChange {
        KuTuApp.invokeWithBusyIndicator {
          lazyPane.init()
        }
      })
    new WettkampfPage(lazyPane)
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
