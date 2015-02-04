package ch.seidel

import scalafx.util.converter.DoubleStringConverter
import java.io.ObjectInputStream
import scalafx.collections.ObservableBuffer
package object domain {
  implicit def dbl2Str(d: Double) = f"${d}%2.3f"
  implicit def str2dbl(d: String) = d.toString()

  trait DataObject {
    def easyprint: String = toString
  }

  case class Verein(id: Long, name: String) extends DataObject {
    override def easyprint = name
  }

  object Athlet {
    def apply(verein: Verein): Athlet = Athlet(0, 0, "M", "<Name>", "<Vorname>", None, "", "", "", Some(verein.id))
  }
  case class Athlet(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Long]) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {case Some(d) => f"$d%tY "; case _ => ""})
  }
  case class AthletView(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Verein]) extends DataObject {
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
    val alterVon: Int
    val alterBis: Int

    def withParent(parent: ProgrammView) = {
      ProgrammView(id, name, aggregate, Some(parent), ord, alterVon, alterBis)
    }

    def toView = {
      ProgrammView(id, name, aggregate, None, ord, alterVon, alterBis)
    }
  }

  case class ProgrammRaw(id: Long, name: String, aggregate: Int, parentId: Long, ord: Int, alterVon: Int, alterBis: Int) extends Programm
  case class ProgrammView(id: Long, name: String, aggregate: Int, parent: Option[ProgrammView], ord: Int, alterVon: Int, alterBis: Int) extends Programm  {
    //override def easyprint = toPath

    def head: ProgrammView = parent match {
      case None    => this
      case Some(p) => p.head
    }
    def aggregatorHead: ProgrammView = parent match {
      case Some(p) if(aggregate != 0) => p.aggregatorHead
      case _       => this
    }
    def aggregatorParent: ProgrammView = parent match {
      case Some(p) if(aggregate != 0) => p.parent.getOrElse(this)
      case _       => this
    }
    def aggregatorSubHead: ProgrammView = parent match {
      case Some(p) if(aggregate != 0 && p.aggregate != 0) => p.aggregatorSubHead
      case Some(p) if(aggregate != 0 && p.aggregate == 0) => this
      case _       => this
    }
    def sameOrigin(other: ProgrammView) = head.equals(other.head)
    def toPath: String = parent match {
      case None    => this.name
      case Some(p) => p.toPath + " / " + name
    }
  }

  case class Wettkampf(id: Long, datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int) extends DataObject {
    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"
    def toView(programm: ProgrammView) = {
      WettkampfView(id, datum, titel, programm, auszeichnung)
    }
  }

  case class WettkampfView(id: Long, datum: java.sql.Date, titel: String, programm: ProgrammView, auszeichnung: Int) extends DataObject {
    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"
  }

  case class Wettkampfdisziplin(id: Long, programmId: Long, disziplinId: Long, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob]) extends DataObject
  case class WettkampfdisziplinView(id: Long, programm: ProgrammView, disziplin: Disziplin, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob]) extends DataObject {
    lazy val notenSpez = {
      detailbeschreibung match {
        case blob: java.sql.Blob => new ObjectInputStream(blob.getBinaryStream).readObject.asInstanceOf[NotenModus]
        case _ => (if(programm.aggregatorHead.id == 1) Athletiktest(Map("<3cm"-> 1d, ">=3cm" -> 10d), 3d) else Wettkampf).asInstanceOf[NotenModus]
      }
    }
  }

  case class Resultat(noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal) extends DataObject {
    def + (r: Resultat) = Resultat(noteD + r.noteD, noteE + r.noteE, endnote + r.endnote)
    lazy val formattedD = if(noteD > 0) f"${noteD}%5.3f" else ""
    lazy val formattedE = if(noteE > 0) f"${noteE}%5.3f" else ""
    lazy val formattedEnd = if(endnote > 0) f"${endnote}%5.2f" else ""
    override def easyprint = f"${formattedD}%6s${formattedE}%6s${formattedEnd}%6s"
  }
  case class Wertung(id: Long, athletId: Long, wettkampfdisziplinId: Long, wettkampfId: Long, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal) extends DataObject
  case class WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal) extends DataObject {
    lazy val resultat = Resultat(noteD, noteE, endnote)
    def + (r: Resultat) = resultat + r

    override def easyprint = {
      resultat.easyprint
    }
  }

  sealed trait DataRow {}
  case class LeafRow(title: String, sum: Resultat, rang: Resultat) extends DataRow
  case class GroupRow(athlet: AthletView, resultate: IndexedSeq[LeafRow], sum: Resultat, rang: Resultat) extends DataRow

  sealed trait NotenModus extends DoubleStringConverter {
    def map(input: String): Double
    val isDNoteUsed: Boolean
    def selectableItems: Option[List[String]] = None
    def calcEndnote(dnote: Double, enote: Double): Double
    override def toString(value: Double): String = value
  }
  case class Athletiktest(punktemapping: Map[String,Double], punktgewicht: Double) extends NotenModus {
    override val isDNoteUsed = false
    override def map(input: String) = punktemapping.getOrElse(input, fromString(input))
    override def calcEndnote(dnote: Double, enote: Double) = enote * punktgewicht
    override def selectableItems: Option[List[String]] = Some(punktemapping.keys.toList)
  }
  case object Wettkampf extends NotenModus {
    override val isDNoteUsed = true
    override def map(input: String) = fromString(input)
    override def calcEndnote(dnote: Double, enote: Double) = dnote + enote
  }

}