package ch.seidel.kutu.view

import ch.seidel.commons.{DisplayablePage, LazyTabPane}
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.domain.*
import org.slf4j.LoggerFactory
import scalafx.beans.binding.Bindings.*
import scalafx.beans.property.BooleanProperty
import scalafx.event.subscriptions.Subscription
import scalafx.scene.control.Tab

object WettkampfPage {
  val logger = LoggerFactory.getLogger(this.getClass)

  def buildTab(wettkampfmode: BooleanProperty, wettkampfInfo: WettkampfInfo, service: KutuService) = {
    logger.debug("Start buildTab")
    val wettkampf = wettkampfInfo.wettkampf
    val progs = wettkampfInfo.leafprograms
    val pathProgs = wettkampfInfo.parentPrograms
    logger.debug("Start Overview")
    val overview = new WettkampfOverviewTab(wettkampf, service)
    logger.debug("Start Alle Wertungen")
    val alleWertungenTabs: Seq[Tab] = (pathProgs map { v =>
      val leafHeadProgs = progs.filter(p => p.programPath.contains(v))
      val pgm = if v.parent.nonEmpty then Some(v) else None
      new WettkampfWertungTab(wettkampfmode, pgm, None, wettkampfInfo, service, {
        service.listAthletenWertungenZuProgramm(leafHeadProgs map (p => p.id), wettkampf.id)
      }) {
        val progHeader = if v.parent.nonEmpty then v.name else ""
        text <== when(wettkampfmode) choose s"Alle $progHeader Wertungen" otherwise s"Alle $progHeader"
        closable = false
      }
    })
    val preferencesTab = new ScoreCalcTemplatesTab(wettkampf, service) // new PreferencesTab(wettkampfInfo, service)

    logger.debug("Start Program Tabs")
    val progSites: Seq[Tab] = (progs map { v =>
      new WettkampfWertungTab(wettkampfmode, Some(v), None, wettkampfInfo, service, {
        service.listAthletenWertungenZuProgramm(progs map (p => p.id), wettkampf.id)
          .filter(w => w.wettkampfdisziplin.programm.programPath.contains(v))
      }) {
        text = v.name
        closable = false
      }
    }) ++ alleWertungenTabs

    logger.debug("Start RiegenTab Tab")
    val riegenSite: Seq[Tab] = Seq(new RiegenTab(wettkampfInfo, service))
    logger.debug("Start Network Tab")
    val networkSite: Seq[Tab] = Seq(
      new NetworkTab(wettkampfmode, wettkampfInfo, service)
    )
    logger.debug("Start Rangliste Tab")
    val ranglisteSite: Seq[Tab] = Seq(
      new RanglisteTab(wettkampfmode, wettkampf, service) {
        text = "Rangliste"
        closable = false
      }
    )

    var subscription: Option[Subscription] = None

    def releaser(): Unit = {
      subscription match {
        case Some(s) => s.cancel()
        case _ =>
      }
      subscription = None
      overview.release
      preferencesTab.release
      (progSites).foreach { t =>
        t.asInstanceOf[WettkampfWertungTab].release
      }
      ranglisteSite.foreach { t =>
        t.asInstanceOf[RanglisteTab].release
      }
      riegenSite.foreach { t =>
        t.asInstanceOf[RiegenTab].release
      }
      networkSite.foreach { t =>
        t.asInstanceOf[NetworkTab].release
      }
    }

    def refresher(pane: LazyTabPane): Seq[Tab] = {
      overview.setLazyPane(pane)
      preferencesTab.setLazyPane(pane)
      (progSites).foreach { t =>
        t.asInstanceOf[WettkampfWertungTab].setLazyPane(pane)
      }
      networkSite.foreach {
        t => t.asInstanceOf[NetworkTab].setLazyPane(pane)
      }
      if wettkampfmode.value then {
        Seq[Tab](overview) ++
          networkSite ++ alleWertungenTabs ++ ranglisteSite
      }
      else {
        Seq[Tab](overview, preferencesTab) ++
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

  def getPage: LazyTabPane = {

    tabPane.init()
    tabPane
  }

  override def release(): Unit = {
    tabPane.release()
  }
}
