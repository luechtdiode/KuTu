package ch.seidel.kutu.view

import java.util.UUID
import ch.seidel.kutu.Config.{homedir, remoteBaseUrl}
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.KuTuServer.renderer
import ch.seidel.kutu.akka.DurchgangChanged
import ch.seidel.kutu.domain.{KutuService, TeamItem, Wettkampf, encodeFileName}
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.renderer.{KategorieTeilnehmerToHtmlRenderer, KategorieTeilnehmerToJSONRenderer, PrintUtil}
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer.RiegenBuilder.mapToGeraeteRiegen
import javafx.beans.property.SimpleObjectProperty
import org.slf4j.{Logger, LoggerFactory}
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

    val seriendaten = service.getAllKandidatenWertungen(wettkampf.uuid.map(UUID.fromString).get)
    val durchgangFileQualifier = durchgang.mkString("_dg(","-",")").replace(" ", "_")
    val haltsFileQualifier = halts.mkString("_h(", "-", ")")
    val filename = "Riegenblatt_" + encodeFileName(wettkampf.easyprint) + durchgangFileQualifier + haltsFileQualifier + ".html"
    val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
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

  def doSelectedTeilnehmerExport(dialogText: String, durchgang: Set[String])(implicit event: ActionEvent): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val virtualTeams = wettkampf.toWettkampf.extraTeams.zipWithIndex
      .map(item => TeamItem(item._2 * -1 - 1, item._1))
      .map(vt => vt.index -> vt)
      .toMap
    val seriendaten = mapToGeraeteRiegen(kandidaten = service.getAllKandidatenWertungen(wettkampf.uuid.map(UUID.fromString).get), durchgangFilter = durchgang)
      .filter(gr => gr.halt == 0)
      .flatMap { gr =>
        gr.kandidaten.map { k =>
          val tm = if (k.wertungen.head.team != 0) virtualTeams.getOrElse(k.wertungen.head.team, TeamItem(k.wertungen.head.team, k.verein)) else TeamItem(0, "")
          ch.seidel.kutu.renderer.Kandidat(
            wettkampfTitel = gr.wettkampfTitel,
            geschlecht = k.geschlecht,
            programm = k.programm,
            id = k.id,
            name = k.name,
            vorname = k.vorname,
            jahrgang = k.jahrgang,
            verein = k.verein,
            team = tm.itemText,
            riege = k.einteilung.map(_.r).getOrElse(""),
            durchgang = gr.durchgang.getOrElse(""),
            start = gr.disziplin.map(_.name).getOrElse(""),
            k.diszipline.map(_.name)
          )
        }
      }
    val durchgangFileQualifier = durchgang.zipWithIndex.map(d => s"${d._2}").mkString("_dg(","-",")")

    val filename = "DurchgangTeilnehmer_" + encodeFileName(wettkampf.easyprint) + durchgangFileQualifier + ".html"
    val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
    if(!dir.exists()) {
      dir.mkdirs();
    }
    val logofile = PrintUtil.locateLogoFile(dir)
    def generate = (lpp: Int) => KuTuApp.invokeAsyncWithBusyIndicator("Durchgang Teilnehmerliste aufbereiten ...") { Future {
      Platform.runLater {
        reprintItems.set(reprintItems.get().filter(p => !durchgang.contains(p.durchgang)))
      }
      new KategorieTeilnehmerToHtmlRenderer {
        override val logger: Logger = LoggerFactory.getLogger(classOf[ExportFunctions])
      }.toHTMLasDurchgangListe(seriendaten, logofile)
    }}
    Platform.runLater {
      PrintUtil.printDialogFuture(dialogText, FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
    }
  }

}
