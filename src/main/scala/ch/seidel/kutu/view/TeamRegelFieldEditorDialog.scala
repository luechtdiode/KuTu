package ch.seidel.kutu.view

import ch.seidel.kutu.domain.*
import javafx.scene.control as jfxsc
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{FlowPane, HBox, Priority, VBox}

import scala.collection.mutable.ArrayBuffer

object TeamRegelFieldEditorDialog {
  private val RuleTypes1 = Seq("Keine Teams", "Aus Verein", "Aus Verband")
  private val RuleTypes2 = Seq("Gesamtwertung", "Gerätewertung")
  private val Aggregates = Seq(
    Sum,
    Avg,
    Med,
    Min,
    Max,
    DevMin,
    DevMax)

  private case class StructuredRule(
      ruleType1: String,
      ruleType2: String,
      aggregate: TeamAggreateFun,
      min: String,
      max: String,
      includeSexGrouping: Boolean,
      selectedCategories: Set[String],
      extraTeams: List[String]
  ) {
    def summary: String = ruleToFormula(this)
  }

  def edit(initialFormula: String, title: String = "Teamregel bearbeiten"): Option[String] =
    edit(initialFormula, Seq.empty, title)

  def edit(initialFormula: String, categories: Seq[String], title: String): Option[String] = {
    val availableCategories = categories.map(_.trim).filter(_.nonEmpty).distinct.sorted
    val rulesModel = ArrayBuffer.empty[StructuredRule]

    val parseError: Option[String] = parseRules(initialFormula, availableCategories.toSet) match {
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
        Left(s"${parseError.get}. Bitte im Feld direkt bearbeiten oder Regeln neu erfassen.")
      } else {
        val combined = rulesModel.map(ruleToFormula).filter(_.nonEmpty).mkString(",")
        validateFormula(combined)
      }
    }

    def updateState(): Unit = {
      currentFormulaCandidate() match {
        case Right(value) =>
          preview.text = value
          status.text = if value.isEmpty then "Keine Teams" else "Formel OK"
          if okButton != null then okButton.setDisable(false)
        case Left(msg) =>
          preview.text = ""
          status.text = msg
          if okButton != null then okButton.setDisable(true)
      }
    }

    val btnAdd = new Button("Regel hinzufügen") {
      onAction = _ => {
        editRule(None, availableCategories).foreach { rule =>
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
          editRule(Some(rulesModel(idx)), availableCategories).foreach { updated =>
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
          rulesList.items.value.update(idx, rulesModel(idx).summary)
          rulesList.items.value.update(idx - 1, rulesModel(idx - 1).summary)

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
        new Label("Formel der Teamregel"),
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

  private def editRule(initial: Option[StructuredRule], availableCategories: Seq[String]): Option[StructuredRule] = {
    val ruleType1 = new ComboBox[String] {
      items = ObservableBuffer.from(RuleTypes1)
      value = initial.map(_.ruleType1).getOrElse("Aus Verein")
    }
    val ruleType2 = new ComboBox[String] {
      items = ObservableBuffer.from(RuleTypes2)
      value = initial.map(_.ruleType2).getOrElse("Gesamtwertung")
    }
    val aggregate = new ComboBox[TeamAggreateFun] {
      items = ObservableBuffer.from(Aggregates)
      value = initial.map(_.aggregate).getOrElse(Sum)
    }
    val min = new TextField {
      promptText = "* oder Zahl"
      text = initial.map(_.min).getOrElse("*")
    }
    val max = new TextField {
      promptText = "* oder Zahl"
      text = initial.map(_.max).getOrElse("*")
    }
    val includeSex = new CheckBox("Geschlecht gruppieren (M+W)") {
      selected = initial.exists(_.includeSexGrouping)
    }
    val categoryChecks = availableCategories.map { category =>
      val cb = new CheckBox(category) {
        selected = initial.exists(_.selectedCategories.contains(category))
      }
      (category, cb)
    }
    val extraTeams = new TextField {
      promptText = "Gemischte Teams (Namen mit Komma trennen)"
      text = initial.map(_.extraTeams.mkString(", ")).getOrElse("")
    }
    val preview = new TextField {
      editable = false
    }
    val status = new Label()
    var okButton: javafx.scene.control.Button = null

    def buildRuleCandidate(): Either[String, StructuredRule] = {
      val selectedRule1 = Option(ruleType1.value.value).getOrElse("Aus Verein")
      val selectedRule2 = Option(ruleType2.value.value).getOrElse("Gesamtwertung")
      if selectedRule1 == "Keine Teams" then Right(StructuredRule("Keine Teams", "", Sum, "*", "*", includeSexGrouping = false, Set.empty, Nil))
      else {
        for
          minVal <- normalizeToken(min.text.value).toRight("Min muss '*' oder eine positive Zahl sein.")
          maxVal <- normalizeToken(max.text.value).toRight("Max muss '*' oder eine positive Zahl sein.")
        yield {
          val selectedCategories = categoryChecks.collect { case (name, cb) if cb.selected.value => name }.toSet
          val teams = Option(extraTeams.text.value)
            .map(_.split("[,\n\r;]").toList.map(_.trim).filter(_.nonEmpty).distinct)
            .getOrElse(Nil)
          StructuredRule(selectedRule1, selectedRule2, aggregate.value.value, minVal, maxVal, includeSex.selected.value, selectedCategories, teams)
        }
      }
    }

    def updateState(): Unit = {
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

    val categoryPane = new FlowPane {
      hgap = 8
      vgap = 4
      children = categoryChecks.map(_._2)
    }
    val panel = new VBox {
      spacing = 8
      padding = Insets(10)
      children = Seq(
        new Label("Gruppierung der Teams"), ruleType1,
        new Label("Auswahl der Wertungen"), ruleType2,
        new Label("Teamresultat Berechnung (Aggregatfunktion)"), aggregate,
        new Label("Mind. Anzahl Teilnehmer/-Innen, zählende Resultate"), min,
        new Label("Max Anzahl Teilnehmer/-Innen"), max,
        includeSex
      ) ++ (if availableCategories.nonEmpty then Seq(new Label("Kategorien"), categoryPane) else Seq.empty) ++ Seq(
        new Label("Gemischte Teams"), extraTeams,
        new Label("Formel der Regel"), preview,
        status
      )
    }

    val dialog = new jfxsc.Dialog[StructuredRule]()
    dialog.setTitle("Regel bearbeiten")
    dialog.getDialogPane.getButtonTypes.addAll(jfxsc.ButtonType.OK, jfxsc.ButtonType.CANCEL)
    dialog.getDialogPane.setContent(panel.delegate)
    okButton = dialog.getDialogPane.lookupButton(jfxsc.ButtonType.OK).asInstanceOf[javafx.scene.control.Button]

    ruleType1.value.onChange(updateState())
    ruleType2.value.onChange(updateState())
    aggregate.value.onChange(updateState())
    min.text.onChange(updateState())
    max.text.onChange(updateState())
    includeSex.selected.onChange(updateState())
    categoryChecks.foreach(_._2.selected.onChange(updateState()))
    extraTeams.text.onChange(updateState())
    updateState()

    dialog.setResultConverter(dialogButton =>
      if dialogButton == jfxsc.ButtonType.OK then buildRuleCandidate().toOption.orNull else null
    )
    val result = dialog.showAndWait()
    if result.isPresent then Some(result.get) else None
  }

  private def parseRules(formula: String, categories: Set[String]): Either[String, List[StructuredRule]] = {
    val normalized = Option(formula).map(_.trim).getOrElse("")
    if normalized.isEmpty || normalized.equalsIgnoreCase("Keine Teams") then Right(Nil)
    else {
      val tokens = normalized.split(",").toList.map(_.trim).filter(_.nonEmpty)
      val parsed = tokens.map(token => parseRuleToken(token, categories).toRight(s"Regel kann im Editor nicht dargestellt werden: $token"))
      val errors = parsed.collect { case Left(msg) => msg }
      if errors.nonEmpty then Left(errors.head)
      else Right(parsed.collect { case Right(rule) => rule })
    }
  }

  private def parseRuleToken(token: String, categories: Set[String]): Option[StructuredRule] = {
    if token.equalsIgnoreCase("Keine Teams") then Some(StructuredRule("Keine Teams",  "", Sum, "*", "*", includeSexGrouping = false, Set.empty, Nil))
    else {
      val parsed = TeamRegel(token)
      parsed.getTeamRegeln.toList match {
        case (r: TeamRegelVereinGesamt) :: Nil =>
          mapRule("Aus Verein", "Gesamtwertung", r.min, r.max, r.aggregateFun, r.grouperDef, r.extraTeamsDef, categories, token)
        case (r: TeamRegelVereinGeraet) :: Nil =>
          mapRule("Aus Verein", "Gerätwertung", r.min, r.max, r.aggregateFun, r.grouperDef, r.extraTeamsDef, categories, token)
        case (r: TeamRegelVerbandGesamt) :: Nil =>
          mapRule("Aus Verband", "Gesamtwertung", r.min, r.max, r.aggregateFun, r.grouperDef, r.extraTeamsDef, categories, token)
        case (r: TeamRegelVerbandGeraet) :: Nil =>
          mapRule("Aus Verband", "Gerätwertung", r.min, r.max, r.aggregateFun, r.grouperDef, r.extraTeamsDef, categories, token)
        case _ => None
      }
    }
  }

  private def mapRule(
      ruleType1: String,
      ruleType2: String,
      min: Int,
      max: Int,
      aggregate: TeamAggreateFun,
      grouperDef: String,
      extraTeamsDef: String,
      categories: Set[String],
      originalToken: String
  ): Option[StructuredRule] = {
    val groups = Option(grouperDef)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.stripPrefix("[").stripSuffix("]"))
      .map(_.split("/").toList.map(_.trim).filter(_.nonEmpty).map(_.split("\\+").map(_.trim).filter(_.nonEmpty).toSet))
      .getOrElse(List.empty)

    val sexGroups = groups.filter(_ == Set("M", "W"))
    val categoryGroups = groups.filter(g => g.nonEmpty && g.subsetOf(categories))
    val unsupported = groups.filterNot(g => g == Set("M", "W") || (g.nonEmpty && g.subsetOf(categories)))

    if unsupported.nonEmpty || categoryGroups.size > 1 || sexGroups.size > 1 then None
    else {
      val candidate = StructuredRule(
        ruleType1 = ruleType1,
        ruleType2 = ruleType2,
        aggregate = aggregate,
        min = asToken(min),
        max = asToken(max),
        includeSexGrouping = sexGroups.nonEmpty,
        selectedCategories = categoryGroups.headOption.getOrElse(Set.empty),
        extraTeams = Option(extraTeamsDef).map(_.stripPrefix("/").split("\\+").toList.map(_.trim).filter(_.nonEmpty)).getOrElse(Nil)
      )
      validateFormula(ruleToFormula(candidate)) match {
        case Right(serialized) if normalizeForCompare(serialized) == normalizeForCompare(originalToken) => Some(candidate)
        case _ => None
      }
    }
  }

  private def validateFormula(value: String): Either[String, String] = {
    val candidate = value.trim
    if candidate.isEmpty then Right("")
    else {
      try {
        val parsed = TeamRegel(candidate)
        val serialized = parsed.toFormel
        if serialized == "Keine Teams" then Left("Teamregel konnte nicht geparst werden.")
        else if normalizeForCompare(serialized) == normalizeForCompare(candidate) then Right(serialized)
        else Left(s"Ungültige Teamregel. Normalisiert wäre: $serialized")
      } catch {
        case e: Throwable => Left(s"Ungültige Teamregel: ${e.getMessage}")
      }
    }
  }

  private def normalizeToken(value: String): Option[String] = {
    val token = Option(value).map(_.trim).getOrElse("*")
    if token == "*" then Some("*")
    else if token.forall(_.isDigit) && token.nonEmpty && token.toInt > 0 then Some(token)
    else None
  }

  private def ruleToFormula(rule: StructuredRule): String = {
    if rule.ruleType1 == "Keine Teams" then ""
    else {
      val rt1 = rule.ruleType1 match {
        case "Aus Verband" => "Verband"
        case _ => "Verein"
      }
      val rt2 = rule.ruleType2 match {
        case "Gesamtwertung" => "Gesamt"
        case _ => "Gerät"
      }
      val groups = {
        val terms = ArrayBuffer.empty[String]
        if rule.includeSexGrouping then terms += "M+W"
        if rule.selectedCategories.nonEmpty then terms += rule.selectedCategories.toList.sorted.mkString("+")
        terms.toList
      }
      val groupPart = if groups.isEmpty then "" else s"[${groups.mkString("/")}]"
      val extraTeamsPart = if rule.extraTeams.isEmpty then "" else s"/${rule.extraTeams.mkString("+")}"
      s"$rt1$rt2$groupPart(${aggregatePrefix(rule.aggregate)}${rule.min}/${rule.max}$extraTeamsPart)"
    }
  }

  private def aggregatePrefix(fn: TeamAggreateFun): String = fn  match {
    case Sum => ""
    case _ => fn.toFormelPart
  }

  private def asToken(value: Int): String = if value <= 0 then "*" else value.toString

  private def normalizeForCompare(value: String): String = value.replaceAll("\\s+", "")
}
