package ch.seidel.kutu.domain

object TeamRegel {
  val defaultRegel = TeamRegelList(List(), Some("Keine Teams"))

  val predefined = Map(
      ("Keine Teams" -> "")
    , ("Aus Verein, drei Bestnoten pro Gerät, mit unbeschränkter Anzahl Mitglieder" -> "VereinGerät(3/*)")
    , ("Aus Verein, drei Bestnoten pro Gerät, mit max vier Mitglieder" -> "VereinGerät(3/4)")
    , ("Aus Verein, drei Gesamt-Bestnoten, mit unbeschränkter Anzahl Mitglieder" -> "VereinGesamt(3/*)")
    , ("Aus Verein, drei Gesamt-Bestnoten, mit max vier Mitglieder" -> "VereinGesamt(3/4)")
    , ("Aus Verband, drei Bestnoten pro Gerät, mit unbeschränkter Anzahl Mitglieder" -> "VerbandGerät(3/*)")
    , ("Aus Verband, drei Bestnoten pro Gerät, mit max vier Mitglieder" -> "VerbandGerät(3/4)")
    , ("Aus Verband, drei Gesamt-Bestnoten, mit unbeschränkter Anzahl Mitglieder" -> "VerbandGesamt(3/*)")
    , ("Aus Verband, drei Gesamt-Bestnoten, mit max vier Mitglieder" -> "VerbandGesamt(3/4)")
    , ("Individuell" -> "VereinGesamt(<min>/<max>/<extrateam1>+<extrateam2>), VerbandGesamt(<min>/<max>), VereinGerät(<min>/<max>), VerbandGerät(<min>/<max>)")
  )
  private val rangePattern = "([\\S]+)\\(([0-9]+)\\/([0-9,\\*]*)(\\/[\\S\\s\\/0-9+]*)?\\)".r

  def apply(formel: String): TeamRegel = {
    def defaultMax(max: String): Int = if (max.equals("*")) 0 else max
    val regeln = formel.split(",").map(_.trim).filter(_.nonEmpty).toList
    val mappedRules: List[TeamRegel] = regeln.flatMap {
      case rangePattern(rulename, min, max, extrateams) =>
        val extraTeamsDef = if (extrateams == null) "" else extrateams
        rulename match {
        case "VereinGesamt" => Some(TeamRegelVereinGesamt(min, defaultMax(max), extraTeamsDef))
        case "VerbandGesamt" => Some(TeamRegelVerbandGesamt(min, defaultMax(max), extraTeamsDef))
        case "VereinGerät" => Some(TeamRegelVereinGeraet(min, defaultMax(max), extraTeamsDef))
        case "VerbandGerät" => Some(TeamRegelVerbandGeraet(min, defaultMax(max), extraTeamsDef))
        case _ => None
      }
      case "Keine Teams" =>None
      case s: String => None
    }
    if (mappedRules.isEmpty) defaultRegel else {
      TeamRegelList(mappedRules)
    }
  }

  def apply(wettkampf: Wettkampf): TeamRegel = wettkampf.teamrule match {
    case Some(regel) => this(regel)
    case _ => defaultRegel
  }
}

sealed trait TeamRegel {
  def getExtrateams: List[String]
  def parseExtrateams(extraTeamsDef: String) = extraTeamsDef.replace("/", "").split("\\+").map(_.trim).toList

  def extractTeams(wertungen: Iterable[WertungView]): List[Team]
  def teamsAllowed: Boolean
  def toFormel: String
  def toRuleName: String
}
case class TeamRegelList(regeln: List[TeamRegel], name: Option[String] = None) extends TeamRegel {
  override def getExtrateams: List[String] = regeln.flatMap(_.getExtrateams)
  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    regeln.flatMap(_.extractTeams(wertungen))
  }

  override def teamsAllowed: Boolean = regeln.nonEmpty && regeln.exists(_.teamsAllowed)
  override def toFormel: String = name.getOrElse(regeln.map(_.toFormel).mkString(","))
  override def toRuleName: String = name.getOrElse(regeln.map(_.toRuleName).sorted.mkString(", "))
}

case class TeamRegelVereinGeraet(min: Int, max: Int, extraTeamsDef: String) extends TeamRegel {
  private val extrateams = parseExtrateams(extraTeamsDef)
  override def getExtrateams: List[String] = extrateams
  override def toFormel: String = s"VereinGerät($min/${if (max > 0) max else "*"}$extraTeamsDef)"
  override def toRuleName: String = s"""Vereins-Team Rangliste (beste $min Gerätewertungen${if (max > 0) s" aus $max" else ""})"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    wertungen
      .filter(w => w.team != 0)
      .toList
      .groupBy(w => if (w.team > 0) (w.athlet.verein.map(_.extendedprint).getOrElse(""), w.team) else (extrateams(w.team * -1 -1), w.team))
      .flatMap { team =>
        val (teamkey, teamwertungen) = team
        val (verein, teamNummer) = teamkey
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if (athletCount >= min && (max == 0 || athletCount <= max)) {
          val perDisciplinWertungen: Map[Disziplin, List[WertungView]] = teamwertungen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
            .flatMap { case (disciplin, wtgs) =>
              val relevantDisciplineValues = wtgs
                .filter(_.resultat.endnote > 0)
                .groupBy(_.resultat.endnote).toList
                .sortBy(_._1).reverse.take(min)
                .flatMap(_._2)
              if (relevantDisciplineValues.isEmpty) None else Some((disciplin, relevantDisciplineValues))
            }
          val perDisciplinCountingWertungen: Map[Disziplin, List[WertungView]] = perDisciplinWertungen
            .flatMap { case (disciplin, wtgs) =>
              val relevantDisciplineValues = wtgs
                .filter(_.resultat.endnote > 0)
                .sortBy(_.resultat.endnote).reverse.take(min)
              if (relevantDisciplineValues.isEmpty) None else Some((disciplin, relevantDisciplineValues))
            }
          val limitedTeamwertungen = if (max > 0) teamwertungen else {
            val allRelevantWertungen = perDisciplinWertungen.values.flatten.map(_.athlet).toSet
            teamwertungen.filter { w =>
              allRelevantWertungen.contains(w.athlet)
            }
          }
          val teamname = if (teamNummer > 0) s"${verein} ${teamNummer}" else verein
          List(Team(s"${teamname}", toRuleName, limitedTeamwertungen, perDisciplinCountingWertungen, perDisciplinWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
case class TeamRegelVereinGesamt(min: Int, max: Int, extraTeamsDef: String) extends TeamRegel {
  val extrateams = parseExtrateams(extraTeamsDef)
  override def getExtrateams: List[String] = extrateams

  override def toFormel: String = s"VereinGesamt($min/${if (max > 0) max else "*"}$extraTeamsDef)"

  override def toRuleName: String = s"""Vereins-Team Rangliste (beste $min Gesamtwertungen${if (max > 0) s" aus $max" else ""})"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    wertungen
      .filter(w => w.team != 0)
      .toList
      .groupBy(w => if (w.team > 0) (w.athlet.verein.map(_.extendedprint).getOrElse(""), w.team) else (extrateams(w.team * -1 -1), w.team))
      .flatMap { team =>
        val (teamkey, teamwertungen) = team
        val (verein, teamNummer) = teamkey
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if (athletCount >= min && (max == 0 || athletCount <= max)) {
          val perAthletWertungen = teamwertungen
            .groupBy(w => w.athlet)
            .map{ case (athlet, wtgs) =>
              val wtgsum = wtgs.filter(_.showInScoreList).map(_.resultat).reduce(_+_)
              (athlet, wtgs, wtgsum)
            }
            .toList
            .sortBy(_._3).reverse.take(min) // sortiert auf Gesamtresultat
            .flatMap(_._2) // mit wertungen die Disziplin-Map aufbauen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
          val limitedTeamwertungen = if (max > 0) teamwertungen else {
            val allRelevantWertungen = perAthletWertungen.values.flatten.toSet
            teamwertungen.filter {
              allRelevantWertungen.contains
            }
          }
          val teamname = if (teamNummer > 0) s"${verein} ${teamNummer}" else verein
          List(Team(s"${teamname}", toRuleName, limitedTeamwertungen, perAthletWertungen, perAthletWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}

case class TeamRegelVerbandGeraet(min: Int, max: Int, extraTeamsDef: String) extends TeamRegel {
  val extrateams = parseExtrateams(extraTeamsDef)
  override def getExtrateams: List[String] = extrateams

  override def toFormel: String = s"VerbandGerät($min/${if (max > 0) max else "*"}$extraTeamsDef)"

  override def toRuleName: String = s"""Verbands-Team Rangliste (beste $min Gerätewertungen${if (max > 0) s" aus $max" else ""})"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    wertungen
      .filter(w => w.team != 0)
      .toList
      .groupBy(w => if (w.team > 0) (w.athlet.verein.flatMap(_.verband).getOrElse(""), w.team) else (extrateams(w.team * -1 -1), w.team))
      .flatMap { team =>
        val (teamkey, teamwertungen) = team
        val (verband, teamNummer) = teamkey
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if (athletCount >= min && (max == 0 || athletCount <= max)) {
          val perDisciplinWertungen: Map[Disziplin, List[WertungView]] = teamwertungen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
            .flatMap { case (disciplin, wtgs) =>
              val relevantDisciplineValues = wtgs
                .filter(_.resultat.endnote > 0)
                .groupBy(_.resultat.endnote).toList
                .sortBy(_._1).reverse.take(min)
                .flatMap(_._2)
              if (relevantDisciplineValues.isEmpty) None else Some((disciplin, relevantDisciplineValues))
            }
          val perDisciplinCountingWertungen: Map[Disziplin, List[WertungView]] = perDisciplinWertungen
            .flatMap { case (disciplin, wtgs) =>
              val relevantDisciplineValues = wtgs
                .filter(_.resultat.endnote > 0)
                .sortBy(_.resultat.endnote).reverse.take(min)
              if (relevantDisciplineValues.isEmpty) None else Some((disciplin, relevantDisciplineValues))
            }
          val limitedTeamwertungen = if (max > 0) teamwertungen else {
            val allRelevantWertungen = perDisciplinWertungen.values.flatten
              .map(_.athlet).toSet
            teamwertungen.filter { w =>
              allRelevantWertungen.contains(w.athlet)
            }
          }
          val teamname = if (teamNummer > 0) s"${verband} ${teamNummer}" else verband
          List(Team(s"${teamname}", toRuleName, limitedTeamwertungen, perDisciplinCountingWertungen, perDisciplinWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
case class TeamRegelVerbandGesamt(min: Int, max: Int, extraTeamsDef: String) extends TeamRegel {
  val extrateams = parseExtrateams(extraTeamsDef)
  override def getExtrateams: List[String] = extrateams

  override def toFormel: String = s"VerbandGesamt($min/${if (max > 0) max else "*"}$extraTeamsDef)"
  override def toRuleName: String = s"""Verbands-Team Rangliste (beste $min Gesamtwertungen${if (max > 0) s" aus $max" else ""})"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    wertungen
      .filter(w => w.team != 0)
      .toList
      .groupBy(w => if (w.team > 0) (w.athlet.verein.flatMap(_.verband).getOrElse(""), w.team) else (extrateams(w.team * -1 -1), w.team))
      .flatMap { team =>
        val (teamkey, teamwertungen) = team
        val (verband, teamNummer) = teamkey
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if (athletCount >= min && (max == 0 || athletCount <= max)) {
          val perAthletWertungen = teamwertungen
            .groupBy(w => w.athlet)
            .map{ case (athlet, wtgs) =>
              val wtgsum = wtgs.filter(_.showInScoreList).map(_.resultat).reduce(_+_)
              (athlet, wtgs, wtgsum)
            }
            .toList
            .sortBy(_._3).reverse.take(min) // sortiert auf Gesamtresultat
            .flatMap(_._2) // mit wertungen die Disziplin-Map aufbauen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
          val limitedTeamwertungen = if (max > 0) teamwertungen else {
            val allRelevantWertungen = perAthletWertungen.values.flatten.toSet
            teamwertungen.filter {
              allRelevantWertungen.contains
            }
          }
          val teamname = if (teamNummer > 0) s"${verband} ${teamNummer}" else verband
          List(Team(s"${teamname}", toRuleName, limitedTeamwertungen, perAthletWertungen, perAthletWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
