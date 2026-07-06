package ch.seidel.kutu.view

import ch.seidel.kutu.domain.RiegenRotationsregel
import javafx.scene.control as jfxsc
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, Priority, VBox}

import scala.collection.mutable.ArrayBuffer

object RiegenRotationsregelFieldEditorDialog {
  private val RegelTypen: Seq[(String, String)] = Seq(
    "Einfache Rotation (Einfach)" -> "Einfach",
    "Kategorie" -> "Kategorie",
    "Verein" -> "Verein",
    "Geschlecht" -> "Geschlecht",
    "Alter absteigend" -> "AlterAbsteigend",
    "Alter aufsteigend" -> "AlterAufsteigend",
    "Name" -> "Name",
    "Vorname" -> "Vorname",
    "Rotierend" -> "Rotierend",
    "Alternierend invers" -> "AltInvers"
  )
  private val AllowedParts: Set[String] = RegelTypen.map(_._2).toSet

  private case class StructuredRule(token: String) {
    def summary: String = token
  }

  def edit(initialFormula: String, title: String = "Riegenrotationsregel bearbeiten"): Option[String] = {
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
        Left(s"${parseError.get}. Bitte Regel neu erfassen.")
      } else {
        validateFormula(rulesModel.map(_.token).mkString("/"))
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
        editRule(None).foreach { rule =>
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
          editRule(Some(rulesModel(idx))).foreach { updated =>
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
          rulesList.items.value.update(idx-1, rulesModel(idx - 1).summary)
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
        new Label("Formel der Riegenrotationsregel"),
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

  private def editRule(initial: Option[StructuredRule]): Option[StructuredRule] = {
    val ruleType = new ComboBox[String] {
      items = ObservableBuffer.from(RegelTypen.map(_._1))
      value = initial.flatMap(r => RegelTypen.find(_._2 == r.token).map(_._1)).getOrElse(RegelTypen.head._1)
    }
    val preview = new TextField {
      editable = false
    }
    val status = new Label()
    var okButton: javafx.scene.control.Button = null

    def buildRuleCandidate(): Either[String, StructuredRule] = {
      RegelTypen.find(_._1 == ruleType.value.value) match {
        case Some((_, token)) => Right(StructuredRule(token))
        case None => Left("Unbekannter Regeltyp.")
      }
    }

    def updateState(): Unit = {
      buildRuleCandidate() match {
        case Right(rule) =>
          validateFormula(rule.token) match {
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

    val panel = new VBox {
      spacing = 8
      padding = Insets(10)
      children = Seq(
        new Label("Regeltyp"), ruleType,
        //new Label("Formel der Regel"), preview,
        status
      )
    }

    val dialog = new jfxsc.Dialog[StructuredRule]()
    dialog.setTitle("Regel bearbeiten")
    dialog.getDialogPane.getButtonTypes.addAll(jfxsc.ButtonType.OK, jfxsc.ButtonType.CANCEL)
    dialog.getDialogPane.setContent(panel.delegate)
    okButton = dialog.getDialogPane.lookupButton(jfxsc.ButtonType.OK).asInstanceOf[javafx.scene.control.Button]

    ruleType.value.onChange(updateState())
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
      val parsed = tokens.map { token =>
        if AllowedParts.contains(token) then Right(StructuredRule(token))
        else Left(s"Regel kann im Editor nicht dargestellt werden: $token")
      }
      val errors = parsed.collect { case Left(msg) => msg }
      if errors.nonEmpty then Left(errors.head)
      else Right(parsed.collect { case Right(rule) => rule })
    }
  }

  private def validateFormula(value: String): Either[String, String] = {
    val candidate = Option(value).map(_.trim).getOrElse("")
    if candidate.isEmpty then Right("")
    else {
      val tokens = candidate.split("/").map(_.trim).filter(_.nonEmpty).toList
      val unknown = tokens.filterNot(AllowedParts.contains)
      if unknown.nonEmpty then Left(s"Ungültige Riegenrotationsregel: ${unknown.head}")
      else Right(RiegenRotationsregel(candidate).toFormel)
    }
  }
}
