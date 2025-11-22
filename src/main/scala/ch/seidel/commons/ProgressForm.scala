package ch.seidel.commons

import scalafx.Includes.double2DurationHelper
import scalafx.animation.FadeTransition
import scalafx.application.{JFXApp3, Platform}
import scalafx.concurrent.{Task, Worker}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Label, ProgressBar, ProgressIndicator}
import scalafx.scene.layout.{BorderPane, HBox, Priority, VBox}
import scalafx.stage.{Modality, Stage, StageStyle}


class ProgressForm(stage: Option[Stage] = None) {
  private val pb = new ProgressBar {
    hgrow = Priority.Always
    prefWidth = 480
  }
  private val pin = new ProgressIndicator

  private lazy val _dialogStage = new Stage {
    initStyle(StageStyle.Utility)
    initModality(Modality.ApplicationModal)
    resizable = false
    minWidth = 480
  }
  def dialogStage = stage match {
    case Some(s) => s
    case None => _dialogStage
  }
  private val label = new Label
  pb.setProgress(-1F)

  pin.setProgress(-1F)
  private val pane = new BorderPane {
    padding = Insets(15)
    center = pb
    top = new HBox {
      prefHeight = 30
      alignment = Pos.TopLeft
      hgrow = Priority.Always
      children.add(label)
    }
  }
  private val scene = new Scene(pane)
  dialogStage.setScene(scene)

  def activateProgressBar(title: String, task: Task[?], onSucces: () => Unit = ()=>{}): Unit = {
    if (task != null) {
      dialogStage.title.value = title
      pb.progressProperty.bind(task.progressProperty)
      pin.progressProperty.bind(task.progressProperty)
      label.text <== task.message
      task.state.onChange { (_, _, newState) =>
        newState match {
          case Worker.State.Succeeded.delegate =>
            pb.progress.unbind()
            pin.progress.unbind()
            label.text.unbind()
            onSucces()

          case _ =>
          // TODO: handle other states
        }
      }
    } else {
      dialogStage.title.value = "Verarbeitung ..."
      label.text.value = title
    }
    dialogStage.show()
    dialogStage.toFront()
  }

  def getDialogStage: Stage = dialogStage
}
