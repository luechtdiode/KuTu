package ch.seidel.kutu.view

import java.sql.Date
import java.text.SimpleDateFormat

import ch.seidel.kutu.domain._
import scalafx.beans.property._

object AthletEditor {
  def apply(init: Athlet) = new AthletEditor(init)
  val coldef = Map(
    "jsid" -> 80,
    "geschlecht" -> 80,
    "name" -> 160,
    "vorname" -> 160,
    "gebdat" -> 80,
    "strasse" -> 180,
    "plz" -> 100,
    "ort" -> 180,
    "activ" -> 100
  )
}

class AthletEditor(init: Athlet) {
  val sdf = new SimpleDateFormat("dd.MM.yyyy")
  val jsid = new StringProperty(s"${init.js_id}")
  val geschlecht = new StringProperty(if init.geschlecht.toUpperCase.startsWith("M") then "M" else "W")
  val name = new StringProperty(init.name)
  val vorname = new StringProperty(init.vorname)
  val gebdat = new StringProperty(init.gebdat match {case Some(d) => sdf.format(d); case _ => ""})
  val strasse = new StringProperty(init.strasse)
  val plz = new StringProperty(init.plz)
  val ort = new StringProperty(init.ort)
  val activ = new StringProperty(if init.activ then {
    "Aktiv"
  } else {
    "Inaktiv"
  })

  def isValid = {
    name.value.nonEmpty &&
    vorname.value.nonEmpty &&
    geschlecht.value.nonEmpty &&
    (optionOfGebDat match {case Some(d) => true case _ => false})
  }

  def reset: Unit = {
    jsid.value_=(s"${init.js_id}")
  }

  private def optionOfGebDat: Option[Date] = {
    gebdat.value match {
      case "" => None
      case s: String => try {
        Some(new java.sql.Date(sdf.parse(s).getTime))
      }
      catch {
        case e: Exception => None
      }
    }
  }

  def commit = Athlet(init.id, Integer.valueOf(jsid.value), if geschlecht.value.toUpperCase.startsWith("M") then "M" else "W", name.value, vorname.value, optionOfGebDat, strasse.value, plz.value, ort.value, init.verein, activ.value.toUpperCase().startsWith("A"))
}
