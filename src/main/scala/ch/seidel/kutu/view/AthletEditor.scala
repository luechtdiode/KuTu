package ch.seidel.kutu.view

import ch.seidel.kutu.domain._
import scalafx.beans.property._
import java.text.SimpleDateFormat
import java.sql.Date

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
  val jsid = new StringProperty(init.js_id + "")
  val geschlecht = new StringProperty(if(init.geschlecht.toUpperCase.startsWith("M")) "M" else "W")
  val name = new StringProperty(init.name)
  val vorname = new StringProperty(init.vorname)
  val gebdat = new StringProperty(init.gebdat match {case Some(d) => sdf.format(d); case _ => ""})
  val strasse = new StringProperty(init.strasse)
  val plz = new StringProperty(init.plz)
  val ort = new StringProperty(init.ort)
  val activ = new StringProperty(init.activ match {case true => "Aktiv" case _ => "Inaktiv"})

  def reset {
    jsid.value_=(init.js_id + "")
  }

  private def optionOfGebDat: Option[Date] = {
    gebdat.value match {
      case "" => None
      case s: String => Some(new java.sql.Date(sdf.parse(s).getTime()))
    }
  }

  def commit = Athlet(init.id, Integer.valueOf(jsid.value), if(geschlecht.value.toUpperCase.startsWith("M")) "M" else "W", name.value, vorname.value, optionOfGebDat, strasse.value, plz.value, ort.value, init.verein, activ.value.toUpperCase().startsWith("A"))
}
