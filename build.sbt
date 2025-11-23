// Auto-generated sbt build translated from pom.xml (initial migration)
ThisBuild / organization := "ch.seidel"
ThisBuild / version := "2.3.22"
ThisBuild / scalaVersion := "3.7.3"

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

// Determine JavaFX classifier for current OS
def javafxClassifier: String = {
  val os = sys.props.getOrElse("os.name", "").toLowerCase
  val arch = sys.props.getOrElse("os.arch", "").toLowerCase
  if (os.contains("win")) "win"
  else if (os.contains("mac") && arch.contains("aarch64")) "mac-aarch64"
  else if (os.contains("mac")) "mac"
  else "linux"
}

// Compiler options (inspired by scala-maven-plugin args)
Compile / scalacOptions ++= {
  val base = Seq(
    "-unchecked",
    "-deprecation",
    "-explaintypes",
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-language:existentials",
    "-g:line"
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
  "--enable-native-access=ALL-UNNAMED"
)

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
  "io.spray" %% "spray-json" % "1.3.6",

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
  "org.openjfx" % "javafx-base" % javafxV classifier javafxClassifier,
  "org.openjfx" % "javafx-controls" % javafxV classifier javafxClassifier,
  "org.openjfx" % "javafx-web" % javafxV classifier javafxClassifier,
  "org.openjfx" % "javafx-graphics" % javafxV classifier javafxClassifier,
  "org.openjfx" % "javafx-media" % javafxV classifier javafxClassifier
)

// Assembly (uber-jar) settings to prepare a package input for jpackage
import sbtassembly.AssemblyPlugin.autoImport.*
assembly / assemblyJarName := s"${name.value}-${version.value}.jar"
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

// Task: prepareJpackage - builds an assembly jar and collects JavaFX native jars into target/package/libs
lazy val prepareJpackage = taskKey[Unit]("Prepare directory for jpackage: assembly jar + JavaFX native jars")
prepareJpackage := {
  val log = streams.value.log
  val out = target.value / "package"
  val libs = out / "libs"
  IO.delete(out)
  IO.createDirectory(libs)

  log.info("Running assembly to create fat jar...")
  val jar: File = (Compile / assembly).value
  val jarTarget = out / jar.getName
  IO.copyFile(jar, jarTarget)
  log.info(s"Copied assembly jar to: ${jarTarget.getAbsolutePath}")

  // find resolved JavaFX jars from update report
  val upd = update.value
  val javafxJars = upd.matching(moduleFilter(organization = "org.openjfx"))
  if (javafxJars.isEmpty) log.warn("No JavaFX jars resolved. Ensure JavaFX dependencies exist for the classifier.")
  javafxJars.foreach { f =>
    IO.copyFile(f, libs / f.getName)
    log.info(s"Copied JavaFX jar: ${f.getName}")
  }

  log.info(s"Prepared jpackage input in: ${out.getAbsolutePath}")
  log.info("Run jpackage manually using the assembly jar and libs folder; example jpackage args are in the README.")
}

//logLevel := Level.Error

// Resource directories (keep same structure as Maven project)
Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "resources"

// Include scala source directories
Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "scala"
Test / unmanagedSourceDirectories += baseDirectory.value / "src" / "test" / "scala"

// Packaging / assembly / native packaging are left as next steps. Use sbt-native-packager or sbt-assembly as needed.

// Expose a quick run task that mirrors the main class used by maven (KuTuApp)
Compile / mainClass := Some("ch.seidel.kutu.KuTuApp")

// Enable recommended forked test reporter for better compatibility
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
