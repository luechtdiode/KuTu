package ch.seidel.kutu.view

import ch.seidel.kutu.domain.Gleichstandsregel
import javafx.scene.control as jfxsc
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, Priority, VBox}

import scala.collection.mutable.ArrayBuffer

object PunktegleichstandsregelFieldEditorDialog {
  private val RuleTypes = Seq(
    "Ohne",
    "E-Note-Summe",
    "E-Note-Best",
    "D-Note-Summe",
    "D-Note-Best",
    "JugendVorAlter",
    "Disziplin",
    "StreichDisziplin",
    "StreichWertungen"
  )
  private val StreichWertungenTypen = Seq("Endnote", "E-Note", "D-Note")
  private val MinMaxTypen = Seq("Min", "Max")

  private val DisziplinPattern = "^Disziplin\\((.+)\\)$".r
  private val StreichDisziplinPattern = "^StreichDisziplin\\((.+)\\)$".r
  private val StreichWertungPattern = "^StreichWertungen\\((Endnote|E-Note|D-Note)(,(Min|Max))*\\)$".r

  private case class StructuredRule(ruleType: String, disziplinen: List[String], typ: String, minMax: String) {
    def summary: String = ruleToFormula(this)
  }

  def edit(initialFormula: String, title: String = "Punktegleichstandsregel bearbeiten", availableDisziplinen: Seq[String] = Seq.empty): Option[String] = {
    val rulesModel = ArrayBuffer.empty[StructuredRule]
    val parseError: Option[String] = parseRules(initialFormula) match {
      case Right(rows) =>
        rulesModel ++= rows
        None
      case Left(msg) => Some(msg)
    }
    var dirty = false

    val rulesList = new ListView[String] {
      prefHeight = 220
      items = ObservableBuffer.from(rulesModel.map(_.summary))
      selectionModel.value.setSelectionMode(SelectionMode.Single)
    }
    if rulesModel.nonEmpty then rulesList.selectionModel.value.select(0)

    val preview = new TextField {
      editable = false
    }
    val status = new Label()
    var okButton: javafx.scene.control.Button = null

    def selectedIndex: Int = rulesList.selectionModel.value.getSelectedIndex

    def refreshList(): Unit = {
      rulesList.items = ObservableBuffer.from(rulesModel.map(_.summary))
      if rulesModel.nonEmpty then {
        val idx = Math.max(0, Math.min(selectedIndex, rulesModel.size - 1))
        rulesList.selectionModel.value.select(idx)
      }
      updateState()
    }

    def currentFormulaCandidate(): Either[String, String] = {
      if parseError.nonEmpty && !dirty then {
        Left(s"${parseError.get}. Bitte Regel direkt neu erfassen.")
      } else {
        validateFormula(rulesModel.map(ruleToFormula).mkString("/"))
      }
    }

    def updateState(): Unit = {
      currentFormulaCandidate() match {
        case Right(value) =>
          preview.text = value
          status.text = if value.isEmpty then "Keine Regel gesetzt" else "Formel OK"
          if okButton != null then okButton.setDisable(false)
        case Left(msg) =>
          preview.text = ""
          status.text = msg
          if okButton != null then okButton.setDisable(true)
      }
    }

    val btnAdd = new Button("Regel hinzufügen") {
      onAction = _ => {
        editRule(None, availableDisziplinen).foreach { rule =>
          dirty = true
          rulesModel += rule
          rulesList.selectionModel.value.select(rulesModel.size - 1)
          refreshList()
        }
      }
    }
    val btnEdit = new Button("Bearbeiten ...") {
      disable = rulesModel.isEmpty
      onAction = _ => {
        val idx = selectedIndex
        if idx >= 0 && idx < rulesModel.size then {
          editRule(Some(rulesModel(idx)), availableDisziplinen).foreach { updated =>
            dirty = true
            rulesModel.update(idx, updated)
            refreshList()
          }
        }
      }
    }
    val btnDelete = new Button("Entfernen") {
      disable = rulesModel.isEmpty
      onAction = _ => {
        val idx = selectedIndex
        if idx >= 0 && idx < rulesModel.size then {
          dirty = true
          rulesModel.remove(idx)
          refreshList()
        }
      }
    }
    val btnUp = new Button("↑") {
      disable = rulesModel.isEmpty
      onAction = _ => {
        val idx = selectedIndex
        if idx > 0 then {
          dirty = true
          val cur = rulesModel(idx)
          rulesModel.update(idx, rulesModel(idx - 1))
          rulesModel.update(idx - 1, cur)
          rulesList.selectionModel.value.select(idx - 1)
          rulesList.items.value.update(idx, rulesModel(idx).summary)
          rulesList.items.value.update(idx-1, rulesModel(idx-1).summary)
          rulesList.selectionModel.value.select(idx - 1)
          updateState()
        }
      }
    }
    val btnDown = new Button("↓") {
      disable = rulesModel.isEmpty
      onAction = _ => {
        val idx = selectedIndex
        if idx >= 0 && idx < rulesModel.size - 1 then {
          dirty = true
          val cur = rulesModel(idx)
          rulesModel.update(idx, rulesModel(idx + 1))
          rulesModel.update(idx + 1, cur)
          rulesList.items.value.update(idx, rulesModel(idx).summary)
          rulesList.items.value.update(idx+1, rulesModel(idx + 1).summary)
          rulesList.selectionModel.value.select(idx + 1)
          updateState()
        }
      }
    }

    val controls = new HBox {
      spacing = 6
      children = Seq(btnAdd, btnEdit, btnDelete, btnUp, btnDown)
    }

    rulesList.selectionModel.value.selectedIndexProperty.onChange {
      val hasSelection = selectedIndex >= 0 && selectedIndex < rulesModel.size
      btnEdit.disable = !hasSelection
      btnDelete.disable = !hasSelection
      btnUp.disable = !hasSelection || selectedIndex <= 0
      btnDown.disable = !hasSelection || selectedIndex >= rulesModel.size - 1
    }

    val panel = new VBox {
      spacing = 10
      padding = Insets(10)
      children = Seq(
        new Label("Regeln"),
        rulesList,
        controls,
        new Label("Formel der Punktegleichstandsregel"),
        preview,
        status
      )
      VBox.setVgrow(rulesList, Priority.Always)
    }

    val dialog = new jfxsc.Dialog[String]()
    dialog.setTitle(title)
    dialog.getDialogPane.getButtonTypes.addAll(jfxsc.ButtonType.OK, jfxsc.ButtonType.CANCEL)
    dialog.getDialogPane.setContent(panel.delegate)
    okButton = dialog.getDialogPane.lookupButton(jfxsc.ButtonType.OK).asInstanceOf[javafx.scene.control.Button]

    updateState()

    dialog.setResultConverter(dialogButton => {
      if dialogButton == jfxsc.ButtonType.OK then {
        currentFormulaCandidate() match {
          case Right(value) => value
          case Left(_) => null
        }
      } else null
    })

    val result = dialog.showAndWait()
    if result.isPresent then Some(result.get) else None
  }

  private def editRule(initial: Option[StructuredRule], availableDisziplinen: Seq[String]): Option[StructuredRule] = {
    val ruleType = new ComboBox[String] {
      items = ObservableBuffer.from(RuleTypes)
      value = initial.map(_.ruleType).getOrElse("E-Note-Summe")
    }
    val initialDisziplinen = initial.map(_.disziplinen.filter(_.nonEmpty).distinct).getOrElse(Nil)
    val disziplinModel = ArrayBuffer.from((initialDisziplinen ++ availableDisziplinen.map(_.trim).filter(_.nonEmpty)).distinct.map { name =>
      new CheckBox(name) {
        selected = initialDisziplinen.contains(name)
      }
    })
    val disziplinen = new ListView[CheckBox] {
      prefHeight = 190
      items = ObservableBuffer.from(disziplinModel)
      selectionModel.value.setSelectionMode(SelectionMode.Single)
      cellFactory.value = { (_: Any) => new ListCell[CheckBox] {
        item.onChange { (_, _, value) =>
          text = null
          graphic = if value == null then null else value
        }
      }}
    }
    val typ = new ComboBox[String] {
      items = ObservableBuffer.from(StreichWertungenTypen)
      value = initial.map(_.typ).getOrElse("Endnote")
    }
    val minMax = new ComboBox[String] {
      items = ObservableBuffer.from(MinMaxTypen)
      value = initial.map(_.minMax).getOrElse("Min")
    }
    val preview = new TextField {
      editable = false
    }
    val status = new Label()
    var okButton: javafx.scene.control.Button = null
    var btnDisziplinUp: Button = null
    var btnDisziplinDown: Button = null

    def selectedDisziplinIndex: Int = disziplinen.selectionModel.value.getSelectedIndex

    def refreshDisziplinen(): Unit = {
      disziplinen.items = ObservableBuffer.from(disziplinModel)
      if disziplinModel.nonEmpty then {
        val idx = Math.max(0, Math.min(selectedDisziplinIndex, disziplinModel.size - 1))
        disziplinen.selectionModel.value.select(idx)
      }
    }

    def buildRuleCandidate(): Either[String, StructuredRule] = {
      val selected = Option(ruleType.value.value).getOrElse("E-Note-Summe")
      val disziplinList = disziplinModel.filter(_.selected.value).map(_.text.value).toList

      selected match {
        case "Disziplin" | "StreichDisziplin" if disziplinList.isEmpty =>
          Left("Für Disziplin-Regeln muss mindestens eine Disziplin angegeben werden.")
        case "StreichWertungen" =>
          Right(StructuredRule("StreichWertungen", Nil, typ.value.value, minMax.value.value))
        case other =>
          Right(StructuredRule(other, disziplinList, "Endnote", "Min"))
      }
    }

    def updateState(): Unit = {
      val selected = Option(ruleType.value.value).getOrElse("")
      val needsDisziplinen = selected == "Disziplin" || selected == "StreichDisziplin"
      val needsStreichWertungen = selected == "StreichWertungen"
      disziplinen.disable = !needsDisziplinen
      val hasDisziplinSelection = selectedDisziplinIndex >= 0 && selectedDisziplinIndex < disziplinModel.size
      if btnDisziplinUp != null then {
        btnDisziplinUp.disable = !needsDisziplinen || !hasDisziplinSelection || selectedDisziplinIndex <= 0
      }
      if btnDisziplinDown != null then {
        btnDisziplinDown.disable = !needsDisziplinen || !hasDisziplinSelection || selectedDisziplinIndex >= disziplinModel.size - 1
      }
      typ.disable = !needsStreichWertungen
      minMax.disable = !needsStreichWertungen

      buildRuleCandidate() match {
        case Right(rule) =>
          validateFormula(ruleToFormula(rule)) match {
            case Right(serialized) =>
              preview.text = serialized
              status.text = "Regelvalidierung OK"
              if okButton != null then okButton.setDisable(false)
            case Left(msg) =>
              preview.text = ""
              status.text = msg
              if okButton != null then okButton.setDisable(true)
          }
        case Left(msg) =>
          preview.text = ""
          status.text = msg
          if okButton != null then okButton.setDisable(true)
      }
    }

    btnDisziplinUp = new Button("↑") {
      onAction = _ => {
        val idx = selectedDisziplinIndex
        if idx > 0 then {
          val cur = disziplinModel(idx)
          disziplinModel.update(idx, disziplinModel(idx - 1))
          disziplinModel.update(idx - 1, cur)
          disziplinen.selectionModel.value.select(idx - 1)

          disziplinen.items.value.update(idx-1, disziplinModel(idx - 1))
          disziplinen.items.value.update(idx, disziplinModel(idx))
          updateState()
        }
      }
    }
    btnDisziplinDown = new Button("↓") {
      onAction = _ => {
        val idx = selectedDisziplinIndex
        if idx >= 0 && idx < disziplinModel.size - 1 then {
          val cur = disziplinModel(idx)
          disziplinModel.update(idx, disziplinModel(idx + 1))
          disziplinModel.update(idx + 1, cur)
          disziplinen.selectionModel.value.select(idx + 1)
          disziplinen.items.value.update(idx+1, disziplinModel(idx + 1))
          disziplinen.items.value.update(idx, disziplinModel(idx))
          updateState()
        }
      }
    }

    val panel = new VBox {
      spacing = 8
      padding = Insets(10)
      children = Seq(
        new Label("Regeltyp"), ruleType,
        new Label("Disziplinen (für Disziplin/StreichDisziplin)"), disziplinen,
        new HBox {
          spacing = 6
          children = Seq(btnDisziplinUp, btnDisziplinDown)
        },
        new Label("Typ (StreichWertungen)"), typ,
        new Label("Min/Max (StreichWertungen)"), minMax,
        new Label("Formel der Regel"), preview,
        status
      )
    }

    val dialog = new jfxsc.Dialog[StructuredRule]()
    dialog.setTitle("Regel bearbeiten")
    dialog.getDialogPane.getButtonTypes.addAll(jfxsc.ButtonType.OK, jfxsc.ButtonType.CANCEL)
    dialog.getDialogPane.setContent(panel.delegate)
    okButton = dialog.getDialogPane.lookupButton(jfxsc.ButtonType.OK).asInstanceOf[javafx.scene.control.Button]

    ruleType.value.onChange(updateState())
    disziplinen.selectionModel.value.selectedIndexProperty.onChange(updateState())
    disziplinModel.foreach(_.selected.onChange(updateState()))
    typ.value.onChange(updateState())
    minMax.value.onChange(updateState())
    refreshDisziplinen()
    updateState()

    dialog.setResultConverter(dialogButton =>
      if dialogButton == jfxsc.ButtonType.OK then buildRuleCandidate().toOption.orNull else null
    )
    val result = dialog.showAndWait()
    if result.isPresent then Some(result.get) else None
  }

  private def parseRules(formula: String): Either[String, List[StructuredRule]] = {
    val normalized = Option(formula).map(_.trim).getOrElse("")
    if normalized.isEmpty then Right(Nil)
    else {
      val tokens = normalized.split("/").toList.map(_.trim).filter(_.nonEmpty)
      val parsed = tokens.map(token => parseRuleToken(token).toRight(s"Regel kann im Editor nicht dargestellt werden: $token"))
      val errors = parsed.collect { case Left(msg) => msg }
      if errors.nonEmpty then Left(errors.head)
      else Right(parsed.collect { case Right(rule) => rule })
    }
  }

  private def parseRuleToken(token: String): Option[StructuredRule] = {
    token match {
      case "Ohne" => Some(StructuredRule("Ohne", Nil, "Endnote", "Min"))
      case "E-Note-Summe" => Some(StructuredRule("E-Note-Summe", Nil, "Endnote", "Min"))
      case "E-Note-Best" => Some(StructuredRule("E-Note-Best", Nil, "Endnote", "Min"))
      case "D-Note-Summe" => Some(StructuredRule("D-Note-Summe", Nil, "Endnote", "Min"))
      case "D-Note-Best" => Some(StructuredRule("D-Note-Best", Nil, "Endnote", "Min"))
      case "JugendVorAlter" => Some(StructuredRule("JugendVorAlter", Nil, "Endnote", "Min"))
      case DisziplinPattern(dl) =>
        Some(StructuredRule("Disziplin", dl.split(",").toList.map(_.trim).filter(_.nonEmpty), "Endnote", "Min"))
      case StreichDisziplinPattern(dl) =>
        Some(StructuredRule("StreichDisziplin", dl.split(",").toList.map(_.trim).filter(_.nonEmpty), "Endnote", "Min"))
      case StreichWertungPattern(typ, _, minmax) =>
        Some(StructuredRule("StreichWertungen", Nil, typ, Option(minmax).filter(_.nonEmpty).getOrElse("Min")))
      case "StreichWertungen" =>
        Some(StructuredRule("StreichWertungen", Nil, "Endnote", "Min"))
      case _ => None
    }
  }

  private def ruleToFormula(rule: StructuredRule): String = {
    rule.ruleType match {
      case "Disziplin" => s"Disziplin(${rule.disziplinen.mkString(",")})"
      case "StreichDisziplin" => s"StreichDisziplin(${rule.disziplinen.mkString(",")})"
      case "StreichWertungen" => s"StreichWertungen(${rule.typ},${rule.minMax})"
      case other => other
    }
  }

  private def validateFormula(value: String): Either[String, String] = {
    val candidate = Option(value).map(_.trim).getOrElse("")
    if candidate.isEmpty then Right("")
    else {
      val tokens = candidate.split("/").toList.map(_.trim).filter(_.nonEmpty)
      val unsupported = tokens.filter(t => parseRuleToken(t).isEmpty)
      if unsupported.nonEmpty then Left(s"Ungültige Punktegleichstandsregel: ${unsupported.head}")
      else Right(Gleichstandsregel(candidate).toFormel)
    }
  }
}
