package ch.seidel.kutu.view

import ch.seidel.commons.*
import ch.seidel.kutu.domain.*
import scalafx.beans.property.BooleanProperty
import scalafx.geometry.*
import scalafx.scene.control.*
import scalafx.scene.layout.*


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
    if selected.value then {
    }
  }

  override def release: Unit = {
  }

  val editableProperty: BooleanProperty = new BooleanProperty()

  override def isPopulated: Boolean = {
    editableProperty.set(true)
    val scoreCalcTemplatesTab = new ScoreCalcTemplatesTab(wettkampfInfo.wettkampf, service) {
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
