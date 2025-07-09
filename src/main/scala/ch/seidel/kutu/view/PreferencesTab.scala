package ch.seidel.kutu.view

import ch.seidel.commons.{AutoCommitTextFieldTableCell, DisplayablePage, LazyTabPane, PageDisplayer, TabWithService}
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

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.NANOS
import java.time.{Duration, LocalDateTime, LocalTime, ZoneOffset}
import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.duration.MILLISECONDS


class PreferencesTab(val wettkampfInfo: WettkampfInfo, override val service: KutuService) extends Tab with TabWithService {
  closable = false
  text = "Wettkampfparameter"

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

  // This handles also the initial load
  onSelectionChanged = _ => {
    if(selected.value) {
    }
  }

  override def release: Unit = {
  }

  val editableProperty: BooleanProperty = new BooleanProperty()

  override def isPopulated: Boolean = {
    editableProperty.set(true)
    val wettkampfEditable = !wettkampfInfo.wettkampf.toWettkampf.isReadonly(homedir, remoteHostOrigin)
    val scoreCalcTemplatesTab = new ScoreCalcTemplatesTab(wettkampfEditable, wettkampfInfo.wettkampf, service) {
      closable = false
      this.isPopulated
    }

    val rootpane = new BorderPane {
      hgrow = Priority.Always
      vgrow = Priority.Always
      margin = Insets(0, 0, 0, 10)
      center = new BorderPane {
        center = new TabPane {
          tabs += scoreCalcTemplatesTab
        }
      }
    }

    content = rootpane

    true
  }

}
