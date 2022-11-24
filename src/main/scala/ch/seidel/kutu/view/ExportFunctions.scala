package ch.seidel.kutu.view

import java.util.UUID

import ch.seidel.kutu.Config.{homedir, remoteBaseUrl}
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.akka.DurchgangChanged
import ch.seidel.kutu.domain.{KutuService, WettkampfView}
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.renderer.PrintUtil
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import javafx.beans.property.SimpleObjectProperty
import scalafx.Includes.jfxObjectProperty2sfx
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.print.PageOrientation

import scala.concurrent.Future

trait ExportFunctions {
  val wettkampfInfo: WettkampfInfo
  val wettkampf = wettkampfInfo.wettkampf
  val service: KutuService
  val reprintItems: SimpleObjectProperty[Set[DurchgangChanged]] = new SimpleObjectProperty[Set[DurchgangChanged]]()
  reprintItems.set(Set.empty)
  println("subscribing RiegenTab for refreshing from websocket")
  val subscription = WebSocketClient.modelWettkampfWertungChanged.onChange { (_, _, newItem) =>
    newItem match {
      case d: DurchgangChanged =>
        reprintItems.set(reprintItems.get() + d)
      case _ =>
    }
  }
  def doSelectedRiegenBelatterExport(dialogText: String, durchgang: Set[String], halts: Set[Int] = Set.empty)(implicit event: ActionEvent): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val seriendaten = service.getAllKandidatenWertungen(wettkampf.uuid.map(UUID.fromString(_)).get)
    val durchgangFileQualifier = durchgang.mkString("_dg(","-",")").replace(" ", "_")
    val haltsFileQualifier = halts.mkString("_h(", "-", ")")
    val filename = "Riegenblatt_" + wettkampf.easyprint.replace(" ", "_") + durchgangFileQualifier + haltsFileQualifier + ".html"
    val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
    if(!dir.exists()) {
      dir.mkdirs();
    }
    val logofile = PrintUtil.locateLogoFile(dir)
    def generate = (lpp: Int) => KuTuApp.invokeAsyncWithBusyIndicator("Riegenblätter aufbereiten ...") { Future {
      Platform.runLater {
        reprintItems.set(reprintItems.get().filter(p => !durchgang.contains(p.durchgang)))
      }
      (new Object with ch.seidel.kutu.renderer.RiegenblattToHtmlRenderer).toHTML(seriendaten, logofile, remoteBaseUrl, durchgang, halts)
    }}
    Platform.runLater {
      PrintUtil.printDialogFuture(dialogText, FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
    }
  }

}
