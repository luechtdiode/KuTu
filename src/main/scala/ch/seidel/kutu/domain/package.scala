package ch.seidel.kutu

import scalafx.util.converter.DoubleStringConverter
import java.io.ObjectInputStream
import scalafx.collections.ObservableBuffer
import java.time.LocalDate
import java.time.ZoneId
import scalafx.util.converter.IntStringConverter
import scalafx.util.converter.LongStringConverter
import java.text.SimpleDateFormat
import org.apache.commons.codec.language.bm._
import org.apache.commons.lang.StringUtils
import org.apache.commons.codec.language.ColognePhonetic
import java.util.TimeZone

package object domain {
  implicit def dbl2Str(d: Double) = f"${d}%2.3f"
  implicit def str2dbl(d: String) = new DoubleStringConverter().fromString(d)
  implicit def str2Int(d: String) = new IntStringConverter().fromString(d)
  implicit def str2Long(d: String) = new LongStringConverter().fromString(d)
  implicit def ld2SQLDate(ld: LocalDate): java.sql.Date = {
    if(ld==null) return null else {
      val inst = ld.atStartOfDay(ZoneId.of("UTC"))
      new java.sql.Date(java.util.Date.from(inst.toInstant()).getTime())
    }
  }
  implicit def sqlDate2ld(sd: java.sql.Date): LocalDate = {
    if(sd==null) return null else {
      sd.toLocalDate()//.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }
  }

//  implicit def dateOption2AthletJahrgang(gebdat: Option[Date]) = gebdat match {
//        case Some(d) => AthletJahrgang(extractYear.format(d))
//        case None    => AthletJahrgang("unbekannt")
//      }

  trait DataObject {
    def easyprint: String = toString
  }

  case class NullObject(caption: String) extends DataObject {
    override def easyprint = caption
  }

  case class RiegeRaw(wettkampfId: Long, r: String, durchgang: Option[String], start: Option[Long]) extends DataObject {
    override def easyprint = r
  }

  case class Riege(r: String, durchgang: Option[String], start: Option[Disziplin]) extends DataObject {
    override def easyprint = r
  }

  case class TurnerGeschlecht(geschlecht: String) extends DataObject {
    override def easyprint = geschlecht.toLowerCase() match {
      case "m" => "Turner"
      case "w" => "Turnerinnen"
      case "f" => "Turnerinnen"
      case _ => "Turner"
    }
  }
  case class Verein(id: Long, name: String, verband: Option[String]) extends DataObject {
    override def easyprint = name
    override def toString = name
  }
  case class Verband(name: String) extends DataObject {
    override def easyprint = name
    override def toString = name
  }

  object Athlet {
    def apply(): Athlet = Athlet(0, 0, "", "", "", None, "", "", "", None, true)
    def apply(verein: Verein): Athlet = Athlet(0, 0, "M", "<Name>", "<Vorname>", None, "", "", "", Some(verein.id), true)
  }
  case class Athlet(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Long], activ: Boolean) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {case Some(d) => f"$d%tY "; case _ => ""})
  }
  case class AthletView(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Verein], activ: Boolean) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {case Some(d) => f"$d%tY "; case _ => " "}) + (verein match {case Some(v) => v.easyprint; case _ => ""})
    def toAthlet = Athlet(id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein.map(_.id), activ)
  }

  object AthletJahrgang {
    def apply(gebdat: Option[java.sql.Date]): AthletJahrgang = gebdat match {
        case Some(d) => AthletJahrgang(f"$d%tY")
        case None    => AthletJahrgang("unbekannt")
      }
  }
  case class AthletJahrgang(hg: String) extends DataObject {
    override def easyprint = "Jahrgang " + hg
  }

  case class WettkampfJahr(hg: String) extends DataObject {
    override def easyprint = "Wettkampf-Jahr " + hg
  }
  case class Disziplin(id: Long, name: String) extends DataObject {
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

    def wettkampfprogramm: ProgrammView = if(aggregator == this) this else head

    def aggregatorHead: ProgrammView = parent match {
      case Some(p) if(aggregate != 0) => p.aggregatorHead
      case _       => this
    }
    def aggregatorParent: ProgrammView = parent match {
      case Some(p) if(aggregate != 0) => p.parent.getOrElse(this)
      case _       => this
    }
    def aggregator: ProgrammView = parent match {
      case Some(p) if(aggregate != 0) => p
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
    override def toString = toPath
  }

  case class Wettkampf(id: Long, datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal) extends DataObject {
    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"
    def toView(programm: ProgrammView) = {
      WettkampfView(id, datum, titel, programm, auszeichnung, auszeichnungendnote)
    }
  }

  case class WettkampfView(id: Long, datum: java.sql.Date, titel: String, programm: ProgrammView, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal) extends DataObject {
    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"
    def toWettkampf = Wettkampf(id, datum, titel, programm.id, auszeichnung, auszeichnungendnote)
  }

  case class Wettkampfdisziplin(id: Long, programmId: Long, disziplinId: Long, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob], notenfaktor: scala.math.BigDecimal, ord: Int, masculin: Int, feminim: Int) extends DataObject {
    override def easyprint = f"$disziplinId%02d: $kurzbeschreibung"
  }
  case class WettkampfdisziplinView(id: Long, programm: ProgrammView, disziplin: Disziplin, kurzbeschreibung: String, detailbeschreibung: Option[Array[Byte]], notenSpez: NotenModus, ord: Int, masculin: Int, feminim: Int) extends DataObject {
    override def easyprint = disziplin.name
    def toWettkampdisziplin = Wettkampfdisziplin(id, programm.id, disziplin.id, kurzbeschreibung, None, notenSpez.calcEndnote(0, 1), ord, masculin, feminim)
  }

  case class Resultat(noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal) extends DataObject {
    def + (r: Resultat) = Resultat(noteD + r.noteD, noteE + r.noteE, endnote + r.endnote)
    def / (cnt: Int) = Resultat(noteD / cnt, noteE / cnt, endnote / cnt)
    def * (cnt: Long) = Resultat(noteD * cnt, noteE * cnt, endnote * cnt)
    lazy val formattedD = if(noteD > 0) f"${noteD}%4.2f" else ""
    lazy val formattedE = if(noteE > 0) f"${noteE}%4.2f" else ""
    lazy val formattedEnd = if(endnote > 0) f"${endnote}%6.2f" else ""
    override def easyprint = f"${formattedD}%6s${formattedE}%6s${formattedEnd}%6s"
  }
  case class Wertung(id: Long, athletId: Long, wettkampfdisziplinId: Long, wettkampfId: Long, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal, riege: Option[String], riege2: Option[String]) extends DataObject
  case class WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal, riege: Option[String], riege2: Option[String]) extends DataObject {
    lazy val resultat = Resultat(noteD, noteE, endnote)
    def + (r: Resultat) = resultat + r
    def toWertung = Wertung(id, athlet.id, wettkampfdisziplin.id, wettkampf.id, noteD, noteE, endnote, riege, riege2)
    def toWertung(riege: String) = Wertung(id, athlet.id, wettkampfdisziplin.id, wettkampf.id, noteD, noteE, endnote, Some(riege), riege2)
    override def easyprint = {
      resultat.easyprint
    }
  }

  sealed trait DataRow {}
  case class LeafRow(title: String, sum: Resultat, rang: Resultat, auszeichnung: Boolean) extends DataRow
  case class GroupRow(athlet: AthletView, resultate: IndexedSeq[LeafRow], sum: Resultat, rang: Resultat, auszeichnung: Boolean) extends DataRow {
    lazy val withDNotes = resultate.filter(w => w.sum.noteD > 0).nonEmpty
    lazy val divider = if(withDNotes || resultate.isEmpty) 1 else resultate.filter{r => r.sum.endnote > 0}.size
  }

  sealed trait NotenModus extends DoubleStringConverter /*with AutoFillTextBoxFactory.ItemComparator[String]*/ {
    val isDNoteUsed: Boolean
    def selectableItems: Option[List[String]] = None
    def calcEndnote(dnote: Double, enote: Double): Double
    override def toString(value: Double): String = value
    /*override*/ def shouldSuggest(item: String, query: String): Boolean = false
  }

  case class Athletiktest(punktemapping: Map[String,Double], punktgewicht: Double) extends NotenModus {
    override val isDNoteUsed = false
//    override def shouldSuggest(item: String, query: String): Boolean = {
//      findLikes(query).find(x => x.equalsIgnoreCase(item)).size > 0
//    }
//    def findnearest(value: Double): Double = {
//      val sorted = punktemapping.values.toList.sorted
//      if(value.equals(0.0d)) value else
//        sorted.find { x => x >= value } match {
//        case Some(v) => v
//        case None => sorted.last
//      }
//    }
//    def findLikes(value: String) = {
//      val lv = value.toLowerCase()
//      def extractDigits(lv: String) = lv.filter(c => c.isDigit || c == '.')
//      lazy val lvv = extractDigits(lv)
//      val orderedKeys = punktemapping.keys.toList.sortBy(punktemapping).map(x => x.toLowerCase())
//      orderedKeys.filter(v => v.contains(lv) || extractDigits(v).equals(lvv))
//    }
//    def mapToDouble(input: String) = try {findnearest(super.fromString(input))} catch {case _: Throwable => 0d}
//    def findLike(value: String): String = {
//      val lv = value.toLowerCase()
//      def extractDigits(lv: String) = lv.filter(c => c.isDigit || c == '.')
//      lazy val lvv = extractDigits(lv)
//      val valuedKeys = punktemapping.keys.toList.sortBy(punktemapping)
//      if(valuedKeys.contains(value)) {
//        return value
//      }
//      val lvd = mapToDouble(value)
//      if(lvd > 0d && punktemapping.values.exists { v => v == lvd }) {
//        return value
//      }
//      val orderedKeys = punktemapping.keys.toList.sortBy(punktemapping).map(_.toLowerCase())
//      orderedKeys.find(v => v.equals(lv)).getOrElse {
//    	  orderedKeys.find(v => extractDigits(v).equals(lvv)).getOrElse {
//          orderedKeys.find(v => v.startsWith(lv)).getOrElse {
//            orderedKeys.find(v => v.contains(lv)).getOrElse {
//              value
//            }
//          }
//        }
//      }
//    }
//    override def toString(value: Double): String = punktemapping.find(p => p._2 == value).map(_._1).getOrElse(value)
    //override def fromString(input: String) = punktemapping.getOrElse(findLike(input), mapToDouble(input))
    override def calcEndnote(dnote: Double, enote: Double) = enote * punktgewicht
    override def selectableItems: Option[List[String]] = Some(punktemapping.keys.toList.sortBy(punktemapping))
  }
  case object KuTuWettkampf extends NotenModus {
    override val isDNoteUsed = true
    //override def fromString(input: String) = super.fromString(input)
    override def calcEndnote(dnote: Double, enote: Double) = dnote + enote
  }
  case object GeTuWettkampf extends NotenModus {
    override val isDNoteUsed = false
    //override def fromString(input: String) = super.fromString(input)
    override def calcEndnote(dnote: Double, enote: Double) = enote
  }

  object MatchCode {
    val bmenc = new BeiderMorseEncoder()
    bmenc.setRuleType(RuleType.EXACT)
    bmenc.setMaxPhonemes(5)
    val bmenc2 = new BeiderMorseEncoder()
    bmenc2.setRuleType(RuleType.EXACT)
    bmenc2.setMaxPhonemes(5)
    bmenc2.setNameType(NameType.SEPHARDIC)
    val bmenc3 = new BeiderMorseEncoder()
    bmenc3.setRuleType(RuleType.EXACT)
    bmenc3.setMaxPhonemes(5)
    bmenc3.setNameType(NameType.ASHKENAZI)
    val colenc = new ColognePhonetic()
    def encArrToList(enc: String) = enc.split("-").flatMap(_.split("\\|"))
    def encode(name: String): Seq[String] =
      encArrToList(bmenc.encode(name)) ++
      encArrToList(bmenc2.encode(name)) ++
      encArrToList(bmenc3.encode(name)) ++
      Seq(colenc.encode(name).mkString(""))

    def similarFactor(name1: String, name2: String) = {
      val diff = StringUtils.getLevenshteinDistance(name1, name2)
      val diffproz = 100 * diff / name1.length()
      val similar = 100 - diffproz
      val threshold = 80 //%
      if(similar >= threshold) {
        similar
      }
      else {
        0
      }
    }
  }

  case class MatchCode(id: Long, name: String, vorname: String, jahrgang: String, verein: Long) {
    import MatchCode._
    val encodedNamen = encode(name)
    val encodedVorNamen = encode(vorname)
  }

  case class Kandidat(wettkampfTitel: String, geschlecht: String, programm: String, id: Long,
                      name: String, vorname: String, jahrgang: String, verein: String, einteilung: Option[Riege], einteilung2: Option[Riege], diszipline: Seq[String])
  case class GeraeteRiege(wettkampfTitel: String, durchgang: Option[String], halt: Int, disziplin: Option[Disziplin], kandidaten: Seq[Kandidat])

}