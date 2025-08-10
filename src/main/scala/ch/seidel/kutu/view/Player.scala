package ch.seidel.kutu.view

import javafx.scene.media.{Media, MediaPlayer}
import javafx.util.Duration

class MediaController {
  var player: Option[Player] = Option.empty

  def prepare(song: String): Player = {
    player.foreach(p => p.player.dispose())
    player = Some(Player(song))
    player.get
  }

  def play(): Unit = {
    player.foreach(_.player.play())
  }
  def pause(): Unit = {
    player.foreach(_.player.pause())
  }
  def stop(): Unit = {
    player.foreach(_.player.stop())
  }
  def toStart(): Unit = {
    player.foreach(_.player.stop())
    player.foreach(_.player.seek(Duration.seconds(0)))
  }
}

private case class Player(file: String) {
  private val media = new Media(file)
  val player = new MediaPlayer(media)
  player.setAutoPlay(false)
}
