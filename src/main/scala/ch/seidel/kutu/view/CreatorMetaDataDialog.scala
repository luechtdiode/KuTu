package ch.seidel.kutu.view

import ch.seidel.kutu.domain.CreatorMetaData
import javafx.scene.control as jfxsc

import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{Priority, VBox}
import javafx.stage.{Modality, Window}

/**
 * Erfasst die Veranstalter-Daten inkl. Akzeptanz der Nutzungsbedingungen für den
 * Altbestand-Reparaturweg (Secret ohne `admin`-Claim).
 *
 * Eigener Dialog statt `PageDisplayer.askFor`: die Reparatur läuft asynchron aus einem
 * Hintergrund-Thread, und der Aufrufer steuert den Ablauf, nicht eine Command-Function.
 */
object CreatorMetaDataDialog {

  private val termsVersion = CreatorMetaData.currentTermsVersion

  def ask(owner: Window, initial: CreatorMetaData = CreatorMetaData("", "", "", "")): Option[CreatorMetaData] = {
    val name = new TextField {
      promptText = "Name des Veranstalters"
      text = initial.creatorName
      prefColumnCount = 32
    }
    val address = new TextField {
      promptText = "Adresse des Veranstalters"
      text = initial.creatorAddress
      prefColumnCount = 32
    }
    val phone = new TextField {
      promptText = "Telefon des Veranstalters"
      text = initial.creatorPhone
      prefColumnCount = 32
    }
    // Ohne vorbelegte, akzeptierte Daten bleibt die Checkbox bewusst leer: die
    // Akzeptanz muss eine aktive Handlung des Veranstalters sein.
    val terms = new CheckBox(s"Ich akzeptiere die Nutzungsbedingungen (Version $termsVersion)") {
      selected = initial.termsVersion == termsVersion
    }
    val termsView = new TextArea {
      text = CreatorMetaData.termsText
      editable = false
      wrapText = true
      prefRowCount = 12
    }
    val status = new Label()

    var okButton: javafx.scene.control.Button = null

    def candidate(): CreatorMetaData =
      CreatorMetaData(name.text.value, address.text.value, phone.text.value, termsVersion)

    def updateState(): Unit = {
      val data = candidate()
      val missing = Seq(
        "Name" -> data.creatorName,
        "Adresse" -> data.creatorAddress,
        "Telefon" -> data.creatorPhone
      ).collect { case (label, value) if Option(value).forall(_.trim.isEmpty) => label }

      if !terms.selected.value then
        status.text = "Bitte die Nutzungsbedingungen akzeptieren."
      else if missing.nonEmpty then
        status.text = s"Bitte ausfüllen: ${missing.mkString(", ")}."
      else
        status.text = "Alle Angaben vollständig."
      if okButton != null then okButton.setDisable(!data.isComplete || !terms.selected.value)
    }

    val panel = new VBox {
      spacing = 8
      padding = Insets(10)
      children = Seq(
        new Label("Für die Web-UI-Verwaltung dieses Wettkampfs werden die folgenden Angaben des Veranstalters benötigt."),
        new Label("Name"), name,
        new Label("Adresse"), address,
        new Label("Telefon"), phone,
        terms,
        new Label("Nutzungsbedingungen"), termsView,
        status
      )
      VBox.setVgrow(termsView, Priority.Always)
    }

    val dialog = new jfxsc.Dialog[CreatorMetaData]()
    dialog.setTitle("Veranstalter-Daten erfassen")
    dialog.initModality(Modality.WINDOW_MODAL)
    Option(owner).foreach(dialog.initOwner)
    dialog.getDialogPane.getButtonTypes.addAll(jfxsc.ButtonType.OK, jfxsc.ButtonType.CANCEL)
    dialog.getDialogPane.setContent(panel.delegate)
    okButton = dialog.getDialogPane.lookupButton(jfxsc.ButtonType.OK).asInstanceOf[javafx.scene.control.Button]

    name.text.onChange(updateState())
    address.text.onChange(updateState())
    phone.text.onChange(updateState())
    terms.selected.onChange(updateState())
    updateState()

    dialog.setResultConverter(button =>
      if button == jfxsc.ButtonType.OK && candidate().isComplete && terms.selected.value then candidate() else null
    )

    val result = dialog.showAndWait()
    if result.isPresent then Some(result.get) else None
  }
}
