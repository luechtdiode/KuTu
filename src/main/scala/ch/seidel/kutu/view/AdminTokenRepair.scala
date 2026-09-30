package ch.seidel.kutu.view

import ch.seidel.jwt.JsonWebToken

import java.util.concurrent.TimeUnit

/**
 * Entscheidungslogik für die Web-UI-Admin-Reparatur.
 *
 * Bewusst ohne JavaFX, damit der Ablauf ohne Fenster und ohne xvfb testbar bleibt.
 * Der Ablauf ist mehrstufig: lokale Vorprüfung -> Serverabfrage -> ggf. Erfassung der
 * Veranstalter-Daten -> erneute Abfrage -> Token speichern.
 *
 * Die lokale Prüfung liest nur die Claims, sie validiert keine Signatur: der Desktop kennt
 * den serverseitigen Schlüssel nicht. Der Server bleibt also die autoritative Instanz.
 */
object AdminTokenRepair {

  private val adminKey = "admin"
  private val expiredAtKey = "expiredAtKey"

  /** Ergebnis des lokalen JWT-Checks: schneller Vorlauf, bevor eine Netzwerkanfrage erfolgt. */
  enum LocalCheck:
    /** Token enthält admin und ist unendlich gültig - keine Serverabfrage nötig. */
    case Usable
    /** Altbestand ohne admin-Claim oder unlesbarer Token - Reparatur erforderlich. */
    case NeedsRepair
    /** Kein Secret hinterlegt. */
    case NoSecret

  /**
   * Liest die Claims eines Tokens ohne Signaturprüfung.
   */
  def decodeClaims(token: String): Option[Map[String, String]] = token match {
    case JsonWebToken(_, claims, _) => claims.asSimpleMap.toOption
    case _ => None
  }

  def isExpiryInfinite(claims: Map[String, String]): Boolean =
    claims.get(expiredAtKey).exists { value =>
      try value.toLong - System.currentTimeMillis() > TimeUnit.DAYS.toMillis(36500) // > 100 Jahre
      catch { case _: NumberFormatException => false }
    }

  def hasAdminClaim(claims: Map[String, String]): Boolean = claims.get(adminKey).contains("true")

  def localCheck(hasAdmin: Boolean, hasInfiniteExpiry: Boolean): LocalCheck =
    if hasAdmin && hasInfiniteExpiry then LocalCheck.Usable else LocalCheck.NeedsRepair

  def localCheckFromToken(token: Option[String]): LocalCheck = token match {
    case None => LocalCheck.NoSecret
    case Some(value) if value.trim.isEmpty => LocalCheck.NoSecret
    case Some(value) => decodeClaims(value) match {
      case Some(claims) => localCheck(hasAdminClaim(claims), isExpiryInfinite(claims))
      case None => LocalCheck.NeedsRepair
    }
  }

  /**
   * Nächster Schritt nach der Serverabfrage.
   *
   * @param status      HTTP-Status der Antwort, `None` bei Verbindungsproblem
   * @param creatorSent true wenn in diesem Versuch Veranstalter-Daten mitgeschickt wurden.
   *                    Ein 409 ohne mitgeschickte Daten bedeutet "Daten fehlen", ein 409 mit
   *                    mitgeschickten Daten bedeutet "Daten weichen ab".
   */
  enum NextStep:
    /** Browser öffnen. */
    case OpenBrowser
    /** Veranstalter-Daten inkl. Nutzungsbedingungen erheben, dann erneut senden. */
    case CollectCreatorData
    /** Abbrechen und dem Benutzer den Grund anzeigen. */
    case Abort(reason: String)

  def nextStep(status: Option[Int], creatorSent: Boolean): NextStep = status match {
    case Some(200) => NextStep.OpenBrowser
    case Some(409) if !creatorSent => NextStep.CollectCreatorData
    case Some(409) => NextStep.Abort(
      "Für diesen Wettkampf sind bereits andere Veranstalter-Daten hinterlegt. " +
        "Diese können nur noch auf der Server-Seite geändert werden.")
    case Some(401) | Some(403) => NextStep.Abort(
      "Das gespeicherte Passwort wird vom Server nicht mehr akzeptiert. " +
        "Bitte das Passwort dieses Wettkampfs prüfen.")
    case Some(404) => NextStep.Abort(
      "Der Wettkampf existiert auf dem Server nicht (mehr).")
    case Some(400) => NextStep.Abort(
      "Der Server hat die Veranstalter-Daten nicht akzeptiert. Bitte die Eingaben prüfen.")
    case Some(other) => NextStep.Abort(s"Der Server antwortete mit HTTP $other.")
    case None => NextStep.Abort("Der Server ist nicht erreichbar.")
  }
}
