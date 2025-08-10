package ch.seidel.kutu.view

import ch.seidel.commons._
import ch.seidel.kutu.Config._
import ch.seidel.kutu.KuTuApp.{enc, handleAction}
import ch.seidel.kutu.actors._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.http.WebSocketClient
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.renderer._
import ch.seidel.kutu.squad.RiegenBuilder.{generateRiegen2Name, generateRiegenName}
import ch.seidel.kutu.{Config, KuTuApp, KuTuServer}
import javafx.scene.{control => jfxsc}
import org.slf4j.{Logger, LoggerFactory}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.StringProperty.sfxStringProperty2jfx
import scalafx.beans.property._
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.observableBuffer2ObservableList
import scalafx.event.ActionEvent
import scalafx.event.subscriptions.Subscription
import scalafx.geometry._
import scalafx.print.PageOrientation
import scalafx.scene.Node
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableView.sfxTableView2jfx
import scalafx.scene.control._
import scalafx.scene.input.{Clipboard, KeyEvent}
import scalafx.scene.layout._
import scalafx.util.StringConverter
import scalafx.util.converter.{DefaultStringConverter, DoubleStringConverter}

import java.io.File
import java.net.URI
import java.time.{LocalDate, Period}
import java.util.UUID
import java.util.concurrent.{ScheduledFuture, TimeUnit}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.{Codec, Source}
import scala.util.{Failure, Success}

class WettkampfWertungTab(wettkampfmode: BooleanProperty, programm: Option[ProgrammView], riege: Option[GeraeteRiege], wettkampfInfo: WettkampfInfo, override val service: KutuService, athleten: => IndexedSeq[WertungView]) extends Tab with TabWithService {
  val logger = LoggerFactory.getLogger(this.getClass)
  val wettkampf = wettkampfInfo.wettkampf
  val wettkampfFilterDate = if (wettkampfInfo.isJGAlterklasse) {
    LocalDate.of(wettkampf.datum.toLocalDate.getYear, 1, 1)
  } else {
    wettkampf.datum.toLocalDate
  }

  logger.debug("create Wertungen Tab for " + programm)

  import language.implicitConversions

  implicit def doublePropertyToObservableValue(p: DoubleProperty): ObservableValue[Double, Double] = p.asInstanceOf[ObservableValue[Double, Double]]

  private var lazypane: Option[LazyTabPane] = None

  def setLazyPane(pane: LazyTabPane): Unit = {
    lazypane = Some(pane)
  }

  def refreshLazyPane(): Unit = {
    lazypane match {
      case Some(pane) => pane.refreshTabs()
      case _ =>
    }
  }

  def refreshOtherLazyPanes(): Unit = {
    lazypane match {
      case Some(pane) => pane.refreshTabs()
      case _ =>
    }
  }

  var scheduledGears: List[Disziplin] = List.empty

  var subscription: Option[Subscription] = None
  var websocketsubscription: Option[Subscription] = None

  override def release: Unit = {
    websocketsubscription.foreach(_.cancel())
    websocketsubscription = None
    subscription match {
      case Some(s) =>
        s.cancel()
        subscription = None
      case None =>
    }
  }


  def defaultFilter: (WertungView) => Boolean = { wertung =>
    programm match {
      case Some(progrm) =>
        wertung.wettkampfdisziplin.programm.programPath.contains(progrm)
      case None =>
        true
    }
  }

  def reloadWertungen(extrafilter: (WertungView) => Boolean = defaultFilter) = {
    athleten.
      filter(wv => wv.wettkampf.id == wettkampf.id).
      filter(extrafilter).
      groupBy(wv => wv.athlet).
      map(wvg => wvg._2.map(WertungEditor)).toIndexedSeq
  }

  var wertungen = Seq[IndexedSeq[WertungEditor]]()
  val wkModel = ObservableBuffer.from(wertungen)
  val wkview = new TableView[IndexedSeq[WertungEditor]](wkModel) {
    id = "kutu-table"
    editable = !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)
  }
  var emptyRiege = GeraeteRiege("", "", None, 0, None, Seq(), false, "")
  var relevantRiegen: Map[String, (Boolean, Int)] = Map[String, (Boolean, Int)]()

  val cmbDurchgangFilter = new GeraeteRiegeComboBox(wkview)

  def rebuildDurchgangFilterList = {
    val kandidaten = service.getAllKandidatenWertungen(UUID.fromString(wettkampf.uuid.get))
    val alleRiegen = RiegenBuilder.mapToGeraeteRiegen(kandidaten)
    var newGearList = alleRiegen.flatMap(r => r.disziplin).distinct
    newGearList = if (newGearList.isEmpty) disziplinlist else newGearList
    scheduledGears = newGearList

    val ret = alleRiegen
      .filter(r => riege.isEmpty || riege.get.sequenceId == r.sequenceId)
      .filter(r => wertungen.exists { p => r.kandidaten.exists { k => p.head.init.athlet.id == k.id } })
    emptyRiege = GeraeteRiege("", "", None, 0, None, Seq(), ret.forall(r => r.erfasst), "")
    emptyRiege +: ret
  }

  def computeRelevantRiegen = {
    val prefilteredRiegen: Set[(String, Int)] =
      if (wertungen.size > 0)
        wertungen.
          flatMap(x => x.flatMap(x => Seq(x.init.riege, x.init.riege2).flatten).toSet).
          groupBy(x => x).map(x => (x._1, x._2.size)).toSet
      else
        Set.empty[(String, Int)]

    prefilteredRiegen.
      map(x => x._1 -> (relevantRiegen.getOrElse(x._1, (true, x._2))._1, x._2))
      .toMap
  }

  def riegen(onSelectedChange: (String, Boolean) => Boolean): IndexedSeq[RiegeEditor] = {
    service.listRiegenZuWettkampf(wettkampf.id).filter { r => relevantRiegen.contains(r._1) }.sortBy(r => r._1).map(x =>
      RiegeEditor(
        wettkampf.id,
        x._1,
        x._2,
        if (relevantRiegen.contains(x._1)) relevantRiegen(x._1)._2 else 0,
        relevantRiegen.contains(x._1) && relevantRiegen(x._1)._1,
        x._3,
        x._4,
        Some(onSelectedChange)))
  }

  val riegenFilterModel = ObservableBuffer[RiegeEditor]()

  val athletHeaderPane: AthletHeaderPane = new AthletHeaderPane(wkview)
  val disziplinlist = wettkampfInfo.disziplinList
  val withDNotes = wettkampfInfo.isDNoteUsed
  var lastFilter = ""
  var durchgangFilter = riege match {
    case Some(riege) => riege
    case None => emptyRiege
  }

  var lazyEditorPaneUpdater: Map[String, ScheduledFuture[_]] = Map.empty

  def submitLazy(name: String, task: () => Unit, delay: Long): Unit = {
    lazyEditorPaneUpdater.get(name).foreach(_.cancel(true))
    val ft = KuTuApp.lazyExecutor.schedule(new Runnable() {
      def run = {
        Platform.runLater {
          task()
        }
      }
    }, delay, TimeUnit.SECONDS)

    lazyEditorPaneUpdater = lazyEditorPaneUpdater + (name -> ft)
  }

  def updateEditorPane(focusHolder: Option[Node] = None): Unit = {
    def task: () => Unit = () => {
      if (selected.value) {
        if (logger.isDebugEnabled()) {
          logger.debug("updating EditorPane ")
        }
        athletHeaderPane.adjust
        updateRiegen()
        val model = cmbDurchgangFilter.items.getValue
        val raw = rebuildDurchgangFilterList
        val selected = cmbDurchgangFilter.selectionModel.value.selectedItem.value
        model.foreach { x =>
          raw.find {
            _.softEquals(x)
          } match {
            case Some(item) =>
              val reselect = selected.softEquals(x)
              model.set(model.indexOf(x), item)
              if (reselect) {
                durchgangFilter = item
                cmbDurchgangFilter.selectionModel.value.select(item)
              }
            case None =>
          }
        }
        val toRemove =
          for {
            o <- model
            i = raw.find {
              _.softEquals(o)
            }
            if (i.isEmpty && o.softEquals(emptyRiege))
          }
          yield {
            model.indexOf(o)
          }
        for {i <- toRemove.sorted.reverse} {
          model.remove(i)
        }

        for {
          o <- raw
          i = model.find {
            _.softEquals(o)
          }
          if (i.isEmpty)
        } {
          model.insert(raw.indexWhere { rx => rx.softEquals(o) }, o)
        }
        updateAlleRiegenCheck()
        focusHolder.foreach(_.requestFocus())
      }
    }

    submitLazy("updateEditorPane", task, 5)
  }

  case class DoubleConverter(notenModus: NotenModus) extends DoubleStringConverter {
    override def toString(value: Double) = notenModus.toString(value)

    override def fromString(var1: String): Double = if (var1 == null || var1.trim.isEmpty) Double.NaN
    else {
      super.fromString(var1)
    }
  }

  def wertungenCols = if (wertungen.nonEmpty) {
    val indexerE = Iterator.from(0)
    val indexerD = Iterator.from(0)
    val indexerF = Iterator.from(0)
    import javafx.css.PseudoClass
    val editableCssClass = PseudoClass.getPseudoClass("editable")
    val formularCssClass = PseudoClass.getPseudoClass("formular")
    wertungen.head.map { wertung =>
      val dNoteLabel = wertung.init.wettkampfdisziplin.notenSpez.getDifficultLabel
      val eNoteLabel = wertung.init.wettkampfdisziplin.notenSpez.getExecutionLabel

      def formEditorAction(index: Int, mapper: Wertung => String): FormularAction[IndexedSeq[WertungEditor], Double] = new FormularAction[IndexedSeq[WertungEditor], Double] {
        override def hasFormular(cell: TextFieldWithToolButtonTableCell[IndexedSeq[WertungEditor], Double]): Boolean = {
          val w = cell.tableRow.value.item.value(index)
          w.init.defaultVariables.nonEmpty
        }
        override def clear(cell: TextFieldWithToolButtonTableCell[IndexedSeq[WertungEditor], Double], ae: ActionEvent): Unit = {
          val w = cell.tableRow.value.item.value(index)
          w.clearInput()
          val box = TemplateFormular(w)
          PageDisplayer.showInDialog(box.caption, new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                center = box
              }
            }
          },
            new Button("OK") {
              onAction = handleAction { implicit e: ActionEvent =>
                try {
                  cell.commitEdit(cell.sc.fromString(mapper(w.commit)))
                }
                catch {
                  case e: IllegalArgumentException =>
                    PageDisplayer.showErrorDialog(box.caption)(e)
                }
              }
            }
          )(ae)
          //if (w.isDirty) {
          //  service.updateWertung(w.updateAndcommit)
          //}
        }
        override def fire(cell: TextFieldWithToolButtonTableCell[IndexedSeq[WertungEditor], Double], ae: ActionEvent): Unit = {
          val w = cell.tableRow.value.item.value(index)
          val box = TemplateFormular(w)
          PageDisplayer.showInDialog(box.caption, new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                center = box
              }
            }
          },
            new Button("OK") {
              onAction = handleAction { implicit e: ActionEvent =>
                try {
                  cell.commitEdit(cell.sc.fromString(mapper(w.commit)))
                }
                catch {
                  case e: IllegalArgumentException =>
                    PageDisplayer.showErrorDialog(box.caption)(e)
                }
              }
            }
          )(ae)
        }
      }

      lazy val clDnote = new WKTableColumn[Double](indexerD.next()) {
        val hasNoFormTemplate = wertungen.forall(we => if (index < we.size) we(index).init.wettkampfdisziplin.notenSpez.template.isEmpty else true)
        text = dNoteLabel

        cellValueFactory = { x =>
          if (x.value.size > index)
            x.value(index).noteD
          else
            wertung.noteD
        }
        cellFactory.value = { _: Any =>
          if (hasNoFormTemplate) {
            new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], Double](
              DoubleConverter(wertung.init.wettkampfdisziplin.notenSpez),
              (cell) => {
                if (cell.tableRow.value != null && cell.tableRow.value.item.value != null && index < cell.tableRow.value.item.value.size) {
                  val w = cell.tableRow.value.item.value(index)
                  val editable = w.matchesSexAssignment
                  cell.editable = editable
                  cell.pseudoClassStateChanged(editableCssClass, cell.isEditable)
                }
              }
            )
          } else {
            new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], Double](
              DoubleConverter(wertung.init.wettkampfdisziplin.notenSpez),
              (cell) => {
                if (cell.tableRow.value != null && cell.tableRow.value.item.value != null && index < cell.tableRow.value.item.value.size) {
                  val w = cell.tableRow.value.item.value(index)
                  val editable = w.matchesSexAssignment
                  cell.editable = editable
                  cell.pseudoClassStateChanged(formularCssClass, cell.isEditable)
                }
              }, formEditorAction(index, we => we.noteD.map(_.toString()).getOrElse("")))
          }
        }

          styleClass += "table-cell-with-value"
          prefWidth = if (wertung.init.wettkampfdisziplin.isDNoteUsed) 60 else 0
          editable = !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && wertung.init.wettkampfdisziplin.isDNoteUsed
          visible = wertung.init.wettkampfdisziplin.isDNoteUsed

        onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
          if (evt.rowValue != null && evt.rowValue.size > index) {
            val disciplin = evt.rowValue(index)
            disciplin.reset
          }
        }
          onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
            if (evt.rowValue != null && evt.rowValue.size > index) {
              val disciplin = evt.rowValue(index)
              if (disciplin.init.defaultVariables.isEmpty) {
                if (evt.newValue.toString == "NaN") {
                  disciplin.noteD.value = evt.newValue
                  disciplin.noteE.value = evt.newValue
                  disciplin.endnote.value = evt.newValue
                } else {
                  val updatedWertung = disciplin.init.validatedResult(
                    disciplin.toDouble(evt.newValue),
                    disciplin.toDouble(disciplin.noteE.value))
                  disciplin.noteD.set(updatedWertung.noteD.doubleValue)
                  disciplin.noteE.set(updatedWertung.noteE.doubleValue)
                  disciplin.endnote.set(updatedWertung.endnote.doubleValue)
                }
              }
              if (disciplin.isDirty) {
                service.updateWertung(disciplin.updateAndcommit)
              }
            }
            evt.tableView.requestFocus()
          }
        }
      lazy val clEnote = new WKTableColumn[Double](indexerE.next()) {
        val hasNoFormTemplate = wertungen.forall(we => if (index < we.size) we(index).init.wettkampfdisziplin.notenSpez.template.isEmpty else true)
          text = eNoteLabel
          cellValueFactory = { x => if (x.value.size > index) x.value(index).noteE else wertung.noteE }
          cellFactory.value = { _: Any =>
          if (hasNoFormTemplate) {
            new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], Double](
              DoubleConverter(wertung.init.wettkampfdisziplin.notenSpez),
              (cell) => {
                if (cell.tableRow.value != null && cell.tableRow.value.item.value != null && index < cell.tableRow.value.item.value.size) {
                  val w = cell.tableRow.value.item.value(index)
                  val editable = w.matchesSexAssignment
                  cell.editable = editable
                  cell.pseudoClassStateChanged(editableCssClass, cell.isEditable)
                }
              }
            )
          }else {
            new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], Double](
              DoubleConverter(wertung.init.wettkampfdisziplin.notenSpez),
              (cell) => {
                if (cell.tableRow.value != null && cell.tableRow.value.item.value != null && index < cell.tableRow.value.item.value.size) {
                  val w = cell.tableRow.value.item.value(index)
                  val editable = w.matchesSexAssignment
                  cell.editable = editable
                  cell.pseudoClassStateChanged(formularCssClass, cell.isEditable)
                }
              }, formEditorAction(index, we => we.noteE.map(_.toString()).getOrElse("")))
          }
        }

          styleClass += "table-cell-with-value"
          prefWidth = if (hasNoFormTemplate) 60 else 100
          editable = !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)

  onEditCancel = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
          if (evt.rowValue != null && evt.rowValue.size > index) {
            val disciplin = evt.rowValue(index)
            disciplin.reset
          }
        }

        onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], Double]) => {
            if (evt.rowValue != null && evt.rowValue.size > index) {
            val disciplin = evt.rowValue(index)
            if (disciplin.init.defaultVariables.isEmpty) {
              if (evt.newValue.toString == "NaN") {
                disciplin.noteD.value = evt.newValue
                disciplin.noteE.value = evt.newValue
                disciplin.endnote.value = evt.newValue
              } else {
                val result = disciplin.init.validatedResult(
                  disciplin.toDouble(disciplin.noteD.value),
                  disciplin.toDouble(evt.newValue)
                )
                disciplin.noteE.set(result.noteE.doubleValue)
                disciplin.endnote.set(result.endnote.doubleValue)
              }
            }
            if (disciplin.isDirty) {
                service.updateWertung(disciplin.updateAndcommit)
              }
            }
            evt.tableView.requestFocus()
          }
        }
        lazy val clEndnote = new WKTableColumn[Double](indexerF.next()) {
          text = "Endnote"
          cellValueFactory = { x => if (x.value.size > index) x.value(index).endnote else wertung.endnote }
          cellFactory.value = { _: Any => new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], Double](DoubleConverter(wertung.init.wettkampfdisziplin.notenSpez)) }
          styleClass += "table-cell-with-value"
          prefWidth = 80
          editable = false
        }
        val cl: jfxsc.TableColumn[IndexedSeq[WertungEditor], _] = if (withDNotes) {
          new TableColumn[IndexedSeq[WertungEditor], String] {
            text = wertung.init.wettkampfdisziplin.disziplin.name
            columns ++= Seq(clDnote, clEnote, clEndnote)
          }
        }
        else {
          // TODO Option, falls Mehrere Programme die gleichen Geräte benötigen,
          //  abhängig von subpath oder aggr. gerät mit Pfad qualifizieren
          clEnote.text = wertung.init.wettkampfdisziplin.disziplin.name
          clEnote
        }
        cl
      }
  }
  else {
    IndexedSeq[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]()
  }

  val athletCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = List(
    new WKTableColumn[String](-1) {
      text = "Athlet"
      cellValueFactory = { x =>
        new ReadOnlyStringWrapper(x.value, "athlet", {
          val a = x.value.head.init.athlet
          s"${a.vorname} ${a.name} ${
            (a.gebdat match {
              case Some(d) => f"$d%tY "
              case _ => " "
            })
          }"
        })
      }
      //        delegate.impl_setReorderable(false) // shame on me??? why this feature should not be a requirement?
      prefWidth = 150
      editable = false
    },
    new WKTableColumn[String](-1) {
      text = "Verein"
      cellValueFactory = { x =>
        new ReadOnlyStringWrapper(x.value, "verein", {
          val a = x.value.head.init.athlet
          s"${
            a.verein.map {
              _.name
            }.getOrElse("ohne Verein")
          }"
        })
      }
      //        delegate.impl_setReorderable(false)
      prefWidth = 100
      editable = false
    }
  )

  case object TeamItems {
    def apply(editor: WertungEditor): List[TeamItem] = this.apply(editor.init)

    def apply(editor: WertungView): List[TeamItem] = {
      val (teamname, vereinTeams) = editor.athlet.verein match {
        case Some(v) if (editor.wettkampf.teamrule.nonEmpty) =>
          if (editor.wettkampf.teamrule.exists(r => r.contains("VereinGe")))
            (s"${v.easyprint}", wkModel
              .filter(editorRow => editorRow(0).init.athlet.verein == editor.athlet.verein)
              .map(_.init(0).init.team)
              .filter(_ > 0).toSet.toList.sorted)
          else {
            val verband = editor.athlet.verein.flatMap(_.verband).getOrElse(v.easyprint)
            (s"${v.verband.getOrElse(v.extendedprint)}", wkModel
              .filter(editorRow => editorRow(0).init.athlet.verein.exists(_.verband.exists(_.equals(verband))))
              .map(_.init(0).init.team)
              .filter(_ > 0).toSet.toList.sorted)
          }
        case _ => (s"${editor.athlet.verein.getOrElse("")}", wkModel
          .filter(editorRow => editorRow(0).init.athlet.verein == editor.athlet.verein)
          .map(_.init(0).init.team)
          .filter(_ > 0).toSet.toList.sorted)
      }

      val nextVereinTeam = if (vereinTeams.isEmpty) 1 else vereinTeams.max + 1

      (1 to nextVereinTeam).toList.map(idx => TeamItem(idx, teamname)) :::
        editor.wettkampf.extraTeams.zipWithIndex.map(item => TeamItem(item._2 * -1 - 1, item._1))
    }

    def map(editor: WertungView): Option[TeamItem] = apply(editor).find(team => team.index == editor.team)

    def findSelectedTeamId(wertung: WertungView, selection: TeamItem): Option[Int] = {
      selection match {
        case TeamItem(0, name) if name.nonEmpty => apply(wertung)
          .find(team => team.machtesItemText(name))
          .map(_.index)
        case TeamItem(idx, _) if idx != 0 => Some(idx)
        case _ => None
      }
    }


    def tableCellSuggestProvider: (IndexedSeq[WertungEditor], String) => List[TeamItem] = (row: IndexedSeq[WertungEditor], userInput: String) => {
      if (userInput.isEmpty) {
        List.empty
      } else {
        val teams = TeamItems(row.head)
        val suggests: List[TeamItem] = if (isNumeric(userInput)) {
          val userIdx: Int = userInput
          teams.filter(t => t.index == userIdx)
        } else {
          val userText = userInput.toLowerCase
          teams.filter(team => team.name.toLowerCase.contains(userText))
        }
        if (suggests.nonEmpty) {
          suggests
        } else {
          teams
        }
      }
    }

    def stringConverter: StringConverter[TeamItem] = new StringConverter[TeamItem]() {
      override def fromString(string: String): TeamItem = {
        if (isNumeric(string)) {
          val userIdx: Int = string
          TeamItem(userIdx, string)
        } else {
          TeamItem(0, string)
        }
      }

      override def toString(t: TeamItem): String = t.itemText
    }
  }


  val riegeCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = if (!wettkampfInfo.isAggregated) {
    List(new WKTableColumn[String](-1) {
      text = "Riege"
      cellFactory.value = { _: Any =>
        new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
      }
      cellValueFactory = { x =>
        new ReadOnlyStringWrapper(x.value, "riege", {
          s"${x.value.head.init.riege.getOrElse("keine Einteilung")}"
        })
      }
      //        delegate.impl_setReorderable(false)
      editable <== when(Bindings.createBooleanBinding(() => {
        !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && !wettkampfmode.value
      },
        wettkampfmode
      )) choose true otherwise false
      visible <== when(wettkampfmode) choose false otherwise true
      prefWidth = 100

      onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
        if (!wettkampfmode.value) {
          val rowIndex = wkModel.indexOf(evt.rowValue)
          val newRiege = if (evt.newValue.trim.isEmpty() || evt.newValue.equals("keine Einteilung")) None
          else Some(evt.newValue)
          logger.debug("start riege-rename")
          service.updateAllWertungenAsync(
            evt.rowValue.map(wertung =>
              wertung.updateAndcommit.copy(riege = newRiege))).andThen {
            case Success(ws) => logger.debug("saved riege-rename")
              KuTuApp.invokeWithBusyIndicator {
                val selected = wkview.selectionModel.value.selectedCells
                refreshOtherLazyPanes()
                wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                updateRiegen()
                //updateEditorPane(Some(evt.tableView))
                logger.debug("finished riege-rename")
              }
            case Failure(e) => logger.error("not saved", e)
          }

          evt.tableView.selectionModel.value.select(rowIndex, this)
          evt.tableView.requestFocus()
        }
      }
    },
      new WKTableColumn[String](-1) {
        text = "Riege 2"
        cellFactory.value = { _: Any =>
          new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
        }

        cellValueFactory = { x =>
          new ReadOnlyStringWrapper(x.value, "riege2", {
            s"${x.value.head.init.riege2.getOrElse("keine Einteilung")}"
          })
        }
        editable <== when(Bindings.createBooleanBinding(() => {
          !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && !wettkampfmode.value
        },
          wettkampfmode
        )) choose true otherwise false
        visible <== when(wettkampfmode) choose false otherwise true
        prefWidth = 100

        onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
          if (!wettkampfmode.value) {
            val rowIndex = wkModel.indexOf(evt.rowValue)
            val newRiege = if (evt.newValue.trim.isEmpty || evt.newValue.equals("keine Einteilung")) None
            else Some(evt.newValue)
            logger.debug("start riege-rename")
            service.updateAllWertungenAsync(
              evt.rowValue.map(wertung =>
                wertung.updateAndcommit.copy(riege2 = newRiege))).andThen {
              case Success(ws) => logger.debug("saved riege-rename")
                KuTuApp.invokeWithBusyIndicator {
                  val selected = wkview.selectionModel.value.selectedCells
                  refreshOtherLazyPanes()
                  wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                  selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                  updateRiegen()
                  //updateEditorPane(Some(evt.tableView))
                  logger.debug("finished riege-rename")
                }
              case Failure(e) => logger.error("not saved", e)
            }

            evt.tableView.selectionModel.value.select(rowIndex, this)
            evt.tableView.requestFocus()
          }
        }
      },
      new WKTableColumn[TeamItem](-1) {

        text = "Team"
        cellFactory.value = { _: Any =>
          new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], TeamItem](TeamItems.stringConverter, TeamItems.tableCellSuggestProvider)
        }

        cellValueFactory = { x =>
          new ReadOnlyObjectProperty[TeamItem](x.value, "team", {
            val teams = TeamItems(x.value.head)
            teams.find(t => t.index == x.value.head.init.team).getOrElse(TeamItem(0, ""))
          })
        }
        editable <== when(Bindings.createBooleanBinding(() => {
          !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && !wettkampfmode.value
        },
          wettkampfmode
        )) choose true otherwise false

        visible = wettkampfInfo.teamRegel.teamsAllowed
        prefWidth = 100

        onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], TeamItem]) => {
          if (!wettkampfmode.value && evt.rowValue != null) {
            val rowIndex = wkModel.indexOf(evt.rowValue)
            val newTeam: Option[Int] = if (evt.rowValue.nonEmpty) TeamItems.findSelectedTeamId(evt.rowValue.head.init, evt.newValue) else None
            logger.debug("start team-reassignment")
            service.updateAllWertungenAsync(
              evt.rowValue.map(wertung => {
                val wertungView = wertung.init.copy(team = newTeam.getOrElse(0))
                wertungView.copy(
                  riege = Some(generateRiegenName(wertungView)),
                  riege2 = generateRiegen2Name(wertungView)).toWertung
              })).andThen {
              case Success(ws) => logger.debug("saved team-reassignment")
                KuTuApp.invokeWithBusyIndicator {
                  val selected = wkview.selectionModel.value.selectedCells
                  refreshOtherLazyPanes()
                  wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                  selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                  updateRiegen()
                  //updateEditorPane(Some(evt.tableView))
                  logger.debug("finished team-reassignment")
                }
              case Failure(e) => logger.error("not saved", e)
            }

            evt.tableView.selectionModel.value.select(rowIndex, this)
            evt.tableView.requestFocus()
          }
        }
      })
  } else {
    val cols: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = wettkampfInfo.groupHeadPrograms
      .filter { p =>
        programm match {
          case Some(pgm) if (pgm.programPath.contains(p)) => true
          case None => true
          case _ => false
        }
      }
      .map { p =>
        val col: jfxsc.TableColumn[IndexedSeq[WertungEditor], _] = new TableColumn[IndexedSeq[WertungEditor], String] {
          text = s"${p.name}"
          //            delegate.impl_setReorderable(false)
          columns ++= Seq(
            new WKTableColumn[String](-1) {
              text = "Riege"
              cellFactory.value = { _: Any =>
                new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
              }
              cellValueFactory = { x =>
                new ReadOnlyStringWrapper(x.value, "riege", {
                  s"${x.value.find(we => we.init.wettkampfdisziplin.programm.programPath.contains(p)).flatMap(we => we.init.riege).getOrElse("keine Einteilung")}"
                })
              }
              editable <== when(Bindings.createBooleanBinding(() => {
                !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && !wettkampfmode.value
              },
                wettkampfmode
              )) choose true otherwise false
              visible <== when(wettkampfmode) choose false otherwise true
              prefWidth = 100

              onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
                if (!wettkampfmode.value) {
                  val rowIndex = wkModel.indexOf(evt.rowValue)
                  val newRiege = if (evt.newValue.trim.isEmpty() || evt.newValue.equals("keine Einteilung")) None
                  else Some(evt.newValue)
                  logger.debug("start riege-rename")
                  service.updateAllWertungenAsync(
                    evt.rowValue.map(wertung =>
                      wertung.updateAndcommit.copy(riege = newRiege))).andThen {
                    case Success(ws) => logger.debug("saved riege-rename")
                      KuTuApp.invokeWithBusyIndicator {
                        val selected = wkview.selectionModel.value.selectedCells
                        refreshOtherLazyPanes()
                        wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                        selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                        updateRiegen()
                        //updateEditorPane(Some(evt.tableView))
                        logger.debug("finished riege-rename")
                      }
                    case Failure(e) => logger.error("not saved", e)
                  }

                  evt.tableView.selectionModel.value.select(rowIndex, this)
                  evt.tableView.requestFocus()
                }
              }
            },
            new WKTableColumn[String](-1) {
              text = "Riege 2"
              cellFactory.value = { _: Any =>
                new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], String](new DefaultStringConverter())
              }
              cellValueFactory = { x =>
                new ReadOnlyStringWrapper(x.value, "riege2", {
                  s"${x.value.find(we => we.init.wettkampfdisziplin.programm.programPath.contains(p)).flatMap(we => we.init.riege2).getOrElse("keine Einteilung")}"
                })
              }
              //        delegate.impl_setReorderable(false)
              editable <== when(Bindings.createBooleanBinding(() => {
                !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && !wettkampfmode.value
              },
                wettkampfmode
              )) choose true otherwise false
              visible <== when(wettkampfmode) choose false otherwise true
              prefWidth = 100

              onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
                if (!wettkampfmode.value) {
                  val rowIndex = wkModel.indexOf(evt.rowValue)
                  val newRiege = if (evt.newValue.trim.isEmpty() || evt.newValue.equals("keine Einteilung")) None
                  else Some(evt.newValue)
                  logger.debug("start riege-rename")
                  service.updateAllWertungenAsync(
                    evt.rowValue.map(wertung =>
                      wertung.updateAndcommit.copy(riege2 = newRiege))).andThen {
                    case Success(ws) => logger.debug("saved riege-rename")
                      KuTuApp.invokeWithBusyIndicator {
                        val selected = wkview.selectionModel.value.selectedCells
                        refreshOtherLazyPanes()
                        wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                        selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                        updateRiegen()
                        //updateEditorPane(Some(evt.tableView))
                        logger.debug("finished riege-rename")
                      }
                    case Failure(e) => logger.error("not saved", e)
                  }

                  evt.tableView.selectionModel.value.select(rowIndex, this)
                  evt.tableView.requestFocus()
                }
              }
            },
            new WKTableColumn[TeamItem](-1) {
              text = "Team"
              cellFactory.value = { _: Any =>
                new AutoCommitTextFieldTableCell[IndexedSeq[WertungEditor], TeamItem](TeamItems.stringConverter, TeamItems.tableCellSuggestProvider)
              }

              cellValueFactory = { x =>
                new ReadOnlyObjectProperty[TeamItem](x.value, "team", {
                  val teams = TeamItems(x.value.head)
                  teams.find(t => t.index == x.value.head.init.team).getOrElse(TeamItem(0, ""))
                })
              }
              editable <== when(Bindings.createBooleanBinding(() => {
                !wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin) && !wettkampfmode.value
              },
                wettkampfmode
              )) choose true otherwise false

              visible = wettkampfInfo.teamRegel.teamsAllowed
              prefWidth = 100

              onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], TeamItem]) => {
                if (!wettkampfmode.value && evt.rowValue != null) {
                  val rowIndex = wkModel.indexOf(evt.rowValue)
                  val newTeam: Option[Int] = if (evt.rowValue.nonEmpty) TeamItems.findSelectedTeamId(evt.rowValue.head.init, evt.newValue) else None
                  logger.debug("start team-reassignment")
                  service.updateAllWertungenAsync(
                    evt.rowValue.map(wertung => {
                      val wertungView = wertung.init.copy(team = newTeam.getOrElse(0))
                      wertungView.copy(
                        riege = Some(generateRiegenName(wertungView)),
                        riege2 = generateRiegen2Name(wertungView)).toWertung
                    })).andThen {
                    case Success(ws) => logger.debug("saved team-reassignment")
                      KuTuApp.invokeWithBusyIndicator {
                        val selected = wkview.selectionModel.value.selectedCells
                        refreshOtherLazyPanes()
                        wkModel.update(rowIndex, ws.map(w => WertungEditor(w)).toIndexedSeq)
                        selected.foreach(c => wkview.selectionModel.value.select(c.row, c.tableColumn.asInstanceOf[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]]))
                        updateRiegen()
                        //updateEditorPane(Some(evt.tableView))
                        logger.debug("finished team-reassignment")
                      }
                    case Failure(e) => logger.error("not saved", e)
                  }

                  evt.tableView.selectionModel.value.select(rowIndex, this)
                  evt.tableView.requestFocus()
                }
              }
            }
          )
        }
        col
      }
    cols
  }

  val sumCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor], _]] = List(
    new WKTableColumn[String](-1) {
      text = "Punkte"
      cellValueFactory = { x =>
        def sum = {
          x.value.flatMap(w => w.calculatedWertung.value.endnote).sum
        }

        val sumProperty = new StringProperty(f"$sum%3.3f")
        sumProperty <== createStringBinding(() => f"$sum%3.3f", x.value.map(_.calculatedWertung): _*)
        sumProperty
      }
      prefWidth = 100
      delegate.setReorderable(false)
      styleClass += "table-cell-with-value"
      editable = false
    })
  wkview.columns ++= athletCol ++ riegeCol ++ wertungenCols ++ sumCol
  var isFilterRefreshing = false

  wkModel.onChange { (seq1, seq2) =>
    import scalafx.collections.ObservableBuffer._
    if (!isFilterRefreshing) {
      def updateWertungen(index: Int): Unit = {
        val changed = wkModel.get(index)
        val idx = wertungen.indexWhere { x => x.head.init.athlet.id == changed.head.init.athlet.id }
        wertungen = wertungen.updated(idx, changed)
      }

      seq2.foreach {
        case Remove(position, removed) =>
        case Add(position, added) =>
          updateWertungen(position)
        case Reorder(start, end, permutation) =>
        case Update(from, to) =>
      }
    }
  }

  def updateFilteredList(newVal: String, newDurchgang: GeraeteRiege): Unit = {
    val wkListHadFocus = wkview.focused.value
    val selected = wkview.selectionModel.value.selectedCells
    val searchQuery = newVal.toUpperCase().split(" ")
    lastFilter = newVal
    durchgangFilter = newDurchgang
    isFilterRefreshing = true
    wkModel.clear()

    def restoreVisibility(col: TableColumn[_, _]): Unit = {
      col.sortable.value = true
      if (col.delegate.isInstanceOf[WKTCAccess]) {
        val tca = col.delegate.asInstanceOf[WKTCAccess]
        if (tca.getIndex > -1) {
          val v = scheduledGears.contains(disziplinlist(tca.getIndex))
          col.setVisible(v)
        }
      }
      col.columns.foreach(restoreVisibility(_))
    }

    def hideIfNotUsed(col: TableColumn[_, _]): Unit = {
      col.sortable.value = false
      if (col.delegate.isInstanceOf[WKTCAccess]) {
        val tca = col.delegate.asInstanceOf[WKTCAccess]
        if (tca.getIndex > -1) {
          val v = (tca.getIndex >= disziplinlist.size || scheduledGears.contains(disziplinlist(tca.getIndex))) && durchgangFilter.disziplin.isDefined && tca.getIndex == disziplinlist.indexOf(durchgangFilter.disziplin.get)
          col.setVisible(v)
        }
      }
      col.columns.foreach(hideIfNotUsed(_))
    }

    val orderedWertungen = if (durchgangFilter.softEquals(emptyRiege)) {
      wkview.columns.foreach {
        restoreVisibility(_)
      }
      wertungen
    }
    else {
      wkview.columns.foreach {
        hideIfNotUsed(_)
      }
      wertungen.sortBy { w => durchgangFilter.kandidaten.indexWhere { x => x.id == w.head.init.athlet.id } }
    }
    for {athlet <- orderedWertungen} {
      def isRiegenFilterConform(athletRiegen: Set[Option[String]]) = {
        val undefined = athletRiegen.forall { case None => true case _ => false }
        val durchgangKonform = durchgangFilter.softEquals(emptyRiege) ||
          durchgangFilter.kandidaten.filter { k => athletRiegen.contains(k.einteilung.map(_.r)) }.nonEmpty
        durchgangKonform && (undefined || !athletRiegen.forall { case Some(riege) => !relevantRiegen.getOrElse(riege, (false, 0))._1 case _ => true })
      }

      val matches = athlet.nonEmpty && isRiegenFilterConform(athlet.flatMap(a => Set(a.init.riege, a.init.riege2)).toSet) &&
        searchQuery.forall { search =>
          if (search.isEmpty() || athlet(0).init.athlet.name.toUpperCase().contains(search)) {
            true
          }
          else if (athlet(0).init.athlet.vorname.toUpperCase().contains(search)) {
            true
          }
          else if (athlet(0).init.athlet.verein match {
            case Some(v) => v.name.toUpperCase().contains(search)
            case None => false
          }) {
            true
          }
          else if (athlet(0).init.riege match {
            case Some(r) => r.toUpperCase().contains(search)
            case None => false
          }) {
            true
          }
          else if (athlet(0).init.riege2 match {
            case Some(r) => r.toUpperCase().contains(search)
            case None => false
          }) {
            true
          }
          else {
            false
          }
        }

      if (matches) {
        wkModel.add(athlet)
      }
    }
    wkview.sortOrder.clear()
    if (wkListHadFocus) {
      wkview.requestFocus()
      selected.foreach(s => wkview.selectionModel.value.selectedCells.add(s))
    }
    isFilterRefreshing = false
  }

  val txtUserFilter = new TextField() {
    promptText = "Athlet-Filter"
    text.addListener { (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
      if (!lastFilter.equalsIgnoreCase(newVal)) {
        updateFilteredList(newVal, durchgangFilter)
      }
    }
  }
  txtUserFilter.focused.onChange({
    if (!txtUserFilter.focused.value) {
      val focusSetter = AutoCommitTextFieldTableCell.selectFirstEditable(wkview)
      Platform.runLater(new Runnable() {
        override def run = {
          focusSetter()
        }
      })
    }
  })
  wkview.focused.onChange({
    if (wkview.focused.value) {
      txtUserFilter.promptText = "Athlet-Filter (CTRL+F)"
    }
    else {
      txtUserFilter.promptText = "Athlet-Filter"
    }
  })

  val teilnehmerCntLabel = new Label {
    margin = Insets(5, 0, 5, 5)
  }
  val alleRiegenCheckBox = new CheckBox {
    focusTraversable = false
    text = "Alle Riegen" + programm.map(" im " + _.name).getOrElse("")
    margin = Insets(5, 0, 5, 5)
  }

  def updateAlleRiegenCheck(toggle: Boolean = false): Unit = {
    val allselected = relevantRiegen.values.forall { x => x._1 }
    val newAllSelected = if (toggle) !allselected else allselected
    if (toggle) {
      relevantRiegen = relevantRiegen.map(r => (r._1, (newAllSelected, r._2._2)))
      updateFilteredList(lastFilter, durchgangFilter)
      updateRiegen()
    }
    else {
      alleRiegenCheckBox.selected.value = newAllSelected
    }
    val counttotriegen = riegenFilterModel.foldLeft(0)((sum, r) => sum + r.initanz)
    val counttotprogramm = wertungen.size
    val countsel = riegenFilterModel.filter(r => relevantRiegen(r.initname)._1).foldLeft(0)((sum, r) => sum + r.initviewanz)
    val rc = relevantRiegen.size
    teilnehmerCntLabel.text = s"$rc Riegen mit $counttotriegen Riegenmitglieder/-innen, $countsel von $counttotprogramm" + programm.map(" im " + _.name).getOrElse("")
  }

  alleRiegenCheckBox onAction = (event: ActionEvent) => {
    updateAlleRiegenCheck(true)
  }
  cmbDurchgangFilter.onAction = _ => {
    val d = if (!cmbDurchgangFilter.selectionModel.value.isEmpty) {
      cmbDurchgangFilter.selectionModel.value.getSelectedItem
    }
    else {
      emptyRiege
    }
    if (!durchgangFilter.softEquals(d)) {
      updateFilteredList(lastFilter, d)
    }
  }

  def updateRiegen(): Unit = {
    relevantRiegen = computeRelevantRiegen

    def onSelectedChange(name: String, selected: Boolean) = {
      if (relevantRiegen.contains(name)) {
        relevantRiegen = relevantRiegen.updated(name, (selected, relevantRiegen(name)._2))
        updateFilteredList(lastFilter, durchgangFilter)
        updateAlleRiegenCheck()
        selected
      }
      else {
        false
      }
    }

    riegenFilterModel.clear()
    riegen(onSelectedChange).foreach(riegenFilterModel.add(_))
  }

  def reloadData() = {
    val selectionstore = wkview.selectionModel.value.getSelectedCells.toList
    val coords = for (ts <- selectionstore) yield {
      if (ts.getColumn > -1 && ts.getTableColumn.getParentColumn != null)
        (ts.getRow, (3 + athletHeaderPane.index) * -100)
      else
        (ts.getRow, ts.getColumn)
    }

    val columnrebuild = wertungen.isEmpty
    isFilterRefreshing = true
    wertungen = reloadWertungen()
    if (columnrebuild && wertungen.nonEmpty) {
      wkview.columns.clear()
      riege match {
        case None =>
          wkview.columns ++= athletCol ++ riegeCol ++ wertungenCols ++ sumCol
        case _ =>
          wkview.columns ++= athletCol ++ wertungenCols ++ sumCol
      }
    }
    val lastDurchgangSelection = cmbDurchgangFilter.selectionModel.value.getSelectedItem
    if (riege.isEmpty) {
      cmbDurchgangFilter.items = ObservableBuffer.from(rebuildDurchgangFilterList)
      cmbDurchgangFilter.items.value.find(x => lastDurchgangSelection == null || x.softEquals(lastDurchgangSelection)) match {
        case Some(item) =>
          cmbDurchgangFilter.selectionModel.value.select(item)
          durchgangFilter = item;
        case None =>
      }
    } else {
      cmbDurchgangFilter.items = ObservableBuffer.from(rebuildDurchgangFilterList.filter(x => x.softEquals(riege.get)))
      cmbDurchgangFilter.items.value.headOption match {
        case Some(item) =>
          cmbDurchgangFilter.selectionModel.value.select(item)
          durchgangFilter = item;
        case None =>
      }
    }

    updateRiegen()
    lastFilter = ""
    updateFilteredList(lastFilter, durchgangFilter)
    txtUserFilter.text.value = lastFilter

    try {
      for (ts <- coords) {
        if (ts._2 < -100) {
          val toSelectParent = wkview.columns(ts._2 / -100)
          val firstVisible = toSelectParent.getColumns.find(p => p.width.value > 50d).getOrElse(toSelectParent.columns(0))
          wkview.selectionModel.value.select(ts._1, firstVisible)
          wkview.scrollToColumn(firstVisible)
        }
        else {
          wkview.selectionModel.value.select(ts._1, wkview.columns(ts._2))
          wkview.scrollToColumn(wkview.columns(ts._2))
        }
        wkview.scrollTo(ts._1)

      }
    }
    catch {
      case e: Exception =>
    }
    updateEditorPane(if (wkview.focused.value) Some(wkview) else None)
    isFilterRefreshing = false
  }

  def handleWertungUpdated(wertung: Wertung) = {
    AutoCommitTextFieldTableCell.doWhenEditmodeEnds(() => {
      val selectionstore = wkview.selectionModel.value.getSelectedCells.toList
      val columnIndex = selectionstore.map(tp => wkview.getColumns.indexOf(tp.getTableColumn))
      wertungen.foreach { aw =>
        aw.foreach { w =>
          if (w.init.id == wertung.id && w.commit.endnote != wertung.endnote) {
            w.update(w.init.updatedWertung(wertung))
          }
        }
      }

      selectionstore
        .foreach(c =>
          columnIndex.foreach { i =>
            if (i > -1 && wkview.columns.size > i) wkview.selectionModel.value.select(
              c.row,
              wkview.columns.get(i)
            )
          }
        )
    })
  }

  val editableProperty = new BooleanProperty()
  editableProperty <== wettkampfmode.not()
  val riegenFilterView = new RiegenFilterView(editableProperty,
    wettkampf, service,
    () => {
      disziplinlist
    },
    true,
    riegenFilterModel) {
  }

  riegenFilterView.addListener((editor: RiegeEditor) => {
    refreshLazyPane()
    reloadData()
  })


  def doLoadRanglisteFromCSV(progrm: ProgrammView)(implicit event: ActionEvent) = {
    import scala.concurrent.ExecutionContext.Implicits._
    import scala.util.{Failure, Success}
    val csvRangliste = new File(homedir + "/" + encodeFileName(wettkampf.easyprint) + s"/rangliste-${progrm.name}.csv".toLowerCase)
    val source = Source.fromFile(csvRangliste)(Codec.ISO8859)
    val vereineList = service.selectVereine
    val verbandPartsResolver: Verein=>Set[String] = _.verband match {
      case Some(verband) => verband.split(",").toSet + verband
      case None => Set()
    }
    val knownVerbandList = vereineList.flatMap(verbandPartsResolver).toSet
    val wkdiszs = service.listWettkampfDisziplineViews(wettkampf.toWettkampf)
    val lines = source.getLines().toList
    val fullDFields = "^([0-9.]+)\\s([\\D ]*)\\s([0-9]{4})\\s([\\D ]*)\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9}).*$".r
    val partialDFields = "^([0-9.]+)\\s([\\D ]*)\\s([0-9]{4})\\s([\\D ]*)\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9})\\s([0-9.*]{3,4})\\s([0-9.*]{5,9}).*$".r
    val singleDFields = "^([0-9.]+)\\s([\\D ]*)\\s([0-9]{4})\\s([\\D ]*)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+)\\s([0-9.*]+).*$".r

    def mapWertung(geraet: String, valueD: String, valueE: String) = {
      val dValue = valueD.split(",").map(BigDecimal(_)).sorted.reverse.head
      val wkds = wkdiszs
        .filter(_.programm.equals(progrm))
        .find(_.disziplin.name.equals(geraet))
      val wd: Long = wkds.map(_.id).getOrElse(0)
      val defaultvariables = wkds
        .flatMap(wd => wd.notenSpez.template.map(t => t.toView(t.variables)))

      Wertung(
        id = 0,
        athletId = 0,
        wettkampfdisziplinId = wd,
        wettkampfId = wettkampf.id,
        wettkampfUUID = wettkampf.uuid.getOrElse(""),
        noteD = Some(dValue),
        noteE = Some(BigDecimal(valueE) - dValue),
        endnote = Some(BigDecimal(valueE)),
        riege = None,
        riege2 = None,
        team = None,
        variables = defaultvariables
      )
    }

    def mapAthlet(name: String, jahrgang: String, verein: String) = {
      val guessedVerband = verein.split(" ").last
      val fallbackVerein = Verein(0, verein.replace(guessedVerband, "").replace("/", ", ").trim, Some(guessedVerband))
      val v = knownVerbandList
        .filter(verband => verein.contains(verband))
        .map { verband =>
          Verein(0, verein.replace(verband, "").replace("/", ", ").trim, Some(verband))
        }.headOption.getOrElse(fallbackVerein)
      val ns = name.split(" ")
      val a = new Athlet(
        id = 0,
        js_id = 0,
        geschlecht = "M",
        name = ns.last,
        vorname = ns.reverse.tail.reverse.mkString(" ").trim,
        gebdat = Some(service.getSQLDate("01.01." + jahrgang)),
        strasse = "",
        plz = "",
        ort = "",
        verein = service.findVereinLike(v, exact = true),
        activ = true
      )
      (v, a)
    }

    val rowfields = lines
      .map {
        case fullDFields(rang, name, jahrgang, verein, bodenD, bodenE, pferdD1, pferdD2, pferdE, ringeD, ringeE, sprungD1, sprungD2, sprungE, barrenD, barrenE, reckD, reckE, totalD, totalE) =>
          val (v: Verein, a: Athlet) = mapAthlet(name, jahrgang, verein)

          (a, v, List(
            mapWertung("Boden", bodenD, bodenE),
            mapWertung("Pferd Pauschen", pferdD1 + "," + pferdD2, pferdE),
            mapWertung("Ring", ringeD, ringeE),
            mapWertung("Sprung", sprungD1 + "," + sprungD2, sprungE),
            mapWertung("Barren", barrenD, barrenE),
            mapWertung("Reck", reckD, reckE)
          ))

        case partialDFields(rang, name, jahrgang, verein, bodenD, bodenE, pferdD1, pferdE, ringeD, ringeE, sprungD1, sprungD2, sprungE, barrenD, barrenE, reckD, reckE, totalD, totalE) =>
          val (v: Verein, a: Athlet) = mapAthlet(name, jahrgang, verein)

          (a, v, List(
            mapWertung("Boden", bodenD, bodenE),
            mapWertung("Pferd Pauschen", pferdD1, pferdE),
            mapWertung("Ring", ringeD, ringeE),
            mapWertung("Sprung", sprungD1 + "," + sprungD2, sprungE),
            mapWertung("Barren", barrenD, barrenE),
            mapWertung("Reck", reckD, reckE)
          ))

        case singleDFields(rang, name, jahrgang, verein, bodenD, bodenE, pferdD, pferdE, ringeD, ringeE, sprungD, sprungE, barrenD, barrenE, reckD, reckE, totalD, totalE) =>
          val (v: Verein, a: Athlet) = mapAthlet(name, jahrgang, verein)
          (a, v, List(
            mapWertung("Boden", bodenD, bodenE),
            mapWertung("Pferd Pauschen", pferdD, pferdE),
            mapWertung("Ring", ringeD, ringeE),
            mapWertung("Sprung", sprungD, sprungE),
            mapWertung("Barren", barrenD, barrenE),
            mapWertung("Reck", reckD, reckE)
          ))
      }

    val athletModel = ObservableBuffer[(Long, Athlet, AthletView, List[Wertung])]()
    val cache = new java.util.ArrayList[MatchCode]()
    val cliprawf = KuTuApp.invokeAsyncWithBusyIndicator("RanglisteDaten von CSV-Datei einlesen ...") {
      Future {

        val importvereine = rowfields

        val vereineList = service.selectVereine
        val vereineMap = vereineList.map(v => v.id -> v).toMap
        val athletSearchFn = service.findAthleteLike(wettkampf = None, cache = cache, exclusive = false, exactVerein = false)_
        val csvRaw = importvereine.map { row =>
          val (parsed, verein, wertungen) = row
          val candidate = athletSearchFn(parsed)
          val suggestion = AthletView(
            candidate.id, candidate.js_id,
            candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
            candidate.strasse, candidate.plz, candidate.ort,
            candidate.verein.filter(_ > 0L)
              .map(v => vereineMap(v))
              .orElse(service.findVereinLike(verein, exact = false)
                .filter(_ > 0L)
                .map(v => vereineMap(v))
                .orElse(Some(verein))),
            activ = true)
          (progrm.id, parsed, suggestion, wertungen)
        }
        csvRaw
      }
    }
    cliprawf.onComplete {
      case Failure(t) => println(t.toString)
      case Success(clipraw) => Platform.runLater {
        if (clipraw.nonEmpty) {
          athletModel.appendAll(clipraw)
          val filteredModel = ObservableBuffer.from(athletModel)
          val athletTable = new TableView[(Long, Athlet, AthletView, List[Wertung])](filteredModel) {
            columns ++= List(
              new TableColumn[(Long, Athlet, AthletView, List[Wertung]), String] {
                text = "Athlet (Name, Vorname, JG)"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "athlet", {
                    s"${x.value._2.shortPrint}"
                  })
                }
                minWidth = 250
              },
              new TableColumn[(Long, Athlet, AthletView, List[Wertung]), String] {
                text = "Verein"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "verein", {
                    s"${x.value._3.verein.get.extendedprint}"
                  })
                }
              },
              new TableColumn[(Long, Athlet, AthletView, List[Wertung]), String] {
                text = "Importvorschlag"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "vorschlag", {
                    if (x.value._3.id > 0) "existierend" else "neu importieren"
                  })
                }
              }
            )
          }
          athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)
          val filter = new TextField() {
            promptText = "Such-Text"
            text.addListener { (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
              val sortOrder = athletTable.sortOrder.toList
              filteredModel.clear()
              val searchQuery = newVal.toUpperCase().split(" ")
              for {(progrid, athlet, vorschlag, oldProgId) <- athletModel
                   } {
                val matches = searchQuery.forall { search =>
                  if (search.isEmpty() || athlet.name.toUpperCase().contains(search)) {
                    true
                  }
                  else if (athlet.vorname.toUpperCase().contains(search)) {
                    true
                  }
                  else if (vorschlag.verein match {
                    case Some(v) => v.name.toUpperCase().contains(search)
                    case None => false
                  }) {
                    true
                  }
                  else {
                    false
                  }
                }

                if (matches) {
                  filteredModel.add((progrid, athlet, vorschlag, oldProgId))
                }
              }
              athletTable.sortOrder.clear()
              val restored = athletTable.sortOrder ++= sortOrder
            }
          }
          PageDisplayer.showInDialog("Aus CSV laden ...", new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                minWidth = 600
                center = new BorderPane {
                  hgrow = Priority.Always
                  vgrow = Priority.Always
                  top = filter
                  center = athletTable
                  minWidth = 550
                }

              }
            }
          }, new Button("OK") {
            onAction = (event: ActionEvent) => {
              if (!athletTable.selectionModel().isEmpty) {
                val selected = athletTable.items.value.zipWithIndex.filter {
                  x => athletTable.selectionModel.value.isSelected(x._2)
                }.map(_._1)
                selected
                  .groupBy(_._3.verein)
                  .filter(_._1.nonEmpty)
                  .map { grp =>
                    val v = insertVereinIfMissing(grp._1.get)
                    (v.id, grp._2.map { c =>
                      val a = c._2.copy(verein = Some(v.id))
                      val av: AthletView = c._3.copy(verein = Some(v))
                      (c._1, a, av, c._4)
                    })
                  }
                  .foreach { grp =>
                    println(s"insert ${grp._2.size} Athletes of Verein ${grp._1}")
                    insertClipboardAssignments(grp._1, grp._2)
                  }
              }
            }
          }, new Button("OK Alle") {
            onAction = (event: ActionEvent) => {
              // insert to competition
              filteredModel
                .groupBy(_._3.verein)
                .filter(_._1.nonEmpty)
                .map { grp =>
                  val v = insertVereinIfMissing(grp._1.get)
                  (v.id, grp._2.map { c =>
                    val a = c._2.copy(verein = Some(v.id))
                    val av: AthletView = c._3.copy(verein = Some(v))
                    (c._1, a, av, c._4)
                  })
                }
                .foreach { grp =>
                  println(s"insert ${grp._2.size} Athletes of Verein ${grp._1}")
                  insertClipboardAssignments(grp._1, grp._2)
                }
            }
          })
        }
      }
    }
  }

  def doLoadFromCSV(filename: URI, progrm: Option[ProgrammView])(implicit event: ActionEvent) = {
    import scala.concurrent.ExecutionContext.Implicits._
    import scala.util.{Failure, Success}

    val programms = progrm.map(p => service.readWettkampfLeafs(p.head.id)).toSeq.flatten
    val source = Source.fromFile(filename)(Codec.ISO8859)
    val lines = source.getLines().toList
    val header = lines.head
    val rows = lines.tail
    val fieldnames = header.split(";").zipWithIndex.map(e => (e._2.trim, e._1.trim)).toMap
    val rowfields = rows
      .map(r => r.split(";").zipWithIndex.map(r => fieldnames(r._2.trim) -> r._1.replace("\"", "").trim).toMap)
    val normalizedPrograms = programms.map(p => p.name.replace("-", "").toUpperCase -> p).toMap

    val athletModel = ObservableBuffer[(Long, Athlet, AthletView, Long)]()
    val cache = new java.util.ArrayList[MatchCode]()
    val cliprawf = KuTuApp.invokeAsyncWithBusyIndicator("Daten von CSV-Datei einlesen ...") {
      Future {

        val importvereine = rowfields
          .map { fields =>
            val ver = fields("VEREIN").trim
            val verb = fields("VERBAND").trim
            val rlz = fields("RLZ_TZ").trim
            val rlzverb = fields("VERBAND_RLZ").trim

            val verein = List(ver, rlz).filter(_.nonEmpty).distinct.mkString(", ")
            val verband = List(verb, rlz, rlzverb).filter(_.nonEmpty).distinct.filter(v => !verein.contains(v)).mkString(", ")
            (fields, Verein(0, verein, Some(verband)))
          }
          .map { row =>
            val vereinId: Long = service.findVereinLike(row._2).getOrElse(0L)
            (row._1, row._2.copy(id = vereinId))
          }

        val vereineList = service.selectVereine
        val vereineMap = vereineList.map(v => v.id -> v).toMap

        val csvRaw = importvereine.map { row =>
          val (fields, verein) = row
          val parsed = new Athlet(
            id = 0,
            js_id = 0,
            geschlecht = "M",
            name = fields("NAME_TURNER"),
            vorname = fields("VORNAME_TURNER"),
            gebdat = Some(service.getSQLDate("01.01." + fields("JG_TURNER"))),
            strasse = "",
            plz = "",
            ort = "",
            verein = Some(verein.id),
            activ = true
          )
          val candidate = service.findAthleteLike(cache = cache, exclusive = false)(parsed)
          val progId: Long = normalizedPrograms(fields("WETTKAMPF_TEIL").trim.toUpperCase()).id
          val suggestion = AthletView(
            candidate.id, candidate.js_id,
            candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
            candidate.strasse, candidate.plz, candidate.ort,
            Some(verein), true)
          val oldProg: Long = wertungen.find(w => w.head.init.athlet.easyprint.equals(suggestion.easyprint)).map(w => w.head.init.wettkampfdisziplin.programm.id).getOrElse(0)
          (progId, parsed, suggestion, oldProg)
        }

        val toRemove = wertungen.map(_.head.init.athlet).distinct.filter(a => !csvRaw.exists(csv => csv._3.easyprint.equals(a.easyprint))).map { a =>
          (0L, a.toAthlet, a, 0L)
        }.toList
        csvRaw.filter(item => item._4 != item._1) ++ toRemove
      }
    }
    cliprawf.onComplete {
      case Failure(t) => println(t.toString)
      case Success(clipraw) => Platform.runLater {
        if (clipraw.nonEmpty) {
          athletModel.appendAll(clipraw)
          val programms = progrm.map(p => service.readWettkampfLeafs(p.head.id)).toSeq.flatten
          val filteredModel = ObservableBuffer.from(athletModel)
          val athletTable = new TableView[(Long, Athlet, AthletView, Long)](filteredModel) {
            columns ++= List(
              new TableColumn[(Long, Athlet, AthletView, Long), String] {
                text = "Athlet (Name, Vorname, JG)"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "athlet", {
                    s"${x.value._2.shortPrint}"
                  })
                }
                minWidth = 250
              },
              new TableColumn[(Long, Athlet, AthletView, Long), String] {
                text = progrm.map(p => if (p.head.name.toUpperCase.contains("GETU")) "Kategorie" else "Programm").getOrElse(".")
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "programm", {
                    programms.find { p => p.id == x.value._1 || p.aggregatorHead.id == x.value._1 } match {
                      case Some(programm) => if (x.value._4 > 0) "Umteilen auf " + programm.name else programm.name
                      case _ => "unbekannt"
                    }
                  })
                }
              },
              new TableColumn[(Long, Athlet, AthletView, Long), String] {
                text = "Import-Vorschlag"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "dbmatch", {
                    if (x.value._1 == 0L) "wird entfernt" else if (x.value._3.id > 0) "als " + x.value._3.easyprint else "wird neu importiert"
                  })
                }
              }
            )
          }
          athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)
          val filter = new TextField() {
            promptText = "Such-Text"
            text.addListener { (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
              val sortOrder = athletTable.sortOrder.toList
              filteredModel.clear()
              val searchQuery = newVal.toUpperCase().split(" ")
              for {(progrid, athlet, vorschlag, oldProgId) <- athletModel
                   } {
                val matches = searchQuery.forall { search =>
                  if (search.isEmpty() || athlet.name.toUpperCase().contains(search)) {
                    true
                  }
                  else if (athlet.vorname.toUpperCase().contains(search)) {
                    true
                  }
                  else if (vorschlag.verein match {
                    case Some(v) => v.name.toUpperCase().contains(search)
                    case None => false
                  }) {
                    true
                  }
                  else {
                    false
                  }
                }

                if (matches) {
                  filteredModel.add((progrid, athlet, vorschlag, oldProgId))
                }
              }
              athletTable.sortOrder.clear()
              val restored = athletTable.sortOrder ++= sortOrder
            }
          }
          PageDisplayer.showInDialog("Aus CSV laden ...", new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                minWidth = 600
                center = new BorderPane {
                  hgrow = Priority.Always
                  vgrow = Priority.Always
                  top = filter
                  center = athletTable
                  minWidth = 550
                }

              }
            }
          }, new Button("OK") {
            onAction = (event: ActionEvent) => {
              if (!athletTable.selectionModel().isEmpty) {
                val selected = athletTable.items.value.zipWithIndex.filter {
                  x => athletTable.selectionModel.value.isSelected(x._2)
                }.map(_._1)
                syncImportedAthletListWithCompetition(selected)
              }
            }
          }, new Button("OK Alle") {
            onAction = (event: ActionEvent) => {
              syncImportedAthletListWithCompetition(filteredModel)
            }
          })
        }
      }
    }
  }

  def doPasteFromExcel(progrm: Option[ProgrammView])(implicit event: ActionEvent) = {
    import scala.concurrent.ExecutionContext.Implicits._
    import scala.util.{Failure, Success}
    val athletModel = ObservableBuffer[(Long, Athlet, AthletView)]()
    val vereineList = service.selectVereine
    val vereineMap = vereineList.map(v => v.id -> v).toMap
    val vereine = ObservableBuffer.from(vereineList)
    val cbVereine = new ComboBox[Verein] {
      items = vereine
    }
    val programms = progrm.map(p => service.readWettkampfLeafs(p.head.id)).toSeq.flatten
    val clipboardlines = Source.fromString(Clipboard.systemClipboard.getString + "").getLines()
    val cache = new java.util.ArrayList[MatchCode]()
    val cliprawf = KuTuApp.invokeAsyncWithBusyIndicator("Daten von Excel Clipboard einlesen ...") {
      Future {
        clipboardlines.
          map { line => line.split("\\t").map(_.trim()) }.
          filter { fields => fields.length > 2 }.
          map { fields =>
            val parsed = Athlet(
              id = 0,
              js_id = "",
              geschlecht = if (!"".equals(fields(4))) "W" else "M",
              name = fields(0),
              vorname = fields(1),
              gebdat = if (fields(2).length > 4)
                Some(service.getSQLDate(fields(2)))
              else if (fields(2).length == 4) {
                Some(service.getSQLDate("01.01." + fields(2)))
              }
              else {
                None
              },
              strasse = "",
              plz = "",
              ort = "",
              verein = None,
              activ = true
            )
            val candidate = service.findAthleteLike(cache = cache, exclusive = false)(parsed)
            val progId: Long = try {
              programms(Integer.valueOf(fields(3)) - 1).id
            }
            catch {
              case d: Exception =>
                programms.find(pgm => pgm.name.equalsIgnoreCase(fields(3))) match {
                  case Some(p) => p.id
                  case _ => progrm match {
                    case Some(p) => p.id
                    case None => 0L
                  }
                }
            }
            (progId, parsed, AthletView(
              candidate.id, candidate.js_id,
              candidate.geschlecht, candidate.name, candidate.vorname, candidate.gebdat,
              candidate.strasse, candidate.plz, candidate.ort,
              candidate.verein.map(vereineMap), true))
          }.toList
      }
    }
    cliprawf.onComplete {
      case Failure(t) => logger.debug(t.toString)
      case Success(clipraw) => Platform.runLater {
        //        val clipraw = Await.result(cliprawf, Duration.Inf)
        if (clipraw.nonEmpty) {
          athletModel.appendAll(clipraw)
          clipraw.find(a => a._3.verein match {
            case Some(v) => true
            case _ => false
          }) match {
            case Some((id, athlet, candidate)) =>
              cbVereine.selectionModel.value.select(candidate.verein.get)
            case _ =>
          }
          val filteredModel = ObservableBuffer.from(athletModel)
          val athletTable = new TableView[(Long, Athlet, AthletView)](filteredModel) {
            columns ++= List(
              new TableColumn[(Long, Athlet, AthletView), String] {
                text = "Athlet (Name, Vorname, JG)"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "athlet", {
                    s"${x.value._2.shortPrint}"
                  })
                }
                minWidth = 250
              },
              new TableColumn[(Long, Athlet, AthletView), String] {
                text = programm.map(p => if (p.head.name.toUpperCase.contains("GETU")) "Kategorie" else "Programm").getOrElse(".")
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "programm", {
                    programms.find { p => p.id == x.value._1 || p.aggregatorHead.id == x.value._1 } match {
                      case Some(programm) => programm.name
                      case _ => "unbekannt"
                    }
                  })
                }
              },
              new TableColumn[(Long, Athlet, AthletView), String] {
                text = "Import-Vorschlag"
                cellValueFactory = { x =>
                  new ReadOnlyStringWrapper(x.value, "dbmatch", {
                    if (x.value._3.id > 0) "als " + x.value._3.easyprint else "wird neu importiert"
                  })
                }
              }
            )
          }
          athletTable.selectionModel.value.setSelectionMode(SelectionMode.Multiple)
          val filter = new TextField() {
            promptText = "Such-Text"
            text.addListener { (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) =>
              val sortOrder = athletTable.sortOrder.toList
              filteredModel.clear()
              val searchQuery = newVal.toUpperCase().split(" ")
              for {(progrid, athlet, vorschlag) <- athletModel
                   } {
                val matches = searchQuery.forall { search =>
                  if (search.isEmpty() || athlet.name.toUpperCase().contains(search)) {
                    true
                  }
                  else if (athlet.vorname.toUpperCase().contains(search)) {
                    true
                  }
                  else if (vorschlag.verein match {
                    case Some(v) => v.name.toUpperCase().contains(search)
                    case None => false
                  }) {
                    true
                  }
                  else {
                    false
                  }
                }

                if (matches) {
                  filteredModel.add((progrid, athlet, vorschlag))
                }
              }
              athletTable.sortOrder.clear()
              val restored = athletTable.sortOrder ++= sortOrder
            }
          }
          PageDisplayer.showInDialog("Aus Excel einfügen ...", new DisplayablePage() {
            def getPage: Node = {
              new BorderPane {
                hgrow = Priority.Always
                vgrow = Priority.Always
                minWidth = 600
                top = new HBox {
                  prefHeight = 50
                  alignment = Pos.BottomRight
                  hgrow = Priority.Always
                  children = Seq(new Label("Turner/-Innen aus dem Verein  "), cbVereine)
                }
                center = new BorderPane {
                  hgrow = Priority.Always
                  vgrow = Priority.Always
                  top = filter
                  center = athletTable
                  minWidth = 550
                }

              }
            }
          }, new Button("OK") {
            disable <== when(cbVereine.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
            onAction = (event: ActionEvent) => {
              if (!athletTable.selectionModel().isEmpty) {
                val selectedAthleten = athletTable.items.value.zipWithIndex.filter {
                  x => athletTable.selectionModel.value.isSelected(x._2)
                }.map(_._1)
                insertClipboardAssignments(cbVereine.selectionModel.value.selectedItem.value.id, selectedAthleten.map(x => (x._1, x._2, x._3, List())))
              }
            }
          }, new Button("OK Alle") {
            disable <== when(cbVereine.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
            onAction = (event: ActionEvent) => {
              insertClipboardAssignments(cbVereine.selectionModel.value.selectedItem.value.id, filteredModel.map(x => (x._1, x._2, x._3, List())))
            }
          })
        }
      }
    }
  }

  private def insertVereinIfMissing(verein: Verein) = {
    if (verein.id > 0L) {
      //println(s"verein updated: ${v}")
      verein
    }
    else {
      val vv = service.insertVerein(verein)
      println(s"verein inserted: ${vv}")
      vv
    }
  }

  private def insertImportedAthletListToCompetition(athletList: ObservableBuffer[(Long, Athlet, AthletView, Long)]): Unit = {
    athletList
      .filter(_._1 > 0L)
      .filter(_._4 == 0)
      .groupBy(_._3.verein)
      .filter(_._1.nonEmpty)
      .map { grp =>
        val v = insertVereinIfMissing(grp._1.get)
        (v.id, grp._2.map { c =>
          val a = c._2.copy(verein = Some(v.id))
          val av: AthletView = c._3.copy(verein = Some(v))
          (c._1, a, av)
        })
      }
      .foreach { grp =>
        println(s"insert ${grp._2.size} Athletes of Verein ${grp._1}")
        insertClipboardAssignments(grp._1, grp._2.map(x => (x._1, x._2, x._3, List())))
      }

  }

  private def syncImportedAthletListWithCompetition(athletList: ObservableBuffer[(Long, Athlet, AthletView, Long)]): Unit = {
    // insert (progId, parsed, suggestion, oldProg) to competition
    insertImportedAthletListToCompetition(athletList)
    // move to program
    athletList
      .filter(_._1 > 0L)
      .filter(_._4 > 0)
      .foreach { grp =>
        service.moveToProgram(wettkampf.id, grp._1, 0, grp._3)
      }
    // remove
    athletList
      .filter(_._1 == 0L)
      .foreach { grp =>
        val wertungenIds = wertungen.flatMap(row => row.filter(w => w.init.athlet.easyprint.equals(grp._3.easyprint))).map(_.init.id).toSet
        service.unassignAthletFromWettkampf(wertungenIds)
      }
  }

  private def insertClipboardAssignments(vereinId: Long, selectedAthleten: ObservableBuffer[(Long, Athlet, AthletView, List[Wertung])]): Unit = {
    val clip = selectedAthleten.map { x =>
      val (progrId, importathlet, candidateView, wertungen) = x
      val id = if (candidateView.id > 0 &&
        (importathlet.gebdat match {
          case Some(d) =>
            candidateView.gebdat match {
              case Some(cd) => f"${cd}%tF".endsWith("-01-01")
              case _ => true
            }
          case _ => false
        })) {
        val athlet = service.insertAthlete(Athlet(
          id = candidateView.id,
          js_id = candidateView.js_id,
          geschlecht = candidateView.geschlecht,
          name = candidateView.name,
          vorname = candidateView.vorname,
          gebdat = importathlet.gebdat,
          strasse = candidateView.strasse,
          plz = candidateView.plz,
          ort = candidateView.ort,
          verein = Some(vereinId),
          activ = true
        ))
        athlet.id
      }
      else if (candidateView.id > 0) {
        candidateView.id
      }
      else {
        val athlet = service.insertAthlete(Athlet(
          id = 0,
          js_id = candidateView.js_id,
          geschlecht = candidateView.geschlecht,
          name = candidateView.name,
          vorname = candidateView.vorname,
          gebdat = candidateView.gebdat,
          strasse = candidateView.strasse,
          plz = candidateView.plz,
          ort = candidateView.ort,
          verein = Some(vereinId),
          activ = true
        ))
        athlet.id
      }
      (progrId, id, wertungen.map{wertung =>
        wertung.copy(athletId=id)
      })
    }
    if (clip.nonEmpty) {
      for ((progId, athletes) <- clip.groupBy(_._1).map(x => (x._1, x._2.map(x => (x._2, x._3))))) {
        if (athletes.exists(_._2.isEmpty)) {
          service.assignAthletsToWettkampf(wettkampf.id, Set(progId), athletes.map(_._1).toSet, None)
        } else {
          athletes.flatMap(_._2).foreach { wertung =>
            service.updateOrinsertWertung(wertung)
          }
        }
      }
      reloadData()
    }
  }

  val generateTeilnehmerListe = new Button with KategorieTeilnehmerToHtmlRenderer {
    override val logger: Logger = WettkampfWertungTab.this.logger
    text = "Teilnehmerliste erstellen"
    minWidth = 75
    onAction = (event: ActionEvent) => {
      if (wkModel.nonEmpty) {
        val driver = wkModel.toSeq
        val programme = driver.flatten.map(x => x.init.wettkampfdisziplin.programm).foldLeft(Seq[ProgrammView]()) { (acc, pgm) =>
          if (!acc.exists { x => x.id == pgm.id }) {
            acc :+ pgm
          }
          else {
            acc
          }
        }
        logger.debug(programme.toString)

        val riegen = service.selectRiegen(wettkampf.id).map(r => r.r -> (r.start.map(_.name).getOrElse(""), r.durchgang.getOrElse(""))).toMap
        val seriendaten = for {
          programm <- programme

          athletwertungen <- driver.map(we => we.filter { x => x.init.wettkampfdisziplin.programm.id == programm.id })
          if (athletwertungen.nonEmpty)
        }
        yield {
          val einsatz = athletwertungen.head.init
          val team = TeamItems.map(einsatz).map(_.itemText).getOrElse("")
          val athlet = einsatz.athlet
          ch.seidel.kutu.renderer.Kandidat(
            einsatz.wettkampf.easyprint
            , athlet.geschlecht match { case "M" => "Turner" case _ => "Turnerin" }
            , einsatz.wettkampfdisziplin.programm.easyprint
            , athlet.id
            , athlet.name
            , athlet.vorname
            , AthletJahrgang(athlet.gebdat).jahrgang
            , athlet.verein match { case Some(v) => v.easyprint case _ => "" }
            , team
            , einsatz.riege.getOrElse("")
            , riegen.getOrElse(einsatz.riege.getOrElse(""), ("", ""))._2
            , riegen.getOrElse(einsatz.riege.getOrElse(""), ("", ""))._1
            , athletwertungen.filter { wertung =>
              if (wertung.init.wettkampfdisziplin.feminim == 0 && !wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else if (wertung.init.wettkampfdisziplin.masculin == 0 && wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else {
                true
              }
            }.map(_.init.wettkampfdisziplin.disziplin.easyprint)
          )
        }
        val filename = "Teilnehmerliste_" +
          encodeFileName(wettkampf.easyprint) +
          programm.map("_Programm_" + _.easyprint.replace(" ", "_")).getOrElse("") +
          riege.map("_Riege_" + _.caption.replace(" ", "_")).getOrElse("") + ".html"
        val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
        if (!dir.exists()) {
          dir.mkdirs()
        }
        val file = new java.io.File(dir.getPath + "/" + filename)

        def generate(lpp: Int) = toHTMLasKategorienListe(seriendaten, PrintUtil.locateLogoFile(dir), service.selectSimpleDurchgaenge(wettkampf.id)
          .map(d => (d, d.effectivePlanStart(wettkampf.datum.toLocalDate))))

        PrintUtil.printDialog(text.value, FilenameDefault(filename, dir), adjustLinesPerPage = false, generate, orientation = PageOrientation.Portrait)(event)
      }
    }
  }
  val generateVereinsTeilnehmerListe = new Button with KategorieTeilnehmerToHtmlRenderer {
    override val logger: Logger = WettkampfWertungTab.this.logger
    text = "Vereins-Teilnehmerliste erstellen"
    minWidth = 75
    onAction = (event: ActionEvent) => {
      if (wkModel.nonEmpty) {
        val driver = wkModel.toSeq
        val programme = driver.flatten.map(x => x.init.wettkampfdisziplin.programm).foldLeft(Seq[ProgrammView]()) { (acc, pgm) =>
          if (!acc.exists { x => x.id == pgm.id }) {
            acc :+ pgm
          }
          else {
            acc
          }
        }
        logger.debug(programme.toString)
        val riegen = service.selectRiegen(wettkampf.id).map(r => r.r -> (r.start.map(_.name).getOrElse(""), r.durchgang.getOrElse(""))).toMap
        val seriendaten = for {
          programm <- programme

          athletwertungen <- driver.map(we => we.filter { x => x.init.wettkampfdisziplin.programm.id == programm.id })
          if (athletwertungen.nonEmpty)
        }
        yield {
          val einsatz = athletwertungen.head.init
          val team = TeamItems.map(einsatz).map(_.itemText).getOrElse("")
          val athlet = einsatz.athlet
          ch.seidel.kutu.renderer.Kandidat(
            einsatz.wettkampf.easyprint
            , athlet.geschlecht match { case "M" => "Turner" case _ => "Turnerin" }
            , einsatz.wettkampfdisziplin.programm.easyprint
            , athlet.id
            , athlet.name
            , athlet.vorname
            , AthletJahrgang(athlet.gebdat).jahrgang
            , athlet.verein match { case Some(v) => v.easyprint case _ => "" }
            , team
            , einsatz.riege.getOrElse("")
            , riegen.getOrElse(einsatz.riege.getOrElse(""), ("", ""))._2
            , riegen.getOrElse(einsatz.riege.getOrElse(""), ("", ""))._1
            , athletwertungen.filter { wertung =>
              if (wertung.init.wettkampfdisziplin.feminim == 0 && !wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else if (wertung.init.wettkampfdisziplin.masculin == 0 && wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else {
                true
              }
            }.map(_.init.wettkampfdisziplin.disziplin.easyprint)
          )
        }
        val filename = "Vereins-Teilnehmerliste_" +
          encodeFileName(wettkampf.easyprint) +
          programm.map("_Programm_" + _.easyprint.replace(" ", "_")).getOrElse("") +
          riege.map("_Riege_" + _.caption.replace(" ", "_")).getOrElse("") + ".html"
        val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
        if (!dir.exists()) {
          dir.mkdirs()
        }

        def generate(lpp: Int) = toHTMLasVereinsListe(seriendaten, PrintUtil.locateLogoFile(dir), service.selectSimpleDurchgaenge(wettkampf.id)
          .map(d => (d, d.effectivePlanStart(wettkampf.datum.toLocalDate))))

        PrintUtil.printDialog(text.value, FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
      }
    }
  }
  val generateNotenblaetter = new Button with NotenblattToHtmlRenderer {
    override val logger: Logger = WettkampfWertungTab.this.logger
    text = "Notenblätter erstellen"
    minWidth = 75

    onAction = (event: ActionEvent) => {
      if (wkModel.nonEmpty) {
        val driver = wkModel.toSeq
        val programme = driver.flatten.map(x => x.init.wettkampfdisziplin.programm).foldLeft(Seq[ProgrammView]()) { (acc, pgm) =>
          if (!acc.exists { x => x.id == pgm.id }) {
            acc :+ pgm
          }
          else {
            acc
          }
        }
        logger.debug(programme.toString)
        val riegendurchgaenge = service.selectRiegen(wettkampf.id).map(r => r.r -> r).toMap
        val seriendaten = for {
          programm <- programme

          athletwertungen <- driver.map(we => we.filter { x => x.init.wettkampfdisziplin.programm.id == programm.id })
          if (athletwertungen.nonEmpty)
        }
        yield {
          val einsatz = athletwertungen.head.init
          val athlet = einsatz.athlet
          ch.seidel.kutu.domain.Kandidat(
            einsatz.wettkampf.easyprint
            , athlet.geschlecht match { case "M" => "Turner" case _ => "Turnerin" }
            , einsatz.wettkampfdisziplin.programm.easyprint
            , athlet.id
            , athlet.name
            , athlet.vorname
            , AthletJahrgang(athlet.gebdat).jahrgang
            , athlet.verein match { case Some(v) => v.easyprint case _ => "" }
            , riegendurchgaenge.get(einsatz.riege.getOrElse(""))
            , None
            , athletwertungen.filter { wertung =>
              if (wertung.init.wettkampfdisziplin.feminim == 0 && !wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else if (wertung.init.wettkampfdisziplin.masculin == 0 && wertung.init.athlet.geschlecht.equalsIgnoreCase("M")) {
                false
              }
              else {
                true
              }
            }.map(_.init.wettkampfdisziplin.disziplin)
            ,
            Seq.empty,
            athletwertungen.toSeq.map(_.view)
          )
        }
        val filename = "Notenblatt_" +
          encodeFileName(wettkampf.easyprint) +
          programm.map("_Programm_" + _.easyprint.replace(" ", "_")).getOrElse("") +
          riege.map("_Riege_" + _.caption.replace(" ", "_")).getOrElse("") + ".html"
        val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
        if (!dir.exists()) {
          dir.mkdirs()
        }
        val logofile = PrintUtil.locateLogoFile(dir)

        def generate(lpp: Int) = wettkampf.programm.head.id match {
          case 20 => toHTMLasGeTu(seriendaten, logofile)
          case n if (n == 11 || n == 31) => toHTMLasKuTu(seriendaten, logofile)
          case _ => toHTMLasATT(seriendaten, logofile)
        }

        PrintUtil.printDialog(text.value, FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Landscape)(event)
      }
    }
  }
  val generateBestenliste = new Button with BestenListeToHtmlRenderer {
    text = "Bestenliste erstellen"
    minWidth = 75
    disable.value = wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)
    onAction = (event: ActionEvent) => {
      if (!WebSocketClient.isConnected) {
        val filename = "Bestenliste_" + encodeFileName(wettkampf.easyprint) + ".html"
        val dir = new java.io.File(homedir + "/" + encodeFileName(wettkampf.easyprint))
        if (!dir.exists()) {
          dir.mkdirs()
        }
        val logofile = PrintUtil.locateLogoFile(dir)

        def generate(lpp: Int) = toHTMListe(WertungServiceBestenResult.getBestenResults, logofile)

        PrintUtil.printDialog(text.value, FilenameDefault(filename, dir), false, generate, orientation = PageOrientation.Portrait)(event)
      } else {
        Await.result(KuTuServer.finishDurchgangStep(wettkampf), Duration.Inf)
        val topResults = s"${Config.remoteBaseUrl}/?" + new String(enc.encodeToString((s"top&c=${wettkampf.uuid.get}").getBytes))
        KuTuApp.hostServices.showDocument(topResults)
      }
      WertungServiceBestenResult.resetBestenResults
    }
  }
  val riegenRemoveButton = new Button {
    text = "Riege löschen"
    minWidth = 75
    disable <== when(riegenFilterView.selectionModel.value.selectedItemProperty().isNull()) choose true otherwise false
    onAction = (event: ActionEvent) => {
      KuTuApp.invokeWithBusyIndicator {
        val selectedRiege = riegenFilterView.selectionModel.value.getSelectedItem.name.value
        for {
          wl <- reloadWertungen { wertung =>
            wertung.riege match {
              case Some(r) => r.equals(selectedRiege)
              case _ => false
            }
          }
          w <- wl
        } {
          service.updateWertung(w.updateAndcommit.copy(riege = None))
        }
        for {
          wl <- reloadWertungen { wertung =>
            wertung.riege2 match {
              case Some(r) => r.equals(selectedRiege)
              case _ => false
            }
          }
          w <- wl
        } {
          service.updateWertung(w.updateAndcommit.copy(riege2 = None))
        }
        reloadData()
      }
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
          }
        }
      })
    }
  }

  val removeButton = new Button {
    text = "Athlet entfernen"
    minWidth = 75
    onAction = (event: ActionEvent) => {
      if (!wkview.selectionModel().isEmpty) {
        val wertungEditor = wkview.selectionModel().getSelectedItem.head
        val athletwertungen = wkview.selectionModel().getSelectedItem.map(_.init.id).toSet
        implicit val impevent = event
        PageDisplayer.showInDialog(text.value, new DisplayablePage() {
          def getPage: Node = {
            new HBox {
              prefHeight = 50
              alignment = Pos.BottomRight
              hgrow = Priority.Always
              children = Seq(
                new Label(
                  s"Soll '${wertungEditor.init.athlet.easyprint}' wirklich aus der Einteilung im ${wertungEditor.init.wettkampfdisziplin.programm.name} entfernt werden?"))
            }
          }
        }, new Button("OK") {
          onAction = (event: ActionEvent) => {
            service.unassignAthletFromWettkampf(athletwertungen)
          }
        })
      }
    }
  }
  val moveToOtherProgramButton = new Button {
    val pgmpattern = ".*GETU.*/i".r
    text = programm.map(p => p.head.name match {
      case pgmpattern() => "Tu/Ti Kategorie wechseln ..."
      case _ => "Tu/Ti Programm wechseln ..."
    }).getOrElse(".")
    minWidth = 75
    onAction = (event: ActionEvent) => {
      implicit val impevent = event
      val athlet = wkview.selectionModel().getSelectedItem.head.init.athlet
      val alter = if (wettkampfInfo.isJGAlterklasse) {
        athlet.gebdat.map(d => Period.between(d.toLocalDate, wettkampf.datum).getYears).getOrElse(100)
      } else {
        athlet.gebdat.map(d => wettkampf.datum.toLocalDate.getYear - d.toLocalDate.getYear).getOrElse(100)
      }

      val programms = programm.toList.flatMap(p => service.readWettkampfLeafs(p.head.id)).filter(p => {
        Range.inclusive(p.alterVon, p.alterBis).contains(alter)
      })
      val prmodel = ObservableBuffer.from(programms)
      val cbProgramms = new ComboBox[ProgrammView] {
        items = prmodel
      }
      PageDisplayer.showInDialog(text.value, new DisplayablePage() {
        def getPage: Node = {
          new HBox {
            prefHeight = 50
            alignment = Pos.BottomRight
            hgrow = Priority.Always
            children = Seq(new Label("Neue Zuteilung  "), cbProgramms)
          }
        }
      }, new Button("OK") {
        onAction = (event: ActionEvent) => {
          if (!wkview.selectionModel().isEmpty) {
            val wertung = wkview.selectionModel().getSelectedItem.head.init
            service.moveToProgram(wettkampf.id, cbProgramms.selectionModel().selectedItem.value.id, wertung.team, wertung.athlet)
            //              reloadData()
          }
        }
      })
    }
  }
  val setRiege2ForAllButton = new Button {
    text = "2. Riege"
    tooltip = "2. Riegenzuteilung für alle in der Liste angezeigten Tu/Ti"
    minWidth = 75
    onAction = (event: ActionEvent) => {
      implicit val impevent = event
      val selectedRiege = wkModel
      val txtRiegenName = new TextField
      PageDisplayer.showInDialog(text.value, new DisplayablePage() {
        def getPage: Node = {
          new HBox {
            prefHeight = 50
            alignment = Pos.BottomRight
            hgrow = Priority.Always
            children = Seq(new Label("Neuer Riegenname für die zweite Riegenzuteilung "), txtRiegenName)
          }
        }
      }, new Button("OK") {
        onAction = (event: ActionEvent) => {
          KuTuApp.invokeWithBusyIndicator {
            val ws = selectedRiege.map(wl => wl.map(w => w.init.copy(riege2 = if (txtRiegenName.text.value.nonEmpty) Some(txtRiegenName.text.value) else None).toWertung))
            ws.foreach { x => x.foreach(service.updateWertungSimple(_)) }
            reloadData()
          }
        }
      })
    }
  }

  //addButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
  val moveAvaillable = programm.forall { p => p.head.id != 1L }
  moveToOtherProgramButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose moveAvaillable otherwise false
  setRiege2ForAllButton.disable <== when(Bindings.createBooleanBinding(() => {
    wkModel.isEmpty
  },
    wkModel
  )) choose true otherwise false
  removeButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

  val csvInputFile = new File(homedir + "/" + encodeFileName(wettkampf.easyprint) + "/excelinput.csv")

  val actionButtons = programm match {
    case None => wettkampf.programm.id match {
      case 1 => // Athletiktest
        val addButton = new Button {
          text = "Athlet hinzufügen"
          minWidth = 75
          onAction = (event: ActionEvent) => {
            new AthletSelectionDialog(
              text.value, wettkampfFilterDate, wettkampf.programm.alterVon, wettkampf.programm.alterBis, Set("W", "M"), wertungen.map(w => w.head.init.athlet), service,
              (selection: Set[Long]) => {
                service.assignAthletsToWettkampf(wettkampf.id, Set(2, 3), selection, None)
                reloadData()
              }
            ).execute(event)
          }
        }
        List[Button](addButton, removeButton, setRiege2ForAllButton, riegeRenameButton, riegenRemoveButton, generateTeilnehmerListe, generateVereinsTeilnehmerListe, generateNotenblaetter)

      case _ => // andere
        val pasteFromExcel = new Button("Aus Excel einfügen ...") {
          onAction = (event: ActionEvent) => {
            doPasteFromExcel(Some(wettkampf.programm))(event)
          }
        }
        val loadFromExcel = new Button("Aus CSV laden ...") {
          onAction = (event: ActionEvent) => {
            val uri = csvInputFile.toURI
            doLoadFromCSV(uri, Some(wettkampf.programm))(event)
          }
        }
        if (csvInputFile.exists()) {
          List[Button](loadFromExcel, removeButton, setRiege2ForAllButton, riegeRenameButton, riegenRemoveButton, generateTeilnehmerListe, generateVereinsTeilnehmerListe, generateNotenblaetter)
        } else {
          List[Button](pasteFromExcel, removeButton, setRiege2ForAllButton, riegeRenameButton, riegenRemoveButton, generateTeilnehmerListe, generateVereinsTeilnehmerListe, generateNotenblaetter)
        }
    }
    case Some(progrm) =>
      val addButton = new Button {
        text = "Athlet hinzufügen"
        minWidth = 75
        onAction = (event: ActionEvent) => {
          val wkdisziplinlist = service.listWettkampfDisziplines(wettkampf.id)
          val sex = wkdisziplinlist.flatMap { wkd =>
            (wkd.feminim, wkd.masculin) match {
              case (1, 0) => Set("W")
              case (0, 1) => Set("M")
              case _ => Set("M", "W")
            }
          }.toSet
          new AthletSelectionDialog(
            text.value, wettkampfFilterDate, progrm.alterVon, progrm.alterBis, sex, wertungen.map(w => w.head.init.athlet), service,
            (selection: Set[Long]) => {
              service.assignAthletsToWettkampf(wettkampf.id, Set(progrm.id), selection, None)
              reloadData()
            }
          ).execute(event)
        }
      }
      val pasteFromExcel = new Button("Aus Excel einfügen ...") {
        onAction = (event: ActionEvent) => {
          doPasteFromExcel(Some(progrm))(event)
        }
      }
      val loadFromExcel = new Button("Aus CSV laden ...") {
        onAction = (event: ActionEvent) => {
          val uri = csvInputFile.toURI
          doLoadFromCSV(uri, Some(wettkampf.programm))(event)
        }
      }
      val loadFromRangliste = new Button("Aus Rangliste laden ...") {
        onAction = (event: ActionEvent) => {
          doLoadRanglisteFromCSV(progrm)(event)
        }
      }

      if (csvInputFile.exists()) {
        List(addButton, loadFromExcel, loadFromRangliste, moveToOtherProgramButton, removeButton, setRiege2ForAllButton, riegenRemoveButton, generateTeilnehmerListe, generateNotenblaetter).filter(btn => !btn.text.value.equals("."))
      } else {
        List(addButton, pasteFromExcel, moveToOtherProgramButton, removeButton, setRiege2ForAllButton, riegenRemoveButton, generateTeilnehmerListe, generateNotenblaetter).filter(btn => !btn.text.value.equals("."))
      }
  }

  val clearButton = new Button {
    text = "Athlet zurücksetzen"
    minWidth = 75
    onAction = (event: ActionEvent) => {
      if (!wkview.selectionModel().isEmpty) {
        val selected = wkview.selectionModel().getSelectedItem
        implicit val impevent = event
        PageDisplayer.showInDialog(text.value, new DisplayablePage() {
          def getPage: Node = {
            new HBox {
              prefHeight = 50
              alignment = Pos.BottomRight
              hgrow = Priority.Always
              children = Seq(
                new Label(
                  s"Sollen wirklich die in diesem Wettkampf bereits erfassten Resultate für '${selected.head.init.athlet.easyprint}' zurückgesetzt werden?"))
            }
          }
        }, new Button("OK") {
          onAction = (event: ActionEvent) => {
            val rowIndex = wkModel.indexOf(selected)
            if (rowIndex > -1) {
              for (disciplin <- selected) {
                disciplin.clearInput()
                if (disciplin.isDirty) {
                  service.updateWertung(disciplin.updateAndcommit)
                }
              }
            }
          }
        })
      }
    }
  }
  clearButton.disable <== when(wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

  val clearAllButton = new Button {
    text = "Alle angezeigten Resultate zurücksetzen"
    minWidth = 75
    onAction = (event: ActionEvent) => {
      implicit val impevent = event
      PageDisplayer.showInDialog(text.value, new DisplayablePage() {
        def getPage: Node = {
          new HBox {
            prefHeight = 50
            alignment = Pos.BottomRight
            hgrow = Priority.Always
            children = Seq(
              new Label(
                s"Sollen wirklich alle angezeigten, in diesem Wettkampf erfassten Resultate zurückgesetzt werden?"))
          }
        }
      }, new Button("OK") {
        onAction = (event: ActionEvent) => {
          isFilterRefreshing = true
          try {
            for ((wertungen, rowIndex) <- wkModel.zipWithIndex) {
              for ((disciplin, index) <- wertungen.zipWithIndex) {
                if (durchgangFilter.sequenceId.equals(emptyRiege.sequenceId)
                  || durchgangFilter.disziplin.isEmpty
                  || index == disziplinlist.indexOf(durchgangFilter.disziplin.get)) {
                  disciplin.clearInput()
                  if (disciplin.isDirty) {
                    service.updateWertung(disciplin.updateAndcommit)
                  }
                }
              }
            }
          } finally {
            isFilterRefreshing = false
          }
          wkview.requestFocus()
        }
      })
    }
  }
  clearAllButton.disable <== when(Bindings.createBooleanBinding(() => {
    wkModel.isEmpty
  },
    wkModel
  )) choose true otherwise false
  wkview.selectionModel.value.setCellSelectionEnabled(true)
  wkview.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
    AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(wkview, true, txtUserFilter)(ke)
  }

  onSelectionChanged = _ => {
    if (selected.value) {
      reloadData()
    }
  }

  val editTablePane = new BorderPane {
    hgrow = Priority.Always
    vgrow = Priority.Always
    top = athletHeaderPane
    center = new StackPane {
      children += wkview
    }
  }

  val filterControl = new HBox {
    children += teilnehmerCntLabel
  }

  val riegenHeader = new VBox {
    focusTraversable = false
    maxWidth = Double.MaxValue
    minHeight = Region.USE_PREF_SIZE
    val title = new Label {
      text = "Riegen-Filter"
      styleClass += "toolbar-header"
    }
    children += title
    children += filterControl
    children += alleRiegenCheckBox
  }
  riegenFilterView.focusTraversable = false
  val riegenFilterPane = new BorderPane {
    focusTraversable = false
    hgrow = Priority.Always
    vgrow = Priority.Always
    prefWidth = 500
    margin = Insets(0, 0, 0, 10)
    top = riegenHeader
    center = new BorderPane {
      center = riegenFilterView
    }
    bottom = filterControl
  }

  override def isPopulated = {
    logger.debug("populate Wertungen Tab for " + programm)

    subscription match {
      case None =>
        subscription = Some(wettkampfmode.onChange {
          def op = {
            isPopulated
          }

          KuTuApp.invokeWithBusyIndicator(op)
        })
      // logger.debug("was not populated and subscription new accquired "+ programm+ wettkampf + hashCode())
      case _ =>
      // logger.debug("was populated and subscription active "+ programm+ wettkampf + hashCode())
    }

    websocketsubscription match {
      case None =>
        websocketsubscription = Some(WebSocketClient.modelWettkampfWertungChanged.onChange { (_, _, newItem) =>
          if (selected.value) {
            newItem match {
              case LastResults(results) =>
                reloadData()
              case a@AthletWertungUpdated(_, wertung, _, _, _, _) =>
                handleWertungUpdated(wertung)
              case a@AthletWertungUpdatedSequenced(_, wertung, _, _, _, _, _) =>
                handleWertungUpdated(wertung)

              case a@AthletMovedInWettkampf(athlet, wettkampfUUID, pgmId, team) =>
                reloadData()
              case a@AthletRemovedFromWettkampf(athlet, wettkampfUUID) =>
                reloadData()
              case _ =>
            }
          }
        })
        logger.debug("ws was not populated and subscription new accquired " + programm + wettkampf + hashCode())
      case _ =>
        logger.debug("ws was populated and subscription active " + programm + wettkampf + hashCode())
    }

    alleRiegenCheckBox.selected.value = true

    if (wettkampfmode.value) {
      riegenFilterView.selectionModel.value.setCellSelectionEnabled(false)
      riegenFilterView.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
      }
    } else {
      riegenFilterView.selectionModel.value.setCellSelectionEnabled(true)
      riegenFilterView.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
        AutoCommitTextFieldTableCell.handleDefaultEditingKeyEvents(riegenFilterView, false, txtUserFilter)(ke)
      }
    }

    val cont = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      top = new ToolBar {
        content = List(
          new Label {
            text = (programm match {
              case Some(progrm) =>
                s"Programm ${progrm.name}  "
              case None => ""
            }) + " " + (riege match {
              case Some(r) =>
                s"${r.caption}  "
              case None => ""
            }).trim
            maxWidth = Double.MaxValue
            minHeight = Region.USE_PREF_SIZE
            styleClass += "toolbar-header"
          }
        ) ++ (
          riege match {
            case None =>
              if (wettkampfmode.value || wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin))
                List(cmbDurchgangFilter, txtUserFilter, generateBestenliste) ++
                  (if (!wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)) List(removeButton) else List.empty)
              else
                actionButtons :+ clearButton :+ clearAllButton :+ cmbDurchgangFilter :+ txtUserFilter
            case _ =>
              List(txtUserFilter, generateBestenliste)
          }
          )
      }
      center = riege match {
        case None if (!wettkampfmode.value) =>
          new SplitPane {
            orientation = Orientation.Horizontal
            items += riegenFilterPane
            items += editTablePane

            setDividerPosition(0, 0.3d)

            SplitPane.setResizableWithParent(riegenFilterPane, false)
          }
        case _ => editTablePane
      }
    }

    content = cont
    logger.debug("Wertungen Tab for " + programm + " populated.")

    true
  }

  logger.debug("Wertungen Tab for " + programm + " created.")
}
