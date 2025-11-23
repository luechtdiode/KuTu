package ch.seidel.kutu.view

import javafx.scene.control as jfxsc
import scalafx.scene.control.*

trait TCAccess[R, E, IDX] {
  def getIndex: IDX

  def valueEditor(selectedRow: R): E
}

trait WKTCAccess extends TCAccess[IndexedSeq[WertungEditor], WertungEditor, Int] {
}

class WKJFSCTableColumn[T](val index: Int) extends jfxsc.TableColumn[IndexedSeq[WertungEditor], T] with WKTCAccess {
  override def getIndex: Int = index

  override def valueEditor(selectedRow: IndexedSeq[WertungEditor]): WertungEditor = selectedRow(index)
}

class WKTableColumn[T](val index: Int) extends TableColumn[IndexedSeq[WertungEditor], T] with WKTCAccess {
  override val delegate: jfxsc.TableColumn[IndexedSeq[WertungEditor], T] = new WKJFSCTableColumn[T](index)

  override def getIndex: Int = index

  override def valueEditor(selectedRow: IndexedSeq[WertungEditor]): WertungEditor = selectedRow(index)
}
