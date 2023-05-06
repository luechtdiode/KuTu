package ch.seidel.kutu.domain

import java.time.temporal.ChronoUnit


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
  def apply(wettkampf: Wettkampf): Gleichstandsregel = this(wettkampf.programmId)
}

sealed trait Gleichstandsregel {
  def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): Long
  def toFormel: String
}

case object GleichstandsregelDefault extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): Long = 1
  override def toFormel: String = "Ohne"
}

case class GleichstandsregelList(regeln: List[Gleichstandsregel]) extends Gleichstandsregel {
  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): Long = {
    regeln
      .zipWithIndex
      .map(regel => (regel._1.factorize(currentWertung, athlWertungen), regel._2))
      .map(t => Math.floor(Math.pow(10, 10 - t._2)).toLong * t._1)
      .sum
  }
  override def toFormel: String = regeln.map(_.toFormel).mkString("/")
}

case class GleichstandsregelDisziplin(disziplinOrder: List[String]) extends Gleichstandsregel {
  override def toFormel: String = s"Disziplin${disziplinOrder.mkString("(", ",", ")")}"

  val reversedOrder = disziplinOrder.reverse
  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): Long = {
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
  override def toFormel: String = "JugendVorAlter"

  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): Long = {
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

  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): Long = {
    athlWertungen.map(_.noteE).max * 1000L toLong
  }
}

case object GleichstandsregelENoteSumme extends Gleichstandsregel {
  override def toFormel: String = "E-Note-Summe"

  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): Long = {
    athlWertungen.map(_.noteE).sum * 1000L toLong
  }
}

case object GleichstandsregelDNoteBest extends Gleichstandsregel {
  override def toFormel: String = "D-Note-Best"

  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): Long = {
    athlWertungen.map(_.noteD).max * 1000L toLong
  }
}

case object GleichstandsregelDNoteSumme extends Gleichstandsregel {
  override def toFormel: String = "D-Note-Summe"

  override def factorize(currentWertung: WertungView, athlWertungen: List[Resultat]): Long = {
    athlWertungen.map(_.noteD).sum * 1000L toLong
  }
}
