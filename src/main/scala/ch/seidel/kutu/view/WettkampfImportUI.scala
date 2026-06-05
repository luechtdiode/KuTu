package ch.seidel.kutu.view

import ch.seidel.commons.PageDisplayer
import ch.seidel.kutu.data.CsvImportData
import ch.seidel.kutu.domain.ProgrammView
import javafx.scene.control as jfxsc
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, VBox}
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter

import java.io.File
import java.net.URI
import scala.collection.mutable
import ch.seidel.kutu.data.WettkampfImportSupport.*

class WettkampfImportUI(homedir: String) {
  private case class CsvImportConfig(fieldMapping: Map[String, Option[String]], genderValueMapping: Map[String, String])

  def createImportMenu(
                        progrm: Option[ProgrammView],
                        exportInitialDirectory: File,
                        onExcelImport: ActionEvent => Unit,
                        onCsvImport: (URI, ActionEvent) => Unit,
                        onPdfImport: (URI, ProgrammView, ActionEvent) => Unit,
                        onExcelExportEmpty: (URI, ActionEvent) => Unit,
                        onExcelExportWithData: (URI, ActionEvent) => Unit): MenuButton = {
    val excelImportItem = new MenuItem("Aus Excel Clipboard einfügen ...") {
      onAction = (actionEvent: ActionEvent) => onExcelImport(actionEvent)
    }

    val csvImportItem = new MenuItem("Aus CSV/Excel importieren ...") {
      onAction = (actionEvent: ActionEvent) => {
        val selectedCsv = chooseImportFile(
          titleText = "Aus CSV/Excel-Datei importieren",
          filters = Seq(
            new ExtensionFilter("CSV/Excel Dateien", Seq("*.csv", "*.xlsx", "*.xls")),
            new ExtensionFilter("CSV Dateien", "*.csv"),
            new ExtensionFilter("Excel Dateien", Seq("*.xlsx", "*.xls")),
            new ExtensionFilter("Alle Dateien", "*.*")),
          initialFileName = Some("excelinput.csv")
        )
        selectedCsv.foreach(file => onCsvImport(file.toURI, actionEvent))
      }
    }

    val pdfImportItem = new MenuItem("Aus Faxe KuTu Rangliste-PDF importieren ...") {
      disable = progrm.isEmpty || progrm.size > 1 || progrm.forall(_.parent.isEmpty)
      onAction = (actionEvent: ActionEvent) => {
        progrm.foreach { p =>
          val suggestedName = s"rangliste-${p.name}.pdf".toLowerCase
          val selectedPdf = chooseImportFile(
            titleText = "Faxe KuTu Rangliste-PDF importieren",
            filters = Seq(new ExtensionFilter("PDF Dateien", "*.pdf"), new ExtensionFilter("Alle Dateien", "*.*")),
            initialFileName = Some(suggestedName)
          )
          selectedPdf.foreach(file => onPdfImport(file.toURI, p, actionEvent))
        }
      }
    }

    val exportEmptyItem = new MenuItem("Excelvorlage für Datenimport erstellen ...") {
      onAction = (actionEvent: ActionEvent) => {
        val selectedFile = chooseExportFile(
          titleText = "Excel-Vorlage erstellen",
          defaultDirectory = exportInitialDirectory,
          initialFileName = Some("athleten-import-vorlage.xlsx")
        )
        selectedFile.foreach(file => onExcelExportEmpty(file.toURI, actionEvent))
      }
    }

    val exportWithDataItem = new MenuItem("Als Excel exportieren ...") {
      onAction = (actionEvent: ActionEvent) => {
        val selectedFile = chooseExportFile(
          titleText = "Athletendaten als Excel exportieren",
          defaultDirectory = exportInitialDirectory,
          initialFileName = Some("athleten-import.xlsx")
        )
        selectedFile.foreach(file => onExcelExportWithData(file.toURI, actionEvent))
      }
    }

    new MenuButton("Import/Export") {
      if progrm.exists(_.toPath.contains("KuTu")) then {
        items ++= Seq(csvImportItem, pdfImportItem, new SeparatorMenuItem(), exportEmptyItem, exportWithDataItem)
      } else if progrm.exists(_.toPath.contains("GeTu")) then {
        items ++= Seq(excelImportItem, csvImportItem, new SeparatorMenuItem(), exportEmptyItem, exportWithDataItem)
      } else {
        items ++= Seq(csvImportItem, new SeparatorMenuItem(), exportEmptyItem, exportWithDataItem)
      }
    }
  }

  def prepareTabularImport(filename: URI): Option[CsvImportData] = {
    val tableData = readTabularFile(filename)
    if tableData.isEmpty then {
      PageDisplayer.showWarnDialog("Aus CSV/Excel laden ...", "Die gewaehlte Datei ist leer oder enthaelt keine Header-Zeile.")
      return None
    }

    val (csvHeaders, sourceRows) = tableData.get
    showCsvImportConfigDialog(csvHeaders).map { config =>
      val mappedRows = mapFieldRows(sourceRows, config.fieldMapping)
        .filter(row => !(row.getOrElse("NAME", "").equals("Mustermann") && row.getOrElse("VERBAND", "").equals("Musterverband")  && (row.getOrElse("VORNAME", "").startsWith("Max") || row.getOrElse("VORNAME", "").startsWith("Klara"))))
      CsvImportData(mappedRows, config.genderValueMapping)
    }
  }

  private def chooseImportFile(titleText: String, filters: Seq[ExtensionFilter], initialFileName: Option[String] = None): Option[File] = {
    val fileChooser = new FileChooser {
      title = titleText
      initialDirectory = new java.io.File(homedir)
      extensionFilters ++= filters.map(_.delegate)
    }
    initialFileName.foreach(name => fileChooser.initialFileName = name)
    Option(fileChooser.showOpenDialog(null))
  }

  private def chooseExportFile(titleText: String, defaultDirectory: File, initialFileName: Option[String] = None): Option[File] = {
    val effectiveDirectory = if defaultDirectory.exists() then defaultDirectory else new java.io.File(homedir)
    val fileChooser = new FileChooser {
      title = titleText
      initialDirectory = effectiveDirectory
      extensionFilters ++= Seq(
        new ExtensionFilter("Excel Dateien", Seq("*.xlsx")),
        new ExtensionFilter("Alle Dateien", "*.*")
      ).map(_.delegate)
    }
    initialFileName.foreach(name => fileChooser.initialFileName = name)
    Option(fileChooser.showSaveDialog(null)).map { file =>
      if file.getName.toLowerCase.endsWith(".xlsx") then file
      else new File(file.getAbsolutePath + ".xlsx")
    }
  }

  private def showCsvImportConfigDialog(csvHeaders: Seq[String]): Option[CsvImportConfig] = {
    val fieldSelectors = mutable.LinkedHashMap[String, ComboBox[String]]()
    val grid = new GridPane {
      hgap = 8
      vgap = 8
      padding = Insets(12)
    }

    CsvDefaultFieldMapping.keys.toSeq.zipWithIndex.foreach { case (logicalField, row) =>
      val selector = new ComboBox[String] {
        items = ObservableBuffer.from(csvHeaders :+ "")
        prefWidth = 280
        value = csvHeaders.find(h => CsvDefaultFieldMapping(logicalField).exists(_.equalsIgnoreCase(h))).orNull
      }
      fieldSelectors += logicalField -> selector
      grid.add(new Label(logicalField), 0, row)
      grid.add(selector, 1, row)
    }

    val mappingInfo = new Label("Geschlecht-Mapping (Dateiwert=KuTu-App Wert M/W, eine Zeile pro Mapping):")
    val genderMappingArea = new TextArea {
      prefRowCount = 6
      text = DefaultGenderValueMappingRaw
    }

    val contentPane = new VBox {
      spacing = 10
      children = Seq(
        new Label("CSV/Excel Feldzuordnung"),
        grid,
        mappingInfo,
        genderMappingArea
      )
    }

    val dialog = new jfxsc.Dialog[CsvImportConfig]()
    dialog.setTitle("CSV/Excel-Import konfigurieren")
    dialog.getDialogPane.getButtonTypes.addAll(jfxsc.ButtonType.CANCEL, jfxsc.ButtonType.OK)
    dialog.getDialogPane.setContent(contentPane.delegate)
    dialog.setResultConverter(dialogButton => {
      if dialogButton == jfxsc.ButtonType.OK then {
        val selectedFieldMapping = fieldSelectors.map { case (logicalField, selector) =>
          if selector.value == null || selector.value.value == null || selector.value.value.trim.isEmpty then logicalField -> None
          else logicalField -> Option(selector.value.value)
        }.toMap
        val effectiveGenderMap = effectiveGenderValueMapping(genderMappingArea.text.value)
        CsvImportConfig(selectedFieldMapping, effectiveGenderMap)
      } else null
    })

    val result = dialog.showAndWait()
    if result.isPresent then Some(result.get) else None
  }
}


