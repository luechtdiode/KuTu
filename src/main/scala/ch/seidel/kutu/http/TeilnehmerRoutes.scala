package ch.seidel.kutu.http

import ch.seidel.kutu.{Config, KuTuApp}
import ch.seidel.kutu.Config.{homedir, remoteBaseUrl}
import ch.seidel.kutu.actors.{CompetitionCoordinatorClientActor, CompetitionRegistrationClientActor, RefreshWettkampfMap, RegistrationChanged, RegistrationResync}
import ch.seidel.kutu.data.WettkampfImportSupport.*
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.renderer.{FilenameDefault, KategorieTeilnehmerToHtmlRenderer, NotenblattToHtmlRenderer, ServerPrintUtil}
import ch.seidel.kutu.squad.RiegenBuilder.{generateRiegen2Name, generateRiegenName}
import ch.seidel.kutu.view.PrintUtil
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import org.apache.pekko.http.scaladsl.server.Route
import org.slf4j.{Logger, LoggerFactory}
import scalafx.application.Platform
import scalafx.print.PageOrientation
import spray.json.*

import java.io.File
import java.nio.file.Files
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration.{Duration, DurationInt}

case class TeilnehmerItem(athletId: Long, name: String, vorname: String, geschlecht: String, jahrgang: String,
                          verein: String, programmId: Long, programm: String,
                          riege: Option[String], riege2: Option[String], team: Int, teamName: String, reserve: Int)

case class TeilnehmerUpdate(programmId: Option[Long], riege: Option[String], riege2: Option[String], team: Option[Int], reserve: Option[Int])

case class TeilnehmerProgramm(id: Long, name: String)

case class TeilnehmerImportRow(key: String, status: String, name: String, vorname: String, jahrgang: String,
                               verein: String, programm: String, oldProgramm: String, team: Int, reserve: Int)

case class TeilnehmerImportPreview(rows: List[TeilnehmerImportRow])

case class TeilnehmerImportResult(applied: Int)

trait TeilnehmerRoutes extends SprayJsonSupport with JsonSupport with JwtSupport with AuthSupport
  with RouterLogging with KutuService with CIDSupport {

  import DefaultJsonProtocol.*

  private given teilnehmerItemFormat: RootJsonFormat[TeilnehmerItem] = jsonFormat13(TeilnehmerItem.apply)
  private given teilnehmerUpdateFormat: RootJsonFormat[TeilnehmerUpdate] = jsonFormat5(TeilnehmerUpdate.apply)
  private given teilnehmerProgrammFormat: RootJsonFormat[TeilnehmerProgramm] = jsonFormat2(TeilnehmerProgramm.apply)
  private given teilnehmerImportRowFormat: RootJsonFormat[TeilnehmerImportRow] = jsonFormat10(TeilnehmerImportRow.apply)
  private given teilnehmerImportPreviewFormat: RootJsonFormat[TeilnehmerImportPreview] = jsonFormat1(TeilnehmerImportPreview.apply)
  private given teilnehmerImportResultFormat: RootJsonFormat[TeilnehmerImportResult] = jsonFormat1(TeilnehmerImportResult.apply)

  private val teilnehmerLogger: Logger = LoggerFactory.getLogger(classOf[TeilnehmerRoutes])
  private val kategorieRenderer = new KategorieTeilnehmerToHtmlRenderer {
    override val logger: Logger = teilnehmerLogger
  }
  private val notenblattRenderer = new NotenblattToHtmlRenderer {
    override val logger: Logger = teilnehmerLogger
  }
  private val xlsxContentType = ContentType(
    MediaTypes.`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`)

  // one entry per athlete (first wertung carries riege/team data)
  private def teilnehmerRows(wettkampfId: Long): Seq[IndexedSeq[WertungView]] =
    selectWertungen(wettkampfId = Some(wettkampfId))
      .groupBy(_.athlet.id).values
      .map(_.toIndexedSeq)
      .toSeq
      .sortBy(r => (r.head.athlet.verein.map(_.name).getOrElse(""), r.head.wettkampfdisziplin.programm.name, r.head.athlet.name, r.head.athlet.vorname))

  private def teamItemsFor(wertung: WertungView, rows: Seq[IndexedSeq[WertungView]]): List[TeamItem] = {
    val wk = wertung.wettkampf
    val athletVerein = wertung.athlet.verein
    def teamNumbers(filter: WertungView => Boolean): List[Int] =
      rows.map(_.head).filter(filter).map(_.team).filter(_ > 0).distinct.sorted.toList
    val (teamname, vereinTeams) = athletVerein match {
      case Some(v) if wk.teamrule.nonEmpty =>
        if wk.teamrule.exists(_.contains("VereinGe")) then
          (v.easyprint, teamNumbers(_.athlet.verein == athletVerein))
        else {
          val verband = v.verband.getOrElse(v.easyprint)
          (v.verband.getOrElse(v.extendedprint), teamNumbers(_.athlet.verein.exists(_.verband.exists(_.equals(verband)))))
        }
      case _ => (athletVerein.map(_.easyprint).getOrElse(""), teamNumbers(_.athlet.verein == athletVerein))
    }
    val nextTeam = if vereinTeams.isEmpty then 1 else vereinTeams.max + 1
    (1 to nextTeam).toList.map(idx => TeamItem(idx, teamname)) :::
      wk.extraTeams.zipWithIndex.map(item => TeamItem(item._2 * -1 - 1, item._1))
  }

  private def teamTextOf(wertung: WertungView, rows: Seq[IndexedSeq[WertungView]]): String =
    teamItemsFor(wertung, rows).find(_.index == wertung.team).map(_.itemText).getOrElse("")

  private def toItem(row: IndexedSeq[WertungView], rows: Seq[IndexedSeq[WertungView]]): TeilnehmerItem = {
    val w = row.head
    TeilnehmerItem(w.athlet.id, w.athlet.name, w.athlet.vorname, w.athlet.geschlecht,
      AthletJahrgang(w.athlet.gebdat).jahrgang, w.athlet.verein.map(_.easyprint).getOrElse(""),
      w.wettkampfdisziplin.programm.id, w.wettkampfdisziplin.programm.name,
      w.riege, w.riege2, w.team, teamTextOf(w, rows), w.reserve)
  }

  private def selectedRows(rows: Seq[IndexedSeq[WertungView]], ids: Option[String]): Seq[IndexedSeq[WertungView]] = {
    val idSet = ids.toList.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty).flatMap(_.toLongOption).toSet
    if idSet.isEmpty then rows else rows.filter(r => idSet.contains(r.head.athlet.id))
  }

  private def relevantDisziplinen(athletwertungen: Seq[WertungView]) = athletwertungen.filter { wertung =>
    if wertung.wettkampfdisziplin.feminim == 0 && !wertung.athlet.geschlecht.equalsIgnoreCase("M") then false
    else if wertung.wettkampfdisziplin.masculin == 0 && wertung.athlet.geschlecht.equalsIgnoreCase("M") then false
    else true
  }

  private def toExportRow(row: IndexedSeq[WertungView]): Map[String, String] = {
    val w = row.head
    val (verein, rlz, verband) = w.athlet.verein.map {
      case v if v.name.contains(", ") =>
        val parts = v.name.split(",").map(_.trim)
        (parts(0), parts(1), v.verband.getOrElse(""))
      case v => (v.name, "", v.verband.getOrElse(""))
    }.getOrElse(("", "", ""))
    Map(
      "NAME" -> w.athlet.name,
      "VORNAME" -> w.athlet.vorname,
      "JAHRGANG" -> w.athlet.gebdat.map(d => f"$d%tY").getOrElse(""),
      "GESCHLECHT" -> w.athlet.geschlecht,
      "VEREIN" -> verein,
      "VERBAND" -> verband,
      "KATEGORIE" -> w.wettkampfdisziplin.programm.name,
      "TEAM" -> (if w.team > 0 then w.team.toString else ""),
      "RESERVE" -> (if w.team > 0 && w.reserve > 0 then w.reserve.toString else ""),
      "RLZ_TZ" -> rlz,
      "VERBAND_RLZ" -> ""
    )
  }

  private def exportExcelBytes(wettkampf: Wettkampf, rows: Seq[IndexedSeq[WertungView]], includeData: Boolean): Array[Byte] = {
    val tmp = Files.createTempFile("teilnehmer", ".xlsx").toFile
    try {
      val exportRows = if includeData then
        rows.map(toExportRow).sortBy(r => (r("VEREIN"), r("KATEGORIE"), r("JAHRGANG"), r("NAME")))
      else Seq.empty
      if exportRows.nonEmpty then writeExcelFile(tmp, ImportExcelHeaders, exportRows)
      else writeExcelFile(tmp, ImportExcelHeaders, readWettkampfLeafs(wettkampf.programmId).zipWithIndex.map { case (pgm, idx) =>
        Map(
          "NAME" -> "Mustermann",
          "VORNAME" -> (if idx % 2 == 0 then s"Max ${idx + 1}" else s"Klara ${idx + 1}"),
          "JAHRGANG" -> (java.time.LocalDate.now().getYear - 10).toString,
          "GESCHLECHT" -> (if idx % 2 == 0 then "M" else "W"),
          "VEREIN" -> "TV Musterstadt",
          "VERBAND" -> "Musterverband",
          "KATEGORIE" -> pgm.name,
          "TEAM" -> "<Teamnummer, optional>",
          "RESERVE" -> "<Reserve Reihenfolge, optional>",
          "RLZ_TZ" -> "<Regionales Leistungszentrum oder Talentzentrum, optional>",
          "VERBAND_RLZ" -> "<Verband des RLZ, optional>")
      })
      Files.readAllBytes(tmp.toPath)
    } finally tmp.delete()
  }

  private def readImportRows(filename: String, bytes: Array[Byte]): Seq[Map[String, String]] = {
    val suffix = if filename.toLowerCase.endsWith(".xlsx") then ".xlsx"
    else if filename.toLowerCase.endsWith(".xls") then ".xls"
    else ".csv"
    val tmp = Files.createTempFile("teilnehmer-import", suffix).toFile
    try {
      Files.write(tmp.toPath, bytes)
      readTabularFile(tmp.toURI).map { case (headers, sourceRows) =>
        val mapping: Map[String, Option[String]] = CsvDefaultFieldMapping.map { case (field, aliases) =>
          field -> headers.find(h => aliases.exists(_.equalsIgnoreCase(h.trim)))
        }
        mapFieldRows(sourceRows, mapping)
          .filter(row => !(row.getOrElse("NAME", "").equals("Mustermann") && row.getOrElse("VERBAND", "").equals("Musterverband")))
      }.getOrElse(Seq.empty)
    } finally tmp.delete()
  }

  private def buildImport(wettkampf: Wettkampf, bytes: Array[Byte], filename: String) = {
    val wkView = wettkampf.toView(readProgramm(wettkampf.programmId))
    val importService = new WettkampfImportService(this, wkView)
    val rows = teilnehmerRows(wettkampf.id)
    val programms = readWettkampfLeafs(wettkampf.programmId)
    val existing = rows.map(_.head)
    val previewRows = importService.prepareStructuredPreviewRows(
      rowfields = readImportRows(filename, bytes),
      programms = programms,
      progrm = None,
      options = importService.StructuredPreviewOptions(
        removeMissingAthletes = true,
        fixedVerein = None,
        genderValueMapping = effectiveGenderValueMapping(DefaultGenderValueMappingRaw),
        resolveVereinFromFields = true),
      context = importService.StructuredPreviewContext(
        existingAthlets = existing.map(_.athlet),
        existingProgramsByAthletEasyprint = existing.map(w => w.athlet.easyprint -> w.wettkampfdisziplin.programm.id).toMap,
        existingTeamsByAthletEasyprint = existing.map(w => w.athlet.easyprint -> (w.team, w.reserve)).toMap)
    )
    val wertungIds = rows.map(r => r.head.athlet.easyprint -> r.map(_.id).toSet).toMap
    (importService, previewRows, programms, wertungIds)
  }

  private def readUpload(entity: RequestEntity): Future[(String, Array[Byte], Map[String, String])] = {
    given org.apache.pekko.stream.Materializer = Core.materializer
    Future.successful(entity).flatMap { e =>
      org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal(e).to[Multipart.FormData].flatMap(_.toStrict(30.seconds))
    }.map { strict =>
      val parts = strict.strictParts
      val file = parts.find(_.name == "file")
      val fields = parts.filter(_.filename.isEmpty).map(p => p.name -> p.entity.data.utf8String).toMap
      (file.flatMap(_.filename).getOrElse("import.xlsx"), file.map(_.entity.data.toArray).getOrElse(Array.empty[Byte]), fields)
    }
  }

  lazy val teilnehmerRoutes: Route = {
    (handleCID & extractUri) { (clientId: String, _) =>
      pathPrefixLabeled("competition" / JavaUUID / "teilnehmer", "competition/:uuid/teilnehmer") { wkuuid =>
        authenticatedAdmin() { userId =>
          if !userId.equals(wkuuid.toString) || !wettkampfExists(wkuuid.toString) then complete(StatusCodes.Conflict)
          else {
            val wettkampf = readWettkampf(wkuuid.toString)
            def refresh(): Unit = CompetitionCoordinatorClientActor.publish(RefreshWettkampfMap(wkuuid.toString), clientId)

            pathEndOrSingleSlash {
              get {
                complete(Future {
                  val rows = teilnehmerRows(wettkampf.id)
                  rows.map(toItem(_, rows)).toList.toJson
                })
              }
            } ~
              path("programme") {
                get {
                  complete(Future {
                    readWettkampfLeafs(wettkampf.programmId).map(p => TeilnehmerProgramm(p.id, p.name)).toList.toJson
                  })
                }
              } ~
              pathPrefix("athlet" / LongNumber) { athletId =>
                path("teams") {
                  get {
                    complete(Future {
                      val rows = teilnehmerRows(wettkampf.id)
                      rows.find(_.head.athlet.id == athletId) match {
                        case Some(r) => teamItemsFor(r.head, rows).toJson(using DefaultJsonProtocol.listFormat(using teamFormat))
                        case None => JsArray()
                      }
                    })
                  }
                } ~
                  pathEndOrSingleSlash {
                    put {
                      entity(as[TeilnehmerUpdate]) { update =>
                        val rows = teilnehmerRows(wettkampf.id)
                        rows.find(_.head.athlet.id == athletId) match {
                          case None => complete(StatusCodes.NotFound)
                          case Some(row) =>
                            val normalize = (s: Option[String]) => s.map(_.trim).filter(s => s.nonEmpty && s != "keine Einteilung")
                            val first = row.head
                            val newPgm = update.programmId.getOrElse(first.wettkampfdisziplin.programm.id)
                            val newTeam = update.team.getOrElse(first.team)
                            val newReserve = update.reserve.getOrElse(first.reserve)
                            onComplete(Future {
                              if newPgm != first.wettkampfdisziplin.programm.id || newTeam != first.team || newReserve != first.reserve then {
                                moveToProgram(wettkampf.id, newPgm, newTeam, newReserve, first.athlet, publishWS = false)
                                CompetitionRegistrationClientActor.publish(RegistrationResync(wettkampf.uuid.get), clientId)
                              }
                              val current = teilnehmerRows(wettkampf.id).find(_.head.athlet.id == athletId).get
                              val riegeChanged = update.riege.exists(r => normalize(Some(r)) != current.head.riege) ||
                                update.riege2.exists(r => normalize(Some(r)) != current.head.riege2)
                              if riegeChanged then {
                                val newRiege = update.riege.map(r => normalize(Some(r))).getOrElse(current.head.riege)
                                val newRiege2 = update.riege2.map(r => normalize(Some(r))).getOrElse(current.head.riege2)
                                Await.result(updateAllWertungenAsync(current.map(_.copy(riege = newRiege, riege2 = newRiege2).toWertung)), Duration.Inf)
                                /*(newRiege.toList ++ newRiege2.toList).distinct.foreach { name =>
                                  findAndStoreMatchingRiege(RiegeRaw(wettkampf.id, name, None, None, 0))
                                }*/
                                cleanUnusedRiegen(wettkampf.id)
                              }
                              val rows2 = teilnehmerRows(wettkampf.id)
                              val result = rows2.find(_.head.athlet.id == athletId).get
                              refresh()
                              toItem(result, rows2).toJson
                            }) {
                              case Success(json) => complete(json)
                              case Failure(e) =>
                                teilnehmerLogger.error("update teilnehmer failed", e)
                                complete(StatusCodes.InternalServerError, e.getMessage)
                            }
                        }
                      }
                    }
                  }
              } ~
              path("export") {
                get {
                  parameters(Symbol("includeData").as[Boolean].withDefault(true), Symbol("ids").?) { (includeData, ids) =>
                    complete(Future {
                      val bytes = exportExcelBytes(wettkampf, selectedRows(teilnehmerRows(wettkampf.id), ids), includeData)
                      HttpResponse(entity = HttpEntity(xlsxContentType, bytes),
                        headers = List(`Content-Disposition`(ContentDispositionTypes.attachment,
                          Map("filename" -> s"Teilnehmerliste_${encodeFileName(wettkampf.easyprint)}.xlsx"))))
                    })
                  }
                }
              } ~
              pathPrefix("import") {
                path("preview") {
                  post {
                    extractRequestEntity { entity =>
                      complete(readUpload(entity).map { case (filename, bytes, _) =>
                        val (_, rows, programms, _) = buildImport(wettkampf, bytes, filename)
                        val pgmNames = programms.map(p => p.id -> p.name).toMap
                        TeilnehmerImportPreview(rows.map { r =>
                          val status = if r.progId == 0L then "entfernt" else if r.oldProg == 0L then "neu" else "geändert"
                          TeilnehmerImportRow(r.athletView.easyprint, status, r.athletView.name, r.athletView.vorname,
                            AthletJahrgang(r.athletView.gebdat).jahrgang, r.athletView.verein.map(_.easyprint).getOrElse(""),
                            pgmNames.getOrElse(r.progId, ""), pgmNames.getOrElse(r.oldProg, ""), r.team, r.reserve)
                        }.toList).toJson
                      })
                    }
                  }
                } ~
                  path("apply") {
                    post {
                      extractRequestEntity { entity =>
                        complete(readUpload(entity).map { case (filename, bytes, fields) =>
                          val selected = fields.get("selected").map(_.parseJson.convertTo[List[String]](using DefaultJsonProtocol.listFormat(using StringJsonFormat)).toSet)
                          val (importService, rows, _, wertungIds) = buildImport(wettkampf, bytes, filename)
                          val chosen = rows.filter(r => selected.forall(_.contains(r.athletView.easyprint)))
                          importService.applyStructuredImportSelection(chosen.asInstanceOf[Seq[importService.ImportRow]], wertungIds, publishWS = false)
                          refresh()
                          TeilnehmerImportResult(chosen.size).toJson
                        })
                      }
                    }
                  }
              } ~
              path("print" / Segment) { kind =>
                get {
                  parameters(Symbol("ids").?) { ids =>
                    val rows = selectedRows(teilnehmerRows(wettkampf.id), ids)
                    if rows.isEmpty then complete(StatusCodes.NotFound)
                    else complete(Future {
                      val dir = new File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint))
                      val logo = ServerPrintUtil.locateLogoFile(dir)
                      val riegen = selectRiegen(wettkampf.id).map(r => r.r -> r).toMap
                      val allRows = teilnehmerRows(wettkampf.id)
                      val dgMapping = selectSimpleDurchgaenge(wettkampf.id).map(d => (d, d.effectivePlanStart(wettkampf.datum.toLocalDate)))
                      def listData = rows.map { row =>
                        val w = row.head
                        val r = riegen.get(w.riege.getOrElse(""))
                        ch.seidel.kutu.renderer.Kandidat(w.wettkampf.easyprint,
                          w.athlet.geschlecht match { case "M" => "Turner" case _ => "Turnerin" },
                          w.wettkampfdisziplin.programm.easyprint, w.athlet.id, w.athlet.name, w.athlet.vorname,
                          AthletJahrgang(w.athlet.gebdat).jahrgang, w.athlet.verein.map(_.easyprint).getOrElse(""),
                          teamTextOf(w, allRows), w.riege.getOrElse(""), r.flatMap(_.durchgang).getOrElse(""),
                          r.flatMap(_.start).map(_.name).getOrElse(""),
                          relevantDisziplinen(row).map(_.wettkampfdisziplin.disziplin.easyprint))
                      }
                      val html = kind match {
                        case "teilnehmer" => kategorieRenderer.toHTMLasKategorienListe(listData, logo, dgMapping, 0)
                        case "vereine" => kategorieRenderer.toHTMLasVereinsListe(listData, logo, dgMapping, 0)
                        case "durchgang" => kategorieRenderer.toHTMLasDurchgangListe(listData, logo, dgMapping, 0)
                        case "notenblatt" =>
                          val seriendaten = rows.map { row =>
                            val w = row.head
                            ch.seidel.kutu.domain.Kandidat(w.wettkampf.easyprint,
                              w.athlet.geschlecht match { case "M" => "Turner" case _ => "Turnerin" },
                              w.wettkampfdisziplin.programm.easyprint, w.athlet.id, w.athlet.name, w.athlet.vorname,
                              AthletJahrgang(w.athlet.gebdat).jahrgang, w.athlet.verein.map(_.easyprint).getOrElse(""),
                              riegen.get(w.riege.getOrElse("")), None,
                              relevantDisziplinen(row).map(_.wettkampfdisziplin.disziplin), Seq.empty, row)
                          }
                          wettkampf.programmId match {
                            case 20 => notenblattRenderer.toHTMLasGeTu(seriendaten, logo)
                            case n if n == 11 || n == 31 => notenblattRenderer.toHTMLasKuTu(seriendaten, logo)
                            case _ => notenblattRenderer.toHTMLasATT(seriendaten, logo)
                          }
                        case _ =>
                          val seriendaten = getAllKandidatenWertungen(wettkampf.uuid.map(UUID.fromString).get)

                          (new Object with ch.seidel.kutu.renderer.RiegenblattToHtmlRenderer)
                            .toHTML(seriendaten, logo, remoteBaseUrl, dgMapping = selectSimpleDurchgaenge(wettkampf.id).map(d => (d, d.effectivePlanStart(wettkampf.datum.toLocalDate))))
                      }
                      HttpEntity(ContentTypes.`text/html(UTF-8)`, html)
                    })
                  }
                }
              }
          }
        }
      }
    }
  }
}
