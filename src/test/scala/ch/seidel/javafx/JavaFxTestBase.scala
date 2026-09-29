package ch.seidel.javafx

import javafx.application.Platform
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
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
          try {
            Platform.startup(() => latch.countDown())
            latch.await()
          } catch {
            case _: IllegalStateException =>
              // JavaFX toolkit already initialized
          }
          toolkitInitialized = true
        }
      }
    }
  }

  /**
   * Führt den Block auf dem JavaFX-Application-Thread aus und wartet auf das Ergebnis.
   *
   * JavaFX-Knoten (Buttons, Labels, Controls) dürfen nur auf dem FX-Thread erzeugt werden. Werden
   * sie vom ScalaTest-Thread erzeugt, während der FX-Thread gerade CSS-Klassen initialisiert,
   * entsteht ein Deadlock zwischen den Klassen-Initialisierungen von
   * StyleableObjectProperty und StyleablePropertyHelper - der Testlauf hängt dann ohne jede
   * Ausgabe. Deshalb werden alle Knoten-Erzeugungen in FX-Tests hierüber geleitet.
   */
  def onFxThread[A](body: => A): A = {
    if Platform.isFxApplicationThread then body
    else {
      val noResult = new AnyRef
      val outcome = new AtomicReference[Any](noResult)
      val failure = new AtomicReference[Throwable](null)
      val latch = new CountDownLatch(1)
      Platform.runLater(() => {
        try {
          outcome.set(body.asInstanceOf[Any])
        } catch {
          case e: Throwable => failure.set(e)
        } finally {
          latch.countDown()
        }
      })
      // Der FX-Thread darf hier nicht auf den Test-Thread warten, also endlos warten.
      latch.await()
      val error = failure.get()
      if error != null then throw error
      val result = outcome.get()
      // ScalaTest 3.3 hat kein einstufiges apply fuer TestFailedException, daher RuntimeException.
      if result eq noResult then throw new RuntimeException("Der FX-Block hat kein Ergebnis geliefert.")
      result.asInstanceOf[A]
    }
  }
}
