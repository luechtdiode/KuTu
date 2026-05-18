package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.Durchgang
import org.slf4j.LoggerFactory

object DurchgangGrouper {
  private val logger = LoggerFactory.getLogger(getClass)
  private val DurchgangNamePattern = "^(.*)\\((\\d+)\\)$".r
  private val DurchgangRiege2Pattern = "^(.*)\\((\\.+)\\)$".r

  def groupDurchgaengeByKategorien(durchgaenge: SuggestedDurchgaenge, maxParallelProGruppe: Int = Int.MaxValue): Seq[SuggestedDurchgang] = {
    val titleMapping = buildTitleMapping(durchgaenge, maxParallelProGruppe)
    durchgaenge.toSeq.sortBy { case (name, _) => sortKey(name) }.map { case (name, startgeraete) =>
      SuggestedDurchgang(name = name, title = titleMapping.getOrElse(name, name), startgeraete = startgeraete)
    }
  }

  def groupDurchgaengeByKategorienAsMap(durchgaenge: SuggestedDurchgaenge, maxParallelProGruppe: Int = Int.MaxValue): Map[(String, String), DurchgangStationZuteilung] = {
    groupDurchgaengeByKategorien(durchgaenge, maxParallelProGruppe).map(item => (item.title, item.name) -> item.startgeraete).toMap
  }

  private def buildTitleMapping(durchgaenge: SuggestedDurchgaenge, maxParallelProGruppe: Int = Int.MaxValue): Map[String, String] = {
    if maxParallelProGruppe < 2 then
      Map.empty
    else {
      case class GroupState(names: Vector[String], categories: Set[String], isRiege2: Boolean, forcedGroupingTitle: Boolean)
      val normalizedMaxParallel = if maxParallelProGruppe <= 0 then Int.MaxValue else maxParallelProGruppe

      val grouped = orderedNames(durchgaenge.keys.toSeq).foldLeft(Vector.empty[GroupState]) { (groups, durchgangName) =>
        val (_, iteration) = splitName(durchgangName)
        val thisIsRiege2 = iteration == Int.MaxValue
        val categories = categoriesOf(durchgangName)
        if categories.isEmpty then {
          logger.warn(s"No categories could be derived for suggested Durchgang '$durchgangName'. Falling back to the Durchgang name as title.")
          groups :+ GroupState(Vector(durchgangName), Set.empty, isRiege2=thisIsRiege2, forcedGroupingTitle = normalizedMaxParallel < Int.MaxValue)
        }
        else {
          val targetIndex = groups.indexWhere(state => state.categories.intersect(categories).isEmpty && state.names.size < normalizedMaxParallel && state.isRiege2 == thisIsRiege2)
          if targetIndex >= 0 then {
            groups.updated(targetIndex, GroupState(groups(targetIndex).names :+ durchgangName, groups(targetIndex).categories ++ categories, isRiege2=thisIsRiege2, groups(targetIndex).forcedGroupingTitle))
          }
          else {
            groups :+ GroupState(Vector(durchgangName), categories, isRiege2=thisIsRiege2, forcedGroupingTitle = normalizedMaxParallel < Int.MaxValue)
          }
        }
      }

      grouped.zipWithIndex.flatMap { indexedgroup =>
        val (group, idx) = indexedgroup
        val nonEmptyCategories = group.categories.filter(_.nonEmpty)
        val categoryLabel = nonEmptyCategories.toSeq.map(s => s.replace("(", "").replace(")", "")).sorted.mkString("(",", ",")")
        val title = if !group.forcedGroupingTitle && (group.names.size <= 1 || nonEmptyCategories.isEmpty) then group.names.head
        else if categoryLabel.nonEmpty then s"Abteilung ${idx + 1} $categoryLabel" else s"Abteilung ${idx + 1}"
        group.names.map(_ -> title)
      }.toMap
    }
  }

  def applyTitles(durchgaenge: Seq[Durchgang], titleMapping: Map[String, String]): Seq[Durchgang] = {
    durchgaenge.map(d => d.copy(title = titleMapping.getOrElse(d.name, d.name)))
  }

  private def orderedNames(names: Seq[String]): Seq[String] = names.sortBy(sortKey)

  private def sortKey(name: String): (String, Int, String) = {
    val (baseName, round) = splitName(name)
    (baseName, round, name)
  }

  private def splitName(name: String): (String, Int) = name match {
    case DurchgangNamePattern(base, round) => (base.trim, round.toIntOption.getOrElse(Int.MaxValue))
    case DurchgangRiege2Pattern(base, riege2name) => (s"${base.trim} ${riege2name}", Int.MaxValue)
    case _ => (name.trim, Int.MaxValue)
  }

  private[squad] def categoriesOf(durchgangName: String): Set[String] = {
    val (baseName, _) = splitName(durchgangName)
    baseName.replace("-Tu", "").replace("-Ti", "").split("&").map(_.trim).filter(_.nonEmpty).toSet
  }
}

