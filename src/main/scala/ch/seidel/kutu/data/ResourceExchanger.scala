package ch.seidel.kutu.data

import ch.seidel.kutu.actors.*
import ch.seidel.kutu.calc.{ScoreAggregateFn, ScoreCalcTemplate, ScoreCalcTemplateView, TemplateViewJsonReader}
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.renderer.ServerPrintUtil
import ch.seidel.kutu.squad.RiegenBuilder
import ch.seidel.kutu.view.*
import ch.seidel.kutu.{Config, KuTuApp}
import org.slf4j.LoggerFactory
import slick.jdbc

import java.io.*
import java.math.BigInteger
import java.security.{DigestInputStream, MessageDigest}
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import java.util.{Base64, UUID}
import scala.compiletime.constValueTuple
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.deriving.Mirror
import scala.io.Source

/**
 */
object ResourceExchanger extends KutuService with RiegenBuilder {
  private val logger = LoggerFactory.getLogger(this.getClass)
  val enc: Base64.Encoder = Base64.getUrlEncoder
  private val dec: Base64.Decoder = Base64.getUrlDecoder
  import ch.seidel.kutu.domain.{given_Conversion_String_BigDecimal, given_Conversion_String_Int, given_Conversion_String_Long}

  def processWSMessage[T](wettkampf: Wettkampf, refresher: (Option[T], KutuAppEvent) => Unit): (Option[T], KutuAppEvent) => Unit = {
    val cache = new java.util.ArrayList[MatchCode]()
    val cache2: scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]] = scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]]()
    val wkDiszs = listWettkampfDisziplineViews(wettkampf)
      .map(d => d.id -> d).toMap

    def mapToLocal(athlet: AthletView, wettkampf: Option[Long]) = {
      val mappedverein = athlet.verein match {
        case Some(v) => findVereinLike(Verein(id = 0, name = v.name, verband = v.verband))
        case _ => None
      }
      val mappedAthlet = findAthleteLike(cache, wettkampf, exclusive = false)(athlet.toAthlet.copy(id = 0, verein = mappedverein))
      val mappedAthletView = athlet.updatedWith(mappedAthlet)
      //println(mappedAthletView, mappedverein, cache)
      mappedAthletView
    }

    def opFn: (Option[T], KutuAppEvent) => Unit = {
      case (sender, LastResults(results)) =>
        val mappedWertungen: Seq[AthletWertungUpdatedSequenced] = results.groupBy(_.athlet).flatMap { tuple =>
          val (athlet, wertungen) = tuple
          val mappedAthletView: AthletView = mapToLocal(athlet, Some(wettkampf.id))
          wertungen.groupBy(_.wertung.wettkampfdisziplinId).map {
            _._2.maxBy(_.sequenceId)
          }.map { updatedSequenced =>
            val programm = updatedSequenced.programm
            val disz = wkDiszs.get(updatedSequenced.wertung.wettkampfdisziplinId).map(_.disziplin.easyprint).getOrElse(s"Disz${updatedSequenced.wertung.wettkampfdisziplinId}")

            val mappedWertung = updatedSequenced.wertung.copy(
              athletId = mappedAthletView.id,
              wettkampfId = wettkampf.id,
              wettkampfUUID = updatedSequenced.wettkampfUUID)
            logger.info(s"received for ${athlet.vorname} ${athlet.name} (${athlet.verein.getOrElse("")}) im Pgm $programm Disz $disz: ${mappedWertung.resultatWithVariables}")
            updatedSequenced.copy(athlet = mappedAthletView, wertung = mappedWertung)
          }
        }.toSeq
        try {
          KuTuApp.invokeWithBusyIndicator {
            refresher(sender, LastResults(
              mappedWertungen.zip(updateWertungenWithIDMapping(mappedWertungen.map(_.wertung), cache2)).map {
                x => x._1.copy(wertung = x._2)
              }.toList))
          }
        } catch {
          case e: Exception =>
            logger.error(s"failed to complete save LastResults ", e)
        }
      case (sender, bulkEvent@BulkEvent(wettkampfUUID, events)) =>
        if !Config.isLocalHostServer && wettkampf.uuid.contains(wettkampfUUID) then {
          events.foreach {
            case ds: DurchgangStarted => storeDurchgangStarted(ds)
            case df: DurchgangFinished => storeDurchgangFinished(df)
            case df: DurchgangResetted => storeDurchgangResetted(df)
            case _ =>
          }
          refresher(sender, bulkEvent)
        }
      case (sender, ds@DurchgangStarted(wettkampfUUID, _, _)) =>
        if !Config.isLocalHostServer && wettkampf.uuid.contains(wettkampfUUID) then {
          storeDurchgangStarted(ds)
        }
        refresher(sender, ds)

      case (sender, df@DurchgangFinished(wettkampfUUID, _, _)) =>
        if !Config.isLocalHostServer && wettkampf.uuid.contains(wettkampfUUID) then {
          storeDurchgangFinished(df)
        }
        refresher(sender, df)

      case (sender, dr@DurchgangResetted(wettkampfUUID, _)) =>
        if !Config.isLocalHostServer && wettkampf.uuid.contains(wettkampfUUID) then {
          storeDurchgangResetted(dr)
        }
        refresher(sender, dr)

      case (sender, uws: AthletWertungUpdatedSequenced) => opFn(sender, uws.toAthletWertungUpdated())
      case (sender, uw@AthletWertungUpdated(athlet, wertung, wettkampfUUID, _, _, programm)) =>
        if Config.isLocalHostServer then {
          refresher(sender, uw)
        } else if wettkampf.uuid.contains(wettkampfUUID) then /*Future*/ {
          val disz = wkDiszs.get(uw.wertung.wettkampfdisziplinId).map(_.disziplin.easyprint).getOrElse(s"Disz${uw.wertung.wettkampfdisziplinId}")
          logger.info(s"received for ${uw.athlet.vorname} ${uw.athlet.name} (${uw.athlet.verein.getOrElse("")}) im Pgm $programm Disz $disz: ${wertung.resultatWithVariables}")
          val mappedAthletView: AthletView = mapToLocal(athlet, Some(wettkampf.id))
          val mappedWertung = wertung.copy(athletId = mappedAthletView.id, wettkampfId = wettkampf.id, wettkampfUUID = wettkampfUUID)
          wertung.mediafile.foreach(putMedia)
          try {
            val vw = updateWertungWithIDMapping(mappedWertung, cache2)
            logger.info(s"saved for ${mappedAthletView.vorname} ${mappedAthletView.name} (${uw.athlet.verein.getOrElse("")}) im Pgm $programm Disz $disz: ${wertung.resultatWithVariables}")
            refresher(sender, uw.copy(athlet.copy(id = mappedAthletView.id), wertung = vw))
          } catch {
            case e: Exception =>
              logger.error(s"failed to complete save new score for " +
                s"${mappedAthletView.vorname} ${mappedAthletView.name} (${mappedAthletView.verein.getOrElse("")}) " +
                s"im Pgm $programm Disz $disz new new Wertung: $wertung", e)
              refresher(sender, uw)
          }
        }
      case (sender, uw: MediaPlayerAction) =>
        if Config.isLocalHostServer then {
          refresher(sender, uw)
        } else if wettkampf.uuid.contains(uw.wertung.wettkampfUUID) then /*Future*/ {
          logger.info(s"received MediaAction for ${uw.athlet.vorname} ${uw.athlet.name} (${uw.athlet.verein})")
          val mappedAthletView: AthletView = mapToLocal(uw.athlet, Some(wettkampf.id))
          val mappedWertung = uw.wertung.copy(athletId = mappedAthletView.id)
          val mappedAction = uw match {
            case AthletMediaAquire(_, _, _) => AthletMediaAquire(uw.wertung.wettkampfUUID, mappedAthletView, mappedWertung)
            case AthletMediaRelease(_, _, _) => AthletMediaRelease(uw.wertung.wettkampfUUID, mappedAthletView, mappedWertung)
            case AthletMediaStart(_, _, _) => AthletMediaStart(uw.wertung.wettkampfUUID, mappedAthletView, mappedWertung)
            case AthletMediaPause(_, _, _) => AthletMediaPause(uw.wertung.wettkampfUUID, mappedAthletView, mappedWertung)
            case AthletMediaToStart(_, _, _) => AthletMediaToStart(uw.wertung.wettkampfUUID, mappedAthletView, mappedWertung)
          }
          try {
            refresher(sender, mappedAction)
          } catch {
            case e: Exception =>
              logger.error(s"failed to complete playeraction for " +
                s"${mappedAthletView.vorname} ${mappedAthletView.name} (${mappedAthletView.verein.getOrElse("")}) " +
                s": $mappedAction", e)
              refresher(sender, uw)
          }
        }
      case (sender, scorePublished@ScoresPublished(scoreId: String, title: String, query: String, published: Boolean, wettkampfUUID: String)) =>
        if wettkampf.uuid.contains(wettkampfUUID) then /*Future*/ {
          logger.info(s"received $scorePublished")
          updatePublishedScore(wettkampf.id, scoreId, title, query, published, propagate = false)
          refresher(sender, scorePublished)
        }
      case (sender, awm@AthletsAddedToWettkampf(athlets, wettkampfUUID, programm, team)) =>
        if wettkampf.uuid.contains(wettkampfUUID) then /*Future*/ {
          cache.clear()
          val insertedAthlets = athlets
            .map { aa =>
              val (athlet, media) = aa
              logger.info(s"received for ${athlet.vorname} ${athlet.name} (${athlet.verein.getOrElse(() => "")}) " +
                s"to be added to competition ${awm.wettkampfUUID} to Program-Id:$programm, to team: $team")
              val mappedAthletView: AthletView = mapToLocal(athlet, None)
              if mappedAthletView.id == 0 then {
                (insertAthlete(mappedAthletView.toAthlet).id, media)
              } else {
                (mappedAthletView.id, media)
              }
            }
            .toSet
          assignAthletsToWettkampf(wettkampf.id, Set(programm), insertedAthlets, Some(team))
          refresher(sender, awm)
          logger.info(s"${athlets.size} assigned to competition ${awm.wettkampfUUID} to Program-Id:${awm.pgmId}")
        }
      case (sender, awm@AthletMovedInWettkampf(athlet, wettkampfUUID, programm, team)) =>
        if wettkampf.uuid.contains(wettkampfUUID) then /*Future*/ {
          logger.info(s"received for ${awm.athlet.vorname} ${awm.athlet.name} (${awm.athlet.verein.getOrElse(() => "")}) " +
            s"to be moved in competition ${awm.wettkampfUUID} to Program-Id:$programm, to Team: $team")
          val mappedAthletView: AthletView = mapToLocal(athlet, Some(wettkampf.id))
          val mappedEvent = awm.copy(athlet = mappedAthletView)
          for durchgang <- moveToProgram(mappedEvent) do {
            logger.info(s"durchgang $durchgang changed competition $wettkampfUUID")
            refresher(sender, DurchgangChanged(durchgang, wettkampfUUID, mappedEvent.athlet))
          }
          logger.info(s"${mappedAthletView.vorname} ${mappedAthletView.name} (${mappedAthletView.verein.getOrElse(() => "")}) " +
            s"moved in competition ${awm.wettkampfUUID} to Program-Id:${awm.pgmId}")
          refresher(sender, mappedEvent)
        }
      case (sender, arw@AthletRemovedFromWettkampf(athlet, wettkampfUUID)) =>
        if wettkampf.uuid.contains(wettkampfUUID) then /*Future*/ {
          logger.info(s"received for ${arw.athlet.vorname} ${arw.athlet.name} (${arw.athlet.verein.getOrElse(() => "")}) " +
            s"to be removed from competition ${arw.wettkampfUUID}")
          cache.clear()
          val mappedAthletView: AthletView = mapToLocal(athlet, Some(wettkampf.id))
          val mappedEvent = arw.copy(athlet = mappedAthletView)
          for durchgang <- unassignAthletFromWettkampf(mappedEvent) do {
            logger.info(s"durchgang $durchgang changed competition $wettkampfUUID")
            refresher(sender, DurchgangChanged(durchgang, wettkampfUUID, mappedEvent.athlet))
          }
          logger.info(s"${mappedAthletView.vorname} ${mappedAthletView.name} (${mappedAthletView.verein.getOrElse(() => "")}) " +
            s"removed from competition ${arw.wettkampfUUID}")
          refresher(sender, mappedEvent)

        }
      case (_, MessageAck(msg)) =>
        logger.debug(msg) // ignore
      case (sender, someOther) =>
        refresher(sender, someOther)
    }

    opFn
  }

  def saveMediaFile(file: InputStream, wettkampf: Wettkampf, media: Media): MediaAdmin = {
    val digestStream = new DigestInputStream(file, MessageDigest.getInstance("MD5"))
    val buffer = new BufferedInputStream(digestStream, 5 * 1024 * 1024)
    val mediaAdmin: MediaAdmin = putMedia(Media(media.id, media.name.trim, media.extension.trim))
    val wettkampfAudiofilesDir = mediaAdmin.computeFilePath(wettkampf).getParentFile
    if !wettkampfAudiofilesDir.exists() then {
      wettkampfAudiofilesDir.mkdirs()
    }
    val uploadedOriginalFile = mediaAdmin.computeFilePath(wettkampf)
    val fos = new FileOutputStream(uploadedOriginalFile)
    try {
      buffer.transferTo(fos)
    } finally {
      fos.flush()
      fos.close()
    }
    if uploadedOriginalFile.length() > Config.mediafileMaxSize then {
      val maxSize = java.text.NumberFormat.getInstance().format(Config.mediafileMaxSize / 1024)
      val currentSize = java.text.NumberFormat.getInstance().format(uploadedOriginalFile.length() / 1024)
      uploadedOriginalFile.delete()
      throw new RuntimeException(s"Die Datei ${media.name} ist mit $currentSize Kilobytes zu gross. Sie darf nicht grösser als $maxSize Kilobytes sein.")
    }

    val hash = digestStream.getMessageDigest.digest()
    val hashedMediaAdmin = mediaAdmin.copy(md5 = new BigInteger(1, hash).toString(16))
    saveAndReindexMediaFile(wettkampf, uploadedOriginalFile, hashedMediaAdmin)
    /*
    TODO and then ffmpeg.exe -i input.ogg -vn -f md5 output.mp3 to get the hash usable as filename
    https://superuser.com/questions/1044413/audio-md5-checksum-with-ffmpeg
    https://ffmpeg.org/ffmpeg.html#Video-and-Audio-file-format-conversion
    https://github.com/linuxserver/docker-ffmpeg
     */
  }

  private def saveAndReindexMediaFile(wettkampf: Wettkampf, uploadedOriginalFile: File, mediaAdmin: MediaAdmin) = {
    val md5HashFile = mediaAdmin.computeFilePath(wettkampf)
    if !md5HashFile.equals(uploadedOriginalFile) then {
      if md5HashFile.exists() then {
        md5HashFile.delete()
      }
      uploadedOriginalFile.renameTo(md5HashFile)
    }
    val finalmedia = updateMedia(mediaAdmin)
    logger.info(s"saved and reindexed mediafile: $finalmedia ($uploadedOriginalFile -> $md5HashFile")
    finalmedia
  }

  def cleanupMediaFiles(): Unit = {
    logger.info("cleanup media files ...")
    val cleanedMedialist = cleanUnusedMedia().map(m => m.filename).toSet
    for wettkampf <- listWettkaempfeView do {
      val competitionMediaDir = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint) + "/audiofiles")
      if competitionMediaDir.exists() && competitionMediaDir.isDirectory then {
        competitionMediaDir
          .listFiles()
          .filter(f => !cleanedMedialist.contains(f.getName))
          .foreach { mp3File =>
            mp3File.delete()
            logger.info("mp3-file deleted " + mp3File.getName)
          }
      }
    }
  }

  def importWettkampfMediaFile(wettkampf: Wettkampf, registration: AthletRegistration, file: InputStream): AthletRegistration = {
    registration.mediafile match {
      case Some(media) =>
        updateAthletRegistration(registration.copy(mediafile = Some(saveMediaFile(file, wettkampf, media.toMedia)))).get
      case _ => registration
    }
  }

  def importWettkampfMediaFiles(wettkampf: Wettkampf, mediaList: List[MediaAdmin], file: InputStream): Unit = {
    val buffer = new BufferedInputStream(file)
    buffer.mark(1024 * 1024 * 1024) // max 1GB
    type ZipStream = (ZipEntry, InputStream)
    class ZipEntryTraversableClass extends Iterator[ZipStream] {
      buffer.reset()
      val zis = new ZipInputStream(buffer)
      val zisIterator: Iterator[ZipEntry] = Iterator.continually(zis.getNextEntry)
        .filter(ze => ze == null || !ze.isDirectory)
        .takeWhile(ze => ze != null).iterator

      override def hasNext: Boolean = zisIterator.hasNext

      override def next(): (ZipEntry, InputStream) = (zisIterator.next(), zis)
    }
    new ZipEntryTraversableClass().foreach { entry =>
      val keys = (entry._1.getName + "@0").split("@")
      val filename = keys(0)
      val mediaId = keys(1)
      val media: Media = mediaList.find(m => m.id.equals(mediaId)).map(_.toMedia)
        .getOrElse(mediaList.find(m => m.filename.equals(filename)).map(_.toMedia)
          .getOrElse(Media("", filename, filename.substring(filename.lastIndexOf(".") + 1))))
      saveMediaFile(entry._2, wettkampf, media)
    }
  }

  def importWettkampf(file: InputStream): Wettkampf = {
    val buffer = new BufferedInputStream(file)
    buffer.mark(1024 * 1024 * 1024) // max 1GB
    type ZipStream = (ZipEntry, InputStream)
    class ZipEntryTraversableClass extends Iterator[ZipStream] {
      buffer.reset()
      val zis = new ZipInputStream(buffer)
      val zisIterator: Iterator[ZipEntry] = Iterator.continually(zis.getNextEntry)
        .filter(ze => ze == null || !ze.isDirectory)
        .takeWhile(ze => ze != null).iterator

      override def hasNext: Boolean = zisIterator.hasNext

      override def next(): (ZipEntry, InputStream) = (zisIterator.next(), zis)
    }

    val collection = new ZipEntryTraversableClass().foldLeft(Map[String, (Seq[String], Map[String, Int])]()) { (acc, entry) =>
      if entry._1.getName.endsWith(".csv") then {
        val csv = Source.fromInputStream(entry._2, "utf-8").getLines().toList
        val header = csv.take(1).map(_.dropWhile {
          _.isUnicodeIdentifierPart
        }).flatMap(DBService.parseLine).zipWithIndex.toMap
        acc + (entry._1.getName -> (csv.drop(1), header))
      } else acc
    }

    def getValue(header: Map[String, Int], fields: IndexedSeq[String], key: String, default: String): String = {
      if header.contains(key) && fields(header(key)).nonEmpty then fields(header(key)) else default
    }

    val (vereinCsv, vereinHeader) = collection("vereine.csv")
    logger.info("importing vereine ...", vereinHeader)
    val vereinNameIdx = vereinHeader("name")
    val vereinVerbandIdx = vereinHeader.getOrElse("verband", -1)
    val vereinIdIdx = vereinHeader("id")
    val vereinInstances = vereinCsv.map(DBService.parseLine).filter(_.size == vereinHeader.size).map { fields =>
      val candidate = Verein(id = 0, name = fields(vereinNameIdx), verband = if vereinVerbandIdx > -1 then Some(fields(vereinVerbandIdx)) else None)
      val verein = insertVerein(candidate)
      (fields(vereinIdIdx), verein)
    }.toMap

    val (athletCsv, athletHeader) = collection("athleten.csv")
    logger.info("importing athleten ...", athletHeader)
    val mappedAthletes = athletCsv.map(DBService.parseLine).filter(_.size == athletHeader.size).map { fields =>
      val geb = fields(athletHeader("gebdat")).replace("Some(", "").replace(")", "")
      (fields(athletHeader("id")), Athlet(
        id = 0,
        js_id = fields(athletHeader("js_id")),
        geschlecht = fields(athletHeader("geschlecht")),
        name = fields(athletHeader("name")),
        vorname = fields(athletHeader("vorname")),
        gebdat = if geb.nonEmpty then Some(geb) else None,
        strasse = fields(athletHeader("strasse")),
        plz = fields(athletHeader("plz")),
        ort = fields(athletHeader("ort")),
        verein = vereinInstances.get(fields(athletHeader("verein"))).map(v => v.id),
        activ = fields(athletHeader("activ")).toUpperCase() match {
          case "TRUE" => true
          case _ => false
        }
      ))
    }
    val cache = new java.util.ArrayList[MatchCode]()
    val athletInstanceCandidates = mappedAthletes.map { imported =>
      val (csvId, importathlet) = imported
      val candidate = findAthleteLike(cache, exclusive = false)(importathlet)

      if candidate.id > 0 then {
        importathlet.gebdat match {
          case Some(d) =>
            candidate.gebdat match {
              case Some(cd) if !f"$cd%tF".endsWith("-01-01") || d.equals(cd) =>
                (csvId, candidate, false)
              case _ =>
                (csvId, candidate.copy(gebdat = importathlet.gebdat), true)
            }
          case None =>
            (csvId, candidate, false)
        }
      }
      else {
        (csvId, candidate, true)
      }
    }
    val athletInstances = (athletInstanceCandidates
      .filter(x => !x._3)
      .map { toInsert =>
        val (key, candidate, _) = toInsert
        (key, candidate)
      } ++ insertAthletes(athletInstanceCandidates.filter(x => x._3).map { toInsert =>
      val (key, candidate, _) = toInsert
      (key, candidate)
    }))
      .toMap

    val (wettkampfCsv, wettkampfHeader) = collection("wettkampf.csv")
    logger.info("importing wettkampf ...", wettkampfHeader)
    val wettkampfInstances = wettkampfCsv.map(DBService.parseLine).filter(_.size == wettkampfHeader.size).map { fields =>
      val uuid = wettkampfHeader.get("uuid").flatMap(uuidIdx => Some(fields(uuidIdx)))
      logger.info("wettkampf uuid: " + uuid)
      val wettkampf = createWettkampf(
        auszeichnung = fields(wettkampfHeader("auszeichnung")),
        auszeichnungendnote = try {
          BigDecimal.valueOf(fields(wettkampfHeader("auszeichnungendnote")))
        } catch {
          case e: Exception => 0
        },
        datum = fields(wettkampfHeader("datum")),
        programmId = Set(fields(wettkampfHeader("programmId"))),
        titel = fields(wettkampfHeader("titel")),
        notificationEMail = wettkampfHeader.get("notificationEMail").map(fields).getOrElse(""),
        altersklassen = wettkampfHeader.get("altersklassen").map(fields).getOrElse(""),
        jahrgangsklassen = wettkampfHeader.get("jahrgangsklassen").map(fields).getOrElse(""),
        punktegleichstandsregel = wettkampfHeader.get("punktegleichstandsregel").map(fields).getOrElse(""),
        rotation = wettkampfHeader.get("rotation").map(fields).getOrElse(""),
        teamrule = wettkampfHeader.get("teamrule").map(fields).getOrElse(""),
        uuidOption = uuid
      )

      (fields(wettkampfHeader("id")), wettkampf)
    }.toMap
    /*
        if (collection.contains("media.csv")) {
          val (mediaData, mediaHeader) = collection("media.csv")
          logger.info("importing medias ...", mediaHeader)
          saveOrUpdateMedias(mediaData.map(DBService.parseLine).filter(_.size == mediaHeader.size).map { fields =>
            MediaAdmin(
              id = fields(mediaHeader("id")),
              name= fields(mediaHeader("name")),
              extension= fields(mediaHeader("extension")),
              stage= fields(mediaHeader("stage")),
              metadata= fields(mediaHeader("metadata")),
              md5= fields(mediaHeader("md5")),
              stamp= fields(mediaHeader("stamp"))
            )
          })
        }
    */
    val wkdisziplines = wettkampfInstances.map { w =>
      (w._2.id, listWettkampfDisziplines(w._2.id).map(d => d.id -> d).toMap)
    }
    val (wertungenCsv, wertungenHeader) = collection("wertungen.csv")
    logger.info("importing wertungen ...", wertungenHeader)
    val wertungInstances = wertungenCsv.map(DBService.parseLine).filter(_.size == wertungenHeader.size).map { fields =>
      val athletid: Long = fields(wertungenHeader("athletId"))
      val wettkampfid: Long = fields(wertungenHeader("wettkampfId"))
      val w = Wertung(
        id = fields(wertungenHeader("id")),
        athletId = athletInstances.get(s"$athletid") match {
          case Some(a) => a.id
          case None => athletid
        },
        wettkampfdisziplinId = fields(wertungenHeader("wettkampfdisziplinId")),
        wettkampfId = wettkampfInstances.get(s"$wettkampfid") match {
          case Some(w) => w.id
          case None => wettkampfid
        },
        wettkampfUUID = wettkampfInstances.get(s"$wettkampfid") match {
          case Some(w) => w.uuid.getOrElse("")
          case None => ""
        },
        noteD = fields(wertungenHeader("noteD")) match {
          case "" => None
          case value => Some(value)
        },
        noteE = fields(wertungenHeader("noteE")) match {
          case "" => None
          case value => Some(value)
        },
        endnote = fields(wertungenHeader("endnote")) match {
          case "" => None
          case value => Some(value)
        },
        riege = if fields(wertungenHeader("riege")).nonEmpty then Some(fields(wertungenHeader("riege"))) else None,
        riege2 = if fields(wertungenHeader("riege2")).nonEmpty then Some(fields(wertungenHeader("riege2"))) else None,
        team = if wertungenHeader.contains("team") && fields(wertungenHeader("team")).nonEmpty then Some(fields(wertungenHeader("team"))) else None,
        mediafile = if wertungenHeader.contains("mediafile") && fields(wertungenHeader("mediafile")).nonEmpty then MediaJsonReader(Some(new String(dec.decode(fields(wertungenHeader("mediafile")))))) else None,
        variables = if wertungenHeader.contains("variables") && fields(wertungenHeader("variables")).nonEmpty then TemplateViewJsonReader(Some(new String(dec.decode(fields(wertungenHeader("variables")))))) else None
      )
      w
    }

    if collection.contains("scorecalc_templates.csv") then {
      val (scoreCalcTemplate, scoreCalcTemplateHeader) = collection("scorecalc_templates.csv")
      logger.info("importing scorecalc_templates ...", scoreCalcTemplateHeader)
      updateScoreCalcTemplates(scoreCalcTemplate.map(DBService.parseLine).filter(_.size == scoreCalcTemplateHeader.size).map { fields =>
        val wettkampfid = fields(scoreCalcTemplateHeader("wettkampfId"))
        val planTimeRaw = ScoreCalcTemplate(
          id = 0L,
          wettkampfId = wettkampfInstances.get(wettkampfid + "") match {
            case Some(w) => Some(w.id)
            case None => Some(wettkampfid)
          },
          wettkampfdisziplinId = fields(scoreCalcTemplateHeader("wettkampfdisziplinId")) match {
            case "" => None
            case value => Some(BigDecimal(value).toLong)
          },
          disziplinId = fields(scoreCalcTemplateHeader("disziplinId")) match {
            case "" => None
            case value => Some(BigDecimal(value).toLong)
          },
          dFormula = fields(scoreCalcTemplateHeader("dFormula")),
          eFormula = fields(scoreCalcTemplateHeader("eFormula")),
          pFormula = fields(scoreCalcTemplateHeader("pFormula")),
          aggregateFn = fields(scoreCalcTemplateHeader("aggregateFn")) match {
            case "" => None
            case value => ScoreAggregateFn(Some(value))
          },
        )
        planTimeRaw
      })
    }

    val start = System.currentTimeMillis()
    wertungInstances.flatMap(rt => rt.mediafile).toSet.foreach(m => putMedia(m))
    val inserted = wertungInstances.groupBy(w => w.wettkampfId).map { wkWertungen =>
      val (wettkampfid, wertungen) = wkWertungen
      val wettkampf = wettkampfInstances.values.find(w => w.id == wettkampfid).get
      val wkDisziplines = wkdisziplines(wettkampf.id)
      updateOrinsertWertungenZuWettkampf(wettkampf, wertungen.groupBy(w => w.athletId).flatMap { aw =>
        val (athletid, wertungen) = aw
        val programms = wertungen.map(wertung => wkDisziplines(wertung.wettkampfdisziplinId).programmId).toSet

        val requiredDisciplines = wkdisziplines(wettkampf.id).filter(wd => programms.contains(wd._2.programmId))
        val filtered = wertungen.filter(w => requiredDisciplines.contains(w.wettkampfdisziplinId))
        wertungen.filter(!filtered.contains(_)).foreach(w => logger.debug("WARNING: No matching Disciplin - " + w))

        val missing = requiredDisciplines.keys.filter(d => !filtered.exists(w => w.wettkampfdisziplinId == d))
        missing.foreach(w => logger.debug("WARNING: missing Disciplin - " + requiredDisciplines(w).easyprint))
        val completeWertungenSet = filtered ++ missing.map { missingDisziplin =>
          Wertung(
            id = 0L,
            athletId = athletid,
            wettkampfdisziplinId = missingDisziplin,
            wettkampfId = wettkampf.id,
            wettkampfUUID = wettkampfInstances.get(s"${wettkampf.id}") match {
              case Some(w) => w.uuid.getOrElse("")
              case None => ""
            },
            noteD = None,
            noteE = None,
            endnote = None,
            riege = None,
            riege2 = None,
            team = None,
            mediafile = None,
            variables = None
          )
        }
        completeWertungenSet
      })
    }.sum
    // wertungen: 1857 / inserted: 1857, duration: 6335ms
    logger.debug(s"wertungen: ${wertungInstances.size} / inserted: $inserted, duration: ${System.currentTimeMillis() - start}ms")

    if collection.contains("riegen.csv") then {
      val (riegenCsv, riegenHeader) = collection("riegen.csv")
      logger.info("importing riegen ...", riegenHeader)
      updateOrinsertRiegen(riegenCsv.map(DBService.parseLine).filter(_.size == riegenHeader.size).map { fields =>
        val wettkampfid = fields(riegenHeader("wettkampfId"))
        val riege = RiegeRaw(
          wettkampfId = wettkampfInstances.get(wettkampfid + "") match {
            case Some(w) => w.id
            case None => wettkampfid
          },
          r = fields(riegenHeader("r")),
          durchgang = if fields(riegenHeader("durchgang")).nonEmpty then Some(fields(riegenHeader("durchgang"))) else None,
          start = if fields(riegenHeader("start")).nonEmpty then Some(fields(riegenHeader("start"))) else None,
          kind = getValue(riegenHeader, fields, "kind", "0")
        )
        riege
      })
    }

    if collection.contains("plan_times.csv") then {
      val (planTimesCsv, planTimesHeader) = collection("plan_times.csv")
      logger.info("importing plan times ...", planTimesHeader)
      updateOrInsertPlanTimes(planTimesCsv.map(DBService.parseLine).filter(_.size == planTimesHeader.size).map { fields =>
        val wettkampfid = fields(planTimesHeader("wettkampfId"))
        val planTimeRaw = WettkampfPlanTimeRaw(
          id = 0L,
          wettkampfId = wettkampfInstances.get(wettkampfid + "") match {
            case Some(w) => w.id
            case None => wettkampfid
          },
          wettkampfDisziplinId = fields(planTimesHeader("wettkampfDisziplinId")),
          wechsel = fields(planTimesHeader("wechsel")),
          einturnen = fields(planTimesHeader("einturnen")),
          uebung = fields(planTimesHeader("uebung")),
          wertung = fields(planTimesHeader("wertung"))
        )
        planTimeRaw
      })
    }

    if collection.contains("durchgaenge.csv") then {
      import java.time.format.DateTimeFormatter
      import java.util.TimeZone
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
        .withZone(TimeZone.getTimeZone("UTC").toZoneId)
      val (durchgangCsv, durchgangHeader) = collection("durchgaenge.csv")
      logger.info("importing durchgaenge ...", durchgangHeader)
      updateOrInsertDurchgaenge(durchgangCsv.map(DBService.parseLine).filter(_.size == durchgangHeader.size).map { fields =>
        val wettkampfid = fields(durchgangHeader("wettkampfId"))

        implicit def toTS(tsString: String): Option[Timestamp] = tsString match {
          case s if s.isEmpty => None
          case _ =>
            val paddedTs = s"${tsString}000000".substring(0, 26)
            Timestamp.from(Instant.from(formatter.parse(paddedTs))) match {
              case ts: Timestamp if ts.getTime == 0 => None
              case ts => Some(ts)
            }
        }

        val durchgang = Durchgang(
          id = 0L,
          wettkampfId = wettkampfInstances.get(wettkampfid + "") match {
            case Some(w) => w.id
            case None => wettkampfid
          },
          title = fields(durchgangHeader("title")),
          name = fields(durchgangHeader("name")),
          durchgangtype = DurchgangType(fields(durchgangHeader("durchgangtype"))),
          ordinal = fields(durchgangHeader("ordinal")),
          planStartOffset = fields(durchgangHeader("planStartOffset")),
          effectiveStartTime = if fields(durchgangHeader("effectiveStartTime")).nonEmpty then fields(durchgangHeader("effectiveStartTime")) else None,
          effectiveEndTime = if fields(durchgangHeader("effectiveEndTime")).nonEmpty then fields(durchgangHeader("effectiveEndTime")) else None,
        )
        durchgang
      })
    }

    if collection.contains("scoredefs.csv") then {
      val (scoredefsCsv, scoreDefHeader) = collection("scoredefs.csv")
      logger.info("importing scoredefs ...", scoreDefHeader)
      updateOrinsertScoreDefs(scoredefsCsv.map(DBService.parseLine).filter(_.size == scoreDefHeader.size).map { fields =>
        val wettkampfid = fields(scoreDefHeader("wettkampfId"))
        PublishedScoreRaw(
          wettkampfId = wettkampfInstances.get(wettkampfid + "") match {
            case Some(w) => w.id
            case None => wettkampfid
          },
          id = fields(scoreDefHeader("id")),
          title = fields(scoreDefHeader("title")),
          query = fields(scoreDefHeader("query")),
          published = fields(scoreDefHeader("published")).toBoolean,
          publishedDate = fields(scoreDefHeader("publishedDate"))
        )
      })
    }

    if wettkampfInstances.values.size == 1 then {
      val wettkampf = wettkampfInstances.values.head

      new ZipEntryTraversableClass().foreach { entry =>
        if entry._1.getName.startsWith(".at") && entry._1.getName.contains(Config.remoteHostOrigin) && !wettkampf.hasSecred(Config.homedir, Config.remoteHostOrigin) then {
          val filename = entry._1.getName
          if !wettkampf.hasSecred(Config.homedir, Config.remoteHostOrigin) then {
            wettkampf.saveSecret(Config.homedir, Config.remoteHostOrigin, Source.fromInputStream(entry._2, "utf-8").mkString)
            logger.info("secret was written " + filename)
          }
        }
      }
      new ZipEntryTraversableClass().foreach { entry =>
        if entry._1.getName.startsWith(".from") && entry._1.getName.contains(Config.remoteHostOrigin) && !wettkampf.hasRemote(Config.homedir, Config.remoteHostOrigin) then {
          val filename = entry._1.getName
          if !wettkampf.hasRemote(Config.homedir, Config.remoteHostOrigin) then {
            wettkampf.saveRemoteOrigin(Config.homedir, Config.remoteHostOrigin)
            logger.info("remote-info was written " + filename)
          }
        }
      }
      new ZipEntryTraversableClass().foreach { entry =>
        if entry._1.getName.endsWith(".scoredef") then {
          val filename = entry._1.getName
          val wettkampfDir = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint))
          if !wettkampfDir.exists() then {
            wettkampfDir.mkdir()
          }
          val scoredefFile = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint) + "/" + filename)
          val fos = new FileOutputStream(scoredefFile)
          val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
          Iterator
            .continually(entry._2.read(bytes))
            .takeWhile(b => -1 != b)
            .foreach(read => fos.write(bytes, 0, read))
          fos.flush()
          fos.close()
          logger.info("scoredef-file was written " + scoredefFile.getName)
        }
      }
      new ZipEntryTraversableClass().foreach { entry =>
        if entry._1.getName.startsWith("logo") then {
          val filename = entry._1.getName
          if entry._1.getSize > Config.logoFileMaxSize then {
            val maxSize = java.text.NumberFormat.getInstance().format(Config.logoFileMaxSize / 1024d)
            val currentSize = java.text.NumberFormat.getInstance().format(entry._1.getSize / 1024d)
            throw new RuntimeException(s"Die Datei $filename ist mit $currentSize zu gross. Sie darf nicht grösser als $maxSize Kilobytes sein.")
          }
          val logodir = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint))
          if !logodir.exists() then {
            logodir.mkdir()
          }
          val logofile = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint) + "/" + filename)
          val fos = new FileOutputStream(logofile)
          val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
          Iterator
            .continually(entry._2.read(bytes))
            .takeWhile(b => -1 != b)
            .foreach(read => fos.write(bytes, 0, read))
          fos.flush()
          fos.close()
          logger.info("logo was written " + logofile.getName)
        }
      }
      new ZipEntryTraversableClass()
        .filter(entry => entry._1.getName.toLowerCase.contains(".mp3")) //startsWith("/audiofiles/"))
        .foreach { entry =>
          val keys = (entry._1.getName + "@0").split("@")
          val filename = keys(0)
          val mediaId = keys(1)
          val media: Media = searchMedia(mediaId)
            .map(_.toMedia)
            .getOrElse(
              searchMedia(filename)
                .map(_.toMedia)
                .getOrElse(
                  Media("", filename, filename.substring(filename.lastIndexOf(".")))))

          /*if (entry._1.getSize > Config.mediafileMaxSize) {
            val maxSize = java.text.NumberFormat.getInstance().format(Config.mediafileMaxSize / 1024d)
            val currentSize = java.text.NumberFormat.getInstance().format(entry._1.getSize / 1024d)
            throw new RuntimeException(s"Die Datei $filename ist mit $currentSize zu gross. Sie darf nicht grösser als $maxSize Kilobytes sein.")
          }*/
          saveMediaFile(entry._2, wettkampf, media)
        }
    }

    logger.info("import finished")
    wettkampfInstances.head._2
  }

  def moveAll(source: jdbc.JdbcBackend.Database, target: jdbc.JdbcBackend.Database): Unit = {
    try {
      DBService.startDB(Some(source))
      val wettkampfliste = listWettkaempfeView
      for
        wk <- wettkampfliste
      do {
        val copyStream = new CopyStream(100000)
        DBService.startDB(Some(source))
        exportWettkampfToStream(wk.toWettkampf, copyStream)
        DBService.startDB(Some(target))
        importWettkampf(copyStream.toInputStream)
      }
    } finally {
      DBService.startDB(Some(target))
    }
  }

  def exportWettkampfMediaFiles(wettkampf: Wettkampf, filename: String): Unit = {
    exportWettkampfMediaFilesToStream(wettkampf, List.empty, new FileOutputStream(filename))
  }

  private def zipMediaFiles(wettkampf: Wettkampf, medialist: List[Media], zip: ZipOutputStream): Unit = {
    val competitionMediaDir = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint) + "/audiofiles")
    if competitionMediaDir.exists() && competitionMediaDir.isDirectory then {
      competitionMediaDir
        .listFiles()
        .filter(f => f.getName.toLowerCase.endsWith(".mp3")) //startsWith("/audiofiles/")) //
        .groupBy { mp3File =>
          searchMedia(mp3File.getName)
        }
        .filter(_._1.exists { m => medialist.isEmpty || medialist.exists(me => me.id.equals(m.id)) })
        .foreach { group =>
          val (Some(media), mp3Files) = group: @unchecked
          val mp3File = mp3Files.head
          if mp3File.length() > Config.mediafileMaxSize then {
            val maxSize = java.text.NumberFormat.getInstance().format(Config.mediafileMaxSize / 1024d)
            val currentSize = java.text.NumberFormat.getInstance().format(mp3File.length() / 1024d)
            throw new RuntimeException(s"Die Datei ${mp3File.getName} ist mit $currentSize zu gross. Sie darf nicht grösser als $maxSize Kilobytes sein.")
          }
          val name = s"${media.filename}@${media.id}@${media.extension}"
          val fis = new FileInputStream(mp3File)
          zip.putNextEntry(new ZipEntry(name))
          val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
          Iterator
            .continually(fis.read(bytes))
            .takeWhile(b => -1 != b)
            .foreach(read => zip.write(bytes, 0, read))
          zip.closeEntry()
          logger.info(s"mp3-file ${mp3File.getName} was taken as $name")
        }
    }
  }

  def exportWettkampfMediaFilesToStream(wettkampf: Wettkampf, medialist: List[Media], os: OutputStream): Unit = {
    val zip = new ZipOutputStream(os)
    zipMediaFiles(wettkampf, medialist, zip)
    zip.finish()
    zip.close()
  }

  def exportWettkampf(wettkampf: Wettkampf, filename: String): Unit = {
    exportWettkampfToStream(wettkampf, new FileOutputStream(filename), withSecret = true, withMediaFiles = true)
  }

  def exportWettkampfToStream(wettkampf: Wettkampf, os: OutputStream, withSecret: Boolean = false, withMediaFiles: Boolean = false): Unit = {
    val zip = new ZipOutputStream(os)
    zip.putNextEntry(new ZipEntry("wettkampf.csv"))
    zip.write((getHeader[Wettkampf] + "\n" + getValues(wettkampf)).getBytes("utf-8"))
    zip.closeEntry()

    val wertungen = selectWertungen(wettkampfId = Some(wettkampf.id))

    val vereine = wertungen.flatMap(_.athlet.verein).toSet
    zip.putNextEntry(new ZipEntry("vereine.csv"))
    zip.write((getHeader[Verein] + "\n").getBytes("utf-8"))
    for verein <- vereine do {
      zip.write((getValues(verein) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val athleten = wertungen.map(_.athlet).toSet
    zip.putNextEntry(new ZipEntry("athleten.csv"))
    zip.write((getHeader[Athlet] + "\n").getBytes("utf-8"))
    for athlet <- athleten do {
      zip.write((getValues(athlet) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()
    /*
        zip.putNextEntry(new ZipEntry("media.csv"))
        zip.write((getHeader[MediaAdmin] + "\n").getBytes("utf-8"))
        for (media <- wertungen.flatMap(_.mediafile).map(m => loadMedia(m.id))) {
          zip.write((getValues(media) + "\n").getBytes("utf-8"))
        }
        zip.closeEntry()
    */
    val wertungenRaw = wertungen.map(_.toWertung)
    zip.putNextEntry(new ZipEntry("wertungen.csv"))
    zip.write((getHeader[Wertung] + "\n").getBytes("utf-8"))
    for wertung <- wertungenRaw do {
      zip.write((getValues(wertung) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val riegenRaw = selectRiegenRaw(wettkampf.id)
    zip.putNextEntry(new ZipEntry("riegen.csv"))
    zip.write((getHeader[RiegeRaw] + "\n").getBytes("utf-8"))
    for riege <- riegenRaw do {
      zip.write((getValues(riege) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val planTimes = loadWettkampfDisziplinTimes(using wettkampf.id)
    zip.putNextEntry(new ZipEntry("plan_times.csv"))
    zip.write((getHeader[WettkampfPlanTimeRaw] + "\n").getBytes("utf-8"))
    for planTime <- planTimes do {
      zip.write((getValues(planTime.toWettkampfPlanTimeRaw) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val templates = loadScoreCalcTemplates(wettkampf.id)
    zip.putNextEntry(new ZipEntry("scorecalc_templates.csv"))
    zip.write((getHeader[ScoreCalcTemplate] + "\n").getBytes("utf-8"))
    for scorecalctemplate <- templates do {
      zip.write((getValues(scorecalctemplate) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val durchgaenge = selectDurchgaenge(UUID.fromString(wettkampf.uuid.get))
    zip.putNextEntry(new ZipEntry("durchgaenge.csv"))
    zip.write((getHeader[Durchgang] + "\n").getBytes("utf-8"))
    for durchgang <- durchgaenge do {
      zip.write((getValues(durchgang) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val scores = Await.result(listPublishedScores(UUID.fromString(wettkampf.uuid.get)), Duration.Inf).map(sv => sv.toRaw)
    zip.putNextEntry(new ZipEntry("scoredefs.csv"))
    zip.write((getHeader[PublishedScoreRaw] + "\n").getBytes("utf-8"))
    for score <- scores do {
      zip.write((getValues(score) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val competitionDir = new java.io.File(Config.homedir + "/" + encodeFileName(wettkampf.easyprint))

    val logofile = ServerPrintUtil.locateLogoFile(competitionDir)
    if logofile.exists() then {

      if logofile.length() > Config.logoFileMaxSize then {
        val maxSize = java.text.NumberFormat.getInstance().format(Config.logoFileMaxSize / 1024)
        val currentSize = java.text.NumberFormat.getInstance().format(logofile.length() / 1024)
        throw new RuntimeException(s"Die Datei ${logofile.getName} ist mit $currentSize Kilobytes zu gross. Sie darf nicht grösser als $maxSize Kilobytes sein.")
      }

      zip.putNextEntry(new ZipEntry(logofile.getName))
      val fis = new FileInputStream(logofile)
      val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
      Iterator
        .continually(fis.read(bytes))
        .takeWhile(b => -1 != b)
        .foreach(read => zip.write(bytes, 0, read))
      zip.closeEntry()
      logger.info("logo was taken " + logofile.getName)
    }
    // pick score-defs
    if competitionDir.exists() then {
      competitionDir
        .listFiles()
        .filter(f => f.getName.endsWith(".scoredef"))
        .toList
        .foreach { scoredefFile =>
          zip.putNextEntry(new ZipEntry(scoredefFile.getName))
          val fis = new FileInputStream(scoredefFile)
          val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
          Iterator
            .continually(fis.read(bytes))
            .takeWhile(b => -1 != b)
            .foreach(read => zip.write(bytes, 0, read))
          zip.closeEntry()
          logger.info("scoredef-file was taken " + scoredefFile.getName)
        }
    }
    if withSecret && wettkampf.hasSecred(Config.homedir, Config.remoteHostOrigin) then {
      val secretfile = wettkampf.filePath(Config.homedir, Config.remoteHostOrigin).toFile
      zip.putNextEntry(new ZipEntry(secretfile.getName))
      val fis = new FileInputStream(secretfile)
      val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
      Iterator
        .continually(fis.read(bytes))
        .takeWhile(b => -1 != b)
        .foreach(read => zip.write(bytes, 0, read))
      zip.closeEntry()
      logger.info("secret was taken " + secretfile.getName)
    }
    if withSecret && wettkampf.hasRemote(Config.homedir, Config.remoteHostOrigin) then {
      val secretfile = wettkampf.fromOriginFilePath(Config.homedir, Config.remoteHostOrigin).toFile
      zip.putNextEntry(new ZipEntry(secretfile.getName))
      val fis = new FileInputStream(secretfile)
      val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
      Iterator
        .continually(fis.read(bytes))
        .takeWhile(b => -1 != b)
        .foreach(read => zip.write(bytes, 0, read))
      zip.closeEntry()
      logger.info("remote-info was taken " + secretfile.getName)
    }
    if withMediaFiles then {
      zipMediaFiles(wettkampf, List.empty, zip)
    }
    zip.finish()
    zip.close()
  }

  inline def getHeader[T](using m: Mirror.ProductOf[T]): String =
    constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]].map(fieldname => "\"" + fieldname + "\"").mkString(",")

  def getValues[T <: Product](instance: T)(using m: Mirror.ProductOf[T]): String = {
    val caseValues = getCaseValues(instance)
    val values = caseValues.collect {
      case Some(verein: Verein) => s"${verein.id}"
      case Some(wk: Wettkampf) => s"${wk.id}"
      case Some(programm: Programm) => s"${programm.id}"
      case Some(athlet: Athlet) => s"${athlet.id}"
      case Some(athlet: AthletView) => s"${athlet.id}"
      case Some(disziplin: Disziplin) => s"${disziplin.id}"
      case Some(media: Media) => enc.encodeToString(mediaFormat.write(media).compactPrint.getBytes)
      //case Some(media: MediaAdmin) => enc.encodeToString(mediaAdminFormat.write(media).compactPrint.getBytes)
      case Some(template: ScoreCalcTemplateView) => enc.encodeToString(scoreCalcTemplateViewFormat.write(template).compactPrint.getBytes)
      case Some(ts: Timestamp) =>
        import java.time.format.DateTimeFormatter
        import java.util.TimeZone
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
        formatter = formatter.withZone(TimeZone.getTimeZone("UTC").toZoneId)
        formatter.format(ts.toInstant)
      case Some(value) => value.toString
      case None => ""
      case null => ""
      case e => e.toString
    }
    values.map("\"" + _ + "\"").mkString(",")
  }

  private val charset = "ISO-8859-1"
  private val charset2 = "UTF-8"
  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")

  def exportDurchgaenge(wettkampf: Wettkampf, filename: String): Unit = {
    val fileOutputStream = new BufferedOutputStream(new FileOutputStream(filename))
    fileOutputStream.write(0xef); // emits 0xef
    fileOutputStream.write(0xbb); // emits 0xbb
    fileOutputStream.write(0xbf); // emits 0xbf
    val diszipline = listDisziplinesZuWettkampf(wettkampf.id)
    val durchgaenge = selectDurchgaenge(UUID.fromString(wettkampf.uuid.get)).map(d => d.name -> d).toMap
    val sep = ";"
    fileOutputStream.write(f"""sep=$sep\nDurchgang${sep}Summe${sep}Min${sep}Max${sep}Durchschn.${sep}Total-Zeit${sep}Einturn-Zeit${sep}Gerät-Zeit""".getBytes(charset))

    diszipline.foreach { x =>
      fileOutputStream.write(f"$sep${x.name}${sep}Anz".getBytes(charset))
    }
    fileOutputStream.write("\r\n".getBytes(charset))

    val durchgangEditors = listRiegenZuWettkampf(wettkampf.id)
      .filter(_._3.nonEmpty)
      .sortBy(r => r._1)
      .map { x =>
        RiegeEditor(
          wettkampf.id,
          x._1,
          x._2,
          0,
          enabled = true,
          x._3,
          x._4,
          None)
      }
      .groupBy(re => re.initdurchgang).toSeq
      .sortBy(re => re._1)
      .flatMap { res =>
        val (durchgang, rel) = res
        durchgaenge.get(durchgang.getOrElse(rel.head.initdurchgang.getOrElse(""))).map(dg => DurchgangEditor(wettkampf.id, dg, rel))
      }
      .toList

    def writeDurchgangRow(x: DurchgangEditor): Unit = {
      val dgtext = if x.durchgang.planStartOffset != 0 && x.durchgang.name.equals(x.durchgang.title) then {
        s"${x.durchgang.name}\nStart: ${x.durchgang.effectivePlanStart(wettkampf.datum.toLocalDate).format(formatter)}\nEnde: ${x.durchgang.effectivePlanFinish(wettkampf.datum.toLocalDate).format(formatter)}"
      }
      else {
        x.durchgang.name
      }
      fileOutputStream.write(f"""$dgtext$sep${x.anz.value}$sep${x.min.value}$sep${x.max.value}$sep${x.avg.value}$sep${toShortDurationFormat(x.durchgang.planTotal)}$sep${toShortDurationFormat(x.durchgang.planEinturnen)}$sep${toShortDurationFormat(x.durchgang.planGeraet)}""".getBytes(charset))
      diszipline.foreach { d =>
        fileOutputStream.write(f"$sep${x.initstartriegen.getOrElse(d, Seq[RiegeEditor]()).map(r => f"${r.name.value.replace("M,", "Tu,").replace("W,", "Ti,")} (${r.anz.value})").mkString("\"", "\n", "\"")}$sep${x.initstartriegen.getOrElse(d, Seq[RiegeEditor]()).map(r => r.anz.value).sum}".getBytes(charset))
      }
      fileOutputStream.write("\r\n".getBytes(charset))
    }

    for group <- DurchgangEditor(durchgangEditors) do {
      group match {
        case x: GroupDurchgangEditor =>
          val dgtext = if x.durchgang.planStartOffset != 0 then {
            s"${x.durchgang.title}\nStart: ${x.durchgang.effectivePlanStart(wettkampf.datum.toLocalDate).format(formatter)}\nEnde: ${x.durchgang.effectivePlanFinish(wettkampf.datum.toLocalDate).format(formatter)}"
          }
          else {
            x.durchgang.title
          }
          fileOutputStream.write(f"""$dgtext$sep${x.anz.value}$sep${x.min.value}$sep${x.max.value}$sep${x.avg.value}$sep${toShortDurationFormat(x.durchgang.planTotal)}$sep${toShortDurationFormat(x.durchgang.planEinturnen)}$sep${toShortDurationFormat(x.durchgang.planGeraet)}""".getBytes(charset))
          fileOutputStream.write("\r\n".getBytes(charset))
          x.aggregates.foreach(x => writeDurchgangRow(x))
        case x: DurchgangEditor =>
          writeDurchgangRow(x)
      }
    }
    fileOutputStream.flush()
    fileOutputStream.close()
  }

  def exportSimpleDurchgaenge(wettkampf: Wettkampf, filename: String): Unit = {
    val fileOutputStream = new BufferedOutputStream(new FileOutputStream(filename))
    val diszipline = listDisziplinesZuWettkampf(wettkampf.id)
    val durchgaenge = selectDurchgaenge(UUID.fromString(wettkampf.uuid.get)).map(d => d.name -> d).toMap

    val sep = ";"
    fileOutputStream.write(f"""sep=$sep\nDurchgang${sep}Summe${sep}Min${sep}Max${sep}Total-Zeit${sep}Einturn-Zeit${sep}Gerät-Zeit""".getBytes(charset))

    diszipline.foreach { x =>
      fileOutputStream.write(f"$sep${x.name}${sep}Ti${sep}Tu".getBytes(charset))
    }
    fileOutputStream.write("\r\n".getBytes(charset))

    val riege2Map = listRiegen2ToRiegenMapZuWettkampf(wettkampf.id)

    val allRiegen = listRiegenZuWettkampf(wettkampf.id)
      .filter(_._3.nonEmpty)
      .sortBy(r => r._1)
      .map(x =>
        RiegeEditor(
          wettkampf.id,
          x._1,
          x._2,
          0,
          enabled = true,
          x._3,
          x._4,
          None))
    val allRiegenIndex = allRiegen.map(r => r.initname -> r).toMap
    val durchgangEditors = allRiegen
      .flatMap(riege => {
        riege2Map.get(riege.initname) match {
          case Some(barrenRiegen) => barrenRiegen
            .map(allRiegenIndex)
            .map(br => br.copy(initstart = riege.initstart, initdurchgang = riege.initdurchgang))
          case None => List(riege)
        }
      })
      .groupBy(re => re.initdurchgang).toSeq
      .sortBy(re => re._1)
      .flatMap { res =>
        val (name, rel) = res
        durchgaenge.get(name.getOrElse(rel.head.initdurchgang.getOrElse(""))).map(dg => DurchgangEditor(wettkampf.id, dg, rel))
      }
      .toList

    def writeDurchgangRow(x: DurchgangEditor): Unit = {
      val dgtext = if x.durchgang.planStartOffset != 0 && x.durchgang.name.equals(x.durchgang.title) then {
        s"${x.durchgang.name}\nStart: ${x.durchgang.effectivePlanStart(wettkampf.datum.toLocalDate).format(formatter)}\nEnde: ${x.durchgang.effectivePlanFinish(wettkampf.datum.toLocalDate).format(formatter)}"
      }
      else {
        x.durchgang.name
      }
      fileOutputStream.write(f"""$dgtext$sep${x.anz.value}$sep${x.min.value}$sep${x.max.value}$sep${toShortDurationFormat(x.durchgang.planTotal)}$sep${toShortDurationFormat(x.durchgang.planEinturnen)}$sep${toShortDurationFormat(x.durchgang.planGeraet)}""".getBytes(charset))
      val riegen = x.riegenWithMergedClubs()
      val rows = riegen.values.map(_.size).max
      val riegenFields = for {
        row <- 0 to rows
        d <- diszipline
        r = riegen.getOrElse(d, Seq())
      } yield {
        (row, if r.size > row then {
          f"$sep${r(row)._1}$sep${r(row)._2}$sep${r(row)._3}"
        } else {
          f"$sep$sep$sep"
        })
      }
      val rs = riegenFields.groupBy(_._1).toList
        .sortBy(_._1)
        .map(r => {
          val tuples = r._2
          val strings = tuples.map(_._2)
          val fieldsString = strings.mkString("", "", f"\r\n$sep$sep$sep$sep$sep$sep")
          fieldsString
        }) :+ "\r\n"

      rs.foreach(row => {
        fileOutputStream.write(row.getBytes(charset))
      })
      fileOutputStream.write("\r\n".getBytes(charset))
    }

    for group <- DurchgangEditor(durchgangEditors) do {
      group match {
        case x: GroupDurchgangEditor =>
          val dgtext = if x.durchgang.planStartOffset != 0 then {
            s"${x.durchgang.title}\nStart: ${x.durchgang.effectivePlanStart(wettkampf.datum.toLocalDate).format(formatter)}\nEnde: ${x.durchgang.effectivePlanFinish(wettkampf.datum.toLocalDate).format(formatter)}"
          }
          else {
            x.durchgang.title
          }
          fileOutputStream.write(f"""$dgtext$sep${x.anz.value}$sep${x.min.value}$sep${x.max.value}$sep${toShortDurationFormat(x.durchgang.planTotal)}$sep${toShortDurationFormat(x.durchgang.planEinturnen)}$sep${toShortDurationFormat(x.durchgang.planGeraet)}""".getBytes(charset))
          fileOutputStream.write("\r\n".getBytes(charset))
          x.aggregates.foreach(x => writeDurchgangRow(x))
        case x: DurchgangEditor =>
          writeDurchgangRow(x)
      }
    }

    fileOutputStream.flush()
    fileOutputStream.close()
  }
}