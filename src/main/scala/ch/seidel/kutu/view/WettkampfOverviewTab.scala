package ch.seidel.kutu.view

import ch.seidel.commons.*
import ch.seidel.kutu.Config.{homedir, remoteBaseUrl, remoteHostOrigin}
import ch.seidel.kutu.KuTuApp.{controlsView, getStage, handleAction, modelWettkampfModus, selectedWettkampf, selectedWettkampfSecret, stage}
import ch.seidel.kutu.data.{ByAltersklasse, ByJahrgangsAltersklasse}
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer.{CompetitionsJudgeToHtmlRenderer, PrintUtil, WettkampfOverviewToHtmlRenderer}
import ch.seidel.kutu.*
import javafx.scene.text.FontSmoothingType
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.event.ActionEvent
import scalafx.event.subscriptions.Subscription
import scalafx.print.PageOrientation
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.scene.web.WebView
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter

import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

class WettkampfOverviewTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService
  with WettkampfOverviewToHtmlRenderer with CompetitionsJudgeToHtmlRenderer {

  var subscription: List[Subscription] = List.empty

  override def release: Unit = {
    subscription.foreach(_.cancel())
    subscription = List.empty
  }

  private var lazypane: Option[LazyTabPane] = None

  def setLazyPane(pane: LazyTabPane): Unit = {
    lazypane = Some(pane)
  }

  def refreshLazyPane(): Unit = {
    lazypane match {
      case Some(pane) => pane.refreshTabs()
      case _ =>
    }
  }
  private val webView = new WebView {
    fontSmoothingType = FontSmoothingType.GRAY
    import ch.seidel.javafx.webview.HyperLinkRedirectListener
    engine.getLoadWorker.stateProperty.addListener(new HyperLinkRedirectListener(this.delegate))
  }

  def reloadData(): Unit = {
    webView.engine.loadContent(createDocument)
  }

  private def createDocument = {
    val logofile = PrintUtil.locateLogoFile(new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint)))
    val data = if wettkampf.altersklassen.nonEmpty then {
      val aks = ByAltersklasse("AK", Altersklasse.parseGrenzen(wettkampf.altersklassen))
      val facts = service.listAKOverviewFacts(UUID.fromString(wettkampf.uuid.get))
      val grouper: (java.time.LocalDate, String, ProgrammView) => Altersklasse = (gebdat, geschlecht, programm) => aks.makeGroupBy(wettkampf.toWettkampf)(gebdat, geschlecht, programm)
      facts.groupBy(_._1).flatMap(gr => gr._2.groupBy(x => (x._2, grouper(x._5, x._4, x._2))).map { gr2 =>
        val pgmname = if gr2._1._2.easyprint.contains(gr2._1._1.name) then gr2._1._2.easyprint else s"${gr2._1._1.name} ${gr2._1._2.easyprint}"
        (gr._1, pgmname, gr2._1._2.alterVon, gr2._2.count(_._4.equals("M")), gr2._2.count(_._4.equals("W")))
      }).toList
    } else if wettkampf.jahrgangsklassen.nonEmpty then {
      val aks = ByJahrgangsAltersklasse("AK", Altersklasse.parseGrenzen(wettkampf.jahrgangsklassen))
      val facts = service.listAKOverviewFacts(UUID.fromString(wettkampf.uuid.get))
      val grouper: (java.time.LocalDate, String, ProgrammView) => Altersklasse = (gebdat, geschlecht, programm) => aks.makeGroupBy(wettkampf.toWettkampf)(gebdat, geschlecht, programm)
      facts.groupBy(_._1).flatMap(gr => gr._2.groupBy(x => (x._2, grouper(x._5, x._4, x._2))).map { gr2 =>
        val pgmname = if gr2._1._2.easyprint.contains(gr2._1._1.name) then gr2._1._2.easyprint else s"${gr2._1._1.name} ${gr2._1._2.easyprint}"
        (gr._1, pgmname, gr2._1._2.alterVon, gr2._2.count(_._4.equals("M")), gr2._2.count(_._4.equals("W")))
      }).toList
    } else {
      service.listOverviewStats(UUID.fromString(wettkampf.uuid.get))
    }
    val teams = TeamRegel(wettkampf.teamrule)
    val wertungen = if teams.teamsAllowed then {
      service.selectWertungen(wettkampfId = Some(wettkampf.id))
    } else Seq()
    val eventString = WettkampfInfo(wettkampf, service).wkEventString

    val document = toHTML(wettkampf, eventString, data, wertungen.toList, logofile)
    document
  }

  onSelectionChanged = _ => {
    if selected.value then {
      reloadData()
    }
  }

  def importAnmeldungen(implicit event: ActionEvent): Unit = {
    RegistrationAdminDialog.importRegistrations(WettkampfInfo(wettkampf, service), KuTuServer, vereinsupdated =>
      if vereinsupdated then KuTuApp.updateTree else reloadData()
    )
  }

  override def isPopulated: Boolean = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val wettkampfEditable = !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)
    reloadData()
    text = "Übersicht"
    closable = false
    subscription = subscription :+ KuTuApp.selectedWettkampfSecret.onChange { (_, _, _) => reloadData() }
    subscription = subscription :+ LocalServerStates.localServerProperty.onChange { (_, _, _) => reloadData() }
    subscription = subscription :+ ConnectionStates.connectedProperty.onChange { (_, _, _) => reloadData() }
    subscription = subscription :+ ConnectionStates.remoteServerProperty.onChange { (_, _, _) => reloadData() }
    val reportMenu = new MenuButton {
      text = "Liste erstellen"
      disable <== when(Bindings.createBooleanBinding(() =>
        !wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin)
          || !ConnectionStates.connectedWithProperty.value.equals(wettkampf.uuid.getOrElse("")),
        ConnectionStates.connectedWithProperty,
        selectedWettkampfSecret,
        controlsView.selectionModel().selectedItem)
      ) choose true otherwise false
      items += KuTuApp.makeMenuAction(s"Liste der Online Vereins-Registrierungen ...") { (caption: String, action: ActionEvent) =>
        val filename = "VereinsRegistrierungen_" + encodeFileName(wettkampf.easyprint) + ".html"
        val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
        if !dir.exists() then {
          dir.mkdirs()
        }

        def generate = (lpp: Int) => KuTuApp.invokeAsyncWithBusyIndicator(caption) {
          Future {
            //toHTMLasClubRegistrationsList(wettkampf, KuTuServer.getAllRegistrationsRemote(wettkampf.toWettkampf), logofile)
            KuTuServer.getAllRegistrationsHtmlRemote(wettkampf.toWettkampf)
          }
        }

        Platform.runLater {
          PrintUtil.printDialogFuture(caption, FilenameDefault(filename, dir), adjustLinesPerPage = false, generate, orientation = PageOrientation.Landscape)(action)
        }
      }
      items += KuTuApp.makeMenuAction(s"Liste der Online Wertungsrichter-Anmeldungen ...") { (caption: String, action: ActionEvent) =>
        val filename = "WRAnmeldungen_" + encodeFileName(wettkampf.easyprint) + ".html"
        val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
        if !dir.exists() then {
          dir.mkdirs()
        }

        def generate = (lpp: Int) => KuTuApp.invokeAsyncWithBusyIndicator(caption) {
          Future {
            KuTuServer.getAllJudgesHTMLRemote(wettkampf.toWettkampf)
          }
        }

        Platform.runLater {
          PrintUtil.printDialogFuture(caption, FilenameDefault(filename, dir), adjustLinesPerPage = false, generate, orientation = PageOrientation.Landscape)(action)
        }
      }
    }
    content = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      top = new ToolBar {
        content = List(
          new Button {
            text = "Wettkampf-Logo laden"
            minWidth = 75
            disable = !wettkampfEditable
            onAction = (event: ActionEvent) => {
              implicit val e: ActionEvent = event
              val fileChooser = new FileChooser {
                title = "Wettkampf Logo laden"
                initialDirectory = new java.io.File(homedir)
                extensionFilters ++= Seq(
                  new ExtensionFilter("All supported Graphic-Files", List("*.svg", "*.png", "*.jpeg", "*.jpg")),
                  new ExtensionFilter("Vectorgraphic-Files", "*.svg"),
                  new ExtensionFilter("Portable Networkgraphic-Files", "*.png"),
                  new ExtensionFilter("Joint Photographic Experts Group Graphic-Files", List("*.jpeg", "*.jpg"))
                )
                initialFileName.value = "logo.jpg"
              }
              val selectedFile = fileChooser.showOpenDialog(getStage())
              if selectedFile != null then {
                if selectedFile.length() > Config.logoFileMaxSize then {
                  val maxSize = java.text.NumberFormat.getInstance().format(Config.logoFileMaxSize / 1024)
                  val currentSize = java.text.NumberFormat.getInstance().format(selectedFile.length() / 1024)
                  PageDisplayer.showWarnDialog("Wettkampf-Logo laden", s"Die Datei ${selectedFile.getName} ist mit $currentSize Kilobytes zu gross. Sie darf nicht grösser als $maxSize Kilobytes sein.")
                } else KuTuApp.invokeWithBusyIndicator {
                  import java.nio.file.{Files, StandardCopyOption}
                  val reg_ex = """.*\.(\w+)""".r
                  val extension = selectedFile.getName match {
                    case reg_ex(ext) => ext
                    case _ => ""
                  }
                  val destLogoFile = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint) + "/logo." + extension)
                  Files.find(new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint)).toPath,
                    1, (path, fileattr) => {
                      path.getFileName.toFile.getName.startsWith("logo.")
                    }).forEach(path => {
                    Files.delete(path)
                  })
                  Files.copy(selectedFile.toPath, destLogoFile.toPath, StandardCopyOption.REPLACE_EXISTING)
                  reloadData()
                }
              }
            }
          },
          new Button {
            val uploadMenu = KuTuApp.makeWettkampfUploadMenu(wettkampf, when(Bindings.createBooleanBinding(() => {
              !wettkampfEditable ||
                (wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin) && !ConnectionStates.connectedProperty.value) ||
                Config.isLocalHostServer || modelWettkampfModus.value
            },
              KuTuApp.selectedWettkampfSecret,
              LocalServerStates.localServerProperty,
              ConnectionStates.connectedProperty,
              ConnectionStates.remoteServerProperty,
              modelWettkampfModus
            )) choose true otherwise false)
            onAction = uploadMenu.onAction.get
            text <== uploadMenu.text
            disable <== uploadMenu.disable
          },
          new Button {
            text = "Online Anmeldungen importieren ..."
            disable <== when(Bindings.createBooleanBinding(() =>
              !wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin)
                || !ConnectionStates.connectedWithProperty.value.equals(wettkampf.uuid.getOrElse("")),
              ConnectionStates.connectedWithProperty,
              selectedWettkampfSecret,
              controlsView.selectionModel().selectedItem)
            ) choose true otherwise false
            onAction = (e: ActionEvent) => importAnmeldungen(e)
          },
          reportMenu,
          new Button {
            text = "Übersicht drucken ..."
            minWidth = 75
            onAction = (event: ActionEvent) => {
              import scala.concurrent.ExecutionContext.Implicits.global
              val filename = "Uebersicht_" + encodeFileName(wettkampf.easyprint) + ".html"
              val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
              if !dir.exists() then {
                dir.mkdirs()
              }

              def generate = (lpp: Int) => Future {
                createDocument
              }
              Platform.runLater {
                PrintUtil.printDialogFuture("Wettkampfübersicht drucken ...", FilenameDefault(filename, dir), adjustLinesPerPage = false, generate, orientation = PageOrientation.Portrait)(event)
              }
            }
          }

        )
      }
      center = new VBox {
        children.add(
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = webView
          }.asInstanceOf[Node]
        )
      }
    }
    true
  }
}