package ch.seidel.kutu.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit


object Gleichstandsregel {
  private val getu = "Disziplin(Schaukelringe,Sprung,Reck)"
  private val kutu = "E-Note-Summe/D-Note-Summe/JugendVorAlter"
  val predefined = Map(
    ("Ohne - Punktgleichstand => gleicher Rang" -> "")
    , ("GeTu" -> getu)
    , ("KuTu" -> kutu)
    , ("Individuell" -> "")
  )
  val disziplinPattern = "^Disciplin\\((.+)\\)$".r

  def apply(formel: String): Gleichstandsregel = {
    val regeln = formel.split("/").toList
    val mappedFactorizers: List[Gleichstandsregel] = regeln.flatMap {
      case disziplinPattern(dl) => Some(GleichstandsregelDisziplin(dl.split(",").toList).asInstanceOf[Gleichstandsregel])
      case "E-Note-Summe" => Some(GleichstandsregelENoteSumme.asInstanceOf[Gleichstandsregel])
      case "E-Note-Best" => Some(GleichstandsregelENoteBest.asInstanceOf[Gleichstandsregel])
      case "D-Note-Summe" => Some(GleichstandsregelDNoteSumme.asInstanceOf[Gleichstandsregel])
      case "D-Note-Best" => Some(GleichstandsregelDNoteBest.asInstanceOf[Gleichstandsregel])
      case "JugendVorAlter" => Some(GleichstandsregelJugendVorAlter.asInstanceOf[Gleichstandsregel])
      case "Ohne" => Some(GleichstandsregelDefault.asInstanceOf[Gleichstandsregel])
      case _ => None
    }
    if (mappedFactorizers.isEmpty) {
      GleichstandsregelList(List(GleichstandsregelDefault))
    } else {
      GleichstandsregelList(mappedFactorizers)
    }
  }
}

sealed trait Gleichstandsregel {
  def factorize(currentWertung: WertungView, athlWertungen: List[WertungView]): Long
}

case object GleichstandsregelDefault extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[WertungView]): Long = 1
}

case class GleichstandsregelList(regeln: List[Gleichstandsregel]) extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[WertungView]): Long = {
    regeln
      .zipWithIndex
      .map(regel => (regel._1.factorize(currentWertung, athlWertungen), regel._2))
      .map(t => Math.floor(Math.pow(10, 10 - t._2)).toLong * t._1)
      .sum
  }
}

case class GleichstandsregelDisziplin(disziplinOrder: List[String]) extends Gleichstandsregel {
  val reversedOrder = disziplinOrder.reverse
  override def factorize(currentWertung: WertungView, athlWertungen: List[WertungView]): Long = {
    val idx = 1 + reversedOrder.indexOf(currentWertung.wettkampfdisziplin.disziplin.name)
    val ret = if (idx <= 0) {
      1L
    }
    else {
      Math.floor(Math.pow(100, idx)).toLong// idx.toLong
    }
    ret
  }
}

case object GleichstandsregelJugendVorAlter extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[WertungView]): Long = {
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
  override def factorize(currentWertung: WertungView, athlWertungen: List[WertungView]): Long = {
    athlWertungen.map(_.resultat.noteE).max * 1000L toLong
  }
}

case object GleichstandsregelENoteSumme extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[WertungView]): Long = {
    athlWertungen.map(_.resultat.noteE).sum * 1000L toLong
  }
}

case object GleichstandsregelDNoteBest extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[WertungView]): Long = {
    athlWertungen.map(_.resultat.noteD).max * 1000L toLong
  }
}

case object GleichstandsregelDNoteSumme extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[WertungView]): Long = {
    athlWertungen.map(_.resultat.noteD).sum * 1000L toLong
  }
}
