package ch.seidel.kutu

import ch.seidel.kutu.KuTuAppTree.showThumbnails
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.view.{VereinTableView, WettkampfTableView}
import scalafx.Includes.*
import scalafx.beans.property.BooleanProperty
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Orientation}
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.control.TreeItem.sfxTreeItemToJfx
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{KeyEvent, MouseEvent}
import scalafx.scene.layout.{Priority, Region, TilePane}

import java.io.IOException
import java.util.prefs.Preferences
import scala.collection.immutable.TreeMap

object AppNavigationModel {
  def create(service: KutuService): KuTuAppTree = new KuTuAppTree(service)
}

/**
 * Wettkampf (Home)
 *    - ATT Frühling 2015
 *    - Jugendcup 2015
 *      Stammdaten
 *    - Athlet (+Verein)
 *    - Programm (Athletiktest)
 *      - Programm (ATT Kraft)
 *      - Programm (ATT Beweglichkeit)
 *    - Dizsiplin (Barren, Reck, ...)
 *    - Programmdisziplin (Barren-P1, Reck-P1, ...)
 */
case class KuTuAppThumbNail(context: Any, button: Button, item: TreeItem[String])

object KuTuAppTree {
  val showThumbnails = new BooleanProperty()
  showThumbnails.value = Preferences.userRoot().getBoolean("showThumbnails", true)
  showThumbnails.onChange {
    Preferences.userRoot().putBoolean("showThumbnails", showThumbnails.value)
  }
}

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
    thumbnails map { group =>
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
      if wkinputStream == null then {
        throw new IOException("Unable to locate resource: " + wkfilePath)
      }
      val wkimage = new Image(wkinputStream)
      val vfilePath = "/images/verein-shadowed.png"
      val vinputStream = this.getClass.getResourceAsStream(vfilePath)
      if vinputStream == null then {
        throw new IOException("Unable to locate resource: " + vfilePath)
      }
      val vimage = new Image(vinputStream)

      def thmb(context: Any, path: String, node: String) = {
        val thmbitem = new TreeItem[String](node) {
          expanded = false
        }
        val img = new ImageView {
          context match {
            case _: Verein => image = vimage
            case _: WettkampfView => image = wkimage
          }
        }
        val t = context match {
          case wv: WettkampfView => f"${wv.titel}\n${wv.datum}%td.${wv.datum}%tm.${wv.datum}%tY\n${wv.programm.easyprint}" //wv.easyprint
          case v: Verein => s"${v.name}${v.verband.map("\n(" + _ + ")").getOrElse("")}"
          case _ => node
        }
        val button = new Button(node, img) {
          prefWidth = 250
          maxWidth = 250
          minWidth = 250
          prefHeight = 140
          //contentDisplay = ContentDisplay.Top
          styleClass.clear()
          styleClass += "sample-tile"
          wrapText = true
          tooltip = new Tooltip {
            text = t
          }
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
        }, false, 2) //,
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

  // val searchQuery = newVal.toUpperCase().split(" ")
  def dashboardFilter(query: String)(thumb: KuTuAppThumbNail) = {
    val filter = query.toUpperCase().split(" ")
    filter.isEmpty || filter.forall { txt =>
      thumb.item.value.value.toUpperCase.contains(txt) ||
        (thumb.context match {
          case v: Verein => v.easyprint.toUpperCase.contains(txt)
          case wv: WettkampfView => wv.easyprint.toUpperCase.contains(txt)
          case _ => false
        })
    }
  }

  def getDashThumbsCtrl(filter: String = ""): List[Node] =
    thumbnails.map {
      case (heading, ts) =>
        val nails = getThumbs(heading)
          .filter(dashboardFilter(filter))
        (Seq[Node](
          createCategoryLabel(heading),
          if showThumbnails.value then createTiles(nails) else createListViews(nails)
        ), ts._3)
    }.toList.sortBy(_._2).flatMap(_._1)

  def getDashThumb(ctrlGrpName: String, filter: String = "") = {
    val nails = getThumbs(ctrlGrpName)
      .filter(dashboardFilter(filter))
    Seq(
      createCategoryLabel(ctrlGrpName),
      if showThumbnails.value then createTiles(nails) else createListViews(nails)
    )
  }

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

  private def createListViews(value: List[KuTuAppThumbNail]) = {
    val wkTable = new WettkampfTableView(value
      .filter(_.context.isInstanceOf[WettkampfView])
      .map(_.context.asInstanceOf[WettkampfView])) {
      hgrow = Priority.Always
      vgrow = Priority.Always
    }
    if wkTable.items.value.nonEmpty then {
      def openWK(): Unit = {
        val wk = wkTable.selectionModel.value.getSelectedItem
        value.find(n => n.context.equals(wk)).foreach { thmbitem =>
          KuTuApp.controlsView.selectionModel().select(thmbitem.item)
        }
      }

      wkTable.onKeyPressed = (ae: KeyEvent) => {
        if ae.code.isWhitespaceKey then openWK()
      }
      wkTable.onMouseClicked = (ae: MouseEvent) => {
        if ae.clickCount > 1 then openWK()
      }
      wkTable
    } else {
      val vereinTable = VereinTableView(value
        .filter(_.context.isInstanceOf[Verein])
        .map(_.context.asInstanceOf[Verein]))

      def openVerein(): Unit = {
        val verein = vereinTable.selectionModel.value.getSelectedItem
        value.find(n => n.context.equals(verein)).foreach { thmbitem =>
          KuTuApp.controlsView.selectionModel().select(thmbitem.item)
        }
      }

      vereinTable.onKeyPressed = (ae: KeyEvent) => {
        if ae.code.isWhitespaceKey then openVerein()
      }
      vereinTable.onMouseClicked = (ae: MouseEvent) => {
        if ae.clickCount > 1 then openVerein()
      }
      vereinTable
    }
  }
}