package ch.seidel.kutu.view.player

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ObservableList}
import javafx.concurrent.Worker.State
import javafx.concurrent.{Task, Worker}
import javafx.util.Pair
import org.w3c.dom.Element

import java.io.{File, FilenameFilter, InputStreamReader}
import java.net.{URI, URL, URLEncoder}
import java.util
import java.util.Scanner
import javax.xml.parsers.DocumentBuilderFactory


class PlayList {
  private val songs = FXCollections.observableArrayList[Pair[String, String]]
  private var url: String = null

  def load(url: String): Unit = {
    this.url = url
    if url.toLowerCase.endsWith(".mp3") then songs.add(new Pair[String, String](url.substring(url.lastIndexOf('/')+1, url.lastIndexOf('.')-1), url))
    else if url.toLowerCase.endsWith(".m3u") then loadM3U(url)
    else if url.toLowerCase.endsWith(".xml") then loadPhlowXML(url)
    else {
      val dir = new java.io.File(URI.create(url))
      if dir.exists() then {
        val files = dir.listFiles(new FilenameFilter {
          override def accept(dir: File, name: String): Boolean = {
            val ln = name.toLowerCase
            ln.endsWith(".mp3")
            /*(
              ln.toLowerCase.endsWith(".aif")
              || ln.endsWith(".aiff")
              || ln.endsWith(".fxm")
              || ln.endsWith(".flv")
              || ln.endsWith(".m3u8")
              || ln.endsWith(".mp3")
              || ln.endsWith(".mp4")
              || ln.endsWith(".m4a")
              || ln.endsWith(".m4v")
              || ln.endsWith(".wav"))
             */
          }
        })
        if files != null then {
          files
            .toList
            .filter(_ != null)
            .foreach(audiofile => {
              val fn = audiofile.toPath.getFileName.toString
              songs.add(new Pair[String, String](fn.substring(0, fn.lastIndexOf('.')), audiofile.toURI.toString))
            })
        }
      }
    }
  }

  private def loadM3U(url: String): Unit = {
    val fetchPlayListTask = new Task[util.List[Pair[String, String]]]() {
      @throws[Exception]
      override protected def call: util.List[Pair[String, String]] = {
        println("call()")
        val baseUrl = url.substring(0, url.lastIndexOf('/') + 1)
        println("baseUrl = " + baseUrl)
        val songs = new util.ArrayList[Pair[String, String]]
        val con = URI.create(url).toURL.openConnection
        val in = new InputStreamReader(con.getInputStream, "ISO-8859-1")
        val source = new Scanner(in)
        while source.hasNextLine do {
          val inputLine = source.nextLine()
          println(inputLine)
          if inputLine.charAt(0) != '#' then songs.add(new Pair[String, String](inputLine.substring(0, inputLine.lastIndexOf('.')), baseUrl + URLEncoder.encode(inputLine, "UTF-8")))
        }
        in.close()
        System.out.println("content = " + util.Arrays.toString(songs.toArray))
        songs
      }
    }
    fetchPlayListTask.stateProperty.addListener(new ChangeListener[Worker.State]() {
      override def changed(arg0: ObservableValue[? <: Worker.State], oldState: Worker.State, newState: Worker.State): Unit = {
        println("newState = " + newState)
        if newState eq State.SUCCEEDED then try songs.addAll(fetchPlayListTask.get)
        catch {
          case ex: Exception =>
            ex.printStackTrace()
        }
        else if fetchPlayListTask.getException != null then fetchPlayListTask.getException.printStackTrace()
      }
    })
    new Thread(fetchPlayListTask).start()
  }

  private def loadPhlowXML(url: String): Unit = {
    val fetchPlayListTask = new Task[util.List[Pair[String, String]]]() {
      @throws[Exception]
      override protected def call: util.List[Pair[String, String]] = {
        val baseUrl = url.substring(0, url.lastIndexOf('/') + 1)
        val songs = new util.ArrayList[Pair[String, String]]
        val con = URI.create(url).toURL.openConnection
        val dbFactory = DocumentBuilderFactory.newInstance
        val dBuilder = dbFactory.newDocumentBuilder
        val doc = dBuilder.parse(con.getInputStream)
        val root = doc.getDocumentElement
        val children = root.getElementsByTagName("file")
        for i <- 0 until children.getLength do {
          val child = children.item(i).asInstanceOf[Element]
          val name = child.getAttributes.getNamedItem("name").getTextContent
          if name.endsWith(".mp3") then {
            val titleElements = child.getElementsByTagName("title")
            var title: String = null
            if titleElements.getLength > 0 then title = titleElements.item(0).getTextContent.trim
            else title = name.substring(0, name.lastIndexOf('.'))
            songs.add(new Pair[String, String](title, baseUrl + URLEncoder.encode(name, "UTF-8")))
          }
        }
        songs
      }
    }
    fetchPlayListTask.stateProperty.addListener(new ChangeListener[Worker.State]() {
      override def changed(arg0: ObservableValue[? <: Worker.State], oldState: Worker.State, newState: Worker.State): Unit = {
        if newState eq State.SUCCEEDED then try songs.addAll(fetchPlayListTask.get)
        catch {
          case ex: Exception =>
            ex.printStackTrace()
        }
        else if fetchPlayListTask.getException != null then fetchPlayListTask.getException.printStackTrace()
      }
    })
    new Thread(fetchPlayListTask).start()
  }

  def getUrl: String = url

  def getSongs: ObservableList[Pair[String, String]] = songs
}