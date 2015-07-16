package ch.seidel

import java.text.SimpleDateFormat

import ch.seidel.domain._

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
    this.next match {
      case Some(n) => n.groupBy(next)
      case None    => this.next = Some(next)
    }
    this
  }

  def reset {
    next = None
  }

  def select(wvlist: Seq[WertungView]): Iterable[GroupSection] = {
    val grouped = wvlist groupBy grouper
    next match {
      case Some(ng) => mapAndSortNode(ng, grouped)
      case None     => mapAndSortLeaf(grouped)
    }
  }

  private def mapAndSortLeaf(grouped: Map[DataObject, Seq[WertungView]]) = {
    def x(switch: DataObject, list: Seq[WertungView]) = {
      val grouped = list.groupBy { x => x.athlet }.map { x =>
        val r = x._2.map(y => y.resultat).reduce((r1, r2) => r1 + r2)
        (x._1, r)
      }
      GroupSection.mapRang(grouped).toSeq
    }

    def reduce(switch: DataObject, list: Seq[WertungView]): Seq[GroupSection] = {
      list.toList match {
        //                case head :: _ if(head.wettkampfdisziplin.programm.aggregate > 0) =>
        //                  Seq(GroupNode(switch, sort(x(switch, list), leafsorter)))
        case _ =>
          Seq(GroupLeaf(switch, list))
      }
    }
    sort(grouped.flatMap(x => reduce(x._1, x._2)), sorter)
  }

  private def mapAndSortNode(ng: GroupBy, grouped: Map[DataObject, Seq[WertungView]]) = {
    sort(grouped.map { x =>
      val (grp, seq) = x
      GroupNode(grp, ng.select(seq))
    }, sorter)
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
  protected override val grouper = (v: WertungView) => {
    v.wettkampfdisziplin.disziplin
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[Disziplin].ord.compareTo(gs2.groupKey.asInstanceOf[Disziplin].ord) < 0
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
