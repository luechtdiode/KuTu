package ch.seidel.kutu.view

import ch.seidel.kutu.domain.Verein
import scalafx.scene.control._
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.collections.ObservableBuffer

object VereinTableView {
  def apply(vereinList: List[Verein]) = new VereinTableView(vereinList)
}

class VereinTableView(vereinList: List[Verein]) extends TableView[Verein] {
  val filteredModel: ObservableBuffer[Verein] = ObservableBuffer.from(vereinList)
  items = filteredModel
  columns ++= List(
    new TableColumn[Verein, String] {
      text = "Verein"
      cellValueFactory = { x =>
        new ReadOnlyStringWrapper(x.value, "verein", {
          s"${x.value.name}"
        })
      }
    },
    new TableColumn[Verein, String] {
      text = "Verband"
      cellValueFactory = { x =>
        new ReadOnlyStringWrapper(x.value, "verband", {
          x.value.verband.getOrElse("")
        })
      }
    }
  )
  selectionModel.value.setSelectionMode(SelectionMode.Single)
  val filter: TextField = new TextField() {
    promptText = "Such-Text"
    text.addListener { (_, _, newVal: String) =>
      val sortOrderList = sortOrder.toList;
      filteredModel.clear()
      val searchQuery = newVal.toUpperCase().split(" ")
      for {verein <- vereinList
           } {
        val matches = searchQuery.forall { search =>
          if (search.isEmpty || verein.easyprint.toUpperCase().contains(search)) {
            true
          }
          else {
            false
          }
        }

        if (matches) {
          filteredModel.add(verein)
        }
      }
      sortOrder.clear()
      sortOrder ++= sortOrderList
    }
  }

}
