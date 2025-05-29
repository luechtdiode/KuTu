package ch.seidel.kutu.domain

import ch.seidel.kutu.data.GroupSection.STANDARD_SCORE_FACTOR
import org.controlsfx.validation.{Severity, ValidationResult, Validator}

import java.time.temporal.ChronoUnit
import scala.math.BigDecimal.{RoundingMode, long2bigDecimal}


object Gleichstandsregel {
  private val getu = "Disziplin(Schaukelringe,Sprung,Reck)"
  private val kutu = "E-Note-Summe/D-Note-Summe/JugendVorAlter"
  private val kutustv = "StreichWertungen(Endnote,Min)/StreichWertungen(E-Note,Min)/StreichWertungen(D-Note,Min)"

  val predefined = Map(
      ("Ohne - Punktgleichstand => gleicher Rang" -> "Ohne")
    , ("GeTu Punktgleichstandsregel" -> getu)
    , ("KuTu Punktgleichstandsregel" -> kutu)
    , ("KuTu STV Punktgleichstandsregel" -> kutustv)
    , ("Individuell" -> "")
  )
  val disziplinPattern = "^Disziplin\\((.+)\\)$".r
  val streichDisziplinPattern = "^StreichDisziplin\\((.+)\\)$".r
  val streichWertungPattern = "^StreichWertungen\\((Endnote|E-Note|D-Note)(,(Min|Max))*\\)$".r

  def apply(formel: String): Gleichstandsregel = {
    val gleichstandsregelList = parseFormel(formel)
    validated(gleichstandsregelList)
  }

  def validated(regel: Gleichstandsregel): Gleichstandsregel = {
    try {
      if (STANDARD_SCORE_FACTOR / 1000L < regel.powerRange) {
        println(s"Max scorefactor ${STANDARD_SCORE_FACTOR / 1000L}, powerRange ${regel.powerRange}, zu gross: ${regel.powerRange - STANDARD_SCORE_FACTOR / 1000L}")
        throw new RuntimeException("Bitte reduzieren, es sind zu viele Regeln definiert")
      }
    } catch {
      case _: ArithmeticException => throw new RuntimeException("Bitte reduzieren, es sind zu viele Regeln definiert")
      case y: Exception => throw y
    }
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
    if (mappedFactorizers.isEmpty) {
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
      case id if (id > 0 && id < 4) => // Athletiktest
        GleichstandsregelDefault
      case id if ((id > 10 && id < 20) || id == 28) => // KuTu Programm
        Gleichstandsregel(kutu)
      case id if (id > 19 && id < 27) || (id > 73 && id < 84) => // GeTu Kategorie
        Gleichstandsregel(getu)
      case id if (id > 30 && id < 41) => // KuTuRi Programm
        Gleichstandsregel(kutu)
      case _ => GleichstandsregelDefault
    }
  }
  def apply(wettkampf: Wettkampf): Gleichstandsregel = wettkampf.punktegleichstandsregel match {
    case Some(regel) if (regel.trim.nonEmpty) => this (regel)
    case _ => this (wettkampf.programmId)
  }
}

sealed trait Gleichstandsregel {
  def factorize(athlWertungen: List[WertungView]): BigDecimal
  def toFormel: String
  def powerRange: Long = 1L
}

case object GleichstandsregelDefault extends Gleichstandsregel {
  override def factorize(athlWertungen: List[WertungView]): BigDecimal = 1
  override def toFormel: String = "Ohne"
}

case class GleichstandsregelList(regeln: List[Gleichstandsregel]) extends Gleichstandsregel {
  override def factorize(athlWertungen: List[WertungView]): BigDecimal = {
    regeln
      .foldLeft((BigDecimal(0), STANDARD_SCORE_FACTOR / 1000L)){(acc, regel) =>
        val factor = regel.factorize(athlWertungen)
        (acc._1 + factor * acc._2 / regel.powerRange, acc._2 / regel.powerRange)
      }
      ._1.setScale(0, RoundingMode.HALF_UP)
  }
  override def toFormel: String = regeln.map(_.toFormel).mkString("/")
  override def powerRange: Long = regeln
    .foldRight(BigDecimal(1L)) { (regel, acc) =>
      acc + acc * regel.powerRange
    }.toLongExact
}

case class GleichstandsregelDisziplin(disziplinOrder: List[String]) extends Gleichstandsregel {
  override def toFormel: String = s"Disziplin${disziplinOrder.mkString("(", ",", ")")}"
  override def powerRange: Long = 10000L

  val zippedDisziplins = disziplinOrder.reverse.zipWithIndex
  override def factorize(athlWertungen: List[WertungView]): BigDecimal = {
    zippedDisziplins.foldLeft(BigDecimal(0L)) { (acc, disziplin) =>
      val wertungen = athlWertungen.filter(_.wettkampfdisziplin.disziplin.name == disziplin._1)
      val level = BigDecimal(100 * disziplin._2 + 1)
      val wertungenSum = wertungen.map(_.resultat).map(_.endnote * level).sum
      acc + wertungenSum
    }.setScale(0, RoundingMode.HALF_UP)
  }
}

case class GleichstandsregelStreichDisziplin(disziplinOrder: List[String]) extends Gleichstandsregel {
  override def toFormel: String = s"StreichDisziplin${disziplinOrder.mkString("(", ",", ")")}"
  override def powerRange: Long = 1000000L

  val reversedOrder = disziplinOrder.reverse.zipWithIndex
  override def factorize(athlWertungen: List[WertungView]): BigDecimal = {
    reversedOrder.foldLeft(BigDecimal(0L)) { (acc, disziplin) =>
      val wertungen = athlWertungen.filter(_.wettkampfdisziplin.disziplin.name != disziplin._1)
      val level = BigDecimal(100 * disziplin._2 + 1)
      val wertungenSum = wertungen.map(_.resultat).map(_.endnote * level).sum
      acc + wertungenSum
    }.setScale(3, RoundingMode.HALF_UP)
  }
}

case class GleichstandsregelStreichWertungen(typ: String = "Endnote", minmax: String = "Min") extends Gleichstandsregel {
  private val _minmax = if (minmax == null || minmax.isEmpty) "Min" else minmax
  override def toFormel: String = s"StreichWertungen($typ,${_minmax})"
  override def powerRange: Long = 100000L

  private def pickWertung(w: WertungView): BigDecimal = {
    typ match {
      case "Endnote" => w.resultat.endnote
      case "D-Note" => w.resultat.noteD
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

    if (athlWertungen.nonEmpty) {
      val level = BigDecimal(1000 * athlWertungen.size)
      val sum = athlWertungen
        .map(pickWertung)
        .sum.setScale(3, RoundingMode.HALF_UP)
      sum * level + f(athlWertungen.drop(1))
    } else {
      BigDecimal(0L)
    }
  }

  override def factorize(athlWertungen: List[WertungView]): BigDecimal = {
    f(sort(athlWertungen).drop(1))
  }
}

case object GleichstandsregelJugendVorAlter extends Gleichstandsregel {
  override def toFormel: String = "JugendVorAlter"

  override def powerRange: Long = 100L

  override def factorize(athlWertungen: List[WertungView]): BigDecimal = {
    val currentWertung = athlWertungen.head
    val jet = currentWertung.wettkampf.datum.toLocalDate
    val gebdat = currentWertung.athlet.gebdat match {
      case Some(d) => d.toLocalDate
      case None => jet.minus(100, ChronoUnit.YEARS)
    }
    val alterInTagen = jet.toEpochDay - gebdat.toEpochDay
    val alterInJahren = alterInTagen / 365
    100L - alterInJahren
  }
}

case object GleichstandsregelENoteBest extends Gleichstandsregel {
  override def toFormel: String = "E-Note-Best"
  override def powerRange = 10000L

  override def factorize(athlWertungen: List[WertungView]): BigDecimal = {
    athlWertungen.map(w => w.resultat).map(_.noteE).max * 1000L setScale(0, RoundingMode.HALF_UP)
  }

}

case object GleichstandsregelENoteSumme extends Gleichstandsregel {
  override def toFormel: String = "E-Note-Summe"
  override def powerRange = 100000L
  override def factorize(athlWertungen: List[WertungView]): BigDecimal = {
    athlWertungen.map(w => w.resultat).map(_.noteE).sum * 1000L setScale(0, RoundingMode.HALF_UP)
  }
}

case object GleichstandsregelDNoteBest extends Gleichstandsregel {
  override def toFormel: String = "D-Note-Best"
  override def powerRange = 10000L

  override def factorize(athlWertungen: List[WertungView]): BigDecimal = {
    athlWertungen.map(w => w.resultat).map(_.noteD).max * 1000L setScale(0, RoundingMode.HALF_UP)
  }
}

case object GleichstandsregelDNoteSumme extends Gleichstandsregel {
  override def toFormel: String = "D-Note-Summe"
  override def powerRange = 100000L

  override def factorize(athlWertungen: List[WertungView]): BigDecimal = {
    athlWertungen.map(w => w.resultat).map(_.noteD).sum * 1000L setScale(0, RoundingMode.HALF_UP)
  }
}
