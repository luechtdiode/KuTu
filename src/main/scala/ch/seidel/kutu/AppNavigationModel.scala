package ch.seidel.kutu

import java.io.IOException

import ch.seidel.kutu.domain._
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Orientation}
import scalafx.scene.Node
import scalafx.scene.control.TreeItem.sfxTreeItemToJfx
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{Region, TilePane}

import scala.collection.immutable.TreeMap

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
case class KuTuAppThumbNail(context: Any, button: Button, item: TreeItem[String])

/**
 * The class provide accessibility methods to access the
 * underlying map
 */
class KuTuAppTree(service: KutuService) {

  lazy val thumbnails: Map[String, (List[KuTuAppThumbNail], Boolean, Int)] = createThumbnails()
	lazy val tree: Map[String, (List[TreeItem[String]], Boolean, Int)] = createTree()

  def getService = service

  /**
   * build a map by iterating through the examples folder.
   * This is used in UI
   */
  private def createTree(): Map[String, (List[TreeItem[String]], Boolean, Int)] = {
    thumbnails map {group =>
      group._1 -> (group._2._1.map(_.item), group._2._2, group._2._3)
    }
  }

  private def createThumbnails() = {
    // Sanity check, the listing mey not work when ScalaFX KuTuApp is packaged into a jar.
//    val exampleRootFiles = examplePath.listFiles()
//    if (exampleRootFiles == null)
//      throw new IOException("Cannot list files in the example directory. May be caused by Issue #10.")

    implicit val session = service.database.createSession()
    try {
      val wkfilePath = "/images/wettkampf-shadowed.png"
      val wkinputStream = this.getClass.getResourceAsStream(wkfilePath)
      if (wkinputStream == null) {
        throw new IOException("Unable to locate resource: " + wkfilePath)
      }
      val wkimage = new Image(wkinputStream)
      val vfilePath = "/images/verein-shadowed.png"
      val vinputStream = this.getClass.getResourceAsStream(vfilePath)
      if (vinputStream == null) {
        throw new IOException("Unable to locate resource: " + vfilePath)
      }
      val vimage = new Image(vinputStream)
      def thmb(context: Any, path: String, node: String) = {
        val thmbitem = new TreeItem[String](node) {
          expanded = false
        }
        val img = new ImageView {
          context match {
            case _:Verein        => image = vimage
            case _:WettkampfView => image = wkimage
          }
        }
        val button = new Button(node, img) {
          prefWidth = 140
          prefHeight = 145
          contentDisplay = ContentDisplay.Top
          styleClass.clear()
          styleClass += "sample-tile"
          onAction = (ae: ActionEvent) => {
            KuTuApp.controlsView.selectionModel().select(thmbitem)
          }
        }
        KuTuAppThumbNail(context, button, thmbitem)
      }
      TreeMap(
          "Wettkämpfe" -> (service.listWettkaempfeView.map { wk =>
              thmb(wk, "Wettkämpfe", s"${wk.titel} ${wk.datum}")
            }.toList, true, 1),
            "Athleten" -> (service.selectVereine.map { a =>
              thmb(a, "Athleten", s"${a.name}" + a.verband.map(v => s" ($v)").getOrElse(""))
            }, false, 2)//,
//          "Analysen" -> service.selectWertungen().map { d =>
//              thmb("Analysen", s"d.wettkampfdisziplin.disziplin.name}%s")
//            }.toList
          )
    }
    finally {
      session.close()
    }
  }

  def getLeaves(keyName: String) = tree(keyName)._1

  /**
   * returns the entire tree
   */
  def getTree: List[TreeItem[String]] =
    tree.toList.sortBy(_._2._3).map {
    case (name, (items, exp, ord)) => new TreeItem[String](name) {
      expanded = exp
      children = items
    }
  }

  def getThumbs(keyName: String): List[KuTuAppThumbNail] =
    thumbnails.getOrElse(keyName, (List[KuTuAppThumbNail](), false, 0))._1

  def getDashThumbsCtrl: List[Node] =
    thumbnails.map {
      case (heading, ts) => (Seq[Node](createCategoryLabel(heading), createTiles(ts._1)), ts._3)
    }.toList.sortBy(_._2).flatMap(_._1)

  def getDashThumb(ctrlGrpName: String) =
    Seq(
      createCategoryLabel(ctrlGrpName),
      createTiles(getThumbs(ctrlGrpName))
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
    orientation = Orientation.Horizontal
    styleClass += "category-page-flow"
    children = value.map(_.button)
  }
}