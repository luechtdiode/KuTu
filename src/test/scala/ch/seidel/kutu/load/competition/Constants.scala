package ch.seidel.kutu.load.competition

import io.gatling.core.Predef.*
import io.gatling.core.feeder.FeederBuilder
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef.*

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

object Constants {
  val numberOfUsers: Int = 500//System.getProperty("numberOfUsers").toInt
  val duration: FiniteDuration = FiniteDuration(2, TimeUnit.MINUTES)//System.getProperty("durationMinutes").toInt.minutes
  val pause: FiniteDuration = FiniteDuration(2000, TimeUnit.MILLISECONDS)//System.getProperty("pauseBetweenRequestsMs").toInt.millisecond
  val responseTimeMs = 500
  val responseSuccessPercentage = 99
  private val url: String = "http://localhost:5757"//System.getProperty("url")
  private val repeatTimes: Int = 1//System.getProperty("numberOfRepetitions").toInt
  private val successStatus: Int = 200
  private val isDebug = System.getProperty("debug").toBoolean

  // Define HTTP protocol to be used in simulations
  val httpProtocol = http
    .baseUrl(url)
    // Check response code is 200
    .check(status.is(successStatus))

  /**
    * Creates a scenario by given, name, feed and executions.
    * @param name Scenario name
    * @param feed Feed used to put data into session
    * @param chains Executable that are chained together
    * @return
    */
  def createScenario(name: String, feed: FeederBuilder, chains: ChainBuilder*): ScenarioBuilder = {
    // Do given amount of repetitions only
    if (Constants.repeatTimes > 0) {
      scenario(name).feed(feed).repeat(Constants.repeatTimes) {
        exec(chains).pause(Constants.pause)
      }
    } else {
      // Loop forever, it is important to put maxDuration() in Simulation setUp() method
      scenario(name).feed(feed).forever(name) {
        exec(chains).pause(Constants.pause)
      }
    }
  }

}