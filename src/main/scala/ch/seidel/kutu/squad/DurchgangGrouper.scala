package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.Durchgang
import org.slf4j.LoggerFactory

object DurchgangGrouper {
  private val logger = LoggerFactory.getLogger(getClass)
  private val DurchgangNamePattern = "^(.*)\\((\\d+)\\)$".r

  def groupDurchgaengeByKategorien(durchgaenge: SuggestedDurchgaenge): Seq[SuggestedDurchgang] = {
    val titleMapping = buildTitleMapping(durchgaenge)
    durchgaenge.toSeq.sortBy { case (name, _) => sortKey(name) }.map { case (name, startgeraete) =>
      SuggestedDurchgang(name = name, title = titleMapping.getOrElse(name, name), startgeraete = startgeraete)
    }
  }

  def groupDurchgaengeByKategorienAsMap(durchgaenge: SuggestedDurchgaenge): Map[(String, String), DurchgangStationZuteilung] = {
    groupDurchgaengeByKategorien(durchgaenge).map(item => (item.title, item.name) -> item.startgeraete).toMap
  }

  def buildTitleMapping(durchgaenge: SuggestedDurchgaenge): Map[String, String] = {
    case class GroupState(names: Vector[String], categories: Set[String])

    val grouped = orderedNames(durchgaenge.keys.toSeq).foldLeft(Vector.empty[GroupState]) { (groups, durchgangName) =>
      val categories = categoriesOf(durchgangName)
      if categories.isEmpty then {
        logger.warn(s"No categories could be derived for suggested Durchgang '$durchgangName'. Falling back to the Durchgang name as title.")
        groups :+ GroupState(Vector(durchgangName), Set.empty)
      }
      else {
        val targetIndex = groups.indexWhere(state => state.categories.intersect(categories).isEmpty)
        if targetIndex >= 0 then groups.updated(targetIndex, GroupState(groups(targetIndex).names :+ durchgangName, groups(targetIndex).categories ++ categories))
        else groups :+ GroupState(Vector(durchgangName), categories)
      }
    }

    grouped.zipWithIndex.flatMap { indexedgroup =>
      val (group, idx) = indexedgroup
      val nonEmptyCategories = group.categories.filter(_.nonEmpty)
      val title = if group.names.size <= 1 || nonEmptyCategories.isEmpty then group.names.head
      else s"Abteilung ${idx+1} ${nonEmptyCategories.toSeq.sorted.mkString("-")}"
      group.names.map(_ -> title)
    }.toMap
  }

  def applyTitles(durchgaenge: Seq[Durchgang], titleMapping: Map[String, String]): Seq[Durchgang] = {
    durchgaenge.map(d => d.copy(title = titleMapping.getOrElse(d.name, d.name)))
  }

  private def orderedNames(names: Seq[String]): Seq[String] = names.sortBy(sortKey)

  private def sortKey(name: String): (Int, String, String) = {
    val (baseName, round) = splitName(name)
    (round, baseName, name)
  }

  private def splitName(name: String): (String, Int) = name match {
    case DurchgangNamePattern(base, round) => (base.trim, round.toIntOption.getOrElse(Int.MaxValue))
    case _ => (name.trim, Int.MaxValue)
  }

  private[squad] def categoriesOf(durchgangName: String): Set[String] = {
    val (baseName, _) = splitName(durchgangName)
    baseName.split("&").map(_.trim).filter(_.nonEmpty).toSet
  }
}

