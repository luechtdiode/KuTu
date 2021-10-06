package ch.seidel.kutu.data

import ch.seidel.kutu.akka._
import ch.seidel.kutu.data.CaseObjectMetaUtil._
import ch.seidel.kutu.domain._
import ch.seidel.kutu.renderer.PrintUtil
import ch.seidel.kutu.squad.RiegenBuilder
import ch.seidel.kutu.view._
import ch.seidel.kutu.{Config, KuTuApp}
import org.slf4j.LoggerFactory
import slick.jdbc

import java.io._
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.reflect.runtime.universe._

/**
 */
object ResourceExchanger extends KutuService with RiegenBuilder {
  private val logger = LoggerFactory.getLogger(this.getClass)
  def processWSMessage[T](wettkampf: Wettkampf, refresher: (Option[T], KutuAppEvent)=>Unit) = {
    val cache = new java.util.ArrayList[MatchCode]()
    def mapToLocal(athlet: AthletView) = {
      val mappedverein = athlet.verein match {
        case Some(v) => findVereinLike(Verein(id = 0, name = v.name, verband = None))
        case _ => None
      }
      val mappedAthlet = findAthleteLike(cache, Some(wettkampf.id))(athlet.toAthlet.copy(id = 0, verein = mappedverein))
      val mappedAthletView = athlet.updatedWith(mappedAthlet)
      mappedAthletView
    }

    def opFn: (Option[T], KutuAppEvent)=>Unit = {
      case (sender, LastResults(results)) =>
        val mappedWertungen: Seq[AthletWertungUpdatedSequenced] = results.groupBy(_.athlet).flatMap{ tuple =>
          val (athlet, wertungen) = tuple
          val mappedAthletView: AthletView = mapToLocal(athlet)
          wertungen.groupBy(_.wertung.wettkampfdisziplinId).map{_._2.sortBy(_.sequenceId).last}.map{ updatedSequenced =>
            val programm = updatedSequenced.programm
            val mappedWertung = updatedSequenced.wertung.copy(
                athletId = mappedAthletView.id,
                wettkampfId = wettkampf.id,
                wettkampfUUID = updatedSequenced.wettkampfUUID)
            logger.info(s"received for ${athlet.vorname} ${athlet.name} (${athlet.verein.getOrElse(() => "")}) " +
              s"im Pgm $programm new Wertung: D:${mappedWertung.noteD}, E:${mappedWertung.noteE}")
            updatedSequenced.copy(athlet = athlet, wertung = mappedWertung)
          }
        }.toSeq
        try {
          KuTuApp.invokeWithBusyIndicator {
            refresher(sender, LastResults(
              mappedWertungen.zip(updateWertungWithIDMapping(mappedWertungen.map(_.wertung))).map {
                x => x._1.copy(wertung = x._2)
              }.toList))
          }
        } catch {
          case e: Exception =>
            logger.error(s"failed to complete save LastResults ", e)
        }
      case (sender, bulkEvent @ BulkEvent(wettkampfUUID, events)) =>
        if (!Config.isLocalHostServer() && wettkampf.uuid.contains(wettkampfUUID)) {
          events.foreach {
            case ds:DurchgangStarted => storeDurchgangStarted(ds)
            case df:DurchgangFinished => storeDurchgangFinished(df)
            case _ =>
          }
          refresher(sender, bulkEvent)
        }
      case (sender, ds @ DurchgangStarted(wettkampfUUID, _, _)) =>
        if (!Config.isLocalHostServer() && wettkampf.uuid.contains(wettkampfUUID)) {
          storeDurchgangStarted(ds)
        }
        refresher(sender, ds)

      case (sender, df @ DurchgangFinished(wettkampfUUID, _, _)) =>
        if (!Config.isLocalHostServer() && wettkampf.uuid.contains(wettkampfUUID)) {
          storeDurchgangFinished(df)
        }
        refresher(sender, df)

      case (sender, uws: AthletWertungUpdatedSequenced) => opFn(sender, uws.toAthletWertungUpdated())
      case (sender, uw @ AthletWertungUpdated(athlet, wertung, wettkampfUUID, _, _, programm)) =>
        if (Config.isLocalHostServer()) {
          refresher(sender, uw)
        } else if (wettkampf.uuid.contains(wettkampfUUID)) /*Future*/ {
          logger.info(s"received for ${uw.athlet.vorname} ${uw.athlet.name} (${uw.athlet.verein.getOrElse(()=>"")}) " +
            s"im Pgm $programm new Wertung: D:${wertung.noteD}, E:${wertung.noteE}")
          val mappedAthletView: AthletView = mapToLocal(athlet)
          val mappedWertung = wertung.copy(athletId = mappedAthletView.id, wettkampfId = wettkampf.id, wettkampfUUID = wettkampfUUID)
          try {
            val vw = updateWertungWithIDMapping(mappedWertung)
            logger.info(s"saved for ${mappedAthletView.vorname} ${mappedAthletView.name} (${uw.athlet.verein.getOrElse(()=>"")}) " +
              s"im Pgm $programm new Wertung: D:${vw.noteD}, E:${vw.noteE}")
            refresher(sender, uw.copy(athlet.copy(id = mappedAthletView.id), wertung = vw))
          } catch {
            case e: Exception =>
              logger.error(s"failed to complete save new score for " +
                s"${mappedAthletView.vorname} ${mappedAthletView.name} (${mappedAthletView.verein.getOrElse("")}) " +
                s"im Pgm $programm new Wertung: D:${mappedWertung.noteD}, E:${mappedWertung.noteE}", e)
              refresher(sender, uw)
          }
        }
      case (sender, scorePublished @ScoresPublished(scoreId: String, title: String, query: String, published: Boolean, wettkampfUUID: String)) =>
        if (wettkampf.uuid.contains(wettkampfUUID)) /*Future*/ {
          logger.info(s"received ${scorePublished}")
          updatePublishedScore(wettkampf.id, scoreId, title, query, published, false)
          refresher(sender, scorePublished)
        }
      case (sender, awm @AthletMovedInWettkampf(athlet, wettkampfUUID, programm)) =>
        if (wettkampf.uuid.contains(wettkampfUUID)) /*Future*/ {
          logger.info(s"received for ${awm.athlet.vorname} ${awm.athlet.name} (${awm.athlet.verein.getOrElse(() => "")}) " +
            s"to be moved in competition ${awm.wettkampfUUID} to Program-Id:${programm}")
          val mappedAthletView: AthletView = mapToLocal(athlet)
          val mappedEvent = awm.copy(athlet = mappedAthletView)
          for(durchgang <- moveToProgram(mappedEvent)) {
            logger.info(s"durchgang $durchgang changed competition ${wettkampfUUID}")
            refresher(sender, DurchgangChanged(durchgang, wettkampfUUID, mappedEvent.athlet))
          }
          logger.info(s"${mappedAthletView.vorname} ${mappedAthletView.name} (${mappedAthletView.verein.getOrElse(() => "")}) " +
            s"moved in competition ${awm.wettkampfUUID} to Program-Id:${awm.pgmId}")
          refresher(sender, mappedEvent)
        }
      case (sender, arw @AthletRemovedFromWettkampf(athlet, wettkampfUUID)) =>
        if (wettkampf.uuid.contains(wettkampfUUID)) /*Future*/ {
          logger.info(s"received for ${arw.athlet.vorname} ${arw.athlet.name} (${arw.athlet.verein.getOrElse(() => "")}) " +
            s"to be removed from competition ${arw.wettkampfUUID}")
          val mappedAthletView: AthletView = mapToLocal(athlet)
          val mappedEvent = arw.copy(athlet = mappedAthletView)
          for(durchgang <- unassignAthletFromWettkampf(mappedEvent)) {
            logger.info(s"durchgang $durchgang changed competition ${wettkampfUUID}")
            refresher(sender, DurchgangChanged(durchgang, wettkampfUUID, mappedEvent.athlet))
          }
          logger.info(s"${mappedAthletView.vorname} ${mappedAthletView.name} (${mappedAthletView.verein.getOrElse(() => "")}) " +
            s"removed from competition ${arw.wettkampfUUID}")
          refresher(sender, mappedEvent)

        }
      case (_, MessageAck(_)) => // ignore
      case (sender, someOther) => 
        refresher(sender, someOther)
    }
    
    opFn
  }

  def importWettkampf(file: InputStream) = {
    val buffer = new BufferedInputStream(file)
    buffer.mark(40*4096)
    type ZipStream = (ZipEntry,InputStream)
    class ZipEntryTraversableClass extends Iterator[ZipStream] {
      buffer.reset
      val zis = new ZipInputStream(buffer)
      var currentEntry: Option[ZipEntry] = None
      val zisIterator = Iterator.continually(zis.getNextEntry)
        .map(entry => {
          currentEntry match {
            case Some(entry) => zis.closeEntry()
            case _ =>
          }
          entry
        })
        .filter(ze => ze == null || !ze.isDirectory)
        .takeWhile(ze => ze != null).iterator

      override def hasNext: Boolean = zisIterator.hasNext
      override def next(): (ZipEntry, InputStream) = (zisIterator.next(), zis)
    }

    val collection = new ZipEntryTraversableClass().foldLeft(Map[String, (Seq[String],Map[String,Int])]()) { (acc, entry) =>
      if (entry._1.getName.endsWith(".csv")) {
        val csv = Source.fromInputStream(entry._2, "utf-8").getLines().toList
        val header = csv.take(1).map(_.dropWhile {_.isUnicodeIdentifierPart }).flatMap(DBService.parseLine).zipWithIndex.toMap
        acc + (entry._1.getName -> (csv.drop(1), header))
      } else acc
    }

    def getValue(header: Map[String, Int], fields: IndexedSeq[String], key: String, default: String): String = {
      if (header.contains(key) && fields(header(key)).length > 0) fields(header(key)) else default
    }

    val (vereinCsv, vereinHeader) = collection("vereine.csv")
    logger.info("importing vereine ...", vereinHeader)
    val vereinNameIdx = vereinHeader("name")
    val vereinVerbandIdx = vereinHeader.getOrElse("verband", -1)
    val vereinIdIdx = vereinHeader("id")
    val vereinInstances = vereinCsv.map(DBService.parseLine).filter(_.size == vereinHeader.size).map{fields =>
      val candidate = Verein(id = 0, name = fields(vereinNameIdx), verband = if(vereinVerbandIdx > -1) Some(fields(vereinVerbandIdx)) else None)
      val verein = insertVerein(candidate)
      (fields(vereinIdIdx), verein)
    }.toMap

    val (athletCsv, athletHeader) = collection("athleten.csv")
    logger.info("importing athleten ...", athletHeader)
    val mappedAthletes = athletCsv.map(DBService.parseLine).filter(_.size == athletHeader.size).map{fields =>
      val geb = fields(athletHeader("gebdat")).replace("Some(", "").replace(")","")
      (fields(athletHeader("id")), Athlet(
          id = 0,
          js_id = fields(athletHeader("js_id")),
          geschlecht = fields(athletHeader("geschlecht")),
          name = fields(athletHeader("name")),
          vorname = fields(athletHeader("vorname")),
          gebdat = if(geb.length > 0) Some(geb) else None,
          strasse = fields(athletHeader("strasse")),
          plz = fields(athletHeader("plz")),
          ort = fields(athletHeader("ort")),
          verein = vereinInstances.get(fields(athletHeader("verein"))).map(v => v.id),
          activ = fields(athletHeader("activ")).toUpperCase() match {case "TRUE" => true case _ => false}
          ))
    }
    val cache = new java.util.ArrayList[MatchCode]()
    val athletInstanceCandidates = mappedAthletes.map{imported =>
      val (csvId, importathlet) = imported
      val candidate = findAthleteLike(cache)(importathlet)

      if(candidate.id > 0) {
        importathlet.gebdat match {
        case Some(d) =>
          candidate.gebdat match {
            case Some(cd) if(!f"${cd}%tF".endsWith("-01-01") || d.equals(cd)) =>
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
      .map{toInsert =>
        val (key, candidate, _) = toInsert
        (key, candidate)
      } ++ insertAthletes(athletInstanceCandidates.filter(x => x._3).map{toInsert =>
        val (key, candidate, _) = toInsert
        (key, candidate)
      }))
      .toMap
      
    val (wettkampfCsv, wettkampfHeader) = collection("wettkampf.csv")
    logger.info("importing wettkampf ...", wettkampfHeader)
    val wettkampfInstances = wettkampfCsv.map(DBService.parseLine).filter(_.size == wettkampfHeader.size).map{fields =>
      val uuid = wettkampfHeader.get("uuid").map(uuidIdx => Some(fields(uuidIdx))).getOrElse(None)
      logger.info("wettkampf uuid: " + uuid)
      val wettkampf = createWettkampf(
          auszeichnung = fields(wettkampfHeader("auszeichnung")),
          auszeichnungendnote = try {BigDecimal.valueOf(fields(wettkampfHeader("auszeichnungendnote")))} catch {case e:Exception => 0},
          datum = fields(wettkampfHeader("datum")),
          programmId = Set(fields(wettkampfHeader("programmId"))),
          titel = fields(wettkampfHeader("titel")),
          uuidOption = uuid
          )
          
      new ZipEntryTraversableClass().foreach{entry =>
        if (entry._1.getName.startsWith("logo")) {
          val filename = entry._1.getName
          val logodir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          if (!logodir.exists()) {
            logodir.mkdir()
          }
          val logofile = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_") + "/" + filename)
          val fos = new FileOutputStream(logofile)
          val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
          Iterator
          .continually(entry._2.read(bytes))
          .takeWhile(-1 !=)
          .foreach(read=> fos.write(bytes, 0, read))
          fos.flush()
          fos.close()
          logger.info("logo was written " + logofile.getName)
        }
      }
      new ZipEntryTraversableClass().foreach{entry =>
        if (entry._1.getName.endsWith(".scoredef")) {
          val filename = entry._1.getName
          val wettkampfDir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))
          if (!wettkampfDir.exists()) {
            wettkampfDir.mkdir()
          }
          val scoredefFile = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_") + "/" + filename)
          val fos = new FileOutputStream(scoredefFile)
          val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
          Iterator
            .continually(entry._2.read(bytes))
            .takeWhile(-1 !=)
            .foreach(read=> fos.write(bytes, 0, read))
          fos.flush()
          fos.close()
          logger.info("scoredef-file was written " + scoredefFile.getName)
        }
      }
      new ZipEntryTraversableClass().foreach{entry =>
        if (entry._1.getName.startsWith(".at") && entry._1.getName.contains(Config.remoteHostOrigin) && !wettkampf.hasSecred(Config.homedir, Config.remoteHostOrigin)) {
          val filename = entry._1.getName
          if (!wettkampf.hasSecred(Config.homedir, Config.remoteHostOrigin)) {
            wettkampf.saveSecret(Config.homedir, Config.remoteHostOrigin, Source.fromInputStream(entry._2, "utf-8").mkString)
            logger.info("secret was written " + filename)
          }
        }
      }
      new ZipEntryTraversableClass().foreach{entry =>
        if (entry._1.getName.startsWith(".from") && entry._1.getName.contains(Config.remoteHostOrigin) && !wettkampf.hasRemote(Config.homedir, Config.remoteHostOrigin)) {
          val filename = entry._1.getName
          if (!wettkampf.hasRemote(Config.homedir, Config.remoteHostOrigin)) {
            wettkampf.saveRemoteOrigin(Config.homedir, Config.remoteHostOrigin)
            logger.info("remote-info was written " + filename)
          }
        }
      }
      (fields(wettkampfHeader("id")), wettkampf)
    }.toMap
    
    val wkdisziplines = wettkampfInstances.map{w =>
      (w._2.id, listWettkampfDisziplines(w._2.id).map(d => d.id -> d).toMap)
    }
    val (wertungenCsv, wertungenHeader) = collection("wertungen.csv")
    logger.info("importing wertungen ...", wertungenHeader)
    val wertungInstances = wertungenCsv.map(DBService.parseLine).filter(_.size == wertungenHeader.size).map{fields =>
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
        noteD = fields(wertungenHeader("noteD")) match {case "" => None case value => Some(value)},
        noteE = fields(wertungenHeader("noteE")) match {case "" => None case value => Some(value)},
        endnote = fields(wertungenHeader("endnote")) match {case "" => None case value => Some(value)},
        riege = if(fields(wertungenHeader("riege")).length > 0) Some(fields(wertungenHeader("riege"))) else None,
        riege2 = if(fields(wertungenHeader("riege2")).length > 0) Some(fields(wertungenHeader("riege2"))) else None
      )
//      println(w.athletId, getAthletName(w.athletId), w.endnote, w.wettkampfdisziplinId, w.wettkampfdisziplinId, getWettkampfDisziplinName(w))
      w
    }
    val start = System.currentTimeMillis()
    val inserted = wertungInstances.groupBy(w => w.wettkampfId).map{wkWertungen =>
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
        val completeWertungenSet = filtered ++ missing.map{missingDisziplin => 
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
            riege2 = None
          )      
        }
        completeWertungenSet
      })
    }.sum
    // wertungen: 1857 / inserted: 1857, duration: 6335ms
    logger.debug(s"wertungen: ${wertungInstances.size} / inserted: $inserted, duration: ${System.currentTimeMillis() - start}ms")

    if(collection.contains("riegen.csv")) {
      val (riegenCsv, riegenHeader) = collection("riegen.csv")
      logger.info("importing riegen ...", riegenHeader)
      updateOrinsertRiegen(riegenCsv.map(DBService.parseLine).filter(_.size == riegenHeader.size).map{fields =>
        val wettkampfid = fields(riegenHeader("wettkampfId"))
        val riege = RiegeRaw(
            wettkampfId = wettkampfInstances.get(wettkampfid + "") match {
              case Some(w) => w.id
              case None => wettkampfid
            },
            r = fields(riegenHeader("r")),
            durchgang = if(fields(riegenHeader("durchgang")).length > 0) Some(fields(riegenHeader("durchgang"))) else None,
            start = if(fields(riegenHeader("start")).length > 0) Some(fields(riegenHeader("start"))) else None,
            kind = getValue(riegenHeader, fields, "kind", "0")
        )
        riege
      })
    }

    if(collection.contains("plan_times.csv")) {
      val (planTimesCsv, planTimesHeader) = collection("plan_times.csv")
      logger.info("importing plan times ...", planTimesHeader)
      updateOrInsertPlanTimes(planTimesCsv.map(DBService.parseLine).filter(_.size == planTimesHeader.size).map{fields =>
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

    if(collection.contains("durchgaenge.csv")) {
      import java.time.format.DateTimeFormatter
      import java.util.TimeZone
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
        .withZone(TimeZone.getTimeZone("UTC").toZoneId)
      val (durchgangCsv, durchgangHeader) = collection("durchgaenge.csv")
      logger.info("importing durchgaenge ...", durchgangHeader)
      updateOrInsertDurchgaenge(durchgangCsv.map(DBService.parseLine).filter(_.size == durchgangHeader.size).map{fields =>
        val wettkampfid = fields(durchgangHeader("wettkampfId"))
        implicit def toTS(tsString: String): Option[Timestamp] = tsString match {
          case s if (s.isEmpty) => None
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
          effectiveStartTime = if(fields(durchgangHeader("effectiveStartTime")).length > 0) fields(durchgangHeader("effectiveStartTime")) else None,
          effectiveEndTime = if(fields(durchgangHeader("effectiveEndTime")).length > 0) fields(durchgangHeader("effectiveEndTime")) else None,
        )
        durchgang
      })
    }

    if(collection.contains("scoredefs.csv")) {
      val (scoredefsCsv, scoreDefHeader) = collection("scoredefs.csv")
      logger.info("importing scoredefs ...", scoreDefHeader)
      updateOrinsertScoreDefs(scoredefsCsv.map(DBService.parseLine).filter(_.size == scoreDefHeader.size).map{fields =>
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

    logger.info("import finished")
    wettkampfInstances.head._2
  }

  def moveAll(source: jdbc.JdbcBackend.DatabaseDef, target: jdbc.JdbcBackend.DatabaseDef): Unit = {
    try {
      DBService.startDB(Some(source))
      val wettkampfliste = listWettkaempfeView
      for {
        wk <- wettkampfliste
      } {
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

  def exportWettkampf(wettkampf: Wettkampf, filename: String): Unit = {
    exportWettkampfToStream(wettkampf, new FileOutputStream(filename), true)
  }
  
  def exportWettkampfToStream(wettkampf: Wettkampf, os: OutputStream, withSecret: Boolean = false): Unit = {
    val zip = new ZipOutputStream(os);
    zip.putNextEntry(new ZipEntry("wettkampf.csv"));
    zip.write((getHeader[Wettkampf] + "\n" + getValues(wettkampf)).getBytes("utf-8"))
    zip.closeEntry()

    val wertungen = selectWertungen(wettkampfId = Some(wettkampf.id))

    val vereine = wertungen.flatMap(_.athlet.verein).toSet
    zip.putNextEntry(new ZipEntry("vereine.csv"));
    zip.write((getHeader[Verein] + "\n").getBytes("utf-8"))
    for (verein <- vereine) {
      zip.write((getValues(verein) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val athleten = wertungen.map(_.athlet).toSet
    zip.putNextEntry(new ZipEntry("athleten.csv"));
    zip.write((getHeader[Athlet] + "\n").getBytes("utf-8"))
    for (athlet <- athleten) {
      zip.write((getValues(athlet) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val wertungenRaw = wertungen.map(_.toWertung)
    zip.putNextEntry(new ZipEntry("wertungen.csv"));
    zip.write((getHeader[Wertung] + "\n").getBytes("utf-8"))
    for (wertung <- wertungenRaw) {
      zip.write((getValues(wertung) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val riegenRaw = selectRiegenRaw(wettkampf.id)
    zip.putNextEntry(new ZipEntry("riegen.csv"));
    zip.write((getHeader[RiegeRaw] + "\n").getBytes("utf-8"))
    for (riege <- riegenRaw) {
      zip.write((getValues(riege) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val planTimes = loadWettkampfDisziplinTimes(UUID.fromString(wettkampf.uuid.get))
    zip.putNextEntry(new ZipEntry("plan_times.csv"));
    zip.write((getHeader[WettkampfPlanTimeRaw] + "\n").getBytes("utf-8"))
    for (planTime <- planTimes) {
      zip.write((getValues(planTime.toWettkampfPlanTimeRaw) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val durchgaenge = selectDurchgaenge(UUID.fromString(wettkampf.uuid.get))
    zip.putNextEntry(new ZipEntry("durchgaenge.csv"));
    zip.write((getHeader[Durchgang] + "\n").getBytes("utf-8"))
    for (durchgang <- durchgaenge) {
      zip.write((getValues(durchgang) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val scores = Await.result(listPublishedScores(UUID.fromString(wettkampf.uuid.get)), Duration.Inf).map(sv => sv.toRaw)
    zip.putNextEntry(new ZipEntry("scoredefs.csv"));
    zip.write((getHeader[PublishedScoreRaw] + "\n").getBytes("utf-8"))
    for (score <- scores) {
      zip.write((getValues(score) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val competitionDir = new java.io.File(Config.homedir + "/" + wettkampf.easyprint.replace(" ", "_"))

    val logofile = PrintUtil.locateLogoFile(competitionDir);
    if (logofile.exists()) {
      zip.putNextEntry(new ZipEntry(logofile.getName));
      val fis = new FileInputStream(logofile)
      val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
      Iterator
        .continually(fis.read(bytes))
        .takeWhile(-1 !=)
        .foreach(read => zip.write(bytes, 0, read))
      zip.closeEntry()
      println("logo was taken " + logofile.getName)
    }
    // pick score-defs
    if (competitionDir.exists()) {
      competitionDir
        .listFiles()
        .filter(f => f.getName.endsWith(".scoredef"))
        .toList
        .foreach { scoredefFile =>
          zip.putNextEntry(new ZipEntry(scoredefFile.getName));
          val fis = new FileInputStream(scoredefFile)
          val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
          Iterator
            .continually(fis.read(bytes))
            .takeWhile(-1 !=)
            .foreach(read => zip.write(bytes, 0, read))
          zip.closeEntry()
          println("scoredef-file was taken " + scoredefFile.getName)
        }
    }
    if (withSecret && wettkampf.hasSecred(Config.homedir, Config.remoteHostOrigin)) {
      val secretfile = wettkampf.filePath(Config.homedir, Config.remoteHostOrigin).toFile();
      zip.putNextEntry(new ZipEntry(secretfile.getName));
      val fis = new FileInputStream(secretfile)
      val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
      Iterator
      .continually(fis.read(bytes))
      .takeWhile(-1 !=)
      .foreach(read=> zip.write(bytes, 0, read))
      zip.closeEntry()
      println("secret was taken " + secretfile.getName)
    }
    if (withSecret && wettkampf.hasRemote(Config.homedir, Config.remoteHostOrigin)) {
      val secretfile = wettkampf.fromOriginFilePath(Config.homedir, Config.remoteHostOrigin).toFile();
      zip.putNextEntry(new ZipEntry(secretfile.getName));
      val fis = new FileInputStream(secretfile)
      val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
      Iterator
        .continually(fis.read(bytes))
        .takeWhile(-1 !=)
        .foreach(read=> zip.write(bytes, 0, read))
      zip.closeEntry()
      println("remote-info was taken " + secretfile.getName)
    }
    zip.finish()
    zip.close()
  }

  def getHeader[T: TypeTag] = {
    val fields = getCaseMethods[T]
    fields.map(f => "\"" + f.name.encodedName + "\"").mkString(",")
  }

  def getValues[T: TypeTag: reflect.ClassTag](instance: T) = {
    val im = rm.reflect(instance)
    val values = typeOf[T].members.collect {
      case m: MethodSymbol if m.isCaseAccessor =>
        im.reflectMethod(m).apply() match {
          case Some(verein: Verein) => s"${verein.id}"
          case Some(wk: Wettkampf) => s"${wk.id}"
          case Some(programm: Programm) => s"${programm.id}"
          case Some(athlet: Athlet) => s"${athlet.id}"
          case Some(athlet: AthletView) => s"${athlet.id}"
          case Some(disziplin: Disziplin) => s"${disziplin.id}"
          case Some(ts: Timestamp) =>
            import java.time.format.DateTimeFormatter
            import java.util.TimeZone
            var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            formatter = formatter.withZone(TimeZone.getTimeZone("UTC").toZoneId)
            formatter.format(ts.toInstant)
          case Some(value) => value.toString
          case None => ""
          case e => e.toString
        }
    }
    values.map("\"" + _ + "\"").mkString(",")
  }


  def exportEinheiten(wettkampf: Wettkampf, filename: String): Unit = {
    val export = new FileOutputStream(filename);
    val riegenRaw = suggestRiegen(Seq(0), selectWertungen(wettkampfId = Some(wettkampf.id)))
    val mapVereinVerband = selectVereine.map(v => v.name -> v.verband.getOrElse("")).toMap
    val sep = ";"
    def butify(grpkey: String, anzahl: Int) = {
      val parts = grpkey.split(",")
      // Verband, Verein, Kategorie, Geschlecht, Anzahl, Bezeichnung
      //            2         1           0
      mapVereinVerband(parts.drop(2).head) + sep + (parts.drop(2) :+ parts(1).split("//.")(0) :+ parts(0).replace("M", "Tu").replace("W", "Ti")).mkString(sep) + sep + anzahl + sep +
                      (parts.drop(2) :+ parts(1).split("//.")(0) :+ parts(0).replace("M", "(Tu)").replace("W", "(Ti)")).mkString(" ") + s" (${anzahl})"
    }
    def butifyATT(grpkey: String, anzahl: Int) = {
      val parts = grpkey.split(",")
      val geschl = parts(0).replace("M", "Tu").replace("W", "Ti")
      val jg = parts(1)
      val verein = parts(2)
      val kat = parts(3)
      val rearranged = Seq(verein, jg, geschl)
      // Verband, Verein, Jahrgang, Geschlecht, Anzahl, Bezeichnung
      //            2         1         0
      mapVereinVerband(verein) + sep + rearranged.mkString(sep) + sep + anzahl + sep +
                      rearranged.mkString(" ") + s" (${anzahl})"
    }
    val riegen = riegenRaw.map{r =>
      val anzahl = r._2.map(w => w.athletId).toSet.size
      if(wettkampf.programmId == 1) {
        butifyATT(r._1, anzahl)
      }
      else {
        butify(r._1, anzahl)
      }
    }
    if(wettkampf.programmId == 1) {
      export.write(f"sep=${sep}\nVerband${sep}Verein${sep}Jahrgang${sep}Geschlecht${sep}Anzahl${sep}Einheitsbezeichnung\n".getBytes("ISO-8859-1"))
    }
    else {
      export.write(f"sep=${sep}\nVerband${sep}Verein${sep}Kategorie${sep}Geschlecht${sep}Anzahl${sep}Einheitsbezeichnung\n".getBytes("ISO-8859-1"))
    }
    for(riege <- riegen) {
      export.write((riege + "\n").getBytes("ISO-8859-1"))
    }

    export.flush()
    export.close()
  }

  def exportDurchgaenge(wettkampf: Wettkampf, filename: String): Unit = {
    val export = new FileOutputStream(filename);
    val diszipline = listDisziplinesZuWettkampf(wettkampf.id)
    val durchgaenge = selectDurchgaenge(UUID.fromString(wettkampf.uuid.get)).map(d => d.name -> d).toMap

    val sep = ";"
    export.write(f"""sep=${sep}\nDurchgang${sep}Summe${sep}Min${sep}Max${sep}Durchschn.${sep}Total-Zeit${sep}Einturn-Zeit${sep}Gerät-Zeit""".getBytes("ISO-8859-1"))

    diszipline.foreach { x =>
        export.write(f"${sep}${x.name}${sep}Anz".getBytes("ISO-8859-1"))
    }
    export.write("\r\n".getBytes("ISO-8859-1"))

    listRiegenZuWettkampf(wettkampf.id)
      .sortBy(r => r._1)
      .map(x =>
        RiegeEditor(
            wettkampf.id,
            x._1,
            x._2,
            0,
            true,
            x._3,
            x._4,
            None))
      .groupBy(re => re.initdurchgang).toSeq
      .sortBy(re => re._1)
      .map{res =>
        val (name, rel) = res
        DurchgangEditor(wettkampf.id, durchgaenge(name.getOrElse("")), rel)
      }
      .foreach { x =>
        export.write(f"""${x.durchgang.name}${sep}${x.anz.value}${sep}${x.min.value}${sep}${x.max.value}${sep}${x.avg.value}${sep}${toShortDurationFormat(x.durchgang.planTotal)}${sep}${toShortDurationFormat(x.durchgang.planEinturnen)}${sep}${toShortDurationFormat(x.durchgang.planGeraet)}""".getBytes("ISO-8859-1"))
        diszipline.foreach { d =>
          export.write(f"${sep}${x.initstartriegen.getOrElse(d, Seq[RiegeEditor]()).map(r => f"${r.name.value.replace("M,", "Tu,").replace("W,", "Ti,")} (${r.anz.value})").mkString("\"","\n", "\"")}${sep}${x.initstartriegen.getOrElse(d, Seq[RiegeEditor]()).map(r => r.anz.value).sum}".getBytes("ISO-8859-1"))
        }
        export.write("\r\n".getBytes("ISO-8859-1"))
      }

    export.flush()
    export.close()
  }

  def exportSimpleDurchgaenge(wettkampf: Wettkampf, filename: String): Unit = {
    val export = new FileOutputStream(filename);
    val diszipline = listDisziplinesZuWettkampf(wettkampf.id)
    val durchgaenge = selectDurchgaenge(UUID.fromString(wettkampf.uuid.get)).map(d => d.name -> d).toMap

    val sep = ";"
    export.write(f"""sep=${sep}\nDurchgang${sep}Summe${sep}Min${sep}Max${sep}Total-Zeit${sep}Einturn-Zeit${sep}Gerät-Zeit""".getBytes("ISO-8859-1"))

    diszipline.foreach { x =>
      export.write(f"${sep}${x.name}${sep}Ti${sep}Tu".getBytes("ISO-8859-1"))
    }
    export.write("\r\n".getBytes("ISO-8859-1"))

    val riege2Map = listRiegen2ToRiegenMapZuWettkampf(wettkampf.id)

    val allRiegen = listRiegenZuWettkampf(wettkampf.id)
      .sortBy(r => r._1)
      .map(x =>
        RiegeEditor(
          wettkampf.id,
          x._1,
          x._2,
          0,
          true,
          x._3,
          x._4,
          None))
    val allRiegenIndex = allRiegen.map(r => r.initname -> r).toMap
    allRiegen
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
      .map{res =>
        val (name, rel) = res
        DurchgangEditor(wettkampf.id, durchgaenge(name.getOrElse("")), rel)
      }
      .foreach { x =>
        export.write(f"""${x.durchgang.name}${sep}${x.anz.value}${sep}${x.min.value}${sep}${x.max.value}${sep}${toShortDurationFormat(x.durchgang.planTotal)}${sep}${toShortDurationFormat(x.durchgang.planEinturnen)}${sep}${toShortDurationFormat(x.durchgang.planGeraet)}""".getBytes("ISO-8859-1"))
        val riegen = x.riegenWithMergedClubs()
        val rows = riegen.values.map(_.size).max
        val riegenFields = for {
          row <- (0 to rows)
          d <- diszipline
          r = riegen.getOrElse(d, Seq())
        } yield {
          (row, if (r.size > row) {
            f"${sep}${r(row)._1}${sep}${r(row)._2}${sep}${r(row)._3}"
          } else {
            f"${sep}${sep}${sep}"
          })
        }
        val rs = riegenFields.groupBy(_._1).toList
          .sortBy(_._1)
          .map(r => {
            val tuples = r._2
            val strings = tuples.map(_._2)
            val fieldsString = strings.mkString("", "", f"\r\n${sep}${sep}${sep}${sep}${sep}${sep}")
            fieldsString
          }) :+ "\r\n"

        rs.foreach(row => {
            export.write(row.getBytes("ISO-8859-1"))
          })
        export.write("\r\n".getBytes("ISO-8859-1"))
      }

    export.flush()
    export.close()
  }
}