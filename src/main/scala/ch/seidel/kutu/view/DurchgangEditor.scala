package ch.seidel.kutu.view

import ch.seidel.kutu.domain._
import scalafx.beans.property._

object DurchgangEditor {
  def apply(wettkampfid: Long, durchgang: Durchgang, initstartriegen: Seq[RiegeEditor]): DurchgangEditor = {
      CompetitionDurchgangEditor(wettkampfid, durchgang,
          initstartriegen.filter{riege => riege.initstart match {
              case Some(s) => durchgang.name.equalsIgnoreCase(riege.durchgang.value)
              case _ => false
            }
          }.map{riege =>
            (riege.initstart.get, riege)
          }.groupBy(_._1).map(t => (t._1, t._2.map(_._2)))
      )
  }
  def apply(group: List[DurchgangEditor]): List[GroupDurchgangEditor] = group.groupBy(_.durchgang.title)
    .map { case (durchgangTitle, editors) =>
      GroupDurchgangEditor(
        editors.head.wettkampfid,
        editors.map(_.durchgang).foldLeft(editors.head.durchgang){(acc, dg) => acc.toAggregator(dg)},
        editors.filter(_.durchgang.title == durchgangTitle).sortBy(_.durchgang.name))
    }
    .toList.sortBy(_.durchgang.title)
}

trait DurchgangEditor {
  val wettkampfid: Long
  val durchgang: Durchgang
  val isHeader = false
  def getAnz = if(initstartriegen.size > 0) initstartriegen.map(r => r._2.map(rr => rr.initanz).sum).sum else 0
  def getMin = if(initstartriegen.size > 0) initstartriegen.map(r => r._2.map(rr => rr.initanz).sum).min else 0
  def getMax = if(initstartriegen.size > 0) initstartriegen.map(r => r._2.map(rr => rr.initanz).sum).max else 0
  def getAvg = if(initstartriegen.size > 0) anz.value / initstartriegen.size else 0

  val name = StringProperty(durchgang.name)
  val title = StringProperty(durchgang.title)
  val cellvalue = StringProperty(if (durchgang.name == durchgang.title) durchgang.name else durchgang.name)
  val anz: IntegerProperty
  val min: IntegerProperty
  val max: IntegerProperty
  val avg: IntegerProperty

  val initstartriegen: Map[Disziplin, Seq[RiegeEditor]] = Map.empty
  def valueEditor(index: Disziplin): Seq[RiegeEditor] = initstartriegen.get(index).getOrElse(Seq.empty)

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
}

case class CompetitionDurchgangEditor(wettkampfid: Long, durchgang: Durchgang, override val initstartriegen: Map[Disziplin, Seq[RiegeEditor]]) extends DurchgangEditor {
  override val anz = IntegerProperty(getAnz)
  override val min = IntegerProperty(getMin)
  override val max = IntegerProperty(getMax)
  override val avg = IntegerProperty(getAvg)
}

case class GroupDurchgangEditor(wettkampfid: Long, durchgang: Durchgang, aggregates: List[DurchgangEditor]) extends DurchgangEditor {
  override val isHeader = true
  override val initstartriegen = aggregates
    .flatMap(_.initstartriegen)
    .foldLeft(Map[Disziplin, Seq[RiegeEditor]]()){ case (acc, (disz, riegen)) =>
      acc.updated(disz, acc.getOrElse(disz, Seq.empty) ++ riegen)
    }
  override val anz = IntegerProperty(getAnz)
  override val min = IntegerProperty(getMin)
  override val max = IntegerProperty(getMax)
  override val avg = IntegerProperty(getAvg)
}
