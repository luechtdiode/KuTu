package ch.seidel.kutu.http

import ch.seidel.kutu.Config
import ch.seidel.kutu.domain.Wettkampf
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.StatusCode
import org.slf4j.LoggerFactory

import java.io.{FileOutputStream, InputStream}
import java.nio.file.Files

object LogoHandler {
  private val log = LoggerFactory.getLogger(this.getClass)

  def handleLogoUpload(wettkampf: Wettkampf, fileext: String, is: InputStream, homedir: String = Config.homedir): Unit = {
    val competitionDir = wettkampf.prepareFilePath(homedir, readOnly = false)
    val homedirPath = new java.io.File(homedir).toPath.toAbsolutePath.normalize
    val compDirPath = competitionDir.toPath.toAbsolutePath.normalize
    if !compDirPath.startsWith(homedirPath) then {
      throw new RuntimeException(s"Ungültiger Wettkampf-Pfad: $compDirPath")
    }
    val dirName = competitionDir.getName
    if dirName == "." || dirName == ".." || dirName.isEmpty then {
      throw new RuntimeException("Ungültiger Wettkampfname")
    }
    Files.find(competitionDir.toPath, 1,
      (path, attrs) => !attrs.isSymbolicLink && path.getFileName.toFile.getName.startsWith("logo.")
    ).forEach(path => Files.deleteIfExists(path))
    val logofile = new java.io.File(competitionDir, s"logo.$fileext")
    val fos = new FileOutputStream(logofile)
    try {
      val bytes = new Array[Byte](1024)
      Iterator.continually(is.read(bytes)).takeWhile(-1 != _).foreach(read => fos.write(bytes, 0, read))
      fos.flush()
    } finally { fos.close() }
    log.info(s"Logo uploaded for ${wettkampf.easyprint}: ${logofile.getName}")
  }
}
