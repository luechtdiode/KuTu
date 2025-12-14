package ch.seidel.javafx.webview

import ch.seidel.javafx.JavaFxTestBase
import com.sun.webkit.dom.MouseEventImpl
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker
import javafx.scene.web.WebView
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.w3c.dom.events.EventTarget
import org.w3c.dom.{Document, Node, NodeList}
import scalafx.beans.property
import scalafx.beans.property.ReadOnlyObjectProperty

import java.util.concurrent.CountDownLatch

class HyperLinkRedirectListenerTest extends AnyWordSpec
  with Matchers
  with JavaFxTestBase {

  "HyperLinkRedirectListener" should {
    "be instantiated without errors" in {
      val latch = new CountDownLatch(1)
      Platform.runLater(() => {
        val webview = new WebView()
        val listener = new HyperLinkRedirectListener(webview)
        listener should not be null
        latch.countDown()
      })
      latch.await()
    }

    "listener should handle changed" in {
      val latch = new CountDownLatch(1)
      Platform.runLater(() => {
        val webview = new WebView()
        val listener = new HyperLinkRedirectListener(webview)
        val property = new ReadOnlyObjectProperty[Worker.State](null, "state", Worker.State.SCHEDULED)
        listener.changed(property, Worker.State.SCHEDULED, Worker.State.READY)
        latch.countDown()
      })
      latch.await()
    }

    "listener should handle clicked links" in {
      val latch = new CountDownLatch(1)
      val latch2 = new CountDownLatch(1)
      var targetUrl: String = null
      var webview: WebView = null
      var eventTarget: EventTarget = null
      Platform.runLater(() => {
        webview = new WebView()
        val listener = new HyperLinkRedirectListener(webview) {
          override def changed(observable: ObservableValue[? <: Worker.State], oldValue: Worker.State, newValue: Worker.State): Unit = {
            super.changed(observable, oldValue, newValue)
            if (newValue == Worker.State.SUCCEEDED) {
              val document = webview.getEngine.getDocument
              val anchors = document.getElementsByTagName("a")
              for (i <- 0 until anchors.getLength) {
                val node = anchors.item(i)
                eventTarget = node.asInstanceOf[EventTarget]
                latch.countDown()
              }
            }
          }
          override def openLinkInSystemBrowser(url: String): Unit = {
            println(s"openLinkInSystemBrowser called with URL: $url")
            targetUrl = url
            latch2.countDown()
          }
        }
        webview.getEngine.getLoadWorker.stateProperty.addListener(listener)
        webview.getEngine.loadContent("""<html><body><a href="http://example.com">Example</a></body></html>""")
      })
      latch.await()

      Platform.runLater(() => {
        import org.w3c.dom.events.MouseEvent as DOMMouseEvent
        val document = webview.getEngine.getDocument
        val event: DOMMouseEvent = document.asInstanceOf[org.w3c.dom.events.DocumentEvent]
          .createEvent("MouseEvents")
          .asInstanceOf[DOMMouseEvent]

        event.initMouseEvent(
          "click",        // type
          true,           // canBubble
          true,           // cancelable
          null,           // view
          1,              // detail
          0, 0,           // screenX, screenY
          0, 0,           // clientX, clientY
          false,          // ctrlKey
          false,          // altKey
          false,          // shiftKey
          false,          // metaKey
          0,              // button
          eventTarget     // relatedTarget
        )

        eventTarget.dispatchEvent(event)
      })
      latch2.await()
      targetUrl should be("http://example.com/")
    }
  }


}
