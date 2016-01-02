package ch.seidel.kutu.data

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.InputStream
import java.io.{BufferedReader, FileOutputStream, FileInputStream, File}
import java.util.zip.{ZipEntry, ZipOutputStream, ZipInputStream}
import reflect.runtime.universe._
import scala.io.Source
import scala.annotation.tailrec
import ch.seidel.kutu.domain._

/**
 */
object ResourceExchanger extends KutuService {
  private val rm = reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  def importWettkampf(filename: String) = {
    type ZipStream = (ZipEntry,InputStream)
    class ZipEntryTraversableClass(filename: String) extends Traversable[ZipStream] {
      val zis = new ZipInputStream(new FileInputStream(filename))
      def entryIsValid(ze: ZipEntry) = !ze.isDirectory

      def foreach[U](f: ZipStream => U) {
        @tailrec
        def loop(x: ZipEntry): Unit = if (x != null && entryIsValid(x)) {
          f(x, zis)
          zis.closeEntry()
          loop(zis.getNextEntry())
        }
        loop(zis.getNextEntry())
      }
    }

    val zip: Traversable[ZipStream] = new ZipEntryTraversableClass(filename)
    val collection = zip.foldLeft(Map[String, (Seq[String],Map[String,Int])]()) { (acc, entry) =>
      val csv = Source.fromInputStream(entry._2, "utf-8").getLines().toList
      val header = csv.take(1).map(_.dropWhile {_.isUnicodeIdentifierPart }).flatMap(parseLine).zipWithIndex.toMap
      acc + (entry._1.getName -> (csv.drop(1), header))
    }

    val (vereinCsv, vereinHeader) = collection("vereine.csv")
    println(vereinHeader)
    val vereinNameIdx = vereinHeader("name")
    val vereinVerbandIdx = vereinHeader.getOrElse("verband", -1)
    val vereinIdIdx = vereinHeader("id")
    val vereinInstances = vereinCsv.map(parseLine).filter(_.size == vereinHeader.size).map{fields =>
      val candidate = Verein(id = 0, name = fields(vereinNameIdx), verband = if(vereinVerbandIdx > -1) Some(fields(vereinVerbandIdx)) else None)
      val verein = insertVerein(candidate)
      (fields(vereinIdIdx), verein)
    }.toMap
    println(vereinInstances.toList)

    val (athletCsv, athletHeader) = collection("athleten.csv")
    println(athletHeader)
    val athletInstances = athletCsv.map(parseLine).filter(_.size == athletHeader.size).map{fields =>
      val geb = fields(athletHeader("gebdat")).replace("Some(", "").replace(")","")
      val importathlet = Athlet(
          id = 0,
          js_id = fields(athletHeader("js_id")),
          geschlecht = fields(athletHeader("geschlecht")),
          name = fields(athletHeader("name")),
          vorname = fields(athletHeader("vorname")),
          gebdat = if(geb.length > 0) Some(geb) else None,
          strasse = fields(athletHeader("strasse")),
          plz = fields(athletHeader("plz")),
          ort = fields(athletHeader("ort")),
          verein = vereinInstances.get(fields(athletHeader("verein"))).map(v => v.id),
          activ = fields(athletHeader("activ")).toUpperCase() match {case "TRUE" => true case _ => false}
          )
      val candidate = findAthleteLike(importathlet)
      val athlet = if(candidate.id > 0 &&
                           (importathlet.gebdat match {
                             case Some(d) =>
                               candidate.gebdat match {
                                 case Some(cd) =>cd.toString().startsWith("01.01")
                                 case _        => true
                               }
                             case _ => false
                             })) {
         candidate
      }
      else {
         insertAthlete(candidate)
      }
      (fields(athletHeader("id")), athlet)
    }.toMap

    val (wettkampfCsv, wettkampfHeader) = collection("wettkampf.csv")
    println(wettkampfHeader)
    val wettkampfInstances = wettkampfCsv.map(parseLine).filter(_.size == wettkampfHeader.size).map{fields =>
      val wettkampf = createWettkampf(
          auszeichnung = fields(wettkampfHeader("auszeichnung")),
          auszeichnungendnote = try {BigDecimal.valueOf(fields(wettkampfHeader("auszeichnungendnote")))} catch {case e:Exception => 0},
          datum = fields(wettkampfHeader("datum")),
          programmId = Set(fields(wettkampfHeader("programmId"))),
          titel = fields(wettkampfHeader("titel"))
          )
      (fields(wettkampfHeader("id")), wettkampf)
    }.toMap

    val (wertungenCsv, wertungenHeader) = collection("wertungen.csv")
    wertungenCsv.map(parseLine).filter(_.size == wertungenHeader.size).foreach{fields =>
      val athletid: Long = fields(wertungenHeader("athletId"))
      val wettkampfid: Long = fields(wertungenHeader("wettkampfId"))
      val w = Wertung(
        id = fields(wertungenHeader("id")),
        athletId = athletInstances.get(athletid + "") match {
          case Some(a) => a.id
          case None => athletid
        },
        wettkampfdisziplinId = fields(wertungenHeader("wettkampfdisziplinId")),
        wettkampfId = wettkampfInstances.get(wettkampfid + "") match {
          case Some(w) => w.id
          case None => wettkampfid
        },
        noteD = BigDecimal.valueOf(fields(wertungenHeader("noteD"))),
        noteE = BigDecimal.valueOf(fields(wertungenHeader("noteE"))),
        endnote = BigDecimal.valueOf(fields(wertungenHeader("endnote"))),
        riege = if(fields(wertungenHeader("riege")).length > 0) Some(fields(wertungenHeader("riege"))) else None,
        riege2 = if(fields(wertungenHeader("riege2")).length > 0) Some(fields(wertungenHeader("riege2"))) else None
      )
      updateOrinsertWertung(w)
    }
    if(collection.contains("riegen.csv")) {
      val (riegenCsv, riegenHeader) = collection("riegen.csv")
      riegenCsv.map(parseLine).filter(_.size == riegenHeader.size).foreach{fields =>
        val riege = RiegeRaw(
            wettkampfId = fields(riegenHeader("wettkampfId")),
            r = fields(riegenHeader("r")),
            durchgang = if(fields(riegenHeader("durchgang")).length > 0) Some(fields(riegenHeader("durchgang"))) else None,
            start = if(fields(riegenHeader("start")).length > 0) Some(fields(riegenHeader("start"))) else None
            )
        updateOrinsertRiege(riege)
      }
    }
    wettkampfInstances.head._2
  }

  private def getCaseMethods[T: TypeTag] = typeOf[T].members.collect {
    case m: MethodSymbol if m.isCaseAccessor => m
  }.toList

  private def getHeader[T: TypeTag] = {
    val fields = getCaseMethods[T]
    fields.map(f => "\"" + f.name.encoded + "\"").mkString(",")
  }

  private def getValues[T: TypeTag: reflect.ClassTag](instance: T) = {
    val im = rm.reflect(instance)
    val values = typeOf[T].members.collect {
      case m: MethodSymbol if m.isCaseAccessor =>
        im.reflectMethod(m).apply() match {
          case Some(verein: Verein) => verein.id + ""
          case Some(programm: Programm) => programm.id + ""
          case Some(athlet: Athlet) => athlet.id + ""
          case Some(athlet: AthletView) => athlet.id + ""
          case Some(disziplin: Disziplin) => disziplin.id + ""
          case Some(value) => value.toString
          case None => ""
          case e => e.toString
        }
    }
    values.map("\"" + _ + "\"").mkString(",")
  }

  def exportWettkampf(wettkampf: Wettkampf, filename: String) {
    val zip = new ZipOutputStream(new FileOutputStream(filename));
    zip.putNextEntry(new ZipEntry("wettkampf.csv"));
    zip.write((getHeader[Wettkampf] + "\n" + getValues(wettkampf)).getBytes("utf-8"))
    zip.closeEntry()

    val wertungen = selectWertungen(wettkampfId = Some(wettkampf.id))

    val vereine = wertungen.flatMap(_.athlet.verein).toSet
    zip.putNextEntry(new ZipEntry("vereine.csv"));
    zip.write((getHeader[Verein] + "\n").getBytes("utf-8"))
    for(verein <- vereine) {
      zip.write((getValues(verein) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val athleten = wertungen.map(_.athlet).toSet
    zip.putNextEntry(new ZipEntry("athleten.csv"));
    zip.write((getHeader[Athlet] + "\n").getBytes("utf-8"))
    for(athlet <- athleten) {
      zip.write((getValues(athlet) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val wertungenRaw = wertungen.map(_.toWertung)
    zip.putNextEntry(new ZipEntry("wertungen.csv"));
    zip.write((getHeader[Wertung] + "\n").getBytes("utf-8"))
    for(wertung <- wertungenRaw) {
      zip.write((getValues(wertung) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    val riegenRaw = selectRiegenRaw(wettkampf.id)
    zip.putNextEntry(new ZipEntry("riegen.csv"));
    zip.write((getHeader[Riege] + "\n").getBytes("utf-8"))
    for(riege <- riegenRaw) {
      zip.write((getValues(riege) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    zip.finish()
    zip.close()
  }
}