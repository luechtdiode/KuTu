package ch.seidel.commons

import javafx.concurrent.Task

case class TaskSteps(title: String) extends Task[Void] {
  updateMessage(title)
  var steps: List[(String, () => Unit)] = List.empty

  @throws[InterruptedException]
  override def call: Void = {
    val allSteps = steps.size
    for (((msg, stepFn), idx) <- steps.zipWithIndex) {
      println(msg)
      updateMessage(msg)
      if (allSteps > 1) {
        updateProgress((idx * 2) + 1, allSteps * 2)
      }
      stepFn()
      if (allSteps > 1) {
        updateProgress(idx * 2 + 2, allSteps * 2)
      }
      println(msg + "ready")
    }
    null
  }

  def nextStep(msg: String, stepFn: () => Unit): TaskSteps = {
    steps = steps :+ (msg, stepFn)
    this
  }
}