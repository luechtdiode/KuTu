package ch.seidel.javafx

import javafx.application.Platform
import java.util.concurrent.CountDownLatch
import org.scalatest.{BeforeAndAfterAll, Suite}

trait JavaFxTestBase extends BeforeAndAfterAll { this: Suite =>

  @volatile private var toolkitInitialized = false

  override def beforeAll(): Unit = {
    super.beforeAll()
    initializeJavaFxToolkit()
  }

  private def initializeJavaFxToolkit(): Unit = {
    if (!toolkitInitialized) {
      synchronized {
        if (!toolkitInitialized) {
          val latch = new CountDownLatch(1)
          Platform.startup(() => latch.countDown())
          latch.await()
          toolkitInitialized = true
        }
      }
    }
  }
}
