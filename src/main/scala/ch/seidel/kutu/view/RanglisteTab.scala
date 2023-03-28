package ch.seidel.kutu.view

import java.util.UUID
import ch.seidel.commons.{DisplayablePage, PageDisplayer}
import ch.seidel.kutu.Config._
import ch.seidel.kutu.ConnectionStates
import ch.seidel.kutu.KuTuApp.handleAction
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.{Durchgang, KutuService, WertungView, WettkampfView, encodeFileName}
import ch.seidel.kutu.renderer.PrintUtil.FilenameDefault
import scalafx.Includes.when
import scalafx.beans.binding.Bindings
import scalafx.beans.property.BooleanProperty
import scalafx.event.ActionEvent
import scalafx.scene.Node
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.layout.{BorderPane, Priority, VBox}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RanglisteTab(wettkampfmode: BooleanProperty, wettkampf: WettkampfView, override val service: KutuService) extends DefaultRanglisteTab(wettkampfmode, service) {
  override val title = wettkampf.easyprint
  val programmText = wettkampf.programm.id match {
    case 20 => "Kategorie"
    case _ => "Programm"
  }
  val altersklassen = Seq(
    6,7,8,9,10,11,12,13,14,15,16,17,18,24,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100
  )
  def riegenZuDurchgang: Map[String, Durchgang] = {
    val riegen = service.listRiegenZuWettkampf(wettkampf.id)
    riegen.map(riege => riege._1 -> riege._3.map(durchgangName => Durchgang(0, durchgangName)).getOrElse(Durchgang())).toMap
  }

  override def groupers: List[FilterBy] = {
    List(ByNothing(), ByWettkampfProgramm(programmText), ByProgramm(programmText), ByJahrgang(), ByAltersklasse(altersklassen), ByGeschlecht(), ByVerband(), ByVerein(), ByDurchgang(riegenZuDurchgang), ByRiege(), ByDisziplin())
  }

  override def getData: Seq[WertungView] = service.selectWertungen(wettkampfId = Some(wettkampf.id))

  override def getSaveAsFilenameDefault: FilenameDefault = {
    val foldername = encodeFileName(wettkampf.easyprint)
    FilenameDefault("Rangliste_" + foldername + ".html", new java.io.File(homedir + "/" + foldername))
  }

  val btnBereitstellen = new Button {
    text = "Bereitstellen ..."
    val p = wettkampf.toWettkampf
    visible <== when(wettkampfmode) choose false otherwise true

    disable <== when(Bindings.createBooleanBinding(() => {
      !p.hasSecred(homedir, remoteHostOrigin) ||
        !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")) ||
        !lastPublishedScoreView.getValue.isEmpty || wettkampfmode.getValue
    }, ConnectionStates.connectedWithProperty, lastPublishedScoreView, wettkampfmode
    )) choose true otherwise false

    onAction = handleAction { action: ActionEvent =>
      lastScoreDef.getValue.foreach { scoredef =>
        implicit val e = action

        val txtScoreName = new TextField {
          prefWidth = 500
          promptText = "Publizierter Ranglisten-Titel"
          text = normalizeFilterText(scoredef.toRestQuery)
        }

        PageDisplayer.showInDialog("Rangliste bereitstellen", new DisplayablePage() {
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
            lastPublishedScoreView.setValue(Some(
              service.savePublishedScore(wettkampf.id, txtScoreName.text.value, scoredef.toRestQuery, false, true)
            ))
          }
        })
      }
    }
  }
  val btnErneutBereitstellen = new Button {
    text = "Erneut bereitstellen ..."
    tooltip = "Die Publikation wird dadurch zurückgezogen, bis sie explizit wieder freigegeben wird."
    val p = wettkampf.toWettkampf
    visible <== when(wettkampfmode) choose false otherwise true
    disable <== when(Bindings.createBooleanBinding(() => {
      !p.hasSecred(homedir, remoteHostOrigin) ||
        !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")) ||
        lastPublishedScoreView.getValue.isEmpty || wettkampfmode.getValue ||
        lastPublishedScoreView.getValue.map(_.query) == lastScoreDef.getValue.map(_.toRestQuery)
    }, ConnectionStates.connectedWithProperty, lastPublishedScoreView, lastScoreDef, wettkampfmode

    )) choose true otherwise false

    onAction = handleAction { action: ActionEvent =>
      lastScoreDef.getValue.foreach { scoredef =>
        implicit val e = action

        val txtScoreName = new TextField {
          prefWidth = 500
          promptText = "Publizierter Ranglisten-Titel"
          text = normalizeFilterText(scoredef.toRestQuery)
        }

        PageDisplayer.showInDialog("Rangliste bereitstellen", new DisplayablePage() {
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
            lastPublishedScoreView.getValue.foreach { filter =>
              lastPublishedScoreView.setValue(Some(
                service.updatePublishedScore(wettkampf.id, filter.id, txtScoreName.text.value, scoredef.toRestQuery, false, true)
              ))
            }
          }
        })
      }
    }
  }
  val btnPublikationFreigeben = new Button {
    text = "Publikation freigeben ..."
    val p = wettkampf.toWettkampf

    disable <== when(Bindings.createBooleanBinding(() => {
      !p.hasSecred(homedir, remoteHostOrigin) ||
        !ConnectionStates.connectedWithProperty.value.equals(p.uuid.map(_.toString).getOrElse("")) ||
        lastPublishedScoreView.getValue.isEmpty || lastPublishedScoreView.getValue.get.published ||
        lastPublishedScoreView.getValue.map(_.query) != lastScoreDef.getValue.map(_.toRestQuery)

    }, ConnectionStates.connectedWithProperty, lastPublishedScoreView, lastScoreDef
    )) choose true otherwise false

    onAction = handleAction { implicit action: ActionEvent =>
      PageDisplayer.showInDialog(text.value, new DisplayablePage() {
        def getPage: Node = {
          new BorderPane {
            hgrow = Priority.Always
            vgrow = Priority.Always
            center = new VBox {
              children.addAll(new Label("Die freigegebene Publikation einer Rangliste kann nicht rückgängig gemacht werden!"))
            }
          }
        }
      },
        new Button("OK") {
          onAction = handleAction { implicit e: ActionEvent =>
            lastPublishedScoreView.getValue.foreach { filter =>
              lastPublishedScoreView.setValue(Some(
                service.updatePublishedScore(wettkampf.id, filter.id, filter.title, filter.query, true, true)
              ))
            }
          }
        }
      )
    }
  }

  override def getActionButtons: List[Button] =
    if (wettkampfmode.value) {
      List(
        btnPublikationFreigeben)
    } else {
      List(
        btnBereitstellen,
        btnErneutBereitstellen,
        btnPublikationFreigeben)
    }


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
