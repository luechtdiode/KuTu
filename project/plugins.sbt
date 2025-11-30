// Project plugins for sbt build (initial migration)
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.5.8")
// Native packager plugin removed temporarily to allow sbt plugin resolution during validation.
// Re-add `sbt-native-packager` later if you want automated jpackage/packaging integration.
// sbt-native-packager: enables `jpackage` and platform installers
// To enable automated jpackage integration, uncomment and adjust the version below
// Ensure the sbt plugin resolver is available so plugin artifacts can be resolved.
// resolvers += "sbt-plugin-releases" at "https://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"
// addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.9.25")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.1.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")
addSbtPlugin("io.gatling" % "gatling-sbt" % "4.20.8")