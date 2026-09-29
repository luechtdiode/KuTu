package ch.seidel.commons

import ch.seidel.javafx.JavaFxTestBase
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SortUtilsTest extends AnyWordSpec
  with Matchers
  with JavaFxTestBase {

  "SortUtils" should {
    "correctly compare strings ignoring case" in {
      val compare = SortUtils.sortKeys

      compare("apple", "Banana") should be(true)
      compare("Banana", "apple") should be(false)
      compare("cherry", "cherry") should be(false)
      compare("Date", "date") should be(false)
      compare("fig", "Fig") should be(false)
      compare("Grape", "grape") should be(false)
      compare("kiwi", "Kiwi") should be(false)
      compare("Lemon", "Lime") should be(true)
      compare("Mango", "Mango") should be(false)
      compare("nectarine", "Nectarine") should be(false)
    }

    "correctly sort a list of strings ignoring case" in {
      val strings = List("banana", "Apple", "cherry", "date", "Fig", "grape")
      val sortedStrings = strings.sortWith(SortUtils.sortKeys)

      sortedStrings should equal(List("Apple", "banana", "cherry", "date", "Fig", "grape"))
    }

    "correctly sort TreeItems by their values ignoring case" in {
      import scalafx.scene.control.TreeItem

      // TreeItems sind JavaFX-Knoten und müssen auf dem FX-Thread erzeugt werden.
      val sortedValues = onFxThread {
        val items = List(
          new TreeItem[String]("banana"),
          new TreeItem[String]("Apple"),
          new TreeItem[String]("cherry")
        )
        items.sortWith(SortUtils.treeItemSort).map(_.value())
      }

      sortedValues should equal(List("Apple", "banana", "cherry"))
    }

    "correctly sort KuTuAppThumbNails by their button text ignoring case" in {
      import ch.seidel.kutu.KuTuAppThumbNail
      import scalafx.scene.control.Button
      import scalafx.scene.control.TreeItem

      val sortedTexts = onFxThread {
        val items = List(
          new TreeItem[String]("banana"),
          new TreeItem[String]("Apple"),
          new TreeItem[String]("cherry")
        )
        val thumbnails = items.map(treeitem => KuTuAppThumbNail("context", new Button(treeitem.getValue), treeitem))

        thumbnails.sortWith(SortUtils.thumbNailsSort).map(_.button.text())
      }

      sortedTexts should equal(List("Apple", "banana", "cherry"))
    }
  }
}
