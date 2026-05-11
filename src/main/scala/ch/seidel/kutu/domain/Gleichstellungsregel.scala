package ch.seidel.kutu.domain

import org.controlsfx.validation.{Severity, ValidationResult, Validator}

import java.time.temporal.ChronoUnit
import scala.math.BigDecimal.long2bigDecimal


object Gleichstandsregel {
  private val getu = "Disziplin(Schaukelringe,Sprung,Reck)"
  private val kutu = "E-Note-Summe/D-Note-Summe/JugendVorAlter"
  private val kutustv = "StreichWertungen(Endnote,Min)/StreichWertungen(E-Note,Min)/StreichWertungen(D-Note,Min)"

  val predefined: Map[String, String] = Map(
      "Ohne - Punktgleichstand => gleicher Rang" -> "Ohne"
    , "GeTu Punktgleichstandsregel" -> getu
    , "KuTu Punktgleichstandsregel" -> kutu
    , "KuTu STV Punktgleichstandsregel" -> kutustv
    , "Individuell" -> ""
  )
  private val disziplinPattern = "^Disziplin\\((.+)\\)$".r
  private val streichDisziplinPattern = "^StreichDisziplin\\((.+)\\)$".r
  private val streichWertungPattern = "^StreichWertungen\\((Endnote|E-Note|D-Note)(,(Min|Max))*\\)$".r

  def apply(formel: String): Gleichstandsregel = {
    val gleichstandsregelList = parseFormel(formel)
    validated(gleichstandsregelList)
  }

  def validated(regel: Gleichstandsregel): Gleichstandsregel = {
    regel
  }
  private def parseFormel(formel: String) = {
    val regeln = formel.split("/").toList

    val mappedFactorizers: List[Gleichstandsregel] = regeln.flatMap {
      case streichDisziplinPattern(dl) => Some(GleichstandsregelStreichDisziplin(dl.split(",").toList).asInstanceOf[Gleichstandsregel])
      case disziplinPattern(dl) => Some(GleichstandsregelDisziplin(dl.split(",").toList).asInstanceOf[Gleichstandsregel])
      case streichWertungPattern(typ, _, minmax) => Some(GleichstandsregelStreichWertungen(typ, minmax).asInstanceOf[Gleichstandsregel])
      case "StreichWertungen" => Some(GleichstandsregelStreichWertungen().asInstanceOf[Gleichstandsregel])
      case "E-Note-Summe" => Some(GleichstandsregelENoteSumme.asInstanceOf[Gleichstandsregel])
      case "E-Note-Best" => Some(GleichstandsregelENoteBest.asInstanceOf[Gleichstandsregel])
      case "D-Note-Summe" => Some(GleichstandsregelDNoteSumme.asInstanceOf[Gleichstandsregel])
      case "D-Note-Best" => Some(GleichstandsregelDNoteBest.asInstanceOf[Gleichstandsregel])
      case "JugendVorAlter" => Some(GleichstandsregelJugendVorAlter.asInstanceOf[Gleichstandsregel])
      case "Ohne" => Some(GleichstandsregelDefault.asInstanceOf[Gleichstandsregel])
      case s: String => Some(GleichstandsregelDefault.asInstanceOf[Gleichstandsregel])
    }
    if mappedFactorizers.isEmpty then {
      GleichstandsregelList(List(GleichstandsregelDefault))
    } else {
      GleichstandsregelList(mappedFactorizers)
    }
  }

  def createValidator: Validator[String] = (control, formeltext) => {
    try {
      val gleichstandsregelList = parseFormel(formeltext)
      validated(gleichstandsregelList)
      ValidationResult.fromMessageIf(control, "Formel valid", Severity.ERROR, false)
    } catch {
      case e: Exception =>
        ValidationResult.fromMessageIf(control, e.getMessage, Severity.ERROR, true)
    }
  }

  def apply(programmId: Long): Gleichstandsregel = {
    programmId match {
      case id if id > 0 && id < 4 => // Athletiktest
        GleichstandsregelDefault
      case id if (id > 10 && id < 20) || id == 28 => // KuTu Programm
        Gleichstandsregel(kutustv)
      case id if (id > 19 && id < 27) || (id > 73 && id < 84) => // GeTu Kategorie
        Gleichstandsregel(getu)
      case id if id > 30 && id < 41 => // KuTuRi Programm
        Gleichstandsregel(kutustv)
      case _ => GleichstandsregelDefault
    }
  }
  def apply(wettkampf: Wettkampf): Gleichstandsregel = wettkampf.punktegleichstandsregel match {
    case Some(regel) if regel.trim.nonEmpty => this (regel)
    case _ => this (wettkampf.programmId)
  }
}

sealed trait Gleichstandsregel {
  protected val maxvalue = 30000 // max is 30.000
  def rankingValue(athlWertungen: List[WertungView]): BigDecimal
  protected def maxOrZero(values: List[BigDecimal]): BigDecimal = if values.isEmpty then 0 else values.max
  def compare(left: List[WertungView], right: List[WertungView]): Int = rankingValue(left).compare(rankingValue(right))
  def toFormel: String
}

case object GleichstandsregelDefault extends Gleichstandsregel {
  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = 0
  override def toFormel: String = "Ohne"
}

case class GleichstandsregelList(regeln: List[Gleichstandsregel]) extends Gleichstandsregel {
  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = {
    regeln.map(_.rankingValue(athlWertungen)).sum
  }
  override def compare(left: List[WertungView], right: List[WertungView]): Int = {
    regeln.iterator
      .map(_.compare(left, right))
      .find(_ != 0)
      .getOrElse(0)
  }
  override def toFormel: String = regeln.map(_.toFormel).mkString("/")
}

case class GleichstandsregelDisziplin(disziplinOrder: List[String]) extends Gleichstandsregel {
  override def toFormel: String = s"Disziplin${disziplinOrder.mkString("(", ",", ")")}"

  private val zippedDisziplins = disziplinOrder.reverse.zipWithIndex
  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = {
    val result = zippedDisziplins.foldLeft(BigDecimal(0L)) { (acc, disziplin) =>
      val wertungen = athlWertungen.filter(_.wettkampfdisziplin.disziplin.name.equals(disziplin._1))
      val level = BigDecimal(maxvalue).pow(disziplin._2)
      val wertungenSum = wertungen.map(_.resultat).map(_.endnote).sum
      acc + wertungenSum * level
    }
    result
  }
}

case class GleichstandsregelStreichDisziplin(disziplinOrder: List[String]) extends Gleichstandsregel {
  override def toFormel: String = s"StreichDisziplin${disziplinOrder.mkString("(", ",", ")")}"
  override val maxvalue: Int = 300 * 6//disziplinOrder.length

  private val reversedOrder = disziplinOrder.reverse.zipWithIndex
  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = {
    reversedOrder.foldLeft(BigDecimal(0L)) { (acc, disziplin) =>
      val wertungen = athlWertungen.filter(!_.wettkampfdisziplin.disziplin.name.equals(disziplin._1))
      val level = BigDecimal(maxvalue).pow(disziplin._2)
      val wertungenSum = wertungen.map(_.resultat).map(_.endnote).sum
      acc + (wertungenSum * level)
    }
  }
}

case class GleichstandsregelStreichWertungen(typ: String = "Endnote", minmax: String = "Min") extends Gleichstandsregel {
  private val _minmax = if minmax == null || minmax.isEmpty then "Min" else minmax
  override def toFormel: String = s"StreichWertungen($typ,${_minmax})"
  private val maxGeraete = 6
  override val maxvalue: Int = 180

  private def pickWertung(w: WertungView): BigDecimal = {
    typ match {
      case "Endnote" => w.resultat.endnote
      case "A-Note" => w.resultat.noteD
      case "D-Note" => w.resultat.noteD
      case "B-Note" => w.resultat.noteE
      case "E-Note" => w.resultat.noteE
      case _ => w.resultat.endnote
    }
  }

  private def sort(wertungen: List[WertungView]): List[WertungView] = {
    _minmax match {
      case "Min" => wertungen.sortBy(pickWertung)
      case "Max" => wertungen.sortBy(pickWertung).reverse
      case _ => wertungen
    }
  }

  private def f(athlWertungen: List[WertungView]): BigDecimal = {
    if athlWertungen.nonEmpty then {
      val level = BigDecimal(maxvalue).pow(athlWertungen.size)
      val sum = athlWertungen
        .map(pickWertung)
        .sum
      sum * level + f(athlWertungen.drop(1))
    } else {
      BigDecimal(0L)
    }
  }

  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = {
    f(sort(athlWertungen).slice(1, maxGeraete)) + BigDecimal(maxvalue).pow(maxGeraete - athlWertungen.size)
  }
}

case object GleichstandsregelJugendVorAlter extends Gleichstandsregel {
  override def toFormel: String = "JugendVorAlter"
  override val maxvalue: Int = 100

  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = {
    athlWertungen.headOption.map { currentWertung =>
      val jet = currentWertung.wettkampf.datum.toLocalDate
      val gebdat = currentWertung.athlet.gebdat match {
        case Some(d) => d.toLocalDate
        case None => jet.minus(maxvalue, ChronoUnit.YEARS)
      }
      val alterInTagen = jet.toEpochDay - gebdat.toEpochDay
      val alterInJahren = alterInTagen / 365
      maxvalue - alterInJahren
    }.getOrElse(0L)
  }
}

case object GleichstandsregelENoteBest extends Gleichstandsregel {
  override def toFormel: String = "E-Note-Best"

  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = {
    maxOrZero(athlWertungen.map(_.resultat.noteE))
  }
}

case object GleichstandsregelENoteSumme extends Gleichstandsregel {
  override def toFormel: String = "E-Note-Summe"

  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = {
    athlWertungen.map(w => w.resultat).map(_.noteE).sum
  }
}

case object GleichstandsregelDNoteBest extends Gleichstandsregel {
  override def toFormel: String = "D-Note-Best"

  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = {
    maxOrZero(athlWertungen.map(_.resultat.noteD))
  }
}

case object GleichstandsregelDNoteSumme extends Gleichstandsregel {
  override def toFormel: String = "D-Note-Summe"

  override def rankingValue(athlWertungen: List[WertungView]): BigDecimal = {
    athlWertungen.map(w => w.resultat).map(_.noteD).sum
  }
}
