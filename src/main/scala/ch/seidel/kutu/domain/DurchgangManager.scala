package ch.seidel.kutu.domain

import ch.seidel.kutu.squad.DurchgangGrouper

/**
 * Reusable logic for durchgang manipulation, following the RiegenTab pattern:
 * load all → transform in memory → write all back.
 *
 * Used by both the JavaFX client and REST endpoints.
 */
class DurchgangManager(service: DurchgangService) {

  /**
   * Rename a single durchgang. Updates riege, durchgang, and durchgangstation tables.
   */
  def renameDurchgang(wettkampfId: Long, oldName: String, newName: String): Unit = {
    service.renameDurchgang(wettkampfId, oldName, newName)
  }

  /**
   * Merge multiple durchgänge into one target.
   * Renames each source durchgang to the target name.
   */
  def mergeDurchgaenge(wettkampfId: Long, sourceNames: Set[String], targetName: String): Unit = {
    sourceNames.foreach { sourceName =>
      if (sourceName != targetName) {
        service.renameDurchgang(wettkampfId, sourceName, targetName)
      }
    }
  }

  /**
   * Move durchgänge to a different group by changing their title.
   * Follows the RiegenTab pattern: load all, transform, write all.
   */
  def moveDurchgangToGroup(wettkampfId: Long, sourceNames: Set[String], targetGroupTitle: String): Unit = {
    withAllDurchgaenge(wettkampfId) { all =>
      val moved = all.map { d =>
        if (sourceNames.contains(d.name)) d.copy(title = targetGroupTitle) else d
      }
      recalculateAbteilungTitles(moved)
    }
  }

  /**
   * Dissolve group: set title = name for selected durchgänge.
   */
  def ungroupDurchgaenge(wettkampfId: Long, sourceNames: Set[String]): Unit = {
    withAllDurchgaenge(wettkampfId) { all =>
      all.map { d =>
        if (sourceNames.contains(d.name)) d.copy(title = d.name) else d
      }
    }
  }

  /**
   * Aggregate durchgänge into a group by setting a shared title.
   */
  def aggregateDurchgaenge(wettkampfId: Long, sourceNames: Set[String], groupTitle: String): Unit = {
    withAllDurchgaenge(wettkampfId) { all =>
      all.map { d =>
        if (sourceNames.contains(d.name)) d.copy(title = groupTitle) else d
      }
    }
  }

  private val AbteilungPattern = "^Abteilung (\\d+)(.*)$".r

  private def recalculateAbteilungTitles(durchgaenge: Seq[Durchgang]): Seq[Durchgang] = {
    val byTitle = durchgaenge.groupBy(_.title)
    byTitle.foldLeft(durchgaenge) { case (acc, (title, members)) =>
      title match {
        case AbteilungPattern(num, _) =>
          val categories = members.flatMap(m => DurchgangGrouper.categoriesOf(m.name)).toSet.filter(_.nonEmpty)
          val categoryLabel = categories.toSeq.map(s => s.replace("(", "").replace(")", "")).sorted.mkString("(", ", ", ")")
          val newTitle = if (categoryLabel.nonEmpty) s"Abteilung $num $categoryLabel" else s"Abteilung $num"
          if (newTitle == title) acc
          else acc.map(d => if (d.title == title) d.copy(title = newTitle) else d)
        case _ => acc
      }
    }
  }

  private def withAllDurchgaenge(wettkampfId: Long)(transform: Seq[Durchgang] => Seq[Durchgang]): Unit = {
    val simple = service.selectSimpleDurchgaenge(wettkampfId)
    val all = simple.map(sd => Durchgang(
      id = sd.id,
      wettkampfId = sd.wettkampfId,
      title = sd.title,
      name = sd.name,
      durchgangtype = sd.durchgangtype,
      ordinal = sd.ordinal,
      planStartOffset = sd.planStartOffset,
      effectiveStartTime = sd.effectiveStartTime,
      effectiveEndTime = sd.effectiveEndTime,
      planEinturnen = 0,
      planGeraet = 0,
      planTotal = 0
    ))
    val transformed = transform(all)
    service.updateOrInsertDurchgaenge(transformed)
  }
}
