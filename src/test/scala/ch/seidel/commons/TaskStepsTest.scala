package ch.seidel.commons

import ch.seidel.javafx.JavaFxTestBase
import javafx.application.Platform
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TaskStepsTest extends AnyWordSpec
  with Matchers
  with JavaFxTestBase {

  "TaskSteps" should {
    "correctly track progress through steps" in {
      val steps = TaskSteps("Test Task")
      val latch = new java.util.concurrent.CountDownLatch(4)
      var lastMessage: String = ""
      steps.messageProperty().addListener((_, _, newValue) =>
        println(s"Message changed to: $newValue")
        lastMessage = newValue
        if (newValue.endsWith("done (3 steps)")) latch.countDown()
      )
      steps
        .nextStep("Step 1", () => {
          println(steps.getProgress)
          latch.countDown()
        })
        .nextStep("Step 2", () => {
          println(steps.getProgress)
          latch.countDown()
        })
        .nextStep("Step 3", () => {
          println(steps.getProgress)
          latch.countDown()
        })

      Platform.runLater(steps)
      latch.await()
      
      lastMessage should be("Test Task done (3 steps)")
    }
  }
}
