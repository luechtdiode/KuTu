package ch.seidel.kutu.view.player

import ch.seidel.commons.PageDisplayer
import ch.seidel.kutu.{Config, ConnectionStates}
import ch.seidel.kutu.actors.{AthletMediaAquire, AthletMediaIsAtStart, AthletMediaIsFree, AthletMediaIsPaused, AthletMediaIsRunning, AthletMediaPause, AthletMediaRelease, AthletMediaStart, AthletMediaToStart, ForgetMyMediaPlayer, KutuAppEvent, MediaPlayerAction, MediaPlayerDisconnected, MediaPlayerEvent, MediaPlayerIsReady, UseMyMediaPlayer}
import ch.seidel.kutu.domain.{KutuService, Wettkampf}
import ch.seidel.kutu.http.WebSocketClient
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.{SimpleBooleanProperty, SimpleDoubleProperty}
import javafx.collections.ListChangeListener
import javafx.event.{ActionEvent, EventHandler}
import javafx.geometry.Orientation
import javafx.scene.control.{Button, Label, Slider}
import javafx.scene.effect.DropShadow
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.{AudioSpectrumListener, EqualizerBand, Media, MediaPlayer}
import javafx.scene.paint.{Color, CycleMethod, LinearGradient, Stop}
import javafx.scene.shape.Line
import javafx.scene.transform.Rotate
import javafx.scene.{Group, Scene}
import javafx.stage.{Stage, StageStyle}
import javafx.util.{Duration, Pair}
import scalafx.Includes.{jfxProperty2sfx, observableList2ObservableBuffer}
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.image.{Image, ImageView}

import java.util

object Player extends JFXApp3 {
  private val playList = new PlayList
  private var currentSongIndex = 0
  private var mediaPlayer: MediaPlayer = null
  private val sliders = new util.ArrayList[Slider](10)
  private var balanceKnob: Slider = null
  private var volumeKnob: Slider = null
  private var leftVUPointer: Line = null
  private var rightVUPointer: Line = null
  private var trackLabel: Label = null
  private var timeLabel: Label = null
  private var nameLabel: Label = null
  private val leftVURotation = new Rotate(-40, 0, 0)
  private val rightVURotation = new Rotate(-40, 0, 0)
  private val leftVU = new SimpleDoubleProperty(0)
  private val rightVU = new SimpleDoubleProperty(0)
  private val vuMeters = new util.ArrayList[VUMeter](10)
  private var spectrumListener: AudioSpectrumListener = null
  private var component: Option[Stage] = None
  private var wettkampf: Option[Wettkampf] = None
  private var service: Option[KutuService] = None

  val isNetworkMediaPlayer = new SimpleBooleanProperty(false)

  override def start(): Unit = {
    // load initial playlist
    loadPlayList("http://ia600402.us.archive.org/11/items/their_finest_hour_vol1/their_finest_hour_vol1_files.xml")
    //loadPlayList("http://www.archive.org/download/their_finest_hour_vol3/their_finest_hour_vol3_files.xml");
    val root: Group = initComponents(() => Platform.exit())

    val scene = new Scene(root, 1204, 763)
    scene.setFill(Color.TRANSPARENT)
    //scene.getStylesheets.add(getClass.getResource("Player.css").toExternalForm)
    stage = new PrimaryStage
    stage.setScene(scene)
    stage.initStyle(StageStyle.TRANSPARENT)
    stage.show()
  }

  def setWettkampf(wettkampf: Wettkampf, service: KutuService): Unit = {
    try {
      getInitializedPlayerStage
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    this.wettkampf = Some(wettkampf)
    this.service = Some(service)
    ConnectionStates.connectedProperty.addListener(_ => {
      if (ConnectionStates.connectedProperty.getValue && isNetworkMediaPlayer.getValue) {
        WebSocketClient.publish(UseMyMediaPlayer(wettkampf.uuid.get, Config.deviceId))
        releasePlayer()
      }
    })
  }

  def useMyMediaPlayerAsNetworkplayer(flag: Boolean): Unit = {
    this.wettkampf.foreach(wettkampf => {
      if (flag) {
        WebSocketClient.publish(UseMyMediaPlayer(wettkampf.uuid.get, Config.deviceId))
        releasePlayer()
      } else {
        isNetworkMediaPlayer.set(false)
        WebSocketClient.publish(ForgetMyMediaPlayer(wettkampf.uuid.get, Config.deviceId))
      }
    })
  }

  def isPlayerRunning(): Boolean = {
    mediaPlayer != null && mediaPlayer.getStatus != null && mediaPlayer.getStatus.equals(Status.PLAYING)
  }

  def clearPlayList(): Unit = {
    if (isPlayerRunning()) {
      if (playList.getSongs.size() > currentSongIndex) {
        playList.getSongs.retainAll(playList.getSongs(currentSongIndex))
      }
      currentSongIndex = 0
    } else {
      playList.getSongs.clear()
      currentSongIndex = 0
    }
  }

  def loadPlayList(uri: String): Unit = {
    playList.load(uri)
  }

  def addToPlayList(caption: String, uri: String): Unit = {
    if (!playList.getSongs.exists(_.getValue.equals(uri))) {
      playList.getSongs.add(new Pair[String, String](caption, uri))
    }
  }

  def getPlayList(): PlayList = playList

  def hide(): Unit = {
    playerStage().foreach(_.close())
  }

  def load(song: String, aquire: AthletMediaAquire):Unit = {
    if (lastAction.isEmpty || lastAction.contains(aquire)) {
      if (playList.getSongs.exists(_.getKey.equals(song))) {
        lastAction = Some(aquire)
        show(song)
      } else {
        _handleMediaAction(aquire)
      }
    } else {
      PageDisplayer.showWarnDialog("Musik laden", "Der Player wird gerade von einer anderen Musik verwendet.\nDer gewünschte Titel kann noch nicht geladen werden.")
      val stage = getInitializedPlayerStage
      stage.show()
      stage.toFront()
    }
  }

  def show(song: String = ""): Unit = {
    val s = playSong(song)
    s.show()
    s.toFront()
  }

  def playSong(song: String = "", autoplay: Boolean = false): Stage = {
    val wasPlaying = mediaPlayer != null && mediaPlayer.getStatus != null && mediaPlayer.getStatus == MediaPlayer.Status.PLAYING
    val s = getInitializedPlayerStage
    playList.getSongs.toList.zipWithIndex.find(s => s._1.getKey.equals(song)).map(_._2).foreach(songIndex => {
      currentSongIndex = songIndex
      if (!wasPlaying) {
        play(currentSongIndex, autoplay)
      }
    })
    s
  }

  def handlePrevious(): Unit = {
    if (mediaPlayer != null && (currentSongIndex == 0 || mediaPlayer.getCurrentTime.toSeconds > 3)) {
      mediaPlayer.seek(Duration.seconds(0))
      mediaPlayer.pause()
    } else if (currentSongIndex > 0) {
      currentSongIndex -= 1
      play(currentSongIndex)
    }
  }

  def handleNext(): Unit = {
    if (currentSongIndex < playList.getSongs.size - 1) {
      currentSongIndex += 1
      play(currentSongIndex)
    }
  }

  def handlePause(): Unit = {
    if (mediaPlayer == null) play(currentSongIndex)
    else if (mediaPlayer.getStatus eq MediaPlayer.Status.PLAYING) mediaPlayer.pause()
    else mediaPlayer.play()
  }

  var lastAction: Option[MediaPlayerAction] = None

  private def handleMediaEvent(event: MediaPlayerEvent): Unit = {
    event match {
      case MediaPlayerIsReady(context) if context.equals(Config.deviceId) =>
        isNetworkMediaPlayer.set(true)
      case MediaPlayerDisconnected(context) if context.equals(Config.deviceId) =>
        isNetworkMediaPlayer.set(false)
      case _ =>
        //ignore
    }
  }
  private def handleMediaAction(action: MediaPlayerAction): Unit = {
    if (isNetworkMediaPlayer.getValue) _handleMediaAction(action)
  }
  private def _handleMediaAction(action: MediaPlayerAction): Unit = action match {
    case a@AthletMediaAquire(wkuuid, athlet, wertung) =>
      if (lastAction.isEmpty) {
        lastAction = Some(a)
        clearPlayList()
        wertung.mediafile.flatMap(m => service.get.loadMedia(m.id)).foreach { media =>
          val title = s"${athlet.vorname} ${athlet.name} (${athlet.verein.map(_.name).getOrElse("")}) - ${media.name}"
          addToPlayList(title, media.computeFilePath(wettkampf.get).toURI.toASCIIString.toLowerCase)
        }
        show(getPlayList().getSongs.head.getKey)
      }
    case AthletMediaRelease(wkuuid,athlet, wertung) =>
      lastAction match {
        case Some(a) if a.athlet == athlet && a.wertung.mediafile == wertung.mediafile =>
          releasePlayer()
        case _ => // ignore
      }
    case AthletMediaStart(wkuuid,athlet, wertung) =>
      lastAction match {
        case Some(a) if a.athlet == athlet && a.wertung.mediafile == wertung.mediafile =>
          play(0, autoplay = true)
          lastAction = Some(a)
        case _ => // ignore
      }
    case AthletMediaPause(wkuuid,athlet, wertung) =>
      lastAction match {
        case Some(a) if a.athlet == athlet && a.wertung.mediafile == wertung.mediafile =>
          handlePause()
          lastAction = Some(a)
        case _ => // ignore
      }
    case AthletMediaToStart(wkuuid,athlet, wertung) =>
      lastAction match {
        case Some(a) if a.athlet == athlet && a.wertung.mediafile == wertung.mediafile =>
          handlePrevious()
          lastAction = Some(a)
        case _ => // ignore
      }
  }

  private def playerStage(): Option[Stage] = component

  private def getInitializedPlayerStage: Stage = {
    playerStage() match {
      case None =>
        val group: Group = Player.initComponents(() => playerStage().foreach(s => s.close()))
        val scene = new Scene(group, 1204, 763)
        scene.setFill(Color.BLACK)
        val s = new Stage(StageStyle.DECORATED) {
          setScene(scene)
        }
        component = Some(s)
        s
      case Some(s) => s
    }
  }

  private def resetDisplay(): Unit = {
    if (trackLabel != null) trackLabel.setText("Track 0/0")
    //if (timeLabel != null) timeLabel.setText("Remaining: 0:00   Total: 0:00")
    if (nameLabel != null) nameLabel.setText("no Track loaded")
  }
  private def initComponents(onClose: () => Unit) = {
    val prevBtn = new Button
    prevBtn.setOnAction(new EventHandler[ActionEvent]() {
      override def handle(event: ActionEvent): Unit = {
        handlePrevious()
      }
    })
    val nextBtn = new Button
    nextBtn.setOnAction(new EventHandler[ActionEvent]() {
      override def handle(event: ActionEvent): Unit = {
        handleNext()
      }
    })
    val playPauseBtn = new Button
    playPauseBtn.setOnAction(new EventHandler[ActionEvent]() {
      override def handle(event: ActionEvent): Unit = {
        handlePause()
      }
    })
    val loadBtn = new Button
    loadBtn.setOnAction(new EventHandler[ActionEvent]() {
      override def handle(event: ActionEvent): Unit = {
        LoadDialog.loadPlayList(wettkampf, getInitializedPlayerStage, playList);
      }
    })
    val powerBtn = new Button
    powerBtn.setOnAction(new EventHandler[ActionEvent]() {
      override def handle(event: ActionEvent): Unit = {
        releasePlayer()
        onClose()
      }
    })
    WebSocketClient.registerMediaPlayerActionHandler(handleMediaAction)
    WebSocketClient.registerMediaPlayerEventHandler(handleMediaEvent)

    val background = new ImageView()
    val bgResource = getClass.getResourceAsStream("/images/player/player-background.png")
    if (bgResource != null) {
      background.setImage(new Image(bgResource))
    }

    for (i <- 0 until 10) {
      sliders.add(i, new Slider(EqualizerBand.MIN_GAIN, EqualizerBand.MAX_GAIN, 0))
      sliders.get(i).setOrientation(Orientation.VERTICAL)
    }
    trackLabel = new Label("Track 0/0")
    timeLabel = new Label("Remaining: 0:00   Total: 0:00")
    nameLabel = new Label("no Track loaded")

    leftVUPointer = new Line
    leftVUPointer.setStartX(0)
    leftVUPointer.setStartY(0)
    leftVUPointer.setEndX(0)
    leftVUPointer.setEndY(-83)
    leftVUPointer.setStroke(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE, new Stop(0.33, Color.TRANSPARENT), new Stop(0.34, Color.BLACK), new Stop(1, Color.web("#7e7e7e"))))
    leftVUPointer.setEffect(new DropShadow(5, new Color(0, 0, 0, 0.3)))
    leftVUPointer.setStrokeWidth(3)
    leftVUPointer.setTranslateX(375)
    leftVUPointer.setTranslateY(615)
    leftVUPointer.getTransforms.add(leftVURotation)

    rightVUPointer = new Line
    rightVUPointer.setStartX(0)
    rightVUPointer.setStartY(0)
    rightVUPointer.setEndX(0)
    rightVUPointer.setEndY(-83)
    rightVUPointer.setStroke(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE, new Stop(0.33, Color.TRANSPARENT), new Stop(0.34, Color.BLACK), new Stop(1, Color.web("#7e7e7e"))))
    rightVUPointer.setEffect(new DropShadow(5, new Color(0, 0, 0, 0.3)))
    rightVUPointer.setStrokeWidth(3)
    rightVUPointer.setTranslateX(593)
    rightVUPointer.setTranslateY(615)
    rightVUPointer.getTransforms.add(rightVURotation)

    /*val lcd = Font.loadFont(getClass.getResourceAsStream("lcddot.ttf"), 14)
    trackLabel.setFont(lcd)
    timeLabel.setFont(lcd)
    nameLabel.setFont(lcd)

     */
    for (i <- 0 until 10) {
      vuMeters.add(new VUMeter)
    }
    balanceKnob = new Slider(-1, 1, 0)
    volumeKnob = new Slider(0, 1, 1)
    spectrumListener = new AudioSpectrumListener() {
      override def spectrumDataUpdate(timestamp: Double, duration: Double, magnitudes: Array[Float], phases: Array[Float]): Unit = {
        var average: Double = 0
        for (i <- magnitudes.indices) {
          val corr = sliders.get(i).getValue
          val level = (60 + math.max(-60, math.min(60, magnitudes(i) + corr))) / 60
          vuMeters.get(i).setValue(level)
          average += level
        }
        // make up VU meter values
        average = average / magnitudes.length
        average *= mediaPlayer.getVolume
        if (mediaPlayer.getBalance == 0) {
          leftVU.set(average)
          rightVU.set(average)
        }
        else if (mediaPlayer.getBalance > 0) {
          leftVU.set(average * (1 - mediaPlayer.getBalance))
          rightVU.set(average)
        }
        else if (mediaPlayer.getBalance < 0) {
          leftVU.set(average)
          rightVU.set(average * (mediaPlayer.getBalance + 1))
        }
      }
    }
    balanceKnob.setBlockIncrement(0.1)
    balanceKnob.setId("balance")
    balanceKnob.getStyleClass.add("knobStyle")
    volumeKnob.setBlockIncrement(0.1)
    volumeKnob.setId("volume")
    volumeKnob.getStyleClass.add("knobStyle")
    val root = new Group
    root.setAutoSizeChildren(false)
    root.getChildren.addAll(background, prevBtn, playPauseBtn, nextBtn, loadBtn, powerBtn, trackLabel, timeLabel, nameLabel, leftVUPointer, rightVUPointer, balanceKnob, volumeKnob)
    root.getChildren.addAll(sliders)
    root.getChildren.addAll(vuMeters)
    prevBtn.resizeRelocate(106, 285, 74, 74)
    playPauseBtn.resizeRelocate(201, 285, 88, 74)
    nextBtn.resizeRelocate(310, 285, 74, 74)
    loadBtn.resizeRelocate(413, 285, 77, 74)
    powerBtn.resizeRelocate(104, 532, 68, 86)
    for (i <- 0 until 10) {
      sliders.get(i).resizeRelocate(515 + (58 * i), 228 - 20, 53, 181 + 40)
    }
    trackLabel.resizeRelocate(122, 95, 389, 26)
    timeLabel.resizeRelocate(122, 125, 389, 26)
    nameLabel.resizeRelocate(122, 155, 389, 26)
    for (i <- 0 until 10) {
      vuMeters.get(i).setLayoutX(542 + (58 * i))
      vuMeters.get(i).setLayoutY(177)
    }
    balanceKnob.resizeRelocate(735, 535, 87, 87)
    volumeKnob.resizeRelocate(907, 491, 175, 175)
    // listen for when we have songs
    playList.getSongs.addListener(new ListChangeListener[Pair[String, String]]() {
      override def onChanged(change: ListChangeListener.Change[_ <: Pair[String, String]]): Unit = {
        if (playList.getSongs.nonEmpty || isPlayerRunning()) {
          lastAction.foreach {
            case a: AthletMediaStart =>
              change.getRemoved.forEach { context =>
                if (a.wertung.mediafile.exists(m => m.name.contains(context))) {
                  currentSongIndex = 0
                  play(currentSongIndex)
                }
              }
            case _ =>
          }
        }
        else {
          if (mediaPlayer != null) {
            mediaPlayer.stop()
            mediaPlayer.setAudioSpectrumListener(null)
          }
          resetDisplay()
          for (i <- 0 until 10) {
            vuMeters.get(i).setValue(0)
          }
          leftVU.set(0)
          rightVU.set(0)
        }
      }
    })
    root.getStylesheets.add(getClass.getResource("/css/Player.css").toExternalForm)
    root
  }

  def releasePlayer(): Unit = {
    if (mediaPlayer != null) {
      mediaPlayer.stop()
      mediaPlayer.setAutoPlay(false)
      mediaPlayer.seek(Duration.seconds(0))
      playList.getSongs.clear()
    }
    lastAction.foreach {
      case a: MediaPlayerAction =>
        publishMediaEventIfConnected(AthletMediaIsFree(a.wertung.mediafile.get, ""))
      case _ =>
    }
    lastAction = None

    mediaPlayer = null
  }

  private def play(songIndex: Int, autoplay: Boolean = false): Unit = {
    if (mediaPlayer != null) {
      mediaPlayer.stop()
      mediaPlayer.setAudioSpectrumListener(null)
      for (i <- 0 until 10) {
        vuMeters.get(i).setValue(0)
      }
      leftVU.set(0)
      rightVU.set(0)
    }
    resetDisplay()
    if (playList.getSongs.nonEmpty && playList.getSongs.size() > songIndex) {
      val context = playList.getSongs.get(songIndex).getKey
      val media = new Media(playList.getSongs.get(songIndex).getValue)
      val player = new MediaPlayer(media)
      mediaPlayer = player
      player.seek(Duration.seconds(0))
      player.setAutoPlay(autoplay)
      player.setOnError(new Runnable() {
        override def run(): Unit = {
          println("mediaPlayer.getError() = " + player.getError)
        }
      })
      lastAction.foreach {
        case a: MediaPlayerAction =>
          if (!autoplay)
            publishMediaEventIfConnected(AthletMediaIsAtStart(a.wertung.mediafile.get, context))
          else
            publishMediaEventIfConnected(AthletMediaIsRunning(a.wertung.mediafile.get, context))
        case _ =>
      }

      player.setOnEndOfMedia(() => {
        player.stop()
        player.setAutoPlay(false)
        player.seek(Duration.seconds(0))
        lastAction.foreach {
          case a: MediaPlayerAction =>
            publishMediaEventIfConnected(AthletMediaIsFree(a.wertung.mediafile.get, context))
            resetDisplay()
            lastAction = None
          case _ =>
        }
      })
      trackLabel.setText("Track " + (currentSongIndex + 1) + "/" + playList.getSongs.size)
      nameLabel.setText(playList.getSongs.get(currentSongIndex).getKey)
      timeLabel.textProperty() <== Bindings.createStringBinding(() => {
        def formatDuration(time: Duration) = {
          val minutes = time.toMinutes
          val minutesWhole = Math.floor(minutes).toInt
          val secondsWhole = ((minutes - minutesWhole) * 60).round.toInt
          String.format("%1$02d:%2$02d", minutesWhole, secondsWhole)
        }

        if (player.getStatus == null) "Streaming..."
        else player.getStatus match {
          case Status.PLAYING =>
            lastAction.foreach {
              case a: MediaPlayerAction => publishMediaEventIfConnected(AthletMediaIsRunning(a.wertung.mediafile.get, context))
              case _ =>
            }
            if (media == null || media.getDuration == null) "Time: 00:00   Remaining: 00:00   Total: 00:00"
            else if (player.getCurrentTime == null) "Time: 00:00   Remaining: 00:00   Total: " + formatDuration(media.getDuration)
            else "Time: " + formatDuration(player.getCurrentTime) + "   Remaining: " + formatDuration(media.getDuration.subtract(player.getCurrentTime)) + "   Total: " + formatDuration(media.getDuration)
          case Status.PAUSED =>
            lastAction.foreach {
              case a: MediaPlayerAction => publishMediaEventIfConnected(AthletMediaIsPaused(a.wertung.mediafile.get, context))
              case _ =>
            }
            if (media == null || media.getDuration == null) "Paused"
            else if (player.getCurrentTime == null) "Paused at: 00:00   Remaining: 00:00   Total: " + formatDuration(media.getDuration)
            else "Paused at: " + formatDuration(player.getCurrentTime) + "   Remaining: " + formatDuration(media.getDuration.subtract(player.getCurrentTime)) + "   Total: " + formatDuration(media.getDuration)
          case _ =>
            if (media == null || media.getDuration == null) "Streaming..."
            else if (player.getCurrentTime == null) "Paused at: 00:00   Remaining: 00:00   Total: " + formatDuration(media.getDuration)
            else "Paused at: " + formatDuration(player.getCurrentTime) + "   Remaining: " + formatDuration(media.getDuration.subtract(player.getCurrentTime)) + "   Total: " + formatDuration(media.getDuration)
        }
      }, player.currentTimeProperty(), player.statusProperty())
      balanceKnob.valueProperty() <==> player.balanceProperty()
      volumeKnob.valueProperty() <==> player.volumeProperty()
      leftVURotation.angleProperty <== Bindings.createDoubleBinding(() => {
        if (player.getStatus == null) -40d
        else player.getStatus match {
          case Status.PLAYING =>
            val zeroOne = leftVU.get
            -40 + (80 * zeroOne)
          case _ =>
            -40d
        }
      }, leftVU, player.statusProperty(), player.balanceProperty())

      rightVURotation.angleProperty <== Bindings.createDoubleBinding(() => {
        if (player.getStatus == null) -40d
        else player.getStatus match {
          case Status.PLAYING =>
            val zeroOne = rightVU.get
            -40 + (80 * zeroOne)
          case _ =>
            -40d
        }
      }, rightVU, player.statusProperty(), player.balanceProperty())

      player.setAudioSpectrumNumBands(10)
      player.setAudioSpectrumInterval(1d / 30d)
      for (i <- 0 until math.min(sliders.size(), player.getAudioEqualizer.getBands.size())) {
        sliders.get(i).valueProperty() <==> player.getAudioEqualizer.getBands.get(i).gainProperty()
      }
      for (i <- 0 until 10) {
        vuMeters.get(i).setValue(0)
      }

      player.setAudioSpectrumListener(spectrumListener)
    }
  }

  private def publishMediaEventIfConnected(action: KutuAppEvent) = {
    if (isNetworkMediaPlayer.getValue) {
      WebSocketClient.publish(action)
    }
  }
}