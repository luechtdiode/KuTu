package ch.seidel.kutu.view

import java.awt.Desktop
import java.util.UUID

import javafx.scene.{ control => jfxsc }
import javafx.scene.text.Text
import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.handle
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.jfxBooleanBinding2sfx
import scalafx.Includes.jfxBounds2sfx
import scalafx.Includes.jfxCellEditEvent2sfx
import scalafx.Includes.jfxKeyEvent2sfx
import scalafx.Includes.jfxMouseEvent2sfx
import scalafx.Includes.jfxObjectProperty2sfx
import scalafx.Includes.jfxParent2sfx
import scalafx.Includes.jfxPixelReader2sfx
import scalafx.Includes.jfxReadOnlyBooleanProperty2sfx
import scalafx.Includes.jfxTableViewSelectionModel2sfx
import scalafx.Includes.jfxText2sfxText
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.Includes.when
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.StringProperty
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.event.ActionEvent
import scalafx.geometry.BoundingBox
import scalafx.geometry.Bounds
import scalafx.geometry.Insets
import scalafx.geometry.Point2D
import scalafx.geometry.Pos
import scalafx.print.PageOrientation
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.ButtonBase
import scalafx.scene.control.CheckBox
import scalafx.scene.control.ComboBox
import scalafx.scene.control.ContextMenu
import scalafx.scene.control.Label
import scalafx.scene.control.ListView
import scalafx.scene.control.Menu
import scalafx.scene.control.MenuButton
import scalafx.scene.control.MenuItem
import scalafx.scene.control.SelectionMode
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.SeparatorMenuItem
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn.CellEditEvent
import scalafx.scene.control.TableColumn.sfxTableColumn2jfx
import scalafx.scene.control.TablePosition
import scalafx.scene.control.TableView
import scalafx.scene.control.TableView.sfxTableView2jfx
import scalafx.scene.control.TextField
import scalafx.scene.control.ToolBar
import scalafx.scene.control.cell.CheckBoxListCell
import scalafx.scene.control.cell.CheckBoxTableCell
import scalafx.scene.control.cell.ComboBoxTableCell
import scalafx.scene.image.Image
import scalafx.scene.image.ImageView
import scalafx.scene.image.WritableImage
import scalafx.scene.input.ClipboardContent
import scalafx.scene.input.DataFormat
import scalafx.scene.input.KeyEvent
import scalafx.scene.input.TransferMode
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.util.StringConverter
import scalafx.util.converter.DefaultStringConverter

import ch.seidel.commons.AutoCommitTextFieldTableCell
import ch.seidel.commons.DisplayablePage
import ch.seidel.commons.PageDisplayer
import ch.seidel.commons.TabWithService
import ch.seidel.kutu.KuTuApp
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.domain.Disziplin
import ch.seidel.kutu.domain.GemischteRiegen
import ch.seidel.kutu.domain.GemischterDurchgang
import ch.seidel.kutu.domain.GetrennteDurchgaenge
import ch.seidel.kutu.domain.KutuService
import ch.seidel.kutu.domain.Riege
import ch.seidel.kutu.domain.RiegeRaw
import ch.seidel.kutu.domain.SexDivideRule
import ch.seidel.kutu.domain.WettkampfView
import ch.seidel.kutu.domain.str2Int
import ch.seidel.kutu.Config._
import ch.seidel.kutu.renderer.WertungsrichterQRCode
import ch.seidel.kutu.renderer.WertungsrichterQRCodesToHtmlRenderer
import ch.seidel.kutu.renderer.PrintUtil
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer.RiegenBuilder
import ch.seidel.kutu.squad.DurchgangBuilder

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
  override def valueEditor(selectedRow: DurchgangEditor): Seq[RiegeEditor] = selectedRow.initstartriegen(index)
}
class DurchgangTableColumn[T](val index: Disziplin) extends TableColumn[DurchgangEditor, T] with DurchgangTCAccess {
  override val delegate: jfxsc.TableColumn[DurchgangEditor, T] = new DurchgangJFSCTableColumn[T](index)
  override def getIndex: Disziplin = index
  override def valueEditor(selectedRow: DurchgangEditor): Seq[RiegeEditor] = selectedRow.initstartriegen(index)
}

class DurchgangView(wettkampf: WettkampfView, service: KutuService, disziplinlist: () => Seq[Disziplin], durchgangModel: ObservableBuffer[DurchgangEditor]) extends TableView[DurchgangEditor] {
    
  id = "durchgang-table"
  items = durchgangModel

  columns ++= Seq(
    new TableColumn[DurchgangEditor, String] {
      prefWidth = 130
      text = "Durchgang"
      cellValueFactory = { x => x.value.name }
    }
    , new TableColumn[DurchgangEditor, String] {
      prefWidth = 40
      text = "Sum"
      cellValueFactory = { x => x.value.anz.asInstanceOf[ObservableValue[String,String]]}
    }
    , new TableColumn[DurchgangEditor, String] {
      prefWidth = 40
      text = "Min"
      cellValueFactory = { x => x.value.min.asInstanceOf[ObservableValue[String,String]]}
    }
    , new TableColumn[DurchgangEditor, String] {
      prefWidth = 40
      text = "Max"
      cellValueFactory = { x => x.value.max.asInstanceOf[ObservableValue[String,String]]}
    }
    , new TableColumn[DurchgangEditor, String] {
      prefWidth = 30
      text = "ø"
      cellValueFactory = { x => x.value.avg.asInstanceOf[ObservableValue[String,String]]}
    }
  )

  columns ++= disziplinlist().map {disziplin =>
    val dc: jfxsc.TableColumn[DurchgangEditor, String] = new TableColumn[DurchgangEditor, String] {
      text = disziplin.name
      prefWidth = 230
      columns ++= Seq(
          new DurchgangTableColumn[String](disziplin) {
            text = "Riege"
            prefWidth = 190
            cellValueFactory = { x =>
              x.value.initstartriegen.get(disziplin) match {
                case Some(re) => StringProperty(re.map(rs => s"${rs.name.value} (${rs.anz.value})").mkString("\n"))
                case _ => StringProperty("")
              }
            }
          }
          , new TableColumn[DurchgangEditor, String] {
            text = "Anz"
            prefWidth = 40
            cellValueFactory = { x =>
              x.value.initstartriegen.get(disziplin) match {
                case Some(re) => StringProperty(re.map(rs => rs.anz.value).sum.toString)
                case _ => StringProperty("0")
              }
            }
          }
        )
    }
    dc
  }

}

class RiegenFilterView(isEditable: Boolean, wettkampf: WettkampfView, service: KutuService, disziplinlist: () => Seq[Disziplin], asFilter: Boolean, riegenFilterModel: ObservableBuffer[RiegeEditor]) extends TableView[RiegeEditor] {
  type RigenChangeListener = RiegeEditor => Unit
  var changeListeners = List[RigenChangeListener]()

  def addListener(listener: RigenChangeListener) {
    changeListeners = changeListeners :+ listener
  }

  def removeListener(listener: RigenChangeListener) {
    changeListeners = changeListeners filter(f => f != listener)
  }

  def fireRiegeChanged(riege: RiegeEditor) {
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
      cellFactory = { x => new CheckBoxTableCell[RiegeEditor, Boolean]() }
      editable = true
    })
  }

  columns ++= List(
    new TableColumn[RiegeEditor, String] {
      text = "Riege"
      prefWidth = 190
      cellValueFactory = { x => x.value.name }
      editable = isEditable
      if(isEditable) {
        cellFactory = { _ => new AutoCommitTextFieldTableCell[RiegeEditor, String](new DefaultStringConverter()) }
        onEditCommit = (evt: CellEditEvent[RiegeEditor, String]) => {
          val editor = evt.rowValue
          editor.name.value = evt.newValue
          val updated = RiegeEditor(
              evt.rowValue.wettkampfid,
              evt.rowValue.initanz,
              evt.rowValue.initviewanz,
              evt.rowValue.enabled,
              Riege(editor.name.value, editor.initdurchgang, editor.initstart),
              evt.rowValue.onSelectedChange)
          service.renameRiege(wettkampf.id, evt.rowValue.initname, evt.newValue)
          fireRiegeChanged(updated)
          val rowIndex = riegenFilterModel.indexOf(evt.rowValue)
          evt.tableView.selectionModel.value.select(rowIndex, this)
          evt.tableView.sort()
          evt.tableView.requestFocus()
        }
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
      if(isEditable) {
        cellValueFactory = { x => x.value.durchgang }
      }
      cellFactory = { _ => new AutoCommitTextFieldTableCell[RiegeEditor, String](new DefaultStringConverter()) }
      editable = isEditable
      if(isEditable) {
        onEditCommit = (evt: CellEditEvent[RiegeEditor, String]) => {
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
    }
    , new TableColumn[RiegeEditor, Disziplin] {
      text = "Start"
      prefWidth = 400
      val converter = new StringConverter[Disziplin] {
        override def toString(d: Disziplin) = if(d != null) d.easyprint else ""
        override def fromString(s: String) = if(s != null) disziplinlist().find { d => d.name.equals(s) }.getOrElse(null) else null
      }
      val list = ObservableBuffer[Disziplin](disziplinlist())
      if(isEditable) {
        cellValueFactory = { x => x.value.start.asInstanceOf[ObservableValue[Disziplin,Disziplin]] }
      }
      cellFactory = { _ => new ComboBoxTableCell[RiegeEditor, Disziplin](converter, list) }
      editable = isEditable
      if(isEditable) {
        onEditCommit = (evt: CellEditEvent[RiegeEditor, Disziplin]) => {
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
    }
  )
}

class RiegenTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService {
  val programmText = wettkampf.programm.id match {case 20 => "Kategorie" case _ => "Programm"}
  val riegenFilterModel = ObservableBuffer[RiegeEditor]()
  val durchgangModel = ObservableBuffer[DurchgangEditor]()
  lazy val disziplinlist = service.listDisziplinesZuWettkampf(wettkampf.id)

  closable = false
  text = "Riegeneinteilung"

  def isAthletikTest() = {
    wettkampf.programm.aggregatorHead.id == 1
  }

  def reloadRiegen() {
    riegenFilterModel.clear()
    riegen().foreach(riegenFilterModel.add(_))
  }

  def reloadDurchgaenge() {
    durchgangModel.clear()
    riegenFilterModel.groupBy(re => re.initdurchgang).toList.sortBy(_._1).map{res =>
      val (name, rel) = res
      DurchgangEditor(wettkampf.id, name.getOrElse(""), rel)
    }.foreach {durchgangModel.add(_)}
  }

  def reloadData() {
    reloadRiegen()
    reloadDurchgaenge()
//    riegenFilterView.sort()
  }

  def onNameChange(name1: String, name2: String) = {
    reloadData()
  }

  def onSelectedChange(name: String, selected: Boolean) = {
    selected
  }

  def onRiegeChanged(sorter: () => Unit)(editor: RiegeEditor) {
    reloadData()
    sorter()
//    reloadDurchgaenge()
  }

  // This handles also the initial load
  onSelectionChanged = handle {
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
    text = if(isAthletikTest) "0" else "11"
    tooltip = "Max. Gruppengrösse oder 0 für gleichmässige Verteilung mit einem Durchgang."
  }

  override def isPopulated = {

    val riegenFilterView = new RiegenFilterView(true,
        wettkampf, service,
        () => {disziplinlist},
        false,
        riegenFilterModel)

    val durchgangView = new DurchgangView(
        wettkampf, service,
        () => {disziplinlist},
        durchgangModel)

    val riegenFilterTab = new Tab {
      text = "Riegen"
      content = riegenFilterView
      closable = false
    }
    val durchgangTab = new Tab {
      text = "Durchgänge"
      content = durchgangView
      closable = false
    }
    def makeRiegenFilterActiveBinding = {
      Bindings.createBooleanBinding(() => {
        ! (riegenFilterView.selectionModel.value.selectedItem.isNotNull().value && riegenFilterTab.selectedProperty.value)
      },
        riegenFilterView.selectionModel.value.selectedItemProperty().isNull(),
        riegenFilterTab.selectedProperty
      )
    }
    def makeDurchgangActiveBinding = {
      Bindings.createBooleanBinding(() => {
        ! (durchgangView.selectionModel.value.selectedItem.isNotNull().value && durchgangTab.selectedProperty.value)
      },
        durchgangView.selectionModel.value.selectedItemProperty().isNull(),
        durchgangTab.selectedProperty
      )
    }

    def doRegenerateDurchgang(durchgang: Set[String])(implicit action: ActionEvent) = {
    	  val cbSplitSex = new ComboBox[SexDivideRule]() {
          items.get.addAll(GemischteRiegen, GemischterDurchgang, GetrennteDurchgaenge)
          selectionModel.value.selectFirst()
        }
        val chkSplitPgm = new CheckBox() {
          text = "Programme / Kategorien teilen"
          selected = true
        }
        val isGeTU = wettkampf.programm.aggregatorHead.id == 20
        val lvOnDisziplines = new ListView[CheckListBoxEditor[Disziplin]] {
          disziplinlist.foreach { d =>
            val cde = CheckListBoxEditor[Disziplin](d)
            cde.selected.value = d.id != 5 || !isGeTU
            items.get.add(cde)
          }
          cellFactory = CheckBoxListCell.forListView(_.selected)
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
			  PageDisplayer.showInDialog("Durchgänge " + durchgang.mkString("[", ", ","]") + " neu zuteilen ...", new DisplayablePage() {
 				  def getPage: Node = {
  				  new GridPane {
  				    prefWidth = 400
  				    hgap = 10
              vgap = 10
              add(new Label("Maximale Gruppengrösse: "), 0, 0)
  				    add(txtGruppengroesse, 0,1)
  				    add(new Label("Geschlechter-Trennung: "), 1, 0)
  				    add(cbSplitSex, 1, 1)
  				    add(chkSplitPgm, 0, 2, 2, 1)
  				    add(new Label("Verteilung auf folgende Diszipline: "), 0, 3, 2, 1)
  				    add(lvOnDisziplines, 0, 4, 2, 2)
  				  }
  			  }
			  }, new Button("OK") {
			  onAction = (event: ActionEvent) => {
				  if (!txtGruppengroesse.text.value.isEmpty) {
					  KuTuApp.invokeWithBusyIndicator {
						  val riegenzuteilungen = DurchgangBuilder(service).suggestDurchgaenge(
							  wettkampf.id,
							  str2Int(txtGruppengroesse.text.value), durchgang,
							  splitSex = cbSplitSex.getSelectionModel.getSelectedItem,
							  splitPgm = chkSplitPgm.selected.value,
							  onDisziplinList = getSelectedDisziplines)

						  for{
						    durchgang <- riegenzuteilungen.keys
							  (start, riegen) <- riegenzuteilungen(durchgang)
							  (riege, wertungen) <- riegen
						  } {
							  service.insertRiegenWertungen(RiegeRaw(
							    wettkampfId = wettkampf.id,
							    r = riege,
							    durchgang = Some(durchgang),
							    start = Some(start.id)
							  ), wertungen)
						  }
						  reloadData()
              riegenFilterView.sort
              durchgangView.sort
					  }
				  }
			  }
		  })
    }

    val btnRegenerateDurchgang = new Button("Durchgang neu einteilen ...") {
      onAction = (ae: ActionEvent) => {
        val actSelection = durchgangView.selectionModel().selectedItems.map(d => d.initname)
        if(actSelection.nonEmpty) {
          doRegenerateDurchgang(actSelection.toSet)(ae)
        }
      }
      disable <== when(makeDurchgangActiveBinding) choose true otherwise false
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
                  riegenFilterView.sort
                  durchgangView.sort
  						  }
						  }
					  }
				  }
			  })
      }
      ret.setDisable(durchgang.size < 2)
      ret
    }

    def toGeraetId(tcpl: List[Int]): List[Long] = {
      def tgi(tcp: Int): Int = {
        (tcp - 5) / 2
      }
      tcpl map tgi filter { _ > -1 } map { disziplinlist(_).id }
    }

    def makeMoveDurchganMenu(durchgang: DurchgangEditor, cells: List[TablePosition[_, _]]): Menu = {
      val selectedGerate = toGeraetId(cells.map(c => c.column))
      new Menu("In anderen Durchgang verschieben") {
        durchgang.initstartriegen
        .filter(d => selectedGerate.isEmpty || selectedGerate.contains(d._1.id))
        .flatMap(_._2)
        .toList.sortBy(r => r.initanz)
        .foreach{riege =>
          items += new Menu(riege.initname + " ("+riege.initanz+")") {
            durchgangModel.filter(d => !d.equals(durchgang)).foreach{durchgang =>
        	    items += KuTuApp.makeMenuAction(durchgang.initname) {(caption, action) =>
        	      val toSave = riege.copy(initdurchgang = Some(durchgang.initname))
  						  KuTuApp.invokeWithBusyIndicator {
        				  service.updateOrinsertRiege(toSave.commit)
        				  reloadData()
                  riegenFilterView.sort
                  durchgangView.sort
  						  }
        	    }
            }
          }.asInstanceOf[MenuItem]
        }
        disable.value = items.size() == 0
      }
    }
    def makeMoveStartgeraetMenu(durchgang: DurchgangEditor, cells: List[TablePosition[_, _]]): Menu = {
      val selectedGerate = toGeraetId(cells.map(c => c.column))
      new Menu("Auf anderes Startgerät verschieben") {
        durchgang.initstartriegen.filter(d => selectedGerate.isEmpty || selectedGerate.contains(d._1.id)).flatMap(_._2).toList.sortBy(r => r.initanz).foreach{riege =>
          val von = "Von " + riege.initstart.map(d => d.name + " (" + durchgang.initstartriegen.get(d).map(r => r.map(re => re.initanz).sum).getOrElse(0) + ")").getOrElse("?") + " auf "
          items += new Menu(riege.initname+ " ("+riege.initanz+")") {
            disziplinlist
            .filter(d => riege.initstart match {case Some(dd) if(dd.equals(d)) => false case _ => true})
            .foreach{start =>
        	    items += KuTuApp.makeMenuAction(von + start.name + " (" + durchgang.initstartriegen.get(start).map(r => r.map(re => re.initanz).sum).getOrElse(0) + ")") {(caption, action) =>
        	      val toSave = riege.copy(initstart = Some(start))
  						  KuTuApp.invokeWithBusyIndicator {
        				  service.updateOrinsertRiege(toSave.commit)
        				  reloadData()
                  riegenFilterView.sort
                  durchgangView.sort
  						  }
        	    }
            }
          }.asInstanceOf[MenuItem]
        }
        disable.value = items.size() == 0
      }
    }
    def makeRenameDurchgangMenu: MenuItem = {
      val m = KuTuApp.makeMenuAction("Durchgang umbenennen ...") {(caption, action) =>
      			  implicit val impevent = action
			  val selectedDurchgang = durchgangView.selectionModel.value.selectedItem.value.initname
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
                riegenFilterView.sort
                durchgangView.sort
      			  }
      		  }
      	  }
      	)
      }
      m.disable = durchgangView.selectionModel.value.selectedItems.size != 1
      m
    }

    val btnEditDurchgang = new MenuButton("Durchgang ...") {
      disable <== when(makeDurchgangActiveBinding) choose true otherwise false
    }

    durchgangView.selectionModel.value.setSelectionMode(SelectionMode.Multiple)
    durchgangView.selectionModel.value.setCellSelectionEnabled(true)
    
    durchgangView.setOnDragDetected((event) => {
      val focusedCells = durchgangView.selectionModel.value.selectedCells.toList
      val selectedGerate = toGeraetId(focusedCells.map(c => c.column))
      val actDurchgangSelection = focusedCells.map(c => durchgangView.items.value.get(c.row)).toSet.filter(_ != null)
      if(actDurchgangSelection.size == 1 && selectedGerate.size == 1) {
        val startgeraet = selectedGerate.head
        val durchgangEditor = actDurchgangSelection.head
        val riegenEditorCandidates = durchgangEditor.initstartriegen.filter(d => selectedGerate.isEmpty || selectedGerate.contains(d._1.id)).flatMap(_._2).toList.sortBy(r => r.initname).map(r => r.initname)
        if (event.pickResult.getIntersectedNode.isInstanceOf[Text]) {
          val text = event.pickResult.getIntersectedNode.asInstanceOf[Text]
          val vb = DurchgangView.positionInScene(text.parent.value)
          val pointInText = new Point2D(event.getSceneX - vb.x, event.getSceneY - vb.y)
          val spacePerRiege = (text.boundsInLocal.value.getHeight) / riegenEditorCandidates.size
          val hoveredTextIndex = (Math.abs(Math.max(1,pointInText.y)) / spacePerRiege).toInt
          val hoveredText = riegenEditorCandidates(Math.min(riegenEditorCandidates.size -1, hoveredTextIndex))

          val snp = text.parent.value.snapshot(null, null)
          
          val width = snp.getWidth.toInt
          val height: Int = Math.min(snp.getHeight.toInt - 4 + (hoveredTextIndex * spacePerRiege).toInt, spacePerRiege.toInt)
          val croppedImage = new WritableImage(snp.getPixelReader(), 
              0, 5 + (hoveredTextIndex * spacePerRiege).toInt+1, 
              width, height)

          val db = durchgangView.startDragAndDrop(TransferMode.Move)
          db.setDragView(croppedImage)
          val content = new ClipboardContent()
          content.put(DurchgangView.DRAG_RIEGE, (durchgangEditor.initname, hoveredText, startgeraet))
          content.putString(hoveredText)
          db.setContent(content)

          event.consume()
        }
      }
    })
    durchgangView.setOnDragDropped((event) => {
        val db = event.getDragboard()
        // If this is a meaningful drop...
        if (db.hasContent(DurchgangView.DRAG_RIEGE)) {
          val (selecteddurchgang, selectedriege, selectedGeraet) = db.getContent(DurchgangView.DRAG_RIEGE).asInstanceOf[(String, String, Long)]
          val fromDisziplin = disziplinlist.filter(_.id == selectedGeraet).head
          val dg = durchgangView.items.value.toList.filter(dge => dge.initname == selecteddurchgang).head
          dg.initstartriegen.filter(d => d._1.id == selectedGeraet).flatMap(_._2).filter(r => r.initname == selectedriege).headOption match {
            case Some(riege) =>
              def findTableCell(node: Object): Option[jfxsc.TableCell[DurchgangEditor,_]] = 
                if (node.isInstanceOf[jfxsc.TableCell[_,_]]) {
                  Some(node.asInstanceOf[jfxsc.TableCell[DurchgangEditor,_]])
                } else if (node.isInstanceOf[Text]) {
                  findTableCell(node.asInstanceOf[Text].getParent)
                } else {
                  None
                }
              findTableCell(event.getPickResult.getIntersectedNode) match {
                case Some(selectedCell) => 
                  val durchgang = selectedCell.getTableRow
                  val startGeraetColumn = selectedCell.getTableColumn
                  if (startGeraetColumn.isInstanceOf[DurchgangTCAccess]) {
                    val targetStartgeraet = startGeraetColumn.asInstanceOf[DurchgangTCAccess].getDisziplin
                    if (targetStartgeraet != fromDisziplin || !durchgang.getItem.equals(dg)) {
                      val targetDurchgang = durchgang.getItem.asInstanceOf[DurchgangEditor].initname
                      val toSave = riege.copy(initstart = Some(targetStartgeraet), initdurchgang = Some(targetDurchgang))
                      println(targetDurchgang, targetStartgeraet)
          					  KuTuApp.invokeWithBusyIndicator {
              				  service.updateOrinsertRiege(toSave.commit)
              				  reloadData()
                        riegenFilterView.sort
                        durchgangView.sort
          					  }          
                    }                  
                  }
                case None => 
              }
              event.setDropCompleted(true)
            case None => event.setDropCompleted(false)
          }
        }
        event.consume()
    })
    durchgangView.setOnDragOver((event) => {
        if ( event.getDragboard().hasContent(DurchgangView.DRAG_RIEGE)) {
            event.acceptTransferModes(TransferMode.Move)
        }
        event.consume()
    })
        
    durchgangView.getSelectionModel().getSelectedCells().onChange { ( _, newItem) =>
      Platform.runLater {
        val focusedCells = durchgangView.selectionModel.value.selectedCells.toList
        val as = focusedCells.map(c => durchgangView.items.value.get(c.row)).toSet
        val actSelection = as/*durchgangView.selectionModel.value.selectedItems.toList*/.filter(_ != null).map(d => d.initname)
        durchgangView.contextMenu = new ContextMenu() {
            items += makeRegenereateDurchgangMenu(actSelection.toSet)
            items += makeMergeDurchganMenu(actSelection.toSet)
            items += makeRenameDurchgangMenu
            if(actSelection.size == 1) {
          	  items += new SeparatorMenuItem()
              items += makeMoveDurchganMenu(durchgangView.selectionModel().selectedItems.head, focusedCells)
              items += makeMoveStartgeraetMenu(durchgangView.selectionModel().selectedItems.head, focusedCells)
            }
        }
        
        btnEditDurchgang.text.value = "Durchgang " + actSelection.mkString("[",", ", "]") + " bearbeiten"
        btnEditDurchgang.items.clear
        btnEditDurchgang.items += makeRegenereateDurchgangMenu(actSelection.toSet)
        btnEditDurchgang.items += makeMergeDurchganMenu(actSelection.toSet)
        btnEditDurchgang.items += makeRenameDurchgangMenu
        if(actSelection.size == 1) {
      	  btnEditDurchgang.items += new SeparatorMenuItem()
          btnEditDurchgang.items += makeMoveDurchganMenu(durchgangView.selectionModel().selectedItems.head, focusedCells)
          btnEditDurchgang.items += makeMoveStartgeraetMenu(durchgangView.selectionModel().selectedItems.head, focusedCells)
        }
      }
    }
    var warnIcon: Image = null
    try {
      warnIcon = new Image(getClass().getResourceAsStream("/images/OrangeWarning.png"))
    } catch {
      case e: Exception => e.printStackTrace()
    }
    
    def makeRiegenSuggestMenu(): MenuItem = {
      val m = KuTuApp.makeMenuAction("Riegen & Durchgänge frisch einteilen ...") {(caption: String, action: ActionEvent) =>
        doRiegenSuggest(action)
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
    
    def computeRiegenSuggest(event: ActionEvent, gruppengroesse: Int) {
      implicit val impevent = event
      PageDisplayer.showInDialog("Riegen neu einteilen ...", new DisplayablePage() {
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
                    s"Mit der Neueinteilung der Riegen und Druchgänge werden die bisherigen Einteilungen zurückgesetzt."))
          }
        }
      }, new Button("OK") {
        onAction = (event: ActionEvent) => {
          implicit val impevent = event
				  KuTuApp.invokeWithBusyIndicator {
					  val riegenzuteilungen = DurchgangBuilder(service).suggestDurchgaenge(
						  wettkampf.id,
						  gruppengroesse)

						service.cleanAllRiegenDurchgaenge(wettkampf.id)

					  for{
					    durchgang <- riegenzuteilungen.keys
						  (start, riegen) <- riegenzuteilungen(durchgang)
						  (riege, wertungen) <- riegen
					  } {
						  service.insertRiegenWertungen(RiegeRaw(
						    wettkampfId = wettkampf.id,
						    r = riege,
						    durchgang = Some(durchgang),
						    start = Some(start.id)
						  ), wertungen)
					  }
					  reloadData()
            riegenFilterView.sort
            durchgangView.sort
				  } 
        }
      })
    }

    def doRiegenReset(event: ActionEvent) {
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
            riegenFilterView.sort
            durchgangView.sort
				  } 
        }
      })    
    }
    
    def doRiegenSuggest(event: ActionEvent) {
		  implicit val impevent = event
  	  val txtGruppengroesse2 = new TextField() {
  	    text <==> txtGruppengroesse.text 
  	    tooltip = "Max. Gruppengrösse oder 0 für gleichmässige Verteilung mit einem Durchgang."
  	  }
		  PageDisplayer.showInDialog("Riegen neu einteilen ...", new DisplayablePage() {
			  def getPage: Node = {
				  new HBox {
					  prefHeight = 50
					  alignment = Pos.BottomRight
					  hgrow = Priority.Always
					  children = Seq(new Label("Maximale Gruppengrösse: "), txtGruppengroesse2)
				  }
			  }
		  }, new Button("OK (bestehende Einteilung wird zurückgesetzt)", new ImageView { image = warnIcon }) {
			  onAction = (event: ActionEvent) => {
				  if (!txtGruppengroesse2.text.value.isEmpty) {
				    Platform.runLater{
				      computeRiegenSuggest(event, str2Int(txtGruppengroesse2.text.value))
				    }
				  }
			  }
		  })
    }
      
    def doRiegenEinheitenExport(event: ActionEvent) {
		  implicit val impevent = event
		  KuTuApp.invokeWithBusyIndicator {
			  val filename = "Einheiten.csv"
					  val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
			  if(!dir.exists()) {
				  dir.mkdirs();
			  }
			  val file = new java.io.File(dir.getPath + "/" + filename)

			  ResourceExchanger.exportEinheiten(wettkampf.toWettkampf, file.getPath)
			  Desktop.getDesktop().open(file);
		  }
    }
    
    def makeRiegenEinheitenExport(): MenuItem = {
      val m = KuTuApp.makeMenuAction("Riegen Einheiten export") {(caption: String, action: ActionEvent) =>
        doRiegenEinheitenExport(action)
      }
      m
    }
    
    def doDurchgangExport(event: ActionEvent) {
		  implicit val impevent = event
		  KuTuApp.invokeWithBusyIndicator {
			  val filename = "Durchgaenge.csv"
					  val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
			  if(!dir.exists()) {
				  dir.mkdirs();
			  }
			  val file = new java.io.File(dir.getPath + "/" + filename)

			  ResourceExchanger.exportDurchgaenge(wettkampf.toWettkampf, file.getPath)
			  Desktop.getDesktop().open(file);
		  }
    }
    
    def makeDurchgangExport(): MenuItem = {
      val m = KuTuApp.makeMenuAction("Durchgang-Planung export") {(caption: String, action: ActionEvent) =>
        doDurchgangExport(action)
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
                riegenFilterView.sort
                durchgangView.sort
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
                riegenFilterView.sort
                durchgangView.sort
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
			  val selectedDurchgang = riegenFilterView.selectionModel.value.getSelectedItem.durchgang.value
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
                riegenFilterView.sort
                durchgangView.sort
      			  }
      		  }
      	  }
      	)
		  }
    }
    
    def doRiegenBelatterExport(event: ActionEvent) {
      val seriendaten = service.getAllKandidatenWertungen(wettkampf.uuid.map(UUID.fromString(_)).get)
      val filename = "Riegenblatt_" + wettkampf.easyprint.replace(" ", "_") + ".html"
      val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
      if(!dir.exists()) {
        dir.mkdirs();
      }
      val logofile = PrintUtil.locateLogoFile(dir)
      def generate(lpp: Int) = (new Object with ch.seidel.kutu.renderer.RiegenblattToHtmlRenderer).toHTML(seriendaten, logofile)
      PrintUtil.printDialog(text.value, FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
    }
    
    def makeRiegenBlaetterExport(): MenuItem = {
      val m = KuTuApp.makeMenuAction("Riegenblätter erstellen") {(caption: String, action: ActionEvent) =>
        doRiegenBelatterExport(action)
      }
      m
    }

    def makeRiegenQRCodesExport(): MenuItem = {
      val m = KuTuApp.makeMenuAction("QR-Codes für Wertungsrichter erstellen") {(caption: String, action: ActionEvent) =>
        
        val seriendaten = RiegenBuilder.mapToGeraeteRiegen(service.getAllKandidatenWertungen(wettkampf.uuid.map(UUID.fromString(_)).get).toList)
              .filter(gr => gr.durchgang.nonEmpty && gr.disziplin.nonEmpty) 
              .map(WertungsrichterQRCode.toMobileConnectData(wettkampf, remoteBaseUrl))
              .toSet.toList
        val filename = "WertungsrichterConnectQRCodes_" + wettkampf.easyprint.replace(" ", "_") + ".html"
        val dir = new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
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
      items += makeRiegenEinheitenExport()
      items += makeDurchgangExport()
      items += makeRiegenBlaetterExport()
      items += makeRiegenQRCodesExport()
    }
    
    val riegenFilterControl = new ToolBar {
      content = List[ButtonBase](
          btnRiegen
        , btnEditDurchgang
        , riegeRenameButton
        , riegenRemoveButton
        , durchgangRenameButton
        , btnExport
      )
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
        }
      }
    }

    def sorter = () => {
      riegenFilterView.sort
      durchgangView.sort
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
