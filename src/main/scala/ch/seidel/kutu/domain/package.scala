package ch.seidel.kutu

import java.net.URLEncoder
import java.nio.file.{Files, LinkOption, StandardOpenOption}
import java.time.{LocalDate, ZoneId}
import java.util.concurrent.TimeUnit

import ch.seidel.kutu.data.NameCodec
import org.apache.commons.codec.language.ColognePhonetic
import org.apache.commons.codec.language.bm._
import org.apache.commons.text.similarity.LevenshteinDistance

import scala.concurrent.duration.Duration

package object domain {
  implicit def dbl2Str(d: Double) = f"${d}%2.3f"
  implicit def str2bd(value: String): BigDecimal = {
    if (value != null) {
      val trimmed = value.trim()

      if (trimmed.length() < 1) {
          null
      } else {
          BigDecimal(trimmed) 
      }
    } else {
        null          
    }
  }
  implicit def str2dbl(value: String): Double = {
    if (value != null) {
      val trimmed = value.trim()

      if (trimmed.length() < 1) {
          0d
      } else {
          val bigd: BigDecimal = trimmed
          bigd.toDouble
      }
    } else {
        0d          
    }    
  }
  implicit def str2Int(value: String): Int = {
    if (value != null) {
      val trimmed = value.trim()

      if (trimmed.length() < 1) {
        0
      } else {
        Integer.valueOf(trimmed)
      }
    } else {
      0         
    }    
  }
  implicit def str2Long(value: String): Long = {
    if (value != null) {
      val trimmed = value.trim()

      if (trimmed.length() < 1) {
        0L
      } else {
        java.lang.Long.valueOf(trimmed)
      }
    } else {
      0L      
    }    
  }
  implicit def ld2SQLDate(ld: LocalDate): java.sql.Date = {
    if(ld==null) null else {
      val inst = ld.atStartOfDay(ZoneId.of("UTC"))
      new java.sql.Date(java.util.Date.from(inst.toInstant).getTime)
    }
  }
  implicit def sqlDate2ld(sd: java.sql.Date): LocalDate = {
    if(sd==null) null else {
      sd.toLocalDate//.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }
  }

  def toTimeFormat(millis: Long) = if (millis <= 0) "" else f"${new java.util.Date(millis)}%tT"
  def toDurationFormat(from: Long, to: Long) = { 
    val too = if (to <= 0 && from > 0) System.currentTimeMillis() else to
    if (too - from <= 0) "" else {
      val d = Duration(too - from, TimeUnit.MILLISECONDS)
      List((d.toDays, "d"), (d.toHours - d.toDays * 24, "h"), (d.toMinutes - d.toHours * 60, "m"), (d.toSeconds - d.toMinutes * 60, "s"))
      .filter(_._1 > 0)
      .map(p => s"${p._1}${p._2}")
      .mkString(", ")
    }
  }

//  implicit def dateOption2AthletJahrgang(gebdat: Option[Date]) = gebdat match {
//        case Some(d) => AthletJahrgang(extractYear.format(d))
//        case None    => AthletJahrgang("unbekannt")
//      }
  
  val encodeInvalidURIRegEx =  "[,&.*+?/^${}()|\\[\\]\\\\]".r
  def encodeURIComponent(uri: String) = encodeInvalidURIRegEx.replaceAllIn(uri, "_")
  
  def encodeURIParam(uri: String) = URLEncoder.encode(uri, "UTF-8")
    .replaceAll(" ", "%20")
    .replaceAll("\\+", "%20")
    .replaceAll("\\%21", "!")
    .replaceAll("\\%27", "'")
    .replaceAll("\\%28", "(")
    .replaceAll("\\%29", ")")
    .replaceAll("\\%7E", "~")
    
  trait DataObject {
    def easyprint: String = toString
    def capsulatedprint: String = {
      val ep = easyprint
      if (ep.matches(".*\\s,\\.;.*")) s""""$ep"""" else ep
    }
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
    def apply(): Athlet = Athlet(0, 0, "", "", "", None, "", "", "", None, activ = true)
    def apply(verein: Verein): Athlet = Athlet(0, 0, "M", "<Name>", "<Vorname>", None, "", "", "", Some(verein.id), activ = true)
    def apply(verein: Long): Athlet = Athlet(0, 0, "M", "<Name>", "<Vorname>", None, "", "", "", Some(verein), activ = true)
  }
  case class Athlet(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Long], activ: Boolean) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {case Some(d) => f"$d%tY "; case _ => ""})
  }
  case class AthletView(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Verein], activ: Boolean) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {case Some(d) => f"$d%tY "; case _ => " "}) + (verein match {case Some(v) => v.easyprint; case _ => ""})
    def toAthlet = Athlet(id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein.map(_.id), activ)
  }

  object Wertungsrichter {
    def apply(): Wertungsrichter = Wertungsrichter(0, 0, "", "", "", None, "", "", "", None, activ = true)
  }
  case class Wertungsrichter(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Long], activ: Boolean) extends DataObject {
    override def easyprint = "Wertungsrichter " + name
  }
  case class WertungsrichterView(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Verein], activ: Boolean) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {case Some(d) => f"$d%tY "; case _ => " "}) + (verein match {case Some(v) => v.easyprint; case _ => ""})
    def toWertungsrichter = Wertungsrichter(id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein.map(_.id), activ)
  }
  
  object Durchgang {
    def apply(): Durchgang = Durchgang(0, "nicht zugewiesen")
  }
  case class Durchgang(wettkampfId: Long, durchgang: String) extends DataObject {
    override def easyprint = durchgang
  }  
  case class Durchgangstation(wettkampfId: Long, durchgang: String, d_Wertungsrichter1: Option[Long], e_Wertungsrichter1: Option[Long], d_Wertungsrichter2: Option[Long], e_Wertungsrichter2: Option[Long], geraet: Disziplin) extends DataObject {
    override def easyprint = toString
  }
  case class DurchgangstationView(wettkampfId: Long, durchgang: String, d_Wertungsrichter1: Option[WertungsrichterView], e_Wertungsrichter1: Option[WertungsrichterView], d_Wertungsrichter2: Option[WertungsrichterView], e_Wertungsrichter2: Option[WertungsrichterView], geraet: Disziplin) extends DataObject {
    override def easyprint = toString
    def toDurchgangstation = Durchgangstation(wettkampfId, durchgang, d_Wertungsrichter1.map(_.id), e_Wertungsrichter1.map(_.id), d_Wertungsrichter2.map(_.id), e_Wertungsrichter2.map(_.id), geraet)
  }
  
  object AthletJahrgang {
    def apply(gebdat: Option[java.sql.Date]): AthletJahrgang = gebdat match {
      case Some(d) => AthletJahrgang(f"$d%tY")
      case None    => AthletJahrgang("unbekannt")
    }
  }
  
  case class AthletJahrgang(jahrgang: String) extends DataObject {
    override def easyprint = "Jahrgang " + jahrgang
  }

  case class WettkampfJahr(wettkampfjahr: String) extends DataObject {
    override def easyprint = "Wettkampf-Jahr " + wettkampfjahr
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

//  object Wettkampf {
//    def apply(id: Long, datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal): Wettkampf = 
//      Wettkampf(id, datum, titel, programmId, auszeichnung, auszeichnungendnote, if(id == 0) Some(UUID.randomUUID().toString()) else None)
//    def apply(id: Long, datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, uuid: String): Wettkampf = 
//      if(uuid != null) Wettkampf(id, datum, titel, programmId, auszeichnung, auszeichnungendnote, Some(uuid))
//      else apply(id, datum, titel, programmId, auszeichnung, auszeichnungendnote)
//  }
  case class Wettkampf(id: Long, uuid: Option[String], datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal) extends DataObject {
    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"
    
    def toView(programm: ProgrammView) = {
      WettkampfView(id, uuid, datum, titel, programm, auszeichnung, auszeichnungendnote)
    }
    
    def prepareFilePath(homedir: String) = {
      val dir = new java.io.File(homedir + "/" + easyprint.replace(" ", "_"))
      if(!dir.exists) {
        dir.mkdirs
      }
      dir
    }
    
    def filePath(homedir: String, origin: String) = new java.io.File(prepareFilePath(homedir), ".at." + origin).toPath
    
    def saveSecret(homedir: String, origin: String, secret: String) {
      val path = filePath(homedir, origin)
      val fos = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)
      try {
        fos.write(secret.getBytes("utf-8"))
        fos.flush
      } finally {
        fos.close
      }
      val os = System.getProperty("os.name").toLowerCase
      if (os.indexOf("win") > -1) {
        Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)
      }
    }
    
    def readSecret(homedir: String, origin: String): Option[String] = {
      val path = filePath(homedir, origin)
      if (path.toFile.exists) {
        Some(new String(Files.readAllBytes(path), "utf-8"))
      }
      else {
        None
      }
    }
    def removeSecret(homedir: String, origin: String) {
      val atFile = filePath(homedir, origin).toFile
      if (atFile.exists) {
        atFile.delete()
      }
    }
    def hasSecred(homedir: String, origin: String): Boolean = readSecret(homedir, origin) match {case Some(_) => true case None => false }
  }

//  object WettkampfView {
//    def apply(id: Long, datum: java.sql.Date, titel: String, programm: ProgrammView, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal): WettkampfView = 
//      WettkampfView(id, datum, titel, programm, auszeichnung, auszeichnungendnote, if(id == 0) Some(UUID.randomUUID().toString()) else None)
//    def apply(id: Long, datum: java.sql.Date, titel: String, programm: ProgrammView, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, uuid: String): WettkampfView = 
//      if(uuid != null) WettkampfView(id, datum, titel, programm, auszeichnung, auszeichnungendnote, Some(uuid))
//      else apply(id, datum, titel, programm, auszeichnung, auszeichnungendnote)
//  }
  case class WettkampfView(id: Long, uuid: Option[String], datum: java.sql.Date, titel: String, programm: ProgrammView, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal) extends DataObject {
    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"
    def toWettkampf = Wettkampf(id, uuid, datum, titel, programm.id, auszeichnung, auszeichnungendnote)
  }

  case class Wettkampfdisziplin(id: Long, programmId: Long, disziplinId: Long, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob], notenfaktor: scala.math.BigDecimal, masculin: Int, feminim: Int, ord: Int) extends DataObject {
    override def easyprint = f"$disziplinId%02d: $kurzbeschreibung"
  }
  case class WettkampfdisziplinView(id: Long, programm: ProgrammView, disziplin: Disziplin, kurzbeschreibung: String, detailbeschreibung: Option[Array[Byte]], notenSpez: NotenModus, masculin: Int, feminim: Int, ord: Int) extends DataObject {
    override def easyprint = disziplin.name
    def toWettkampdisziplin = Wettkampfdisziplin(id, programm.id, disziplin.id, kurzbeschreibung, None, notenSpez.calcEndnote(0, 1), masculin, feminim, ord)
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
  case class Wertung(id: Long, athletId: Long, wettkampfdisziplinId: Long, wettkampfId: Long, wettkampfUUID: String, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal, riege: Option[String], riege2: Option[String]) extends DataObject {
    lazy val resultat = Resultat(noteD, noteE, endnote)
    def updatedWertung(valuesFrom: Wertung) = copy(noteD = valuesFrom.noteD, noteE = valuesFrom.noteE, endnote = valuesFrom.endnote)
  }
  case class WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal, riege: Option[String], riege2: Option[String]) extends DataObject {
    lazy val resultat = Resultat(noteD, noteE, endnote)
    def + (r: Resultat) = resultat + r
    def toWertung = Wertung(id, athlet.id, wettkampfdisziplin.id, wettkampf.id, wettkampf.uuid.getOrElse(""), noteD, noteE, endnote, riege, riege2)
    def toWertung(riege: String) = Wertung(id, athlet.id, wettkampfdisziplin.id, wettkampf.id, wettkampf.uuid.getOrElse(""), noteD, noteE, endnote, Some(riege), riege2)
    def updatedWertung(valuesFrom: Wertung) = copy(noteD = valuesFrom.noteD, noteE = valuesFrom.noteE, endnote = valuesFrom.endnote)
    def showInScoreList = {
      (endnote > 0) || (athlet.geschlecht match {
        case "M" => wettkampfdisziplin.masculin > 0
        case "W" => wettkampfdisziplin.feminim > 0
        case _ => endnote > 0
      })
    }
    override def easyprint = {
      resultat.easyprint
    }
  }

  sealed trait DataRow {}
  case class LeafRow(title: String, sum: Resultat, rang: Resultat, auszeichnung: Boolean) extends DataRow
  case class GroupRow(athlet: AthletView, resultate: IndexedSeq[LeafRow], sum: Resultat, rang: Resultat, auszeichnung: Boolean) extends DataRow {
    lazy val withDNotes = resultate.exists(w => w.sum.noteD > 0)
    lazy val divider = if(withDNotes || resultate.isEmpty) 1 else resultate.count{r => r.sum.endnote > 0}
  }

  sealed trait NotenModus /*with AutoFillTextBoxFactory.ItemComparator[String]*/ {
    val isDNoteUsed: Boolean
    def selectableItems: Option[List[String]] = None
    def validated(dnote: Double, enote: Double): (Double, Double)
    def calcEndnote(dnote: Double, enote: Double): Double
    def verifiedAndCalculatedWertung(wertung: Wertung) = {
      val (d, e) = validated(wertung.noteD.doubleValue(), wertung.noteE.doubleValue())
      wertung.copy(noteD = d, noteE = e, endnote = calcEndnote(d, e))
    }
    def toString(value: Double): String = value
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
    override def validated(dnote: Double, enote: Double): (Double, Double) = (0, enote)
    override def calcEndnote(dnote: Double, enote: Double) = enote * punktgewicht
    override def selectableItems: Option[List[String]] = Some(punktemapping.keys.toList.sortBy(punktemapping))
  }
  case object KuTuWettkampf extends NotenModus {
    override val isDNoteUsed = true
    //override def fromString(input: String) = super.fromString(input)
    override def validated(dnote: Double, enote: Double): (Double, Double) =
      ( BigDecimal(dnote).setScale(3, BigDecimal.RoundingMode.FLOOR).max(0).min(30).toDouble,
        BigDecimal(enote).setScale(3, BigDecimal.RoundingMode.FLOOR).max(0).min(30).toDouble)
    override def calcEndnote(dnote: Double, enote: Double) =
      BigDecimal(dnote + enote).setScale(3, BigDecimal.RoundingMode.FLOOR).max(0).min(30).toDouble
  }
  case object GeTuWettkampf extends NotenModus {
    override val isDNoteUsed = false
    //override def fromString(input: String) = super.fromString(input)
    override def validated(dnote: Double, enote: Double): (Double, Double) =
      ( BigDecimal(dnote).setScale(3, BigDecimal.RoundingMode.FLOOR).max(0).min(30).toDouble,
        BigDecimal(enote).setScale(3, BigDecimal.RoundingMode.FLOOR).max(0).min(30).toDouble)
    override def calcEndnote(dnote: Double, enote: Double) =
      BigDecimal(enote).setScale(2, BigDecimal.RoundingMode.FLOOR).max(0).min(10).toDouble
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
      Seq(NameCodec.encode(name), colenc.encode(name).mkString(""))

    def similarFactor(name1: String, name2: String, threshold: Int = 80) = {
      val diff = LevenshteinDistance.getDefaultInstance.apply(name1, name2)
      val diffproz = 100 * diff / name1.length()
      val similar = 100 - diffproz
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
    def swappednames = MatchCode(id, vorname, name, jahrgang, verein)
  }

  case class Kandidat(wettkampfTitel: String, geschlecht: String, programm: String, id: Long,
                      name: String, vorname: String, jahrgang: String, verein: String, einteilung: Option[Riege], einteilung2: Option[Riege], diszipline: Seq[Disziplin], diszipline2: Seq[Disziplin], wertungen: Seq[WertungView])
  case class GeraeteRiege(wettkampfTitel: String, durchgang: Option[String], halt: Int, disziplin: Option[Disziplin], kandidaten: Seq[Kandidat], erfasst: Boolean) {
    private val hash: Long = {
      Seq(wettkampfTitel,
      durchgang,
      halt, disziplin).hashCode()
    }
    
    def softEquals(other: GeraeteRiege) = {
      hash == other.hash
    }
  }

  sealed trait SexDivideRule {
    val name: String
    override def toString = name
  }
  case object GemischteRiegen extends SexDivideRule {
    override val name = "gemischte Geräteriegen"
  }
  case object GemischterDurchgang extends SexDivideRule {
    override val name = "gemischter Durchgang"
  }
  case object GetrennteDurchgaenge extends SexDivideRule {
    override val name = "getrennte Durchgänge"
  }
}