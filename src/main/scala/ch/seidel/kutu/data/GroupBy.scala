package ch.seidel.kutu.data

import java.text.SimpleDateFormat
import ch.seidel.kutu.domain._
import scala.collection.mutable.HashMap
import scalafx.Includes._
import scala.math.BigDecimal.int2bigDecimal

sealed trait GroupBy {
  val groupname: String
  protected var next: Option[GroupBy] = None
  protected val grouper: (WertungView) => DataObject
  protected val sorter: Option[(GroupSection, GroupSection) => Boolean] //= leafsorter
  protected val leafsorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.sum.endnote > gs2.sum.endnote
  })

  override def toString = groupname

  def /(next: GroupBy): GroupBy = groupBy(next)

  def traverse[T >: GroupBy, A](accumulator: A)(op: (T, A) => A): A = {
    next match {
      case Some(gb) => gb.traverse(op(this, accumulator)) { op }
      case _ => op(this, accumulator)
    }
  }

  def groupBy[T >: GroupBy](next: T): T = {
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
          this.next = Some(next.asInstanceOf[GroupBy])
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

sealed trait FilterBy extends GroupBy {
  def filterItems: List[DataObject] = filtItems
  protected def items(fromData: Seq[WertungView]): List[DataObject]
  private[FilterBy] var filter: Option[DataObject] = None
  private[FilterBy] var filtItems: List[DataObject] = List()

  def analyze(wvlist: Seq[WertungView]): Seq[DataObject] = {
    filtItems = items(wvlist)
    filtItems
  }

  override def select(wvlist: Seq[WertungView]): Iterable[GroupSection] = {
    filtItems = items(wvlist)
    super.select(wvlist.filter(g => if(getFilter.isDefined) getFilter.get.equals(grouper(g)) else true))
  }

  def setFilter(f: Option[DataObject]) {
    filter = f
  }
  def getFilter = {
    filter
  }
  override def reset {
    super.reset
    filter = None
  }

}

case object ByNothing extends GroupBy with FilterBy {
  override val groupname = "keine"
  protected override val grouper = (v: WertungView) => {
    v
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.sum.endnote < gs2.sum.endnote
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case object ByAthlet extends GroupBy with FilterBy {
  override val groupname = "Athlet"
  protected override val grouper = (v: WertungView) => {
    v.athlet
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.sum.endnote < gs2.sum.endnote
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case class ByProgramm(text: String = "Programm/Kategorie") extends GroupBy with FilterBy {
  override val groupname = text
  protected override val grouper = (v: WertungView) => {
    v.wettkampfdisziplin.programm
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[ProgrammView].ord.compareTo(gs2.groupKey.asInstanceOf[ProgrammView].ord) < 0
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case class ByWettkampfProgramm(text: String = "Programm/Kategorie") extends GroupBy with FilterBy {
  override val groupname = "Wettkampf-" + text
  protected override val grouper = (v: WertungView) => {
    v.wettkampfdisziplin.programm.wettkampfprogramm
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[ProgrammView].ord.compareTo(gs2.groupKey.asInstanceOf[ProgrammView].ord) < 0
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}
case object ByWettkampfArt extends GroupBy with FilterBy {
  override val groupname = "Wettkampf-Art"
  protected override val grouper = (v: WertungView) => {
    v.wettkampfdisziplin.programm.head
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[ProgrammView].ord.compareTo(gs2.groupKey.asInstanceOf[ProgrammView].ord) < 0
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case object ByRiege extends GroupBy with FilterBy {
  override val groupname = "Riege"
  protected override val grouper = (v: WertungView) => {
    Riege(v.riege match {case Some(r) => r case None => "keine Einteilung"}, None, None)
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.easyprint.compareTo(gs2.groupKey.easyprint) < 0
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case object ByJahr extends GroupBy with FilterBy {
  override val groupname = "Wettkampf-Jahr"
  private val extractYear = new SimpleDateFormat("YYYY")
  protected override val grouper = (v: WertungView) => {
    WettkampfJahr(extractYear.format(v.wettkampf.datum))
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[WettkampfJahr].hg.compareTo(gs2.groupKey.asInstanceOf[WettkampfJahr].hg) < 0
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case object ByJahrgang extends GroupBy with FilterBy {
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

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case object ByDisziplin extends GroupBy with FilterBy {
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

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case object ByGeschlecht extends GroupBy with FilterBy {
  override val groupname = "Geschlecht"
  protected override val grouper = (v: WertungView) => {
    TurnerGeschlecht(v.athlet.geschlecht)
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[TurnerGeschlecht].easyprint.compareTo(gs2.groupKey.asInstanceOf[TurnerGeschlecht].easyprint) > 0
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case object ByVerein extends GroupBy with FilterBy {
  override val groupname = "Verein"
  protected override val grouper = (v: WertungView) => {
    v.athlet.verein match {
      case Some(v) => v
      case _       => Verein(0, "kein", None)
    }
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[Verein].name.compareTo(gs2.groupKey.asInstanceOf[Verein].name) < 0
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}

case object ByVerband extends GroupBy with FilterBy {
  override val groupname = "Verband"
  protected override val grouper = (v: WertungView) => {
    v.athlet.verein match {
      case Some(v) => Verband(v.verband.getOrElse("kein"))
      case _       => Verband("kein")
    }
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.asInstanceOf[Verband].name.compareTo(gs2.groupKey.asInstanceOf[Verband].name) < 0
  })

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.map(_._1).toList
  }
}