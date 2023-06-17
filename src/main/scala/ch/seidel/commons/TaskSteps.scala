package ch.seidel.commons

import javafx.concurrent.Task
import org.slf4j.LoggerFactory

case class TaskSteps(title: String) extends Task[Void] {
  private val logger = LoggerFactory.getLogger(this.getClass)
  updateMessage(title)
  var steps: List[(String, () => Unit)] = List.empty

  @throws[InterruptedException]
  override def call: Void = {
    val allSteps = steps.size
    for (((msg, stepFn), idx) <- steps.zipWithIndex) {
      logger.info(msg)
      updateMessage(msg)
      if (allSteps > 1) {
        updateProgress((idx * 2) + 1, allSteps * 2)
      }
      stepFn()
      if (allSteps > 1) {
        updateProgress(idx * 2 + 2, allSteps * 2)
      }
      logger.info(msg + "ready")
    }
    null
  }

  def nextStep(msg: String, stepFn: () => Unit): TaskSteps = {
    steps = steps :+ (msg, stepFn)
    this
  }
}