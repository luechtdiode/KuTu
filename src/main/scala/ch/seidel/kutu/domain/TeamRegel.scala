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
    , ("Individuell" -> "VereinGesamt(<min>/<max>), VerbandGesamt(<min>/<max>, VereinGerät(<min>/<max>), VerbandGerät(<min>/<max>)")
  )
  private val rangePattern = "([\\S]+)\\(([0-9]+)/([0-9,\\*]*)\\)".r

  def apply(formel: String): TeamRegel = {
    def defaultMax(max: String): Int = if (max.equals("*")) 0 else max
    val regeln = formel.split(",").map(_.trim).filter(_.nonEmpty).toList
    val mappedRules: List[TeamRegel] = regeln.flatMap {
      case rangePattern(rulename, min, max) => rulename match {
        case "VereinGesamt" => Some(TeamRegelVereinGesamt(min, defaultMax(max)))
        case "VerbandGesamt" => Some(TeamRegelVerbandGesamt(min, defaultMax(max)))
        case "VereinGerät" => Some(TeamRegelVereinGeraet(min, defaultMax(max)))
        case "VerbandGerät" => Some(TeamRegelVerbandGeraet(min, defaultMax(max)))
        case _ => None
      }
      case "Keine Teams" => Some(defaultRegel.asInstanceOf[TeamRegel])
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
  def extractTeams(wertungen: Iterable[WertungView]): List[Team]
  def teamsAllowed: Boolean
  def toFormel: String
  def toRuleName: String
}
case class TeamRegelList(regeln: List[TeamRegel], name: Option[String] = None) extends TeamRegel {
  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    regeln.flatMap(_.extractTeams(wertungen))
  }

  override def teamsAllowed: Boolean = regeln.nonEmpty && regeln.exists(_.teamsAllowed)
  override def toFormel: String = name.getOrElse(regeln.map(_.toFormel).mkString(","))
  override def toRuleName: String = name.getOrElse(regeln.map(_.toRuleName).sorted.mkString(", "))
}

case class TeamRegelVereinGeraet(min: Int, max: Int) extends TeamRegel {
  override def toFormel: String = s"VereinGerät($min/${if (max > 0) max else "*"})"
  override def toRuleName: String = s"""Vereins-Team Rangliste (beste $min Gerätewertungen${if (max > 0) s" aus $max" else ""})"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    wertungen
      .filter(w => w.team > 0)
      .toList
      .groupBy(w => (w.athlet.verein, w.team))
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
          List(Team(s"${verein.map(_.easyprint).getOrElse("")} ${teamNummer}", toRuleName, limitedTeamwertungen, perDisciplinWertungen, perDisciplinCountingWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
case class TeamRegelVereinGesamt(min: Int, max: Int) extends TeamRegel {
  override def toFormel: String = s"VereinGesamt($min/${if (max > 0) max else "*"})"

  override def toRuleName: String = s"""Vereins-Team Rangliste (beste $min Gesamtwertungen${if (max > 0) s" aus $max" else ""})"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    wertungen
      .filter(w => w.team > 0)
      .toList
      .groupBy(w => (w.athlet.verein, w.team))
      .flatMap { team =>
        val (teamkey, teamwertungen) = team
        val (verein, teamNummer) = teamkey
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if (athletCount >= min && (max == 0 || athletCount <= max)) {
          val perDisciplinWertungen = teamwertungen
            .groupBy(w => w.athlet)
            .map{ case (athlet, wtgs) =>
              val wtgsum = wtgs.map(_.resultat).reduce(_+_)
              (athlet, wtgs, wtgsum)
            }
            .toList
            .sortBy(_._3).reverse.take(min) // sortiert auf Gesamtresultat
            .flatMap(_._2) // mit wertungen die Disziplin-Map aufbauen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
          val limitedTeamwertungen = if (max > 0) teamwertungen else {
            val allRelevantWertungen = perDisciplinWertungen.values.flatten.toSet
            teamwertungen.filter {
              allRelevantWertungen.contains
            }
          }
          List(Team(s"${verein.map(_.easyprint).getOrElse("")} ${teamNummer}", toRuleName, limitedTeamwertungen, perDisciplinWertungen, perDisciplinWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}

case class TeamRegelVerbandGeraet(min: Int, max: Int) extends TeamRegel {
  override def toFormel: String = s"VerbandGerät($min/${if (max > 0) max else "*"})"

  override def toRuleName: String = s"""Verbands-Team Rangliste (beste $min Gerätewertungen${if (max > 0) s" aus $max" else ""})"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    wertungen
      .filter(w => w.team > 0)
      .toList
      .groupBy(w => (w.athlet.verein.flatMap(_.verband), w.team))
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
          List(Team(s"${verband.getOrElse("")} ${teamNummer}", toRuleName, limitedTeamwertungen, perDisciplinCountingWertungen, perDisciplinWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
case class TeamRegelVerbandGesamt(min: Int, max: Int) extends TeamRegel {
  override def toFormel: String = s"VerbandGesamt($min/${if (max > 0) max else "*"})"
  override def toRuleName: String = s"""Verbands-Team Rangliste (beste $min Gesamtwertungen${if (max > 0) s" aus $max" else ""})"""

  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    wertungen
      .filter(w => w.team > 0)
      .toList
      .groupBy(w => (w.athlet.verein.flatMap(_.verband), w.team))
      .flatMap { team =>
        val (teamkey, teamwertungen) = team
        val (verband, teamNummer) = teamkey
        val athletCount = teamwertungen.map(w => w.athlet.id).toSet.size
        if (athletCount >= min && (max == 0 || athletCount <= max)) {
          val perDisciplinWertungen = teamwertungen
            .groupBy(w => w.athlet)
            .map{ case (athlet, wtgs) =>
              val wtgsum = wtgs.map(_.resultat).reduce(_+_)
              (athlet, wtgs, wtgsum)
            }
            .toList
            .sortBy(_._3).reverse.take(min) // sortiert auf Gesamtresultat
            .flatMap(_._2) // mit wertungen die Disziplin-Map aufbauen
            .groupBy(w => w.wettkampfdisziplin.disziplin)
          val limitedTeamwertungen = if (max > 0) teamwertungen else {
            val allRelevantWertungen = perDisciplinWertungen.values.flatten.toSet
            teamwertungen.filter {
              allRelevantWertungen.contains
            }
          }
          List(Team(s"${verband.getOrElse("")} ${teamNummer}", toRuleName, limitedTeamwertungen, perDisciplinWertungen, perDisciplinWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
