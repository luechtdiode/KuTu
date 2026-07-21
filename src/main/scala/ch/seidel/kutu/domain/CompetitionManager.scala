package ch.seidel.kutu.domain

import ch.seidel.kutu.Config
import ch.seidel.kutu.renderer.ServerPrintUtil

import java.io.File
import java.nio.file.Files
import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.Await

class CompetitionManager(service: KutuService) {

  def cloneWettkampfData(sourceUuid: String, target: Wettkampf): Unit = {
    val wkToCopy = service.readWettkampf(sourceUuid)
    val sourceFolder = new File(Config.homedir + "/" + encodeFileName(wkToCopy.easyprint))
    val targetFolder = new File(Config.homedir + "/" + encodeFileName(target.easyprint))
    if !targetFolder.exists() then targetFolder.mkdirs()

    val sourceLogo = ServerPrintUtil.locateLogoFile(sourceFolder)
    if !targetFolder.equals(sourceFolder) && sourceLogo.exists() then {
      val logofileCopyTo = targetFolder.toPath.resolve(sourceLogo.getName)
      if !logofileCopyTo.toFile.exists() then {
        Files.copy(sourceLogo.toPath, logofileCopyTo)
      }
    }

    if wkToCopy.programmId == target.programmId then {
      service.updateOrInsertPlanTimes(
        service.loadWettkampfDisziplinTimes(using wkToCopy.id)
          .map(_.toWettkampfPlanTimeRaw.copy(wettkampfId = target.id)))
      service.updateScoreCalcTemplates(
        service.loadScoreCalcTemplates(wkToCopy.id)
          .map(_.copy(wettkampfId = Some(target.id))))
    }

    if !targetFolder.equals(sourceFolder) then {
      Option(sourceFolder.listFiles()).foreach(_.toList
        .filter(f => f.getName.endsWith(".scoredef"))
        .sortBy(_.getName)
        .foreach(scoreFileSource => {
          val targetFilePath = targetFolder.toPath.resolve(scoreFileSource.getName)
          if !targetFilePath.toFile.exists() then {
            Files.copy(scoreFileSource.toPath, targetFilePath)
          }
        }))
    }

    val scores = Await.result(
      service.listPublishedScores(UUID.fromString(wkToCopy.uuid.get)), Duration.Inf)
    scores.foreach(score => {
      service.savePublishedScore(
        wettkampfId = target.id, title = score.title, query = score.query,
        published = false, propagate = false)
    })
  }
}
