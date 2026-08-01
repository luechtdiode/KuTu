package ch.seidel.kutu.http

import ch.seidel.kutu.actors.{CompetitionCoordinatorClientActor, RefreshWettkampfMap}
import ch.seidel.kutu.calc.{ScoreCalcTemplate, ScoreCalcVariable}
import ch.seidel.kutu.calc.parser.{Expression, MathExpCompiler}
import ch.seidel.kutu.domain.*
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json.*

case class ScoreCalcPreviewRequest(wettkampfdisziplinId: Long, template: ScoreCalcTemplate, values: List[ScoreCalcVariable])

case class ScoreCalcPreviewResponse(
  exercises: List[List[ScoreCalcVariable]],
  noteD: Option[BigDecimal],
  noteE: Option[BigDecimal],
  endnote: Option[BigDecimal],
  dValidState: String,
  eValidState: String,
  pValidState: String,
  valid: Boolean)

trait ScoreCalcRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport with RouterLogging with KutuService with CIDSupport {

  import DefaultJsonProtocol.*

  lazy val scoreCalcRoutes: Route = {
    handleCID { (clientId: String) =>
      pathPrefixLabeled("admin" / "competition" / JavaUUID, "admin/competition/:uuid") { wkuuid =>
        pathPrefixLabeled("scorecalc", "scorecalc") {
          pathEnd {
            get {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  complete {
                    scoreCalcTemplatesOf(readWettkampf(wkuuid.toString)).toJson
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            } ~
            post {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  entity(as[ScoreCalcTemplate]) { template =>
                    val wettkampf = readWettkampf(wkuuid.toString)
                    val created = createScoreCalcTempate(template.copy(id = 0L, wettkampfId = Some(wettkampf.id)))
                    CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wkuuid.toString), clientId)
                    complete(created.toJson)
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathPrefixLabeled(LongNumber, ":id") { templateId =>
            put {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  entity(as[ScoreCalcTemplate]) { template =>
                    val wettkampf = readWettkampf(wkuuid.toString)
                    val saved = template.copy(id = templateId, wettkampfId = Some(wettkampf.id))
                    updateScoreCalcTemplate(saved)
                    CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wkuuid.toString), clientId)
                    complete(saved.toJson)
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            } ~
            delete {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  deleteScoreCalcTemplate(ScoreCalcTemplate(templateId, None, None, None, "", "", "", None))
                  CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wkuuid.toString), clientId)
                  complete(JsObject("status" -> JsString("ok")))
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathLabeled("options", "options") {
            get {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  complete(scoreCalcOptions(readWettkampf(wkuuid.toString)))
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          } ~
          pathLabeled("preview", "preview") {
            post {
              authenticatedAdmin() { userId =>
                if userId.equals(wkuuid.toString) then {
                  entity(as[ScoreCalcPreviewRequest]) { request =>
                    val wettkampf = readWettkampf(wkuuid.toString)
                    complete(previewScoreCalc(wettkampf, request).toJson)
                  }
                } else {
                  complete(StatusCodes.Conflict)
                }
              }
            }
          }
        }
      }
    }
  }

  private def scoreCalcTemplatesOf(wettkampf: Wettkampf): List[ScoreCalcTemplate] = {
    val wettkampfdisziplinViews = listWettkampfDisziplineViews(wettkampf)
    val disziplinList = wettkampfdisziplinViews.map(_.disziplin).distinct.sortBy(_.name)
    loadScoreCalcTemplatesAll(wettkampf.id)
      .filter { sct =>
        sct.wettkampfId.contains(wettkampf.id) ||
          sct.disziplinId.exists(did => disziplinList.exists(d => d.id == did)) ||
          sct.wettkampfdisziplinId.exists(wkdid => wettkampfdisziplinViews.exists(wkd => wkd.id == wkdid))
      }
      .sortBy(_.sortOrder)
  }

  private def scoreCalcOptions(wettkampf: Wettkampf): JsObject = {
    val wettkampfdisziplinViews = listWettkampfDisziplineViews(wettkampf)
    val disziplinen = wettkampfdisziplinViews.map(_.disziplin).distinct.sortBy(_.name)
    JsObject(
      "disziplinen" -> disziplinen.map(_.toJson).toJson,
      "wettkampfdisziplinen" -> wettkampfdisziplinViews
        .sortBy(wkd => s"${wkd.disziplin.name} ${wkd.programm.name}")
        .map { wkd =>
          JsObject(
            "id" -> JsNumber(wkd.id),
            "easyprint" -> JsString(wkd.easyprint),
            "disziplinId" -> JsNumber(wkd.disziplin.id),
            "disziplinName" -> JsString(wkd.disziplin.name),
            "isDNoteUsed" -> JsBoolean(wkd.isDNoteUsed),
            "dNoteLabel" -> JsString(wkd.notenSpez.getDifficultLabel),
            "eNoteLabel" -> JsString(wkd.notenSpez.getExecutionLabel)
          )
        }.toJson
    )
  }

  private def previewScoreCalc(wettkampf: Wettkampf, request: ScoreCalcPreviewRequest): ScoreCalcPreviewResponse = {
    val cache = scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]()
    val wkv = readWettkampfDisziplinView(wettkampf.id, request.wettkampfdisziplinId, cache)
    val nspatch = wkv.notenSpez match {
      case sw: StandardWettkampf => sw.copy(scoreTemplate = Some(request.template))
      case other => other
    }
    val wkvPatched = wkv.copy(notenSpez = nspatch)
    val view = request.template.toView(request.values)
    val wertung = Wertung(
      0L, 0L, request.wettkampfdisziplinId, wettkampf.id, wettkampf.uuid.getOrElse(""),
      None, None, None, None, None, None, None, Some(view))
    val result = wkvPatched.verifiedAndCalculatedWertung(wertung)
    val dState = validateFormula(request.template.dExpression(request.template.variables), "D")
    val eState = validateFormula(request.template.eExpression(request.template.variables), "E")
    val pState = validateFormula(request.template.pExpression(request.template.variables), "Penalty")
    ScoreCalcPreviewResponse(
      view.variables,
      result.noteD, result.noteE, result.endnote,
      dState, eState, pState,
      dState.contains("OK") && eState.contains("OK") && pState.contains("OK"))
  }

  private def validateFormula(rendered: String, label: String): String =
    try {
      Expression(MathExpCompiler(rendered))
      s"$label Formel $rendered OK"
    } catch {
      case e: Exception => s"$label Formel mit Fehler: ${e.getMessage}"
    }
}
