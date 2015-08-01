package ch.seidel

import java.text.SimpleDateFormat
import ch.seidel.domain._
import scala.collection.mutable.HashMap

sealed trait GroupBy {
  val groupname: String
  private var next: Option[GroupBy] = None
  protected val grouper: (WertungView) => DataObject
  protected val sorter: Option[(GroupSection, GroupSection) => Boolean] //= leafsorter
  protected val leafsorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.sum.endnote > gs2.sum.endnote
  })

  override def toString = groupname

  def /(next: GroupBy): GroupBy = groupBy(next)

  def groupBy(next: GroupBy): GroupBy = {
    if(this == next) {
      next
    }
    else {
      this.next match {
        case Some(n) =>
          if(n != this) {
            n.groupBy(next)
            this
          }
          else {
            n
          }
        case None =>
          this.next = Some(next)
          this
      }
    }
  }

  def reset {
    next = None
  }

  def select(wvlist: Seq[WertungView]): Iterable[GroupSection] = {
    val grouped = wvlist groupBy grouper filter(g => g._2.size > 0)
    next match {
      case Some(ng) => mapAndSortNode(ng, grouped)
      case None     => mapAndSortLeaf(grouped)
    }
  }

  private def mapAndSortLeaf(grouped: Map[DataObject, Seq[WertungView]]) = {
    def reduce(switch: DataObject, list: Seq[WertungView]): Seq[GroupSection] = {
      Seq(GroupLeaf(switch, list))
    }
    sort(grouped.flatMap(x => reduce(x._1, x._2)).filter(g => g.sum.endnote > 0), sorter)
  }

  private def mapAndSortNode(ng: GroupBy, grouped: Map[DataObject, Seq[WertungView]]) = {
    sort(grouped.map { x =>
      val (grp, seq) = x
      val list = ng.select(seq)
      if(list.isEmpty) {
        GroupNode(grp, Seq(GroupSum(grp, Resultat(0, 0, 0), Resultat(0, 0, 0), Resultat(0, 0, 0))))
      }
      else {
        GroupNode(grp, ng.select(seq))
      }
    }.filter(g => g.sum.endnote > 0), sorter)
  }

  private def sort(mapped: Iterable[GroupSection], sorter: Option[(GroupSection, GroupSection) => Boolean]) = {
    sorter match {
      case Some(s) => mapped.toList.sortWith(s)
      case None    => mapped
    }
  }
}

case object ByNothing extends GroupBy {
  override val groupname = "keine"
  protected override val grouper = (v: WertungView) => {
    v
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.sum.endnote < gs2.sum.endnote
  })
}

case object ByProgramm extends GroupBy {
  override val groupname = "Programm"
  protected override val grouper = (v: WertungView) => {
    v.wettkampfdisziplin.programm
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[ProgrammView].ord.compareTo(gs2.groupKey.asInstanceOf[ProgrammView].ord) < 0
  })
}
case object ByWettkampfProgramm extends GroupBy {
  override val groupname = "Wettkampf-Programm"
  protected override val grouper = (v: WertungView) => {
    if(v.wettkampfdisziplin.programm.aggregator == v.wettkampfdisziplin.programm) {
      v.wettkampfdisziplin.programm
    }
    else {
    	v.wettkampfdisziplin.programm.head
    }
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[ProgrammView].ord.compareTo(gs2.groupKey.asInstanceOf[ProgrammView].ord) < 0
  })
}
case object ByWettkampfArt extends GroupBy {
  override val groupname = "Wettkampf-Art"
  protected override val grouper = (v: WertungView) => {
    v.wettkampfdisziplin.programm.head
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[ProgrammView].ord.compareTo(gs2.groupKey.asInstanceOf[ProgrammView].ord) < 0
  })
}
case object ByRiege extends GroupBy {
  override val groupname = "Riege"
  protected override val grouper = (v: WertungView) => {
    Riege(v.riege match {case Some(r) => r case None => "keine Einteilung"})
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.easyprint.compareTo(gs2.groupKey.easyprint) < 0
  })
}
case object ByJahrgang extends GroupBy {
  override val groupname = "Jahrgang"
  private val extractYear = new SimpleDateFormat("YYYY")
  protected override val grouper = (v: WertungView) => {
    v.athlet.gebdat match {
      case Some(d) => AthletJahrgang(extractYear.format(d))
      case None    => AthletJahrgang("unbekannt")
    }
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[AthletJahrgang].hg.compareTo(gs2.groupKey.asInstanceOf[AthletJahrgang].hg) < 0
  })
}
case object ByDisziplin extends GroupBy {
  override val groupname = "Disziplin"
  private val ordering = HashMap[Long, Long]()

  protected override val grouper = (v: WertungView) => {
    ordering.put(v.wettkampfdisziplin.disziplin.id, v.wettkampfdisziplin.ord.toLong)
    v.wettkampfdisziplin.disziplin
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    val go1 = ordering.getOrElse(gs1.groupKey.asInstanceOf[Disziplin].id, gs1.groupKey.asInstanceOf[Disziplin].id)
    val go2 = ordering.getOrElse(gs2.groupKey.asInstanceOf[Disziplin].id, gs2.groupKey.asInstanceOf[Disziplin].id)
    go1.compareTo(go2) < 0
  })
}
case object ByGeschlecht extends GroupBy {
  override val groupname = "Geschlecht"
  protected override val grouper = (v: WertungView) => {
    TurnerGeschlecht(v.athlet.geschlecht)
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[TurnerGeschlecht].easyprint.compareTo(gs2.groupKey.asInstanceOf[TurnerGeschlecht].easyprint) > 0
  })
}
case object ByVerein extends GroupBy {
  override val groupname = "Verein"
  protected override val grouper = (v: WertungView) => {
    v.athlet.verein match {
      case Some(v) => v
      case _       => Verein(0, "kein")
    }
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[Verein].name.compareTo(gs2.groupKey.asInstanceOf[Verein].name) < 0
  })
}
