package ch.seidel.commons

import javafx.scene.{ control => jfxsc }
import javafx.collections.{ObservableList, ListChangeListener}
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.util.converter.DefaultStringConverter
import scalafx.scene.layout.Region
import scalafx.scene.control.Label
import scalafx.scene.layout.VBox
import scalafx.scene.layout.BorderPane
import scalafx.beans.property.DoubleProperty
import scalafx.beans.property.StringProperty
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.{Tab, TabPane}
import scalafx.scene.layout.{Priority, StackPane}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{TableView, TableColumn}
import scalafx.scene.control.TableColumn._
import scalafx.beans.property.ReadOnlyStringWrapper
import ch.seidel.domain._
import scalafx.util.converter.DoubleStringConverter
import scalafx.beans.value.ObservableValue
import scalafx.scene.control.ToolBar
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.scene.Group
import scalafx.event.ActionEvent
import scalafx.scene.control.ScrollPane
import java.text.SimpleDateFormat
import scala.collection.mutable.StringBuilder

object WettkampfPage {

  case class WertungEditor(init: WertungView) {
    private var _noteD = init.noteD.toDouble
    def getNoteD = f"${_noteD}%2.3f"
    def setNoteD(v: String) { _noteD = v.toDouble }
    val noteD = new StringProperty(this, "noteD", getNoteD) {
      override def value = getNoteD
      override def value_=(v: String) {
        _noteD = v.toDouble
      }
    }

    var _noteE = init.noteE.toDouble
    def getNoteE = f"${_noteE}%2.3f"
    def setNoteE(v: String) { _noteE = v.toDouble }
    val noteE = new StringProperty(this, "noteE", getNoteE) {
      override def value = getNoteE
      override def value_=(v: String) {
        _noteE = v.toDouble
      }
    }

    var _endnote = init.endnote.toDouble
    def getEndnote = f"${_endnote}%2.3f"
    def setEndnote(v: String) { _endnote = v.toDouble }
    val endnote = new StringProperty(this, "endnote", getEndnote) {
      override def value = getEndnote
      override def value_=(v: String) {
        _endnote = v.toDouble
      }
    }

    def isDirty = _noteD != init.noteD || _noteE != init.noteE || _endnote != init.endnote

    def reset {
      noteD.value = init.noteD.toString
      noteE.value = init.noteE.toString
      endnote.value = init.endnote.toString
    }

    def commit = Wertung(
        init.id, init.athlet.id, init.wettkampfdisziplin.id, init.wettkampf.id,
        scala.math.BigDecimal(_noteD),
        scala.math.BigDecimal(_noteE),
        scala.math.BigDecimal(_endnote))
  }
  trait TabWithService {
    val service: KutuService
    lazy val populated = isPopulated
    def isPopulated: Boolean
  }

  class LazyLoadingTab(programm: ProgrammView, wettkampf: WettkampfView, override val service: KutuService, athleten: => IndexedSeq[WertungView]) extends Tab with TabWithService {
    override def isPopulated = {
      def wertungen = athleten.filter(wv => wv.wettkampfdisziplin.programm.id == programm.id).groupBy(wv => wv.athlet).map(wvg => wvg._2.map(WertungEditor)).toIndexedSeq
      val wkModel = ObservableBuffer[IndexedSeq[WertungEditor]](wertungen)

      val indexerE = Iterator.from(0)
      val indexerD = Iterator.from(0)
      val indexerF = Iterator.from(0)
      val wertungenCols = if(wertungen.nonEmpty) {
        wertungen.head.map{wertung =>
          val clDnote/*: jfxsc.TableColumn[IndexedSeq[WertungEditor],_]*/ = new TableColumn[IndexedSeq[WertungEditor], String] {
            val index = indexerD.next
            text = wertung.init.wettkampfdisziplin.disziplin.name
//            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", f"D: ${wertung.noteD}%2.3f E: ${wertung.noteE}%2.3f = ${wertung.endnote}%2.3f") }
            cellValueFactory = {x => if(x.value.size > index) x.value(index).noteD else wertung.noteD}
            cellFactory = {_ => new TextFieldTableCell[IndexedSeq[WertungEditor], String] (new DefaultStringConverter())}

            styleClass +=  "table-cell-with-value"
            prefWidth = 60
            editable = true
            onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
              val disciplin = evt.rowValue(index)
         		  disciplin.noteD.value = evt.newValue
              val rowIndex = wkModel.indexOf(evt.rowValue)
              if(disciplin.isDirty) {
                wkModel.update(rowIndex, evt.rowValue.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
              }
              evt.tableView.requestFocus()
            }
          }
          val clEnote/*: jfxsc.TableColumn[IndexedSeq[WertungEditor],_]*/ = new TableColumn[IndexedSeq[WertungEditor], String] {
            val index = indexerE.next
            text = wertung.init.wettkampfdisziplin.disziplin.name
//            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", f"D: ${wertung.noteD}%2.3f E: ${wertung.noteE}%2.3f = ${wertung.endnote}%2.3f") }
            cellValueFactory = {x => if(x.value.size > index) x.value(index).noteE else wertung.noteE}
            cellFactory = {x => new TextFieldTableCell[IndexedSeq[WertungEditor], String] (new DefaultStringConverter())}

            styleClass +=  "table-cell-with-value"
            prefWidth = 60
            editable = true
            onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
              val disciplin = evt.rowValue(index)
              disciplin.noteE.value = evt.newValue
              val rowIndex = wkModel.indexOf(evt.rowValue)
              if(disciplin.isDirty) {
                wkModel.update(rowIndex, evt.rowValue.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
              }
              evt.tableView.requestFocus()
            }
          }
          val clEndnote = new TableColumn[IndexedSeq[WertungEditor], String] {
            val index = indexerF.next
            text = wertung.init.wettkampfdisziplin.disziplin.name
//            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", f"D: ${wertung.noteD}%2.3f E: ${wertung.noteE}%2.3f = ${wertung.endnote}%2.3f") }
            cellValueFactory = {x => if(x.value.size > index) x.value(index).endnote else wertung.endnote}
            cellFactory = {x => new TextFieldTableCell[IndexedSeq[WertungEditor], String] (new DefaultStringConverter())}

            styleClass +=  "table-cell-with-value"
            prefWidth = 80
            editable = true
            onEditCommit = (evt: CellEditEvent[IndexedSeq[WertungEditor], String]) => {
              val disciplin = evt.rowValue(index)
              disciplin.endnote.value = evt.newValue
              val rowIndex = wkModel.indexOf(evt.rowValue)
              if(disciplin.isDirty) {
                wkModel.update(rowIndex, evt.rowValue.updated(index, WertungEditor(service.updateWertung(disciplin.commit))))
              }
              evt.tableView.requestFocus()
            }
          }
          val cl: jfxsc.TableColumn[IndexedSeq[WertungEditor],_] = new TableColumn[IndexedSeq[WertungEditor], String] {
            text = clEndnote.text.value
            clDnote.text = "D"
            clEnote.text = "E"
            clEndnote.text = "Endnote"
            prefWidth = 200
            columns ++= Seq(
                clDnote,
                clEnote,
                clEndnote
                )
          }
          cl
        }
      }
      else {
        IndexedSeq[jfxsc.TableColumn[IndexedSeq[WertungEditor],_]]()
      }

      val athletCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor],_]] = List(
          new TableColumn[IndexedSeq[WertungEditor], String] {
            text = "Athlet"
            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", {
                val a = x.value.head.init.athlet
                s"${a.vorname} ${a.name}"
              })
            }
            prefWidth = 150
          },
          new TableColumn[IndexedSeq[WertungEditor], String] {
            text = "Verein"
            cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "verein", {
                val a = x.value.head.init.athlet
                s"${a.verein.map { _.name }.getOrElse("ohne Verein")}"
              })
            }
            prefWidth = 100
          })

      val sumCol: List[jfxsc.TableColumn[IndexedSeq[WertungEditor],_]] = List(
          new TableColumn[IndexedSeq[WertungEditor], String] {
            text = "Punkte"
//              cellValueFactory = {x => new ReadOnlyFloatWrapper(x.value, "punkte", { x.value.map(w => w.endnote).sum.toFloat})}
            cellValueFactory = {x =>
              val w = new ReadOnlyStringWrapper(x.value, "punkte", {f"${x.value.map(w => w._endnote).sum}%3.3f"})

              w
            }
            prefWidth = 80
            styleClass +=  "table-cell-with-value"
          })

      val wkview = new TableView[IndexedSeq[WertungEditor]](wkModel) {
        columns ++= athletCol ++ wertungenCols ++ sumCol
        id = "kutu-table"
        editable = true
      }

      val addButton = new Button {
              text = "Athlet hinzufügen"
              minWidth = 75
              onAction = (event: ActionEvent) => {
                disable = true
                val athletModel = ObservableBuffer[AthletView](
                  service.selectAthletesView.filter{p => wertungen.forall { wp => wp.head.init.athlet.id != p.id}}
                )
                val athletTable = new TableView[AthletView](athletModel) {
                  columns ++= List(
                    new TableColumn[AthletView, String] {
                      text = "Athlet"
                      cellValueFactory = {x => new ReadOnlyStringWrapper(x.value, "athlet", {
                        s"${x.value.vorname} ${x.value.name} (${x.value.verein.map { _.name }.getOrElse("ohne Verein")})"})}
                      //prefWidth = 150
                    }
                  )
                }
                implicit val impevent = event
                PageDisplayer.showInDialog(text.value, new DisplayablePage() {
                  def getPage: Node = {
                    new BorderPane {
                      hgrow = Priority.ALWAYS
                      vgrow = Priority.ALWAYS
                      center = athletTable
                    }
                  }
                }, new Button("OK") {
                  onAction = (event: ActionEvent) => {
                    if(!athletTable.selectionModel().isEmpty) {
                      val athlet = athletTable.selectionModel().getSelectedItem
                      def filter(progId: Long, a: Athlet): Boolean = a.id == athlet.id
                      service.assignAthletsToWettkampf(wettkampf.id, Set(programm.id), Some(filter))
                      wkModel.clear
                      wkModel.appendAll(wertungen)
                    }
                  }
                })
                disable = false
              }
            }
      val removeButton = new Button {
              text = "Athlet entfernen"
              minWidth = 75
              onAction = (event: ActionEvent) => {
                    if(!wkview.selectionModel().isEmpty) {
                      val athletwertungen = wkview.selectionModel().getSelectedItem.map(_.init.id).toSet
                      service.unassignAthletFromWettkampf(athletwertungen)
                      wkModel.remove(wkview.selectionModel().getSelectedIndex)
                    }
                  }
            }
      val clearButton = new Button {
              text = "Athlet zurücksetzen"
              minWidth = 75
              onAction = (event: ActionEvent) => {
                    if(!wkview.selectionModel().isEmpty) {
                      val selected = wkview.selectionModel().getSelectedItem
                      for(disciplin <- selected) {
                        disciplin.noteD.value = "0"
                        disciplin.noteE.value = "0"
                        disciplin.endnote.value = "0"
                        val rowIndex = wkModel.indexOf(selected)
                        if(disciplin.isDirty) {
                          wkModel.update(rowIndex, selected.updated(selected.indexOf(disciplin), WertungEditor(service.updateWertung(disciplin.commit))))
                        }
                      }
                    }
                  }
            }
      //addButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
      removeButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false
      clearButton.disable <== when (wkview.selectionModel.value.selectedItemProperty.isNull()) choose true otherwise false

      content = new BorderPane {
        hgrow = Priority.ALWAYS
        vgrow = Priority.ALWAYS
        center = wkview
        top = new ToolBar {
          content = List(
            new Label {
              text = s"Programm ${programm.name}"
              maxWidth = Double.MaxValue
              minHeight = Region.USE_PREF_SIZE
              styleClass += "toolbar-header"
            },
            addButton, removeButton, clearButton
          )
        }
      }
      /*content =new StackPane {
      alignmentInParent = Pos.TOP_LEFT*/

      true
    }
  }

  class RanglisteTab(wettkampf: WettkampfView, override val service: KutuService) extends Tab with TabWithService {
        /*
         * combo 1. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         * combo 2. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         * combo 3. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         * combo 4. Gruppierung [leer, Programm, Jahrgang, Disziplin, Verein]
         */

//    val daten = service.selectWertungen().groupBy { x =>
//      x.wettkampf }.map(x => (x._1, x._2.groupBy { x =>
//        x.wettkampfdisziplin.programm }.map(x => (x._1, x._2.groupBy { x =>
//          x.athlet }))))
        sealed trait GroupSection {
          val groupKey: DataObject
          val sum: Resultat
          def aggregate: GroupSum = GroupSum(groupKey, sum)
          def easyprint: String
        }
        case class GroupSum(override val groupKey: DataObject, wertung: Resultat) extends GroupSection {
          override val sum: Resultat = wertung
          override def easyprint = groupKey.easyprint + " " + sum.easyprint
        }
        case class GroupLeaf(override val groupKey: DataObject, list: Iterable[WertungView]) extends GroupSection {
          override val sum: Resultat = list.map(_.resultat).reduce((r1, r2) => r1 + r2)
          override def easyprint = {
            val buffer = new StringBuilder()
            buffer.append(groupKey.easyprint).append("\n")
            val ds = list.map(_.wettkampfdisziplin.disziplin).toSet[Disziplin].toList.sortBy { d => d.id }
            buffer.append(f"${"Disziplin"}%40s")
            for(w <- ds) {
              buffer.append(f" ${w.easyprint}%18s")
            }
            buffer.append("\n")
            buffer.append(f"${"Athlet"}%40s")
            val legendd = "D"
            val legenda = "E"
            val legende = "End"
            val legend = f"${legendd}%6s${legenda}%6s${legende}%6s"
            for(w <- ds) {
              buffer.append(f" ${legend}%18s")
            }
            buffer.append("\n")
//            val extractor = groupKey match {
//              case p@Programm => (w: WertungView) => w.wettkampfdisziplin.programm
//              case p@Disziplin =>
//              case p@Verein =>
//              case p@AthletJahrgang =>
//              case _ =>
//            }
            for(wv <- list.groupBy { x => x.athlet }.map{x => (x._1, x._2, x._2.map(w => w.endnote).sum)}.toList.sortBy(_._3).reverse) {
              val (athlet, wertungen, sum) = wv
              buffer.append(f"${athlet.easyprint}%40s")
              for(w <- wertungen.toList.sortBy { x => x.wettkampfdisziplin.disziplin.id }) {
                buffer.append(f" ${w.easyprint}%18s")
              }
              buffer.append("\n")
            }
            buffer.toString()
          }
        }

        case class GroupNode(override val groupKey: DataObject, next: Iterable[GroupSection]) extends GroupSection {
          override val sum: Resultat = next.map(_.sum).reduce((r1, r2) => r1 + r2)
          override def easyprint = {
            val buffer = new StringBuilder()
            buffer.append(groupKey.easyprint).append("\n")
            for(gi <- next) {
              buffer.append(gi.easyprint).append("\n")
            }
            buffer.toString
          }
        }

        sealed trait GroupBy {
          private var next: Option[GroupBy] = None
          protected val grouper: (WertungView) => DataObject
          protected val sorter: Option[(GroupSection, GroupSection) => Boolean] = leafsorter
          protected val leafsorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.sum.endnote > gs2.sum.endnote
          })

          def groupBy(next: GroupBy): GroupBy = {
            this.next match {
              case Some(n) => n.groupBy(next)
              case None    => this.next = Some(next)
            }
            this
          }

          def select(wvlist: Seq[WertungView] = service.selectWertungen()): Iterable[GroupSection] = {
            val grouped = wvlist groupBy grouper
            next match {
              case Some(ng) => mapAndSortNode(ng, grouped)
              case None     => mapAndSortLeaf(grouped)
            }
          }

          private def mapAndSortLeaf(grouped: Map[DataObject, Seq[WertungView]]) = {
            def reduce(switch: DataObject, list: Seq[WertungView]):Seq[GroupSection] = {
              switch match {
                case p: ProgrammView if(p.aggregate > 0) =>
                  list.groupBy { x => x.athlet }.map{ x => GroupSum(x._1, x._2.map(y => y.resultat).reduce((r1, r2) => r1 + r2))}.toSeq
                case _ => list.toList match {
                  case head :: _ if(head.wettkampfdisziplin.programm.aggregate > 0) =>
                    Seq(GroupNode(switch, sort(list.groupBy { x => x.athlet }.map{ x => GroupSum(x._1, x._2.map(y => y.resultat).reduce((r1, r2) => r1 + r2))}.toSeq, leafsorter)))
                  case _ =>
                    Seq(GroupLeaf(switch, list))
                }
              }
            }
            sort(grouped.flatMap(x => reduce(x._1, x._2)), leafsorter)
          }

          private def mapAndSortNode(ng: GroupBy, grouped: Map[DataObject, Seq[WertungView]]) = {
            sort(grouped.map{x =>
              val (grp, seq) = x
              GroupNode(grp, ng.select(seq))
            }, sorter)
          }

          private def sort(mapped: Iterable[GroupSection], sorter: Option[(GroupSection, GroupSection) => Boolean]) = {
            sorter match {
              case Some(s) => mapped.toList.sortWith(s)
              case None    => mapped
            }
          }
        }

        case object ByProgramm extends GroupBy {
          protected override val grouper = (v: WertungView) => {
            v.wettkampfdisziplin.programm
          }
          protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.groupKey.asInstanceOf[ProgrammView].name.compareTo(gs2.groupKey.asInstanceOf[ProgrammView].name) < 0
          })
        }
        case object ByJahrgang extends GroupBy {
          private val extractYear = new SimpleDateFormat("YYYY")
          protected override val grouper = (v: WertungView) => {
            v.athlet.gebdat match {
              case Some(d) => AthletJahrgang(extractYear.format(d))
              case None    => AthletJahrgang("unbekannt")
            }
          }
          protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.groupKey.asInstanceOf[AthletJahrgang].hg.compareTo(gs2.groupKey.asInstanceOf[AthletJahrgang].hg) < 0
          })
        }
        case object ByDisziplin extends GroupBy {
          protected override val grouper = (v: WertungView) => {
            v.wettkampfdisziplin.disziplin
          }
          protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.groupKey.asInstanceOf[Disziplin].name.compareTo(gs2.groupKey.asInstanceOf[Disziplin].name) < 0
          })
        }
        case object ByVerein extends GroupBy {
          protected override val grouper = (v: WertungView) => {
            v.athlet.verein match {
              case Some(v) => v
              case _ => Verein(0, "kein")
            }
          }
          protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1:GroupSection , gs2:GroupSection ) => {
            gs1.groupKey.asInstanceOf[Verein].name.compareTo(gs2.groupKey.asInstanceOf[Verein].name) < 0
          })
        }

    override def isPopulated = {
        val combination = ByProgramm.groupBy(ByJahrgang).groupBy(ByVerein).select(
//        val combination = ByJahrgang.select(
//        val combination = ByDisziplin.select(
            service.selectWertungen().filter(p => p.wettkampf.id == wettkampf.id))

        for(c <- combination) {
          println(c.easyprint)
        }
      true
    }
  }

  class LazyTabPane(progSites: Seq[Tab]) extends TabPane {
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
    id = "source-tabs"
    tabs = progSites

    def init {
      progSites.foreach(_.asInstanceOf[TabWithService].populated)
    }
  }

  def buildTab(wettkampf: WettkampfView, service: KutuService) = {
    val progs = service.readWettkampfLeafs(wettkampf.programm.id)
    lazy val athleten = service.listAthletenWertungenZuProgramm(progs map (p => p.id))

    val progSites: Seq[Tab] = progs map {v =>
      new LazyLoadingTab(v, wettkampf, service, service.listAthletenWertungenZuProgramm(progs map (p => p.id))) {
        text = v.name
        closable = false
      }
    }
    val ranglisteSite: Seq[Tab] = Seq(
      new RanglisteTab(wettkampf, service) {
        text = "Rangliste"
        closable = false
      }
    )

    new WettkampfPage( new LazyTabPane(progSites ++ ranglisteSite))
  }
}

class WettkampfPage(tabPane: WettkampfPage.LazyTabPane)
  extends DisplayablePage {

  def getPage = {
    import WettkampfPage._

    tabPane.init
    tabPane
  }
}
