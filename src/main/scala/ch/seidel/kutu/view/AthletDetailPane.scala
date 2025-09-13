package ch.seidel.kutu.view

import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.data.ResourceExchanger.saveMediaFile
import ch.seidel.kutu.domain.{KutuService, Media, Wettkampf}
import ch.seidel.kutu.view.player.Player
import scalafx.event.ActionEvent
import scalafx.scene.control.Button
import javafx.stage.FileChooser
import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.geometry._
import scalafx.scene.control._
import scalafx.scene.layout._

import java.io.{File, FileInputStream}
import java.net.URI


case class AthletHeaderPane(wettkampf: Wettkampf, service: KutuService, wkview: TableView[IndexedSeq[WertungEditor]]) extends HBox {
  var index = -1
  var selected: IndexedSeq[WertungEditor] = IndexedSeq()
  val lblDivider1 = new Label() {
    text = " : "
    styleClass += "toolbar-header"
  }
  val lblDivider2 = new Label() {
    text = " : "
    visible <== Bindings.createBooleanBinding(() =>
      !wkview.selectionModel().isEmpty && wkview.selectionModel().getSelectedItem.exists(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden")),
      wkview.selectionModel().selectedItemProperty())
    styleClass += "toolbar-header"
  }
  val lblDisciplin = new Label() {
    styleClass += "toolbar-header"
  }
  val lblMedia = new Label() {
    styleClass += "toolbar-header"
  }
  val lblAthlet = new Label() {
    styleClass += "toolbar-header"
  }

  val mediaButton = new MenuButton("♪ Bodenmusik") {
    visible <== Bindings.createBooleanBinding(() =>
      !wkview.selectionModel().isEmpty && wkview.selectionModel().getSelectedItem.exists(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden")),
      wkview.selectionModel().selectedItemProperty())
    items += new MenuItem {
      text = "Mediaplayer"
      disable <== Bindings.createBooleanBinding(() =>
        wkview.selectionModel().isEmpty || !wkview.selectionModel().getSelectedItem
          .filter(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden")).exists(we => we.init.mediafile.nonEmpty),
        wkview.selectionModel().selectedItemProperty())
      onAction = (event: ActionEvent) => {
        if (selected != null && index > -1 && index < selected.size) {
          selected(index).init.mediafile.flatMap(m => service.loadMedia(m.id)).foreach { media =>
            val title = s"${selected.head.init.athlet.vorname} ${selected.head.init.athlet.name} (${selected.head.init.athlet.verein}), Boden - ${media.name}"
            Player.clearPlayList()
            Player.addToPlayList(title, media.computeFilePath(wettkampf).toURI.toASCIIString)
            Player.show(title)
          }
        }
      }
    }
    items += new MenuItem {
      text = "Bodenmusik zuordnen ..."
      disable <== Bindings.createBooleanBinding(() =>
        wkview.selectionModel().isEmpty || !wkview.selectionModel().getSelectedItem.exists(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden")),
        wkview.selectionModel().selectedItemProperty())
      minWidth = 75
      onAction = (event: ActionEvent) => {
        val athletwertung: Option[WertungEditor] = wkview.selectionModel().getSelectedItem.find(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden"))
        athletwertung.foreach { aw =>

          def saveAndAssignMedia(song: String, uri: URI) = {
            val file = new File(uri)
            val songFile = new FileInputStream(file)
            val extension = song.substring(song.lastIndexOf(".") + 1)
            val media = service.searchMedia(song) match {
              case Some(m) => Some(m.toMedia)
              case _ => Some(saveMediaFile(songFile, wettkampf, Media("", song, extension)).toMedia)
            }
            aw.update(service.updateWertung(aw.init.copy(mediafile = media).toWertung))
          }

          val fc: FileChooser = new FileChooser
          fc.setTitle("Audiofile laden")
          fc.getExtensionFilters.addAll(
            //new FileChooser.ExtensionFilter("Audio Dateien", "*.mp3", "*.wav", "*.aif", "*.aiff")
            new FileChooser.ExtensionFilter("MP3 (audio/mpeg)", "*.mp3")
            //, new FileChooser.ExtensionFilter("Audio Interchange File Format (pcm)", "*.aif", "*.aiff")
            //, new FileChooser.ExtensionFilter("Waveform Audio Format", "*.wav")
          )
          fc.setSelectedExtensionFilter(fc.getExtensionFilters.get(0))
          fc.setInitialDirectory(wettkampf.audiofilesDir)
          val file: File = fc.showOpenDialog(KuTuApp.stage)
          if (file != null) {
            saveAndAssignMedia(file.toPath.getFileName.toString, file.toURI)
          }
        }
      }
    }
  }

  wkview.selectionModel.value.selectedItemProperty().onChange(
    (model: scalafx.beans.value.ObservableValue[IndexedSeq[WertungEditor], IndexedSeq[WertungEditor]],
     oldSelection: IndexedSeq[WertungEditor],
     newSelection: IndexedSeq[WertungEditor]) => {
      if (newSelection != null && selected != newSelection) {
        selected = newSelection
        adjust
      }
      else if (newSelection == null) {
        selected = null
        index = -1
        adjust
      }
    })

  wkview.focusModel.value.focusedCell.onChange { (focusModel, oldTablePos, newTablePos) =>
    if (newTablePos != null && selected != null) {
      val column = newTablePos.tableColumn
      val selrow = newTablePos.getRow
      if (column != null && selrow > -1) {
        column match {
          case access: WKTCAccess =>
            val selectedIndex = access.getIndex
            if (selectedIndex > -1 && selectedIndex != index) {
              index = selectedIndex
              adjust
            }
            else if (selectedIndex < 0) {
              index = -1
              adjust
            }
          case _ =>
            index = -1
            adjust
        }
      }
      else {
        index = -1
        adjust
      }
    }
  }

  children = List(lblAthlet, lblDivider1, lblDisciplin, lblDivider2, lblMedia, mediaButton)
  HBox.setMargin(lblAthlet, Insets(0d, 1d, 0d, 5d))
  HBox.setMargin(lblDisciplin, Insets(0d, 1d, 0d, 1d))
  HBox.setMargin(lblMedia, Insets(0d, 1d, 0d, 1d))
  HBox.setMargin(mediaButton, Insets(5d, 5d, 5d, 5d))

  def adjust: Unit = {
    if (selected != null && index > -1 && index < selected.size) {
      lblAthlet.text.value = selected(index).init.athlet.easyprint
      lblDisciplin.text.value = selected(index).init.wettkampfdisziplin.easyprint
      lblMedia.text.value = selected(index).init.mediafile.map(m => s"♪ ${m.name} ").getOrElse("")
    }
    else if (selected != null && selected.nonEmpty) {
      lblMedia.text.value = ""
      lblAthlet.text.value = selected(0).init.athlet.easyprint
      lblDisciplin.text.value = Seq(selected(0).init.riege, selected(0).init.riege2).map(_.getOrElse("")).filter {
        _.nonEmpty
      }.mkString(", ")
    }
    else {
      lblAthlet.text.value = ""
      lblDisciplin.text.value = ""
      lblMedia.text.value = ""
    }
  }
}
