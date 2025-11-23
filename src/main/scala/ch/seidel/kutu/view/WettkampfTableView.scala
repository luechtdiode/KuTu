package ch.seidel.kutu.view

import ch.seidel.kutu.domain.WettkampfView
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.*

object WettkampfTableView {
  def apply(wklviews: List[WettkampfView]) = new WettkampfTableView(wklviews)
}

class WettkampfTableView(wklviews: List[WettkampfView]) extends TableView[WettkampfView] {
  val filteredModel: ObservableBuffer[WettkampfView] = ObservableBuffer.from(wklviews)
  items = filteredModel
  columns ++= List(
      new TableColumn[WettkampfView, String] {
        text = "Datum"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "datum", {
            s"${x.value.datum}"
          })
        }
        //minWidth = 150
      },
      new TableColumn[WettkampfView, String] {
        text = "Titel"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "titel", {
            s"${x.value.titel}"
          })
        }
      },
      new TableColumn[WettkampfView, String] {
        text = "Details"
        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "details", {
            x.value.details
          })
        }
      }
    )
  selectionModel.value.setSelectionMode(SelectionMode.Single)
  val filter: TextField = new TextField() {
    promptText = "Such-Text"
    text.addListener { (_, _, newVal: String) =>
      val sortOrderList = sortOrder.toList
      filteredModel.clear()
      val searchQuery = newVal.toUpperCase().split(" ")
      for wettkampf <- wklviews
           do {
        val matches = searchQuery.forall { search =>
          if search.isEmpty || wettkampf.easyprint.toUpperCase().contains(search) then {
            true
          }
          else {
            false
          }
        }

        if matches then {
          filteredModel.add(wettkampf)
        }
      }
      sortOrder.clear()
      val restored = sortOrder ++= sortOrderList
    }
  }

}
