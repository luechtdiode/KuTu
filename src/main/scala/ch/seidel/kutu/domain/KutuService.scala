package ch.seidel.kutu.domain

import java.text.SimpleDateFormat
import java.text.ParseException
import java.time.LocalDate
import java.time.temporal.TemporalField
import java.time.Period
import java.sql.Date
import java.util.concurrent.TimeUnit
import java.util.Properties
import java.io.File

import scala.io.Source
import scala.annotation.tailrec
import scala.collection.JavaConversions
import scala.concurrent.ExecutionContext.Implicits.global

import slick.jdbc.JdbcBackend.Database
//import slick.jdbc.JdbcBackend.Session
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import slick.jdbc.PositionedResult
import slick.jdbc.GetResult
//import slick.jdbc.SetParameter
//import slick.lifted.Query

import org.sqlite.SQLiteConfig.Pragma
import org.sqlite.SQLiteConfig.DatePrecision

import java.util.Arrays.ArrayList
import java.util.Collections
import java.util.ArrayList
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.collection.JavaConverters
import ch.seidel.kutu.squad._
import org.slf4j.LoggerFactory

trait KutuService extends DBService 
  with VereinService 
  with AthletService 
  with WettkampfService 
  with RiegenService
  with DurchgangService
  with KampfrichterService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  
}