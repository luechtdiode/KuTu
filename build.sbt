import BuildUtils._ 
import JpackageUtils._ 
import JpackageExecutor._ 

ThisBuild / scalaVersion := "3.7.3"
ThisBuild / organization := "ch.seidel"
ThisBuild / version := "2.3.23"

logLevel := Level.Error

name := "KuTu"

// library versions (from pom properties)
val scalafxV     = "21.0.0-R32"
val javafxV     = "21.0.4"
val pekkoHttpV   = "1.3.0"
val pekkoV       = "1.3.0"
val slickV       = "3.6.1"
val scalatestV   = "3.3.0-SNAP4"
val gatlingV     = "3.14.7"
val slf4jV       = "2.0.17"
val logbackV     = "1.5.21"

// Ensure Java compiler options match the project's target
ThisBuild / javacOptions ++= Seq("-source", "21", "-target", "21")

// Resolvers: include Sonatype snapshots because project uses SNAP dependencies
resolvers ++= Seq(
  Resolver.mavenCentral,
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

// Compiler options (inspired by scala-maven-plugin args)
Compile / scalacOptions ++= {
  val base = Seq(
    "-unchecked",
    "-deprecation",
    "-explaintypes",
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-language:existentials"
  )
  base
}

// Prefer Java then Scala compilation order (to match maven config)
Compile / compileOrder := CompileOrder.JavaThenScala

// JVM options for forked runs/tests
Test / fork := true
Test / javaOptions ++= Seq(
  "-server",
  "-Xss2m",
  "-XX:+UseParallelGC",
  "-XX:MaxInlineLevel=20",
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
  "--enable-native-access=ALL-UNNAMED"
)

// Filter some source files ===========================================================
// Define a directory to hold the filtered resources temporarily in the target folder
val filteredResourcesDir = settingKey[File]("directory for filtered resources")
Compile / filteredResourcesDir := crossTarget.value / "sbt-filtered-resources" / "main"

// Define the task using the implementation from the external file
Compile / filterApplicationConfTask := {
  filterApplicationConfImpl(
    (Compile / resourceDirectory).value,
    (Compile / filteredResourcesDir).value,
    streams.value.log,
    version.value
  )
}
// Add the output of the filtering task to the *managed* resources
Compile / managedResources ++= (Compile / filterApplicationConfTask).value

// Exclude the original 'application.conf' from the *unmanaged* resources
Compile / unmanagedResources / excludeFilter := {
  (Compile / unmanagedResources / excludeFilter).value || "application.conf"
}
// ============================================================================

// Basic dependency translation (try to keep the same artifacts where possible)
libraryDependencies ++= Seq(
  // ScalaFX (cross-built)
  "org.scalafx" %% "scalafx" % scalafxV,

  // Pekko / Pekko-HTTP
  "org.apache.pekko" %% "pekko-http" % pekkoHttpV,
  "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpV,
  "org.apache.pekko" %% "pekko-http-xml" % pekkoHttpV,
  "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpV % Test,
  "org.apache.pekko" %% "pekko-actor" % pekkoV,
  "org.apache.pekko" %% "pekko-stream-testkit" % pekkoV % Test,
  "org.apache.pekko" %% "pekko-stream" % pekkoV,
  "org.apache.pekko" %% "pekko-cluster" % pekkoV,
  "org.apache.pekko" %% "pekko-cluster-tools" % pekkoV,

  // Slick
  "com.typesafe.slick" %% "slick" % slickV,
  "com.typesafe.slick" %% "slick-hikaricp" % slickV,

  // JSON / Jackson / Spray
  "com.fasterxml.jackson.core" % "jackson-core" % "2.20.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.20.1",

  // Database drivers / utils
  "org.xerial" % "sqlite-jdbc" % "3.45.2.0",
  "org.postgresql" % "postgresql" % "42.7.8",
  "com.zaxxer" % "HikariCP" % "7.0.2",

  // Utilities
  "org.slf4j" % "slf4j-api" % slf4jV,
  "ch.qos.logback" % "logback-classic" % logbackV,
  "commons-codec" % "commons-codec" % "1.20.0",
  "org.apache.commons" % "commons-lang3" % "3.20.0",
  "org.apache.commons" % "commons-text" % "1.14.0",

  // Additional Java libraries from pom.xml
  "org.controlsfx" % "controlsfx" % "11.2.2",
  "org.simplejavamail" % "simple-java-mail" % "8.12.6",
  "net.glxn" % "qrgen" % "1.4",
  "com.github.markusbernhardt" % "proxy-vole" % "1.0.5",
  "org.javadelight" % "delight-nashorn-sandbox" % "0.5.4",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

  // Pekko persistence & Kryo
  "org.apache.pekko" %% "pekko-persistence" % pekkoV,
  "io.altoo" %% "pekko-kryo-serialization" % "1.3.0",

  // Pekko SLF4J logging
  "org.apache.pekko" %% "pekko-slf4j" % pekkoV,

  // Prometheus metrics (via fr.davit plugin)
  "fr.davit" %% "pekko-http-metrics-prometheus" % "2.1.0" exclude("org.scala-lang.modules", "scala-parser-combinators_2.13"),

  // Test libraries
  "org.scalatest" %% "scalatest" % scalatestV % Test,
  "junit" % "junit" % "4.13.2" % Test,
  "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV % Test,
  // Scala 3 std lib
  "org.scala-lang" %% "scala3-library" % "3.7.4"
)

// Add parser combinators: choose correct artifact for current scalaVersion
libraryDependencies ++= {
  Seq("org.scala-lang.modules" % "scala-parser-combinators_3" % "2.4.0")
}

// Exclude the 2.13 suffixed parser-combinators globally to avoid mixed-resolution during Scala 3 builds
excludeDependencies += ExclusionRule(organization = "org.scala-lang.modules", name = "scala-parser-combinators_2.13")

// Force consistent versions for both suffixed artifacts
dependencyOverrides += "org.scala-lang.modules" % "scala-parser-combinators_3" % "2.4.0"
//dependencyOverrides += "org.scala-lang.modules" % "scala-parser-combinators_2.13" % "2.4.0"

// Add JavaFX platform-specific artifacts (classifier based on OS)
libraryDependencies ++= Seq(
  "org.openjfx" % "javafx-base" % javafxV classifier BuildUtils.javafxClassifier,
  "org.openjfx" % "javafx-controls" % javafxV classifier BuildUtils.javafxClassifier,
  "org.openjfx" % "javafx-web" % javafxV classifier BuildUtils.javafxClassifier,
  "org.openjfx" % "javafx-graphics" % javafxV classifier BuildUtils.javafxClassifier,
  "org.openjfx" % "javafx-media" % javafxV classifier BuildUtils.javafxClassifier
)

// Task: prepareJpackage - copies the compiled jar and all dependencies (including JavaFX) into target/package/libs
prepareJpackage := {
  // Extract all the necessary values using the modern slash syntax
  val log = streams.value.log
  val t = target.value
  val packageJar = (Compile / packageBin).value
  val upd = update.value

  // Call the extracted implementation function
  prepareJpackageImpl(log, t, packageJar, upd)
}

// Resource directories (keep same structure as Maven project)
Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "resources"

// Include scala source directories
Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "scala"
Test / unmanagedSourceDirectories += baseDirectory.value / "src" / "test" / "scala"

// ============================================================================
// Platform-specific jpackage configuration and tasks
// ============================================================================


jpackageApp := {
  // Extract all the necessary values using the modern slash syntax
  val log = streams.value.log
  val baseDir = baseDirectory.value
  val targetDir = target.value
  val sourceDir = (Compile / sourceDirectory).value
  val appVersion = version.value

  // Call the extracted implementation function
  executeJpackage(
    log,
    baseDir,
    targetDir,
    sourceDir,
    appVersion
  )
}

// Add command aliases for convenience
addCommandAlias("packageApp", "prepareJpackage; jpackageApp")

// Expose a quick run task that mirrors the main class used by maven (KuTuApp)
Compile / mainClass := Some("ch.seidel.kutu.KuTuApp")

// Enable recommended forked test reporter for better compatibility
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
