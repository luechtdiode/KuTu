//package ch.seidel.kutu.load
//
//import io.gatling.recorder.GatlingRecorder
//import io.gatling.recorder.config.RecorderPropertiesBuilder
//
//object Recorder extends App {
//
//	val props = new RecorderPropertiesBuilder()
//		.simulationsFolder(IDEPathHelper.recorderSimulationsDirectory.toString)
//		.simulationPackage("ch.seidel.kutu.load.competition")
//		.resourcesFolder(IDEPathHelper.resourcesDirectory.toString)
//
//	GatlingRecorder.fromMap(props.build, Some(IDEPathHelper.recorderConfigFile))
//}