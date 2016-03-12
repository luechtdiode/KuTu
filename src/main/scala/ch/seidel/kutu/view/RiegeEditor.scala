package ch.seidel.kutu.view

import ch.seidel.kutu.domain._
import scalafx.beans.property._

object RiegeEditor {
  def apply(wettkampfid: Long, anz: Int, viewanz: Int, enabled: Boolean, riege: Riege, onSelectedChange: Option[(String, Boolean) => Boolean]): RiegeEditor =
    RiegeEditor(wettkampfid, riege.r, anz, viewanz, enabled, riege.durchgang, riege.start, onSelectedChange)
}

case class RiegeEditor(wettkampfid: Long, initname: String, initanz: Int, initviewanz: Int, enabled: Boolean, initdurchgang: Option[String], initstart: Option[Disziplin], onSelectedChange: Option[(String, Boolean) => Boolean]) {
  val selected = BooleanProperty(enabled)
  val name = StringProperty(initname)
  val anz = IntegerProperty(initanz)
  val anzkat = StringProperty(s"$initviewanz/$initanz")
  val durchgang = StringProperty(initdurchgang.getOrElse(""))
  val start = ObjectProperty(initstart.getOrElse(null))
  if(onSelectedChange.isDefined) {
    selected onChange {
      selected.value = onSelectedChange.get(initname, selected.value)
    }
  }
  def reset {
    name.value = initname
    durchgang.value = initdurchgang.getOrElse("")
    start.value = initstart.getOrElse(null)
  }
  def commit = {
    RiegeRaw (
      wettkampfId = wettkampfid,
      r = name.value,
      durchgang = if(durchgang.value.trim.length > 0) Some(durchgang.value.trim) else None,
      start = if(start.value != null && start.value.isInstanceOf[Disziplin]) Some(start.value.asInstanceOf[Disziplin].id) else None
    )
  }
}
