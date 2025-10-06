package ch.seidel.commons

import java.io.StringWriter
import ch.seidel.kutu.{KuTuApp, KuTuAppTree}
import ch.seidel.kutu.domain._
import ch.seidel.kutu.view._
import javafx.scene.input.{KeyCode, KeyEvent}
import javafx.{scene => jfxs}
import org.slf4j.{Logger, LoggerFactory}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.Observable
import scalafx.beans.property.BooleanProperty
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.{Node, Scene}
import scalafx.scene.control.{Alert, Button, Label, PasswordField, TextArea, TextField}
import scalafx.scene.image._
import scalafx.scene.layout.{BorderPane, GridPane, HBox, Priority, VBox}
import scalafx.stage.{Modality, Stage}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration


/**
 * the class that updates tabbed view or dashboard view
 * based on the TreeItem selected from left pane
 */
object PageDisplayer {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  var errorIcon: Image = null
  try {
    errorIcon = new Image(getClass.getResourceAsStream("/images/RedException.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }
  var warnIcon: Image = null
  try {
    warnIcon = new Image(getClass.getResourceAsStream("/images/OrangeWarning.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }
  var infoIcon: Image = null
  try {
    infoIcon = new Image(getClass.getResourceAsStream("/images/GreenOk.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }

  def showInDialog(tit: String, nodeToAdd: DisplayablePage, commands: Button*)(implicit event: ActionEvent): Unit = {
    val buttons = commands :+ new Button(if(commands.isEmpty) "Schliessen" else "Abbrechen")
    // Create dialog
    val dialogStage = new Stage {
      outer => {
        initModality(Modality.WindowModal)
        event.source match {
          case n: jfxs.Node if n.getScene.getRoot == KuTuApp.getStage().getScene.getRoot =>
            delegate.initOwner(n.getScene.getWindow)
          case _ =>
            delegate.initOwner(KuTuApp.getStage().getScene.getWindow)
        }

        title = tit
        scene = new Scene {
          root = new BorderPane {
            padding = Insets(15)
            center = nodeToAdd.getPage
            bottom = new HBox {
              prefHeight = 50
              alignment = Pos.BottomRight
              hgrow = Priority.Always
              children = buttons
            }
            var first = false
            buttons.foreach { btn =>
              if(!first) {
                first = true
                btn.defaultButton = true
              }
              btn.minWidth = 100
              btn.filterEvent(ActionEvent.Action) { () => outer.close()}}
          }
        }
      }
    }
    dialogStage.delegate.addEventHandler(KeyEvent.KEY_RELEASED, (event: KeyEvent) => {
      if (KeyCode.ESCAPE eq event.getCode) dialogStage.close()
    })

    // Show dialog and wait till it is closed
    dialogStage.showAndWait()
  }
  
  def showInDialogFromRoot(tit: String, nodeToAdd: DisplayablePage, commands: Button*): Unit = {
    val buttons = commands :+ new Button(if(commands.isEmpty) "Schliessen" else "Abbrechen")
    // Create dialog
    val dialogStage = new Stage {
      outer => {
        initModality(Modality.WindowModal)
        delegate.initOwner(KuTuApp.getStage().getScene.getWindow)

        title = tit
        scene = new Scene {
          root = new BorderPane {
            padding = Insets(15)
            center = nodeToAdd.getPage
            bottom = new HBox {
              prefHeight = 50
              alignment = Pos.BottomRight
              hgrow = Priority.Always
              children = buttons
            }
            var first = false
            buttons.foreach { btn =>
              if(!first) {
                first = true
                btn.defaultButton = true
              }
              btn.minWidth = 100
              btn.filterEvent(ActionEvent.Action) { () => outer.close()}}
          }
        }
      }
    }
    dialogStage.delegate.addEventHandler(KeyEvent.KEY_RELEASED, (event: KeyEvent) => {
      if (KeyCode.ESCAPE eq event.getCode) dialogStage.close()
    })
    // Show dialog and wait till it is closed
    dialogStage.showAndWait()
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
      val controls = fields.flatMap{
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
    if (Platform.isFxApplicationThread) ask() else Platform.runLater{ask()}

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
    if (Platform.isFxApplicationThread) ask() else Platform.runLater{ask()}

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
      alert.initOwner(KuTuApp.getStage().getScene.getWindow)
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
      alert.initOwner(KuTuApp.getStage().getScene.getWindow)
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
      alert.initOwner(KuTuApp.getStage().getScene.getWindow)
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
      alert.initOwner(KuTuApp.getStage().getScene.getWindow)
      alert.show()
    }
  }

  def choosePage(wettkampfmode: BooleanProperty, context: Option[Any], value: String = "dashBoard", tree: KuTuAppTree): Node = {
    value match {
      case "dashBoard" => displayPage(new DashboardPage(tree = tree))
      case _           => context match {
        case Some(w: WettkampfView) => chooseWettkampfPage(wettkampfmode, w, tree)
        case Some(v: Verein)        => chooseVereinPage(wettkampfmode, v, tree)
        case _                      => displayPage(new DashboardPage(value.split("-")(1).trim(), tree))
      }
    }
  }
  private def chooseWettkampfPage(wettkampfmode: BooleanProperty, wettkampf: WettkampfView, tree: KuTuAppTree): Node = {
    try {
      displayPage(WettkampfPage.buildTab(wettkampfmode, WettkampfInfo(wettkampf, tree.getService), tree.getService))
    } catch {
      case e:Exception =>
        e.printStackTrace()
        throw e
    }
  }
  private def chooseVereinPage(wettkampfmode: BooleanProperty, verein: Verein, tree: KuTuAppTree): Node = {
    try {
      displayPage(TurnerPage.buildTab(wettkampfmode, verein, tree.getService))
    } catch {
      case e:Exception =>
        e.printStackTrace()
        throw e
    }
  }

  var activePage: Option[DisplayablePage] = None
  
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
