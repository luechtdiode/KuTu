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

  class LazyLoadingTab(programm: ProgrammView, wettkampf: WettkampfView, service: KutuService, athleten: => IndexedSeq[WertungView]) extends Tab {
    lazy val populated = {
      val wertungen = athleten.filter(wv => wv.wettkampfdisziplin.programm.id == programm.id).groupBy(wv => wv.athlet).map(wvg => wvg._2.map(WertungEditor)).toIndexedSeq
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
            cellValueFactory = {x => x.value(index).noteD }
            cellFactory = {x => new TextFieldTableCell[IndexedSeq[WertungEditor], String] (new DefaultStringConverter())}

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
            cellValueFactory = {x => x.value(index).noteE}
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
            cellValueFactory = {x => x.value(index).endnote}
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

      content = new BorderPane {
        hgrow = Priority.ALWAYS
        vgrow = Priority.ALWAYS
        center = wkview
        top = new Label {
          text = s"Programm ${programm.name}"
          maxWidth = Double.MaxValue
          minHeight = Region.USE_PREF_SIZE
          styleClass += "page-subheader"
        }
      }
      /*content =new StackPane {
      alignmentInParent = Pos.TOP_LEFT*/

      true
    }
  }

  class LazyTabPane(progSites: Seq[LazyLoadingTab]) extends TabPane {
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
    id = "source-tabs"
    tabs = progSites :+
      new Tab() {
        text = "Rangliste"
        closable = false
      }

    def init {
      progSites.foreach(_.populated)
    }
  }

  def buildTab(wettkampf: WettkampfView, service: KutuService) = {
    val progs = service.readWettkampfLeafs(wettkampf.programm.id)
    lazy val athleten = service.listAthletenWertungenZuProgramm(progs map (p => p.id))

    val progSites = progs map {v =>
      new LazyLoadingTab(v, wettkampf, service, service.listAthletenWertungenZuProgramm(progs map (p => p.id))) {
        text = v.name
        closable = false
      }
    }
    new WettkampfPage( new LazyTabPane(progSites))
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
