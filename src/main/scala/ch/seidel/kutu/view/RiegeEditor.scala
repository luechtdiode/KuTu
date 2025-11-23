package ch.seidel.kutu.view

import ch.seidel.kutu.domain.*
import scalafx.beans.property.*

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
  val kind = if initviewanz + initanz == 0 then 1 else 0
  if onSelectedChange.isDefined then {
    selected onChange {
      selected.value = onSelectedChange.get(initname, selected.value)
    }
  }
  def reset: Unit = {
    name.value = initname
    durchgang.value = initdurchgang.getOrElse("")
    start.value = initstart.getOrElse(null)
  }
  def commit: RiegeRaw = {
    RiegeRaw (
      wettkampfId = wettkampfid,
      r = name.value,
      durchgang = if durchgang.value.trim.length > 0 then Some(durchgang.value.trim) else None,
      start = if start.value != null && start.value.isInstanceOf[Disziplin] then Some(start.value.asInstanceOf[Disziplin].id) else None,
      kind = kind
    )
  }
}
