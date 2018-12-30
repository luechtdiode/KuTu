package ch.seidel.kutu.data

import scala.io.Source

case class Surname(name: String, feminimCount: Int, masculinCount: Int) {
  val isFeminin = feminimCount > 0
  val isMasculin = masculinCount > 0
}

object Surname {
  def transformStatsMeaning(statsMeaning: String): Int = statsMeaning match {
    case "*" => 0
    case n => n.replaceAll("'", "").toInt
  }

  def apply(name: String, feminimCount: String, masculinCount: String): Surname = {
    Surname(name, transformStatsMeaning(feminimCount), transformStatsMeaning(masculinCount))
  }

  lazy val names: Set[Surname] = {
    val bufferedSource = Source.fromResource("vornamen.csv")("UTF-8")
    try {
      (for {
        line <- bufferedSource.getLines
        cols = line.split("\t").map(_.trim)
        if (cols.size == 3)
      } yield {
        Surname(cols(0), cols(1), cols(2))
      }).toSet[Surname]
    } catch {
      case e: Exception =>
        e.printStackTrace
        Set.empty
    } finally {
      bufferedSource.close
    }
  }

  def isFeminim(name: String) = names.find(sn => sn.isFeminin && sn.name.equalsIgnoreCase(name)).isDefined

  def isMasculin(name: String) = names.find(sn => sn.isMasculin && sn.name.equalsIgnoreCase(name)).isDefined

  def isSurname(name: String) = names.find(sn => sn.name.equalsIgnoreCase(name))
}

