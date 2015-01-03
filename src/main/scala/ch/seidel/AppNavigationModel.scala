package ch.seidel

import java.io.{File, IOException}
import scala.collection.immutable.TreeMap
import scalafx.Includes._
import ch.seidel.commons.{ExampleInfo, PageDisplayer, SortUtils}
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Orientation}
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{Region, TilePane}
import ch.seidel.domain.KutuService

object AppNavigationModel  {
  def create(service: KutuService): KuTuAppTree = new KuTuAppTree(service)
}

/**
 * Wettkampf (Home)
 *    - ATT Frühling 2015
 *    - Jugendcup 2015
 * Stammdaten
 *    - Athlet (+Verein)
 *    - Programm (Athletiktest)
 *      - Programm (ATT Kraft)
 *      - Programm (ATT Beweglichkeit)
 *    - Dizsiplin (Barren, Reck, ...)
 *    - Programmdisziplin (Barren-P1, Reck-P1, ...)
 */
case class KuTuAppThumbNail(context: Any, button: Button)

/**
 * The class provide accessibility methods to access the
 * underlying map
 */
class KuTuAppTree(service: KutuService) {

  val tree: Map[String, List[TreeItem[String]]] = createTree()
  val thumbnails: Map[String, List[KuTuAppThumbNail]] = createThumbnails()

  def getService = service

  /**
   * build a map by iterating through the examples folder.
   * This is used in UI
   */
  private def createTree(): Map[String, List[TreeItem[String]]] = {
//    val exampleRootFiles = examplePath.listFiles()
//    if (exampleRootFiles == null)
//      throw new IOException("Cannot list files in the example directory. May be caused by Issue #10.")
    implicit val session = service.database.createSession()
    try {
      TreeMap(
//          "Athleten" -> service.selectAthletesView.list().map { a => new TreeItem(s"${a.vorname} ${a.name} (${a.verein.map { _.name }.getOrElse("ohne Verein")})") },
          "Wettkämpfe" -> service.listWettkaempfeView.map { wk => new TreeItem(s"${wk.titel} ${wk.datum}") }.toList
          )
    }
    finally {
      session.close()
    }
  }

  private def createThumbnails() = {
    // Sanity check, the listing mey not work when ScalaFX KuTuApp is packaged into a jar.
//    val exampleRootFiles = examplePath.listFiles()
//    if (exampleRootFiles == null)
//      throw new IOException("Cannot list files in the example directory. May be caused by Issue #10.")

    implicit val session = service.database.createSession()
    try {
      val img = new ImageView {
        val filePath = "/images/icon-48x48.png"
        val inputStream = this.getClass.getResourceAsStream(filePath)
        if (inputStream == null) {
          throw new IOException("Unable to locate resource: " + filePath)
      }
        image = new Image(inputStream)
      }
      def thmb(context: Any, path: String, node: String) = {
        val button = new Button(node, img) {
          prefWidth = 140
          prefHeight = 145
          contentDisplay = ContentDisplay.TOP
          styleClass.clear()
          styleClass += "sample-tile"
          onAction = (ae: ActionEvent) => {
            KuTuApp.splitPane.items.remove(1)
            KuTuApp.splitPane.items.add(1,
              PageDisplayer.choosePage(Some(context), path + " > " + node, KuTuAppTree.this))
          }
        }
        KuTuAppThumbNail(context, button)
      }
      TreeMap(
//          "Athleten" -> service.selectAthletes.list().map { a =>
//              thmb("Athleten", s"${a.vorname} ${a.name}")
//            },
          "Wettkämpfe" -> service.listWettkaempfeView.map { wk =>
              thmb(wk, "Wettkämpfe", s"${wk.titel} ${wk.datum}")
            }.toList
//            ,
//          "Analysen" -> service.selectWertungen().map { d =>
//              thmb("Analysen", s"d.wettkampfdisziplin.disziplin.name}%s")
//            }.toList
          )
    }
    finally {
      session.close()
    }
  }

  def getLeaves(keyName: String) = tree(keyName)

  /**
   * returns the entire tree
   */
  def getTree: List[TreeItem[String]] = tree.map {
    case (name, items) => new TreeItem[String](name) {
      expanded = true
      children = items
    }
  }.toList

  def getThumbs(keyName: String) = thumbnails(keyName)

  def getDashThumbsCtrl =
    thumbnails.flatMap {
      case (heading, ts) => Seq(createCategoryLabel(heading), createTiles(ts))
    }

  def getDashThumb(ctrlGrpName: String) =
    Seq(
      createCategoryLabel(ctrlGrpName),
      createTiles(thumbnails(ctrlGrpName))
    )

  private def createCategoryLabel(value: String) =
    new Label {
      text = value
      maxWidth = Double.MaxValue
      minHeight = Region.USE_PREF_SIZE
      styleClass += "category-header"
    }

  private def createTiles(value: List[KuTuAppThumbNail]) = new TilePane {
    prefColumns = 1
    hgap = 4
    vgap = 4
    padding = Insets(10, 10, 10, 10)
    orientation = Orientation.HORIZONTAL
    styleClass += "category-page-flow"
    content = value.map(_.button)
  }
}