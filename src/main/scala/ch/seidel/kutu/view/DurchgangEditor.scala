package ch.seidel.kutu.view

import ch.seidel.kutu.domain._
import scalafx.beans.property._

object DurchgangEditor {
  def apply(wettkampfid: Long, name: String, initstartriegen: Seq[RiegeEditor]): DurchgangEditor = {
      val anz = initstartriegen.map(_.initanz).sum
      new DurchgangEditor(wettkampfid, name, anz,
          initstartriegen.filter{riege => riege.initstart match {
              case Some(s) => name.equalsIgnoreCase(riege.durchgang.value)
              case _ => false
            }
          }.map{riege =>
            (riege.initstart.get, riege)
          }.groupBy(_._1).map(t => (t._1, t._2.map(_._2))).toMap
      )
  }
}

case class DurchgangEditor(wettkampfid: Long, initname: String, initanz: Int, initstartriegen: Map[Disziplin, Seq[RiegeEditor]]) {
  val name = StringProperty(initname)
  val anz = IntegerProperty(initanz)

//  def reset {
//    name.value = initname
//  }
//  def commit = RiegeRaw (
//      wettkampfId = wettkampfid,
//      r = name.value,
//      durchgang = if(durchgang.value.trim.length > 0) Some(durchgang.value.trim) else None,
//      start = if(start.value != null && start.value.isInstanceOf[Disziplin]) Some(start.value.asInstanceOf[Disziplin].id) else None
//    )
}
