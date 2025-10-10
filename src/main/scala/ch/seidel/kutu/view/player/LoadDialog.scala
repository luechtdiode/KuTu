package ch.seidel.kutu.view.player

import ch.seidel.kutu.Config
import ch.seidel.kutu.domain.Wettkampf
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.Scene
import javafx.scene.control.{Button, Label, TextField}
import javafx.scene.layout.{HBox, Priority, VBox}
import javafx.scene.paint.{Color, CycleMethod, LinearGradient, Stop}
import javafx.stage.{Modality, Stage}
import javafx.geometry.{Insets, Pos}
import javafx.stage.FileChooser
import scalafx.stage.DirectoryChooser

import java.io.File

object LoadDialog {
  def loadPlayList(wettkampf: Option[Wettkampf] = None, owner: Stage, playList: PlayList): Unit = {
    val urlField: TextField = new TextField {
      setText(new File(wettkampf.map(_.audiofilesDir.toString).getOrElse(Config.homedir)).toURI.toASCIIString)
      setPrefWidth(500)
      setStyle("-fx-font-size: 0.9em;")
    }
    HBox.setHgrow(urlField, Priority.ALWAYS)
    val dialog: Stage = new Stage
    dialog.initOwner(owner)
    dialog.initModality(Modality.APPLICATION_MODAL)
    dialog.setScene(new Scene(new VBox {
      setSpacing(20)
      setPadding(new Insets(25))
      setStyle("-fx-base: #282828; -fx-background: #282828; -fx-font-size: 1.1em;")
      getChildren.addAll(
        new Label("Adresse für m3u, Phlow xml -Listen, oder Audiofiles (mp3, aif, aiff, wav)."),
        new HBox {
          setSpacing(20)
          getChildren.addAll(urlField, new Button {
            setText("Browse Audio Verzeichnis ...")
            setOnAction(new EventHandler[ActionEvent]() {
              override def handle(arg0: ActionEvent): Unit = {
                val fc: DirectoryChooser = new DirectoryChooser
                fc.setTitle("Audiofiles laden")
                fc.setInitialDirectory(wettkampf.map(_.audiofilesDir).getOrElse(new File(Config.homedir)))
                val file: File = fc.showDialog(dialog)
                if (file != null) {
                  urlField.setText(file.toURI.toString)
                }
              }
            })
          }, new Button {
            setText("Browse Audio Dateien ...")
            setOnAction(new EventHandler[ActionEvent]() {
              override def handle(arg0: ActionEvent): Unit = {
                val fc: FileChooser = new FileChooser
                fc.setTitle("Audiofile laden")
                fc.getExtensionFilters.addAll(
                  //new FileChooser.ExtensionFilter("Audio Dateien", "*.mp3", "*.wav", "*.aif", "*.aiff")
                  new FileChooser.ExtensionFilter("MP3 (audio/mpeg)", "*.mp3")
                  //,new FileChooser.ExtensionFilter("Audio Interchange File Format (pcm)", "*.aif", "*.aiff")
                  //,new FileChooser.ExtensionFilter("Waveform Audio Format", "*.wav")
                )
                fc.setSelectedExtensionFilter(fc.getExtensionFilters.get(0))
                fc.setInitialDirectory(wettkampf.map(_.audiofilesDir).getOrElse(new File(Config.homedir)))
                val file: File = fc.showOpenDialog(dialog)
                if (file != null) {
                  urlField.setText(file.toURI.toString)
                }
              }
            })
          })
        },
        new HBox {
          setSpacing(10)
          setAlignment(Pos.CENTER_RIGHT)
          getChildren.addAll(new Button {
            setText("Abbrechen")
            setCancelButton(true)
            setOnAction(new EventHandler[ActionEvent]() {
              override def handle(arg0: ActionEvent): Unit = {
                dialog.hide()
              }
            })
          },
            new Button {
              setText("Laden...")
              setDefaultButton(true)
              setOnAction(new EventHandler[ActionEvent]() {
                override def handle(arg0: ActionEvent): Unit = {
                  dialog.hide()
                  playList.load(urlField.getText)
                }
              })
            })
        })
    }) {
      setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.REPEAT, new Stop(0, Color.web("#282828")), new Stop(1, Color.web("#202020"))))
    })
    dialog.showAndWait()
  }
}
