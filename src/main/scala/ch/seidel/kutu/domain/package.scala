package ch.seidel.kutu

import ch.seidel.kutu.data.{NameCodec, Surname}
import org.apache.commons.codec.language.ColognePhonetic
import org.apache.commons.codec.language.bm._
import org.apache.commons.text.similarity.LevenshteinDistance

import java.net.URLEncoder
import java.nio.file.{Files, LinkOption, StandardOpenOption}
import java.sql.{Date, Timestamp}
import java.text.{ParseException, SimpleDateFormat}
import java.time.{LocalDate, LocalDateTime, Period, ZoneId}
import java.util.UUID
import java.util.concurrent.TimeUnit
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

  object RiegeRaw {
    val KIND_STANDARD = 0;
    val KIND_EMPTY_RIEGE = 1;
  }

  case class RiegeRaw(wettkampfId: Long, r: String, durchgang: Option[String], start: Option[Long], kind: Int) extends DataObject {
    override def easyprint = r
  }

  case class Riege(r: String, durchgang: Option[String], start: Option[Disziplin], kind: Int) extends DataObject {
    override def easyprint = r

    def toRaw(wettkampfId: Long) = RiegeRaw(wettkampfId, r, durchgang, start.map(_.id), kind)
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
      case Some(d) => f"$d%tY ";
      case _ => ""
    })
    def extendedprint: String = geschlecht match {
      case "W" => s"Ti ${easyprint}"
      case _ => s"Tu ${easyprint}"
    }
    def toPublicView: Athlet = {
      Athlet(id, 0, geschlecht, name, vorname, gebdat
        .map(d => sqlDate2ld(d))
        .map(ld => LocalDate.of(ld.getYear, 1,1))
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
    def extendedprint: String = geschlecht match {
      case "W" => s"Ti ${easyprint}"
      case _ => s"Tu ${easyprint}"
    }
    def toPublicView: AthletView = {
      AthletView(id, 0, geschlecht, name, vorname, gebdat
        .map(d => sqlDate2ld(d))
        .map(ld => LocalDate.of(ld.getYear, 1,1))
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
    def updatedWith(athlet: Athlet) = AthletView(athlet.id, athlet.js_id, athlet.geschlecht, athlet.name, athlet.vorname, athlet.gebdat, athlet.strasse, athlet.plz, athlet.ort, verein.map(v => v.copy(id=athlet.verein.getOrElse(0L))), athlet.activ)
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

  case class Durchgang(id: Long, wettkampfId: Long, title: String, name: String, durchgangtype: DurchgangType, ordinal: Int, planStartOffset: Long, effectiveStartTime: Option[java.sql.Timestamp], effectiveEndTime: Option[java.sql.Timestamp], planEinturnen: Long, planGeraet: Long, planTotal: Long) extends DataObject {
    override def easyprint = name

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

  case class ProgrammView(id: Long, name: String, aggregate: Int, parent: Option[ProgrammView], ord: Int, alterVon: Int, alterBis: Int) extends Programm {
    //override def easyprint = toPath

    def head: ProgrammView = parent match {
      case None => this
      case Some(p) => p.head
    }

    def wettkampfprogramm: ProgrammView = if (aggregator == this) this else head

    def aggregatorHead: ProgrammView = parent match {
      case Some(p) if (aggregate != 0) => p.aggregatorHead
      case _ => this
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

    override def toString = toPath
  }

  //  object Wettkampf {
  //    def apply(id: Long, datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal): Wettkampf =
  //      Wettkampf(id, datum, titel, programmId, auszeichnung, auszeichnungendnote, if(id == 0) Some(UUID.randomUUID().toString()) else None)
  //    def apply(id: Long, datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, uuid: String): Wettkampf =
  //      if(uuid != null) Wettkampf(id, datum, titel, programmId, auszeichnung, auszeichnungendnote, Some(uuid))
  //      else apply(id, datum, titel, programmId, auszeichnung, auszeichnungendnote)
  //  }
  case class Wettkampf(id: Long, uuid: Option[String], datum: java.sql.Date, titel: String, programmId: Long, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, notificationEMail: String) extends DataObject {

    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"

    def toView(programm: ProgrammView): WettkampfView = {
      WettkampfView(id, uuid, datum, titel, programm, auszeichnung, auszeichnungendnote, notificationEMail)
    }

    def toPublic: Wettkampf = Wettkampf(id, uuid, datum, titel, programmId, auszeichnung, auszeichnung, "")

    private def prepareFilePath(homedir: String) = {
      val dir = new java.io.File(homedir + "/" + easyprint.replace(" ", "_"))
      if (!dir.exists) {
        dir.mkdirs
      }
      dir
    }

    def filePath(homedir: String, origin: String) = new java.io.File(prepareFilePath(homedir), ".at." + origin).toPath

    def fromOriginFilePath(homedir: String, origin: String) = new java.io.File(prepareFilePath(homedir), ".from." + origin).toPath

    def saveRemoteOrigin(homedir: String, origin: String): Unit = {
      val path = fromOriginFilePath(homedir, origin)
      val fos = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)
      try {
        fos.write(uuid.toString.getBytes("utf-8"))
        fos.flush
      } finally {
        fos.close
      }
      val os = System.getProperty("os.name").toLowerCase
      if (os.indexOf("win") > -1) {
        Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)
      }
    }

    def hasRemote(homedir: String, origin: String): Boolean = {
      val path = fromOriginFilePath(homedir, origin)
      path.toFile.exists
    }

    def removeRemote(homedir: String, origin: String): Unit = {
      val atFile = fromOriginFilePath(homedir, origin).toFile
      if (atFile.exists) {
        atFile.delete()
      }
    }

    def saveSecret(homedir: String, origin: String, secret: String): Unit = {
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
  case class WettkampfView(id: Long, uuid: Option[String], datum: java.sql.Date, titel: String, programm: ProgrammView, auszeichnung: Int, auszeichnungendnote: scala.math.BigDecimal, notificationEMail: String) extends DataObject {
    override def easyprint = f"$titel am $datum%td.$datum%tm.$datum%tY"

    def toWettkampf = Wettkampf(id, uuid, datum, titel, programm.id, auszeichnung, auszeichnungendnote, notificationEMail)
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

  case class Wettkampfdisziplin(id: Long, programmId: Long, disziplinId: Long, kurzbeschreibung: String, detailbeschreibung: Option[java.sql.Blob], notenfaktor: scala.math.BigDecimal, masculin: Int, feminim: Int, ord: Int) extends DataObject {
    override def easyprint = f"$disziplinId%02d: $kurzbeschreibung"
  }

  case class WettkampfdisziplinView(id: Long, programm: ProgrammView, disziplin: Disziplin, kurzbeschreibung: String, detailbeschreibung: Option[Array[Byte]], notenSpez: NotenModus, masculin: Int, feminim: Int, ord: Int) extends DataObject {
    override def easyprint = disziplin.name

    def toWettkampdisziplin = Wettkampfdisziplin(id, programm.id, disziplin.id, kurzbeschreibung, None, notenSpez.calcEndnote(0, 1, 0), masculin, feminim, ord)
  }

  case class WettkampfPlanTimeRaw(id: Long, wettkampfId: Long, wettkampfDisziplinId: Long, wechsel: Long, einturnen: Long, uebung: Long, wertung: Long) extends DataObject {
    override def easyprint = f"WettkampfPlanTime(disz=$wettkampfDisziplinId, w=$wechsel, e=$einturnen, u=$uebung, w=$wertung)"
  }

  case class WettkampfPlanTimeView(id: Long, wettkampf: Wettkampf, wettkampfdisziplin: WettkampfdisziplinView, wechsel: Long, einturnen: Long, uebung: Long, wertung: Long) extends DataObject {
    override def easyprint = f"WettkampfPlanTime(wk=$wettkampf, disz=$wettkampfdisziplin, w=$wechsel, e=$einturnen, u=$uebung, w=$wertung)"

    def toWettkampfPlanTimeRaw = WettkampfPlanTimeRaw(id, wettkampf.id, wettkampfdisziplin.id, wechsel, einturnen, uebung, wertung)
  }

  case class Resultat(noteD: scala.math.BigDecimal, noteE: scala.math.BigDecimal, endnote: scala.math.BigDecimal) extends DataObject {
    def +(r: Resultat) = Resultat(noteD + r.noteD, noteE + r.noteE, endnote + r.endnote)

    def /(cnt: Int) = Resultat(noteD / cnt, noteE / cnt, endnote / cnt)

    def *(cnt: Long) = Resultat(noteD * cnt, noteE * cnt, endnote * cnt)

    lazy val formattedD = if (noteD > 0) f"${noteD}%4.2f" else ""
    lazy val formattedE = if (noteE > 0) f"${noteE}%4.2f" else ""
    lazy val formattedEnd = if (endnote > 0) f"${endnote}%6.2f" else ""

    override def easyprint = f"${formattedD}%6s${formattedE}%6s${formattedEnd}%6s"
  }

  case class Wertung(id: Long, athletId: Long, wettkampfdisziplinId: Long, wettkampfId: Long, wettkampfUUID: String, noteD: Option[scala.math.BigDecimal], noteE: Option[scala.math.BigDecimal], endnote: Option[scala.math.BigDecimal], riege: Option[String], riege2: Option[String]) extends DataObject {
    lazy val resultat = Resultat(noteD.getOrElse(0), noteE.getOrElse(0), endnote.getOrElse(0))

    def updatedWertung(valuesFrom: Wertung) = copy(noteD = valuesFrom.noteD, noteE = valuesFrom.noteE, endnote = valuesFrom.endnote)

    def valueAsText(valueOption: Option[BigDecimal]) = valueOption match {
      case None => ""
      case Some(value) => value.toString()
    }

    def noteDasText = valueAsText(noteD)

    def noteEasText = valueAsText(noteE)

    def endnoteeAsText = valueAsText(endnote)
  }

  case class WertungView(id: Long, athlet: AthletView, wettkampfdisziplin: WettkampfdisziplinView, wettkampf: Wettkampf, noteD: Option[scala.math.BigDecimal], noteE: Option[scala.math.BigDecimal], endnote: Option[scala.math.BigDecimal], riege: Option[String], riege2: Option[String]) extends DataObject {
    lazy val resultat = Resultat(noteD.getOrElse(0), noteE.getOrElse(0), endnote.getOrElse(0))

    def +(r: Resultat) = resultat + r

    def toWertung = Wertung(id, athlet.id, wettkampfdisziplin.id, wettkampf.id, wettkampf.uuid.getOrElse(""), noteD, noteE, endnote, riege, riege2)

    def toWertung(riege: String, riege2: Option[String]) = Wertung(id, athlet.id, wettkampfdisziplin.id, wettkampf.id, wettkampf.uuid.getOrElse(""), noteD, noteE, endnote, Some(riege), riege2)

    def updatedWertung(valuesFrom: Wertung) = copy(noteD = valuesFrom.noteD, noteE = valuesFrom.noteE, endnote = valuesFrom.endnote)

    def validatedResult(dv: Double, ev: Double) = {
      val w = toWertung
      val scale = wettkampfdisziplin.notenSpez.defaultScale(w)
      val (d, e) = wettkampfdisziplin.notenSpez.validated(dv, ev, scale)
      Resultat(d, e, wettkampfdisziplin.notenSpez.calcEndnote(d, e, scale))
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

  case class LeafRow(title: String, sum: Resultat, rang: Resultat, auszeichnung: Boolean) extends DataRow

  case class GroupRow(athlet: AthletView, resultate: IndexedSeq[LeafRow], sum: Resultat, rang: Resultat, auszeichnung: Boolean) extends DataRow {
    lazy val withDNotes = resultate.exists(w => w.sum.noteD > 0)
    lazy val divider = if (withDNotes || resultate.isEmpty) 1 else resultate.count { r => r.sum.endnote > 0 }
  }

  sealed trait NotenModus /*with AutoFillTextBoxFactory.ItemComparator[String]*/ {
    val isDNoteUsed: Boolean

    def defaultScale(wertung: Wertung): Int = 2

    def selectableItems: Option[List[String]] = None

    def validated(dnote: Double, enote: Double, scale: Int): (Double, Double)

    def calcEndnote(dnote: Double, enote: Double, scale: Int): Double

    def verifiedAndCalculatedWertung(wertung: Wertung): Wertung = {
      if (wertung.noteE.isEmpty) {
        wertung.copy(noteD = None, noteE = None, endnote = None)
      } else {
        val scale = defaultScale(wertung)
        val (d, e) = validated(wertung.noteD.getOrElse(BigDecimal(0)).doubleValue, wertung.noteE.getOrElse(BigDecimal(0)).doubleValue, scale)
        wertung.copy(noteD = Some(d), noteE = Some(e), endnote = Some(calcEndnote(d, e, scale)))
      }
    }

    def toString(value: Double): String = if (value.toString == Double.NaN.toString) ""
    else value

    /*override*/ def shouldSuggest(item: String, query: String): Boolean = false
  }

  case class Athletiktest(punktemapping: Map[String, Double], punktgewicht: Double) extends NotenModus {
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
    override def validated(dnote: Double, enote: Double, scale: Int): (Double, Double) = (0, enote)

    override def calcEndnote(dnote: Double, enote: Double, scale: Int) = enote * punktgewicht

    override def selectableItems: Option[List[String]] = Some(punktemapping.keys.toList.sortBy(punktemapping))
  }

  case object KuTuWettkampf extends NotenModus {
    override val isDNoteUsed = true
    override def defaultScale(wertung: Wertung): Int = 3
    //override def fromString(input: String) = super.fromString(input)
    override def validated(dnote: Double, enote: Double, scale: Int): (Double, Double) =
      (BigDecimal(dnote).setScale(scale, BigDecimal.RoundingMode.FLOOR).max(0).min(30).toDouble,
        BigDecimal(enote).setScale(scale, BigDecimal.RoundingMode.FLOOR).max(0).min(30).toDouble)

    override def calcEndnote(dnote: Double, enote: Double, scale: Int) =
      BigDecimal(dnote + enote).setScale(scale, BigDecimal.RoundingMode.FLOOR).max(0).min(30).toDouble
  }

  case object GeTuWettkampf extends NotenModus {
    override val isDNoteUsed = false
    val scaleExceptions = Set(100L, 141L) // Sprung K6/K7
    override def defaultScale(wertung: Wertung): Int = {
      if (scaleExceptions.contains(wertung.wettkampfdisziplinId)) {
        3 // 3 Stellen nach dem Komma K6/K7 Sprung
      } else {
        super.defaultScale(wertung)
      }
    }

    //override def fromString(input: String) = super.fromString(input)
    def validated(dnote: Double, enote: Double, scale: Int): (Double, Double) =
      (BigDecimal(dnote).setScale(scale, BigDecimal.RoundingMode.FLOOR).max(0).min(10).toDouble,
        BigDecimal(enote).setScale(scale, BigDecimal.RoundingMode.FLOOR).max(0).min(10).toDouble)

    def calcEndnote(dnote: Double, enote: Double, scale: Int) =
      BigDecimal(enote).setScale(scale, BigDecimal.RoundingMode.FLOOR).max(0).min(10).toDouble
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
      kandidaten.foldLeft((false, Seq[Kandidat]()))((acc,kandidat) => {
        if (acc._1) (acc._1, acc._2 :+ kandidat) else {
          val idx = kandidat.indexOf(wertung)
          if(idx > -1)
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
     * @param syncation
     * @return transformed SyncAction toPublicView applied
     */
    def apply(syncation: SyncAction): SyncAction = syncation match {
      case AddVereinAction(verein) => AddVereinAction(verein.toPublicView)
      case ApproveVereinAction(verein) => ApproveVereinAction(verein.toPublicView)
      case RenameVereinAction(verein, oldVerein) => RenameVereinAction(verein.toPublicView, oldVerein)
      case RenameAthletAction(verein, athlet, existing, expected) => RenameAthletAction(verein.toPublicView, athlet.toPublicView, existing.toPublicView, expected.toPublicView)
      case AddRegistration(verein, programId, athlet, suggestion) => AddRegistration(verein.toPublicView, programId, athlet.toPublicView, suggestion.toPublicView)
      case MoveRegistration(verein, fromProgramId, toProgramid, athlet, suggestion) => MoveRegistration(verein.toPublicView, fromProgramId, toProgramid, athlet.toPublicView, suggestion.toPublicView)
      case RemoveRegistration(verein, programId, athlet, suggestion) => RemoveRegistration(verein.toPublicView, programId, athlet.toPublicView, suggestion.toPublicView)
    }
  }
  case class AddVereinAction(override val verein: Registration) extends SyncAction {
    override val caption = s"Verein hinzufügen: ${verein.vereinname}"
  }
  case class RenameVereinAction(override val verein: Registration, oldVerein: Verein) extends SyncAction {
    override val caption = s"Verein korrigieren: ${oldVerein.easyprint} zu ${verein.toVerein.easyprint}"
    def prepareLocalUpdate: Verein = verein.toVerein.copy(id = oldVerein.id)
    def prepareRemoteUpdate: Option[Verein] = verein.selectedInitialClub.map(club => verein.toVerein.copy(id = club.id))
  }
  case class ApproveVereinAction(override val verein: Registration) extends SyncAction {
    override val caption = s"Verein bestätigen: ${verein.vereinname}"
  }
  case class AddRegistration(override val verein: Registration, programId: Long, athlet: Athlet, suggestion: AthletView) extends SyncAction {
    override val caption = s"Neue Anmeldung verarbeiten: ${suggestion.easyprint}"
  }
  case class MoveRegistration(override val verein: Registration, fromProgramId: Long, toProgramid: Long, athlet: Athlet, suggestion: AthletView) extends SyncAction {
    override val caption = s"Umteilung verarbeiten: ${suggestion.easyprint}"
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
                                programId: Long, registrationTime: Long, athlet: Option[AthletView]) extends DataObject {
    def toPublicView = AthletRegistration(id, vereinregistrationId, athletId, geschlecht, name, vorname, gebdat.substring(0,4) + "-01-01", programId, registrationTime, athlet.map(_.toPublicView))
    def capitalizeIfBlockCase(s: String): String = {
      if (s.length > 2 && (s.toUpperCase.equals(s) || s.toLowerCase.equals(s))) {
        s.substring(0,1).toUpperCase + s.substring(1).toLowerCase
      } else {
        s
      }
    }

    def toAthlet: Athlet = {
      if(id == 0 && athletId == None) {
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
            if(feminim && !masculin) "W" else "M"
          case "W" =>
            if(masculin && !feminim) "M" else "W"
          case s: String => "M"
        }
        val currentDate = LocalDate.now()
        val gebDatRaw = str2SQLDate(gebdat)
        val gebDatLocal = gebDatRaw.toLocalDate
        val age = Period.between(gebDatLocal, currentDate).getYears
        if (age > 0 && age < 120) {
          Athlet(
            id = athletId match{case Some(id) => id case None => 0},
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
            id = athletId match{case Some(id) => id case None => 0},
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
      if(!bool) {
        println(s"nonmatch athlet: ${v.extendedprint}, ${toAthlet.extendedprint}")
      }
      bool
    }
    def matchesAthlet(): Boolean = {
      val bool = athlet.nonEmpty && athlet.map(_.toAthlet).exists(matchesAthlet)
      if(!bool) {
        println(s"nonmatch athlet: ${athlet.nonEmpty}, ${athlet.map(_.toAthlet)}, '${athlet.map(_.toAthlet.extendedprint)}' <> '${toAthlet.extendedprint}'")
      }
      bool
    }
  }

  object EmptyAthletRegistration {
    def apply(vereinregistrationId: Long): AthletRegistration = AthletRegistration(0L, vereinregistrationId, None, "", "", "", "", 0L, 0L, None)
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
          if(feminim && !masculin) "W" else "M"
        case "W" =>
          if(masculin && !feminim) "M" else "W"
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