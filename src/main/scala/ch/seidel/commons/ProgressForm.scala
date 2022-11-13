package ch.seidel.commons

import scalafx.concurrent.Task
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Label, ProgressBar, ProgressIndicator}
import scalafx.scene.layout.{BorderPane, HBox, Priority, VBox}
import scalafx.stage.{Modality, Stage, StageStyle}


class ProgressForm {
  private val pb = new ProgressBar {
    hgrow = Priority.Always
    prefWidth = 480
  }
  private val pin = new ProgressIndicator

  private val dialogStage = new Stage {
    initStyle(StageStyle.Utility)
    initModality(Modality.ApplicationModal)
    resizable = false
    minWidth = 480
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

  def activateProgressBar(title: String, task: Task[_]): Unit = {
    if (task != null) {
      dialogStage.title.value = title
      pb.progressProperty.bind(task.progressProperty)
      pin.progressProperty.bind(task.progressProperty)
      label.text <== task.message
    } else {
      dialogStage.title.value = "Verarbeitung ..."
      label.text.value = title
    }
    dialogStage.show()
  }

  def getDialogStage: Stage = dialogStage
}
