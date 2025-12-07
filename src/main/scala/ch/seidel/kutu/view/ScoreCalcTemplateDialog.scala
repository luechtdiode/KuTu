package ch.seidel.kutu.view

import ch.seidel.commons.*
import ch.seidel.kutu.calc.ScoreAggregateFn
import ch.seidel.kutu.domain
import ch.seidel.kutu.view.ScoreCalcTemplatedialog.addVariableIcon
import javafx.scene.control.TextFormatter
import org.controlsfx.validation.ValidationSupport
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{IntegerProperty, ReadOnlyIntegerProperty}
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.*

import java.util.function.UnaryOperator

case class TemplateFormular(working: WertungEditor) extends VBox {
  //val working = init.copy(init = init.init)
  val caption: String = if working.init.athlet.id > 0 then s"Notenerfassung für ${working.init.wettkampfdisziplin.disziplin.name} - ${working.init.athlet.easyprint}" else s"Testformular für ${working.init.wettkampfdisziplin.disziplin.name}"
  spacing = 7.0
  minWidth = 600
  working.variableEditorsList.zipWithIndex.foreach(vs => {
    children.add(new Label(s"${vs._2 + 1}. Übung"))
    children.add(new HBox {
      spacing = 5.0
      vs._1.foreach(v => {
        children.addAll(new Label(v.score.value.name), new TextField() {
          text <==> v.stringvalue
          prefWidth = 80
        })
      })
    })
  })
  children.add(new Label("Berechnete Werte"))
  if working.init.defaultVariables.get.dVariables.nonEmpty then {
    children.addAll(new Label() {
      text <== Bindings.createStringBinding(() => s"${working.init.wettkampfdisziplin.notenSpez.getDifficultLabel}-Note ${working.dFormula.value}", working.calculatedWertung)
    }, new TextField() {
      text <== Bindings.createStringBinding(() => working.calculatedWertung.value.noteDasText, working.calculatedWertung, working.dFormula)
      editable = false
      prefWidth = 100
    })
  }
  if working.init.defaultVariables.get.eVariables.nonEmpty then {
    children.addAll(new Label() {
      text <== Bindings.createStringBinding(() => s"${working.init.wettkampfdisziplin.notenSpez.getExecutionLabel}-Note ${working.eFormula.value}", working.calculatedWertung)
    }, new TextField() {
      text <== Bindings.createStringBinding(() => working.calculatedWertung.value.noteEasText, working.calculatedWertung, working.eFormula)
      prefWidth = 100
      editable = false
    })
  }
  children.addAll(new Label("Endnote"), new TextField() {
    text <== Bindings.createStringBinding(() => working.calculatedWertung.value.endnoteAsText, working.calculatedWertung)
    prefWidth = 100
    editable = false
  })
}

object ScoreCalcTemplatedialog {
  def apply(service: ScoreCalcTempateEditorService, onSelected: ScoreCalcTemplateEditor => Unit) =
    new ScoreCalcTemplatedialog("Neu erfassen ...", service.newEditor(), onSelected)

  def apply(editor: ScoreCalcTemplateEditor, actionTitle: String, onSelected: ScoreCalcTemplateEditor => Unit) =
    new ScoreCalcTemplatedialog(actionTitle, editor.copy(init = editor.init), onSelected)

  var addVariableIcon: Image = null
  try {
    addVariableIcon = new Image(getClass.getResourceAsStream("/images/inplace-add-variable.png"))
  } catch {
    case e: Exception => e.printStackTrace()
  }
}

class FormulaCatchingCaretPositionFilter extends UnaryOperator[TextFormatter.Change] {
  private val _caretPosition = new IntegerProperty()
  val caretPosition: ReadOnlyIntegerProperty = new ReadOnlyIntegerProperty(_caretPosition)
  def apply(change: TextFormatter.Change): TextFormatter.Change = {
    if change.getControl.isFocused then {
      _caretPosition.value = change.getCaretPosition
    }
    change
  }
}

class ScoreCalcTemplatedialog(actionTitle: String, templateEditor: ScoreCalcTemplateEditor, onSelected: ScoreCalcTemplateEditor => Unit) {
  private val scoreCalcTemplateEditor = templateEditor
  val disziplinList: List[domain.Disziplin] = scoreCalcTemplateEditor.context.disziplinList
  private val wettkampfDisziplinList = scoreCalcTemplateEditor.context.wettkampfdisziplinViews

  private val lblDisziplin = new Label {
    text = "Disziplin"
  }
  private val cmbDisziplin = new ComboBox[String] {
    disable <== when(scoreCalcTemplateEditor.editable) choose false otherwise true
    hgrow = Priority.Always
    items.value.add("")
    disziplinList.foreach(d => {
      items.value.add(d.easyprint)
    })
    value <==> scoreCalcTemplateEditor.disziplin
  }

  private val lblWKDisziplin = new Label {
    text = "Kategorie-Disziplin"
  }
  private val cmbWKDisziplin = new ComboBox[String] {
    disable <== when(scoreCalcTemplateEditor.editable) choose false otherwise true
    hgrow = Priority.Always
    items.value.add("")
    wettkampfDisziplinList.foreach(d => {
      items.value.add(d.easyprint)
    })
    value <==> scoreCalcTemplateEditor.kategoriedisziplin
  }

  private val lblAggregatFn = new Label {
    text = "Aggregat Funktion"
  }
  private val cmbAggregatFn = new ComboBox[String] {
    disable <== when(scoreCalcTemplateEditor.editable) choose false otherwise true
    hgrow = Priority.Always
    items.value.add("")
    ScoreAggregateFn.values.foreach(v => items.value.add(v.toString))
    value <==> scoreCalcTemplateEditor.aggregateFn
  }

  private val dCaretFilter = new FormulaCatchingCaretPositionFilter
  private val eCaretFilter = new FormulaCatchingCaretPositionFilter
  private val pCaretFilter = new FormulaCatchingCaretPositionFilter

  private val txtdFormula = new TextField {
    editable <== scoreCalcTemplateEditor.editable
    hgrow = Priority.Always
    prefWidth = 150
    promptText = "Formel für die D-Note/A-Note"
    textFormatter = new TextFormatter[String](dCaretFilter)
    text <==> scoreCalcTemplateEditor.dFormula
  }
  private val lbldFormula = new Label(txtdFormula.promptText.value)

  private val txteFormula = new TextField {
    editable <== scoreCalcTemplateEditor.editable
    hgrow = Priority.Always
    prefWidth = 150
    promptText = "Formel für die E-Note/B-Note"
    textFormatter = new TextFormatter[String](eCaretFilter)
    text <==> scoreCalcTemplateEditor.eFormula
  }

  private val txtpFormula = new TextField {
    editable <== scoreCalcTemplateEditor.editable
    hgrow = Priority.Always
    prefWidth = 150
    promptText = "Formel Penalty"
    textFormatter = new TextFormatter[String](pCaretFilter)
    text <==> scoreCalcTemplateEditor.pFormula
  }

  val txtValidState: Label = new Label() {
    hgrow = Priority.Always
    text <==> scoreCalcTemplateEditor.validState
  }

  private val txtdValidState = new Label() {
    hgrow = Priority.Always
    text <==> scoreCalcTemplateEditor.dvalidState
  }

  private val txteValidState = new Label() {
    hgrow = Priority.Always
    text <==> scoreCalcTemplateEditor.evalidState
  }

  private val txtpValidState = new Label() {
    hgrow = Priority.Always
    text <==> scoreCalcTemplateEditor.pvalidState
  }

  val box: BorderPane = new BorderPane() {
    margin = Insets(10)
    top = new Label("Vorschau") {
      style = "-fx-font-size: 1.2em;-fx-font-weight: bold;-fx-padding: 8px 0 2px 0;-fx-text-fill: #0072aa;"
      styleClass += "toolbar-header"
    }
  }

  private val boxContainer = new BorderPane() {
    style = "-fx-border-color: -fx-focus-color, blue;-fx-border-radius: 10.0;"
    margin = Insets(10)
    center = box
  }

  private val insertDVariable = new Button("", new ImageView { image = addVariableIcon }) {
    //tooltip.value.text = "Variabel einfügen"
    disable <== when(scoreCalcTemplateEditor.editable) choose false otherwise true
    onAction = { ae =>
      val index = scoreCalcTemplateEditor.commit.dVariables.length + 1
      val wkd = scoreCalcTemplateEditor.context.wettkampfdisziplinViews.head
      val dVariable = s"$$${wkd.notenSpez.getDifficultLabel}${wkd.notenSpez.getDifficultLabel} Wert $index.2"
      txtdFormula.insertText(dCaretFilter.caretPosition.value, dVariable)
    }
  }
  private val insertEVariable = new Button("", new ImageView { image = addVariableIcon }) {
    //tooltip.value.text = "Variabel einfügen"
    disable <== when(scoreCalcTemplateEditor.editable) choose false otherwise true
    onAction = { ae =>
      val index = scoreCalcTemplateEditor.commit.eVariables.length + 1
      val wkd = scoreCalcTemplateEditor.context.wettkampfdisziplinViews.head
      val eVariable = s"$$${wkd.notenSpez.getExecutionLabel}${wkd.notenSpez.getExecutionLabel} Wert $index.2"
      txteFormula.insertText(eCaretFilter.caretPosition.value, eVariable)
    }
  }
  private val insertPVariable = new Button("", new ImageView { image = addVariableIcon }) {
    //tooltip.value.text = "Variabel einfügen"
    disable <== when(scoreCalcTemplateEditor.editable) choose false otherwise true
    onAction = { ae =>
      val index = scoreCalcTemplateEditor.commit.pVariables.length + 1
      txtpFormula.insertText(pCaretFilter.caretPosition.value, s"$$PPenalty $index.2")
    }
  }

  val panel: GridPane = new GridPane() {
    alignment = Pos.Center
    hgrow = Priority.Always
    vgrow = Priority.Always
    minWidth = 350

    hgap = 10
    vgap = 10
    //    padding = Insets(5, 5, 5, 5)
    add(lblDisziplin, 0, 0)
    add(cmbDisziplin, 1, 0)
    add(lblWKDisziplin, 0, 1)
    add(cmbWKDisziplin, 1, 1)

    add(lbldFormula, 0, 2)
    add(TextFieldWithToolButton(txtdFormula, insertDVariable).asFormularMode(), 0, 3, 2, 1)
    add(txtdValidState, 0, 4, 2, 1)

    add(new Label(txteFormula.promptText.value), 0, 5)
    add(TextFieldWithToolButton(txteFormula, insertEVariable).asFormularMode(), 0, 6, 2, 1)
    add(txteValidState, 0, 7, 2, 1)

    add(new Label(txtpFormula.promptText.value), 0, 8)
    add(TextFieldWithToolButton(txtpFormula, insertPVariable).asFormularMode(), 0, 9, 2, 1)
    add(txtpValidState, 0, 10, 2, 1)

    add(lblAggregatFn, 0, 11)
    add(cmbAggregatFn, 1, 11)
    add(boxContainer, 4, 0, 4, 12)
    //add(txtValidState, 0, 12, 2, 1)
  }

  val btnOK: Button = new Button("OK") {
    onAction = (event: ActionEvent) => {
      onSelected(
        templateEditor.context.updated(templateEditor.commit)
      )
    }
  }

  btnOK.disable.value = true
  updatePreview()

  def changeHandler(): Unit = {
    updatePreview()
    btnOK.disable.value = !scoreCalcTemplateEditor.isValid
  }

  private def updatePreview(): Unit = {
    println("updatePreview")
    val wkv = scoreCalcTemplateEditor.previewWertung()
    val visible = wkv.init.wettkampfdisziplin.isDNoteUsed
    lbldFormula.visible.value = visible
    txtdFormula.visible.value = visible
    txtdValidState.visible.value = visible
    insertDVariable.visible.value = visible
    box.center = TemplateFormular(wkv)
  }

  val validationSupport = new ValidationSupport
  validationSupport.registerValidator(txteFormula, false, templateEditor.createValidator)
  validationSupport.registerValidator(txtdFormula, false, templateEditor.createValidator)
  validationSupport.registerValidator(txtpFormula, false, templateEditor.createValidator)

  scoreCalcTemplateEditor.disziplin.onChange(changeHandler())
  scoreCalcTemplateEditor.kategoriedisziplin.onChange(changeHandler())
  scoreCalcTemplateEditor.validState.onChange(changeHandler())
  scoreCalcTemplateEditor.dvalidState.onChange(changeHandler())
  scoreCalcTemplateEditor.evalidState.onChange(changeHandler())
  scoreCalcTemplateEditor.pvalidState.onChange(changeHandler())
  scoreCalcTemplateEditor.aggregateFn.onChange(changeHandler())

  def execute(): Unit = {
    PageDisplayer.showInDialogFromRoot(actionTitle, new DisplayablePage() {
      def getPage: Node = {
        new BorderPane {
          hgrow = Priority.Always
          vgrow = Priority.Always
          //          top = filter
          center = panel
          //          minWidth = 350
        }
      }
    }, btnOK)
  }
}