package ch.seidel.kutu.view

import ch.seidel.commons.PageDisplayer
import ch.seidel.kutu.Config.{homedir, remoteHostOrigin}
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.KuTuApp.handleAction
import ch.seidel.kutu.actors.AthletMediaAquire
import ch.seidel.kutu.data.ResourceExchanger.saveMediaFile
import ch.seidel.kutu.domain.{KutuService, Media, Wettkampf}
import ch.seidel.kutu.view.player.Player
import javafx.stage.FileChooser
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.beans.property.BooleanProperty
import scalafx.event.ActionEvent
import scalafx.geometry.*
import scalafx.scene.control.*
import scalafx.scene.layout.*

import java.io.{File, FileInputStream}
import java.net.URI


case class AthletHeaderPane(wettkampf: Wettkampf, service: KutuService, wkview: TableView[IndexedSeq[WertungEditor]], wettkampfmode: BooleanProperty) extends HBox {
  val isReadonly: Boolean = wettkampf.isReadonly(homedir, remoteHostOrigin)
  var index = -1
  var selected: IndexedSeq[WertungEditor] = IndexedSeq()
  private val lblDivider1 = new Label() {
    text = " : "
    styleClass += "toolbar-header"
  }
  private val lblDivider2 = new Label() {
    text = " : "
    visible <== Bindings.createBooleanBinding(() =>
      !wkview.selectionModel().isEmpty && wkview.selectionModel().getSelectedItem.exists(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden")),
      wkview.selectionModel().selectedItemProperty()
    )
    styleClass += "toolbar-header"
  }
  private val lblDisciplin = new Label() {
    styleClass += "toolbar-header"
  }
  private val lblMedia = new Label() {
    styleClass += "toolbar-header"
  }
  private val lblAthlet = new Label() {
    styleClass += "toolbar-header"
  }

  val checkmark = new Region()
  checkmark.getStyleClass.add("check-mark")
  checkmark.visibleProperty.bind(Player.isNetworkMediaPlayer)

  val checkIsUseMyMediaPlayerMenuItem: MenuItem = new MenuItem("Media Player den Wertungsrichtern freigeben", checkmark) {
    onAction = handleAction { (_: ActionEvent) =>
      Player.useMyMediaPlayerAsNetworkplayer(!Player.isNetworkMediaPlayer.getValue)
    }
  }

  private val currentMediaMenuItem = new MenuItem {
    onAction = (event: ActionEvent) => {
      if selected != null then {
        Player.clearPlayList()
        val items = selected
          .filter(a => a.init.wettkampfdisziplin.disziplin.name.equals("Boden"))
          .map(a => (a, a.init.wettkampfdisziplin.disziplin.name, a.init.mediafile.flatMap(m => service.loadMedia(m.id)))).filter(item => item._2.nonEmpty).filter(_._3.nonEmpty)
        items.foreach(item => {
          val (a, disziplin, medias) = item
          val media = medias.get
          val title = s"${selected.head.init.athlet.vorname} ${selected.head.init.athlet.name} ${selected.head.init.athlet.verein.map(v => s"(${v.name})").getOrElse("")}, $disziplin - ${media.name}"
          Player.addToPlayList(title, media.computeFilePath(wettkampf).toURI.toASCIIString.toLowerCase)
        })
        items.headOption.foreach(item => {
          val (a, _, _) = item
          Player.load(Player.getPlayList.getSongs.head.getKey, AthletMediaAquire(a.init.wettkampf.uuid.get, a.init.athlet, a.init.toWertung))
        })
      }
    }
  }
  private val assignMediaMenuItem: MenuItem = new MenuItem {
    text = "Bodenmusik zuordnen ..."
    visible <== when(wettkampfmode) choose false otherwise true
    onAction = (event: ActionEvent) => {
      val athletwertung: Option[WertungEditor] = wkview.selectionModel().getSelectedItem.find(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden"))
      athletwertung.foreach { aw =>

        def saveAndAssignMedia(song: String, uri: URI): Unit = {
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
        if file != null then {
          try {
            saveAndAssignMedia(file.toPath.getFileName.toString, file.toURI)
          } catch {
            case e: Exception =>
              PageDisplayer.showErrorDialog("Audiofile laden", e.getMessage)
          }
          adjust()
        }
      }
    }
  }

  private val deleteMediaAssignmentMenuItem: MenuItem = new MenuItem {
    text = "Bodenmusik löschen ..."
    visible <== when(wettkampfmode) choose false otherwise true
    onAction = (event: ActionEvent) => {
      val athletwertung: Option[WertungEditor] = wkview.selectionModel().getSelectedItem.find(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden") && we.init.mediafile.nonEmpty)
      athletwertung.foreach { aw =>
        aw.update(service.updateWertung(aw.init.copy(mediafile = None).toWertung))
      }
      adjust()
    }
  }

  private val mediaButton = new MenuButton("♪ Bodenmusik") {
    visible <== Bindings.createBooleanBinding(() =>
      (!wettkampfmode.value || wkview.items.value.exists(p => p.exists(w => w.init.mediafile.nonEmpty))) && !wkview.selectionModel().isEmpty && wkview.selectionModel().getSelectedItem.exists(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden")),
      wkview.selectionModel().selectedItemProperty(), wettkampfmode, wkview.items)
    items += KuTuApp.makeMenuAction("Media Player anzeigen ...") { (caption, action) =>
      Player.show()
    }
    if !isReadonly then {
      items += checkIsUseMyMediaPlayerMenuItem
    }
    items += currentMediaMenuItem
    if !isReadonly then {
      items += assignMediaMenuItem
      items += deleteMediaAssignmentMenuItem
    }
  }

  wkview.selectionModel.value.selectedItemProperty().onChange(
    (model: scalafx.beans.value.ObservableValue[IndexedSeq[WertungEditor], IndexedSeq[WertungEditor]],
     oldSelection: IndexedSeq[WertungEditor],
     newSelection: IndexedSeq[WertungEditor]) => {
      if newSelection != null && selected != newSelection then {
        selected = newSelection
        adjust()
      }
      else if newSelection == null then {
        selected = null
        index = -1
        adjust()
      }
    })

  wkview.focusModel.value.focusedCell.onChange { (focusModel, oldTablePos, newTablePos) =>
    if newTablePos != null && selected != null then {
      val column = newTablePos.tableColumn
      val selrow = newTablePos.getRow
      if column != null && selrow > -1 then {
        column match {
          case access: WKTCAccess =>
            val selectedIndex = access.getIndex
            if selectedIndex > -1 && selectedIndex != index then {
              index = selectedIndex
              adjust()
            }
            else if selectedIndex < 0 then {
              index = -1
              adjust()
            }
          case _ =>
            index = -1
            adjust()
        }
      }
      else {
        index = -1
        adjust()
      }
    }
  }

  children = List(lblAthlet, lblDivider1, lblDisciplin, lblDivider2, lblMedia, mediaButton)
  HBox.setMargin(lblAthlet, Insets(0d, 1d, 0d, 5d))
  HBox.setMargin(lblDisciplin, Insets(0d, 1d, 0d, 1d))
  HBox.setMargin(lblMedia, Insets(0d, 1d, 0d, 1d))
  HBox.setMargin(mediaButton, Insets(5d, 5d, 5d, 5d))

  def adjust(): Unit = {
    lblMedia.visible.value = this.mediaButton.visible.value
    assignMediaMenuItem.disable = isReadonly || wkview.selectionModel().isEmpty ||
      !wkview.selectionModel().getSelectedItem.exists(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden"))
    deleteMediaAssignmentMenuItem.disable = isReadonly || wkview.selectionModel().isEmpty ||
      !wkview.selectionModel().getSelectedItem.exists(we => we.init.wettkampfdisziplin.disziplin.name.equals("Boden") && we.init.mediafile.nonEmpty)
    if selected != null && index > -1 && index < selected.size then {
      lblAthlet.text.value = selected(index).init.athlet.easyprint
      lblDisciplin.text.value = selected(index).init.wettkampfdisziplin.easyprint
      lblMedia.text.value = selected(index).init.mediafile.map(m => s"♪ ${m.name} ").getOrElse("")
      val title = s"${selected.head.init.athlet.vorname} ${selected.head.init.athlet.name} ${selected.head.init.athlet.verein.map(v => s"(${v.name})").getOrElse("")}"
      currentMediaMenuItem.text = s"Media Player mit Playlist von $title ..."
      currentMediaMenuItem.disable = selected.flatMap(a => a.init.mediafile.flatMap(m => service.loadMedia(m.id))).isEmpty
    }
    else if selected != null && selected.nonEmpty then {
      lblMedia.text.value = ""
      lblAthlet.text.value = selected(0).init.athlet.easyprint
      lblDisciplin.text.value = Seq(selected(0).init.riege, selected(0).init.riege2).map(_.getOrElse("")).filter {
        _.nonEmpty
      }.mkString(", ")
      val title = s"${selected.head.init.athlet.vorname} ${selected.head.init.athlet.name} ${selected.head.init.athlet.verein.map(v => s"(${v.name})").getOrElse("")}"
      currentMediaMenuItem.text = s"Media Player mit Playlist von $title ..."
      currentMediaMenuItem.disable = selected.flatMap(a => a.init.mediafile.flatMap(m => service.loadMedia(m.id))).isEmpty
    }
    else {
      lblAthlet.text.value = ""
      lblDisciplin.text.value = ""
      lblMedia.text.value = ""
      currentMediaMenuItem.text = s"Media Player (leere Playlist) ..."
      currentMediaMenuItem.disable = true
    }
  }
}
