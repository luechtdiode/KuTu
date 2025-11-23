package ch.seidel.kutu.view

import ch.seidel.commons.*
import ch.seidel.kutu.Config.{homedir, remoteHostOrigin}
import ch.seidel.kutu.domain.*
import javafx.scene.control as jfxsc
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{ReadOnlyStringProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.event.subscriptions.Subscription
import scalafx.scene.Node
import scalafx.scene.control.TableColumn.*
import scalafx.scene.control.*
import scalafx.scene.input.{KeyEvent, MouseEvent}
import scalafx.scene.layout.*
import scalafx.util.converter.DefaultStringConverter

import java.util
import scala.jdk.CollectionConverters.IterableHasAsJava
import ch.seidel.kutu.domain.given_Conversion_String_Int

class ScoreCalcTemplatesTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService {

  var subscription: List[Subscription] = List.empty
  closable = false


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

  val model: ObservableBuffer[ScoreCalcTemplateEditor] = ObservableBuffer.from(List.empty[ScoreCalcTemplateEditor])
  val scoreCalcTemplateEditors: util.Collection[ScoreCalcTemplateEditor] = new util.ArrayList[ScoreCalcTemplateEditor]()
  val context = ScoreCalcTempateEditorService(wettkampf, service)

  def reloadData(): Unit = {
    scoreCalcTemplateEditors.clear()
    scoreCalcTemplateEditors.addAll(context.loadEditors().asJavaCollection)
    model.setAll(scoreCalcTemplateEditors)
  }

  onSelectionChanged = _ => {
    if selected.value then {
      reloadData()
    }
  }
  override def isPopulated: Boolean = {
    val sorter: ScoreCalcTemplateEditor => Int = editor => editor.init.sortOrder

    val cols: List[jfxsc.TableColumn[ScoreCalcTemplateEditor, ?]] = classOf[ScoreCalcTemplateEditor].getDeclaredFields.filter { f =>
      f.getType.equals(classOf[ReadOnlyStringProperty]) && ScoreCalcTemplateEditor.coldef.contains(f.getName)
    }.map { field =>
      field.setAccessible(true)
      val tc: jfxsc.TableColumn[ScoreCalcTemplateEditor, String] = new TableColumn[ScoreCalcTemplateEditor, String] {
        text = field.getName.take(1).toUpperCase() + field.getName.drop(1)
        cellValueFactory = { x =>
          field.get(x.value).asInstanceOf[ReadOnlyStringProperty]
        }
        styleClass += "table-cell-with-value"
        prefWidth = ScoreCalcTemplateEditor.coldef(field.getName)
        editable = false
      }
      tc
    }.toList ++
      classOf[ScoreCalcTemplateEditor].getDeclaredFields.filter { f =>
        f.getType.equals(classOf[StringProperty]) && ScoreCalcTemplateEditor.coldef.contains(f.getName)
      }.map { field =>
        field.setAccessible(true)
        val tc: jfxsc.TableColumn[ScoreCalcTemplateEditor, String] = new TableColumn[ScoreCalcTemplateEditor, String] {
          text = field.getName.take(1).toUpperCase() + field.getName.drop(1)
          cellValueFactory = { x =>
            field.get(x.value).asInstanceOf[StringProperty]
          }
          cellFactory.value = { (_: Any) => new AutoCommitTextFieldTableCell[ScoreCalcTemplateEditor, String](new DefaultStringConverter()) }
          styleClass += "table-cell-with-value"
          prefWidth = ScoreCalcTemplateEditor.coldef(field.getName)
          editable = false
        }
        tc
      }.toList

    class ScoreCalcTemplateView extends TableView[ScoreCalcTemplateEditor](model) {
      columns ++= cols
      id = "score-calc-template-table"
      editable = true
      selectionModel.value.setCellSelectionEnabled(false)

      override def edit(row: Int, column: TableColumn[ScoreCalcTemplateEditor, ?]): Unit = {
        editDialog(row)
      }

      def editDialog(row: Int): Unit = {
        if row < 0 || row >= model.size then {
          ScoreCalcTemplatedialog(context, editor => {
            model.add(editor.context.updated(editor.commit))
            model.sortBy(sorter)
            delegate.requestFocus()
          }).execute()
        } else {
          val selectedEditor = selectionModel.value.selectedItemProperty().value
          ScoreCalcTemplatedialog(selectedEditor, "Bearbeiten", editor => {
            model.update(row, editor.context.updated(editor.commit))
            model.sortBy(sorter)
            delegate.requestFocus()
          }).execute()
        }
      }
    }

    val scoreCalcTemplatesView: ScoreCalcTemplateView = new ScoreCalcTemplateView()
    val newButton = new Button("Neues Formular ...") {
      disable <== when(Bindings.createBooleanBinding(() => {
        wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)
      },
        scoreCalcTemplatesView.selectionModel.value.getSelectedItems
      )) choose true otherwise false
      onAction = { _ =>
        scoreCalcTemplatesView.editDialog(-1)
      }
    }

    val editButton = new Button("Formular bearbeiten ...") {
      disable <== when(Bindings.createBooleanBinding(() => {
        wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) || scoreCalcTemplatesView.selectionModel.value.isEmpty
      },
        scoreCalcTemplatesView.selectionModel.value.getSelectedItems
      )) choose true otherwise false

      onAction = { _ =>
        scoreCalcTemplatesView.editDialog(scoreCalcTemplatesView.selectionModel.value.getSelectedIndex)
      }
    }

    val deleteButton = new Button("Formular löschen") {
      private val selection: TableView.TableViewSelectionModel[ScoreCalcTemplateEditor] = scoreCalcTemplatesView.selectionModel.value
      disable <== when(Bindings.createBooleanBinding(() => {
        wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) || selection.isEmpty || (selection.getSelectedItem == null || !selection.getSelectedItem.isEditable)
      },
        selection.getSelectedItems
      )) choose true otherwise false

      onAction = { _ =>
        val item = selection.getSelectedItem
        if item != null && item.isEditable then {
          context.delete(item)
          model.remove(item)
        }
      }
    }
    var lastFilter: String = ""

    def updateFilteredList(newVal: String): Unit = {
      lastFilter = newVal
      val sortOrder = scoreCalcTemplatesView.sortOrder.toList
      model.clear()
      val searchQuery = newVal.toUpperCase().split(" ")
      scoreCalcTemplateEditors.stream().forEach(template => {
        val matches =
          searchQuery.forall { search =>
            search.isEmpty ||
              template.dFormula.value.toUpperCase().contains(search) ||
              template.eFormula.value.toUpperCase().contains(search) ||
              template.pFormula.value.toUpperCase().contains(search) ||
              template.kategoriedisziplin.value.toUpperCase().contains(search) ||
              template.disziplin.value.toUpperCase().contains(search)
          }

        if matches then {
          model += template
        }
      })
      scoreCalcTemplatesView.sortOrder.clear()
      scoreCalcTemplatesView.sortOrder ++= sortOrder
    }

    val txtFilter = new TextField() {
      promptText = "Filter (Ctrl + F)"
        text.addListener { (o: javafx.beans.value.ObservableValue[? <: String], oldVal: String, newVal: String) =>
        if !lastFilter.equalsIgnoreCase(newVal) then {
          updateFilteredList(newVal)
        }
      }
    }
      val defaultKeyActionHandler: KeyEvent => Unit = (ke: KeyEvent) => AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(scoreCalcTemplatesView, double = false, txtFilter)(ke)
      val defaultMouseActionHandler: MouseEvent => Unit = (me: MouseEvent) => AutoCommitTextFieldTableCell.handleDefaultEditingMouseEvents(scoreCalcTemplatesView, double = false, txtFilter)(me)
      scoreCalcTemplatesView.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) => defaultKeyActionHandler(ke) }
      scoreCalcTemplatesView.filterEvent(MouseEvent.MouseClicked) { (me: MouseEvent) => defaultMouseActionHandler(me) }

    text = "Noternerfassung Formulare"
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
          txtFilter,
          newButton,
          editButton,
          deleteButton
        )
      }
      center = new VBox {
        children.add(
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = scoreCalcTemplatesView
          }.asInstanceOf[Node]
        )
      }
    }
    true
  }
}