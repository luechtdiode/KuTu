package ch.seidel.kutu.http

import ch.seidel.kutu.domain.*
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.{Directives, MalformedRequestContentRejection, RejectionHandler, RequestEntityExpectedRejection, Route}
import spray.json.*

case class AltersklassenPreviewRequest(formel: String)

case class AltersklassenPreviewResponse(valid: Boolean, labels: List[String])

object AltersklassenPreview {
  val MaxFormulaLength = 4096

  def apply(formel: String): AltersklassenPreviewResponse = {
    val candidate = Option(formel).map(_.trim).getOrElse("")
    if candidate.isEmpty then {
      AltersklassenPreviewResponse(valid = true, labels = Nil)
    } else {
      val grenzen = Altersklasse.parseGrenzen(candidate)
      if grenzen.isEmpty then {
        AltersklassenPreviewResponse(valid = false, labels = Nil)
      } else {
        AltersklassenPreviewResponse(valid = true, labels = Altersklasse(grenzen).map(_.easyprint).toList)
      }
    }
  }
}

/**
 * Liefert die aus einer Altersklassen-Formel generierten Bezeichnungen für die Editor-Vorschau.
 *
 * Bewusst ohne Authentifizierung: Die Berechnung liest keine Daten und wird auch beim Anlegen
 * eines neuen Wettkampfs benötigt, bevor ein Wettkampf-Token existiert.
 */
trait AltersklassenRoutes extends Directives with SprayJsonSupport with JsonSupport with RouterLogging {

  private val previewRejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle { case MalformedRequestContentRejection(message, _) =>
      complete(StatusCodes.BadRequest, message)
    }
    .handle { case RequestEntityExpectedRejection =>
      complete(StatusCodes.BadRequest, "Request body erwartet")
    }
    .result()

  lazy val altersklassenRoutes: Route = {
    pathLabeled("altersklassen" / "preview", "altersklassen/preview") {
      pathEnd {
        post {
          handleRejections(previewRejectionHandler) {
            entity(as[AltersklassenPreviewRequest]) { request =>
              val formel = Option(request.formel).getOrElse("")
              if formel.length > AltersklassenPreview.MaxFormulaLength then {
                complete(StatusCodes.BadRequest, "Formel zu lang")
              } else {
                complete(AltersklassenPreview(formel).toJson)
              }
            }
          }
        }
      }
    }
  }
}
