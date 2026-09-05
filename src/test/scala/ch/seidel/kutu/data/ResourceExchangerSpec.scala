package ch.seidel.kutu.data

import ch.seidel.kutu.Config
import ch.seidel.kutu.actors.*
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.calc.ScoreCalcTemplate
import ch.seidel.kutu.domain.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest.BeforeAndAfterEach

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileInputStream}
import java.util.UUID
import java.util.zip.{ZipInputStream, ZipOutputStream}
import scala.jdk.CollectionConverters.*
import scala.concurrent.Await
import scala.concurrent.duration.*

class ResourceExchangerSpec extends KuTuBaseSpec with BeforeAndAfterEach {

  private class Recorder[T] {
    val events = scala.collection.mutable.ListBuffer[(Option[T], KutuAppEvent)]()
    val refresher: (Option[T], KutuAppEvent) => Unit = (sender, event) => events += ((sender, event))
  }

  private def makeWettkampf(uuid: String = UUID.randomUUID().toString): Wettkampf =
    Wettkampf(1L, Some(uuid), new java.sql.Date(System.currentTimeMillis()), "UnitTest-Wettkampf", 20L, 3333, BigDecimal(7.5), "unit@test.de", None, None, None, None, None)

  private def simpleAthlet(name: String, vereinName: String = "Neuer Verein", verband: Option[String] = None): AthletView =
    AthletView(0, 0, "M", name, "Hans", None, "", "", "", Some(Verein(0L, vereinName, verband)), true)

  private def simpleWertung(uuid: String): Wertung =
    Wertung(0L, 0L, 1L, 1L, uuid, Some(1d), Some(1d), Some(2d), None, None, None, None, None)

  private def writeTextFile(file: File, content: String): File = {
    file.getParentFile.mkdirs()
    val fos = new java.io.FileOutputStream(file)
    try fos.write(content.getBytes("utf-8"))
    finally fos.close()
    file
  }

  private def deleteRecursively(file: File): Unit = {
    if file.exists() then {
      if file.isDirectory then {
        file.listFiles().foreach(deleteRecursively)
      }
      file.delete()
    }
  }

  override def afterEach(): Unit = {
    Config.setLocalHostServer(false, None)
  }

  "ResourceExchanger.processWSMessage" should {

    "route unknown events untouched to the refresher" in {
      Config.setLocalHostServer(true, None)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](makeWettkampf("route-unknown"), rec.refresher)
      val event = DonationMailSent(3, BigDecimal(10), "link", "route-unknown")
      opFn(None, event)
      rec.events.map(_._2) shouldBe List(event)
    }

    "drop a MessageAck without further processing" in {
      Config.setLocalHostServer(true, None)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](makeWettkampf("route-ack"), rec.refresher)
      opFn(None, MessageAck("irrelevant"))
      rec.events shouldBe empty
    }

    "forward a DurchgangStarted on localhost" in {
      Config.setLocalHostServer(true, None)
      val rec = new Recorder[Any]
      val wk = makeWettkampf()
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val ds = DurchgangStarted(wk.uuid.get, "D-Gruppe 1")
      opFn(None, ds)
      rec.events.map(_._2) shouldBe List(ds)
    }

    "forward a DurchgangResetted on localhost" in {
      Config.setLocalHostServer(true, None)
      val rec = new Recorder[Any]
      val wk = makeWettkampf()
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val dr = DurchgangResetted(wk.uuid.get, "D-Gruppe 1")
      opFn(None, dr)
      rec.events.map(_._2) shouldBe List(dr)
    }

    "forward a DurchgangFinished on localhost" in {
      Config.setLocalHostServer(true, None)
      val rec = new Recorder[Any]
      val wk = makeWettkampf()
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val df = DurchgangFinished(wk.uuid.get, "D-Gruppe 1")
      opFn(None, df)
      rec.events.map(_._2) shouldBe List(df)
    }

    "forward an AthletWertungUpdated on localhost" in {
      Config.setLocalHostServer(true, None)
      val rec = new Recorder[Any]
      val wk = makeWettkampf()
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val uw = AthletWertungUpdated(simpleAthlet("Lokal"), simpleWertung(wk.uuid.get), wk.uuid.get, "D-Gruppe 1", 1001L, "P1")
      opFn(None, uw)
      rec.events.map(_._2) shouldBe List(uw)
    }

    "convert and forward an AthletWertungUpdatedSequenced on localhost" in {
      Config.setLocalHostServer(true, None)
      val rec = new Recorder[Any]
      val wk = makeWettkampf()
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val uws = AthletWertungUpdatedSequenced(simpleAthlet("Sequenziert"), simpleWertung(wk.uuid.get), wk.uuid.get, "D-Gruppe 1", 1001L, "P1", 5L)
      opFn(None, uws)
      rec.events.map(_._2) shouldBe List(uws.toAthletWertungUpdated())
    }

    "forward a MediaPlayerAction on localhost" in {
      Config.setLocalHostServer(true, None)
      val rec = new Recorder[Any]
      val wk = makeWettkampf()
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val action = AthletMediaStart(wk.uuid.get, simpleAthlet("Player"), simpleWertung(wk.uuid.get))
      opFn(None, action)
      rec.events.map(_._2) shouldBe List(action)
    }

    "drop a MediaPlayerAction for a non-matching remote competition" in {
      Config.setLocalHostServer(false, None)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](makeWettkampf("remote-different"), rec.refresher)
      opFn(None, AthletMediaAquire("other-uuid", simpleAthlet("Fremd"), simpleWertung("other-uuid")))
      rec.events shouldBe empty
    }

    "drop a remote score for a non-matching competition" in {
      Config.setLocalHostServer(false, None)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](makeWettkampf("remote-different-2"), rec.refresher)
      opFn(None, AthletWertungUpdated(simpleAthlet("Fremd"), simpleWertung("other-uuid"), "other-uuid", "D-Gruppe 1", 1001L, "P1"))
      rec.events shouldBe empty
    }

    "drop a ScoresPublished for a non-matching competition" in {
      Config.setLocalHostServer(true, None)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](makeWettkampf("remote-scores"), rec.refresher)
      opFn(None, ScoresPublished("score-1", "Bestenliste", "Kategorie", true, "other-uuid"))
      rec.events shouldBe empty
    }

    "not forward a remote score for a non-matching competition" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-NoMatch", 1)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val before = selectWertungen(None, None, Some(wk.id), None, None)
      opFn(None, AthletWertungUpdated(simpleAthlet("Niemand"), simpleWertung("other-uuid"), "other-uuid", "D-Gruppe 1", 1001L, "P1"))
      selectWertungen(None, None, Some(wk.id), None, None) should have size before.size
      rec.events shouldBe empty
    }
  }

  "ResourceExchanger.processWSMessage remote DB paths" should {

    "store a remote DurchgangStarted in the DB" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-Durchgang", 1)
      makeEinteilung(wk)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val name = selectDurchgaenge(UUID.fromString(wk.uuid.get)).head.name
      val startedAt = java.sql.Timestamp.valueOf("2026-06-01 10:00:00.0")
      val ds = DurchgangStarted(wk.uuid.get, name, startedAt.getTime)
      opFn(None, ds)
      selectDurchgaenge(UUID.fromString(wk.uuid.get)).find(_.name == name).get.effectiveStartTime shouldBe Some(startedAt)
      rec.events.map(_._2) shouldBe List(ds)
    }

    "reset a remote Durchgang via DurchgangResetted" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-Reset", 1)
      makeEinteilung(wk)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val name = selectDurchgaenge(UUID.fromString(wk.uuid.get)).head.name
      opFn(None, DurchgangStarted(wk.uuid.get, name))
      selectDurchgaenge(UUID.fromString(wk.uuid.get)).find(_.name == name).get.effectiveStartTime should not be None
      opFn(None, DurchgangResetted(wk.uuid.get, name))
      selectDurchgaenge(UUID.fromString(wk.uuid.get)).find(_.name == name).get.effectiveStartTime shouldBe None
      rec.events.map(_._2) should contain(DurchgangResetted(wk.uuid.get, name))
    }

    "persist a remote AthletWertungUpdated for the matching competition" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-Score", 1)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val wv = selectWertungen(None, None, Some(wk.id), None, None).head
      val uw = AthletWertungUpdated(
        wv.athlet,
        wv.toWertung.copy(noteD = Some(4d), noteE = Some(4.5d), endnote = Some(8.5d), team = Some(3), variables = None),
        wk.uuid.get, "D-Gruppe 1", wv.wettkampfdisziplin.id, "P1")
      opFn(None, uw)
      val stored = selectWertungen(None, None, Some(wk.id), None, None).find(_.id == wv.id).get
      stored.noteE shouldBe Some(4.5d)
      stored.team shouldBe 3
      val forwarded = rec.events.map(_._2)
      forwarded should have size 1
      val forwardedUw = forwarded.head.asInstanceOf[AthletWertungUpdated]
      forwardedUw.athlet.id shouldBe wv.athlet.id
      forwardedUw.wertung.id shouldBe wv.id
    }

    "persist a remote AthletWertungUpdatedSequenced via conversion" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-SequencedScore", 1)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val wv = selectWertungen(None, None, Some(wk.id), None, None).head
      val uws = AthletWertungUpdatedSequenced(
        wv.athlet,
        wv.toWertung.copy(noteD = Some(3d), noteE = Some(3d), endnote = Some(6d), variables = None),
        wk.uuid.get, "D-Gruppe 1", wv.wettkampfdisziplin.id, "P1", 7L)
      opFn(None, uws)
      val stored = selectWertungen(None, None, Some(wk.id), None, None).find(_.id == wv.id).get
      stored.noteE shouldBe Some(3d)
      rec.events.map(_._2).collect { case e: AthletWertungUpdated => e } should have size 1
    }

    "add a remote athlete to the competition (AthletsAddedToWettkampf)" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-AddAthlet", 1)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val pgIds = readWettkampfLeafs(wk.programmId).map(_.id)
      val newAthlet = simpleAthlet("Neuling", "Verein-1", Some("Verband-1"))
      val awm = AthletsAddedToWettkampf(List((newAthlet, None)), wk.uuid.get, pgIds.head, 1)
      opFn(None, awm)
      val added = selectWertungen(None, None, Some(wk.id), None, None).filter(_.athlet.name == "Neuling")
      added should not be empty
      added.map(_.team).toSet shouldBe Set(1)
      rec.events.map(_._2) should contain(awm)
    }

    "move a remote athlete to another program (AthletMovedInWettkampf)" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-MoveAthlet", 1)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val pgIds = readWettkampfLeafs(wk.programmId).map(_.id)
      val wv = selectWertungen(None, None, Some(wk.id), None, None).head
      val targetPgm = pgIds.find(_ != wv.wettkampfdisziplin.programm.id).get
      opFn(None, AthletMovedInWettkampf(wv.athlet, wk.uuid.get, targetPgm, 2, 0))
      val moved = selectWertungen(None, None, Some(wk.id), None, None).filter(_.athlet.id == wv.athlet.id)
      moved should not be empty
      moved.map(_.team).toSet shouldBe Set(2)
      moved.map(_.wettkampfdisziplin.programm.id).toSet shouldBe Set(targetPgm)
      rec.events.map(_._2) should contain(AthletMovedInWettkampf(wv.athlet, wk.uuid.get, targetPgm, 2, 0))
    }

    "remove a remote athlete from the competition (AthletRemovedFromWettkampf)" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-RemoveAthlet", 1)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val wv = selectWertungen(None, None, Some(wk.id), None, None).head
      val arw = AthletRemovedFromWettkampf(wv.athlet, wk.uuid.get)
      opFn(None, arw)
      selectWertungen(None, None, Some(wk.id), None, None).filter(_.athlet.id == wv.athlet.id) shouldBe empty
      rec.events.map(_._2) should contain(arw)
    }

    "persist a remote ScoresPublished" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-Scores", 1)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val score = ScoresPublished("score-42", "Bestenliste", "Kategorie/AlterAufsteigend", true, wk.uuid.get)
      opFn(None, score)
      val persisted = Await.result(listPublishedScores(UUID.fromString(wk.uuid.get)), Duration.Inf)
      persisted.map(_.id) should contain("score-42")
      persisted.find(_.id == "score-42").get.published shouldBe true
      rec.events.map(_._2) should contain(score)
    }

    "store events bundled in a remote BulkEvent" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-Bulk", 1)
      makeEinteilung(wk)
      val rec = new Recorder[Any]
      val opFn = ResourceExchanger.processWSMessage[Any](wk, rec.refresher)
      val name = selectDurchgaenge(UUID.fromString(wk.uuid.get)).head.name
      val ds = DurchgangStarted(wk.uuid.get, name)
      opFn(None, BulkEvent(wk.uuid.get, List(ds)))
      selectDurchgaenge(UUID.fromString(wk.uuid.get)).find(_.name == name).get.effectiveStartTime should not be None
      rec.events.map(_._2) should contain(BulkEvent(wk.uuid.get, List(ds)))
    }
  }

  "ResourceExchanger.mapLastResults" should {

    "map incoming results onto the local competition data" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-LastResults", 1)
      val wv = selectWertungen(None, None, Some(wk.id), None, None).head
      val incoming = AthletWertungUpdatedSequenced(
        wv.athlet.copy(id = 0L, js_id = 0, verein = Some(Verein(0L, "Verein-1", Some("Verband-1")))),
        wv.toWertung.copy(id = 0L, athletId = 0L, wettkampfId = 0L, noteD = Some(3d), noteE = Some(3d), endnote = Some(6d), variables = None),
        "remote-uuid", "D-Gruppe 1", 1001L, "P1", 7L)
      val mapped = ResourceExchanger.mapLastResults(
        wk,
        new java.util.ArrayList[MatchCode](),
        scala.collection.mutable.Map[Long, List[ScoreCalcTemplate]](),
        List(incoming))
      mapped should have size 1
      mapped.head.athlet.id shouldBe wv.athlet.id
      mapped.head.wertung.athletId shouldBe wv.athlet.id
      mapped.head.wertung.wettkampfId shouldBe wk.id
      mapped.head.wertung.wettkampfdisziplinId shouldBe wv.toWertung.wettkampfdisziplinId
      mapped.head.wertung.wettkampfUUID shouldBe "remote-uuid"
      mapped.head.sequenceId shouldBe 7L
      // the helper persists the mapped score as well (raw E-note is preserved)
      selectWertungen(None, None, Some(wk.id), None, None).find(_.id == wv.id).get.noteE shouldBe Some(3d)
    }
  }

  "ResourceExchanger import/export" should {

    "round-trip export and import a competition" in {
      Config.setLocalHostServer(false, None)
      val sourceWk = insertGeTuWettkampf("WSMessage-ExportImport", 1)
      makeEinteilung(sourceWk)
      val sourceNames = selectWertungen(None, None, Some(sourceWk.id), None, None).map(_.athlet.name).toSet
      val bos = new ByteArrayOutputStream()
      ResourceExchanger.exportWettkampfToStream(sourceWk, bos)
      deleteWettkampf(sourceWk.id)
      val importedWk = ResourceExchanger.importWettkampf(new ByteArrayInputStream(bos.toByteArray))
      importedWk.titel shouldBe sourceWk.titel
      importedWk.uuid shouldBe sourceWk.uuid
      val imported = selectWertungen(None, None, Some(importedWk.id), None, None)
      imported should not be empty
      imported.map(_.athlet.name).toSet shouldBe sourceNames
    }

    "reject an import file whose uuid does not match the expected uuid" in {
      val sourceWk = insertGeTuWettkampf("WSMessage-UuidCheck", 1)
      val bos = new ByteArrayOutputStream()
      ResourceExchanger.exportWettkampfToStream(sourceWk, bos)
      val thrown = the[ValidationException] thrownBy ResourceExchanger.importWettkampf(
        new ByteArrayInputStream(bos.toByteArray), expectedUuid = "will-not-match")
      thrown.getMessage should include("UUID")
    }

    "reject an import file whose notification email is missing when validateEmail is set" in {
      val wk = createWettkampf(
        new java.sql.Date(System.currentTimeMillis()), "WSMessage-MailCheck", Set(20L), "", 3333, 7.5d,
        Some(UUID.randomUUID().toString), "", "", "", "Kategorie/AlterAufsteigend", "")
      val bos = new ByteArrayOutputStream()
      ResourceExchanger.exportWettkampfToStream(wk, bos)
      val thrown = the[ValidationException] thrownBy ResourceExchanger.importWettkampf(
        new ByteArrayInputStream(bos.toByteArray), validateEmail = true)
      thrown.getMessage should include("EMail")
    }
  }

  "ResourceExchanger media files" should {

    "save and reindex an uploaded media file" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-Media", 1)
      val media = Media(UUID.randomUUID().toString, "testfile", "mp3")
      val payload = Array.tabulate[Byte](1024)(i => (i % 128).toByte)
      val uploaded = ResourceExchanger.saveMediaFile(new ByteArrayInputStream(payload), wk, media)
      uploaded.md5Defined shouldBe true
      uploaded.filename should endWith(".mp3")
      val file = uploaded.computeFilePath(wk)
      file.exists() shouldBe true
      file.length() shouldBe payload.length
      deleteRecursively(file.getParentFile.getParentFile)
      cleanUnusedMedia()
    }

    "reject an oversized media file" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-MediaTooLarge", 1)
      val media = Media(UUID.randomUUID().toString, "big", "mp3")
      val payload = new Array[Byte](Config.mediafileMaxSize + 1)
      val thrown = the[ValidationException] thrownBy ResourceExchanger.saveMediaFile(
        new ByteArrayInputStream(payload), wk, media)
      thrown.getMessage should include("zu gross")
      cleanUnusedMedia()
    }

    "export recorded media files into a zip stream" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-MediaZip", 1)
      val media = Media(UUID.randomUUID().toString, "song", "mp3")
      val payload = Array.tabulate[Byte](512)(i => (i % 128).toByte)
      val uploaded = ResourceExchanger.saveMediaFile(new ByteArrayInputStream(payload), wk, media)
      val bos = new ByteArrayOutputStream()
      ResourceExchanger.exportWettkampfMediaFilesToStream(wk, List.empty, bos)
      val zis = new ZipInputStream(new ByteArrayInputStream(bos.toByteArray))
      val entryNames = Iterator.continually(zis.getNextEntry).takeWhile(_ != null).map(_.getName).toList
      val expected = s"${uploaded.filename}@${uploaded.id}@${uploaded.extension}"
      entryNames should contain(expected)
      zis.close()
      deleteRecursively(uploaded.computeFilePath(wk).getParentFile.getParentFile)
      cleanUnusedMedia()
    }

    "remove orphaned media files during cleanup" in {
      Config.setLocalHostServer(false, None)
      val wk = insertGeTuWettkampf("WSMessage-Cleanup", 1)
      val audiofilesDir = new File(wk.prepareFilePath(Config.homedir, readOnly = false).getPath + "/audiofiles")
      audiofilesDir.mkdirs()
      val orphan = writeTextFile(new File(audiofilesDir, "orphan-delete-me.mp3"), "nope")
      orphan.exists() shouldBe true
      ResourceExchanger.cleanupMediaFiles()
      orphan.exists() shouldBe false
      deleteRecursively(audiofilesDir)
      cleanUnusedMedia()
    }
  }

  "ResourceExchanger.xlsx exports" should {

    "export durchgaenge as an xlsx workbook" in {
      val wk = insertGeTuWettkampf("WSMessage-XlsxExport", 1)
      makeEinteilung(wk)
      val target = new java.io.File("target/test-exports")
      target.mkdirs()
      val file = new File(target, "durchgaenge.xlsx")
      ResourceExchanger.exportDurchgaenge(wk, file.getPath)
      val workbook = new XSSFWorkbook(new FileInputStream(file))
      try workbook.getSheet("Durchgaenge") should not be null
      finally workbook.close()
      deleteRecursively(target)
    }

    "export simple durchgaenge as an xlsx workbook" in {
      val wk = insertGeTuWettkampf("WSMessage-SimpleXlsxExport", 1)
      makeEinteilung(wk)
      val target = new java.io.File("target/test-exports")
      target.mkdirs()
      val file = new File(target, "simple-durchgaenge.xlsx")
      ResourceExchanger.exportSimpleDurchgaenge(wk, file.getPath)
      val workbook = new XSSFWorkbook(new FileInputStream(file))
      try workbook.getSheet("Durchgaenge") should not be null
      finally workbook.close()
      deleteRecursively(target)
    }
  }
}