import sbt._
import Keys._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class BuildContextVariables(
  major: String,
  minor: String,
  incremental: String,
  majorminor: String,
  appversion: String,
  versionForName: String,
  appName: String,
  builddate: String
)

object BuildUtils {

  def grabBuildContextvariables(version: String): BuildContextVariables = {
    val parts = version.split('.')
    val major = if (parts.length >= 1) parts(0) else "0"
    val minor = if (parts.length >= 2) parts(1) else "0"
    val incremental = if (parts.length >= 3) parts(2) else "0"

    val majorminor = if (parts.length >= 2) 
      s"${parts(0)}.${parts(1)}" 
    else 
      version

    val appversion = if (parts.length >= 3) 
      s"${parts(0)}.${parts(1)}.${parts(2)}"
    else 
      version

    val versionForName = if (parts.length >= 2) 
      s"v${parts(0)}r${parts(1)}" 
    else 
      version

    val appName = s"TurnerWettkampf-App-$versionForName"

    val builddate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    BuildContextVariables(major, minor, incremental, majorminor, appversion, versionForName, appName, builddate)
  }

  // Determine JavaFX classifier for current OS
  def javafxClassifier: String = {
    val os = sys.props.getOrElse("os.name", "").toLowerCase
    val arch = sys.props.getOrElse("os.arch", "").toLowerCase
    if (os.contains("win")) "win"
    else if (os.contains("mac") && arch.contains("aarch64")) "mac-aarch64"
    else if (os.contains("mac")) "mac"
    else "linux"
  }

  // A full custom task definition that can be imported into build.sbt
  val filterApplicationConfTask = taskKey[Seq[File]]("Filters application.conf and copies it to a managed location")

  def filterApplicationConfImpl(
      resourceDir: File, 
      targetDir: File, 
      log: Logger, 
      versionValue: String): Seq[File] = {
    
    val appConf = resourceDir / "application.conf"
    val targetConf = targetDir / "application.conf"
    val parsedVersion = grabBuildContextvariables(versionValue)

    // Create the destination directory if it doesn't exist
    IO.createDirectory(targetDir)

    // Replace tokens (e.g., ${project.version}) with actual values
    val content = IO.read(appConf, IO.utf8)
    // Replace all your required properties here.
    val filteredContent = content
      .replace("${app.version}", parsedVersion.appversion)
      .replace("${app.majorminor.version}", parsedVersion.majorminor)
      .replace("${buildDate}", parsedVersion.builddate)

    IO.write(targetConf, filteredContent, IO.utf8)
    log.info(s"Filtered $appConf to $targetConf")
    
    // Return the single generated file
    Seq(targetConf)
  }
}
