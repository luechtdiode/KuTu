package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

trait RiegenSplitter {
  private val logger = LoggerFactory.getLogger(classOf[RiegenSplitter])

  private def computeSplitPos(size: Int, targetSize: Option[Int]): Int = {
    val fallback = size / 2
    targetSize match {
      case Some(target) if target > 0 && size > 1 =>
        val bounded = math.max(1, math.min(size - 1, target))
        // Prefer non-symmetric splits when a meaningful target exists.
        if bounded == fallback && size > 2 then math.max(1, fallback - 1) else bounded
      case _ => fallback
    }
  }

  def groupKey(grplst: List[WertungView => String])(wertung: WertungView): String = {
    grplst.foldLeft(""){(acc, f) =>
      acc + "," + f(wertung)
    }.dropWhile(_ == ',')// remove leading ","
  }
  
  @tailrec
  final def splitToRiegenCount(sugg: Seq[(String, Seq[WertungViewsZuAthletView])], requiredRiegenCount: Int, cache: scala.collection.mutable.Map[String, Int]): Seq[(String, Seq[WertungViewsZuAthletView])] = {
    val ret = sugg.sortBy(_._2.size).reverse
    //logger.debug((ret.size, riegencnt))
    if ret.size < requiredRiegenCount then {
      val targetSize = math.ceil(1d * ret.map(_._2.size).sum / math.max(1, requiredRiegenCount)).toInt
      splitToRiegenCount(split(ret.head, ".", cache, Some(targetSize)) ++ ret.tail, requiredRiegenCount, cache)
    }
    else {
      //logger.debug(ret.mkString("\n"))
      ret
    }
  }
  
  @tailrec
  final def splitToMaxTurnerCount(sugg: Seq[(String, Seq[(AthletView, Seq[WertungView])])], maxRiegenTurnerCount: Int, cache: scala.collection.mutable.Map[String, Int]): Seq[(String, Seq[(AthletView, Seq[WertungView])])] = {
    //cache.clear
    val ret = sugg.sortBy(_._2.size).reverse
    if ret.nonEmpty && ret.head._2.size > maxRiegenTurnerCount then {
      splitToMaxTurnerCount(split(ret.head, "#", cache, Some(maxRiegenTurnerCount)) ++ ret.tail, maxRiegenTurnerCount, cache)
    }
    else {
      ret
    }
  }
  
  private def split[A](riege: (String, Seq[A]), delimiter: String, cache: scala.collection.mutable.Map[String, Int], targetSize: Option[Int] = None): Seq[(String, Seq[A])] = {
    val (key, r) = riege
    val oldKey1 = (key + delimiter).split(delimiter).headOption.getOrElse("Riege")
    val oldList = r.toList
    def occurences(key: String) = {
      val cnt = cache.getOrElse(key, 0) + 1
      cache.update(key, cnt)
      f"$cnt%02d"
    }
    val key1 = if key.contains(delimiter) then key else oldKey1 + delimiter + occurences(oldKey1)
    val key2 = oldKey1 + delimiter + occurences(oldKey1)
    val splitpos = computeSplitPos(r.size, targetSize)
    List((key1, oldList.take(splitpos)), (key2, oldList.drop(splitpos)))
  }
  
}