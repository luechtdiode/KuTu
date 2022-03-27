package ch.seidel.kutu.view

import java.util.UUID
import ch.seidel.commons._
import ch.seidel.kutu.domain._
import javafx.scene.{control => jfxsc}
import javafx.util.Callback
import scalafx.Includes._
import scalafx.beans.property.{ReadOnlyStringProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.event.subscriptions.Subscription
import scalafx.scene.Node
import scalafx.scene.control.TableColumn._
import scalafx.scene.control._
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout._
import scalafx.util.converter.DefaultStringConverter

class WettkampfZeitenTab(editableProperty: Boolean, wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService {

  var subscription: List[Subscription] = List.empty

  override def release: Unit = {
    subscription.foreach(_.cancel())
    subscription = List.empty
  }

  private var lazypane: Option[LazyTabPane] = None

  def setLazyPane(pane: LazyTabPane): Unit = {
    lazypane = Some(pane)
  }

  def refreshLazyPane(): Unit = {
    lazypane match {
      case Some(pane) => pane.refreshTabs()
      case _ =>
    }
  }

  override def isPopulated: Boolean = {

    val planTimeViews = service
      .loadWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
      .map { a => ZeitenEditor(a) }
    val model = ObservableBuffer.from(planTimeViews)

    val cols: List[jfxsc.TableColumn[ZeitenEditor, _]] = classOf[ZeitenEditor].getDeclaredFields.filter { f =>
      f.getType.equals(classOf[ReadOnlyStringProperty])
    }.map { field =>
      field.setAccessible(true)
      val tc: jfxsc.TableColumn[ZeitenEditor, String] = new TableColumn[ZeitenEditor, String] {
        text = field.getName.take(1).toUpperCase() + field.getName.drop(1)
        cellValueFactory = { x =>
          field.get(x.value).asInstanceOf[ReadOnlyStringProperty]
        }
        styleClass += "table-cell-with-value"
        prefWidth = ZeitenEditor.coldef(field.getName)
        editable = false
      }
      tc
    }.toList ++
      classOf[ZeitenEditor].getDeclaredFields.filter { f =>
        f.getType.equals(classOf[StringProperty])
      }.map { field =>
        field.setAccessible(true)
        val tc: jfxsc.TableColumn[ZeitenEditor, String] = new TableColumn[ZeitenEditor, String] {
          text = field.getName.take(1).toUpperCase() + field.getName.drop(1)
          cellValueFactory = { x =>
            field.get(x.value).asInstanceOf[StringProperty]
          }
          cellFactory.value = { _:Any => new AutoCommitTextFieldTableCell[ZeitenEditor, String](new DefaultStringConverter()) }
          styleClass += "table-cell-with-value"
          prefWidth = ZeitenEditor.coldef(field.getName)
          editable = editableProperty
          onEditCommit = (evt: CellEditEvent[ZeitenEditor, String]) => {
            field.get(evt.rowValue).asInstanceOf[StringProperty].value = evt.newValue
            val rowIndex = model.indexOf(evt.rowValue)
            val toSave = evt.rowValue.commit
            service.updateWettkampfPlanTimeView(toSave.toWettkampfPlanTimeRaw)
            model.update(rowIndex, new ZeitenEditor(toSave))
            evt.tableView.selectionModel.value.select(rowIndex, this)
            evt.tableView.requestFocus()
          }
        }
        tc
      }.toList

    val zeitenView = new TableView[ZeitenEditor](model) {
      columns ++= cols
      id = "time-table"
      editable = true
    }

    var lastFilter: String = ""

    def updateFilteredList(newVal: String): Unit = {
      lastFilter = newVal
      val sortOrder = zeitenView.sortOrder.toList
      model.clear()
      val searchQuery = newVal.toUpperCase().split(" ")
      for {planTime <- planTimeViews} {
        val matches =
          searchQuery.forall { search =>
            search.isEmpty ||
              planTime.kategorie.value.toUpperCase().contains(search) ||
              planTime.disziplin.value.toUpperCase().contains(search)
          }

        if (matches) {
          model += planTime
        }
      }
      zeitenView.sortOrder.clear()
      zeitenView.sortOrder ++= sortOrder
    }

    val txtFilter = new TextField() {
      promptText = "Filter (Ctrl + F)"
      text.addListener { (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
        if (!lastFilter.equalsIgnoreCase(newVal)) {
          updateFilteredList(newVal)
        }
      }
    }
    zeitenView.selectionModel.value.setCellSelectionEnabled(true)
    zeitenView.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(zeitenView, double = false, txtFilter)(ke)
    }

    text = "Planzeiten"
    content = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      top = new ToolBar {
        content = List(
          new Label {
            text = s"Filter"
            maxWidth = Double.MaxValue
            minHeight = Region.USE_PREF_SIZE
            styleClass += "toolbar-header"
          },
          txtFilter
        )
      }
      center = new VBox {
        children.add(
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = zeitenView
          }.asInstanceOf[Node]
        )
      }
    }
    true
  }
}