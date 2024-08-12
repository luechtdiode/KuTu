package ch.seidel.kutu.view

import ch.seidel.commons.{AutoCommitTextFieldTableCell, DisplayablePage, PageDisplayer, TabWithService}
import ch.seidel.kutu.Config._
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.KuTuApp.hostServices
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.{Disziplin, Durchgang, GemischteRiegen, GemischterDurchgang, GetrennteDurchgaenge, KutuService, Riege, RiegeRaw, SexDivideRule, WettkampfView, encodeFileName, str2Int, toDurationFormat, toTimeFormat}
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer.{PrintUtil, RiegenBuilder, WertungsrichterQRCode, WertungsrichterQRCodesToHtmlRenderer}
import ch.seidel.kutu.squad.DurchgangBuilder
import javafx.scene.text.Text
import javafx.scene.{control => jfxsc}
import scalafx.Includes.{eventClosureWrapperWithParam, jfxActionEvent2sfx, jfxBooleanBinding2sfx, jfxBounds2sfx, jfxCellEditEvent2sfx, jfxKeyEvent2sfx, jfxMouseEvent2sfx, jfxObjectProperty2sfx, jfxParent2sfx, jfxPixelReader2sfx, jfxReadOnlyBooleanProperty2sfx, jfxTableViewSelectionModel2sfx, jfxText2sfxText, observableList2ObservableBuffer, when}
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.event.ActionEvent
import scalafx.geometry._
import scalafx.print.PageOrientation
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableColumn.{sfxTableColumn2jfx, CellEditEvent => TableCellEditEvent}
import scalafx.scene.control.TableView.sfxTableView2jfx
import scalafx.scene.control.TreeTableColumn.sfxTreeTableColumn2jfx
import scalafx.scene.control.TreeTableView.sfxTreeTableView2jfx
import scalafx.scene.control.{ContextMenu, _}
import scalafx.scene.control.cell.{CheckBoxListCell, CheckBoxTableCell, ComboBoxTableCell}
import scalafx.scene.image.{Image, ImageView, WritableImage}
import scalafx.scene.input.{ClipboardContent, DataFormat, KeyEvent, TransferMode}
import scalafx.scene.layout._
import scalafx.scene.{Cursor, Node}
import scalafx.util.StringConverter
import scalafx.util.converter.DefaultStringConverter

import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.NANOS
import java.time.{Duration, LocalDateTime, LocalTime, ZoneOffset}
import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.duration.MILLISECONDS

object DurchgangView {
  val DRAG_RIEGE = new DataFormat("application/x-drag-riege");
  def getVisibleBounds(aNode: Node): Bounds = {
      // If node not visible, return empty bounds
      if(!aNode.isVisible) {
        new BoundingBox(0,0,-1,-1)
      } else if(aNode.getClip != null) {
        aNode.getClip.getBoundsInParent
      } else {
        // If node has parent, get parent visible bounds in node coords
        val bounds = if (aNode.getParent()!=null) getVisibleBounds(aNode.getParent()) else null
        if(bounds != null && !bounds.isEmpty) aNode.parentToLocal(bounds) else bounds
      }
  }
  def positionInTarget(aNode: Node, aTargetNode: Object): Point2D = {
    if (aNode.parent.value == aTargetNode) {
      new Point2D(aNode.getLayoutX,  aNode.getLayoutY)
    } else {
      val parentBounds = positionInScene(aNode.parent.value)
      new Point2D(aNode.getLayoutX + parentBounds.getX, aNode.getLayoutY + parentBounds.getY)
    }
  }
  def positionInScene(aNode: Node): Point2D = {
    if (aNode.parent.value == null) {
      new Point2D(aNode.getLayoutX,  aNode.getLayoutY)
    } else {
      val parentBounds = positionInScene(aNode.parent.value)
      new Point2D(aNode.getLayoutX + parentBounds.getX, aNode.getLayoutY + parentBounds.getY)
    }
  }
}
trait DurchgangTCAccess extends TCAccess[DurchgangEditor, Seq[RiegeEditor],Disziplin] {
  def getDisziplin = getIndex
}
class DurchgangJFSCTableColumn[T](val index: Disziplin) extends jfxsc.TableColumn[DurchgangEditor, T] with DurchgangTCAccess {
  override def getIndex: Disziplin = index
  override def valueEditor(selectedRow: DurchgangEditor): Seq[RiegeEditor] = selectedRow.valueEditor(index)
}
class DurchgangTableColumn[T](val index: Disziplin) extends TableColumn[DurchgangEditor, T] with DurchgangTCAccess {
  override val delegate: jfxsc.TableColumn[DurchgangEditor, T] = new DurchgangJFSCTableColumn[T](index)
  override def getIndex: Disziplin = index
  override def valueEditor(selectedRow: DurchgangEditor): Seq[RiegeEditor] = selectedRow.valueEditor(index)
}
class DurchgangJFSCTreeTableColumn[T](val index: Disziplin) extends jfxsc.TreeTableColumn[DurchgangEditor, T] with DurchgangTCAccess {
  override def getIndex: Disziplin = index
  override def valueEditor(selectedRow: DurchgangEditor): Seq[RiegeEditor] = selectedRow.valueEditor(index)
}
class DurchgangTreeTableColumn[T](val index: Disziplin) extends TreeTableColumn[DurchgangEditor, T] with DurchgangTCAccess {
  override val delegate: jfxsc.TreeTableColumn[DurchgangEditor, T] = new DurchgangJFSCTreeTableColumn[T](index)
  override def getIndex: Disziplin = index
  override def valueEditor(selectedRow: DurchgangEditor): Seq[RiegeEditor] = selectedRow.valueEditor(index)
}

class DurchgangView(wettkampf: WettkampfView, service: KutuService, disziplinlist: () => Seq[Disziplin], durchgangModel: ObservableBuffer[TreeItem[DurchgangEditor]]) extends TreeTableView[DurchgangEditor] {

  id = "durchgang-table"
//  items = durchgangModel
  showRoot = false
  tableMenuButtonVisible = true

  private val rootEditor = DurchgangEditor(wettkampf.id, Durchgang(), List.empty)

  root = new TreeItem[DurchgangEditor](rootEditor) {
    durchgangModel.onChange{
      children = durchgangModel.toList
    }
    styleClass.add("parentrow")
    expanded = true
  }

  columns ++= Seq(
    new TreeTableColumn[DurchgangEditor, String] {
      prefWidth = 130
      text = "Durchgang"
      cellValueFactory = { x => StringProperty(
        if (x.value.getValue.durchgang.name.equals(x.value.getValue.durchgang.title)) {
          s"""${x.value.getValue.durchgang.name}
             |Start: ${x.value.getValue.durchgang.effectivePlanStart(wettkampf.datum.toLocalDate)}
             |Ende: ${x.value.getValue.durchgang.effectivePlanFinish(wettkampf.datum.toLocalDate)}""".stripMargin
        } else {
          x.value.getValue.durchgang.name
        })
      }
    }
    , new TreeTableColumn[DurchgangEditor, String] {
      prefWidth = 40
      text = "Sum"
      cellValueFactory = { x => x.value.getValue.anz.asInstanceOf[ObservableValue[String,String]]}
    }
    , new TreeTableColumn[DurchgangEditor, String] {
      prefWidth = 40
      text = "Min"
      cellValueFactory = { x => x.value.getValue.min.asInstanceOf[ObservableValue[String,String]]}
    }
    , new TreeTableColumn[DurchgangEditor, String] {
      prefWidth = 40
      text = "Max"
      cellValueFactory = { x => x.value.getValue.max.asInstanceOf[ObservableValue[String,String]]}
    }
    , new TreeTableColumn[DurchgangEditor, String] {
      prefWidth = 30
      text = "ø"
      cellValueFactory = { x => x.value.getValue.avg.asInstanceOf[ObservableValue[String,String]]}
    }
    , new TreeTableColumn[DurchgangEditor, String] {
      prefWidth = 110
      text = "Zeitbedarf"
      cellValueFactory = { x => StringProperty(
        s"""Tot:     ${toDurationFormat(x.value.getValue.durchgang.planTotal)}
           |Eint.:    ${toDurationFormat(x.value.getValue.durchgang.planEinturnen)}
           |Gerät.: ${toDurationFormat(x.value.getValue.durchgang.planGeraet)}""".stripMargin)}
    }
  )

  columns ++= disziplinlist().map {disziplin =>
    val dc: jfxsc.TreeTableColumn[DurchgangEditor, String] = new TreeTableColumn[DurchgangEditor, String] {
      text = disziplin.name
      prefWidth = 230
      columns ++= Seq(
          new DurchgangTreeTableColumn[String](disziplin) {
            text = "Riege"
            prefWidth = 190
            cellValueFactory = { x =>
              x.value.getValue.valueEditor(disziplin) match {
                case re: Seq[RiegeEditor] if (re.nonEmpty) => StringProperty(re.map(rs => s"${rs.name.value} (${rs.anz.value})").mkString("\n"))
                case _ => StringProperty("")
              }
            }
          }
          , new TreeTableColumn[DurchgangEditor, String] {
            text = "Anz"
            prefWidth = 40
            cellValueFactory = { x =>
              x.value.getValue.valueEditor(disziplin) match {
                case re: Seq[RiegeEditor] if (re.nonEmpty) => StringProperty(re.map(rs => rs.anz.value).sum.toString)
                case _ => StringProperty("0")
              }
            }
          }
        )
    }
    dc
  }

}

class RiegenFilterView(isEditable: BooleanProperty, wettkampf: WettkampfView, service: KutuService, disziplinlist: () => Seq[Disziplin], asFilter: Boolean, riegenFilterModel: ObservableBuffer[RiegeEditor]) extends TableView[RiegeEditor] {
  type RigenChangeListener = RiegeEditor => Unit
  var changeListeners = List[RigenChangeListener]()

  def addListener(listener: RigenChangeListener): Unit = {
    changeListeners = changeListeners :+ listener
  }

  def removeListener(listener: RigenChangeListener) : Unit ={
    changeListeners = changeListeners filter(f => f != listener)
  }

  def fireRiegeChanged(riege: RiegeEditor): Unit = {
    changeListeners.foreach(listener => listener(riege))
  }

  items = riegenFilterModel
  id = "riege-table"
  editable = true

  if(asFilter) {
    columns ++= List(
    new TableColumn[RiegeEditor, Boolean] {
      text = "Filter"
      cellValueFactory = { x => x.value.selected.asInstanceOf[ObservableValue[Boolean,Boolean]] }
      cellFactory.value = { _:Any => new CheckBoxTableCell[RiegeEditor, Boolean]() }
      editable = true
    })
  }

  columns ++= List(
    new TableColumn[RiegeEditor, String] {
      text = "Riege"
      prefWidth = 190
      cellValueFactory = { x => x.value.name }
      editable  <== when(Bindings.createBooleanBinding(() => {
        !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && isEditable.value
      },
        isEditable
      )) choose true otherwise false
      cellFactory.value = { _:Any => new AutoCommitTextFieldTableCell[RiegeEditor, String](new DefaultStringConverter()) }
      onEditCommit = (evt: TableCellEditEvent[RiegeEditor, String]) => {
        val editor = evt.rowValue
        editor.name.value = evt.newValue
        val updated = RiegeEditor(
            editor.wettkampfid,
            editor.initanz,
            editor.initviewanz,
            editor.enabled,
            Riege(editor.name.value, editor.initdurchgang, editor.initstart, editor.kind),
            editor.onSelectedChange)
        service.renameRiege(wettkampf.id, editor.initname, evt.newValue)
        fireRiegeChanged(updated)
        val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
        evt.tableView.selectionModel.value.select(rowIndex, this)
        evt.tableView.sort()
        evt.tableView.requestFocus()
      }
    }
  )
//  if(asFilter) {
//    columns ++= List(
//      new TableColumn[RiegeEditor, String] {
//        text = "Riege"
//        prefWidth = 190
//        cellValueFactory = { x => x.value.name }
//      }
//    )
//  }

  columns ++= List(
    new TableColumn[RiegeEditor, String] {
      text = "Anz"
      prefWidth = 80
      editable = false
      cellValueFactory = { if(asFilter) {
          x => x.value.anzkat.asInstanceOf[ObservableValue[String,String]]
        }
        else {
          x => x.value.anz.asInstanceOf[ObservableValue[String,String]]
        }
      }
    }
    , new TableColumn[RiegeEditor, String] {
      text = "Durchgang"
      prefWidth = 130
      cellValueFactory = { x => x.value.durchgang }
      cellFactory.value = { _:Any => new AutoCommitTextFieldTableCell[RiegeEditor, String](new DefaultStringConverter()) }
      editable  <== when(Bindings.createBooleanBinding(() => {
        !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && isEditable.value
      },
        isEditable
      )) choose true otherwise false
      onEditCommit = (evt: TableCellEditEvent[RiegeEditor, String]) => {
        val editor = evt.rowValue
        editor.durchgang.value = evt.newValue
        val updated = RiegeEditor(
            evt.rowValue.wettkampfid,
            evt.rowValue.initanz,
            evt.rowValue.initviewanz,
            evt.rowValue.enabled,
            service.updateOrinsertRiege(editor.commit),
            evt.rowValue.onSelectedChange)
        fireRiegeChanged(updated)
        val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
        evt.tableView.selectionModel.value.select(rowIndex, this)
        evt.tableView.requestFocus()
      }
    }
    , new TableColumn[RiegeEditor, Disziplin] {
      text = "Start"
      prefWidth = 400
      val converter = new StringConverter[Disziplin] {
        override def toString(d: Disziplin) = if(d != null) d.easyprint else ""
        override def fromString(s: String) = if(s != null) disziplinlist().find { d => d.name.equals(s) }.getOrElse(null) else null
      }
      val list = ObservableBuffer.from(disziplinlist())
      cellValueFactory = { x => x.value.start.asInstanceOf[ObservableValue[Disziplin,Disziplin]] }
      cellFactory.value = { _:Any => new ComboBoxTableCell[RiegeEditor, Disziplin](converter, list) }
      editable  <== when(Bindings.createBooleanBinding(() => {
        !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && isEditable.value
      },
        isEditable
      )) choose true otherwise false
      onEditCommit = (evt: TableCellEditEvent[RiegeEditor, Disziplin]) => {
        val editor = evt.rowValue
        editor.start.value = evt.newValue
        val updated = RiegeEditor(
            evt.rowValue.wettkampfid,
            evt.rowValue.initanz,
            evt.rowValue.initviewanz,
            evt.rowValue.enabled,
            service.updateOrinsertRiege(evt.rowValue.commit),
            evt.rowValue.onSelectedChange)
        fireRiegeChanged(updated)
        val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
        evt.tableView.selectionModel.value.select(rowIndex, this)
        evt.tableView.requestFocus()
      }
    }
  )
}

class RiegenTab(override val wettkampfInfo: WettkampfInfo, override val service: KutuService) extends Tab with TabWithService with ExportFunctions {
  val programmText = wettkampf.programm.id match {case 20 => "Kategorie" case _ => "Programm"}
  val riegenFilterModel: ObservableBuffer[RiegeEditor] = ObservableBuffer[RiegeEditor]()
  val incompleteAssignments: ObservableBuffer[RiegeEditor] = ObservableBuffer[RiegeEditor]()
  val durchgangModel = ObservableBuffer[TreeItem[DurchgangEditor]]()
  val disziplinlist = wettkampfInfo.disziplinList

  def warnings = durchgangModel.nonEmpty && incompleteAssignments.nonEmpty
  val warnPanelVisible: BooleanProperty = new BooleanProperty()

  closable = false
  text = "Riegeneinteilung"

  def reloadRiegen(): Unit = {
    import scala.jdk.CollectionConverters._
    riegenFilterModel.clear()
    riegenFilterModel.addAll(riegen().asJavaCollection)
  }

  def reloadDurchgaenge(): Unit = {
    val expandedStates = durchgangModel
        .filter(_.isExpanded)
        .map(_.value.value.title.value)
        .toSet
    durchgangModel.clear()
    incompleteAssignments.setAll(riegenFilterModel.filter(re => re.initanz > 0 && (re.initdurchgang.isEmpty || re.initstart.isEmpty)))
    val durchgaenge = service.selectDurchgaenge(wettkampf.uuid.map(UUID.fromString).get).map(d => d.name->d).toMap
    val durchgangEditors = riegenFilterModel.groupBy(re => re.initdurchgang)
      .filter(_._1.isDefined)
      .toList.sortBy(_._1)
      .map{res =>
        val (durchgang, rel) = res
        DurchgangEditor(wettkampf.id, durchgaenge.getOrElse(durchgang.get, Durchgang(wettkampf.id, durchgang.get)), rel.toList)
      }
    for (group <- DurchgangEditor(durchgangEditors)) {
      group match {
        case gd: GroupDurchgangEditor =>
          durchgangModel.add(new TreeItem[DurchgangEditor](gd) {
            for (d <- gd.aggregates) {
              children.add(new TreeItem[DurchgangEditor](d))
            }
            //        styleableParent.styleClass.add("parentrow")
            expanded = expandedStates.contains(gd.title.value)
          })
        case g: DurchgangEditor =>
          durchgangModel.add(new TreeItem[DurchgangEditor](g))
      }
    }
    warnPanelVisible.value = warnings
  }

  def reloadData(): Unit = {
    reloadRiegen()
    reloadDurchgaenge()

  }

  def onNameChange(name1: String, name2: String) = {
    reloadData()
  }

  def onSelectedChange(name: String, selected: Boolean) = {
    selected
  }

  def onRiegeChanged(sorter: () => Unit)(editor: RiegeEditor): Unit = {
    reloadData()
    sorter()
//    reloadDurchgaenge()
  }

  // This handles also the initial load
  onSelectionChanged = _ => {
    if(selected.value) {
      reloadData()
    }
  }

  def riegen(): IndexedSeq[RiegeEditor] = {
    service.listRiegenZuWettkampf(wettkampf.id).sortBy(r => r._1).map(x =>
      RiegeEditor(
          wettkampf.id,
          x._1,
          x._2,
          0,
          true,
          x._3,
          x._4,
          None)).sortBy { re =>
            re.initdurchgang.toString() + re.initname + re.initstart.toString()
          }

  }

  val txtGruppengroesse = new TextField() {
    text = if(wettkampfInfo.isAthletikTest) "0" else "11"
    tooltip = "Max. Gruppengrösse oder 0 für gleichmässige Verteilung mit einem Durchgang."
  }

  override def release: Unit = {
    subscription.cancel();
  }

  val editableProperty: BooleanProperty = new BooleanProperty()

  override def isPopulated: Boolean = {
    editableProperty.set(true)
    val wettkampfEditable = !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)
    val riegenFilterView = new RiegenFilterView(editableProperty,
        wettkampf, service,
        () => {disziplinlist},
        false,
        riegenFilterModel)

    val unassignedRiegenView = new ListView[RiegeEditor] {
      items = incompleteAssignments
      orientation = Orientation.Horizontal
      prefHeight = 40 /*<== when(Bindings.createBooleanBinding(() => {
        incompleteAssignments.isEmpty
      },
        incompleteAssignments
      )) choose 0 otherwise 40
      */

      cellFactory = { (l, c) =>
        l.text = s"${c.initname} (${c.initanz})"
        l.onMouseEntered = _ => {
          l.cursor = Cursor.OpenHand
        }
        l.onMouseExited = _ => {
          l.cursor = Cursor.Default
        }
        l.setOnDragDetected(event => {
            val hoveredText = l.text.value
            val snp = l.snapshot(null, null)
            val width = snp.getWidth.toInt
            val height: Int = snp.getHeight.toInt-10
            val croppedImage = new WritableImage(snp.getPixelReader(), 0, 5, width, height)
            val db = startDragAndDrop(TransferMode.Move)
            db.setDragView(croppedImage)
            val content = new ClipboardContent()
            content.put(DurchgangView.DRAG_RIEGE, ("", c.initname, 0L))
            content.putString(hoveredText)
            db.setContent(content)

            event.consume()
        })
        l.contextMenu = new ContextMenu() {
          durchgangModel
            .flatMap(d => d.getValue match {
              case gd: GroupDurchgangEditor =>
                gd.aggregates
              case ce: CompetitionDurchgangEditor =>
                List(ce)
            })
            .foreach{durchgang =>
              items += new Menu(s"in ${durchgang.name.value} einteilen ...") {
                disziplinlist
                  .foreach { start =>
                    items += KuTuApp.makeMenuAction(s"auf Startgerät ${start.name} (${durchgang.initstartriegen.get(start).map(r => r.map(re => re.initanz).sum).getOrElse(0)})") { (caption, action) =>
                      val toSave = c.copy(initdurchgang = Some(durchgang.durchgang.name), initstart = Some(start))
                      KuTuApp.invokeWithBusyIndicator {
                        service.updateOrinsertRiege(toSave.commit)
                        reloadData()
                      }
                    }
                  }
              }
            }
        }
      }
    }
    var warnIcon: Image = null
    try {
      warnIcon = new Image(getClass.getResourceAsStream("/images/OrangeWarning.png"))
    } catch {
      case e: Exception => e.printStackTrace()
    }


    /*
    warnPanelVisible <== when(Bindings.createBooleanBinding(() => {
      warnings
    },
      durchgangModel,
      incompleteAssignments
    )) choose true otherwise false
*/
    val warnPanel = new BorderPane {
      hgrow = Priority.Always
      id = "warnpanel"
      top = new Label {
        text = "Nicht eingeteilte Riegen"
        graphic = new ImageView(warnIcon)
      }
      bottom = unassignedRiegenView
    }

    val durchgangView = new DurchgangView(
        wettkampf, service,
        () => {disziplinlist},
        durchgangModel)

    val riegenFilterTab = new Tab {
      text = "Riegen"
      content = riegenFilterView
      closable = false
      onSelectionChanged = _ => {
        if(selected.value && wettkampfEditable) {
          reloadData()
        }
      }
    }


    val durchgangTab = new Tab {
      text = "Durchgänge"
      content = new BorderPane {
        hgrow = Priority.Always
        vgrow = Priority.Always
        private def adjustWarnPanel(): Unit = {
          if (wettkampfEditable && warnings) {
            top = warnPanel
          } else {
            top = null
          }
        }
        warnPanelVisible.onChange {
          adjustWarnPanel()
        }
        adjustWarnPanel()
        center = durchgangView
      }
      closable = false
      onSelectionChanged = _ => {
        if(selected.value && wettkampfEditable) {
          reloadData()
        }
      }
    }
    val zeitenTab = new WettkampfZeitenTab(wettkampfEditable, wettkampf, service) {
      closable = false
      this.isPopulated
    }
    def makeRiegenFilterActiveBinding = {
      Bindings.createBooleanBinding(() => {
        ! (riegenFilterView.selectionModel.value.selectedItem.isNotNull.value && riegenFilterTab.selectedProperty.value)
      },
        riegenFilterView.selectionModel.value.getSelectedItems(),
        riegenFilterTab.selectedProperty
      )
    }
    def makeDurchgangActiveBinding = {
      Bindings.createBooleanBinding(() => {
        ! (durchgangView.selectionModel.value.getSelectedCells.nonEmpty && durchgangTab.selectedProperty.value)
      },
        durchgangView.selectionModel.value.getSelectedItems(),
        durchgangTab.selectedProperty
      )
    }

    def doRegenerateDurchgang(durchgang: Set[String])(implicit action: ActionEvent): Unit = {
      val allDurchgaenge = durchgangModel.flatMap(group => {
        if (group.children.isEmpty) {
          ObservableBuffer[jfxsc.TreeItem[DurchgangEditor]](group)
        } else {
          group.children
        }
      })
      val inistartriegen = allDurchgaenge.filter(dg => durchgang.contains(dg.getValue.durchgang.name)).map(_.getValue.initstartriegen)
      val startgeraete = inistartriegen.flatMap(_.keySet).distinct
      val cbSplitSex = new ComboBox[SexDivideRule]() {
          items.get.addAll(null, GemischteRiegen, GemischterDurchgang, GetrennteDurchgaenge)
          if(durchgang.nonEmpty) {
            selectionModel.value.selectFirst()
          }
          promptText = "automatisch"
        }
      val chkSplitPgm = new CheckBox() {
          text = "Programme / Kategorien teilen"
          selected = durchgang.size != 1
        }
      val lvOnDisziplines = new ListView[CheckListBoxEditor[Disziplin]] {
          prefHeight = 250
          disziplinlist.foreach { d =>
            val cde = CheckListBoxEditor[Disziplin](d)
            if (durchgang.isEmpty) {
              cde.selected.value = wettkampfInfo.startDisziplinList.contains(d)
            } else {
              cde.selected.value = startgeraete.contains(d)
            }
            items.get.add(cde)
          }
          cellFactory = CheckBoxListCell.forListView[CheckListBoxEditor[Disziplin]](_.selected)
        }
        def getSelectedDisziplines = {
          val ret = lvOnDisziplines.items.get.filter { item => item.selected.value }.map(item => item.value).toSet
          if(ret.isEmpty) {
            None
          }
          else {
            Some(ret)
          }
        }
        val titel = if (durchgang.nonEmpty)
          "Durchgänge " + durchgang.mkString("[", ", ","]") + " neu zuteilen ..."
          else
          "Riegen frisch einteilen ..."
			  PageDisplayer.showInDialog(titel, new DisplayablePage() {
 				  def getPage: Node = {
            new GridPane {
                prefWidth = 400
                hgap = 10
                vgap = 10
                add(new Label("Maximale Gruppengrösse: "), 0, 0)
                add(txtGruppengroesse, 0, 1)
                add(new Label("Geschlechter-Trennung: "), 1, 0)
                add(cbSplitSex, 1, 1)
                add(chkSplitPgm, 0, 2, 2, 1)
                add(new Label("Verteilung auf folgende Diszipline: "), 0, 3, 2, 1)
                add(lvOnDisziplines, 0, 4, 2, 2)
                if (durchgang.isEmpty) {
                  add(new Label(s"Mit der Neueinteilung der Riegen und Druchgänge werden\ndie bisherigen Einteilungen zurückgesetzt.", new ImageView {
                    image = warnIcon
                  }),
                    0, 6, 2, 1)
                }
              }
  			  }
			  }, new Button(if (durchgang.isEmpty) "OK (bestehende Einteilung wird zurückgesetzt)" else "OK (selektierte Durchgänge werden frisch eingeteilt)", new ImageView { image = warnIcon }) {
			  onAction = (event: ActionEvent) => {
				  if (txtGruppengroesse.text.value.nonEmpty) {
					  KuTuApp.invokeWithBusyIndicator {
						  val riegenzuteilungen = DurchgangBuilder(service).suggestDurchgaenge(
							  wettkampf.id,
							  str2Int(txtGruppengroesse.text.value), durchgang,
							  splitSexOption = cbSplitSex.getSelectionModel.getSelectedItem match {
                  case item: SexDivideRule => Some(item)
                  case _ => None
                },
							  splitPgm = chkSplitPgm.selected.value,
							  onDisziplinList = getSelectedDisziplines)

              if (durchgang.isEmpty) {
                service.cleanAllRiegenDurchgaenge(wettkampf.id)
              }
              for{
						    durchgang <- riegenzuteilungen.keys
							  (start, riegen) <- riegenzuteilungen(durchgang)
							  (riege, wertungen) <- riegen
						  } {
							  service.insertRiegenWertungen(RiegeRaw(
							    wettkampfId = wettkampf.id,
							    r = riege,
							    durchgang = Some(durchgang),
							    start = Some(start.id),
                  kind = if (wertungen.nonEmpty) RiegeRaw.KIND_STANDARD else RiegeRaw.KIND_EMPTY_RIEGE
                ), wertungen)
						  }
              service.updateDurchgaenge(wettkampf.id)
						  reloadData()
              riegenFilterView.sort()
              durchgangView.sort()
					  }
				  }
			  }
		  })
    }

    def makeRegenereateDurchgangMenu(durchgang: Set[String]): MenuItem = {
      val m = KuTuApp.makeMenuAction("Durchgang neu einteilen ...") {(caption: String, action: ActionEvent) =>
        doRegenerateDurchgang(durchgang)(action)
      }
      m.disable <== when(makeDurchgangActiveBinding) choose true otherwise false
      m
    }

    def makeMergeDurchganMenu(durchgang: Set[String]): MenuItem = {
      val ret = KuTuApp.makeMenuAction("Durchgänge zusammenlegen ...") {(caption, action) =>
        implicit val e = action
    	  val txtNeuerDurchgangName = new TextField() {
          text = durchgang.toList.sorted.head
        }

			  PageDisplayer.showInDialog(text.value, new DisplayablePage() {
 				  def getPage: Node = {
  				  new VBox {
  					  children = Seq(new Label("Neuer Durchgangsname: "), txtNeuerDurchgangName)
  				  }
  			  }
			  }, new Button("OK") {
				  onAction = (event: ActionEvent) => {
					  if (!txtNeuerDurchgangName.text.value.isEmpty) {
						  KuTuApp.invokeWithBusyIndicator {
						    durchgang.foreach { selectedDurchgang =>
        				  service.renameDurchgang(wettkampf.id, selectedDurchgang, txtNeuerDurchgangName.text.value)
        				  reloadData()
                  riegenFilterView.sort()
                  durchgangView.sort()
  						  }
						  }
					  }
				  }
			  })
      }
      ret.setDisable(durchgang.size < 2)
      ret
    }

    def makeAllDurchgangTeilnehmerExport() = {
      val allAction = KuTuApp.makeMenuAction("Durchgang-Teilnehmerliste aus allen Durchgängen drucken") { (caption, action) =>
        val allDurchgaenge = durchgangModel.flatMap(group => {
          if (group.children.isEmpty) {
            ObservableBuffer[jfxsc.TreeItem[DurchgangEditor]](group)
          } else {
            group.children
          }
        })
        val selectedDurchgaenge = allDurchgaenge.map(_.getValue.durchgang)
          .map(_.name).toSet
        doSelectedTeilnehmerExport(text.value, selectedDurchgaenge)(action)
      }
      allAction
    }
    def makeSelectedDurchgangTeilnehmerExport(durchgang: Set[String]): Menu = {
      val ret = new Menu() {
        text = "Durchgang-Teilnehmerliste drucken"
        val selectedAction = KuTuApp.makeMenuAction("Durchgang-Teilnehmerliste aus selektion drucken") { (caption, action) =>
          val allDurchgaenge = durchgangModel.flatMap(group => {
            if (group.children.isEmpty) {
              ObservableBuffer[jfxsc.TreeItem[DurchgangEditor]](group)
            } else {
              group.children
            }
          })
          val selectedDurchgaenge = allDurchgaenge.map(_.getValue.durchgang)
            .filter { d: Durchgang => durchgang.contains(d.name) }
            .map(_.name).toSet
          doSelectedTeilnehmerExport(text.value, selectedDurchgaenge)(action)
        }
        selectedAction.setDisable(durchgang.size < 1)
        items += selectedAction
        val allAction: MenuItem = makeAllDurchgangTeilnehmerExport()
        items += allAction
      }
      ret
    }

    def makeAggregateDurchganMenu(durchgang: Set[String]): MenuItem = {
      val ret = KuTuApp.makeMenuAction("Durchgänge in Gruppe zusammenfassen ...") {(caption, action) =>
        implicit val e = action
        val allDurchgaenge = durchgangModel.flatMap(group => {
          if (group.children.isEmpty) {
            ObservableBuffer[jfxsc.TreeItem[DurchgangEditor]](group)
          } else {
            group.children
          }
        })
        val selectedDurchgaenge = allDurchgaenge.map(_.getValue.durchgang).filter {case d: Durchgang => durchgang.contains(d.name)}
        val txtNeuerDurchgangName = new TextField() {
          text = selectedDurchgaenge.head.title
        }

        PageDisplayer.showInDialog(text.value, new DisplayablePage() {
          def getPage: Node = {
            new VBox {
              children = Seq(new Label("Name der Durchgang-Gruppe: "), txtNeuerDurchgangName)
            }
          }
        }, new Button("OK") {
          onAction = (event: ActionEvent) => {
            if (!txtNeuerDurchgangName.text.value.isEmpty) {
              KuTuApp.invokeWithBusyIndicator {
                val toStore = allDurchgaenge.map(_.getValue.durchgang)
                    .map {case d: Durchgang => if (durchgang.contains(d.name)) d.copy(title = txtNeuerDurchgangName.text.value) else d}
                service.updateOrInsertDurchgaenge(toStore)
                reloadData()
                riegenFilterView.sort()
                durchgangView.sort()
              }
            }
          }
        })
      }
      ret.setDisable(durchgang.size < 1)
      ret
    }

    def makeSelectedRiegenBlaetterExport(selectedDurchgaenge: Set[String]): Menu = {
      new Menu {
        text = "Riegenblätter nachdrucken"
        updateItems
        reprintItems.onChange {
          updateItems
        }

        private def updateItems = {
          items.clear()
          val affectedDurchgaenge: Set[String] = reprintItems.get.map(_.durchgang)
          if (selectedDurchgaenge.nonEmpty) {
            items += KuTuApp.makeMenuAction(s"Alle selektierten") { (caption: String, action: ActionEvent) =>
              doSelectedRiegenBelatterExport(text.value, selectedDurchgaenge)(action)
            }
            items += KuTuApp.makeMenuAction(s"Alle selektierten, nur 1. Gerät") { (caption: String, action: ActionEvent) =>
              doSelectedRiegenBelatterExport(text.value, selectedDurchgaenge, Set(0))(action)
            }
            items += KuTuApp.makeMenuAction(s"Alle selektierten, ab 2. Gerät") { (caption: String, action: ActionEvent) =>
              doSelectedRiegenBelatterExport(text.value, selectedDurchgaenge, Set(-1))(action)
            }
          }
          if (affectedDurchgaenge.nonEmpty && selectedDurchgaenge.nonEmpty) {
            items += new SeparatorMenuItem()
          }
          if (affectedDurchgaenge.nonEmpty) {
            val allItem = KuTuApp.makeMenuAction(s"Alle betroffenen (${affectedDurchgaenge.size})") { (caption: String, action: ActionEvent) =>
              doSelectedRiegenBelatterExport(text.value, affectedDurchgaenge)(action)
            }
            items += allItem
            items += new SeparatorMenuItem()
            affectedDurchgaenge.toList.sorted.foreach { durchgang =>
              items += KuTuApp.makeMenuAction(s"${durchgang}") { (caption: String, action: ActionEvent) =>
                doSelectedRiegenBelatterExport(text.value, Set(durchgang))(action)
              }
            }
          }
          disable.value = items.isEmpty
        }
      }
    }

    def toGeraetId(tcpl: List[Int]): List[Long] = {
      def tgi(tcp: Int): Int = {
        (tcp - 5) / 2
      }
      tcpl map tgi filter { _ > -1 } map { disziplinlist(_).id }
    }
    def toGeraetName(id: Long) = disziplinlist.find(p => p.id == id).map(_.name).getOrElse("")

    def makeMoveDurchganMenu(durchgang: DurchgangEditor, cells: List[jfxsc.TreeTablePosition[DurchgangEditor, _]]): Menu = {
      val selectedGerate = toGeraetId(cells.map(c => c.getColumn))
      new Menu("In anderen Durchgang verschieben") {
        durchgang.initstartriegen
        .filter(d => selectedGerate.isEmpty || selectedGerate.contains(d._1.id))
        .flatMap(_._2)
        .filter(r => r.kind != RiegeRaw.KIND_EMPTY_RIEGE)
        .toList.sortBy(r => r.initanz)
        .foreach{riege =>
          val riegenAmStart: Seq[RiegeEditor] = riege.initstart.map(durchgang.initstartriegen(_)).getOrElse(List.empty)
          items += new Menu(riege.initname + " ("+riege.initanz+")") {
            durchgangModel
              .flatMap(d => d.getValue match {
                case gd: GroupDurchgangEditor =>
                  gd.aggregates
                case ce: CompetitionDurchgangEditor =>
                  List(ce)
              })
              .filter(d => !d.equals(durchgang)).foreach{durchgang =>
        	    items += KuTuApp.makeMenuAction(durchgang.durchgang.name) {(caption, action) =>
        	      val toSave = riege.copy(initdurchgang = Some(durchgang.durchgang.name))
  						  KuTuApp.invokeWithBusyIndicator {
                  if (riegenAmStart.size == 1) {
                    service.updateOrinsertRiege(RiegeRaw(wettkampf.id,
                      s"Leere Riege ${durchgang.durchgang.name}/${toGeraetName(riege.start.value.id)}",
                      Some(durchgang.durchgang.name), Some(riege.start.value.id), RiegeRaw.KIND_EMPTY_RIEGE
                    ))
                  }
        				  service.updateOrinsertRiege(toSave.commit)
        				  reloadData()
                  riegenFilterView.sort()
                  durchgangView.sort()
  						  }
        	    }
            }
          }.asInstanceOf[MenuItem]
        }
        disable.value = items.size() == 0
      }
    }

    def makeMoveStartgeraetMenu(durchgang: DurchgangEditor, cells: List[jfxsc.TreeTablePosition[DurchgangEditor, _]]): Menu = {
      val selectedGerate = toGeraetId(cells.map(c => c.getColumn))
      new Menu("Auf anderes Startgerät verschieben") {
        durchgang.initstartriegen
          .filter(d => selectedGerate.isEmpty || selectedGerate.contains(d._1.id))
          .flatMap(_._2)
          .filter(r => r.kind != RiegeRaw.KIND_EMPTY_RIEGE)
          .toList.sortBy(r => r.initanz).foreach { riege =>
          val riegenAmStart: Seq[RiegeEditor] = riege.initstart.map(durchgang.initstartriegen(_)).getOrElse(List.empty)
          val von = "Von " + riege.initstart.map(d => {
            d.name + " (" + riegenAmStart.map(re => re.initanz).sum + ")"
          }).getOrElse("?") + " auf "
          items += new Menu(riege.initname + " (" + riege.initanz + ")") {
            disziplinlist
              .filter(d => riege.initstart match {
                case Some(dd) if (dd.equals(d)) => false
                case _ => true
              })
              .foreach { start =>
                items += KuTuApp.makeMenuAction(von + start.name + " (" + durchgang.initstartriegen.get(start).map(r => r.map(re => re.initanz).sum).getOrElse(0) + ")") { (caption, action) =>
                  val toSave = riege.copy(initstart = Some(start))
                  KuTuApp.invokeWithBusyIndicator {
                    if (riegenAmStart.size == 1) {
                      service.updateOrinsertRiege(RiegeRaw(wettkampf.id,
                        s"Leere Riege ${durchgang.durchgang.name}/${toGeraetName(riege.start.value.id)}",
                        Some(durchgang.durchgang.name), Some(riege.start.value.id), RiegeRaw.KIND_EMPTY_RIEGE
                      ))
                    }
                    service.updateOrinsertRiege(toSave.commit)
                    reloadData()
                    riegenFilterView.sort()
                    durchgangView.sort()
                  }
                }
              }
          }.asInstanceOf[MenuItem]
        }
        disable.value = items.size() == 0
      }
    }
    def makeSetEmptyRiegeMenu(durchgang: DurchgangEditor, cells: List[jfxsc.TreeTablePosition[DurchgangEditor, _]]): MenuItem = {
      val assignedGeraete = durchgang.initstartriegen
        .filter(_._2.forall(_.kind != RiegeRaw.KIND_EMPTY_RIEGE))
        .map(_._1.id).toSet
      val selectedCellsGeraete = toGeraetId(cells.map(c => c.getColumn))
      val selectedGerate: List[Long] = if (selectedCellsGeraete.nonEmpty)
        selectedCellsGeraete
      else
        wettkampfInfo.disziplinList.map(_.id).filter(!assignedGeraete.contains(_))

      val emptyRiegen = durchgang.initstartriegen
        .filter(d => selectedGerate.isEmpty || selectedGerate.contains(d._1.id))
        .flatMap(_._2).toList
        .filter(r => r.kind == RiegeRaw.KIND_EMPTY_RIEGE)
        .map(r => r.initname)
      val menu = KuTuApp.makeMenuAction(if (selectedGerate.size == 1) "Mit leerer Riege besetzen" else "Mit leeren Riegen besetzen") {(caption, action) =>
        KuTuApp.invokeWithBusyIndicator {
          selectedGerate.foreach(selectedGeraet => {
            service.updateOrinsertRiege(RiegeRaw(wettkampf.id,
              s"Leere Riege ${durchgang.durchgang.name}/${toGeraetName(selectedGeraet)}",
              Some(durchgang.durchgang.name), Some(selectedGeraet), RiegeRaw.KIND_EMPTY_RIEGE
            ))
          })
          reloadData()
          riegenFilterView.sort()
          durchgangView.sort()
        }
      }
      menu.disable.value = emptyRiegen.nonEmpty || selectedGerate.isEmpty
      menu
    }
    def makeRemoveEmptyRiegeMenu(durchgang: DurchgangEditor, cells: List[jfxsc.TreeTablePosition[DurchgangEditor, _]]): MenuItem = {
      val selectedGerate = toGeraetId(cells.map(c => c.getColumn))
      val emptyRiegen = durchgang.initstartriegen
        .filter(d => selectedGerate.isEmpty || selectedGerate.contains(d._1.id))
        .flatMap(_._2).toList
        .filter(r => r.kind == RiegeRaw.KIND_EMPTY_RIEGE)
        .map(r => r.initname)
      val menu = KuTuApp.makeMenuAction(if (emptyRiegen.size == 1) "Leere Riege entfernen" else "Leere Riegen entfernen") {(caption, action) =>
        KuTuApp.invokeWithBusyIndicator {
          emptyRiegen.foreach(service.deleteRiege(wettkampf.id, _))
          reloadData()
          riegenFilterView.sort()
          durchgangView.sort()
        }
      }
      menu.disable.value = emptyRiegen.isEmpty
      menu
    }
    def makeRenameDurchgangMenu: MenuItem = {
      val m = KuTuApp.makeMenuAction("Durchgang umbenennen ...") {(caption, action) =>
      			  implicit val impevent = action
			  val selectedDurchgang = durchgangView.selectionModel.value.getSelectedCells.head.getTreeItem.getValue.durchgang.name
			  val txtDurchgangName = new TextField {
    		  text.value = selectedDurchgang
    	  }
    	  PageDisplayer.showInDialog(text.value, new DisplayablePage() {
    		  def getPage: Node = {
      		  new HBox {
      			  prefHeight = 50
      			  alignment = Pos.BottomRight
      			  hgrow = Priority.Always
      			  children = Seq(new Label("Neue Durchgangs-Bezeichung  "), txtDurchgangName)
      		  }
      	  }
      	  }, new Button("OK") {
      		  onAction = (event: ActionEvent) => {
      			  KuTuApp.invokeWithBusyIndicator {
      				  service.renameDurchgang(wettkampf.id, selectedDurchgang, txtDurchgangName.text.value)
      				  reloadData()
                riegenFilterView.sort()
                durchgangView.sort()
      			  }
      		  }
      	  }
      	)
      }
      m.disable = durchgangView.selectionModel.value.getSelectedItems.size() != 1
      m
    }
    def makeStartOffsetDurchgangMenu: MenuItem = {
      val m = KuTuApp.makeMenuAction("Durchgang Start Zeitpunkt ...") {(caption, action) =>
      			  implicit val impevent = action
			  val selectedDurchgang: String = durchgangView.selectionModel.value.getSelectedCells.head.getTreeItem.getValue.durchgang.title
			  val selectedStartTime: String = s"${durchgangView.selectionModel.value.getSelectedCells.head.getTreeItem.getValue.durchgang.effectivePlanStart(wettkampf.datum.toLocalDate)}"
			  val txtDurchgangStartTime = new TextField {
    		  text.value = selectedStartTime
    	  }
    	  PageDisplayer.showInDialog(text.value, new DisplayablePage() {
    		  def getPage: Node = {
      		  new HBox {
      			  prefHeight = 50
      			  alignment = Pos.BottomRight
      			  hgrow = Priority.Always
      			  children = Seq(new Label("Durchgang Start Zeitpunkt (jjjj-mm-ttThh:mm:ss) "), txtDurchgangStartTime)
      		  }
      	  }
      	  }, new Button("OK") {
      		  onAction = (event: ActionEvent) => {
      			  KuTuApp.invokeWithBusyIndicator {
                val startTime = LocalDateTime.parse(txtDurchgangStartTime.text.value)
                val wkstart = LocalDateTime.of(wettkampf.datum.toLocalDate, LocalTime.MIDNIGHT)
                val dur: Duration = Duration.between(wkstart, startTime)
                service.updateStartOffset(wettkampf.id, selectedDurchgang, dur.toMillis)
      				  reloadData()
                riegenFilterView.sort()
                durchgangView.sort()
      			  }
      		  }
      	  }
      	)
      }
      m.disable = durchgangView.selectionModel.value.getSelectedItems.size() != 1
      m
    }

    val btnEditDurchgang = new MenuButton("Durchgang ...") {
      disable <== when(makeDurchgangActiveBinding) choose true otherwise false
    }

    durchgangView.selectionModel.value.setSelectionMode(SelectionMode.Multiple)
    durchgangView.selectionModel.value.setCellSelectionEnabled(true)
    if (!wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)) {
      durchgangView.setOnDragDetected((event) => {
        val focusedCells = durchgangView.selectionModel.value.getSelectedCells.toList
        val selectedGerate = toGeraetId(focusedCells.map(c => c.getColumn))
        val actDurchgangSelection = focusedCells.map(c => c.getTreeItem.getValue).toSet.filter(_ != null)
        if (actDurchgangSelection.size == 1 && selectedGerate.size == 1) {
          val startgeraet = selectedGerate.head
          val durchgangEditor = actDurchgangSelection.head
          val riegenEditorCandidates = durchgangEditor.initstartriegen
            .filter(d => selectedGerate.isEmpty || selectedGerate.contains(d._1.id))
            .flatMap(_._2).toList
            .filter(r => r.kind != RiegeRaw.KIND_EMPTY_RIEGE)
            .sortBy(r => r.initname)
            .map(r => r.initname)
          if (riegenEditorCandidates.nonEmpty && event.pickResult.getIntersectedNode.isInstanceOf[Text]) {
            val text = event.pickResult.getIntersectedNode.asInstanceOf[Text]
            val vb = DurchgangView.positionInScene(text.parent.value)
            val pointInText = new Point2D(event.getSceneX - vb.x, event.getSceneY - vb.y)
            val spacePerRiege = (text.boundsInLocal.value.getHeight) / riegenEditorCandidates.size
            val hoveredTextIndex = (Math.abs(Math.max(1, pointInText.y)) / spacePerRiege).toInt
            val hoveredText = riegenEditorCandidates(Math.min(riegenEditorCandidates.size - 1, hoveredTextIndex))

            val snp = text.parent.value.snapshot(null, null)

            val width = snp.getWidth.toInt
            val height: Int = Math.min(snp.getHeight.toInt - 4 + (hoveredTextIndex * spacePerRiege).toInt, spacePerRiege.toInt)
            val croppedImage = new WritableImage(snp.getPixelReader(),
              0, 5 + (hoveredTextIndex * spacePerRiege).toInt + 1,
              width, height)

            val db = durchgangView.startDragAndDrop(TransferMode.Move)
            db.setDragView(croppedImage)
            val content = new ClipboardContent()
            content.put(DurchgangView.DRAG_RIEGE, (durchgangEditor.durchgang.name, hoveredText, startgeraet))
            content.putString(hoveredText)
            db.setContent(content)

            event.consume()
          }
        }
      })
      durchgangView.setOnDragDropped((event) => {
        val db = event.getDragboard
        // If this is a meaningful drop...
        if (db.hasContent(DurchgangView.DRAG_RIEGE)) {
          val (selecteddurchgang, selectedriege, selectedGeraet) = db.getContent(DurchgangView.DRAG_RIEGE).asInstanceOf[(String, String, Long)]
          val fromDisziplin = disziplinlist.find(_.id == selectedGeraet)
          riegenFilterModel.find{p => p.initname.equals(selectedriege)} match {
            case Some(riege) =>
              val dgOpt = durchgangView.root.value.getChildren.toList.flatMap(d => d +: d.getChildren).map(_.getValue).find(dge => dge.durchgang.name == selecteddurchgang)
              val riegenAmStart: Seq[RiegeEditor] = dgOpt.flatMap(dg => riege.initstart.flatMap(d => dg.initstartriegen.get(d))).getOrElse(List.empty)
              @tailrec
              def findTableCell(node: Object): Option[jfxsc.TreeTableCell[DurchgangEditor, _]] =
                node match {
                  case _: jfxsc.TreeTableCell[_, _] =>
                    Some(node.asInstanceOf[jfxsc.TreeTableCell[DurchgangEditor, _]])
                  case t : Text =>
                    findTableCell(t.getParent)
                  case _ =>
                    None
                }

              findTableCell(event.getPickResult.getIntersectedNode) match {
                case Some(selectedCell) if (!selectedCell.getTableRow.getItem.isHeader) =>
                  val durchgang = selectedCell.getTableRow
                  val startGeraetColumn = selectedCell.getTableColumn
                  startGeraetColumn match {
                    case access: DurchgangTCAccess =>
                      val targetStartgeraet = access.getDisziplin
                      if (!fromDisziplin.contains(targetStartgeraet) || !dgOpt.contains(durchgang.getItem)) {
                        val targetDurchgang = durchgang.getItem.durchgang.name
                        val toSave = riege.copy(initstart = Some(targetStartgeraet), initdurchgang = Some(targetDurchgang))
                        KuTuApp.invokeWithBusyIndicator {
                          if (riegenAmStart.size == 1) {
                            service.updateOrinsertRiege(RiegeRaw(wettkampf.id,
                              s"Leere Riege ${dgOpt.map(_.durchgang.name).getOrElse("")}/${toGeraetName(riege.start.value.id)}",
                              dgOpt.map(_.durchgang.name), Some(riege.start.value.id), RiegeRaw.KIND_EMPTY_RIEGE
                            ))
                          }
                          service.updateOrinsertRiege(toSave.commit)
                          reloadData()
                          riegenFilterView.sort()
                          durchgangView.sort()
                        }
                      }
                    case _ =>
                  }

                case _ =>
                  PageDisplayer.showErrorDialog("Drag & Drop", "Die Riege kann hier nicht zugewiesen werden.")
              }

            case None =>
              PageDisplayer.showErrorDialog("Drag & Drop", "Die Riege kann hier nicht zugewiesen werden.")
          }
        }
        event.setDropCompleted(true)
        event.consume()
      })
      durchgangView.setOnDragOver((event) => {
        if (event.getDragboard.hasContent(DurchgangView.DRAG_RIEGE)) {
          event.acceptTransferModes(TransferMode.Move)
        }
        event.consume()
      })

      durchgangView.getSelectionModel.getSelectedCells.onChange { (_, _) =>
        Platform.runLater {
          val focusedCells: List[jfxsc.TreeTablePosition[DurchgangEditor, _]] = durchgangView.selectionModel.value.getSelectedCells.toList
          val selectedDurchgaenge = focusedCells.filter(c => c.getTreeItem != null).flatMap(c => c.getTreeItem.getValue.isHeader match {
            case true =>
              c.getTreeItem.getChildren
            case false => List(c.getTreeItem)
          }).map(c => c.getValue).toSet
          val selectedDurchgangHeader = focusedCells
            .filter(c => c.getTreeItem.getValue match {
              case gd: GroupDurchgangEditor if (gd.aggregates.size > 1) => true
              case _ => false
            })
            .map(c => c.getTreeItem.getValue).toSet
          val actDurchgangSelection = selectedDurchgaenge.filter(_ != null).map(d => d.durchgang.name)
          val selectedEditor = if (focusedCells.nonEmpty) focusedCells.head.getTreeItem.getValue else null
          durchgangView.contextMenu = new ContextMenu() {
            items += makeRegenereateDurchgangMenu(actDurchgangSelection)
            items += makeMergeDurchganMenu(actDurchgangSelection)
            items += makeRenameDurchgangMenu
            items += makeStartOffsetDurchgangMenu
            if (selectedDurchgangHeader.isEmpty) {
              items += makeAggregateDurchganMenu(actDurchgangSelection)
            }
            if (focusedCells.size == 1 && selectedEditor != null && selectedDurchgangHeader.isEmpty) {
              items += new SeparatorMenuItem()
              items += makeSetEmptyRiegeMenu(selectedEditor, focusedCells)
              items += makeRemoveEmptyRiegeMenu(selectedEditor, focusedCells)
            }
            if (focusedCells.size == 1 && selectedEditor != null) {
              items += new SeparatorMenuItem()
              items += makeMoveDurchganMenu(selectedEditor, focusedCells)
              items += makeMoveStartgeraetMenu(selectedEditor, focusedCells)
            }
            items += new SeparatorMenuItem()
            items += makeSelectedDurchgangTeilnehmerExport(actDurchgangSelection)
            items += makeSelectedRiegenBlaetterExport(actDurchgangSelection)
          }

          btnEditDurchgang.text.value = "Durchgang " + actDurchgangSelection.mkString("[", ", ", "]") + " bearbeiten"
          btnEditDurchgang.items.clear
          btnEditDurchgang.items += makeRegenereateDurchgangMenu(actDurchgangSelection)
          btnEditDurchgang.items += makeMergeDurchganMenu(actDurchgangSelection)
          btnEditDurchgang.items += makeRenameDurchgangMenu
          btnEditDurchgang.items += makeStartOffsetDurchgangMenu
          if (selectedDurchgangHeader.isEmpty) {
            btnEditDurchgang.items += makeAggregateDurchganMenu(actDurchgangSelection)
          }
          if (focusedCells.size == 1 && selectedEditor != null && selectedDurchgangHeader.isEmpty) {
            btnEditDurchgang.items += new SeparatorMenuItem()
            btnEditDurchgang.items += makeSetEmptyRiegeMenu(selectedEditor, focusedCells)
            btnEditDurchgang.items += makeRemoveEmptyRiegeMenu(selectedEditor, focusedCells)
          }
          if (focusedCells.size == 1 && selectedEditor != null) {
            btnEditDurchgang.items += new SeparatorMenuItem()
            btnEditDurchgang.items += makeMoveDurchganMenu(selectedEditor, focusedCells)
            btnEditDurchgang.items += makeMoveStartgeraetMenu(selectedEditor, focusedCells)
          }
          btnEditDurchgang.items += new SeparatorMenuItem()
          btnEditDurchgang.items += makeSelectedDurchgangTeilnehmerExport(actDurchgangSelection)
          btnEditDurchgang.items += makeSelectedRiegenBlaetterExport(actDurchgangSelection)
        }
      }
    }

    def makeRiegenSuggestMenu(): MenuItem = {
      val m = KuTuApp.makeMenuAction("Riegen & Durchgänge frisch einteilen ...") {(caption: String, action: ActionEvent) =>
        doRegenerateDurchgang(Set.empty)(action)
      }
      m.graphic = new ImageView {
        image = warnIcon
    	}
      m
    }

    def makeRiegenResetMenu(): MenuItem = {
      val m = KuTuApp.makeMenuAction("Einteilung von Riegen & Durchgängen zurücksetzen ...") {(caption: String, action: ActionEvent) =>
        doRiegenReset(action)
      }
      m.graphic = new ImageView {
        image = warnIcon
    	}
      m
    }

    def doRiegenReset(event: ActionEvent): Unit = {
		  implicit val impevent = event
      PageDisplayer.showInDialog("Riegen- und Durchgangseinteilung zurücksetzen ...", new DisplayablePage() {
        def getPage: Node = {
          new HBox {
            prefHeight = 50
            alignment = Pos.BottomRight
            hgrow = Priority.Always
            children = Seq(
                new ImageView {
                  image = warnIcon
              	},
                new Label(
                    s"Es werden die bisherigen Einteilungen in Riegen und Durchgänge zurückgesetzt."))
          }
        }
      }, new Button("OK") {
        onAction = (event: ActionEvent) => {
          implicit val impevent = event
				  KuTuApp.invokeWithBusyIndicator {
						service.cleanAllRiegenDurchgaenge(wettkampf.id)
					  reloadData()
            riegenFilterView.sort()
            durchgangView.sort()
				  }
        }
      })
    }

    def doDurchgangExport(event: ActionEvent): Unit = {
		  implicit val impevent = event
		  KuTuApp.invokeWithBusyIndicator {
			  val filename = "Durchgaenge.csv"
					  val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
			  if(!dir.exists()) {
				  dir.mkdirs();
			  }
			  val file = new java.io.File(dir.getPath + "/" + filename)

			  ResourceExchanger.exportDurchgaenge(wettkampf.toWettkampf, file.getPath)
        //hostServices.showDocument(file.toURI.toASCIIString)
        //if (!file.toURI.toASCIIString.equals(file.toURI.toString)) {
          hostServices.showDocument(file.toURI.toString)
        //}
      }
    }

    def makeDurchgangExport(): MenuItem = {
      val m = KuTuApp.makeMenuAction("Durchgang-Planung export") {(caption: String, action: ActionEvent) =>
        doDurchgangExport(action)
      }
      m
    }
    def doDurchgangExport2(event: ActionEvent): Unit = {
      implicit val impevent = event
      KuTuApp.invokeWithBusyIndicator {
        val filename = "Durchgaenge-Einfach.csv"
        val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
        if(!dir.exists()) {
          dir.mkdirs();
        }
        val file = new java.io.File(dir.getPath + "/" + filename)

        ResourceExchanger.exportSimpleDurchgaenge(wettkampf.toWettkampf, file.getPath)
        hostServices.showDocument(file.toURI.toString)
      }
    }

    def makeDurchgangExport2(): MenuItem = {
      val m = KuTuApp.makeMenuAction("Durchgang-Planung export (einfach)") {(caption: String, action: ActionEvent) =>
        doDurchgangExport2(action)
      }
      m
    }
    val riegenRemoveButton = new Button {
  	  text = "Riege löschen"
		  minWidth = 75
		  disable <== when(makeRiegenFilterActiveBinding) choose true otherwise false
		  onAction = (event: ActionEvent) => {
		    val selectedRiege = riegenFilterView.selectionModel.value.getSelectedItem.name.value
          implicit val impevent = event
          PageDisplayer.showInDialog(text.value, new DisplayablePage() {
            def getPage: Node = {
              new HBox {
                prefHeight = 50
                alignment = Pos.BottomRight
                hgrow = Priority.Always
                children = Seq(
                    new Label(
                        s"Soll die Riege '${selectedRiege}' gelöscht werden?\nDamit werden Turner/-Innen die Riegenzuteilung zur gelöschten Riege verlieren."))
              }
            }
          }, new Button("OK") {
            onAction = (event: ActionEvent) => {
      			  KuTuApp.invokeWithBusyIndicator {
      			    service.deleteRiege(wettkampf.id, selectedRiege)
      				  reloadData()
                riegenFilterView.sort()
                durchgangView.sort()
      			  }
            }
          })
		  }
    }

    val riegeRenameButton = new Button {
  	  text = "Riege umbenennen"
		  minWidth = 75
		  disable <== when(makeRiegenFilterActiveBinding) choose true otherwise false
		  onAction = (event: ActionEvent) => {
			  implicit val impevent = event
			  val selectedRiege = riegenFilterView.selectionModel.value.getSelectedItem.name.value
			  val txtRiegenName = new TextField {
    		  text.value = selectedRiege
    	  }
    	  PageDisplayer.showInDialog(text.value, new DisplayablePage() {
    		  def getPage: Node = {
      		  new HBox {
      			  prefHeight = 50
      			  alignment = Pos.BottomRight
      			  hgrow = Priority.Always
      			  children = Seq(new Label("Neuer Riegenname  "), txtRiegenName)
      		  }
      	  }
      	  }, new Button("OK") {
      		  onAction = (event: ActionEvent) => {
      			  KuTuApp.invokeWithBusyIndicator {
      				  service.renameRiege(wettkampf.id, selectedRiege, txtRiegenName.text.value)
      				  reloadData()
                riegenFilterView.sort()
                durchgangView.sort()
      			  }
      		  }
      	  }
      	)
		  }
    }
    val durchgangRenameButton = new Button {
  	  text = "Durchgang umbenennen"
		  minWidth = 75
		  disable <== when(makeDurchgangActiveBinding) choose true otherwise false
		  onAction = (event: ActionEvent) => {
			  implicit val impevent = event
			  val selectedDurchgang = durchgangView.selectionModel.value.getSelectedItem.getValue.durchgang.name
			  val txtDurchgangName = new TextField {
    		  text.value = selectedDurchgang
    	  }
    	  PageDisplayer.showInDialog(text.value, new DisplayablePage() {
    		  def getPage: Node = {
      		  new HBox {
      			  prefHeight = 50
      			  alignment = Pos.BottomRight
      			  hgrow = Priority.Always
      			  children = Seq(new Label("Neue Durchgangs-Bezeichung  "), txtDurchgangName)
      		  }
      	  }
      	  }, new Button("OK") {
      		  onAction = (event: ActionEvent) => {
      			  KuTuApp.invokeWithBusyIndicator {
      				  service.renameDurchgang(wettkampf.id, selectedDurchgang, txtDurchgangName.text.value)
      				  reloadData()
                riegenFilterView.sort()
                durchgangView.sort()
      			  }
      		  }
      	  }
      	)
		  }
    }

    def doRiegenBelatterExport(caption: String, event: ActionEvent): Unit = {
      import scala.concurrent.ExecutionContext.Implicits.global

      val seriendaten = service.getAllKandidatenWertungen(wettkampf.uuid.map(UUID.fromString(_)).get)
      val filename = "Riegenblatt_" + encodeFileName(wettkampf.easyprint) + ".html"
      val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
      if(!dir.exists()) {
        dir.mkdirs();
      }
      val logofile = PrintUtil.locateLogoFile(dir)
      def generate = (lpp: Int) => KuTuApp.invokeAsyncWithBusyIndicator(caption) { Future {
        (new Object with ch.seidel.kutu.renderer.RiegenblattToHtmlRenderer).toHTML(seriendaten, logofile, remoteBaseUrl)
      }}
      Platform.runLater {
        PrintUtil.printDialogFuture(text.value, FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
      }

    }

    def makeRiegenBlaetterExport(): MenuItem = {
      val m = KuTuApp.makeMenuAction("Riegenblätter erstellen") {(caption: String, action: ActionEvent) =>
        doRiegenBelatterExport(caption, action)
      }
      m
    }

    def makeRiegenQRCodesExport(): MenuItem = {
      val m = KuTuApp.makeMenuAction("QR-Codes für Wertungsrichter erstellen") {(caption: String, action: ActionEvent) =>

        val seriendaten = RiegenBuilder.mapToGeraeteRiegen(service.getAllKandidatenWertungen(wettkampf.uuid.map(UUID.fromString(_)).get).toList)
              .filter(gr => gr.durchgang.nonEmpty && gr.disziplin.nonEmpty)
              .map(WertungsrichterQRCode.toMobileConnectData(wettkampf, remoteBaseUrl))
              .toSet.toList
        val filename = "WertungsrichterConnectQRCodes_" + encodeFileName(wettkampf.easyprint) + ".html"
        val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
        if(!dir.exists()) {
          dir.mkdirs();
        }
        val logofile = PrintUtil.locateLogoFile(dir)
        def generate(lpp: Int) = (new Object with WertungsrichterQRCodesToHtmlRenderer).toHTML(seriendaten.sortBy(_.uri), logofile)
        PrintUtil.printDialog(text.value, FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(action)
      }
      m
    }

    val btnRiegen = new MenuButton("Riegen- & Durchgangs-Einteilung") {
      items += makeRiegenSuggestMenu()
      items += makeRiegenResetMenu()
    }

    val btnExport = new MenuButton("Export") {
      items += makeDurchgangExport()
      items += makeDurchgangExport2()
      items += makeAllDurchgangTeilnehmerExport()
      items += makeRiegenBlaetterExport()
      items += makeSelectedRiegenBlaetterExport(Set.empty)
      items += makeRiegenQRCodesExport()
    }

    val riegenFilterControl = new ToolBar {
      if (wettkampfEditable) {
        content = List[ButtonBase](
            btnRiegen
          , btnEditDurchgang
          , riegeRenameButton
          , riegenRemoveButton
          , durchgangRenameButton
          , btnExport
        )
      } else {
        content = List[ButtonBase](
          btnExport
        )
      }
    }

    val rootpane = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      margin = Insets(0, 0, 0, 10)
      top = riegenFilterControl
      center = new BorderPane {
        center = new TabPane {
        	tabs += durchgangTab
          tabs += riegenFilterTab
          tabs += zeitenTab
        }
      }
    }

    def sorter = () => {
      riegenFilterView.sort()
      durchgangView.sort()
    }
    riegenFilterView.addListener(onRiegeChanged(sorter))

    riegenFilterView.selectionModel.value.setCellSelectionEnabled(true)
    riegenFilterView.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(riegenFilterView, false, null)(ke)
    }
    content = rootpane

    true
  }

}
