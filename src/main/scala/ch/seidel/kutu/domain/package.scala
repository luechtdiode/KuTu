package ch.seidel.kutu

import ch.seidel.kutu.data.{NameCodec, Surname}
import org.apache.commons.codec.language.ColognePhonetic
import org.apache.commons.codec.language.bm._
import org.apache.commons.text.similarity.LevenshteinDistance

import java.io.File
import java.net.URLEncoder
import java.nio.file.{Files, LinkOption, Path, StandardOpenOption}
import java.sql.{Date, Timestamp}
import java.text.{ParseException, SimpleDateFormat}
import java.time._
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.math.BigDecimal.RoundingMode

package object domain {
  implicit def dbl2Str(d: Double): String = f"${d}%2.3f"

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
    if (ld == null) null else {
      val inst = ld.atStartOfDay(ZoneId.of("UTC"))
      new java.sql.Date(java.util.Date.from(inst.toInstant).getTime)
    }
  }

  implicit def sqlDate2ld(sd: java.sql.Date): LocalDate = {
    if (sd == null) null else {
      sd.toLocalDate //.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }
  }

  def isNumeric(c: String): Boolean = {
    try {
      Integer.parseInt(c)
      true
    } catch {
      case _: NumberFormatException => false
    }
  }

  val sdf = new SimpleDateFormat("dd.MM.yyyy")
  val sdfShort = new SimpleDateFormat("dd.MM.yy")
  val sdfExported = new SimpleDateFormat("yyyy-MM-dd")
  val sdfYear = new SimpleDateFormat("yyyy")

  def dateToExportedStr(date: Date) = {
    sdfExported.format(date)
  }

  def str2SQLDate(date: String) = {
    if (date == null) null else try {
      new java.sql.Date(sdf.parse(date).getTime)
    }
    catch {
      case _: ParseException => try {
        new java.sql.Date(sdfExported.parse(date).getTime)
      }
      catch {
        case _: ParseException => try {
          new java.sql.Date(sdfShort.parse(date).getTime)
        } catch {
          case _: Exception => {
            val time = try {
              str2Long(date)
            } catch {
              case _: NumberFormatException =>
                sdf.parse(date.split("T")(0)).getTime()
            }
            new java.sql.Date(time)
          }
        }
      }
    }
  }

  def toTimeFormat(millis: Long): String = if (millis <= 0) "" else f"${new java.util.Date(millis)}%tT"

  def toDurationFormat(from: Long, to: Long): String = {
    val too = if (to <= 0 && from > 0) System.currentTimeMillis() else to
    if (too - from <= 0) "" else {
      toDurationFormat(too - from)
    }
  }

  def toDurationFormat(duration: Long): String = {
    if (duration <= 0) "" else {
      val d = Duration(duration, TimeUnit.MILLISECONDS)
      List((d.toDays, "d"), (d.toHours - d.toDays * 24, "h"), (d.toMinutes - d.toHours * 60, "m"), (d.toSeconds - d.toMinutes * 60, "s"))
        .filter(_._1 > 0)
        .map(p => s"${p._1}${p._2}")
        .mkString(", ")
    }
  }

  def toShortDurationFormat(duration: Long): String = {
    val d = Duration(duration, TimeUnit.MILLISECONDS)
    List(f"${d.toHours}%02d", f"${d.toMinutes - d.toHours * 60}%02d", f"${d.toSeconds - d.toMinutes * 60}%02d")
      .mkString(":")
  }

  //  implicit def dateOption2AthletJahrgang(gebdat: Option[Date]) = gebdat match {
  //        case Some(d) => AthletJahrgang(extractYear.format(d))
  //        case None    => AthletJahrgang("unbekannt")
  //      }

  val encodeInvalidURIRegEx = "[,&.*+?/^${}()|\\[\\]\\\\]".r

  def encodeURIComponent(uri: String) = encodeInvalidURIRegEx.replaceAllIn(uri, "_")

  def encodeURIParam(uri: String) = URLEncoder.encode(uri, "UTF-8")
    .replaceAll(" ", "%20")
    .replaceAll("\\+", "%20")
    .replaceAll("\\%21", "!")
    .replaceAll("\\%27", "'")
    .replaceAll("\\%28", "(")
    .replaceAll("\\%29", ")")
    .replaceAll("\\%7E", "~")

  def encodeFileName(name: String): String = {
    val forbiddenChars = List(
      '/', '\\', '<', '>', ':', '"', '|', '?', '*', ' '
    ) :+ (0 to 32)
    val forbiddenNames = List(
      "CON", "PRN", "AUX", "NUL",
      "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
      "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )
    val harmlessName = if (forbiddenNames.contains(name.toUpperCase()))
      "_" + name + "_"
    else
      name
    harmlessName.map(c => if (forbiddenChars.contains(c)) '_' else c)
  }

  trait DataObject extends Ordered[DataObject] {
    def easyprint: String = toString

    def capsulatedprint: String = {
      val ep = easyprint
      if (ep.matches(".*\\s,\\.;.*")) s""""$ep"""" else ep
    }

    def compare(o: DataObject): Int = easyprint.compare(o.easyprint)
  }

  case class NullObject(caption: String) extends DataObject {
    override def easyprint = caption
  }

  object RiegeRaw {
    val KIND_STANDARD = 0;
    val KIND_EMPTY_RIEGE = 1;
    val RIEGENMODE_BY_Program = 1;
    val RIEGENMODE_BY_JG = 2;
    val RIEGENMODE_BY_JG_VEREIN = 3;
  }

  case class RiegeRaw(wettkampfId: Long, r: String, durchgang: Option[String], start: Option[Long], kind: Int) extends DataObject {
    override def easyprint = r
  }

  case class Riege(r: String, durchgang: Option[String], start: Option[Disziplin], kind: Int) extends DataObject {
    override def easyprint = r

    def toRaw(wettkampfId: Long) = RiegeRaw(wettkampfId, r, durchgang, start.map(_.id), kind)
  }

  case class CompoundGrouper(groupers: Seq[DataObject]) extends DataObject { //GenericGrouper(groupers.map(_.easyprint).mkString(","))
    override def easyprint: String = groupers.map(_.easyprint).mkString(",")
  }

  case class GenericGrouper(name: String) extends DataObject {
    override def easyprint: String = name
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

    def extendedprint = s"$name ${verband.getOrElse("")}"

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

    def mapSexPrediction(athlet: Athlet): String = Surname
      .isSurname(athlet.vorname)
      .map { sn => if (sn.isMasculin == sn.isFeminin) athlet.geschlecht else if (sn.isMasculin) "M" else "W" }
      .getOrElse("X")
  }

  case class Athlet(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Long], activ: Boolean) extends DataObject {
    override def easyprint: String = name + " " + vorname + " " + (gebdat match {
      case Some(d) => f"$d%tY "
      case _ => ""
    })

    def extendedprint: String = "" + (geschlecht match {
      case "W" => s"Ti ${name + " " + vorname}"
      case _ => s"Tu ${name + " " + vorname}"
    }) + (gebdat match {
      case Some(d) => f", $d%tF"
      case _ => ""
    })

    def shortPrint: String = "" + (geschlecht match {
      case "W" => s"Ti ${name + " " + vorname}"
      case _ => s"Tu ${name + " " + vorname}"
    }) + " " + (gebdat match {
      case Some(d) => f"$d%tY "
      case _ => ""
    })

    def toPublicView: Athlet = {
      Athlet(id, 0, geschlecht, name, vorname, gebdat
        .map(d => sqlDate2ld(d))
        .map(ld => LocalDate.of(ld.getYear, 1, 1))
        .map(ld => ld2SQLDate(ld))
        , "", "", "", verein, activ)
    }

    def toAthletView(verein: Option[Verein]): AthletView = AthletView(
      id, js_id,
      geschlecht, name, vorname, gebdat,
      strasse, plz, ort,
      verein, activ)
  }

  case class AthletView(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Verein], activ: Boolean) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {
      case Some(d) => f"$d%tY ";
      case _ => " "
    }) + (verein match {
      case Some(v) => v.easyprint;
      case _ => ""
    })

    def extendedprint: String = "" + (geschlecht match {
      case "W" => s"Ti ${name + " " + vorname}"
      case _ => s"Tu ${name + " " + vorname}"
    }) + (gebdat match {
      case Some(d) => f", $d%tF "
      case _ => ""
    }) + (verein match {
      case Some(v) => v.easyprint;
      case _ => ""
    })

    def toPublicView: AthletView = {
      AthletView(id, 0, geschlecht, name, vorname, gebdat
        .map(d => sqlDate2ld(d))
        .map(ld => LocalDate.of(ld.getYear, 1, 1))
        .map(ld => ld2SQLDate(ld))
        , "", "", "", verein, activ)
    }

    def toAthlet = Athlet(id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein.map(_.id), activ)

    def withBestMatchingGebDat(importedGebDat: Option[Date]) = {
      copy(gebdat = importedGebDat match {
        case Some(d) =>
          gebdat match {
            case Some(cd) if (cd.toLocalDate.getYear == d.toLocalDate.getYear) && f"${cd}%tF".endsWith("-01-01") => Some(d)
            case _ => gebdat
          }
        case _ => gebdat
      })
    }

    def updatedWith(athlet: Athlet) = AthletView(athlet.id, athlet.js_id, athlet.geschlecht, athlet.name, athlet.vorname, athlet.gebdat, athlet.strasse, athlet.plz, athlet.ort, verein.map(v => v.copy(id = athlet.verein.getOrElse(0L))), athlet.activ)
  }

  object TeamAggreateFun {
    def apply(text: String): TeamAggreateFun = {
      if (text == null) Sum else text.toLowerCase() match {
        case "avg/" => Avg
        case "median/" => Med
        case "min/" => Min
        case "max/" => Max
        case "devmin/" => DevMin
        case "devmax/" => DevMax
        case _ => Sum
      }
    }
  }

  sealed trait TeamAggreateFun {
    def sum(xs: Iterable[Resultat]): Resultat = if (xs.nonEmpty) xs.reduce(_ + _) else Resultat(0, 0, 0)
    def max(xs: Iterable[Resultat]): Resultat = if (xs.nonEmpty) xs.reduce(_.max(_)) else Resultat(0, 0, 0)
    def min(xs: Iterable[Resultat]): Resultat = if (xs.nonEmpty) xs.reduce(_.min(_)) else Resultat(0, 0, 0)
    def mean(xs: Iterable[Resultat]): Resultat = if (xs.nonEmpty) sum(xs) / xs.size else Resultat(0, 0, 0)
    def median(xs: Iterable[Resultat]): Resultat = xs match {
      case Nil => Resultat(0,0,0)
      case x::Nil => x
      case _ =>
        val l = xs.toList.sortBy(_.endnote)
        val i = Math.max(1, l.size / 2)
        if (l.size % 2 == 0) {
          (l(i-1) + l(i)) / 2
        } else {
          l(i)
        }
    }

    def variance(xs: Iterable[Resultat]): Resultat = {
      if (xs.nonEmpty) {
        val avg = mean(xs)

        mean(xs
          .map(_ - avg)
          .map(_.pow(2))
        )
      } else Resultat(0, 0, 0)
    }

    def stdDev(xs: Iterable[Resultat]): Resultat = xs match {
      case Nil => Resultat(0,0,0)
      case x::Nil => x
      case _ => variance(xs).sqrt
    }

    def apply(results: Iterable[Resultat]): Resultat
    def sortFactor = 1
    def toFormelPart: String = ""

    def toDescriptionPart: String
  }

  case object Sum extends TeamAggreateFun {
    override def apply(results: Iterable[Resultat]): Resultat = sum(results)

    def toDescriptionPart: String = "Summe aus"
  }

  case object Avg extends TeamAggreateFun {
    override def apply(results: Iterable[Resultat]): Resultat = mean(results)

    override def toFormelPart: String = "avg/"

    def toDescriptionPart: String = "⌀ aus"
  }

  case object Med extends TeamAggreateFun {
    override def apply(results: Iterable[Resultat]): Resultat = median(results)

    override def toFormelPart: String = "median/"

    def toDescriptionPart: String = "Median aus"
  }

  case object Max extends TeamAggreateFun {
    override def apply(results: Iterable[Resultat]): Resultat = max(results)

    override def toFormelPart: String = "max/"

    def toDescriptionPart: String = "höchste aus"
  }

  case object Min extends TeamAggreateFun {
    override def apply(results: Iterable[Resultat]): Resultat = min(results)

    override def toFormelPart: String = "min/"

    def toDescriptionPart: String = "niedrigste aus"
  }

  case object DevMin extends TeamAggreateFun {
    override def apply(results: Iterable[Resultat]): Resultat = stdDev(results)
    override def sortFactor = -1
    override def toFormelPart: String = "devmin/"

    def toDescriptionPart: String = "kleinste Abweichung aus"
  }

  case object DevMax extends TeamAggreateFun {
    override def apply(results: Iterable[Resultat]): Resultat = stdDev(results)
    override def toFormelPart: String = "devmax/"

    def toDescriptionPart: String = "grösste Abweichung aus"
  }

  case class Team(name: String, rulename: String, wertungen: List[WertungView], countingWertungen: Map[Disziplin, List[WertungView]], relevantWertungen: Map[Disziplin, List[WertungView]], aggregateFun: TeamAggreateFun) extends DataObject {
    val diszList: Map[Disziplin, Int] = countingWertungen.map { t =>
      val disz = t._1
      val ord = t._2.find(_.wettkampfdisziplin.disziplin == t._1).map(_.wettkampfdisziplin.ord).getOrElse(999)
      (disz, ord)
    }

    val perDisciplinResults: Map[Disziplin, List[Resultat]] = countingWertungen
      .map { case (disciplin, wtg) => (disciplin, wtg
        .map(w => if (w.showInScoreList) w.resultat else Resultat(0, 0, 0)))
      }

    //val perDisciplinSums = perDisciplinResults.map{ case (disciplin, results) => (disciplin, aggregateFun(results)) }
    //val sum = aggregateFun(perDisciplinSums.values)
    //val avg = Avg(perDisciplinSums.values)

    val blockrows = wertungen.map(_.athlet).distinct.size

    def isRelevantResult(disziplin: Disziplin, member: AthletView): Boolean = {
      relevantWertungen(disziplin).find(_.athlet.equals(member)).exists(w => perDisciplinResults(disziplin).exists(r => r.endnote.equals(w.resultat.endnote)))
    }

    override def easyprint: String = "Team " + name
  }

  case class TeamItem(index: Int, name: String) {
    def itemText: String = if (index > 0) s"$name ${index}" else if (index < 0) name else ""

    def machtesItemText(text: String): Boolean = text match {
      case t: String if isNumeric(t) && !"0".equals(t) =>
        val intText: Int = t
        intText == index
      case t: String if t.equalsIgnoreCase(name) => true
      case t: String if t.equalsIgnoreCase(itemText) => true
      case _ => false
    }
  }

  object Wertungsrichter {
    def apply(): Wertungsrichter = Wertungsrichter(0, 0, "", "", "", None, "", "", "", None, activ = true)
  }

  case class Wertungsrichter(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Long], activ: Boolean) extends DataObject {
    override def easyprint = "Wertungsrichter " + name
  }

  case class WertungsrichterView(id: Long, js_id: Int, geschlecht: String, name: String, vorname: String, gebdat: Option[java.sql.Date], strasse: String, plz: String, ort: String, verein: Option[Verein], activ: Boolean) extends DataObject {
    override def easyprint = name + " " + vorname + " " + (gebdat match {
      case Some(d) => f"$d%tY ";
      case _ => " "
    }) + (verein match {
      case Some(v) => v.easyprint;
      case _ => ""
    })

    def toWertungsrichter = Wertungsrichter(id, js_id, geschlecht, name, vorname, gebdat, strasse, plz, ort, verein.map(_.id), activ)
  }

  object DurchgangType {
    def apply(code: Int) = code match {
      case 1 => Competition
      case 2 => WarmUp
      case 3 => AwardCeremony
      case 4 => Pause
      case _ => Competition // default-type
    }
  }

  abstract sealed trait DurchgangType {
    val code: Int

    override def toString(): String = code.toString
  }

  case object Competition extends DurchgangType {
    override val code = 1
  }

  case object WarmUp extends DurchgangType {
    override val code = 2
  }

  case object AwardCeremony extends DurchgangType {
    override val code = 3
  }

  case object Pause extends DurchgangType {
    override val code = 4
  }

  object Durchgang {
    def apply(): Durchgang = Durchgang(0, "nicht zugewiesen")

    def apply(wettkampfId: Long, name: String): Durchgang = Durchgang(0, wettkampfId, name, name, Competition, 50, 0, None, None, 0, 0, 0)

    def apply(id: Long, wettkampfId: Long, title: String, name: String, durchgangtype: DurchgangType, ordinal: Int, planStartOffset: Long, effectiveStartTime: Option[java.sql.Timestamp], effectiveEndTime: Option[java.sql.Timestamp]): Durchgang =
      Durchgang(id, wettkampfId, title, name, durchgangtype, ordinal, planStartOffset, effectiveStartTime, effectiveEndTime, 0, 0, 0)
  }

  case class SimpleDurchgang(id: Long, wettkampfId: Long, title: String, name: String, durchgangtype: DurchgangType, ordinal: Int, planStartOffset: Long, effectiveStartTime: Option[java.sql.Timestamp], effectiveEndTime: Option[java.sql.Timestamp]) extends DataObject {
    override def easyprint = if (name.equals(title)) name else s"$title: $name"

    def effectivePlanStart(wkDate: LocalDate): LocalDateTime = LocalDateTime.of(wkDate, LocalTime.MIDNIGHT).plusNanos(planStartOffset * 1000_000L)
  }

  case class Durchgang(id: Long, wettkampfId: Long, title: String, name: String, durchgangtype: DurchgangType, ordinal: Int, planStartOffset: Long, effectiveStartTime: Option[java.sql.Timestamp], effectiveEndTime: Option[java.sql.Timestamp], planEinturnen: Long, planGeraet: Long, planTotal: Long) extends DataObject {
    override def easyprint = if (name.equals(title)) name else s"$title: $name"

    def effectivePlanStart(wkDate: LocalDate): LocalDateTime = LocalDateTime.of(wkDate, LocalTime.MIDNIGHT).plusNanos(planStartOffset * 1000_000L)

    def effectivePlanFinish(wkDate: LocalDate): LocalDateTime = LocalDateTime.of(wkDate, LocalTime.MIDNIGHT).plusNanos((planStartOffset + (if (planStartOffset > 0) planTotal else 24000 * 3600)) * 1000_000L)

    def toAggregator(other: Durchgang) = Durchgang(0, wettkampfId, title, title, durchgangtype, Math.min(ordinal, other.ordinal), Math.min(planStartOffset, planStartOffset), effectiveStartTime, effectiveEndTime, Math.max(planEinturnen, other.planEinturnen), Math.max(planGeraet, other.planGeraet), Math.max(planTotal, other.planTotal))
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
      case None => AthletJahrgang("unbekannt")
    }
  }

  case class AthletJahrgang(jahrgang: String) extends DataObject {
    override def easyprint = "Jahrgang " + jahrgang
  }

  object Leistungsklasse {
    // https://www.dtb.de/fileadmin/user_upload/dtb.de/Sportarten/Ger%C3%A4tturnen/PDFs/2022/01_DTB-Arbeitshilfe_Gtw_KuerMod_2022_V1.pdf
    val dtb = Seq(
      "Kür", "LK1", "LK2", "LK3", "LK4"
    )
  }

  object Altersklasse {

    // file:///C:/Users/Roland/Downloads/Turn10-2018_Allgemeine%20Bestimmungen.pdf
    val akExpressionTurn10 = "AK7-18,AK24,AK30-100/5"
    val altersklassenTurn10 = Seq(
      6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 24, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100
    ).map(i => ("AK", Seq(), i))
    // see https://www.dtb.de/fileadmin/user_upload/dtb.de/Passwesen/Wettkampfordnung_DTB_2021.pdf
    val akDTBExpression = "AK6,AK18,AK22,AK25"
    val altersklassenDTB = Seq(
      6, 18, 22, 25
    ).map(i => ("AK", Seq(), i))
    // see https://www.dtb.de/fileadmin/user_upload/dtb.de/TURNEN/Standards/PDFs/Rahmentrainingskonzeption-GTm_inklAnlagen_19.11.2020.pdf
    val akDTBPflichtExpression = "AK8-9,AK11-19/2"
    val altersklassenDTBPflicht = Seq(
      7, 8, 9, 11, 13, 15, 17, 19
    ).map(i => ("AK", Seq(), i))
    val akDTBKuerExpression = "AK13-19/2"
    val altersklassenDTBKuer = Seq(
      12, 13, 15, 17, 19
    ).map(i => ("AK", Seq(), i))

    val predefinedAKs = Map(
      ("Ohne" -> "")
      , ("Turn10®" -> akExpressionTurn10)
      , ("DTB" -> akDTBExpression)
      , ("DTB Pflicht" -> akDTBPflichtExpression)
      , ("DTB Kür" -> akDTBKuerExpression)
      , ("Individuell" -> "")
    )

    def apply(altersgrenzen: Seq[(String, Seq[String], Int)]): Seq[Altersklasse] = {
      if (altersgrenzen.isEmpty) {
        Seq.empty
      } else {
        altersgrenzen
          .groupBy(ag => (ag._1, ag._2)) // ag-name + qualifiers
          .map { aggr =>
            aggr._1 -> aggr._2
              .sortBy(ag => ag._3)
              .distinctBy(_._3)
              .foldLeft(Seq[Altersklasse]()) { (acc, ag) =>
                acc :+ Altersklasse(ag._1, acc.lastOption.map(_.alterBis + 1).getOrElse(0), ag._3 - 1, ag._2)
              }.appended(Altersklasse(aggr._2.last._1, aggr._2.last._3, 0, aggr._2.last._2))
          }
          .flatMap(_._2)
          .toSeq
      }
    }

    def apply(klassen: Seq[Altersklasse], alter: Int, geschlecht: String, programm: ProgrammView): Altersklasse = {
      klassen
        .find(klasse => klasse.matchesAlter(alter) && klasse.matchesGeschlecht(geschlecht) && klasse.matchesProgramm(programm))
        .getOrElse(Altersklasse(klassen.head.bezeichnung, alter, alter, Seq(geschlecht, programm.name)))
    }

    def parseGrenzen(klassenDef: String, fallbackBezeichnung: String = "Altersklasse"): Seq[(String, Seq[String], Int)] = {
      /*
      AKWBS(W+BS)7,8,9,10,12,16,AKMBS(M+BS)8,10,15

       */
      val rangeStepPattern = "([\\D\\s]*)([0-9]+)-([0-9]+)/([0-9]+)".r
      val rangepattern = "([\\D\\s]*)([0-9]+)-([0-9]+)".r
      val intpattern = "([\\D\\s]*)([0-9]+)".r
      val qualifierPattern = "(.*)\\(([\\D\\s]+)\\)".r

      def bez(b: String): (String, Seq[String]) = if (b.nonEmpty) {
        b match {
          case qualifierPattern(bezeichnung, qualifiers) => (bezeichnung, qualifiers.split("\\+").toSeq)
          case bezeichnung: String => (bezeichnung, Seq())
        }
      } else ("", Seq())

      klassenDef.split(",")
        .flatMap {
          case rangeStepPattern(bezeichnung, von, bis, stepsize) => Range.inclusive(von, bis, stepsize).map(i => (bez(bezeichnung), i))
          case rangepattern(bezeichnung, von, bis) => (str2Int(von) to str2Int(bis)).map(i => (bez(bezeichnung), i))
          case intpattern(bezeichnung, von) => Seq((bez(bezeichnung), str2Int(von)))
          case _ => Seq.empty
        }.toList
        .foldLeft(Seq[(String, Seq[String], Int)]()) { (acc, item) =>
          if (item._1._1.nonEmpty) {
            acc :+ (item._1._1, item._1._2, item._2)
          } else if (acc.nonEmpty) {
            acc :+ (acc.last._1, acc.last._2, item._2)
          } else {
            acc :+ (fallbackBezeichnung, Seq(), item._2)
          }
        }
        .sortBy(item => (item._1, item._3))
    }

    def apply(klassenDef: String, fallbackBezeichnung: String = "Altersklasse"): Seq[Altersklasse] = {
      apply(parseGrenzen(klassenDef, fallbackBezeichnung))
    }
  }

  case class Altersklasse(bezeichnung: String, alterVon: Int, alterBis: Int, qualifiers: Seq[String]) extends DataObject {
    val geschlechtQualifier = qualifiers.filter(q => Seq("M", "W").contains(q))
    val programmQualifier = qualifiers.filter(q => !Seq("M", "W").contains(q))

    def matchesAlter(alter: Int): Boolean =
      ((alterVon == 0 || alter >= alterVon) &&
        (alterBis == 0 || alter <= alterBis))

    def matchesGeschlecht(geschlecht: String): Boolean = {
      geschlechtQualifier.isEmpty || geschlechtQualifier.contains(geschlecht)
    }

    def matchesProgramm(programm: ProgrammView): Boolean = {
      programmQualifier.isEmpty || programm.programPath.exists(p => programmQualifier.contains(p.name))
    }

    override def easyprint: String = {
      val q = if (qualifiers.nonEmpty) qualifiers.mkString("(", ",", ")") else ""
      if (alterVon > 0 && alterBis > 0)
        if (alterVon == alterBis)
          s"""$bezeichnung$q $alterVon"""
        else s"""$bezeichnung$q $alterVon bis $alterBis"""
      else if (alterVon > 0 && alterBis == 0)
        s"""$bezeichnung$q ab $alterVon"""
      else
        s"""$bezeichnung$q bis $alterBis"""
    }

    def easyprintShort: String = {
      if (alterVon > 0 && alterBis > 0)
        if (alterVon == alterBis)
          s"""$bezeichnung$alterVon"""
        else s"""$bezeichnung$alterVon-$alterBis"""
      else if (alterVon > 0 && alterBis == 0)
        s"""$bezeichnung$alterVon-"""
      else
        s"""$bezeichnung-$alterBis"""
    }

    override def compare(x: DataObject): Int = x match {
      case ak: Altersklasse => alterVon.compareTo(ak.alterVon)
      case _ => x.easyprint.compareTo(easyprint)
    }
  }

  case class WettkampfJahr(wettkampfjahr: String) extends DataObject {
    override def easyprint = "Wettkampf-Jahr " + wettkampfjahr
  }

  case class Disziplin(id: Long, name: String) extends DataObject {
    override def easyprint = name

    def equalsOrPause(other: Disziplin) = math.abs(id) == math.abs(other.id)

    def isPause: Boolean = id < 0

    def asPause: Disziplin = Disziplin(id * -1, s"${name} Pause")

    def harmless: Disziplin = Disziplin(math.abs(id), name)

    def asNonPause: Disziplin = Disziplin(math.abs(id), name.replace(" Pause", ""))

    def normalizedOrdinal(dzl: List[Disziplin]) = dzl.indexOf(asNonPause) + 1
  }

  trait Programm extends DataObject {
    override def easyprint = name

    val id: Long
    val name: String
    val aggregate: Int
    val ord: Int
    val alterVon: Int
    val alterBis: Int
    val riegenmode: Int
    val uuid: String

    def withParent(parent: ProgrammView) = {
      ProgrammView(id, name, aggregate, Some(parent), ord, alterVon, alterBis, uuid, riegenmode)
    }

    def toView = {
      ProgrammView(id, name, aggregate, None, ord, alterVon, alterBis, uuid, riegenmode)
    }
  }

  /**
   * <pre>
   * Krits        +===========================================+=========================================================
   * aggregate ->|0                                          |1
   * +-------------------------------------------+---------------------------------------------------------
   * riegenmode->|1                   |2  / 3(+verein)       |1                     |2  / 3(+verein)
   * Acts         +===========================================+=========================================================
   * Einteilung->| Sex,Pgm,Verein     | Sex,Pgm,Jg(,Verein)  | Sex,Pgm,Verein       | Pgm,Sex,Jg(,Verein)
   * +--------------------+----------------------+----------------------+-----------------------------------
   * Teilnahme   | 1/WK               | 1/WK                 | &lt;<=PgmCnt(Jg)/WK      | 1/Pgm
   * +-------------------------------------------+----------------------------------------------------------
   * Registration| 1/WK               | 1/WK, Pgm/(Jg)       | mind. 1, max 1/Pgm   | 1/WK aut. Tn 1/Pgm
   * +-------------------------------------------+----------------------------------------------------------
   * Beispiele   | GeTu/KuTu/KuTuRi   | Turn10® (BS/OS)      | TG Allgäu (Pfl./Kür) | ATT (Kraft/Bewg)
   * +-------------------------------------------+----------------------------------------------------------
   * Rangliste   | Sex/Programm       | Sex/Programm/Jg      | Sex/Programm         | Sex/Programm/Jg
   * |                    | Sex/Programm/AK      | Sex/Programm/AK      |
   * +===========================================+=========================================================
   * </pre>
   */
  case class ProgrammRaw(id: Long, name: String, aggregate: Int, parentId: Long, ord: Int, alterVon: Int, alterBis: Int, uuid: String, riegenmode: Int) extends Programm

  case class ProgrammView(id: Long, name: String, aggregate: Int, parent: Option[ProgrammView], ord: Int, alterVon: Int, alterBis: Int, uuid: String, riegenmode: Int) extends Programm {
    //override def easyprint = toPath

    def head: ProgrammView = parent match {
      case None => this
      case Some(p) => p.head
    }

    def subHead: Option[ProgrammView] = parent match {
      case None => None
      case Some(p) => if (p.parent.nonEmpty) p.subHead else parent
    }

    def programPath: Seq[ProgrammView] = parent match {
      case None => Seq(this)
      case Some(p) => p.programPath :+ this
    }

    def wettkampfprogramm: ProgrammView = if (aggregator == this) this else aggregatorSubHead

    def aggregatorHead: ProgrammView = parent match {
      case Some(p) if (p.aggregate != 0) => p.aggregatorHead
      case _ => this
    }

    def groupedHead: ProgrammView = parent match {
      case Some(p) if (p.parent.nonEmpty && aggregate != 0) => p
      case _ => aggregatorHead
    }

    def aggregatorParent: ProgrammView = parent match {
      case Some(p) if (aggregate != 0) => p.parent.getOrElse(this)
      case _ => this
    }

    def aggregator: ProgrammView = parent match {
      case Some(p) if (aggregate != 0) => p
      case _ => this
    }

    def aggregatorSubHead: ProgrammView = parent match {
      case Some(p) if (aggregate != 0 && p.aggregate != 0) => p.aggregatorSubHead
      case Some(p) if (aggregate != 0 && p.aggregate == 0) => this
      case _ => this
    }

    def sameOrigin(other: ProgrammView) = head.equals(other.head)

    def toPath: String = parent match {
      case None => this.name
      case Some(p) => p.toPath + " / " + name
    }

    override def compare(o: DataObject): Int = o match {
      case p: ProgrammView => toPath.compareTo(p.toPath)
      case _ => easyprint.compareTo(o.easyprint)
    }

    override def toString = s"$toPath"
  }

  //  object Wettkampf {
  //    def apply(id: Long, datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal): Wettkampf =
  //      Wettkampf(id, datum, titel, programmId, auszeichnung, auszeichnungendnote, if(id == 0) Some(UUID.randomUUID().toString()) else None)
  //    def apply(id: Long, datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, uuid: String): Wettkampf =
  //      if(uuid != null) Wettkampf(id, datum, titel, programmId, auszeichnung, auszeichnungendnote, Some(uuid))
  //      else apply(id, datum, titel, programmId, auszeichnung, auszeichnungendnote)
  //  }
  case class Wettkampf(id: Long, uuid: Option[String], datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, notificationEMail: String, altersklassen: Option[String], jahrgangsklassen: Option[String], punktegleichstandsregel: Option[String], rotation: Option[String], teamrule: Option[String]) extends DataObject {

    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"

    lazy val teamRegeln: Option[TeamRegel] = teamrule.map(TeamRegel(_))
    lazy val extraTeams: List[String] = teamRegeln.map(_.getExtrateams).toList.flatten
    lazy val hasTeams: Boolean = teamRegeln.exists(_.teamsAllowed)

    def toView(programm: ProgrammView): WettkampfView = {
      WettkampfView(id, uuid, datum, titel, programm, auszeichnung, auszeichnungendnote, notificationEMail, altersklassen.getOrElse(""), jahrgangsklassen.getOrElse(""), punktegleichstandsregel.getOrElse(""), rotation.getOrElse(""), teamrule.getOrElse(""))
    }

    def toPublic: Wettkampf = Wettkampf(id, uuid, datum, titel, programmId, auszeichnung, auszeichnung, "",
      altersklassen.map(Altersklasse(_).map(_.easyprint).mkString(", ")),
      jahrgangsklassen.map(Altersklasse(_).map(_.easyprint).mkString(", ")),
      punktegleichstandsregel.map(Gleichstandsregel(_).toFormel),
      rotation.map(RiegenRotationsregel(_).toFormel),
      teamrule.map(TeamRegel(_).toRuleName))

    def prepareFilePath(homedir: String, readOnly: Boolean = true, moveFrom: Option[Wettkampf] = None): File = {
      val targetDir = new File(homedir + "/" + encodeFileName(easyprint))
      if (!readOnly) {
        moveFrom match {
          case None => if (!targetDir.exists()) targetDir.mkdirs
          case Some(p) =>
            val oldDir = new java.io.File(homedir + "/" + encodeFileName(p.easyprint))
            if (!targetDir.exists()) {
              if (oldDir.exists() && !oldDir.equals(targetDir)) {
                oldDir.renameTo(targetDir)
                Files.deleteIfExists(oldDir.toPath)
              } else {
                targetDir.mkdirs()
              }
            } else if (oldDir.exists() && !oldDir.equals(targetDir)) {
              val oldPath = oldDir.toPath
              val newPath = targetDir.toPath
              Files.walk(oldPath)
                .map(source => (source, newPath.resolve(oldPath.relativize(source))))
                .filter { case (source, target) => !Files.exists(target) || Files.getLastModifiedTime(target).compareTo(Files.getLastModifiedTime(source)) < 0 }
                .forEach { case (source, target) => Files.copy(source, target) }
            }
        }
      }
      targetDir
    }


    def filePath(homedir: String, origin: String, readOnly: Boolean = false): Path = new java.io.File(prepareFilePath(homedir, readOnly), ".at." + origin).toPath

    def fromOriginFilePath(homedir: String, origin: String, readOnly: Boolean = false): Path = new java.io.File(prepareFilePath(homedir, readOnly), ".from." + origin).toPath

    def saveRemoteOrigin(homedir: String, origin: String): Unit = {
      val path = fromOriginFilePath(homedir, origin)
      val fos = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)
      try {
        fos.write(uuid.toString.getBytes("utf-8"))
        fos.flush()
      } finally {
        fos.close()
      }
      val os = System.getProperty("os.name").toLowerCase
      if (os.indexOf("win") > -1) {
        Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)
      }
    }

    def hasRemote(homedir: String, origin: String): Boolean = {
      val path = fromOriginFilePath(homedir, origin, readOnly = true)
      path.toFile.exists
    }

    def removeRemote(homedir: String, origin: String): Unit = {
      val atFile = fromOriginFilePath(homedir, origin, readOnly = true).toFile
      if (atFile.exists) {
        atFile.delete()
      }
    }

    def saveSecret(homedir: String, origin: String, secret: String): Unit = {
      val path = filePath(homedir, origin)
      val fos = Files.newOutputStream(path, StandardOpenOption.CREATE)
      try {
        fos.write(secret.getBytes("utf-8"))
        fos.flush()
      } finally {
        fos.close()
      }
      val os = System.getProperty("os.name").toLowerCase
      if (os.indexOf("win") > -1) {
        Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)
      }
    }

    def readSecret(homedir: String, origin: String): Option[String] = {
      val path = filePath(homedir, origin, readOnly = true)
      if (path.toFile.exists) {
        Some(new String(Files.readAllBytes(path), "utf-8"))
      }
      else {
        None
      }
    }

    def removeSecret(homedir: String, origin: String): Unit = {
      val atFile = filePath(homedir, origin).toFile
      if (atFile.exists) {
        atFile.delete()
      }
    }

    def hasSecred(homedir: String, origin: String): Boolean = readSecret(homedir, origin) match {
      case Some(_) => true
      case None => false
    }

    def isReadonly(homedir: String, origin: String): Boolean = !hasSecred(homedir, origin) && hasRemote(homedir, origin)
  }

  //  object WettkampfView {
  //    def apply(id: Long, datum: java.sql.Date, titel: String, programm: ProgrammView, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal): WettkampfView =
  //      WettkampfView(id, datum, titel, programm, auszeichnung, auszeichnungendnote, if(id == 0) Some(UUID.randomUUID().toString()) else None)
  //    def apply(id: Long, datum: java.sql.Date, titel: String, programm: ProgrammView, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, uuid: String): WettkampfView =
  //      if(uuid != null) WettkampfView(id, datum, titel, programm, auszeichnung, auszeichnungendnote, Some(uuid))
  //      else apply(id, datum, titel, programm, auszeichnung, auszeichnungendnote)
  //  }
  case class WettkampfView(id: Long, uuid: Option[String], datum: java.sql.Date, titel: String, programm: ProgrammView, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, notificationEMail: String, altersklassen: String, jahrgangsklassen: String, punktegleichstandsregel: String, rotation: String, teamrule: String) extends DataObject {
    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"

    lazy val details: String = s"${programm.name}" +
      s"${if (teamrule.nonEmpty && !teamrule.equals("Keine Teams")) ", " + teamrule else ""}" +
      s"${if (altersklassen.nonEmpty) ", Altersklassen" else ""}" +
      s"${if (jahrgangsklassen.nonEmpty) ", Jahrgangs Altersklassen" else ""}" +
      ""

    def toWettkampf = Wettkampf(id, uuid, datum, titel, programm.id, auszeichnung, auszeichnungendnote, notificationEMail, Option(altersklassen), Option(jahrgangsklassen), Option(punktegleichstandsregel), Option(rotation), Option(teamrule))
  }

  case class WettkampfStats(uuid: String, wkid: Int, titel: String, finishAthletesCnt: Int, finishClubsCnt: Int, finishOnlineAthletesCnt: Int, finishOnlineClubsCnt: Int) extends DataObject {
  }

  case class WettkampfMetaData(uuid: String, wkid: Int, finishAthletesCnt: Int, finishClubsCnt: Int, finishOnlineAthletesCnt: Int, finishOnlineClubsCnt: Int,
                               finishDonationMail: Option[String], finishDonationAsked: Option[BigDecimal], finishDonationApproved: Option[BigDecimal]) extends DataObject {
  }

  case class PublishedScoreRaw(id: String, title: String, query: String, published: Boolean, publishedDate: java.sql.Date, wettkampfId: Long) extends DataObject {
    override def easyprint = f"PublishedScore($title)"
  }

  object PublishedScoreRaw {
    def apply(title: String, query: String, published: Boolean, publishedDate: java.sql.Date, wettkampfId: Long): PublishedScoreRaw =
      PublishedScoreRaw(UUID.randomUUID().toString, title, query, published, publishedDate, wettkampfId)
  }

  case class PublishedScoreView(id: String, title: String, query: String, published: Boolean, publishedDate: java.sql.Date, wettkampf: Wettkampf) extends DataObject {
    override def easyprint = f"PublishedScore($title - ${wettkampf.easyprint})"

    def isAlphanumericOrdered = query.contains("&alphanumeric")

    def toRaw = PublishedScoreRaw(id, title, query, published, publishedDate, wettkampf.id)
  }

  case class Wettkampfdisziplin(id: Long, programmId: Long, disziplinId: Long, kurzbeschreibung: String, detailbeschreibung: Option[Array[Byte]], notenfaktor: scala.math.BigDecimal, masculin: Int, feminim: Int, ord: Int, scale: Int, dnote: Int, min: Int, max: Int, startgeraet: Int) extends DataObject {
    override def easyprint = f"$disziplinId%02d: $kurzbeschreibung"
  }

  case class WettkampfdisziplinView(id: Long, programm: ProgrammView, disziplin: Disziplin, kurzbeschreibung: String, detailbeschreibung: Option[Array[Byte]], notenSpez: NotenModus, masculin: Int, feminim: Int, ord: Int, scale: Int, dnote: Int, min: Int, max: Int, startgeraet: Int) extends DataObject {
    override def easyprint = disziplin.name

    val isDNoteUsed = dnote != 0

    def verifiedAndCalculatedWertung(wertung: Wertung): Wertung = {
      if (wertung.noteE.isEmpty) {
        wertung.copy(noteD = None, noteE = None, endnote = None)
      } else {
        val (d, e) = notenSpez.validated(wertung.noteD.getOrElse(BigDecimal(0)).doubleValue, wertung.noteE.getOrElse(BigDecimal(0)).doubleValue, this)
        wertung.copy(noteD = Some(d), noteE = Some(e), endnote = Some(notenSpez.calcEndnote(d, e, this)))
      }
    }

    def toWettkampdisziplin = Wettkampfdisziplin(id, programm.id, disziplin.id, kurzbeschreibung, None, notenSpez.calcEndnote(0, 1, this), masculin, feminim, ord, scale, dnote, min, max, startgeraet)
  }

  case class WettkampfPlanTimeRaw(id: Long, wettkampfId: Long, wettkampfDisziplinId: Long, wechsel: Long, einturnen: Long, uebung: Long, wertung: Long) extends DataObject {
    override def easyprint = f"WettkampfPlanTime(disz=$wettkampfDisziplinId, w=$wechsel, e=$einturnen, u=$uebung, w=$wertung)"
  }

  case class WettkampfPlanTimeView(id: Long, wettkampf: Wettkampf, wettkampfdisziplin: WettkampfdisziplinView, wechsel: Long, einturnen: Long, uebung: Long, wertung: Long) extends DataObject {
    override def easyprint = f"WettkampfPlanTime(wk=$wettkampf, disz=$wettkampfdisziplin, w=$wechsel, e=$einturnen, u=$uebung, w=$wertung)"

    def toWettkampfPlanTimeRaw = WettkampfPlanTimeRaw(id, wettkampf.id, wettkampfdisziplin.id, wechsel, einturnen, uebung, wertung)
  }

  case class Resultat(noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal) extends DataObject {
    def -(r: Resultat): Resultat = Resultat(noteD - r.noteD, noteE - r.noteE, endnote - r.endnote)

    def +(r: Resultat): Resultat = Resultat(noteD + r.noteD, noteE + r.noteE, endnote + r.endnote)

    def -(r: BigDecimal): Resultat = Resultat(noteD - r, noteE - r, endnote - r)

    def +(r: BigDecimal): Resultat = Resultat(noteD + r, noteE + r, endnote + r)

    def /(cnt: Int): Resultat = Resultat(noteD / cnt, noteE / cnt, endnote / cnt)

    def *(cnt: Long): Resultat = Resultat(noteD * cnt, noteE * cnt, endnote * cnt)

    def *(cnt: BigDecimal): Resultat = Resultat(noteD * cnt, noteE * cnt, endnote * cnt)

    def max(other: Resultat): Resultat = Resultat(noteD.max(other.noteD), noteE.max(other.noteE), endnote.max(other.endnote))
    def min(other: Resultat): Resultat = Resultat(noteD.min(other.noteD), noteE.min(other.noteE), endnote.min(other.endnote))
    def pow(exponent: Int): Resultat = Resultat(noteD.pow(exponent), noteE.pow(exponent), endnote.pow(exponent))
    def sqrt: Resultat = Resultat(
      BigDecimal.decimal(Math.sqrt(noteD.toDouble)).setScale(noteD.scale, RoundingMode.HALF_UP),
      BigDecimal.decimal(Math.sqrt(noteE.toDouble)).setScale(noteE.scale, RoundingMode.HALF_UP),
      BigDecimal.decimal(Math.sqrt(endnote.toDouble)).setScale(endnote.scale, RoundingMode.HALF_UP))

    lazy val formattedD: String = if (noteD > 0) f"${noteD}%4.2f" else ""
    lazy val formattedE: String = if (noteE > 0) f"${noteE}%4.2f" else ""
    lazy val formattedEnd: String = if (endnote > 0) f"${endnote}%6.2f" else ""

    override def easyprint: String = f"${formattedD}%6s${formattedE}%6s${formattedEnd}%6s"
  }

  case class Wertung(id: Long, athletId: Long, wettkampfdisziplinId: Long, wettkampfId: Long, wettkampfUUID: String, noteD: Option[scala.math.BigDecimal], noteE: Option[scala.math.BigDecimal], endnote: Option[scala.math.BigDecimal], riege: Option[String], riege2: Option[String], team: Option[Int]) extends DataObject {
    lazy val resultat = Resultat(noteD.getOrElse(0), noteE.getOrElse(0), endnote.getOrElse(0))

    def updatedWertung(valuesFrom: Wertung) = copy(noteD = valuesFrom.noteD, noteE = valuesFrom.noteE, endnote = valuesFrom.endnote)

    def valueAsText(valueOption: Option[BigDecimal]) = valueOption match {
      case None => ""
      case Some(value) => value.toString()
    }

    def noteDasText = valueAsText(noteD)

    def noteEasText = valueAsText(noteE)

    def endnoteAsText = valueAsText(endnote)
  }

  case class WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: Option[scala.math.BigDecimal], noteE: Option[scala.math.BigDecimal], endnote: Option[scala.math.BigDecimal], riege: Option[String], riege2: Option[String], team: Int) extends DataObject {
    lazy val resultat = Resultat(noteD.getOrElse(0), noteE.getOrElse(0), endnote.getOrElse(0))

    def +(r: Resultat) = resultat + r

    def getTeamName(extraTeams: List[String]): String = athlet.verein match {
      case Some(v) =>
        if (team == 0) v.easyprint
        else if (team < 0 && extraTeams.size > team * -1 - 1) {
          s"${extraTeams(team * -1 - 1)}"
        }
        else if (wettkampf.teamrule.exists(r => r.contains("VereinGe")))
          s"${v.easyprint} $team"
        else
          s"${v.verband.getOrElse(v.extendedprint)} $team"
      case _ => if (team != 0) "$team" else ""
    }

    lazy val teamName = getTeamName(wettkampf.extraTeams)

    def toWertung = Wertung(id, athlet.id, wettkampfdisziplin.id, wettkampf.id, wettkampf.uuid.getOrElse(""), noteD, noteE, endnote, riege, riege2, Some(team))

    def toWertung(riege: String, riege2: Option[String]) = Wertung(id, athlet.id, wettkampfdisziplin.id, wettkampf.id, wettkampf.uuid.getOrElse(""), noteD, noteE, endnote, Some(riege), riege2, Some(team))

    def updatedWertung(valuesFrom: Wertung) = copy(noteD = valuesFrom.noteD, noteE = valuesFrom.noteE, endnote = valuesFrom.endnote)

    def validatedResult(dv: Double, ev: Double) = {
      val (d, e) = wettkampfdisziplin.notenSpez.validated(dv, ev, wettkampfdisziplin)
      Resultat(d, e, wettkampfdisziplin.notenSpez.calcEndnote(d, e, wettkampfdisziplin))
    }

    def showInScoreList = {
      (endnote.sum > 0) || (athlet.geschlecht match {
        case "M" => wettkampfdisziplin.masculin > 0
        case "W" => wettkampfdisziplin.feminim > 0
        case _ => endnote.sum > 0
      })
    }

    override def easyprint = {
      resultat.easyprint
    }
  }

  sealed trait DataRow {}

  trait ResultRow {
    val athletId: Option[Long] = None
    val sum: Resultat
    lazy val avg = Avg(resultate.map(_.sum).filter(r => r.endnote > 0))
    val rang: Resultat
    val auszeichnung: Boolean
    val resultate: IndexedSeq[LeafRow] = IndexedSeq()
    val divider: Int = 1
  }

  /**
   * Single Result of a row
   *
   * @param title        Discipline name
   * @param sum
   * @param rang
   * @param auszeichnung true, if best score in that discipline
   */
  case class LeafRow(title: String, sum: Resultat, rang: Resultat, auszeichnung: Boolean) extends DataRow with ResultRow

  /**
   * Row of results per each discipline of one athlet/team
   *
   * @param athlet
   * @param resultate
   * @param sum
   * @param rang
   * @param auszeichnung
   */
  case class GroupRow(athlet: AthletView, pgm: ProgrammView, override val resultate: IndexedSeq[LeafRow], sum: Resultat, rang: Resultat, auszeichnung: Boolean) extends DataRow with ResultRow {
    lazy val withDNotes = resultate.exists(w => w.sum.noteD > 0)
    override val athletId = Some(athlet.id)
    override val divider = if (withDNotes || resultate.isEmpty) 1 else resultate.count { r => r.sum.endnote > 0 }
  }

  case class TeamRow(team: Team, override val resultate: IndexedSeq[LeafRow], sum: Resultat, rang: Resultat, auszeichnung: Boolean) extends DataRow with ResultRow {
    lazy val withDNotes = resultate.exists(w => w.sum.noteD > 0)
    override val divider = if (withDNotes || resultate.isEmpty) 1 else resultate.count { r => r.sum.endnote > 0 }
  }

  sealed trait NotenModus {
    def getDifficultLabel: String = "D"

    def getExecutionLabel: String = "E"

    def selectableItems: Option[List[String]] = None

    def validated(dnote: Double, enote: Double, wettkampfDisziplin: WettkampfdisziplinView): (Double, Double)

    def calcEndnote(dnote: Double, enote: Double, wettkampfDisziplin: WettkampfdisziplinView): Double

    def toString(value: Double): String = if (value.toString == Double.NaN.toString) ""
    else value

    /*override*/ def shouldSuggest(item: String, query: String): Boolean = false
  }

  case class StandardWettkampf(punktgewicht: Double, dNoteLabel: String = "D", eNoteLabel: String = "E") extends NotenModus {
    override def validated(dnote: Double, enote: Double, wettkampfDisziplin: WettkampfdisziplinView): (Double, Double) = {
      val dnoteValidated = if (wettkampfDisziplin.isDNoteUsed) BigDecimal(dnote).setScale(wettkampfDisziplin.scale, BigDecimal.RoundingMode.FLOOR).max(wettkampfDisziplin.min).min(wettkampfDisziplin.max).toDouble else 0d
      val enoteValidated = BigDecimal(enote).setScale(wettkampfDisziplin.scale, BigDecimal.RoundingMode.FLOOR).max(wettkampfDisziplin.min).min(wettkampfDisziplin.max).toDouble
      (dnoteValidated, enoteValidated)
    }

    override def calcEndnote(dnote: Double, enote: Double, wettkampfDisziplin: WettkampfdisziplinView) = {
      val dnoteValidated = if (wettkampfDisziplin.isDNoteUsed) dnote else 0d
      BigDecimal(dnoteValidated + enote).setScale(wettkampfDisziplin.scale, BigDecimal.RoundingMode.FLOOR).*(punktgewicht).max(wettkampfDisziplin.min).min(wettkampfDisziplin.max).toDouble
    }

    override def getDifficultLabel: String = dNoteLabel

    override def getExecutionLabel: String = eNoteLabel
  }

  case class Athletiktest(punktemapping: Map[String, Double], punktgewicht: Double) extends NotenModus {

    override def validated(dnote: Double, enote: Double, wettkampfDisziplin: WettkampfdisziplinView): (Double, Double) = (0, enote)

    override def calcEndnote(dnote: Double, enote: Double, wettkampfDisziplin: WettkampfdisziplinView) = enote * punktgewicht

    override def selectableItems: Option[List[String]] = Some(punktemapping.keys.toList.sortBy(punktemapping))
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

    def encArrToList(enc: String) = enc.split("-").flatMap(_.split("\\|")).toList

    def encode(name: String): Seq[String] =
      encArrToList(bmenc.encode(name)) ++
        encArrToList(bmenc2.encode(name)) ++
        encArrToList(bmenc3.encode(name)) ++
        Seq(NameCodec.encode(name), colenc.encode(name).mkString(""))

    def similarFactor(name1: String, name2: String, threshold: Int = 80) = {
      val diff = LevenshteinDistance.getDefaultInstance.apply(name1, name2)
      val diffproz = 100 * diff / name1.length()
      val similar = 100 - diffproz
      if (similar >= threshold) {
        similar
      }
      else {
        0
      }
    }
  }

  case class MatchCode(id: Long, name: String, vorname: String, gebdat: Option[java.sql.Date], verein: Long) {

    import MatchCode._

    val jahrgang = AthletJahrgang(gebdat).jahrgang
    val encodedNamen = encode(name)
    val encodedVorNamen = encode(vorname)

    def swappednames = MatchCode(id, vorname, name, gebdat, verein)
  }

  case class Kandidat(wettkampfTitel: String, geschlecht: String, programm: String, id: Long,
                      name: String, vorname: String, jahrgang: String, verein: String, einteilung: Option[Riege], einteilung2: Option[Riege], diszipline: Seq[Disziplin], diszipline2: Seq[Disziplin], wertungen: Seq[WertungView]) {
    def matches(w1: Wertung, w2: WertungView): Boolean = {
      w2.wettkampfdisziplin.id == w1.wettkampfdisziplinId && w2.athlet.id == w1.athletId
    }

    def indexOf(wertung: Wertung): Int = wertungen.indexWhere(w => matches(wertung, w))

    def updated(idx: Int, wertung: Wertung): Kandidat = {
      if (idx > -1 && matches(wertung, wertungen(idx)))
        copy(wertungen = wertungen.updated(idx, wertungen(idx).updatedWertung(wertung)))
      else this
    }
  }

  case class GeraeteRiege(wettkampfTitel: String, wettkampfUUID: String, durchgang: Option[String], halt: Int, disziplin: Option[Disziplin], kandidaten: Seq[Kandidat], erfasst: Boolean, sequenceId: String) {
    private val hash: Long = {
      Seq(wettkampfUUID,
        durchgang,
        halt, disziplin).hashCode()
    }

    def updated(wertung: Wertung): GeraeteRiege = {
      kandidaten.foldLeft((false, Seq[Kandidat]()))((acc, kandidat) => {
        if (acc._1) (acc._1, acc._2 :+ kandidat) else {
          val idx = kandidat.indexOf(wertung)
          if (idx > -1)
            (true, acc._2 :+ kandidat.updated(idx, wertung))
          else (acc._1, acc._2 :+ kandidat)
        }
      }) match {
        case (found, kandidaten) if found => copy(kandidaten = kandidaten)
        case _ => this
      }
    }

    def caption: String = {
      s"(${sequenceId}) ${durchgang.getOrElse("")}: ${disziplin.map(_.name).getOrElse("")}, ${halt + 1}. Gerät"
    }

    def softEquals(other: GeraeteRiege): Boolean = {
      hash == other.hash
    }
  }

  sealed trait SexDivideRule {
    val name: String

    override def toString: String = name
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

  type OverviewStatTuple = (String, String, Int, Int, Int)

  sealed trait SyncAction {
    val caption: String
    val verein: Registration
  }

  object PublicSyncAction {
    /**
     * Hides some attributes to protect privacy.
     * The product is only and only used by the web-client showing sync-states with some summary-infos
     *
     * @param syncation
     * @return transformed SyncAction toPublicView applied
     */
    def apply(syncation: SyncAction): SyncAction = syncation match {
      case AddVereinAction(verein) => AddVereinAction(verein.toPublicView)
      case ApproveVereinAction(verein) => ApproveVereinAction(verein.toPublicView)
      case RenameVereinAction(verein, oldVerein) => RenameVereinAction(verein.toPublicView, oldVerein)
      case rn@RenameAthletAction(verein, athlet, existing, expected) =>
        if (rn.isGebDatChange)
          RenameAthletAction(verein.toPublicView, athlet.toPublicView, existing.toPublicView.copy(gebdat = existing.gebdat), expected.toPublicView.copy(gebdat = expected.gebdat))
        else
          RenameAthletAction(verein.toPublicView, athlet.toPublicView, existing.toPublicView, expected.toPublicView)
      case AddRegistration(verein, programId, athlet, suggestion, team) => AddRegistration(verein.toPublicView, programId, athlet.toPublicView, suggestion.toPublicView, team)
      case MoveRegistration(verein, fromProgramId, fromTeam, toProgramid, toTeam, athlet, suggestion) => MoveRegistration(verein.toPublicView, fromProgramId, fromTeam, toProgramid, toTeam, athlet.toPublicView, suggestion.toPublicView)
      case RemoveRegistration(verein, programId, athlet, suggestion) => RemoveRegistration(verein.toPublicView, programId, athlet.toPublicView, suggestion.toPublicView)
    }
  }

  case class AddVereinAction(override val verein: Registration) extends SyncAction {
    override val caption = s"Verein hinzufügen: ${verein.vereinname} (${verein.verband})"
  }

  case class RenameVereinAction(override val verein: Registration, oldVerein: Verein) extends SyncAction {
    override val caption = s"Verein korrigieren: ${oldVerein.easyprint}${oldVerein.verband.map(verband => s" ($verband)").getOrElse("")} zu ${verein.toVerein.easyprint}${if (verein.verband.nonEmpty) s"${verein.verband})" else ""}"

    def prepareLocalUpdate: Verein = verein.toVerein.copy(id = oldVerein.id)

    def prepareRemoteUpdate: Option[Verein] = verein.selectedInitialClub.map(club => verein.toVerein.copy(id = club.id))
  }

  case class ApproveVereinAction(override val verein: Registration) extends SyncAction {
    override val caption = s"Verein bestätigen: ${verein.vereinname}"
  }

  case class AddRegistration(override val verein: Registration, programId: Long, athlet: Athlet, suggestion: AthletView, team: Int) extends SyncAction {
    override val caption = s"Neue Anmeldung verarbeiten: ${suggestion.easyprint}"
  }

  case class MoveRegistration(override val verein: Registration, fromProgramId: Long, fromTeam: Int, toProgramid: Long, toTeam: Int, athlet: Athlet, suggestion: AthletView) extends SyncAction {
    override val caption = if (fromTeam != toTeam && fromProgramId == toProgramid) s"Team Einteilung verarbeiten: ${suggestion.easyprint}"
    else s"Program-Umteilung verarbeiten: ${suggestion.easyprint}"
  }

  case class RemoveRegistration(override val verein: Registration, programId: Long, athlet: Athlet, suggestion: AthletView) extends SyncAction {
    override val caption = s"Abmeldung verarbeiten: ${suggestion.easyprint}"
  }

  case class NewRegistration(wettkampfId: Long, vereinname: String, verband: String, respName: String, respVorname: String, mobilephone: String, mail: String, secret: String) {
    def toRegistration: Registration = Registration(0, wettkampfId, None, vereinname, verband, respName, respVorname, mobilephone, mail, Timestamp.valueOf(LocalDateTime.now()).getTime)
  }

  case class Registration(id: Long, wettkampfId: Long, vereinId: Option[Long], vereinname: String, verband: String, respName: String, respVorname: String, mobilephone: String, mail: String, registrationTime: Long, selectedInitialClub: Option[Verein] = None) extends DataObject {
    def toVerein: Verein = Verein(0L, vereinname, Some(verband))

    def toPublicView: Registration = Registration(id, wettkampfId, vereinId, vereinname, verband, respName, respVorname, "***", "***", registrationTime)

    def matchesVerein(v: Verein): Boolean = {
      (v.name.equals(vereinname) && (v.verband.isEmpty || v.verband.get.equals(verband))) || selectedInitialClub.map(_.extendedprint).contains(v.extendedprint)
    }

    def matchesClubRelation(): Boolean = {
      selectedInitialClub.nonEmpty && selectedInitialClub.exists(v => (v.name.equals(vereinname) && (v.verband.isEmpty || v.verband.get.equals(verband))))
    }
  }

  case class RenameAthletAction(override val verein: Registration, athletReg: AthletRegistration, existing: Athlet, expected: Athlet) extends SyncAction {
    override val caption = s"Athlet/-In korrigieren: Von ${existing.extendedprint} zu ${expected.extendedprint}"

    def isSexChange: Boolean = existing.geschlecht != expected.geschlecht

    def isGebDatChange: Boolean = !existing.gebdat.equals(expected.gebdat)

    def applyLocalChange: Athlet = existing.copy(
      geschlecht = expected.geschlecht,
      name = expected.name,
      vorname = expected.vorname,
      gebdat = expected.gebdat
    )

    def applyRemoteChange: AthletView = athletReg
      .toAthlet
      .toAthletView(Some(verein
        .toVerein
        .copy(id = verein.vereinId.get)))
      .copy(
        id = athletReg.athletId.get,
        geschlecht = expected.geschlecht,
        name = expected.name,
        vorname = expected.vorname,
        gebdat = expected.gebdat
      )
  }

  case class RegistrationResetPW(id: Long, wettkampfId: Long, secret: String) extends DataObject

  case class AthletRegistration(id: Long, vereinregistrationId: Long,
                                athletId: Option[Long], geschlecht: String, name: String, vorname: String, gebdat: String,
                                programId: Long, registrationTime: Long, athlet: Option[AthletView], team: Option[Int]) extends DataObject {
    def toPublicView = AthletRegistration(id, vereinregistrationId, athletId, geschlecht, name, vorname, gebdat.substring(0, 4) + "-01-01", programId, registrationTime, athlet.map(_.toPublicView), team)

    def capitalizeIfBlockCase(s: String): String = {
      if (s.length > 2 && (s.toUpperCase.equals(s) || s.toLowerCase.equals(s))) {
        s.substring(0, 1).toUpperCase + s.substring(1).toLowerCase
      } else {
        s
      }
    }

    def toAthlet: Athlet = {
      if (id == 0 && !isLocalIdentified) {
        val nameNorm = capitalizeIfBlockCase(name.trim)
        val vornameNorm = capitalizeIfBlockCase(vorname.trim)
        val nameMasculinTest = Surname.isMasculin(nameNorm)
        val nameFeminimTest = Surname.isFeminim(nameNorm)
        val vornameMasculinTest = Surname.isMasculin(vornameNorm)
        val vornameFeminimTest = Surname.isFeminim(vornameNorm)
        val nameVornameSwitched = (nameMasculinTest || nameFeminimTest) && !(vornameMasculinTest || vornameFeminimTest)
        val defName = if (nameVornameSwitched) vornameNorm else nameNorm
        val defVorName = if (nameVornameSwitched) nameNorm else vornameNorm
        val feminim = nameFeminimTest || vornameFeminimTest
        val masculin = nameMasculinTest || vornameMasculinTest
        val defGeschlecht = geschlecht match {
          case "M" =>
            if (feminim && !masculin) "W" else "M"
          case "W" =>
            if (masculin && !feminim) "M" else "W"
          case s: String => "M"
        }
        val currentDate = LocalDate.now()
        val gebDatRaw = str2SQLDate(gebdat)
        val gebDatLocal = gebDatRaw.toLocalDate
        val age = Period.between(gebDatLocal, currentDate).getYears
        if (age > 0 && age < 120) {
          Athlet(
            id = athletId match {
              case Some(id) if id > 0 => id
              case _ => 0
            },
            js_id = "",
            geschlecht = defGeschlecht,
            name = defName,
            vorname = defVorName,
            gebdat = Some(gebDatRaw),
            strasse = "",
            plz = "",
            ort = "",
            verein = None,
            activ = true
          )
        } else {
          throw new IllegalArgumentException(s"Geburtsdatum ergibt ein unrealistisches Alter von ${age}.")
        }
      }
      else {
        val currentDate = LocalDate.now()
        val gebDatRaw = str2SQLDate(gebdat)
        val gebDatLocal = gebDatRaw.toLocalDate
        val age = Period.between(gebDatLocal, currentDate).getYears
        if (age > 0 && age < 120) {
          Athlet(
            id = athletId match {
              case Some(id) if id > 0 => id
              case _ => 0
            },
            js_id = "",
            geschlecht = geschlecht,
            name = name.trim,
            vorname = vorname.trim,
            gebdat = Some(gebDatRaw),
            strasse = "",
            plz = "",
            ort = "",
            verein = None,
            activ = true
          )
        } else {
          throw new IllegalArgumentException(s"Geburtsdatum ergibt ein unrealistisches Alter von ${age}.")
        }
      }
    }

    def isEmptyRegistration: Boolean = geschlecht.isEmpty

    def isLocalIdentified: Boolean = {
      athletId match {
        case Some(id) if id > 0L => true
        case _ => false
      }
    }

    def matchesAthlet(v: Athlet): Boolean = {
      val bool = toAthlet.extendedprint.equals(v.extendedprint)
      /*if(!bool) {
        println(s"nonmatch athlet: ${v.extendedprint}, ${toAthlet.extendedprint}")
      }*/
      bool
    }

    def matchesAthlet(): Boolean = {
      val bool = athlet.nonEmpty && athlet.map(_.toAthlet).exists(matchesAthlet)
      /*if(!bool) {
        println(s"nonmatch athlet: ${athlet.nonEmpty}, ${athlet.map(_.toAthlet)}, '${athlet.map(_.toAthlet.extendedprint)}' <> '${toAthlet.extendedprint}'")
      }*/
      bool
    }
  }

  object EmptyAthletRegistration {
    def apply(vereinregistrationId: Long): AthletRegistration = AthletRegistration(0L, vereinregistrationId, None, "", "", "", "", 0L, 0L, None, None)
  }

  case class JudgeRegistration(id: Long, vereinregistrationId: Long,
                               geschlecht: String, name: String, vorname: String,
                               mobilephone: String, mail: String, comment: String,
                               registrationTime: Long) extends DataObject {
    def validate(): Unit = {
      if (name == null || name.trim.isEmpty) throw new IllegalArgumentException("JudgeRegistration with empty name")
      if (vorname == null || vorname.trim.isEmpty) throw new IllegalArgumentException("JudgeRegistration with empty vorname")
      if (mobilephone == null || mobilephone.trim.isEmpty) throw new IllegalArgumentException("JudgeRegistration with empty mobilephone")
      if (mail == null || mail.trim.isEmpty) throw new IllegalArgumentException("JudgeRegistration with empty mail")
    }

    def normalized: JudgeRegistration = {
      validate()
      val nameNorm = name.trim
      val vornameNorm = vorname.trim
      val nameMasculinTest = Surname.isMasculin(nameNorm)
      val nameFeminimTest = Surname.isFeminim(nameNorm)
      val vornameMasculinTest = Surname.isMasculin(vornameNorm)
      val vornameFeminimTest = Surname.isFeminim(vornameNorm)
      val nameVornameSwitched = (nameMasculinTest || nameFeminimTest) && !(vornameMasculinTest || vornameFeminimTest)
      val defName = if (nameVornameSwitched) vornameNorm else nameNorm
      val defVorName = if (nameVornameSwitched) nameNorm else vornameNorm
      val feminim = nameFeminimTest || vornameFeminimTest
      val masculin = nameMasculinTest || vornameMasculinTest
      val defGeschlecht = geschlecht match {
        case "M" =>
          if (feminim && !masculin) "W" else "M"
        case "W" =>
          if (masculin && !feminim) "M" else "W"
        case s: String => "M"
      }
      JudgeRegistration(id, vereinregistrationId, defGeschlecht, defName, defVorName, mobilephone, mail, comment, registrationTime)
    }

    def toWertungsrichter: Wertungsrichter = {
      val nj = normalized
      Wertungsrichter(
        id = 0L,
        js_id = "",
        geschlecht = nj.geschlecht,
        name = nj.name,
        vorname = nj.vorname,
        gebdat = None,
        strasse = "",
        plz = "",
        ort = "",
        verein = None,
        activ = true
      )
    }

    def isEmptyRegistration = geschlecht.isEmpty
  }

  object EmptyJudgeRegistration {
    def apply(vereinregistrationId: Long) = JudgeRegistration(0L, vereinregistrationId, "", "", "", "", "", "", 0L)
  }

  case class JudgeRegistrationProgram(id: Long, judgeregistrationId: Long, vereinregistrationId: Long, program: Long, comment: String)

  case class JudgeRegistrationProgramItem(program: String, disziplin: String, disziplinId: Long)
}