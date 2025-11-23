package ch.seidel.kutu.domain

object TeamRegel {
  val defaultRegel = TeamRegelList(List(), Some("Keine Teams"))

  val vereinGesamt = "VereinGesamt"
  val vereinGeraet = "VereinGerät"
  val verbandGesamt = "VerbandGesamt"
  val verbandGeraet = "VerbandGerät"
  val vereinRegeln = Set(vereinGesamt, vereinGeraet)
  val verbandRegeln = Set(verbandGesamt, verbandGeraet)

  val predefined = Map(
      ("Keine Teams" -> "")
    , ("Aus Verein, drei Bestnoten pro Gerät, mit unbeschränkter Anzahl Mitglieder" -> s"$vereinGeraet(3/*)")
    , ("Aus Verein, drei Bestnoten pro Gerät, mit max vier Mitglieder" -> s"$vereinGeraet(3/4)")
    , ("Aus Verein, drei Bestnoten pro Gerät, mit max vier Mitglieder, zusammengefasste Kategorien K6, K7, KH, KD" -> s"$vereinGeraet[K6+K7+KD+KH](3/4)")
    , ("Aus Verein, drei Gesamt-Bestnoten, mit unbeschränkter Anzahl Mitglieder" -> s"$vereinGesamt(3/*)")
    , ("Aus Verein, drei Gesamt-Bestnoten, mit max vier Mitglieder" -> s"$vereinGesamt(3/4)")
    , ("Aus Verein, durchschnitt pro Gerät, mit unbeschränkter Anzahl Mitglieder (M/W)" -> s"$vereinGeraet[M+W](avg/*/*)")
    , ("Aus Verein, median pro Gerät, mit unbeschränkter Anzahl Mitglieder (M/W)" -> s"$vereinGeraet[M+W](median/*/*)")
    , ("Aus Verein, kleinste Abweichung pro Gerät, mit unbeschränkter Anzahl Mitglieder (M/W)" -> s"$vereinGeraet[M+W](devmin/*/*)")
    , ("Aus Verein, grösste Abweichung pro Gerät, mit unbeschränkter Anzahl Mitglieder (M/W)" -> s"$vereinGeraet[M+W](devmax/*/*)")
    , ("Aus Verband, drei Bestnoten pro Gerät, mit unbeschränkter Anzahl Mitglieder" -> s"$verbandGeraet(3/*)")
    , ("Aus Verband, drei Bestnoten pro Gerät, mit max vier Mitglieder" -> s"$verbandGeraet(3/4)")
    , ("Aus Verband, drei Gesamt-Bestnoten, mit unbeschränkter Anzahl Mitglieder" -> s"$verbandGesamt(3/*)")
    , ("Aus Verband, drei Gesamt-Bestnoten, mit max vier Mitglieder" -> s"$verbandGesamt(3/4)")
    , ("Individuell" -> s"$vereinGesamt(<min>/<max>/<extrateam1>+<extrateam2>), $verbandGesamt(<min>/<max>), VereinGerät(<min>/<max>), $vereinGeraet(<min>/<max>)")
  )
  private val rangePattern = "([a-zA-ZäöüÄÖÜ]+)([\\[\\S\\s0-9+\\]]+)?\\(([devmax\\/|devmin\\/|median\\/|min\\/|max\\/|avg\\/|sum\\/]+)?([0-9,\\*]+)\\/([0-9,\\*]*)(\\/[\\S\\s\\/0-9+]*)?\\)".r

  def apply(formel: String): TeamRegel = {
    def default(cnt: String): Int = if cnt.equals("*") then 0 else try cnt.intValue catch {case _: Throwable => 0}
    val regeln = formel.split(",").map(_.trim).filter(_.nonEmpty).toList
    val mappedRules: List[TeamRegel] = regeln.flatMap {
      case rangePattern(rulename, grouper, aggFn, min, max, extrateams) =>
        val aggFun = TeamAggreateFun(aggFn)
        val extraTeamsDef = if extrateams == null then "" else extrateams
        val grouperDef = if grouper == null then "" else grouper
        rulename match {
        case `vereinGesamt` => Some(TeamRegelVereinGesamt(default(min), default(max), extraTeamsDef, grouperDef, aggFun))
        case `verbandGesamt` => Some(TeamRegelVerbandGesamt(default(min), default(max), extraTeamsDef, grouperDef, aggFun))
        case `vereinGeraet` => Some(TeamRegelVereinGeraet(default(min), default(max), extraTeamsDef, grouperDef, aggFun))
        case `verbandGeraet` => Some(TeamRegelVerbandGeraet(default(min), default(max), extraTeamsDef, grouperDef, aggFun))
        case _ => None
      }
      case "Keine Teams" =>None
      case s: String => None
    }
    if mappedRules.isEmpty then defaultRegel else {
      TeamRegelList(mappedRules)
    }
  }

  def apply(wettkampf: Wettkampf): TeamRegel = wettkampf.teamrule match {
    case Some(regel) => this(regel)
    case _ => defaultRegel
  }
}

sealed trait TeamRegel {
  def getTeamRegeln: Seq[TeamRegel] = Seq(this)
  def getExtrateams: List[String]
  def parseExtrateams(extraTeamsDef: String): List[String] = extraTeamsDef.replace("/", "").split("\\+").map(_.trim).toList
  def getGrouperDefs: List[Set[String]] = List.empty
  def parseGrouperDefs(grouperDef: String): List[Set[String]] = grouperDef
    .replace("[", "")
    .replace("]", "")
    .split("/")
    .map { term =>
       term.split("\\+").toSet
    }
    .toList
  private def getMatchingGrouper(name: String): Set[String] = {
    val group = getGrouperDefs.find(_.contains(name)).getOrElse(Set(name))
    group
  }

  def extractTeams(wertungen: Iterable[WertungView]): List[Team]
  def extractExtraTeams(wertungen: Iterable[WertungView]): List[String] = wertungen.map(_.wettkampf).toList.distinct.flatMap(_.extraTeams)

  /**
   * Extracts all teams with default grouped wertungen.
   * Default grouped means per Rule extracted program and sex.
   * Per rule means, that each rule can have its own definition about whether a split or a group of a criterion is required.
   * @param wertungen list of wertungen, containing athlets and its assignment to a program, team, etc., where each
   *                  rule can extract its criterion to apply a splitting- or grouping- rule.
   * @return List[pgm,sex,List[teams]]
   */
  def extractTeamsWithDefaultGouping(wertungen: Iterable[WertungView]): List[(String,String,List[Team])] = {
    val hasNoExplicitTeams = !wertungen.exists(w => w.team != 0)
    wertungen
      .filter(w => hasNoExplicitTeams || w.team != 0)
      .toList
      .groupBy(w => (pgmGrouperText(w), sexGrouperText(w)))
      .map( gr => (gr._1._1, gr._1._2, extractTeams(gr._2)))
      .toList
  }

  def teamsAllowed: Boolean
  def toFormel: String
  def toRuleName: String

  def pgmGrouperText(w: WertungView): String = getMatchingGrouper(w.wettkampfdisziplin.programm.name).mkString(",")
  def sexGrouperText(w: WertungView): String = getMatchingGrouper(w.athlet.geschlecht).mkString(",")
}

case class TeamRegelList(regeln: List[TeamRegel], name: Option[String] = None) extends TeamRegel {
  override def getTeamRegeln: Seq[TeamRegel] = regeln.flatMap(_.getTeamRegeln)
  override def getExtrateams: List[String] = regeln.flatMap(_.getExtrateams)
  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    regeln.flatMap(_.extractTeams(wertungen))
  }
  override def extractTeamsWithDefaultGouping(wertungen: Iterable[WertungView]): List[(String,String,List[Team])] = {
    regeln.flatMap(_.extractTeamsWithDefaultGouping(wertungen))
  }
  override def getGrouperDefs: List[Set[String]] = regeln.flatMap(_.getGrouperDefs)
  override def teamsAllowed: Boolean = regeln.nonEmpty && regeln.exists(_.teamsAllowed)
  override def toFormel: String = name.getOrElse(regeln.map(_.toFormel).mkString(","))
  override def toRuleName: String = name.getOrElse(regeln.map(_.toRuleName).sorted.mkString(", "))
}

case class TeamRegelVereinGeraet(min: Int, max: Int, extraTeamsDef: String, grouperDef: String, aggregateFun: TeamAggreateFun) extends TeamRegel {
  private val extrateams = parseExtrateams(extraTeamsDef)
  override def getExtrateams: List[String] = extrateams

  private val grouperDefs = parseGrouperDefs(grouperDef)
  override def getGrouperDefs: List[Set[String]] = grouperDefs

  override def toFormel: String = s"VereinGerät$grouperDef(${aggregateFun.toFormelPart}${if min > 0 then min else "*"}/${if max > 0 then max else "*"}$extraTeamsDef)"
  override def toRuleName: String = s"""Vereins-Team Rangliste $grouperDef (${aggregateFun.toDescriptionPart} ${if min == 0 then "allen" else s"besten $min"}${if max > 0 then s" von max $max" else ""} Gerätewertungen)"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    val extraTeams = extractExtraTeams(wertungen)
    val hasNoExplicitTeams = !wertungen.exists(w => w.team != 0)
    wertungen
      .filter(w => hasNoExplicitTeams || w.team != 0)
      .toList
      .groupBy(w => w.getTeamName(extraTeams))
      .flatMap { team =>
        val (teamname, teamwertungen) = team
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if ((min == 0 && athletCount > 0) || athletCount >= min) && (max == 0 || athletCount <= max) then {
          val takeCnt = if min == 0 then athletCount else min
          val perDisciplinWertungen: Map[Disziplin, List[WertungView]] = teamwertungen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
            .flatMap { case (disciplin, wtgs) =>
              val relevantDisciplineValues = wtgs
                //.filter(_.resultat.endnote > 0)
                .groupBy(_.resultat.endnote).toList
                .sortBy(_._1).reverse.take(takeCnt)
                .flatMap(_._2)
              if relevantDisciplineValues.isEmpty then None else Some((disciplin, relevantDisciplineValues))
            }
          val perDisciplinCountingWertungen: Map[Disziplin, List[WertungView]] = perDisciplinWertungen
            .flatMap { case (disciplin, wtgs) =>
              val relevantDisciplineValues = wtgs
                //.filter(_.resultat.endnote > 0)
                .sortBy(_.resultat.endnote).reverse.take(takeCnt)
              if relevantDisciplineValues.isEmpty then None else Some((disciplin, relevantDisciplineValues))
            }
          val limitedTeamwertungen = if max > 0 then teamwertungen else {
            val allRelevantWertungen = perDisciplinWertungen.values.flatten.map(_.athlet).toSet
            teamwertungen.filter { w =>
              allRelevantWertungen.contains(w.athlet)
            }
          }
          List(Team(s"${teamname}", toRuleName, limitedTeamwertungen, perDisciplinCountingWertungen, perDisciplinWertungen, aggregateFun))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
case class TeamRegelVereinGesamt(min: Int, max: Int, extraTeamsDef: String, grouperDef: String, aggregateFun: TeamAggreateFun) extends TeamRegel {
  private val extrateams = parseExtrateams(extraTeamsDef)
  override def getExtrateams: List[String] = extrateams

  private val grouperDefs = parseGrouperDefs(grouperDef)
  override def getGrouperDefs: List[Set[String]] = grouperDefs

  override def toFormel: String = s"VereinGesamt$grouperDef(${aggregateFun.toFormelPart}${if min > 0 then min else "*"}/${if max > 0 then max else "*"}$extraTeamsDef)"

  override def toRuleName: String = s"""Vereins-Team Rangliste $grouperDef (${aggregateFun.toDescriptionPart} ${if min == 0 then "allen" else s"besten $min"}${if max > 0 then s" von max $max" else ""} Gesamtwertungen)"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    val extraTeams = extractExtraTeams(wertungen)
    val hasNoExplicitTeams = !wertungen.exists(w => w.team != 0)
    wertungen
      .filter(w => hasNoExplicitTeams || w.team != 0)
      .toList
      .groupBy(w => w.getTeamName(extraTeams))
      .flatMap { team =>
        val (teamname, teamwertungen) = team
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if ((min == 0 && athletCount > 0) || athletCount >= min) && (max == 0 || athletCount <= max) then {
          val takeCnt = if min == 0 then athletCount else min
          val perAthletWertungen = teamwertungen
            .groupBy(w => w.athlet)
            .map{ case (athlet, wtgs) =>
              val wtgsum = wtgs.filter(_.showInScoreList).map(_.resultat).reduce(_+_)
              (athlet, wtgs, wtgsum)
            }
            .toList
            .sortBy(_._3).reverse.take(takeCnt) // sortiert auf Gesamtresultat
            .flatMap(_._2) // mit wertungen die Disziplin-Map aufbauen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
          val limitedTeamwertungen = if max > 0 then teamwertungen else {
            val allRelevantWertungen = perAthletWertungen.values.flatten.toSet
            teamwertungen.filter {
              allRelevantWertungen.contains
            }
          }
          List(Team(s"${teamname}", toRuleName, limitedTeamwertungen, perAthletWertungen, perAthletWertungen, aggregateFun))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}

case class TeamRegelVerbandGeraet(min: Int, max: Int, extraTeamsDef: String, grouperDef: String, aggregateFun: TeamAggreateFun) extends TeamRegel {
  private val extrateams = parseExtrateams(extraTeamsDef)
  override def getExtrateams: List[String] = extrateams

  private val grouperDefs = parseGrouperDefs(grouperDef)
  override def getGrouperDefs: List[Set[String]] = grouperDefs

  override def toFormel: String = s"VerbandGerät$grouperDef(${aggregateFun.toFormelPart}${if min > 0 then min else "*"}/${if max > 0 then max else "*"}$extraTeamsDef)"

  override def toRuleName: String = s"""Verbands-Team Rangliste $grouperDef (${aggregateFun.toDescriptionPart} ${if min == 0 then "allen" else s"besten $min"}${if max > 0 then s" von max $max" else ""}  Gerätewertungen)"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    val extraTeams = extractExtraTeams(wertungen)
    val hasNoExplicitTeams = !wertungen.exists(w => w.team != 0)
    wertungen
      .filter(w => hasNoExplicitTeams || w.team != 0)
      .toList
      .groupBy(w => w.getTeamName(extraTeams))
      .flatMap { team =>
        val (teamname, teamwertungen) = team
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if ((min == 0 && athletCount > 0) || athletCount >= min) && (max == 0 || athletCount <= max) then {
          val takeCnt = if min == 0 then athletCount else min
          val perDisciplinWertungen: Map[Disziplin, List[WertungView]] = teamwertungen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
            .flatMap { case (disciplin, wtgs) =>
              val relevantDisciplineValues = wtgs
                //.filter(_.resultat.endnote > 0)
                .groupBy(_.resultat.endnote).toList
                .sortBy(_._1).reverse.take(takeCnt)
                .flatMap(_._2)
              if relevantDisciplineValues.isEmpty then None else Some((disciplin, relevantDisciplineValues))
            }
          val perDisciplinCountingWertungen: Map[Disziplin, List[WertungView]] = perDisciplinWertungen
            .flatMap { case (disciplin, wtgs) =>
              val relevantDisciplineValues = wtgs
                //.filter(_.resultat.endnote > 0)
                .sortBy(_.resultat.endnote).reverse.take(takeCnt)
              if relevantDisciplineValues.isEmpty then None else Some((disciplin, relevantDisciplineValues))
            }
          val limitedTeamwertungen = if max > 0 then teamwertungen else {
            val allRelevantWertungen = perDisciplinWertungen.values.flatten
              .map(_.athlet).toSet
            teamwertungen.filter { w =>
              allRelevantWertungen.contains(w.athlet)
            }
          }
          List(Team(s"${teamname}", toRuleName, limitedTeamwertungen, perDisciplinCountingWertungen, perDisciplinWertungen, aggregateFun))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
case class TeamRegelVerbandGesamt(min: Int, max: Int, extraTeamsDef: String, grouperDef: String, aggregateFun: TeamAggreateFun) extends TeamRegel {
  private val extrateams = parseExtrateams(extraTeamsDef)
  override def getExtrateams: List[String] = extrateams

  private val grouperDefs = parseGrouperDefs(grouperDef)
  override def getGrouperDefs: List[Set[String]] = grouperDefs

  override def toFormel: String = s"VerbandGesamt$grouperDef(${aggregateFun.toFormelPart}${if min > 0 then min else "*"}/${if max > 0 then max else "*"}$extraTeamsDef)"
  override def toRuleName: String = s"""Verbands-Team Rangliste $grouperDef (${aggregateFun.toDescriptionPart} ${if min == 0 then "allen" else s"besten $min"}${if max > 0 then s" von max $max" else ""}  Gesamtwertungen)"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    val extraTeams = extractExtraTeams(wertungen)
    val hasNoExplicitTeams = !wertungen.exists(w => w.team != 0)
    wertungen
      .filter(w => hasNoExplicitTeams || w.team != 0)
      .toList
      .groupBy(w => w.getTeamName(extraTeams))
      .flatMap { team =>
        val (teamname, teamwertungen) = team
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if ((min == 0 && athletCount > 0) || athletCount >= min) && (max == 0 || athletCount <= max) then {
          val takeCnt = if min == 0 then athletCount else min
          val perAthletWertungen = teamwertungen
            .groupBy(w => w.athlet)
            .map{ case (athlet, wtgs) =>
              val wtgsum = wtgs.filter(_.showInScoreList).map(_.resultat).reduce(_+_)
              (athlet, wtgs, wtgsum)
            }
            .toList
            .sortBy(_._3).reverse.take(takeCnt) // sortiert auf Gesamtresultat
            .flatMap(_._2) // mit wertungen die Disziplin-Map aufbauen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
          val limitedTeamwertungen = if max > 0 then teamwertungen else {
            val allRelevantWertungen = perAthletWertungen.values.flatten.toSet
            teamwertungen.filter {
              allRelevantWertungen.contains
            }
          }
          List(Team(s"${teamname}", toRuleName, limitedTeamwertungen, perAthletWertungen, perAthletWertungen, aggregateFun))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
