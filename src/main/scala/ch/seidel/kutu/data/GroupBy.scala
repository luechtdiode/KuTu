package ch.seidel.kutu.data

import ch.seidel.kutu.domain.*

import java.net.URLDecoder
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.{LocalDate, Period}
import scala.collection.mutable
import scala.math.BigDecimal.int2bigDecimal

import ch.seidel.kutu.domain.given_Conversion_String_Int
import ch.seidel.kutu.domain.given_Conversion_Date_LocalDate

object ScoreListKind {
  def apply(kind: Option[String]): ScoreListKind = kind
    .map {
      case "Einzelrangliste" => Einzelrangliste
      case "Teamrangliste" => Teamrangliste
      case "Kombirangliste" => Kombirangliste
      case _ => Einzelrangliste
    }.getOrElse(Einzelrangliste)
}
sealed trait ScoreListKind
case object Einzelrangliste extends ScoreListKind
case object Teamrangliste extends ScoreListKind
case object Kombirangliste extends ScoreListKind

object ScoreListBestN {
  private val bestOfNCountPattern = "^beste\\(([0-5]{1})\\)$".r
  def apply(value: Option[String]): ScoreListBestN = value
    .map {
      case "all" => AlleWertungen
      case bestOfNCountPattern(count) => BestNWertungen(count)
      case _ => AlleWertungen
    }.getOrElse(AlleWertungen)
}
sealed trait ScoreListBestN extends DataObject {
  val counting: Option[Int]
  val text: String
  def urlParam: String = if counting.isEmpty || counting.get < 1 then "alle" else s"beste(${counting.get})"
  override def toString: String = text
}
case object AlleWertungen extends ScoreListBestN {
  override val counting: Option[Nothing] = None
  override val text: String = "Alle Wertungen"
}
case class BestNWertungen(c: Int) extends ScoreListBestN {
  override val counting: Option[Int] = Some(c)
  override val text: String = if c > 1 then s"Beste $c Wertungen" else "Beste Wertung"
}

sealed trait GroupBy {
  val groupname: String
  protected var next: Option[GroupBy] = None
  protected var isANO: Boolean = false
  protected var isAVG: Boolean = true
  protected var bestNCounting: ScoreListBestN = AlleWertungen
  protected var kind: ScoreListKind = Einzelrangliste

  protected def allName: String = groupname

  protected val allgrouper: WertungView => DataObject = (w: WertungView) => NullObject(allName).asInstanceOf[DataObject]
  protected val grouper: WertungView => DataObject
  protected val sorter: Option[(GroupSection, GroupSection) => Boolean]

  override def toString: String = groupname

  def getKind: ScoreListKind = kind
  def setKind(value: ScoreListKind): Unit = {
    traverse(value) { (gb, acc) =>
      gb.kind = acc
      acc
    }
  }
  def isAlphanumericOrdered: Boolean = isANO

  def setAlphanumericOrdered(value: Boolean): Unit = {
    traverse(value) { (gb, acc) =>
      gb.isANO = acc
      acc
    }
  }

  def isAvgOnMultipleCompetitions: Boolean = isAVG

  def setAvgOnMultipleCompetitions(value: Boolean): Unit = {
    traverse(value) { (gb, acc) =>
      gb.isAVG = acc
      acc
    }
  }
  def getBestNCounting: ScoreListBestN = bestNCounting

  def setBestNCounting(value: ScoreListBestN): Unit = {
    traverse(value) { (gb, acc) =>
      gb.bestNCounting = acc
      acc
    }
  }

  def toRestQuery: String = {
    val groupby = traverse("") { (gb, acc) =>
      acc match {
        case "" =>
          gb.groupname
        case _ =>
          acc + "," + gb.groupname
      }
    }
    s"groupby=$groupby" +
      (if isANO then "&alphanumeric" else "") +
      (if isAVG then "&avg=true" else "&avg=false") +
      s"&kind=$kind&counting=${bestNCounting.urlParam}"
  }

  def chainToString: String = s"$groupname (skipGrouper: $skipGrouper, $allName)" + (next match {
    case Some(gb) => "\n\t/" + gb.chainToString
    case _ => ""
  })

  def skipGrouper: Boolean = false

  def canSkipGrouper: Boolean = false

  def /(next: GroupBy): GroupBy = groupBy(next)

  def traverse[T >: GroupBy, A](accumulator: A)(op: (T, A) => A): A = {
    next match {
      case Some(gb) => gb.traverse(op(this, accumulator)) {
        op
      }
      case _ => op(this, accumulator)
    }
  }

  def groupBy[T >: GroupBy](next: T): T = {
    if this == next then {
      next
    }
    else {
      this.next match {
        case Some(n) =>
          if n != this then {
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

  def reset: Unit = {
    setAlphanumericOrdered(false)
    setAvgOnMultipleCompetitions(true)
    setBestNCounting(AlleWertungen)
    next = None
  }

  def select(wvlist: Seq[WertungView]): Iterable[GroupSection] = {
    val grouped = if skipGrouper then {
      wvlist groupBy allgrouper filter (g => g._2.nonEmpty)
    }
    else {
      wvlist groupBy grouper filter (g => g._2.nonEmpty)
    }

    next match {
      case Some(ng) => mapAndSortNode(ng, grouped)
      case None => mapAndSortLeaf(grouped)
    }
  }

  private def mapAndSortLeaf(grouped: Map[DataObject, Seq[WertungView]]) = {
    def reduce(switch: DataObject, list: Seq[WertungView]): Seq[GroupSection] = {
      val rl = list.filter(_.showInScoreList)
      if rl.nonEmpty then {
        val gl = GroupLeaf(switch, rl, bestOfCountOverride = getBestNCounting.counting)
        kind match {
          case Teamrangliste => TeamSums(gl)
          case Einzelrangliste => Seq(gl)
          case Kombirangliste => gl +: TeamSums(gl)
        }
      } else {
        Seq()
      }
    }

    sort(grouped.flatMap(x => reduce(x._1, x._2)).filter(g => g.sum.endnote > 0), sorter)
  }

  private def mapAndSortNode(ng: GroupBy, grouped: Map[DataObject, Seq[WertungView]]) = {
    sort(grouped.flatMap { x =>
      val (grp, seq) = x
      val list = ng.select(seq)
      if list.nonEmpty then {
        Seq(GroupNode(grp, list))
      } else {
        Seq()
      }
    }.filter(g => g.sum.endnote > 0), sorter)
  }

  private def sort(mapped: Iterable[GroupSection], sorter: Option[(GroupSection, GroupSection) => Boolean]) = {
    sorter match {
      case Some(s) => mapped.toList.sortWith(s)
      case None => mapped
    }
  }
}

sealed trait FilterBy extends GroupBy {

  def items(fromData: Seq[WertungView]): List[DataObject] = {
    val grp = fromData.groupBy(grouper)
    grp.keys.toList
  }

  override def toRestQuery: String = {
    val (groupby, filter) = traverse(("", List[String]())) { (gb, acc) =>
      val (groupby, filter) = acc
      (
        if gb.skipGrouper then {
          groupby
        } else if groupby.isEmpty then {
          gb.groupname
        } else {
          groupby + ":" + gb.groupname
        },
        gb match {
          case f: FilterBy if f.getFilter.nonEmpty =>
            if f.skipGrouper then {
              filter :+ s"&filter=${gb.groupname}:${f.getFilter.map(s => encodeURIParam(s.easyprint)).mkString("!")}"
            } else {
              filter :+ s"&filter=${gb.groupname}:${f.getFilter.map(s => encodeURIParam(s.easyprint)).mkString("!")}"
            }
          case _ =>
            filter
        }
      )
    }
    s"groupby=$groupby${filter.mkString}" +
      (if isANO then "&alphanumeric" else "") +
      (if isAVG then "&avg=true" else "&avg=false") +
      s"&kind=$kind&counting=${bestNCounting.urlParam}"
  }

  private[FilterBy] var filter: Set[DataObject] = Set.empty
  private[FilterBy] var filtItems: List[DataObject] = List.empty

  private[FilterBy] def nullObjectFilter = (d: DataObject) => d match {
    case NullObject(_) => true
    case _ => false
  }

  def filterItems: List[DataObject] =
    if skipGrouper then {
      filtItems ++ getFilter.filter(nullObjectFilter)
    } else {
      filtItems
    }

  def analyze(wvlist: Seq[WertungView]): Seq[DataObject] = {
    filtItems = items(wvlist)
    filtItems
  }

  override protected def allName: String = {
    getFilter.filterNot(nullObjectFilter).map(_.easyprint).mkString("[", ", ", "]")
  }

  override def select(wvlist: Seq[WertungView]): Iterable[GroupSection] = {
    filtItems = items(wvlist)
    super.select(wvlist.filter(g => if getFilter.nonEmpty then getFilter.contains(grouper(g)) else true))
  }

  override def canSkipGrouper: Boolean = getFilter.filterNot(nullObjectFilter).size > 1

  override def skipGrouper: Boolean = getFilter.exists {
    nullObjectFilter
  } && canSkipGrouper

  def setFilter(f: Set[DataObject]): Unit = {
    filter = f
  }

  def getFilter: Set[DataObject] = {
    filter
  }

  override def reset: Unit = {
    super.reset
    filter = Set.empty
  }

}

case class ByNothing() extends GroupBy with FilterBy {
  override val groupname = "keine"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    v
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.sum.endnote < gs2.sum.endnote
  })
}

case class ByAthlet() extends GroupBy with FilterBy {
  override val groupname = "Athlet"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    v.athlet
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.sum.endnote < gs2.sum.endnote
  })
}


case class ByDurchgang(riegenZuDurchgang: Map[String, Durchgang]) extends GroupBy with FilterBy {
  override val groupname = "Durchgang"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    riegenZuDurchgang.getOrElse(v.riege.getOrElse(""), riegenZuDurchgang.getOrElse(v.riege2.getOrElse(""), Durchgang()))
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: Durchgang, p2: Durchgang) =>
        p1.name.compareTo(p2.name) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

case class ByProgramm(text: String = "Programm/Kategorie") extends GroupBy with FilterBy {
  override val groupname: String = text
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    v.wettkampfdisziplin.programm
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: ProgrammView, p2: ProgrammView) =>
        p1.ord.compareTo(p2.ord) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

case class ByWettkampfProgramm(text: String = "Programm/Kategorie") extends GroupBy with FilterBy {
  override val groupname: String = "Wettkampf-" + text
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    v.wettkampfdisziplin.programm.wettkampfprogramm
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: ProgrammView, p2: ProgrammView) =>
        p1.ord.compareTo(p2.ord) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

case class ByWettkampfArt() extends GroupBy with FilterBy {
  override val groupname: String = "Wettkampf-Art"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    v.wettkampfdisziplin.programm.head
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: ProgrammView, p2: ProgrammView) =>
        p1.ord.compareTo(p2.ord) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

case class ByWettkampf() extends GroupBy with FilterBy {
  override val groupname = "Wettkampf"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    v.wettkampf
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: Wettkampf, p2: Wettkampf) =>
        p1.datum.compareTo(p2.datum) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

case class ByRiege() extends GroupBy with FilterBy {
  override val groupname = "Riege"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    Riege(v.riege match { case Some(r) => r case None => "Ohne Einteilung" }, None, None, 0)
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.easyprint.compareTo(gs2.groupKey.easyprint) < 0
  })
}
case class ByRiege2() extends GroupBy with FilterBy {
  override val groupname = "Riege2"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    Riege(v.riege2 match { case Some(r) => r case None => "Ohne Spezial-Einteilung" }, None, None, 0)
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.easyprint.compareTo(gs2.groupKey.easyprint) < 0
  })
}

case class ByJahr() extends GroupBy with FilterBy {
  override val groupname = "Wettkampf-Jahr"
  private val extractYear = new SimpleDateFormat("YYYY")
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    WettkampfJahr(extractYear.format(v.wettkampf.datum))
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: WettkampfJahr, p2: WettkampfJahr) =>
        p1.wettkampfjahr.compareTo(p2.wettkampfjahr) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

case class ByJahrgang() extends GroupBy with FilterBy {
  override val groupname = "Jahrgang"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    v.athlet.gebdat match {
      case Some(d) => AthletJahrgang(f"$d%tY")
      case None => AthletJahrgang("unbekannt")
    }
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: AthletJahrgang, p2: AthletJahrgang) =>
        p1.jahrgang.compareTo(p2.jahrgang) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

case class ByAltersklasse(bezeichnung: String = "GebDat Altersklasse", grenzen: Seq[(String, Seq[String], Int)]) extends GroupBy with FilterBy {
  override val groupname: String = bezeichnung
  val klassen: Seq[Altersklasse] = Altersklasse(grenzen)

  def makeGroupBy(w: Wettkampf)(gebdat: LocalDate, geschlecht: String, programm: ProgrammView): Altersklasse = {
    val wkd: LocalDate = w.datum
    val alter = Period.between(gebdat, wkd.plusDays(1)).getYears
    Altersklasse(klassen, alter, geschlecht, programm)
  }

  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    val wkd: LocalDate = v.wettkampf.datum
    val gebd: LocalDate = sqlDate2ld(v.athlet.gebdat.getOrElse(Date.valueOf(LocalDate.now())))
    makeGroupBy(v.wettkampf)(gebd, v.athlet.geschlecht, v.wettkampfdisziplin.programm)
  }

  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.compareTo(gs2.groupKey) < 0
  })
}

case class ByJahrgangsAltersklasse(bezeichnung: String = "JG Altersklasse", grenzen: Seq[(String, Seq[String], Int)]) extends GroupBy with FilterBy {
  override val groupname: String = bezeichnung
  val klassen: Seq[Altersklasse] = Altersklasse(grenzen)

  def makeGroupBy(w: Wettkampf)(gebdat: LocalDate, geschlecht: String, programm: ProgrammView): Altersklasse = {
    val wkd: LocalDate = w.datum
    val alter = wkd.getYear - gebdat.getYear
    Altersklasse(klassen, alter, geschlecht, programm)
  }

  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    val gebd: LocalDate = sqlDate2ld(v.athlet.gebdat.getOrElse(Date.valueOf(LocalDate.now())))
    makeGroupBy(v.wettkampf)(gebd, v.athlet.geschlecht, v.wettkampfdisziplin.programm)
  }

  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.compareTo(gs2.groupKey) < 0
  })
}

case class ByTeamRule(bezeichnung: String = "Teamregel", regel: TeamRegel) extends GroupBy with FilterBy {
  override val groupname: String = bezeichnung
  setKind(Teamrangliste)

  protected override val grouper: WertungView => DataObject = (w:WertungView) => CompoundGrouper(Seq(GenericGrouper(regel.pgmGrouperText(w)), TurnerGeschlecht(regel.sexGrouperText(w))))

  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.compareTo(gs2.groupKey) < 0
  })
}

case class ByDisziplin() extends GroupBy with FilterBy {
  override val groupname = "Disziplin"
  private val ordering = mutable.HashMap[Long, Long]()

  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    ordering.put(v.wettkampfdisziplin.disziplin.id, v.wettkampfdisziplin.ord.toLong)
    v.wettkampfdisziplin.disziplin
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: Disziplin, p2: Disziplin) =>
        val go1 = ordering.getOrElse(p1.id, p1.id)
        val go2 = ordering.getOrElse(p2.id, p2.id)
        go1.compareTo(go2) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

case class ByGeschlecht() extends GroupBy with FilterBy {
  override val groupname = "Geschlecht"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    TurnerGeschlecht(v.athlet.geschlecht)
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    gs1.groupKey.easyprint.compareTo(gs2.groupKey.easyprint) > 0
  })
}

case class ByVerein() extends GroupBy with FilterBy {
  override val groupname = "Verein"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    v.athlet.verein match {
      case Some(verein) => verein
      case _ => Verein(0, "kein", None)
    }
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: Verein, p2: Verein) =>
        p1.name.compareTo(p2.name) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

case class ByVerband() extends GroupBy with FilterBy {
  override val groupname = "Verband"
  protected override val grouper: WertungView => DataObject = (v: WertungView) => {
    v.athlet.verein match {
      case Some(verein) => Verband(verein.verband.getOrElse("kein"))
      case _ => Verband("kein")
    }
  }
  protected override val sorter: Option[(GroupSection, GroupSection) => Boolean] = Some((gs1: GroupSection, gs2: GroupSection) => {
    (gs1.groupKey, gs2.groupKey) match {
      case (p1: Verband, p2: Verband) =>
        p1.name.compareTo(p2.name) < 0
      case _ => gs1.groupKey.compareTo(gs2.groupKey) < 0
    }
  })
}

object GroupBy {
  private val allGroupers = List(
    ByWettkampfProgramm(), ByProgramm(),
    ByWettkampfProgramm("Kategorie"), ByProgramm("Kategorie"),
    ByWettkampfProgramm("Programm"), ByProgramm("Programm"), ByWettkampf(),
    ByJahrgang(), ByGeschlecht(), ByVerband(), ByVerein(), ByAthlet(),
    ByRiege(), ByRiege2(), ByDisziplin(), ByJahr()
  )

  def apply(query: String, data: Seq[WertungView], groupers: List[FilterBy] = allGroupers): GroupBy = {
    val arguments = query.split("&")
    val groupby = arguments.filter(x => x.length > 8 && x.startsWith("groupby=")).map(x => URLDecoder.decode(x.split("=")(1), "UTF-8")).headOption
    val filter = arguments.filter(x => x.length > 7 && x.startsWith("filter=")).map(x => URLDecoder.decode(x.split("=")(1), "UTF-8"))
    val kind: ScoreListKind = ScoreListKind(
      arguments
        .filter(x => x.startsWith("kind"))
        .map(x => x.split("=")(1))
        .headOption
    )
    val bestOfN: ScoreListBestN = ScoreListBestN(
      arguments
        .filter(x => x.startsWith("counting"))
        .map(x => x.split("=")(1))
        .headOption
    )
    apply(groupby, filter, data, query.contains("&alphanumeric"), !query.contains("&avg=false"), kind, bestOfN, groupers)
  }

  def apply(groupby: Option[String], filter: Iterable[String], data: Seq[WertungView], alphanumeric: Boolean, isAvgOnMultipleCompetitions: Boolean, kind: ScoreListKind, counting: ScoreListBestN, groupers: List[FilterBy]): GroupBy = {
    val filterList = filter.map { flt =>
      val keyvalues = flt.split(":")
      val key = keyvalues(0)
      val values = keyvalues(1).split("!")
      key -> values.toSet
    }.toMap

    val cblist = groupby.toSeq.flatMap(gb => gb.split(":")).map { groupername =>
      groupers.find(grouper => grouper.groupname.equals(groupername))
    }.filter { case Some(_) => true case None => false }.map(_.get)
    val cbllist = if cblist.nonEmpty then cblist else if kind == Teamrangliste then groupers.take(1) else Seq(ByWettkampfProgramm(), ByGeschlecht())

    val cbflist = filterList.keys.map { groupername =>
      groupers.find(grouper => grouper.groupname.equals(groupername))
    }.filter {
      case Some(_) => true
      case None => false
    }.map(_.get).filter(grouper => !cbllist.contains(grouper)) ++ cbllist

    cbflist.foreach { gr =>
      gr.reset
      filterList.get(gr.groupname) match {
        case Some(filterValues) =>
          gr.setFilter(gr.analyze(data).filter { f =>
            filterValues.exists(entry => {
              val itemText = f.easyprint
              val exists = if entry.contains(" ") then {
                entry.split(" ").map(e => e.replace("_", "")).forall(subentry => itemText.contains(subentry))
              } else {
                entry.equalsIgnoreCase(itemText)
              }
              exists
            })
          }.toSet ++ (
            if filterValues.contains("all") || filterValues.contains("alle") then Set(NullObject("alle"))
            else Set.empty)
          )
        case _ =>
      }
    }
    val query = if cbflist.nonEmpty then {
      cbflist.foldLeft(cbflist.head.asInstanceOf[GroupBy])((acc, cb) => if acc != cb then acc.groupBy(cb) else acc)
    } else if data.nonEmpty && data.head.wettkampf.altersklassen.get.nonEmpty then {
      val byAK = groupers.find(p => p.isInstanceOf[ByAltersklasse] && p.groupname.startsWith("Wettkampf")).getOrElse(ByAltersklasse("AK", Altersklasse.parseGrenzen(data.head.wettkampf.altersklassen.get)))
      ByProgramm().groupBy(byAK).groupBy(ByGeschlecht())
    } else if data.nonEmpty && data.head.wettkampf.jahrgangsklassen.get.nonEmpty then {
      val byAK = groupers.find(p => p.isInstanceOf[ByJahrgangsAltersklasse] && p.groupname.startsWith("Wettkampf")).getOrElse(ByJahrgangsAltersklasse("AK", Altersklasse.parseGrenzen(data.head.wettkampf.jahrgangsklassen.get)))
      ByProgramm().groupBy(byAK).groupBy(ByGeschlecht())
    } else if data.nonEmpty && data.head.wettkampf.hasTeams then {
      val regel = TeamRegel(data.head.wettkampf)
      val byTeamRegel = groupers.find(p => p.isInstanceOf[ByTeamRule] && p.groupname.startsWith("Wettkampf")).getOrElse(ByTeamRule(regel.toRuleName, regel))
      byTeamRegel
    }
    else {
      ByProgramm().groupBy(ByGeschlecht())
    }
    query.setAlphanumericOrdered(alphanumeric)
    query.setAvgOnMultipleCompetitions(isAvgOnMultipleCompetitions)
    query.setKind(kind)
    query.setBestNCounting(counting)
    query
  }
}