package ch.seidel.kutu.view

import java.util.UUID

import ch.seidel.commons._
import ch.seidel.kutu.Config.{homedir, remoteHostOrigin}
import ch.seidel.kutu.{ConnectionStates, KuTuApp, KuTuServer}
import ch.seidel.kutu.KuTuApp.{controlsView, selectedWettkampfSecret, stage}
import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer.{PrintUtil, WettkampfOverviewToHtmlRenderer}
import javafx.scene.text.FontSmoothingType
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.event.ActionEvent
import scalafx.event.subscriptions.Subscription
import scalafx.print.PageOrientation
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.web.WebView
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter

import scala.concurrent.Future

class WettkampfOverviewTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService with WettkampfOverviewToHtmlRenderer {

  var subscription: List[Subscription] = List.empty

  override def release {
    subscription.foreach(_.cancel)
    subscription = List.empty
  }

  private var lazypane: Option[LazyTabPane] = None

  def setLazyPane(pane: LazyTabPane): Unit = {
    lazypane = Some(pane)
  }

  def refreshLazyPane() {
    lazypane match {
      case Some(pane) => pane.refreshTabs()
      case _ =>
    }
  }
  val webView = new WebView {
    fontSmoothingType = FontSmoothingType.GRAY
  }

  def reloadData(): Unit = {
    webView.engine.loadContent(createDocument)
  }

  def createDocument = {
    val logofile = PrintUtil.locateLogoFile(new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_")))
    val document = toHTML(wettkampf, service.listOverviewStats(UUID.fromString(wettkampf.uuid.get)), logofile)
    document
  }

  onSelectionChanged = handle {
    if(selected.value) {
      reloadData()
    }
  }


  def importAnmeldungen(implicit event: ActionEvent) = {
    RegistrationAdmin.importRegistrations(WettkampfInfo(wettkampf, service), KuTuServer, vereinsupdated =>
      if (vereinsupdated) KuTuApp.updateTree else reloadData()
    )
  }

  override def isPopulated(): Boolean = {
    val wettkampfEditable = !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)
    reloadData()
    text = "Übersicht"
    closable = false
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
              implicit val e = event
              val fileChooser = new FileChooser {
                title = "Wettkampf Logo laden"
                initialDirectory = new java.io.File(homedir)
                extensionFilters ++= Seq(
                  new ExtensionFilter("All supported Graphic-Files", List("*.svg", "*.png", "*.jpeg","*.jpg")),
                  new ExtensionFilter("Vectorgraphic-Files", "*.svg"),
                  new ExtensionFilter("Portable Networkgraphic-Files", "*.png"),
                  new ExtensionFilter("Joint Photographic Experts Group Graphic-Files", List("*.jpeg","*.jpg"))
                )
                initialFileName.value = "logo.jpg"
              }
              val selectedFile = fileChooser.showOpenDialog(stage)
              if (selectedFile != null) {
                KuTuApp.invokeWithBusyIndicator {
                  import java.nio.file.{Files, StandardCopyOption}
                  val reg_ex = """.*\.(\w+)""".r
                  val extension = selectedFile.getName match {
                    case reg_ex(ext) => ext
                    case _ => ""
                  }
                  val destLogoFile = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_") + "/logo." + extension)
                  Files.find(new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_")).toPath,
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
            text = "Online Anmeldungen importieren ..."
            disable <== when(Bindings.createBooleanBinding(() =>
                   !wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin)
                || !ConnectionStates.connectedWithProperty.value.equals(wettkampf.uuid.getOrElse("")),
              ConnectionStates.connectedWithProperty,
              selectedWettkampfSecret,
              controlsView.selectionModel().selectedItem)
            ) choose true otherwise false
//            disable <== when(Bindings.createBooleanBinding(() =>
//              !wettkampf.toWettkampf.hasSecred(homedir, remoteHostOrigin),
//              ConnectionStates.connectedWithProperty
//            )) choose true otherwise false
            onAction = (e: ActionEvent) => importAnmeldungen(e)
          },
          new Button {
            text = "Übersicht drucken ..."
            minWidth = 75
            onAction = (event: ActionEvent) => {
              import scala.concurrent.ExecutionContext.Implicits.global
              val filename = "Uebersicht_" + wettkampf.easyprint.replace(" ", "_") + ".html"
              val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
              if(!dir.exists()) {
                dir.mkdirs();
              }

              def generate = (lpp: Int) => Future{
                createDocument
              }
              Platform.runLater {
                PrintUtil.printDialogFuture("Wettkampfübersicht drucken ...", FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
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