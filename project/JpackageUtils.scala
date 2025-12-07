// project/JpackageUtils.scala
import sbt._
import Keys._
import sbt.librarymanagement.ConfigurationFilter

object JpackageUtils {

  // Define the task key here so it's accessible from build.sbt
  val prepareJpackage = taskKey[Unit]("Prepare directory for jpackage: compiled jar + all dependencies")

  // The implementation logic is encapsulated in a single method
  def prepareJpackageImpl(
      log: Logger,
      targetValue: File,
      packageJar: File,
      updateReport: UpdateReport): Unit = {

    val out = targetValue / "package"
    val libs = out / "libs"
    IO.delete(out)
    IO.createDirectory(libs)

    log.info("Building package jar...")
    
    // Copy main application jar
    val jarTarget = out / packageJar.getName
    IO.copyFile(packageJar, jarTarget)
    log.info(s"Copied main jar: ${packageJar.getName}")

    // Define which configurations (scopes) to include: Compile and Runtime
    // Use configurationFilter to create the required filter object
    val includedConfigurations: ConfigurationFilter = configurationFilter(
      name = "compile" | "runtime" | "master"
    )
    
    // Copy all dependencies to libs folder, excluding scala-lang orgs
    val allDeps = updateReport.select(
      configuration = includedConfigurations,
      module = _ => true, // include all modules
      artifact = _ => true // include all artifacts
    )
    
    allDeps.foreach { f =>
      IO.copyFile(f, libs / f.getName)
    }

    // Also specifically copy JavaFX jars (already in allDeps but logged separately)
    val javafxJars = updateReport.matching(moduleFilter(organization = "org.openjfx"))
    log.info(s"Copied ${allDeps.length} dependencies to libs folder")
    log.info(s"Including ${javafxJars.length} JavaFX jars")

    log.info(s"Prepared jpackage input in: ${out.getAbsolutePath}")
    log.info(s"JAR: ${jarTarget.getName}")
    log.info(s"Dependencies: libs/ (${allDeps.length} jars)")
  }
}
