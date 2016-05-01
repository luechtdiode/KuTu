package ch.seidel.kutu.view

import javafx.scene.{ control => jfxsc }
import scalafx.Includes._
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry._
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.Tab
import scalafx.scene.control.TableColumn
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableView
import scalafx.scene.control.TextField
import scalafx.scene.control.ToolBar
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.Region
import scalafx.scene.layout.VBox
import scalafx.util.converter.DefaultStringConverter
import scalafx.scene.control.ComboBox
import scalafx.scene.input.Clipboard
import scala.io.Source
import java.text.SimpleDateFormat
import scalafx.scene.control.SelectionMode
import scalafx.application.Platform
import java.io.File
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.awt.Desktop
import javafx.scene.{control => jfxsc}
import scala.IndexedSeq
import scalafx.beans.property._
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableView.sfxTableView2jfx
import scalafx.scene.control.cell.CheckBoxTableCell
import scalafx.scene.layout.StackPane
import scalafx.scene.web.WebView
import ch.seidel.commons._
import ch.seidel.kutu.renderer.NotenblattToHtmlRenderer
import ch.seidel.kutu.domain._
import scalafx.scene.control.CheckBox
import scalafx.scene.control.SplitPane
import ch.seidel.kutu.renderer.KategorieTeilnehmerToHtmlRenderer
import scalafx.scene.control.cell.ComboBoxTableCell
import scalafx.util.StringConverter
import ch.seidel.kutu.KuTuApp
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import ch.seidel.kutu.data.ResourceExchanger
import ch.seidel.kutu.renderer.RiegenblattToHtmlRenderer

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
          new TableColumn[DurchgangEditor, String] {
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

  text = "Riegeneinteilung"

  def reloadRiegen() {
    riegenFilterModel.clear()
    riegen().foreach(riegenFilterModel.add(_))

  }

  def reloadDurchgaenge() {
    durchgangModel.clear()
    riegenFilterModel.groupBy(re => re.initdurchgang).map{res =>
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
          None))
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

    val riegensuggestButton = new Button {
  	  text = "Riegen einteilen"
  	  minWidth = 75
  	  val txtGruppengroesse = new TextField()
  	  onAction = (event: ActionEvent) => {
  		  implicit val impevent = event
			  txtGruppengroesse.text = "11"
			  PageDisplayer.showInDialog(text.value, new DisplayablePage() {
 				  def getPage: Node = {
  				  new HBox {
  					  prefHeight = 50
  					  alignment = Pos.BottomRight
  					  hgrow = Priority.Always
  					  children = Seq(new Label("Maximale Gruppengrösse: "), txtGruppengroesse)
  				  }
  			  }
			  }, new Button("OK") {
				  onAction = (event: ActionEvent) => {
					  if (!txtGruppengroesse.text.value.isEmpty) {
						  KuTuApp.invokeWithBusyIndicator {
							  val riegenzuteilungen = service.suggestDurchgaenge(
  							  wettkampf.id,
  							  str2Int(txtGruppengroesse.text.value))

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
				  }
			  })
  	  }
    }
    val einheitenExportButton = new Button {
  	  text = "Riegen Einheiten export"
		  minWidth = 75
  	  onAction = (event: ActionEvent) => {
  		  implicit val impevent = event
			  KuTuApp.invokeWithBusyIndicator {
  			  val filename = "Einheiten.csv"
  					  val dir = new java.io.File(service.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
  			  if(!dir.exists()) {
  				  dir.mkdirs();
  			  }
  			  val file = new java.io.File(dir.getPath + "/" + filename)

  			  ResourceExchanger.exportEinheiten(wettkampf.toWettkampf, file.getPath)
  			  Desktop.getDesktop().open(file);
  		  }
  	  }
    }
    val durchgangExportButton = new Button {
  	  text = "Durchgang-Planung export"
		  minWidth = 75
  	  onAction = (event: ActionEvent) => {
  		  implicit val impevent = event
			  KuTuApp.invokeWithBusyIndicator {
  			  val filename = "Durchgaenge.csv"
  					  val dir = new java.io.File(service.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
  			  if(!dir.exists()) {
  				  dir.mkdirs();
  			  }
  			  val file = new java.io.File(dir.getPath + "/" + filename)

  			  ResourceExchanger.exportDurchgaenge(wettkampf.toWettkampf, file.getPath)
  			  Desktop.getDesktop().open(file);
  		  }
  	  }
    }
    val riegenRemoveButton = new Button {
  	  text = "Riege löschen"
		  minWidth = 75
		  disable <== when(riegenFilterView.selectionModel.value.selectedItemProperty().isNull()) choose true otherwise false
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
		  disable <== when(riegenFilterView.selectionModel.value.selectedItemProperty().isNull()) choose true otherwise false
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
		  disable <== when(riegenFilterView.selectionModel.value.selectedItemProperty().isNull()) choose true otherwise false
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
    val generateRiegenblaetter = new Button with RiegenblattToHtmlRenderer {
      text = "Riegenblätter erstellen"
      minWidth = 75

      onAction = (event: ActionEvent) => {
        val driver = service.selectWertungen(wettkampfId = Some(wettkampf.id)).groupBy { x => x.athlet }.map(_._2).toList
        val programme = driver.flatten.map(x => x.wettkampfdisziplin.programm).foldLeft(Seq[ProgrammView]()){(acc, pgm) =>
          if(!acc.exists { x => x.id == pgm.id }) {
            acc :+ pgm
          }
          else {
            acc
          }
        }
        println(programme)
        val riegendurchgaenge = service.selectRiegen(wettkampf.id).map(r => r.r-> r).toMap
        val seriendaten = for {
          programm <- programme
          athletwertungen <- driver.map(we => we.filter { x => x.wettkampfdisziplin.programm.id == programm.id})
          if(athletwertungen.nonEmpty)
        }
        yield {
          val einsatz = athletwertungen.head
          val athlet = einsatz.athlet
          Kandidat(
          einsatz.wettkampf.easyprint
          ,athlet.geschlecht match {case "M" => "Turner"  case _ => "Turnerin"}
          ,einsatz.wettkampfdisziplin.programm.easyprint
          ,athlet.id
          ,athlet.name
          ,athlet.vorname
          ,AthletJahrgang(athlet.gebdat).hg
          ,athlet.verein match {case Some(v) => v.easyprint case _ => ""}
          ,riegendurchgaenge.get(einsatz.riege.getOrElse(""))
          ,riegendurchgaenge.get(einsatz.riege2.getOrElse(""))
          ,athletwertungen.filter{wertung =>
            if(wertung.wettkampfdisziplin.feminim == 0 && !wertung.athlet.geschlecht.equalsIgnoreCase("M")) {
              false
            }
            else if(wertung.wettkampfdisziplin.masculin == 0 && wertung.athlet.geschlecht.equalsIgnoreCase("M")) {
              false
            }
            else {
              true
            }
          }.map(_.wettkampfdisziplin.disziplin.easyprint)
          )
        }
        val filename = "Riegenblatt_" + wettkampf.easyprint.replace(" ", "_") + ".html"
        val dir = new java.io.File(service.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
        if(!dir.exists()) {
          dir.mkdirs();
        }
        val file = new java.io.File(dir.getPath + "/" + filename)
        val logofile = if(new java.io.File(dir.getPath + "/logo.jpg").exists()) {
          "logo.jpg"
        }
        else {
          "../logo.jpg"
        }
        val toSave = toHTML(seriendaten, logofile)
        val os = new BufferedOutputStream(new FileOutputStream(file))
        os.write(toSave.getBytes("UTF-8"))
        os.flush()
        os.close()
        Desktop.getDesktop().open(file);
      }
    }

    val riegenFilterControl = new ToolBar {
      content = List(
          riegensuggestButton
        , einheitenExportButton
        , riegeRenameButton
        , riegenRemoveButton
        , durchgangRenameButton
        , durchgangExportButton
        , generateRiegenblaetter
      )
    }

    val riegenFilterPane = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      margin = Insets(0, 0, 0, 10)
      top = riegenFilterControl
      center = new BorderPane {
        center = riegenFilterView
      }
    }

//    durchgangView.selectionModel.value.setCellSelectionEnabled(true)
//    durchgangView.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
//      AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(durchgangView, false, null)(ke)
//    }

    def sorter = () => {
      riegenFilterView.sort
      durchgangView.sort
    }
    riegenFilterView.addListener(onRiegeChanged(sorter))

    riegenFilterView.selectionModel.value.setCellSelectionEnabled(true)
    riegenFilterView.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(riegenFilterView, false, null)(ke)
    }

    val container = new SplitPane {
      orientation = Orientation.VERTICAL
      dividerPositions = 0.4
      id = "page-splitpane"
      items.addAll(riegenFilterPane, durchgangView)
      SplitPane.setResizableWithParent(durchgangView, false)
    }
    content = container

    true
  }

}
