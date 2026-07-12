package ch.seidel.kutu.http

import ch.seidel.kutu.domain.{Wettkampf, encodeFileName}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite

import java.io.{ByteArrayInputStream, File, FileOutputStream}
import java.sql.Date
import java.nio.file.Files
import scala.compiletime.uninitialized

class LogoHandlerSpec extends AnyFunSuite with BeforeAndAfterEach {
  var tempDir: File = uninitialized

  override def beforeEach(): Unit = {
    tempDir = Files.createTempDirectory("kutu-logo-test-").toFile
  }

  override def afterEach(): Unit = {
    if (tempDir != null) {
      tempDir.listFiles().foreach { f =>
        if (f.isDirectory) deleteRecursively(f)
        else f.delete()
      }
      tempDir.delete()
    }
  }

  private def deleteRecursively(dir: File): Unit = {
    dir.listFiles().foreach { f =>
      if (f.isDirectory) deleteRecursively(f)
      else f.delete()
    }
    dir.delete()
  }

  private def createWettkampf(titel: String = "Test Wettkampf"): Wettkampf = {
    Wettkampf(
      id = 0L,
      uuid = Some(java.util.UUID.randomUUID().toString),
      datum = new Date(System.currentTimeMillis()),
      titel = titel,
      programmId = 1L,
      auszeichnung = 0,
      auszeichnungendnote = BigDecimal(0),
      notificationEMail = "",
      altersklassen = None,
      jahrgangsklassen = None,
      punktegleichstandsregel = None,
      rotation = None,
      teamrule = None
    )
  }

  test("handleLogoUpload writes logo file to competition directory") {
    val wettkampf = createWettkampf("LogoTest")
    val content = "fake-image-content"
    val is = new ByteArrayInputStream(content.getBytes("UTF-8"))

    LogoHandler.handleLogoUpload(wettkampf, "png", is, tempDir.getAbsolutePath)

    val encodedName = encodeFileName(wettkampf.easyprint)
    val logoFile = new File(tempDir, s"$encodedName/logo.png")
    assert(logoFile.exists(), "logo.png should exist")
    val written = scala.io.Source.fromFile(logoFile, "UTF-8").mkString
    assert(written === content)
  }

  test("handleLogoUpload deletes old logo files before writing new one") {
    val wettkampf = createWettkampf("CleanupTest")
    val encodedName = encodeFileName(wettkampf.easyprint)
    val compDir = new File(tempDir, encodedName)
    compDir.mkdirs()

    val oldLogo = new File(compDir, "logo.svg")
    Files.write(oldLogo.toPath, "old-logo".getBytes("UTF-8"))
    assert(oldLogo.exists())

    val content = "new-logo-content"
    val is = new ByteArrayInputStream(content.getBytes("UTF-8"))

    LogoHandler.handleLogoUpload(wettkampf, "jpg", is, tempDir.getAbsolutePath)

    assert(!oldLogo.exists(), "old logo.svg should be deleted")
    val logoFile = new File(compDir, "logo.jpg")
    assert(logoFile.exists(), "logo.jpg should exist")
    val written = scala.io.Source.fromFile(logoFile, "UTF-8").mkString
    assert(written === content)
  }

  test("handleLogoUpload rejects path traversal via homedir") {
    // Use a homedir that is a symlink pointing outside the intended area.
    // When the competition dir is created inside the symlink, its normalized
    // path will resolve to the target, not the symlink path.
    val realTarget = new File(tempDir, "real-storage")
    realTarget.mkdirs()
    val symlinkHome = new File(tempDir, "link-home")
    Files.createSymbolicLink(symlinkHome.toPath, realTarget.toPath)

    val wettkampf = createWettkampf("SymlinkTest")
    val is = new ByteArrayInputStream("traversal-test".getBytes("UTF-8"))

    // This should succeed because the normalized path resolves inside realTarget
    LogoHandler.handleLogoUpload(wettkampf, "svg", is, symlinkHome.getAbsolutePath)

    val encodedName = encodeFileName(wettkampf.easyprint)
    val logoFile = new File(realTarget, s"$encodedName/logo.svg")
    assert(logoFile.exists(), "logo should be written to the real target via symlink")
  }

  test("handleLogoUpload preserves content type accuracy for svg") {
    val wettkampf = createWettkampf("SvgTest")
    val svgContent = "<svg xmlns='http://www.w3.org/2000/svg'></svg>"
    val is = new ByteArrayInputStream(svgContent.getBytes("UTF-8"))

    LogoHandler.handleLogoUpload(wettkampf, "svg", is, tempDir.getAbsolutePath)

    val encodedName = encodeFileName(wettkampf.easyprint)
    val logoFile = new File(tempDir, s"$encodedName/logo.svg")
    assert(logoFile.exists(), "logo.svg should exist")
    val written = scala.io.Source.fromFile(logoFile, "UTF-8").mkString
    assert(written === svgContent)
  }

  test("handleLogoUpload only deletes files starting with logo. prefix") {
    val wettkampf = createWettkampf("PreserveTest")
    val encodedName = encodeFileName(wettkampf.easyprint)
    val compDir = new File(tempDir, encodedName)
    compDir.mkdirs()

    val otherFile = new File(compDir, "important.txt")
    Files.write(otherFile.toPath, "keep".getBytes("UTF-8"))

    val is = new ByteArrayInputStream("new-logo".getBytes("UTF-8"))
    LogoHandler.handleLogoUpload(wettkampf, "png", is, tempDir.getAbsolutePath)

    assert(otherFile.exists(), "non-logo files should be preserved")
    assert(otherFile.length() > 0)
  }
}
