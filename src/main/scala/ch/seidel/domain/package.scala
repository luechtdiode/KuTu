package ch.seidel

package object domain {
  trait DataObject {
    def easyprint: String = toString
  }
  case class Verein(id: Long, name: String) extends DataObject {
    override def easyprint = name
  }

  case class Athlet(id: Long, name: String, vorname: String, gebdat: Option[java.sql.Date], verein: Option[Long]) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {case Some(d) => f"$d%tY "; case _ => ""})
  }
  case class AthletView(id: Long, name: String, vorname: String, gebdat: Option[java.sql.Date], verein: Option[Verein]) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {case Some(d) => f"$d%tY "; case _ => " "}) + (verein match {case Some(v) => v.easyprint; case _ => ""})
  }

  case class AthletJahrgang(hg: String) extends DataObject {
    override def easyprint = hg
  }
  case class Disziplin(id: Long, name: String, ord: Int) extends DataObject {
    override def easyprint = name
  }

  trait Programm extends DataObject {
    override def easyprint = name

    val id: Long
    val name: String
    val aggregate: Int
    val ord: Int

    def withParent(parent: ProgrammView) = {
      ProgrammView(id, name, aggregate, Some(parent), ord)
    }

    def toView = {
      ProgrammView(id, name, aggregate, None, ord)
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

  case class ProgrammRaw(id: Long, name: String, aggregate: Int, parentId: Long, ord: Int) extends Programm
  case class ProgrammView(id: Long, name: String, aggregate: Int, parent: Option[ProgrammView], ord: Int) extends Programm  {
    override def easyprint = toPath

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

  case class Wettkampf(id: Long, datum: java.sql.Date, titel: String, programmId: Long) extends DataObject {
    override def easyprint = f"$titel am $datum%tdd.$datum%tMM.$datum%tYYYY"
    def toView(programm: ProgrammView) = {
      WettkampfView(id, datum, titel, programm)
    }
  }

  case class WettkampfView(id: Long, datum: java.sql.Date, titel: String, programm: ProgrammView) extends DataObject {
    override def easyprint = f"$titel im Programm ${programm.easyprint} am $datum%tdd.$datum%tMM.$datum%tYYYY"
  }

  case class Wettkampfdisziplin(id: Long, programmId: Long, disziplinId: Long, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob]) extends DataObject
  case class WettkampfdisziplinView(id: Long, programm: ProgrammView, disziplin: Disziplin, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob]) extends DataObject

  case class Resultat(noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal) extends DataObject {
    def + (r: Resultat) = Resultat(noteD + r.noteD, noteE + r.noteE, endnote + r.endnote)
    override def easyprint = {
      val d = f"${noteD}%5.3f"
      val a = f"${noteE}%5.3f"
      val e = f"${endnote}%5.2f"
      f"${d}%6s${a}%6s${e}%6s"
    }
  }
  case class Wertung(id: Long, athletId: Long, wettkampfdisziplinId: Long, wettkampfId: Long, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal) extends DataObject
  case class WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal) extends DataObject {
    lazy val resultat = Resultat(noteD, noteE, endnote)
    def + (r: Resultat) = resultat + r

    override def easyprint = {
      resultat.easyprint
    }
  }
}