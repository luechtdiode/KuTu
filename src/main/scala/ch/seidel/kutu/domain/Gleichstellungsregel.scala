package ch.seidel.kutu.domain

import ch.seidel.kutu.data.GroupSection.STANDARD_SCORE_FACTOR
import org.controlsfx.validation.{Severity, ValidationResult, Validator}

import java.time.temporal.ChronoUnit
import scala.math.BigDecimal.{RoundingMode, long2bigDecimal}


object Gleichstandsregel {
  private val getu = "Disziplin(Schaukelringe,Sprung,Reck)"
  private val kutu = "E-Note-Summe/D-Note-Summe/JugendVorAlter"
  val predefined = Map(
      ("Ohne - Punktgleichstand => gleicher Rang" -> "Ohne")
    , ("GeTu Punktgleichstandsregel" -> getu)
    , ("KuTu Punktgleichstandsregel" -> kutu)
    , ("Individuell" -> "")
  )
  val disziplinPattern = "^Disziplin\\((.+)\\)$".r

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
      case disziplinPattern(dl) => Some(GleichstandsregelDisziplin(dl.split(",").toList).asInstanceOf[Gleichstandsregel])
      case "E-Note-Summe" => Some(GleichstandsregelENoteSumme.asInstanceOf[Gleichstandsregel])
      case "E-Note-Best" => Some(GleichstandsregelENoteBest.asInstanceOf[Gleichstandsregel])
      case "D-Note-Summe" => Some(GleichstandsregelDNoteSumme.asInstanceOf[Gleichstandsregel])
      case "D-Note-Best" => Some(GleichstandsregelDNoteBest.asInstanceOf[Gleichstandsregel])
      case "JugendVorAlter" => Some(GleichstandsregelJugendVorAlter.asInstanceOf[Gleichstandsregel])
      case "Ohne" => Some(GleichstandsregelDefault.asInstanceOf[Gleichstandsregel])
      case s: String => throw new RuntimeException(s"Unbekannte Regel '$s'")
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
  def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): BigDecimal
  def toFormel: String
  def powerRange: Long = 1L
}

case object GleichstandsregelDefault extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): BigDecimal = 1
  override def toFormel: String = "Ohne"
}

case class GleichstandsregelList(regeln: List[Gleichstandsregel]) extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): BigDecimal = {
    regeln
      .foldLeft((BigDecimal(0), STANDARD_SCORE_FACTOR / 1000L)){(acc, regel) =>
        val factor = regel.factorize(currentWertung, athlWertungen)
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
  override def powerRange: Long = Math.pow(10000, disziplinOrder.size).toLong

  val reversedOrder = disziplinOrder.reverse
  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): BigDecimal = {
    val idx = 1 + reversedOrder.indexOf(currentWertung.wettkampfdisziplin.disziplin.name)
    val ret = if (idx <= 0) {
      BigDecimal(1L)
    }
    else {
      BigDecimal(Math.pow(currentWertung.wettkampfdisziplin.max*100, idx))
    }
    ret.setScale(0, RoundingMode.HALF_UP)
  }
}

case object GleichstandsregelJugendVorAlter extends Gleichstandsregel {
  override def toFormel: String = "JugendVorAlter"

  override def powerRange: Long = 100L

  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): BigDecimal = {
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

  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): BigDecimal = {
    athlWertungen.map(_.noteE).max * 1000L setScale(0, RoundingMode.HALF_UP)
  }

}

case object GleichstandsregelENoteSumme extends Gleichstandsregel {
  override def toFormel: String = "E-Note-Summe"
  override def powerRange = 100000L
  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): BigDecimal = {
    athlWertungen.map(_.noteE).sum * 1000L setScale(0, RoundingMode.HALF_UP)
  }
}

case object GleichstandsregelDNoteBest extends Gleichstandsregel {
  override def toFormel: String = "D-Note-Best"
  override def powerRange = 10000L

  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): BigDecimal = {
    athlWertungen.map(_.noteD).max * 1000L setScale(0, RoundingMode.HALF_UP)
  }
}

case object GleichstandsregelDNoteSumme extends Gleichstandsregel {
  override def toFormel: String = "D-Note-Summe"
  override def powerRange = 100000L

  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): BigDecimal = {
    athlWertungen.map(_.noteD).sum * 1000L setScale(0, RoundingMode.HALF_UP)
  }
}
