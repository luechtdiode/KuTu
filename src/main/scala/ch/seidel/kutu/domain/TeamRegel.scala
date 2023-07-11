package ch.seidel.kutu.domain

object TeamRegel {
  val defaultRegel = TeamRegelList(List(), Some("Keine Teams"))

  val predefined = Map(
      ("Keine Teams" -> "")
    , ("Aus Verein, drei Bestnoten pro Gerät, mit max vier Mitglieder" -> "VereinGeräte(3/4)")
    , ("Aus Verband, drei Bestnoten pro Gerät, mit max vier Mitglieder" -> "VerbandGeräte(3/4)")
    , ("Aus Verein, drei Bestnoten pro Gerät, mit unbeschränkter Anzahl Mitglieder" -> "VereinGeräte(3/*)")
    , ("Aus Verband, drei Bestnoten pro Gerät, mit unbeschränkter Anzahl Mitglieder" -> "VerbandGeräte(3/*)")
    , ("Aus Verein, drei Gesamt-Bestnoten, mit max vier Mitglieder" -> "VereinGesamt(3/4)")
    , ("Aus Verband, drei Gesamt-Bestnoten, mit max vier Mitglieder" -> "VerbandGesamt(3/4)")
    , ("Aus Verein, drei Gesamt-Bestnoten, mit unbeschränkter Anzahl Mitglieder" -> "VereinGesamt(3/*)")
    , ("Aus Verband, drei Gesamt-Bestnoten, mit unbeschränkter Anzahl Mitglieder" -> "VerbandGesamt(3/*)")
    , ("Individuell" -> "VereinGesamt(<min>/<max>), VerbandGesamt(<min>/<max>, VereinGeräte(<min>/<max>), VerbandGeräte(<min>/<max>)")
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
}
case class TeamRegelList(regeln: List[TeamRegel], name: Option[String] = None) extends TeamRegel {
  override def extractTeams(wertungen: Iterable[WertungView]): List[Team] = {
    regeln.flatMap(_.extractTeams(wertungen))
  }

  override def teamsAllowed: Boolean = regeln.nonEmpty && regeln.exists(_.teamsAllowed)
  override def toFormel: String = name.getOrElse(regeln.map(_.toFormel).mkString(","))
}

case class TeamRegelVereinGeraet(min: Int, max: Int) extends TeamRegel {
  override def toFormel: String = s"VereinGerät($min/${if (max > 0) max else "*"})"

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
            .map { case (disciplin, wtgs) => (disciplin, wtgs
              .sortBy(_.resultat.endnote).reverse.take(min))
            }
          List(Team(s"${verein.map(_.easyprint).getOrElse("")} ${teamNummer}", teamwertungen, perDisciplinWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
case class TeamRegelVereinGesamt(min: Int, max: Int) extends TeamRegel {
  override def toFormel: String = s"VereinGesamt($min/${if (max > 0) max else "*"})"

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
          List(Team(s"${verein.map(_.easyprint).getOrElse("")} ${teamNummer}", teamwertungen, perDisciplinWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}

case class TeamRegelVerbandGeraet(min: Int, max: Int) extends TeamRegel {
  override def toFormel: String = s"VerbandGerät($min/${if (max > 0) max else "*"})"

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
            .map { case (disciplin, wtgs) => (disciplin, wtgs
              .sortBy(_.resultat.endnote).reverse.take(min))
            }
          List(Team(s"${verband.getOrElse("")} ${teamNummer}", teamwertungen, perDisciplinWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
case class TeamRegelVerbandGesamt(min: Int, max: Int) extends TeamRegel {
  override def toFormel: String = s"VerbandGesamt($min/${if (max > 0) max else "*"})"

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
          List(Team(s"${verband.getOrElse("")} ${teamNummer}", teamwertungen, perDisciplinWertungen))
        } else {
          List.empty
        }
      }.toList
  }

  override def teamsAllowed: Boolean = true
}
