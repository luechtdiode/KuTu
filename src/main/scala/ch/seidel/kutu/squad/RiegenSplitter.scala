package ch.seidel.kutu.squad

import ch.seidel.kutu.domain._
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

trait RiegenSplitter {
  private val logger = LoggerFactory.getLogger(classOf[RiegenSplitter])
  
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
      splitToRiegenCount(split(ret.head, ".", cache) ++ ret.tail, requiredRiegenCount, cache)
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
    if (ret.size > 0 && ret.head._2.size > maxRiegenTurnerCount) then {
      splitToMaxTurnerCount(split(ret.head, "#", cache) ++ ret.tail, maxRiegenTurnerCount, cache)
    }
    else {
      ret
    }
  }
  
  private def split[A](riege: (String, Seq[A]), delimiter: String, cache: scala.collection.mutable.Map[String, Int]): Seq[(String, Seq[A])] = {
    val (key, r) = riege
    val oldKey1 = (key + delimiter).split(delimiter).headOption.getOrElse("Riege")
    val oldList = r.toList
    def occurences(key: String) = {
      val cnt = cache.getOrElse(key, 0) + 1
      cache.update(key, cnt)
      f"${cnt}%02d"
    }
    val key1 = if key.contains(delimiter) then key else oldKey1 + delimiter + occurences(oldKey1)
    val key2 = oldKey1 + delimiter + occurences(oldKey1)
    val splitpos = r.size / 2
    List((key1, oldList.take(splitpos)), (key2, oldList.drop(splitpos)))
  }
  
}