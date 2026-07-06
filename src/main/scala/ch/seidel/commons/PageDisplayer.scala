package ch.seidel.commons

import ch.seidel.kutu.domain.*
import ch.seidel.kutu.view.*
import ch.seidel.kutu.{KuTuApp, KuTuAppTree}
import javafx.scene as jfxs
import javafx.scene.control as jfxsc
import org.slf4j.{Logger, LoggerFactory}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.image.*
import scalafx.scene.layout.*
import scalafx.scene.Node
import scalafx.stage.Modality

import java.io.StringWriter
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}


/**
 * the class that updates tabbed view or dashboard view
 * based on the TreeItem selected from left pane
 */
object PageDisplayer {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def toButtonType(button: Button, isPrimary: Boolean): jfxsc.ButtonType = {
    val caption = Option(button.text.value).getOrElse("")
    caption.trim.toLowerCase match {
      case "ok" => jfxsc.ButtonType.OK
      case "abbrechen" | "cancel" => jfxsc.ButtonType.CANCEL
      case "schliessen" | "schließen" | "close" => jfxsc.ButtonType.CLOSE
      case _ => new jfxsc.ButtonType(caption, if isPrimary then jfxsc.ButtonBar.ButtonData.OK_DONE else jfxsc.ButtonBar.ButtonData.OTHER)
    }
  }

  private def showDialog(tit: String, nodeToAdd: DisplayablePage, owner: javafx.stage.Window, commands: Seq[Button]): Unit = {
    val content = new BorderPane {
      padding = Insets(15)
      center = nodeToAdd.getPage
    }

    val dialog = new jfxsc.Dialog[Unit]()
    dialog.setTitle(tit)
    dialog.initModality(Modality.WindowModal)
    dialog.initOwner(owner)
    dialog.getDialogPane.setContent(content.delegate)

    val commandButtons = commands.toSeq
    val commandMappings = commandButtons.zipWithIndex.map { case (btn, idx) =>
      val buttonType = toButtonType(btn, idx == 0)
      (buttonType, btn, idx)
    }
    val hasCancelOrClose = commandMappings.exists(m => m._1 == jfxsc.ButtonType.CANCEL || m._1 == jfxsc.ButtonType.CLOSE)
    val fallbackButtonType = if commandMappings.isEmpty then jfxsc.ButtonType.CLOSE else jfxsc.ButtonType.CANCEL
    val allButtonTypes = commandMappings.map(_._1) ++ (if hasCancelOrClose then Seq.empty else Seq(fallbackButtonType))
    dialog.getDialogPane.getButtonTypes.addAll(allButtonTypes*)

    commandMappings.foreach { case (buttonType, sourceButton, idx) =>
      val dialogButton = dialog.getDialogPane.lookupButton(buttonType).asInstanceOf[javafx.scene.control.Button]
      dialogButton.setMinWidth(100)
      if idx == 0 then dialogButton.setDefaultButton(true)
      dialogButton.disableProperty().bind(sourceButton.disable.delegate)
    }

    dialog.setResultConverter(dialogButton => {
      commandMappings.find(_._1 == dialogButton).foreach { case (_, sourceButton, _) =>
        sourceButton.fire()
      }
      ()
    })

    dialog.showAndWait()
  }

  def showInDialog(tit: String, nodeToAdd: DisplayablePage, commands: Button*)(implicit event: ActionEvent): Unit = {
    val owner = event.source match {
      case n: jfxs.Node if n.getScene.getRoot == KuTuApp.getStage.getScene.getRoot =>
        n.getScene.getWindow
      case _ =>
        KuTuApp.getStage.getScene.getWindow
    }
    showDialog(tit, nodeToAdd, owner, commands)
  }

  def showInDialogFromRoot(tit: String, nodeToAdd: DisplayablePage, commands: Button*): Unit = {
    showDialog(tit, nodeToAdd, KuTuApp.getStage.getScene.getWindow, commands)
  }

  /*
      val txtUsername = new TextField {
            prefWidth = 500
            promptText = "Username"
            text = System.getProperty("user.name")
          }

          val txtPassword = new PasswordField {
            prefWidth = 500
            promptText = "Internet Proxy Passwort"
          }
          "Internet Proxy authentication"
          () =>
              setProxyProperties(
                  host = Config.proxyHost.getOrElse(""),
                  port = Config.proxyPort.getOrElse(""),
                  user = txtUsername.text.value.trim(),
                  password = txtPassword.text.value.trim())
            }
   *
   */
  def askFor(caption: String, fields: (String, String)*): Option[Seq[String]] = {
    val p = Promise[Option[Seq[String]]]()

    def ask(): Unit = {
      val controls = fields.flatMap {
        case f if f._1.contains("*") =>
          val t = f._1.split('*')(0)
          Seq(new Label(t), new PasswordField {
            prefWidth = 500
            promptText = t
            text.value = f._2
          })
        case t =>
          Seq(new Label(t._1), new TextField {
            prefWidth = 500
            promptText = t._2
            text.value = t._2
          })
      }
      val observedControls = controls.zipWithIndex.filter(x => x._2 % 2 != 0).map(_._1.asInstanceOf[TextField])
      var ret: Option[Seq[String]] = None
      showInDialogFromRoot(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children = controls
            }
          }
        }
      }, new Button("OK") {
        //          disable <== when(Bindings.createBooleanBinding(() => {
        //                                observedControls.forall(c => !c.text.isEmpty.value)
        //                              },
        //                                observedControls2(0), observedControls2(Math.min(1, observedControls2.size-1)), observedControls2(Math.min(3, observedControls2.size-1)), observedControls2(Math.min(4, observedControls2.size-1))
        //                              )) choose true otherwise false
        onAction = () => {
          ret = Some(observedControls.map(c => c.text.value))
        }
      })
      p success ret
    }

    if Platform.isFxApplicationThread then ask() else Platform.runLater {
      ask()
    }

    Await.result(p.future, Duration.Inf)
  }

  def confirm[T](caption: String, lines: Seq[String], okAction: () => T): Option[T] = {
    val p = Promise[Option[T]]()

    def ask(): Unit = {
      var ret: Option[T] = None
      showInDialogFromRoot(caption, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children = lines.map(text => new Label(text))
            }
          }
        }
      }, new Button("OK") {
        onAction = () => {
          ret = Some(okAction())
        }
      })
      p success ret
    }

    if Platform.isFxApplicationThread then ask() else Platform.runLater {
      ask()
    }

    Await.result(p.future, Duration.Inf)
  }

  def showErrorDialog(caption: String): Throwable => Unit = (error: Throwable) => {
    Platform.runLater {
      import javafx.scene.control.Alert.AlertType
      val alert = new Alert(AlertType.ERROR)
      alert.setTitle("Unerwarteter Fehler")
      alert.setHeaderText(s"Fehler beim Ausführen von '$caption'")
      alert.setContentText(s"${error.getMessage}")

      val label = new Label(s"Details:")
      import java.io.PrintWriter
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      error.printStackTrace(pw)
      val exceptionText = sw.toString
      val textArea = new TextArea(exceptionText)
      textArea.setEditable(false)
      textArea.setWrapText(true)

      textArea.setMaxWidth(100000)
      textArea.setMaxHeight(100000)
      GridPane.setVgrow(textArea, Priority.Always)
      GridPane.setHgrow(textArea, Priority.Always)

      val expContent = new GridPane()
      expContent.setMaxWidth(100000)
      expContent.add(label, 0, 0)
      expContent.add(textArea, 0, 1)

      // Set expandable Exception into the dialog pane.
      alert.getDialogPane.expandableContent = expContent
      alert.initOwner(KuTuApp.getStage.getScene.getWindow)
      alert.show()
    }
  }

  def showErrorDialog(caption: String, message: String): Unit = {
    import javafx.scene.control.Alert.AlertType
    Platform.runLater {
      val alert = new javafx.scene.control.Alert(AlertType.ERROR)
      alert.setTitle("Fehler")
      alert.setHeaderText(caption)
      alert.setContentText(message)
      alert.initOwner(KuTuApp.getStage.getScene.getWindow)
      alert.show()
    }
  }

  def showWarnDialog(caption: String, message: String): Unit = {
    import javafx.scene.control.Alert.AlertType
    Platform.runLater {
      val alert = new javafx.scene.control.Alert(AlertType.WARNING)
      alert.setTitle("Achtung")
      alert.setHeaderText(caption)
      alert.setContentText(message)
      alert.initOwner(KuTuApp.getStage.getScene.getWindow)
      alert.show()
    }
  }

  def showMessageDialog(caption: String, message: String): Unit = {
    import javafx.scene.control.Alert.AlertType
    Platform.runLater {
      val alert = new javafx.scene.control.Alert(AlertType.INFORMATION)
      alert.setTitle("Information")
      alert.setHeaderText(caption)
      alert.setContentText(message)
      alert.initOwner(KuTuApp.getStage.getScene.getWindow)
      alert.show()
    }
  }

  def choosePage(wettkampfmode: BooleanProperty, context: Option[Any], value: String = "dashBoard", tree: KuTuAppTree): Node = {
    value match {
      case "dashBoard" => displayPage(new DashboardPage(tree = tree))
      case _ => context match {
        case Some(w: WettkampfView) => chooseWettkampfPage(wettkampfmode, w, tree)
        case Some(v: Verein) => chooseVereinPage(wettkampfmode, v, tree)
        case _ => displayPage(new DashboardPage(value.split("-")(1).trim(), tree))
      }
    }
  }

  private def chooseWettkampfPage(wettkampfmode: BooleanProperty, wettkampf: WettkampfView, tree: KuTuAppTree): Node = {
    try {
      displayPage(WettkampfPage.buildTab(wettkampfmode, WettkampfInfo(wettkampf, tree.getService), tree.getService))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    }
  }

  private def chooseVereinPage(wettkampfmode: BooleanProperty, verein: Verein, tree: KuTuAppTree): Node = {
    try {
      displayPage(TurnerPage.buildTab(wettkampfmode, verein, tree.getService))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    }
  }

  private var activePage: Option[DisplayablePage] = None

  private def displayPage(nodeToAdd: DisplayablePage): Node = {
    activePage match {
      case Some(p) if p != nodeToAdd =>
        p.release()
      case _ =>
    }
    activePage = Some(nodeToAdd)
    val ret: VBox = new VBox {
      vgrow = Priority.Always
      hgrow = Priority.Always
      val indicator: VBox = new VBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        children = Seq(new Label("loading ..."))
        styleClass += "category-page"
      }
      children = Seq(indicator)
    }

    def op(): Unit = {
      logger.debug("start nodeToAdd.getPage")
      val p = nodeToAdd.getPage
      logger.debug("end nodeToAdd.getPage")
      ret.children = p
      ret.requestLayout()
    }

    KuTuApp.invokeWithBusyIndicator(op())
    ret
  }
}
