// project/JpackageExecutor.scala
import sbt._
import Keys._
import scala.sys.process._ // Import the process utilities for running the command
import BuildUtils.grabBuildContextvariables

object JpackageExecutor {

  // Define the task key here for use in build.sbt
  val jpackageApp = taskKey[Unit]("Package application with jpackage")

  /**
   * Implementation for the jpackageApp task.
   * @param log Logger for sbt output
   * @param baseDir Project base directory
   * @param targetDir Project target directory
   * @param sourceDir Project source directory (for icon path)
   * @param appVersion Current project version
   * @param bundlerOs Target OS name (e.g., "mac", "win", "linux")
   * @param bundlerType Package type (e.g., "dmg", "exe", "deb")
   * @param appLogo Name of the icon file (e.g., "logo.png")
   * @param appName Name of the application (parsed from context variables)
   * @param osName Current operating system name
   * @param javaOptionsStr Extra Java options string
   */
  def executeJpackage(
    log: Logger,
    baseDir: File,
    targetDir: File,
    sourceDir: File,
    appVersion: String
  ): Unit = {
    
    val appName = grabBuildContextvariables(appVersion).appName
    // Determine OS and bundler type
    val osName = sys.props.getOrElse("os.name", "").toLowerCase
    val osArch = sys.props.getOrElse("os.arch", "").toLowerCase

    val bundlerOsAppLogo = 
    if (osName.contains("win")) 
        ("Win64", "msi", "app-logo.ico")
    else if (osName.contains("mac") && osArch.contains("aarch64"))
        ("macOS-aarch64", "pkg", "AppIcon.icns")
    else if (osName.contains("mac"))
        ("macOS-x86_64", "pkg", "AppIcon.icns")
    else 
        ("Linux", "deb", "app-logo.png")

    val bundlerOs = bundlerOsAppLogo._1
    val bundlerType = bundlerOsAppLogo._2
    val appLogo = bundlerOsAppLogo._3

    // Java options for jpackage (VM options passed to the packaged app)
    val javaOptionsStr = Seq(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
    "--add-opens=java.base/java.net=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-opens=java.base/java.time=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
    "--add-opens=java.base/java.math=ALL-UNNAMED",
    "--enable-native-access=ALL-UNNAMED",
    "-XX:+UseZGC",
    "-XX:+ZGenerational",
    "-XX:+ExitOnOutOfMemoryError"
    ).mkString(" ")

    // --- Logic for finding JDK Home ---
    val localJdk = baseDir / s"jdk-25.0.1+8" // Assuming a constant name
    val jdkHome = 
      if (localJdk.exists()) {
        log.info(s"Using local JDK: ${localJdk.getAbsolutePath}")
        localJdk.getAbsolutePath
      } else {
        val fallback = sys.env.getOrElse("JAVA_HOME", "/usr/lib/jvm/default-java")
        log.info(s"Using JAVA_HOME: $fallback")
        fallback
      }
    
    val jpackageBin = s"$jdkHome/bin/jpackage"
    val inputDir = targetDir / "package"
    val outputDir = targetDir / bundlerOs
     
    val iconPath = sourceDir / "resources" / "images" / appLogo
    
    // Find the actual jar file in the package directory
    val jarFiles = (inputDir * "*.jar").get()
    if (jarFiles.isEmpty) throw new RuntimeException(s"No jar file found in ${inputDir.getAbsolutePath}")
    val mainJarFile = jarFiles.head
    val mainJarName = mainJarFile.getName

    log.info(s"Using main jar: $mainJarName")
    
    // --- Build jpackage arguments ---
    val baseArgs = Seq(
      "--main-class", "ch.seidel.kutu.KuTuApp", // Hardcoded main class
      "--type", bundlerType,
      "--input", inputDir.getAbsolutePath,
      "--dest", outputDir.getAbsolutePath,
      "--name", appName,
      "--app-version", appVersion,
      "--main-jar", mainJarName,
      "--icon", iconPath.getAbsolutePath,
      "--copyright", "Interpolar",
      "--vendor", "Interpolar",
      "--java-options", s"-DkutuAPPDIR=$$APPDIR",
      "--java-options", javaOptionsStr
    )
    
    // OS-specific options
    val osSpecificArgs = 
      if (osName.contains("win"))
        Seq("--win-dir-chooser", "--win-shortcut", "--win-per-user-install", "--win-menu")
      else if (osName.contains("mac"))
        Seq("--mac-package-identifier", "ch.seidel.kutu.KuTuApp")
      else if (osName.contains("linux"))
        Seq("--linux-shortcut")
      else Seq()
    
    // Build and execute jpackage command
    val cmd = Seq(jpackageBin) ++ baseArgs ++ osSpecificArgs
    log.info(s"Running jpackage...")
    log.debug(s"Command: ${cmd.mkString(" ")}")
    
    val result = Process(cmd).! // Execute the command
    if (result != 0) throw new RuntimeException(s"jpackage failed with exit code $result")
    
    log.info(s"Successfully created package in ${outputDir.getAbsolutePath}")
  }
}
