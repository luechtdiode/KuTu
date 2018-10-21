package ch.seidel.commons

import ch.seidel.kutu.{KuTuApp, KuTuAppTree}
import ch.seidel.kutu.domain._
import ch.seidel.kutu.view._
import javafx.{scene => jfxs}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.Observable
import scalafx.beans.property.BooleanProperty
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.{Node, Scene}
import scalafx.scene.control.{Button, Label, PasswordField, TextField}
import scalafx.scene.image._
import scalafx.scene.layout.{BorderPane, HBox, Priority, VBox}
import scalafx.stage.{Modality, Stage}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration


/**
 * the class that updates tabbed view or dashboard view
 * based on the TreeItem selected from left pane
 */
object PageDisplayer {
  var errorIcon: Image = null
  try {
    errorIcon = new Image(getClass().getResourceAsStream("/images/RedException.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }
    
  def showInDialog(tit: String, nodeToAdd: DisplayablePage, commands: Button*)(implicit event: ActionEvent) {
    val buttons = commands :+ new Button(if(commands.length == 0) "Schliessen" else "Abbrechen")
    // Create dialog
    val dialogStage = new Stage {
      outer => {
        initModality(Modality.WindowModal)
        event.source match {
          case n: jfxs.Node if(n.getScene.getRoot == KuTuApp.getStage.getScene.getRoot) =>
            delegate.initOwner(n.getScene.getWindow)
          case _ =>
            delegate.initOwner(KuTuApp.getStage.getScene.getWindow)
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
    // Show dialog and wait till it is closed
    dialogStage.showAndWait()
  }
  
  def showInDialogFromRoot(tit: String, nodeToAdd: DisplayablePage, commands: Button*) {
    val buttons = commands :+ new Button(if(commands.length == 0) "Schliessen" else "Abbrechen")
    // Create dialog
    val dialogStage = new Stage {
      outer => {
        initModality(Modality.WINDOW_MODAL)
        delegate.initOwner(KuTuApp.getStage.getScene.getWindow)

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
    val p = Promise[Option[Seq[String]]]
    def ask {
      val controls = fields.flatMap{
        case f if(f._1.contains("*")) => 
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
      val observedControls2 = observedControls.map(c => c.text.asInstanceOf[Observable])
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
            ret = Some(observedControls.map(c => c.text.value).toSeq)
          }
        })
      p success ret      
    }
    if (Platform.isFxApplicationThread) ask else Platform.runLater{ask}      

    Await.result(p.future, Duration.Inf)
  }
  
  def showErrorDialog(caption: String) = (error: Throwable) => {
    Platform.runLater{
      showInDialogFromRoot(caption, 
      new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children = Seq(new HBox{ children = Seq(new ImageView {
                image = errorIcon
          	  }, new Label(s"Fehler beim Ausführen von '$caption'"))}, 
          	  new Label(s"${error}"))
            }
          }
        }
      }
    )
    }
  }
  
  def choosePage(wettkampfmode: BooleanProperty, context: Option[Any], value: String = "dashBoard", tree: KuTuAppTree): Node = {
    value match {
      case "dashBoard" => displayPage(new DashboardPage(tree = tree))
      case _           => context match {
        case Some(w: WettkampfView) => chooseWettkampfPage(wettkampfmode, w, tree)
        case Some(v: Verein)        => chooseVereinPage(v, tree)
        case _                      => displayPage(new DashboardPage(value.split("-")(1).trim(), tree))
      }
    }
  }
  private def chooseWettkampfPage(wettkampfmode: BooleanProperty, wettkampf: WettkampfView, tree: KuTuAppTree): Node = {
    displayPage(WettkampfPage.buildTab(wettkampfmode, wettkampf, tree.getService))
  }
  private def chooseVereinPage(verein: Verein, tree: KuTuAppTree): Node = {
    displayPage(TurnerPage.buildTab(verein, tree.getService))
  }

  var activePage: Option[DisplayablePage] = None
  
  private def displayPage(nodeToAdd: DisplayablePage): Node = {
    activePage match {
      case Some(p) if(p != nodeToAdd) => 
        p.release()
      case _ =>
    }
    activePage = Some(nodeToAdd)
    val ret = new VBox {
      vgrow = Priority.Always
      hgrow = Priority.Always
      val indicator = new VBox {
        vgrow = Priority.Always
        hgrow = Priority.Always
        children = Seq(new Label("loading ..."))
        styleClass += "category-page"
      }
      children = Seq(indicator)
    }
    def op = {
      val p = nodeToAdd.getPage
      ret.children = p
      ret.requestLayout()
    }
    KuTuApp.invokeWithBusyIndicator(op)
    ret
  }
}
