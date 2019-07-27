package ch.seidel.kutu.view

import ch.seidel.kutu.domain._
import scalafx.beans.property._

object DurchgangEditor {
  def apply(wettkampfid: Long, name: String, initstartriegen: Seq[RiegeEditor]): DurchgangEditor = {
      new DurchgangEditor(wettkampfid, name,
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

case class DurchgangEditor(wettkampfid: Long, initname: String, initstartriegen: Map[Disziplin, Seq[RiegeEditor]]) {
  val name = StringProperty(initname)
  val anz = IntegerProperty(initstartriegen.map(r => r._2.map(rr => rr.initanz).sum).sum)
  val min = IntegerProperty(if(initstartriegen.size > 0) initstartriegen.map(r => r._2.map(rr => rr.initanz).sum).min else 0)
  val max = IntegerProperty(if(initstartriegen.size > 0) initstartriegen.map(r => r._2.map(rr => rr.initanz).sum).max else 0)
  val avg = IntegerProperty(if(initstartriegen.size > 0) anz.value / initstartriegen.size else 0)

  def riegenWithMergedClubs(): Map[Disziplin, Seq[(String,Int,Int)]] = {
    initstartriegen.map{
      case (key, riegen) =>
        (key -> riegen
          .groupBy(d => {
            val parts = d.initname.split(",")
            if (parts.length > 2) {
              parts(2) + " " + parts(1)
            } else d.initname
          })
            .map(r => (r._1, r._2.filter(_.initname.contains("W,")).map(_.initanz).sum, r._2.filter(!_.initname.contains("W,")).map(_.initanz).sum))
            .toSeq
        )
    }
  }

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
