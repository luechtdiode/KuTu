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
import ch.seidel.kutu.view._
import ch.seidel.kutu.squad.RiegenBuilder
import org.slf4j.LoggerFactory

/**
 */
object ResourceExchanger extends KutuService with RiegenBuilder {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val rm = reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  def importWettkampf(file: InputStream) = {
    type ZipStream = (ZipEntry,InputStream)
    class ZipEntryTraversableClass extends Traversable[ZipStream] {
      val zis = new ZipInputStream(file)
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

    val zip: Traversable[ZipStream] = new ZipEntryTraversableClass()
    val collection = zip.foldLeft(Map[String, (Seq[String],Map[String,Int])]()) { (acc, entry) =>
      val csv = Source.fromInputStream(entry._2, "utf-8").getLines().toList
      val header = csv.take(1).map(_.dropWhile {_.isUnicodeIdentifierPart }).flatMap(parseLine).zipWithIndex.toMap
      acc + (entry._1.getName -> (csv.drop(1), header))
    }

    val (vereinCsv, vereinHeader) = collection("vereine.csv")
    logger.debug("importing vereine ...", vereinHeader)
    val vereinNameIdx = vereinHeader("name")
    val vereinVerbandIdx = vereinHeader.getOrElse("verband", -1)
    val vereinIdIdx = vereinHeader("id")
    val vereinInstances = vereinCsv.map(parseLine).filter(_.size == vereinHeader.size).map{fields =>
      val candidate = Verein(id = 0, name = fields(vereinNameIdx), verband = if(vereinVerbandIdx > -1) Some(fields(vereinVerbandIdx)) else None)
      val verein = insertVerein(candidate)
      (fields(vereinIdIdx), verein)
    }.toMap

    val (athletCsv, athletHeader) = collection("athleten.csv")
    logger.debug("importing athleten ...", athletHeader)
    val mappedAthletes = athletCsv.map(parseLine).filter(_.size == athletHeader.size).map{fields =>
      val geb = fields(athletHeader("gebdat")).replace("Some(", "").replace(")","")
      (fields(athletHeader("id")), Athlet(
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
          ))
    }
    val cache = new java.util.ArrayList[MatchCode]()
    val athletInstanceCandidates = mappedAthletes.map{imported =>
      val (csvId, importathlet) = imported
      val candidate = findAthleteLike(cache)(importathlet)

      if(candidate.id > 0) {
        importathlet.gebdat match {
        case Some(d) =>
          candidate.gebdat match {
            case Some(cd) if(!f"${cd}%tF".endsWith("-01-01") || d.equals(cd)) =>
              (csvId, candidate, false)
            case _ =>
              (csvId, candidate.copy(gebdat = importathlet.gebdat), true)
          }
        case None =>
          (csvId, candidate, false)
        }
      }
      else {
         (csvId, candidate, true)
      }
    }
    val athletInstances = (athletInstanceCandidates
      .filter(x => !x._3)
      .map{toInsert =>
        val (key, candidate, _) = toInsert
        (key, candidate)
      } ++ insertAthletes(athletInstanceCandidates.filter(x => x._3).map{toInsert =>
        val (key, candidate, _) = toInsert
        (key, candidate)
      }))
      .toMap
      
    val (wettkampfCsv, wettkampfHeader) = collection("wettkampf.csv")
    logger.debug("importing wettkampf ...", wettkampfHeader)
    val wettkampfInstances = wettkampfCsv.map(parseLine).filter(_.size == wettkampfHeader.size).map{fields =>
      val uuid = wettkampfHeader.get("uuid").map(uuidIdx => Some(fields(uuidIdx))).getOrElse(None)
      logger.debug("wettkampf uuid: " + uuid)
      val wettkampf = createWettkampf(
          auszeichnung = fields(wettkampfHeader("auszeichnung")),
          auszeichnungendnote = try {BigDecimal.valueOf(fields(wettkampfHeader("auszeichnungendnote")))} catch {case e:Exception => 0},
          datum = fields(wettkampfHeader("datum")),
          programmId = Set(fields(wettkampfHeader("programmId"))),
          titel = fields(wettkampfHeader("titel")),
          uuidOption = uuid
          )
      (fields(wettkampfHeader("id")), wettkampf)
    }.toMap
    
    val wkdisziplines = wettkampfInstances.map{w =>
      (w._2.id, listWettkampfDisziplines(w._2.id).map(d => d.id -> d).toMap)
    }
    def getAthletName(athletid: Long): String = {
      athletInstances.get(athletid + "") match {
        case Some(a) => a.easyprint
        case None => athletInstances.find(a => a._2.id == athletid).map(_._2.easyprint).getOrElse(athletid + "")
      }
    }
    def getWettkampfDisziplinName(w: Wertung): String = {
      wkdisziplines(w.wettkampfId)(w.wettkampfdisziplinId).kurzbeschreibung
    }
    val (wertungenCsv, wertungenHeader) = collection("wertungen.csv")
    logger.debug("importing wertungen ...", wertungenHeader)
    val wertungInstances = wertungenCsv.map(parseLine).filter(_.size == wertungenHeader.size).map{fields =>
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
        wettkampfUUID = wettkampfInstances.get(wettkampfid + "") match {
          case Some(w) => w.uuid.getOrElse("")
          case None => ""
        },
        noteD = fields(wertungenHeader("noteD")),
        noteE = fields(wertungenHeader("noteE")),
        endnote = fields(wertungenHeader("endnote")),
        riege = if(fields(wertungenHeader("riege")).length > 0) Some(fields(wertungenHeader("riege"))) else None,
        riege2 = if(fields(wertungenHeader("riege2")).length > 0) Some(fields(wertungenHeader("riege2"))) else None
      )
//      println(w.athletId, getAthletName(w.athletId), w.endnote, w.wettkampfdisziplinId, w.wettkampfdisziplinId, getWettkampfDisziplinName(w))
      w
    }
    val start = System.currentTimeMillis()
    val inserted = wertungInstances.groupBy(w => w.wettkampfId).map{wkWertungen =>
      val (wettkampfid, wertungen) = wkWertungen
      val wettkampf = wettkampfInstances.values.find(w => w.id == wettkampfid).get
      val programmIds = readWettkampfLeafs(wettkampf.programmId).map(_.id).toSet
      updateOrinsertWertungenZuWettkampf(wettkampf, wertungen.groupBy(w => w.athletId).flatMap { aw =>
        val (athletid, wertungen) = aw
        val requiredDisciplines = wkdisziplines(wettkampf.id).filter(wd => programmIds.contains(wd._2.programmId))
        lazy val empty = wertungen.forall { w => w.endnote < 1 }
        val filtered = wertungen.filter(w => requiredDisciplines.contains(w.wettkampfdisziplinId))
        wertungen.filter(!filtered.contains(_)).foreach(w => logger.debug("WARNING: No matching Disciplin - " + w))
  
        val missing = requiredDisciplines.keys.filter(d => !filtered.exists(w => w.wettkampfdisziplinId == d))
        missing.foreach(w => logger.debug("WARNING: missing Disciplin - " + requiredDisciplines(w).easyprint))
        val completeWertungenSet = filtered ++ missing.map{missingDisziplin => 
          Wertung(
            id = 0L,
            athletId = athletid,
            wettkampfdisziplinId = missingDisziplin,
            wettkampfId = wettkampf.id,
            wettkampfUUID = wettkampfInstances.get(wettkampf.id + "") match {
              case Some(w) => w.uuid.getOrElse("")
              case None => ""
            },
            noteD = 0d,
            noteE = 0d,
            endnote = 0d,
            riege = None,
            riege2 = None
          )      
        }
        completeWertungenSet
      })
    }.sum
    // wertungen: 1857 / inserted: 1857, duration: 6335ms
    logger.debug(s"wertungen: ${wertungInstances.size} / inserted: $inserted, duration: ${System.currentTimeMillis() - start}ms")
    
    if(collection.contains("riegen.csv")) {
      val (riegenCsv, riegenHeader) = collection("riegen.csv")
      logger.debug("importing riegen ...", riegenHeader)
      updateOrinsertRiegen(riegenCsv.map(parseLine).filter(_.size == riegenHeader.size).map{fields =>
        val wettkampfid = fields(riegenHeader("wettkampfId"))
        val riege = RiegeRaw(
            wettkampfId = wettkampfInstances.get(wettkampfid + "") match {
              case Some(w) => w.id
              case None => wettkampfid
            },
            r = fields(riegenHeader("r")),
            durchgang = if(fields(riegenHeader("durchgang")).length > 0) Some(fields(riegenHeader("durchgang"))) else None,
            start = if(fields(riegenHeader("start")).length > 0) Some(fields(riegenHeader("start"))) else None
            )
        riege
      })
    }
    logger.debug("import finished")
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
    exportWettkampfToStream(wettkampf, new FileOutputStream(filename))
  }
  
  def exportWettkampfToStream(wettkampf: Wettkampf, os: OutputStream) {
    val zip = new ZipOutputStream(os);
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
    zip.write((getHeader[RiegeRaw] + "\n").getBytes("utf-8"))
    for(riege <- riegenRaw) {
      zip.write((getValues(riege) + "\n").getBytes("utf-8"))
    }
    zip.closeEntry()

    zip.finish()
    zip.close()
  }

  def exportEinheiten(wettkampf: Wettkampf, filename: String) {
    val export = new FileOutputStream(filename);
    val riegenRaw = suggestRiegen(Seq(0), selectWertungen(wettkampfId = Some(wettkampf.id)))
    val mapVereinVerband = selectVereine.map(v => v.name -> v.verband.getOrElse("")).toMap
    val sep = ";"
    def butify(grpkey: String, anzahl: Int) = {
      val parts = grpkey.split(",")
      // Verband, Verein, Kategorie, Geschlecht, Anzahl, Bezeichnung
      //            2         1           0
      mapVereinVerband(parts.drop(2).head) + sep + (parts.drop(2) :+ parts(1).split("//.")(0) :+ parts(0).replace("M", "Tu").replace("W", "Ti")).mkString(sep) + sep + anzahl + sep +
                      (parts.drop(2) :+ parts(1).split("//.")(0) :+ parts(0).replace("M", "(Tu)").replace("W", "(Ti)")).mkString(" ") + s" (${anzahl})"
    }
    def butifyATT(grpkey: String, anzahl: Int) = {
      val parts = grpkey.split(",")
      val geschl = parts(0).replace("M", "Tu").replace("W", "Ti")
      val jg = parts(1)
      val verein = parts(2)
      val kat = parts(3)
      val rearranged = Seq(verein, jg, geschl)
      // Verband, Verein, Jahrgang, Geschlecht, Anzahl, Bezeichnung
      //            2         1         0
      mapVereinVerband(verein) + sep + rearranged.mkString(sep) + sep + anzahl + sep +
                      rearranged.mkString(" ") + s" (${anzahl})"
    }
    val riegen = riegenRaw.map{r =>
      val anzahl = r._2.map(w => w.athletId).toSet.size
      if(wettkampf.programmId == 1) {
        butifyATT(r._1, anzahl)
      }
      else {
        butify(r._1, anzahl)
      }
    }
    if(wettkampf.programmId == 1) {
      export.write(f"sep=${sep}\nVerband${sep}Verein${sep}Jahrgang${sep}Geschlecht${sep}Anzahl${sep}Einheitsbezeichnung\n".getBytes("ISO-8859-1"))
    }
    else {
      export.write(f"sep=${sep}\nVerband${sep}Verein${sep}Kategorie${sep}Geschlecht${sep}Anzahl${sep}Einheitsbezeichnung\n".getBytes("ISO-8859-1"))
    }
    for(riege <- riegen) {
      export.write((riege + "\n").getBytes("ISO-8859-1"))
    }

    export.flush()
    export.close()
  }

  def exportDurchgaenge(wettkampf: Wettkampf, filename: String) {
    val export = new FileOutputStream(filename);
    val diszipline = listDisziplinesZuWettkampf(wettkampf.id)
    val sep = ";"
    export.write(f"""sep=${sep}\nDurchgang${sep}Summe${sep}Min${sep}Max${sep}Durchschn.""".getBytes("ISO-8859-1"))

    diszipline.foreach { x =>
        export.write(f"${sep}${x.name}${sep}Anz".getBytes("ISO-8859-1"))
    }
    export.write("\r\n".getBytes("ISO-8859-1"))

    listRiegenZuWettkampf(wettkampf.id)
      .sortBy(r => r._1)
      .map(x =>
        RiegeEditor(
            wettkampf.id,
            x._1,
            x._2,
            0,
            true,
            x._3,
            x._4,
            None))
      .groupBy(re => re.initdurchgang).toSeq
      .sortBy(re => re._1)
      .map{res =>
        val (name, rel) = res
        DurchgangEditor(wettkampf.id, name.getOrElse(""), rel)
      }
      .foreach { x =>
        export.write(f"""${x.initname}${sep}${x.anz.value}${sep}${x.min.value}${sep}${x.max.value}${sep}${x.avg.value}""".getBytes("ISO-8859-1"))
        diszipline.foreach { d =>
          export.write(f"${sep}${x.initstartriegen.getOrElse(d, Seq[RiegeEditor]()).map(r => f"${r.name.value.replace("M,", "Tu,").replace("W,", "Ti,")} (${r.anz.value})").mkString("\"","\n", "\"")}${sep}${x.initstartriegen.getOrElse(d, Seq[RiegeEditor]()).map(r => r.anz.value).sum}".getBytes("ISO-8859-1"))
        }
        export.write("\r\n".getBytes("ISO-8859-1"))
      }

    export.flush()
    export.close()
  }

}