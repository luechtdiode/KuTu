package ch.seidel.kutu.domain

import ch.seidel.kutu.data.GroupSection.STANDARD_SCORE_FACTOR
import org.controlsfx.validation.{Severity, ValidationResult, Validator}

import java.time.temporal.ChronoUnit
import scala.math.BigDecimal.{RoundingMode, long2bigDecimal}


object RiegenRotationsregel {
  val defaultRegel = RiegenRotationsregelList(List(
    RiegenRotationsregelKategorie,
    RiegenRotationsregelVerein,
    RiegenRotationsregelAlter(false),
    RiegenRotationsregelGeschlecht,
    RiegenRotationsregelName,
    RiegenRotationsregelVorname
  ), Some("Einfach"))

  val predefined = Map(
      ("Einfache Rotation" -> "Einfach")
    , ("Verschiebende Rotation" -> "Einfach/Rotierend")
    , ("Verschiebende Rotation alternierend invers" -> "Einfach/Rotierend/AltInvers")
    , ("Individuell" -> "")
  )

  def apply(formel: String): RiegenRotationsregel = {
    val regeln = formel.split("/").map(_.trim).filter(_.nonEmpty).toList
    val mappedRules: List[RiegenRotationsregel] = regeln.flatMap {
      case "Name" => Some(RiegenRotationsregelName.asInstanceOf[RiegenRotationsregel])
      case "Vorname" => Some(RiegenRotationsregelVorname.asInstanceOf[RiegenRotationsregel])
      case "Verein" => Some(RiegenRotationsregelVerein.asInstanceOf[RiegenRotationsregel])
      case "Geschlecht" => Some(RiegenRotationsregelGeschlecht.asInstanceOf[RiegenRotationsregel])
      case "Kategorie" => Some(RiegenRotationsregelKategorie.asInstanceOf[RiegenRotationsregel])
      case "AlterAbsteigend" => Some(RiegenRotationsregelAlter(false).asInstanceOf[RiegenRotationsregel])
      case "AlterAufsteigend" => Some(RiegenRotationsregelAlter(true).asInstanceOf[RiegenRotationsregel])
      case "AltInvers" => Some(RiegenRotationsregelAlternierendInvers.asInstanceOf[RiegenRotationsregel])
      case "Rotierend" => Some(RiegenRotationsregelRotierend.asInstanceOf[RiegenRotationsregel])
      case "Einfach" => Some(defaultRegel.asInstanceOf[RiegenRotationsregel])
      case s: String => None
    }
    if (!mappedRules.exists {
      case RiegenRotationsregelAlternierendInvers => false
      case RiegenRotationsregelRotierend => false
      case _ => true
    }) defaultRegel else {
      RiegenRotationsregelList(mappedRules)
    }
  }

  def apply(wettkampf: Wettkampf): RiegenRotationsregel = wettkampf.rotation match {
    case Some(regel) => this(regel)
    case _ => defaultRegel
  }
}

sealed trait RiegenRotationsregel {
  def sort(kandidat: Kandidat): String = sorter(List(), kandidat).map(align).mkString("-")
  def sorter(acc: List[String], kandidat: Kandidat): List[String]
  def toFormel: String

  private def align(s: String): String = {
    if (s.matches("^[0-9]+$")) s else f"$s%-30s"
  }
}
case class RiegenRotationsregelList(regeln: List[RiegenRotationsregel], name: Option[String] = None) extends RiegenRotationsregel {
  override def sorter(acc: List[String], kandidat: Kandidat): List[String] = {
    regeln.foldLeft(List[String]()){(acc, regel) =>
      regel.sorter(acc, kandidat)
    }
  }

  override def toFormel: String = name.getOrElse(regeln.map(_.toFormel).mkString("/"))
}

case object RiegenRotationsregelAlternierendInvers extends RiegenRotationsregel {
  override def sorter(acc: List[String], kandidat: Kandidat): List[String] = {
    val date = kandidat.wertungen.head.wettkampf.datum.toLocalDate
    val day = date.getDayOfYear
    val reversed = day % 2 == 0
    if (reversed) acc.map(inverseIfAlphaNumeric) else acc
  }
  private def inverseIfAlphaNumeric(s: String): String = {
    if (s.matches("^[0-9]+$")) s else s.reverse
  }
  override def toFormel: String = "AltInvers"
}

case object RiegenRotationsregelRotierend extends RiegenRotationsregel {
  def rotateIfAlphaNumeric(text: String, offset: Int): String = {
    if (text.matches("^[0-9]+$")) text else text.trim.toUpperCase().map(rotate(_, offset))
  }

  def rotate(text: Char, offset: Int): Char = {
    val r1 = text + offset
    val r2 = if (r1 > 'Z') {
      r1 - 26
    } else if (r1 < 'A') {
      r1 + 26
    } else {
      r1
    }
    r2.toChar
  }
  override def sorter(acc: List[String], kandidat: Kandidat): List[String] = {
    val date = kandidat.wertungen.head.wettkampf.datum.toLocalDate
    val day = date.getDayOfYear
    val alphaOffset = day % 26
    acc.map(rotateIfAlphaNumeric(_, alphaOffset))
  }
  override def toFormel: String = "Rotierend"
}

case object RiegenRotationsregelName extends RiegenRotationsregel {
  override def sorter(acc: List[String], kandidat: Kandidat): List[String] =
    acc :+ kandidat.name
  override def toFormel: String = "Name"
}

case object RiegenRotationsregelVorname extends RiegenRotationsregel {
  override def sorter(acc: List[String], kandidat: Kandidat): List[String] =
    acc :+ kandidat.vorname

  override def toFormel: String = "Vorname"
}

case object RiegenRotationsregelKategorie extends RiegenRotationsregel {
  override def sorter(acc: List[String], kandidat: Kandidat): List[String] =
    acc :+ kandidat.programm

  override def toFormel: String = "Vorname"
}

case object RiegenRotationsregelVerein extends RiegenRotationsregel {
  override def sorter(acc: List[String], kandidat: Kandidat): List[String] =
    acc :+ kandidat.verein
    .replaceAll("BTV", "")
    .replaceAll("DTV", "")
    .replaceAll("STV", "")
    .replaceAll("GETU", "")
    .replaceAll("TSV", "")
    .replaceAll("TV", "")
    .replaceAll("TZ", "")
    .replaceAll(" ", "")

  override def toFormel: String = "Verein"
}
case object RiegenRotationsregelGeschlecht extends RiegenRotationsregel {
  override def sorter(acc: List[String], kandidat: Kandidat): List[String] =
    acc :+ kandidat.geschlecht

  override def toFormel: String = "Geschlecht"
}
case class RiegenRotationsregelAlter(aufsteigend: Boolean) extends RiegenRotationsregel {
  override def sorter(acc: List[String], kandidat: Kandidat): List[String] = {
    val date = kandidat.wertungen.head.wettkampf.datum.toLocalDate
    val alter = try {
      val bdate: Int = str2Int(kandidat.jahrgang)
      date.getYear - bdate
    } catch {
      case _: NumberFormatException => 100
    }
    val jg = (if (alter > 15) "0000" else kandidat.jahrgang)
    acc :+ (if (aufsteigend) kandidat.jahrgang.reverse else kandidat.jahrgang)
  }
  override def toFormel: String = if (aufsteigend) "AlterAufsteigend" else "AlterAbsteigend"
}
