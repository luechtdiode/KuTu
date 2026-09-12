package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.Config
import ch.seidel.kutu.calc.{ScoreAggregateFn, ScoreCalcTemplate}

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path}
import java.util.UUID
import scala.compiletime.uninitialized
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.StreamConverters.*

class CompetitionManagerSpec extends KuTuBaseSpec {

  var sourceWettkampf: Wettkampf = uninitialized
  val manager = new CompetitionManager(this)

  private def deleteRecursively(dir: File): Unit = {
    if (dir.exists()) {
      val children = dir.listFiles()
      if (children != null) children.foreach(deleteRecursively)
      dir.delete()
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    sourceWettkampf = createWettkampf(
      new java.sql.Date(System.currentTimeMillis()),
      "SourceWK", Set(20L), "test@test.com",
      3333, 7.5d, Some(UUID.randomUUID().toString),
      "7,8,9", "7,8,9", "", "Kategorie/AlterAufsteigend/Verein/Vorname/Name/Rotierend/AltInvers", ""
    )
  }

  private def createTargetWettkampf(titel: String, programmId: Long = 20L): Wettkampf = {
    createWettkampf(
      new java.sql.Date(System.currentTimeMillis()),
      titel, Set(programmId), "test@test.com",
      3333, 7.5d, Some(UUID.randomUUID().toString),
      "", "", "", "", ""
    )
  }

  private def prepareSourceDirectory(): File = {
    val sourceFolder = new File(Config.homedir + "/" + encodeFileName(sourceWettkampf.easyprint))
    if (!sourceFolder.exists()) sourceFolder.mkdirs()
    sourceFolder
  }

  "CompetitionManager" should {

    "cloneWettkampfData create target directory when it does not exist" in {
      val target = createTargetWettkampf("TargetDirWK")
      val targetFolder = new File(Config.homedir + "/" + encodeFileName(target.easyprint))
      deleteRecursively(targetFolder)

      manager.cloneWettkampfData(sourceWettkampf.uuid.get, target)

      targetFolder.exists() shouldBe true
      deleteRecursively(targetFolder)
    }

    "cloneWettkampfData copy logo file from source to target" in {
      val sourceFolder = prepareSourceDirectory()
      val logoFile = new File(sourceFolder, "logo.png")
      if (!logoFile.exists()) {
        Files.copy(
          getClass.getResourceAsStream("/images/wettkampf.png"),
          logoFile.toPath
        )
      }
      logoFile.exists() shouldBe true

      val target = createTargetWettkampf("TargetLogoWK")
      val targetFolder = new File(Config.homedir + "/" + encodeFileName(target.easyprint))
      deleteRecursively(targetFolder)

      manager.cloneWettkampfData(sourceWettkampf.uuid.get, target)

      val targetLogo = new File(targetFolder, "logo.png")
      targetLogo.exists() shouldBe true
      targetLogo.length() shouldBe logoFile.length()
      deleteRecursively(targetFolder)
    }

    "cloneWettkampfData copy plan times when same program" in {
      initWettkampfDisziplinTimes(using sourceWettkampf.id)
      val sourceTimes = loadWettkampfDisziplinTimes(using sourceWettkampf.id)
      sourceTimes should not be empty

      val target = createTargetWettkampf("TargetPlanTimesWK")
      manager.cloneWettkampfData(sourceWettkampf.uuid.get, target)

      val targetTimes = loadWettkampfDisziplinTimes(using target.id)
      targetTimes.size shouldBe sourceTimes.size
      targetTimes.zip(sourceTimes).foreach { case (t, s) =>
        t.wechsel shouldBe s.wechsel
        t.einturnen shouldBe s.einturnen
        t.uebung shouldBe s.uebung
        t.wertung shouldBe s.wertung
      }
    }

    "cloneWettkampfData copy score calc templates when same program" in {
      val template = ScoreCalcTemplate(
        id = 0, wettkampfId = Some(sourceWettkampf.id),
        disziplinId = None, wettkampfdisziplinId = None,
        dFormula = "max($Dname1.1, $Dname2.1)^",
        eFormula = "10 - avg($Ename1.3, $Ename2.3)",
        pFormula = "($Pname.0 / 10)^",
        aggregateFn = Some(ScoreAggregateFn.Max)
      )
      updateScoreCalcTemplates(List(template))
      val sourceTemplates = loadScoreCalcTemplates(sourceWettkampf.id)
      sourceTemplates should not be empty

      val target = createTargetWettkampf("TargetTemplatesWK")
      manager.cloneWettkampfData(sourceWettkampf.uuid.get, target)

      val targetTemplates = loadScoreCalcTemplates(target.id)
      targetTemplates.size shouldBe sourceTemplates.size
      targetTemplates.foreach { t =>
        t.wettkampfId shouldBe Some(target.id)
      }
    }

    "cloneWettkampfData copy published scores as unpublished" in {
      val score = savePublishedScore(sourceWettkampf.id, "TestScore", "groupby=verein&filter=all", published = true, propagate = false)
      score.published shouldBe true

      val target = createTargetWettkampf("TargetScoresWK")
      manager.cloneWettkampfData(sourceWettkampf.uuid.get, target)

      val targetScores = Await.result(listPublishedScores(UUID.fromString(target.uuid.get)), Duration.Inf)
      targetScores.size shouldBe 1
      targetScores.head.title shouldBe "TestScore"
      targetScores.head.query shouldBe "groupby=verein&filter=all"
      targetScores.head.published shouldBe false
    }

    "cloneWettkampfData copy scoredef files from source to target" in {
      val sourceFolder = prepareSourceDirectory()
      val scoredefFile = new File(sourceFolder, "test-scoredef.scoredef")
      val writer = new PrintWriter(scoredefFile)
      try {
        writer.println("test,data")
      } finally {
        writer.close()
      }
      scoredefFile.exists() shouldBe true

      val target = createTargetWettkampf("TargetScoredefWK")
      val targetFolder = new File(Config.homedir + "/" + encodeFileName(target.easyprint))
      deleteRecursively(targetFolder)

      manager.cloneWettkampfData(sourceWettkampf.uuid.get, target)

      val targetScoredef = new File(targetFolder, "test-scoredef.scoredef")
      targetScoredef.exists() shouldBe true
      Files.readString(targetScoredef.toPath) shouldBe s"test,data${System.lineSeparator()}"
      deleteRecursively(targetFolder)
    }

    "cloneWettkampfData not overwrite existing logo in target" in {
      val sourceFolder = prepareSourceDirectory()
      val logoFile = new File(sourceFolder, "logo.png")
      if (!logoFile.exists()) {
        Files.copy(
          getClass.getResourceAsStream("/images/wettkampf.png"),
          logoFile.toPath
        )
      }

      val target = createTargetWettkampf("TargetExistingLogoWK")
      val targetFolder = new File(Config.homedir + "/" + encodeFileName(target.easyprint))
      targetFolder.mkdirs()
      val existingLogo = new File(targetFolder, "logo.png")
      Files.write(existingLogo.toPath, "existing".getBytes("utf-8"))
      val originalLength = existingLogo.length()

      manager.cloneWettkampfData(sourceWettkampf.uuid.get, target)

      existingLogo.length() shouldBe originalLength
      deleteRecursively(targetFolder)
    }

    "cloneWettkampfData not overwrite existing scoredef in target" in {
      val sourceFolder = prepareSourceDirectory()
      val scoredefFile = new File(sourceFolder, "existing-scoredef.scoredef")
      val writer = new PrintWriter(scoredefFile)
      try {
        writer.println("source,data")
      } finally {
        writer.close()
      }

      val target = createTargetWettkampf("TargetExistingScoredefWK")
      val targetFolder = new File(Config.homedir + "/" + encodeFileName(target.easyprint))
      targetFolder.mkdirs()
      val existingScoredef = new File(targetFolder, "existing-scoredef.scoredef")
      Files.write(existingScoredef.toPath, "target,data".getBytes("utf-8"))

      manager.cloneWettkampfData(sourceWettkampf.uuid.get, target)

      Files.readString(existingScoredef.toPath) shouldBe "target,data"
      deleteRecursively(targetFolder)
    }
  }
}
