package ch.seidel.kutu.view

import java.util.UUID

import ch.seidel.commons.{DisplayablePage, PageDisplayer}
import ch.seidel.kutu.Config._
import ch.seidel.kutu.ConnectionStates
import ch.seidel.kutu.KuTuApp.handleAction
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.{Durchgang, KutuService, WertungView, WettkampfView}
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import ch.seidel.kutu.view.NetworkTab.activeDurchgaengeProp
import scalafx.Includes.when
import scalafx.beans.binding.Bindings
import scalafx.event.ActionEvent
import scalafx.scene.Node
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.layout.{BorderPane, Priority, VBox}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RanglisteTab(wettkampf: WettkampfView, override val service: KutuService) extends DefaultRanglisteTab(service) {
  override val title = wettkampf.easyprint
  val programmText = wettkampf.programm.id match {
    case 20 => "Kategorie"
    case _ => "Programm"
  }

  def riegenZuDurchgang: Map[String, Durchgang] = {
    val riegen = service.listRiegenZuWettkampf(wettkampf.id)
    riegen.map(riege => riege._1 -> riege._3.map(riege => Durchgang(0, riege)).getOrElse(Durchgang())).toMap
  }

  override def groupers: List[FilterBy] = {
    List(ByNothing(), ByWettkampfProgramm(programmText), ByProgramm(programmText), ByJahrgang(), ByGeschlecht(), ByVerband(), ByVerein(), ByDurchgang(riegenZuDurchgang), ByRiege(), ByDisziplin())
  }

  override def getData: Seq[WertungView] = service.selectWertungen(wettkampfId = Some(wettkampf.id))

  override def getSaveAsFilenameDefault: FilenameDefault =
    FilenameDefault("Rangliste_" + wettkampf.easyprint.replace(" ", "_") + ".html", new java.io.File(homedir + "/" + wettkampf.easyprint.replace(" ", "_")))

  override def getActionButtons: List[Button] = List(new Button {
    text = "Publizieren ..."
    val p = wettkampf.toWettkampf

    disable <== when(Bindings.createBooleanBinding(() => {
      !p.hasSecred(homedir, remoteHostOrigin) ||
      !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse(""))
    }, ConnectionStates.connectedWithProperty
    )) choose true otherwise false

    onAction = handleAction { action: ActionEvent =>
      lastScoreDef.foreach { scoredef =>
        implicit val e = action

        val txtScoreName = new TextField {
          prefWidth = 500
          promptText = "Publizierter Ranglisten-Titel"
          text = normalizeFilterText(scoredef.toRestQuery)
        }

        PageDisplayer.showInDialog("Rangliste publizieren", new DisplayablePage() {
          def getPage: Node = {
            new BorderPane {
              hgrow = Priority.Always
              vgrow = Priority.Always
              center = new VBox {
                children.addAll(
                  new Label(txtScoreName.promptText.value), txtScoreName,
                )
              }
            }
          }
        }, new Button("OK") {
          disable <== when(Bindings.createBooleanBinding(() => {
            txtScoreName.text.isEmpty.getValue
          },
            txtScoreName.text
          )) choose true otherwise false
          onAction = handleAction { implicit e: ActionEvent =>
            service.savePublishedScore(wettkampf.id, txtScoreName.text.value, scoredef.toRestQuery, true)
          }
        })
      }
    }
  })

  override def isPopulated = {
    val combos = populate(groupers)

    combos(1).selectionModel.value.select(ByWettkampfProgramm(programmText))
    combos(2).selectionModel.value.select(ByGeschlecht())

    true
  }

  override def getPublishedScores = {
    Await.result(service.listPublishedScores(UUID.fromString(wettkampf.uuid.get)), Duration.Inf)
  }

}
