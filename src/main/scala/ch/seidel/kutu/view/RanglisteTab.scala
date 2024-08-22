package ch.seidel.kutu.view

import java.util.UUID
import ch.seidel.commons.{DisplayablePage, PageDisplayer}
import ch.seidel.kutu.Config._
import ch.seidel.kutu.ConnectionStates
import ch.seidel.kutu.KuTuApp.handleAction
import ch.seidel.kutu.data._
import ch.seidel.kutu.domain.{Altersklasse, Durchgang, KutuService, TeamRegel, WertungView, WettkampfView, encodeFileName, isNumeric}
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

  val altersklassen = Altersklasse.parseGrenzen(wettkampf.altersklassen)
  val jgAltersklassen = Altersklasse.parseGrenzen(wettkampf.jahrgangsklassen)

  def riegenZuDurchgang: Map[String, Durchgang] = {
    val riegen = service.listRiegenZuWettkampf(wettkampf.id)
    riegen.map(riege => riege._1 -> riege._3.map(durchgangName => Durchgang(0, durchgangName)).getOrElse(Durchgang())).toMap
  }

  override def groupers: List[FilterBy] = {
    val standardGroupers = List(ByNothing(),
      ByWettkampfProgramm(programmText), ByProgramm(programmText),
      ByJahrgang(),
      ByAltersklasse("DTB Altersklasse", Altersklasse.altersklassenDTB),
      ByAltersklasse("DTB Kür Altersklasse", Altersklasse.altersklassenDTBKuer),
      ByAltersklasse("DTB Pflicht Altersklasse", Altersklasse.altersklassenDTBPflicht),
      ByJahrgangsAltersklasse("Turn10® Altersklasse", Altersklasse.altersklassenTurn10),
      ByGeschlecht(),
      ByVerband(), ByVerein(),
      ByDurchgang(riegenZuDurchgang), ByRiege(), ByRiege2(), ByDisziplin())
    val akenhanced = (altersklassen.nonEmpty, jgAltersklassen.nonEmpty) match {
      case (true,true) => standardGroupers ++ List(ByAltersklasse("Wettkampf Altersklassen", altersklassen), ByJahrgangsAltersklasse("Wettkampf JG-Altersklassen", jgAltersklassen))
      case (false,true) => standardGroupers :+ ByJahrgangsAltersklasse("Wettkampf JG-Altersklassen", jgAltersklassen)
      case (true,false) => standardGroupers :+ ByAltersklasse("Wettkampf Altersklassen", altersklassen)
      case _ => standardGroupers
    }
    if (wettkampf.toWettkampf.hasTeams) {
      TeamRegel(wettkampf.toWettkampf).getTeamRegeln.map(r => ByTeamRule("Wettkampf Teamregel " + r.toRuleName, r)).toList ++ akenhanced
    } else {
      akenhanced
    }

  }

  override def getData: Seq[WertungView] = {
    val scheduledDisziplines = service.listScheduledDisziplinIdsZuWettkampf(wettkampf.id)
    service.selectWertungen(wettkampfId = Some(wettkampf.id))
      .filter(w => scheduledDisziplines.contains(w.wettkampfdisziplin.disziplin.id))
  }

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
    val akg = groupers.find(p => p.isInstanceOf[ByAltersklasse] && p.groupname.startsWith("Wettkampf"))
    val jakg = groupers.find(p => p.isInstanceOf[ByJahrgangsAltersklasse] && p.groupname.startsWith("Wettkampf"))
    val team = groupers.find(p => p.isInstanceOf[ByTeamRule] && p.groupname.startsWith("Wettkampf"))
    if (akg.nonEmpty) {
      combos(1).selectionModel.value.select(ByProgramm(programmText))
      combos(2).selectionModel.value.select(akg.get)
      combos(3).selectionModel.value.select(ByGeschlecht())
    } else if (jakg.nonEmpty) {
      combos(1).selectionModel.value.select(ByProgramm(programmText))
      combos(2).selectionModel.value.select(jakg.get)
      combos(3).selectionModel.value.select(ByGeschlecht())
    } else if (team.nonEmpty) {
      combos(1).selectionModel.value.select(team.get)
    } else {
      combos(1).selectionModel.value.select(ByProgramm(programmText))
      combos(2).selectionModel.value.select(ByGeschlecht())
    }

    true
  }

  override def getPublishedScores = {
    Await.result(service.listPublishedScores(UUID.fromString(wettkampf.uuid.get)), Duration.Inf)
  }

}
