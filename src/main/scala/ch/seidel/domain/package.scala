package ch.seidel

package object domain {

  case class Verein(id: Long, name: String)

  case class Athlet(id: Long, name: String, vorname: String, gebdat: Option[java.sql.Date], verein: Option[Long])
  case class AthletView(id: Long, name: String, vorname: String, gebdat: Option[java.sql.Date], verein: Option[Verein])

  case class Disziplin(id: Long, name: String)

  trait Programm {
    val id: Long
    val name: String

    def withParent(parent: ProgrammView) = {
      ProgrammView(id, name, Some(parent))
    }

    def toView = {
      ProgrammView(id, name, None)
    }

    final def buildPathToParent(path: Seq[Programm]): ProgrammView = {
      if (path.nonEmpty) {
        path.head.buildPathToParent(path.tail).withParent(toView)
      }
      else {
        toView
      }
    }
  }

  case class ProgrammRaw(id: Long, name: String, parentId: Long) extends Programm
  case class ProgrammView(id: Long, name: String, parent: Option[ProgrammView]) extends Programm {
    def head: ProgrammView = parent match {
      case None    => this
      case Some(p) => p.head
    }
    def sameOrigin(other: ProgrammView) = head.equals(other.head)
    def toPath: String = parent match {
      case None    => this.name
      case Some(p) => p.toPath + " / " + name
    }
  }

  case class Wettkampf(id: Long, datum: java.sql.Date, titel: String, programmId: Long) {
    def toView(programm: ProgrammView) = {
      WettkampfView(id, datum, titel, programm)
    }
  }
  case class WettkampfView(id: Long, datum: java.sql.Date, titel: String, programm: ProgrammView)

  case class Wettkampfdisziplin(id: Long, programmId: Long, disziplinId: Long, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob])
  case class WettkampfdisziplinView(id: Long, programm: ProgrammView, disziplin: Disziplin, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob])

  case class Wertung(id: Long, athletId: Long, wettkampfdisziplinId: Long, wettkampfId: Long, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal)
  case class WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal)
}