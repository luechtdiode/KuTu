package ch.seidel.kutu.load

import java.nio.file.Path

object IDEPathHelper {

  val gatlingConfUrl: Path = Path.of(getClass.getClassLoader.getResource("gatling.conf").toURI)
  val projectRootDir: Path = gatlingConfUrl.subpath(0, 3)

  val mavenSourcesDirectory: Path = projectRootDir.resolve("src/test/scala")
  val mavenResourcesDirectory: Path = projectRootDir.resolve("src/test/resources")
  val mavenTargetDirectory: Path = projectRootDir.resolve("target")
  val mavenBinariesDirectory: Path = mavenTargetDirectory.resolve("test-classes")

  val resourcesDirectory: Path = mavenResourcesDirectory
  val recorderSimulationsDirectory: Path = mavenSourcesDirectory
  val resultsDirectory: Path = mavenTargetDirectory.resolve("gatling")
  val recorderConfigFile: Path = mavenResourcesDirectory.resolve("recorder.conf")
}