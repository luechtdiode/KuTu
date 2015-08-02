package ch.seidel.domain

import scala.io.Source
import java.io.File
import scala.annotation.tailrec

/**
 * @author Roland
 */
object TestResourceLoader extends App with KutuService {

  val vereinCsv = Source.fromFile(new File("./db/verein_201508012103.csv")).getLines()
  val vereinHeader = vereinCsv.take(1).map(_.dropWhile {_.isUnicodeIdentifierPart }).flatMap(parseLine).zipWithIndex.toMap
  println(vereinHeader)
  val vereinNameIdx = vereinHeader("name")
  val vereinIdIdx = vereinHeader("id")
  val vereinInstances = vereinCsv.map(parseLine).map{fields =>
    val candidate = Verein(id = 0, name = fields(vereinNameIdx))
    val verein = insertVerein(candidate)
    (fields(vereinIdIdx), verein)
  }.toMap
  println(vereinInstances.toList)

  val athletCsv = Source.fromFile(new File("./db/athlet_201508012323.csv")).getLines()
  val athletHeader = athletCsv.take(1).map(_.dropWhile {_.isUnicodeIdentifierPart }).flatMap(parseLine).zipWithIndex.toMap
  println(athletHeader)
  val athletInstances = athletCsv.map(parseLine).map{fields =>
    val candidate = Athlet(
        id = 0,
        js_id = fields(athletHeader("js_id")),
        geschlecht = fields(athletHeader("geschlecht")),
        name = fields(athletHeader("name")),
        vorname = fields(athletHeader("vorname")),
        gebdat = Some(fields(athletHeader("gebdat"))),
        strasse = fields(athletHeader("strasse")),
        plz = fields(athletHeader("plz")),
        ort = fields(athletHeader("ort")),
        verein = Some(vereinInstances(fields(athletHeader("verein"))).id),
        activ = fields(athletHeader("activ")).toUpperCase() match {case "TRUE" => true case _ => false}
        )
    val athlet = insertAthlete(candidate)
    (fields(athletHeader("id")), athlet)
  }.toMap

  val wettkampfCsv = Source.fromFile(new File("./db/wettkampf_201508012104.csv")).getLines()
  val wettkampfHeader = wettkampfCsv.take(1).map(_.dropWhile {_.isUnicodeIdentifierPart }).flatMap(parseLine).zipWithIndex.toMap
  println(wettkampfHeader)
  val wettkampfInstances = wettkampfCsv.map(parseLine).map{fields =>
    val wettkampf = createWettkampf(
        auszeichnung = fields(wettkampfHeader("auszeichnung")),
        datum = fields(wettkampfHeader("datum")),
        programmId = Set(fields(wettkampfHeader("programm_id"))),
        titel = fields(wettkampfHeader("titel"))
        )
    (fields(wettkampfHeader("id")), wettkampf)
  }.toMap

}