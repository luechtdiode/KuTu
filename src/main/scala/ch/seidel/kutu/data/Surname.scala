package ch.seidel.kutu.data

import scala.io.Source

case class Surname(name: String, feminimCount: Int, masculinCount: Int) {
  val isFeminin: Boolean = feminimCount > 0
  val isMasculin: Boolean = masculinCount > 0
  def matchesSex(sex: String): Boolean = (isFeminin && "F".equalsIgnoreCase(sex)) || (isMasculin && "M".equalsIgnoreCase(sex))
}

object Surname {
  import ch.seidel.kutu.domain.given_Conversion_String_Int
  private def transformStatsMeaning(statsMeaning: String): Int = statsMeaning match {
    case "*" => 0
    case n => n.replace("'", "")
  }

  def apply(name: String, feminimCount: String, masculinCount: String): Surname = {
    Surname(name, transformStatsMeaning(feminimCount), transformStatsMeaning(masculinCount))
  }

  private lazy val names: Set[Surname] = {
    given String = "UTF-8"
    val bufferedSource = Source.fromResource("vornamen.csv")
    try {
      (for
        line <- bufferedSource.getLines()
        cols = line.split("\t").map(_.trim)
        if cols.length == 3
      yield {
        Surname(cols(0), cols(1), cols(2))
      }).toSet[Surname]
    } catch {
      case e: Exception =>
        e.printStackTrace()
        Set.empty
    } finally {
      bufferedSource.close
    }
  }

  def isFeminim(name: String): Boolean = {
    val namen = name.split(" ")
    namen.forall(sn => isSurname(sn).exists(sn => sn.isFeminin))
  }

  def isMasculin(name: String): Boolean ={
    val namen = name.split(" ")
    namen.forall(sn => isSurname(sn).exists(sn => sn.isMasculin))
  }
  def isSurname(name: String): Option[Surname] = {
    val namen = name.split(" ")
    val surnames = names.filter(sn => namen.exists(nn => sn.name.equalsIgnoreCase(nn)))
    val isMc = surnames.count(_.isMasculin)
    val isFc = surnames.count(_.isFeminin)
    val isM = isMc >= isFc
    val isF = isMc <= isFc
    if surnames.nonEmpty && (isF || isM) then
      Some(Surname(name, if isF then 1 else 0, if isM then 1 else 0))
    else
      None
  }
}

