package ch.seidel.kutu.view

import ch.seidel.kutu.domain.Altersklasse
import javafx.scene.control as jfxsc
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, Priority, VBox}

import scala.collection.mutable.ArrayBuffer

object AltersklassenFieldEditorDialog {
  private case class KlassenToken(bezeichnung: String, qualifiers: Seq[String], alterVon: Int, alterBis: Option[Int], schrittweite: Option[Int]) {
    def summary: String = tokenToFormula(this)
  }

  private case class PrefixDef(bezeichnung: String, qualifiers: Seq[String])

  def edit(initialFormula: String, title: String = "Altersklassen bearbeiten"): Option[String] = {
    val rows = ArrayBuffer.empty[KlassenToken]
    val parseError: Option[String] = parseTokens(initialFormula) match {
      case Right(parsed) =>
        rows ++= parsed
        None
      case Left(msg) => Some(msg)
    }
    var dirty = false

    val rulesList = new ListView[String] {
      prefHeight = 220
      items = ObservableBuffer.from(rows.map(_.summary))
      selectionModel.value.setSelectionMode(SelectionMode.Single)
    }
    if rows.nonEmpty then rulesList.selectionModel.value.select(0)

    val preview = new TextField {
      editable = false
    }
    val previewtext = new ListView[String] {
      prefHeight = 120
      items = ObservableBuffer.empty[String]
    }

    val status = new Label()
    var okButton: javafx.scene.control.Button = null

    def selectedIndex: Int = rulesList.selectionModel.value.getSelectedIndex

    def refreshList(): Unit = {
      rulesList.items = ObservableBuffer.from(rows.map(_.summary))
      if rows.nonEmpty then {
        val idx = Math.max(0, Math.min(selectedIndex, rows.size - 1))
        rulesList.selectionModel.value.select(idx)
      }
      updateState()
    }

    def currentFormulaCandidate(): Either[String, String] = {
      if parseError.nonEmpty && !dirty then {
        Left(s"${parseError.get}. Bitte Formel direkt neu erfassen.")
      } else {
        validateFormula(rows.map(tokenToFormula).mkString(","))
      }
    }

    def renderText(formel: String): Unit = {
      val wkAlterklassen = Altersklasse(formel)
      previewtext.items = ObservableBuffer.from(wkAlterklassen.map(_.easyprint))
    }

    def updateState(): Unit = {
      previewtext.items.value.clear()
      currentFormulaCandidate() match {
        case Right(value) =>
          preview.text = value
          status.text = if value.isEmpty then "Keine Altersklassen" else "Formel OK"
          renderText(value)
          if okButton != null then okButton.setDisable(false)
        case Left(msg) =>
          preview.text = ""
          status.text = msg
          if okButton != null then okButton.setDisable(true)
      }
    }

    val btnAdd = new Button("Regel hinzufügen") {
      onAction = _ => {
        editToken(None).foreach { token =>
          rows += token
          dirty = true
          rulesList.selectionModel.value.select(rows.size - 1)
          refreshList()
        }
      }
    }
    val btnEdit = new Button("Bearbeiten ...") {
      disable = rows.isEmpty
      onAction = _ => {
        val idx = selectedIndex
        if idx >= 0 && idx < rows.size then {
          editToken(Some(rows(idx))).foreach { token =>
            rows.update(idx, token)
            dirty = true
            refreshList()
          }
        }
      }
    }
    val btnDelete = new Button("Entfernen") {
      disable = rows.isEmpty
      onAction = _ => {
        val idx = selectedIndex
        if idx >= 0 && idx < rows.size then {
          rows.remove(idx)
          dirty = true
          refreshList()
        }
      }
    }
    val btnUp = new Button("↑") {
      disable = rows.isEmpty
      onAction = _ => {
        val idx = selectedIndex
        if idx > 0 then {
          val cur = rows(idx)
          rows.update(idx, rows(idx - 1))
          rows.update(idx - 1, cur)
          dirty = true
          rulesList.items.value.update(idx, rows(idx).summary)
          rulesList.items.value.update(idx - 1, rows(idx - 1).summary)

          rulesList.selectionModel.value.select(idx - 1)
          updateState()
        }
      }
    }
    val btnDown = new Button("↓") {
      disable = rows.isEmpty
      onAction = _ => {
        val idx = selectedIndex
        if idx >= 0 && idx < rows.size - 1 then {
          val cur = rows(idx)
          rows.update(idx, rows(idx + 1))
          rows.update(idx + 1, cur)
          dirty = true
          rulesList.items.value.update(idx, rows(idx).summary)
          rulesList.items.value.update(idx + 1, rows(idx + 1).summary)

          rulesList.selectionModel.value.select(idx + 1)
          updateState()
        }
      }
    }

    rulesList.selectionModel.value.selectedIndexProperty.onChange {
      val hasSelection = selectedIndex >= 0 && selectedIndex < rows.size
      btnEdit.disable = !hasSelection
      btnDelete.disable = !hasSelection
      btnUp.disable = !hasSelection || selectedIndex <= 0
      btnDown.disable = !hasSelection || selectedIndex >= rows.size - 1
    }

    val controls = new HBox {
      spacing = 6
      children = Seq(btnAdd, btnEdit, btnDelete, btnUp, btnDown)
    }

    val editpanel = new VBox {
      spacing = 10
      padding = Insets(10)
      children = Seq(
        new Label("Altersklassen-Regeln"),
        rulesList,
        controls,
        new Label("Formel der Altersklassen-Regeln"),
        preview,
        status
      )
      VBox.setVgrow(rulesList, Priority.Always)
    }
    val panel = new HBox {
      spacing = 10
      padding = Insets(10)
      children = Seq(editpanel, previewtext)
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

  private def editToken(initial: Option[KlassenToken]): Option[KlassenToken] = {
    val txtBezeichnung = new TextField {
      promptText = "z.B. AK"
      text = initial.map(_.bezeichnung).getOrElse("")
    }
    val txtQualifier = new TextField {
      promptText = "optional, z.B. W+BS"
      text = initial.map(_.qualifiers.mkString("+")).getOrElse("")
    }
    val txtVon = new TextField {
      promptText = "Startalter"
      text = initial.map(_.alterVon.toString).getOrElse("")
    }
    val txtBis = new TextField {
      promptText = "optional, Endalter"
      text = initial.flatMap(_.alterBis).map(_.toString).getOrElse("")
    }
    val txtSchritt = new TextField {
      promptText = "optional, Schrittweite"
      text = initial.flatMap(_.schrittweite).map(_.toString).getOrElse("")
    }
    val preview = new TextField {
      editable = false
    }
    val previewtext = new ListView[String] {
      prefHeight = 120
      items = ObservableBuffer.empty[String]
    }

    val status = new Label()
    var okButton: javafx.scene.control.Button = null

    def parsePositiveInt(input: String): Option[Int] = {
      val normalized = Option(input).map(_.trim).getOrElse("")
      if normalized.isEmpty then None
      else if normalized.forall(_.isDigit) then {
        val value = normalized.toInt
        if value > 0 then Some(value) else None
      } else None
    }

    def candidate(): Either[String, KlassenToken] = {
      val von = parsePositiveInt(txtVon.text.value).toRight("Startalter muss eine positive Zahl sein.")
      val bis = Option(txtBis.text.value).map(_.trim).filter(_.nonEmpty) match {
        case Some(value) => parsePositiveInt(value).toRight("Endalter muss eine positive Zahl sein.").map(Some(_))
        case None => Right(None)
      }
      val schritt = Option(txtSchritt.text.value).map(_.trim).filter(_.nonEmpty) match {
        case Some(value) => parsePositiveInt(value).toRight("Schrittweite muss eine positive Zahl sein.").map(Some(_))
        case None => Right(None)
      }

      for
        alterVon <- von
        alterBis <- bis
        schrittweite <- schritt
        _ <- if alterBis.exists(_ < alterVon) then Left("Endalter muss größer oder gleich Startalter sein.") else Right(())
        _ <- if alterBis.isEmpty && schrittweite.nonEmpty then Left("Schrittweite ist nur bei einem Bereich erlaubt.") else Right(())
      yield {
        val qualifiers = Option(txtQualifier.text.value)
          .map(_.split("[+;,]").toSeq.map(_.trim).filter(_.nonEmpty).distinct)
          .getOrElse(Seq.empty)
        KlassenToken(
          bezeichnung = Option(txtBezeichnung.text.value).map(_.trim).getOrElse(""),
          qualifiers = qualifiers,
          alterVon = alterVon,
          alterBis = alterBis,
          schrittweite = schrittweite
        )
      }
    }

    def renderText(token: KlassenToken): Unit = {
      val wkAlterklassen = Altersklasse(tokenToFormula(token))
      previewtext.items = ObservableBuffer.from(wkAlterklassen.map(_.easyprint))
    }

    def updateState(): Unit = {
      previewtext.items.value.clear()
      candidate() match {
        case Right(token) =>
          renderText(token)
          validateFormula(tokenToFormula(token)) match {
            case Right(value) =>
              preview.text = value
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

    val editpanel = new VBox {
      spacing = 8
      padding = Insets(10)
      children = Seq(
        new Label("Bezeichnung"), txtBezeichnung,
        new Label("Gültigkeit nur für eine Gruppe mit entspr. Geschlecht oder Kategorie"), txtQualifier,
        new Label("Startalter"), txtVon,
        new Label("Endalter"), txtBis,
        new Label("Schrittweite (Teilung der Altersklasse zwischen Start-/Endalter alle n Jahre)"), txtSchritt,
        new Label("Formel der Regel"), preview,
        status
      )
    }

    val panel = new HBox {
      spacing = 8
      padding = Insets(10)
      children = Seq(editpanel, previewtext)
    }

    val dialog = new jfxsc.Dialog[KlassenToken]()
    dialog.setTitle("Altersklasse bearbeiten")
    dialog.getDialogPane.getButtonTypes.addAll(jfxsc.ButtonType.OK, jfxsc.ButtonType.CANCEL)
    dialog.getDialogPane.setContent(panel.delegate)
    okButton = dialog.getDialogPane.lookupButton(jfxsc.ButtonType.OK).asInstanceOf[javafx.scene.control.Button]

    txtBezeichnung.text.onChange(updateState())
    txtQualifier.text.onChange(updateState())
    txtVon.text.onChange(updateState())
    txtBis.text.onChange(updateState())
    txtSchritt.text.onChange(updateState())
    updateState()

    dialog.setResultConverter(dialogButton =>
      if dialogButton == jfxsc.ButtonType.OK then candidate().toOption.orNull else null
    )
    val result = dialog.showAndWait()
    if result.isPresent then Some(result.get) else None
  }

  private def parseTokens(formula: String): Either[String, List[KlassenToken]] = {
    val normalized = Option(formula).map(_.trim).getOrElse("")
    if normalized.isEmpty then Right(Nil)
    else {
      val tokens = normalized.split(",").toList.map(_.trim).filter(_.nonEmpty)
      val (_, parsed, errors) = tokens.foldLeft((Option.empty[PrefixDef], List.empty[KlassenToken], List.empty[String])) { (acc, token) =>
        val (lastPrefix, rows, errs) = acc
        parseToken(token, lastPrefix) match {
          case Right((row, currentPrefix)) => (Some(currentPrefix), rows :+ row, errs)
          case Left(msg) => (lastPrefix, rows, errs :+ msg)
        }
      }
      if errors.nonEmpty then Left(errors.head) else Right(parsed)
    }
  }

  private def parseToken(token: String, lastPrefix: Option[PrefixDef]): Either[String, (KlassenToken, PrefixDef)] = {
    import Altersklasse.*
    token match {
      case rangeStepPattern(prefixRaw, vonRaw, bisRaw, schrittRaw) =>
        val prefix = parsePrefix(prefixRaw, lastPrefix)
        for
          p <- prefix
          from <- parseInt(vonRaw, "Startalter")
          to <- parseInt(bisRaw, "Endalter")
          step <- parseInt(schrittRaw, "Schrittweite")
          _ <- if to < from then Left(s"Ungültiger Bereich: $token") else Right(())
        yield KlassenToken(p.bezeichnung, p.qualifiers, from, Some(to), Some(step)) -> p
      case rangepattern(prefixRaw, vonRaw, bisRaw) =>
        val prefix = parsePrefix(prefixRaw, lastPrefix)
        for
          p <- prefix
          from <- parseInt(vonRaw, "Startalter")
          to <- parseInt(bisRaw, "Endalter")
          _ <- if to < from then Left(s"Ungültiger Bereich: $token") else Right(())
        yield KlassenToken(p.bezeichnung, p.qualifiers, from, Some(to), None) -> p
      case intpattern(prefixRaw, vonRaw) =>
        val prefix = parsePrefix(prefixRaw, lastPrefix)
        for
          p <- prefix
          from <- parseInt(vonRaw, "Alter")
        yield KlassenToken(p.bezeichnung, p.qualifiers, from, None, None) -> p
      case _ => Left(s"Regel kann im Editor nicht dargestellt werden: $token")
    }
  }

  private def parsePrefix(rawPrefix: String, lastPrefix: Option[PrefixDef]): Either[String, PrefixDef] = {
    import Altersklasse.*
    val cleaned = Option(rawPrefix).map(_.trim).getOrElse("")
    if cleaned.isEmpty then Right(lastPrefix.getOrElse(PrefixDef("", Nil)))
    else {
      cleaned match {
        case qualifierPattern(bezeichnungRaw, qualifiersRaw) =>
          val qualifiers = qualifiersRaw
            .split("[+;,]")
            .toSeq
            .map(_.trim)
            .filter(_.nonEmpty)
            .distinct
          val bezeichnung = Option(bezeichnungRaw).map(_.trim).getOrElse("")
          Right(PrefixDef(bezeichnung, qualifiers))
        case _ =>
          Right(PrefixDef(cleaned, Nil))
      }
    }
  }

  private def parseInt(value: String, label: String): Either[String, Int] = {
    if value.forall(_.isDigit) then {
      val parsed = value.toInt
      if parsed > 0 then Right(parsed) else Left(s"$label muss positiv sein.")
    } else Left(s"$label muss numerisch sein.")
  }

  private def tokenToFormula(token: KlassenToken): String = {
    val qualifierPart = if token.qualifiers.isEmpty then "" else token.qualifiers.mkString("(", "+", ")")
    val prefix = s"${token.bezeichnung.trim}$qualifierPart"
    token.alterBis match {
      case None => s"$prefix${token.alterVon}"
      case Some(bis) if token.schrittweite.exists(_ > 1) => s"$prefix${token.alterVon}-$bis/${token.schrittweite.get}"
      case Some(bis) => s"$prefix${token.alterVon}-$bis"
    }
  }

  private def validateFormula(value: String): Either[String, String] = {
    val candidate = Option(value).map(_.trim).getOrElse("")
    if candidate.isEmpty then Right("")
    else {
      val parsed = Altersklasse.parseGrenzen(candidate)
      if parsed.isEmpty then Left("Ungültige Altersklassendefinition.")
      else Right(candidate)
    }
  }
}
