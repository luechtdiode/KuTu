// Project plugins for sbt build (initial migration)
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.5.8")
// Native packager plugin removed temporarily to allow sbt plugin resolution during validation.
// Re-add `sbt-native-packager` later if you want automated jpackage/packaging integration.
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.1.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")
