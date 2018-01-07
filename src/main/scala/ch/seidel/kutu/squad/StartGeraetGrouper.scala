package ch.seidel.kutu.squad

import scala.annotation.tailrec
import ch.seidel.kutu.domain._
import ch.seidel.kutu.squad._

trait StartGeraetGrouper extends RiegenSplitter {
  
  def groupWertungen(programm: String, wertungen: Map[AthletView, Seq[WertungView]], 
      grp: List[WertungView => String], grpAll: List[WertungView => String], 
      startgeraete: List[Disziplin], maxRiegenSize: Int, splitSex: SexDivideRule, jahrgangGroup: Boolean)
    (implicit cache: scala.collection.mutable.Map[String, Int]) = {
    
    val startgeraeteSize = startgeraete.size

    // per groupkey, transform map to seq, sorted by all groupkeys
    val atheltenInRiege = wertungen.groupBy(w => groupKey(grp)(w._2.head)).toSeq.map{x =>
      (/*grpkey*/  x._1, // Riegenname
       /*values*/  x._2.foldLeft((Seq[(AthletView, Seq[WertungView])](), Set[Long]())){(acc, w) =>
          val (data, seen) = acc
          val (athlet, _ ) = w
          if(seen.contains(athlet.id)) acc else (w +: data, seen + athlet.id)
        }
        ._1.sortBy(w => groupKey(grpAll)(w._2.head)) // Liste der Athleten in der Riege, mit ihren Wertungen
      )
    }

    val athletensum = atheltenInRiege.flatMap{_._2.map(aw => aw._1.id)}.toSet.size
    val maxRiegenSize2 = if(maxRiegenSize > 0) maxRiegenSize else math.max(14, math.ceil(1d * athletensum / startgeraeteSize).intValue())
    val riegen = splitToMaxTurnerCount(atheltenInRiege, maxRiegenSize2, cache).map(r => Map(r._1 -> r._2))
    // Maximalausdehnung. Nun die sinnvollen Zusammenlegungen

    def combineToDurchgangSize(relevantcombis: Seq[RiegeAthletWertungen]): Seq[RiegeAthletWertungen] = {
      splitSex match {
        case GemischterDurchgang =>
          val rcm = relevantcombis.filter(c => c.head._2.head._1.geschlecht.equalsIgnoreCase("M"))
          val rcw = relevantcombis.filter(c => c.head._2.head._1.geschlecht.equalsIgnoreCase("W"))
          Stager.solve(startgeraeteSize, maxRiegenSize2, rcm)++ Stager.solve(startgeraeteSize, maxRiegenSize2, rcw)
        case _ =>
          Stager.solve(startgeraeteSize, maxRiegenSize2, relevantcombis)
      }
    }
    
    def vereinStats(like: Option[Verein], startriege: RiegeAthletWertungen) = {
      startriege.values.flatMap{x =>
        x.flatMap{xx =>
          xx._1.verein.filter{verein =>
//                println(s"seek $like found $verein")
            like.equals(Some(verein))
          }
        }
      }.size
    }

    @tailrec
    def spreadEven(startriegen: Seq[RiegeAthletWertungen]): Seq[RiegeAthletWertungen] = {
      /*
       * 1. Durchschnittsgrösse ermitteln
       * 2. Grösste Abweichungen ermitteln (kleinste, grösste)
       * 3. davon (teilbare) Gruppen filtern
       * 4. schieben.
       */
      val averageSize = startriegen.map(_.durchgangRiegeSize).sum / startriegen.size

      def smallestDividable(r: RiegeAthletWertungen) = {
        if(r.size > 1) {
          Some(r.keys.map(x => (x, r(x))).toSeq.sortBy(y => y._2.size).head)
        }
        else {
          None
        }
      }
      val stats = startriegen.map{raw =>
        // Riege, Anz. Gruppen, Anz. Turner, Std.Abweichung, (kleinste Gruppekey, kleinste Gruppe)
        val anzTurner = raw.durchgangRiegeSize
        val abweichung = anzTurner - averageSize
        (raw, raw.size, anzTurner, abweichung, smallestDividable(raw) )
      }.sortBy(_._4).reverse // Abweichung          
      val kleinsteGruppe = stats.last
      type GrpStats = (RiegeAthletWertungen, Int, Int, Int, Option[(String, Seq[(AthletView, Seq[WertungView])])])
      def checkSC(p1: GrpStats, p2: GrpStats): Boolean = {
        splitSex match {
          case GemischterDurchgang =>
            val ret = p1._1.head._2.head._1.geschlecht.equals(p2._1.head._2.head._1.geschlecht)
            ret
          case _ =>
            true
        }
      }
      stats.find{p =>
        val b11 = p._5 != None && p != kleinsteGruppe
        val b12 = checkSC(p, kleinsteGruppe)
        val b2 = p._3 > averageSize
        val b3 = b11 && p._5.get._2.size + kleinsteGruppe._3 <= averageSize
        lazy val v1 = vereinStats(p._5.get._2.head._1.verein, p._1)
        lazy val v2 = vereinStats(p._5.get._2.head._1.verein, kleinsteGruppe._1)
        lazy val b4 = v1 - p._5.get._2.size < v2 + p._5.get._2.size
        lazy val b5 = {
          p._5.get._2.size + kleinsteGruppe._3 <= averageSize + (kleinsteGruppe._3 / 2)
        }
        lazy val b6 = v1 - p._5.get._2.size == 0
        lazy val b7 = v2 > 0

        b11 && b12 && ((b2 && b3 && b4) || (b2 && b3 && b5 && b6 && b7))
      } match {
        case Some(groessteTeilbare) =>
          val gt = groessteTeilbare._1 - groessteTeilbare._5.get._1
          val sg = kleinsteGruppe._1 ++ Map(groessteTeilbare._5.get._1 -> groessteTeilbare._5.get._2)
          spreadEven(gt +: startriegen.filter(sr => sr != groessteTeilbare._1 && sr != kleinsteGruppe._1) :+ sg)
        case None => stats.find(p => p != kleinsteGruppe && checkSC(p, kleinsteGruppe) && p._3 > averageSize && p._5 != None && p._5.get._2.size + kleinsteGruppe._3 <= averageSize) match {
          case Some(groessteTeilbare) =>
            val gt = groessteTeilbare._1 - groessteTeilbare._5.get._1
            val sg = kleinsteGruppe._1 ++ Map(groessteTeilbare._5.get._1 -> groessteTeilbare._5.get._2)
            spreadEven(gt +: startriegen.filter(sr => sr != groessteTeilbare._1 && sr != kleinsteGruppe._1) :+ sg)
          case None => startriegen
        }
      }
    }

    def handleVereinMerges(startriegen: Seq[RiegeAthletWertungen]): Seq[RiegeAthletWertungen] = {
      if(jahrgangGroup) {
        startriegen
      }
      else splitSex match {
        case GemischteRiegen =>
          bringVereineTogether(startriegen)
        case GemischterDurchgang =>
          startriegen
        case GetrennteDurchgaenge =>
          bringVereineTogether(startriegen)
      }
    }

    def bringVereineTogether(startriegen: Seq[RiegeAthletWertungen]): Seq[RiegeAthletWertungen] = {
      
      def findSubstitutesFor(riegeToReplace: RiegeAthletWertungen, zielriege: RiegeAthletWertungen): Option[RiegeAthletWertungen] = {
        // RiegenAthletWertungen = Map[String, Seq[(AthletView, Seq[WertungView])]]
        val replaceCnt = riegeToReplace.durchgangRiegeSize
        val candidates = (zielriege - riegeToReplace.keys.head).toList.sortBy(x => x._2.size).reverse.foldLeft(Map().asInstanceOf[RiegeAthletWertungen]){(acc, candidate) =>
          val grouped = acc + candidate
          if (grouped.durchgangRiegeSize <= replaceCnt) {
            grouped
          } else {
            acc
          }
        }
        if (candidates.nonEmpty) {
          Some(candidates)
        } else {
          None
        }
      }
      
      @tailrec
      def _bringVereineTogether(startriegen: Seq[RiegeAthletWertungen], variantsCache: Set[Seq[RiegeAthletWertungen]]): Seq[RiegeAthletWertungen] = {
        val averageSize = startriegen.map(_.durchgangRiegeSize).sum / startriegen.size
        val optimized = startriegen.flatMap{raw =>
          raw.values.flatMap{r =>
            r.map{rr =>
              (rr._1.verein -> raw)
            }
          }
        }.groupBy{vereinraw =>
          vereinraw._1
        }.map{vereinraw =>
          vereinraw._1 -> vereinraw._2.map(_._2).toSet // (Option[Verein] -> Set[RiegenAthletWertungen}) RiegenAthletWertungen = Map[String, Seq[(AthletView, Seq[WertungView])]]
        }.foldLeft(startriegen){(accStartriegen, item) =>
          val (verein, riegen) = item
          val ret = riegen.map(f => (f, f.filter(ff => ff._2.exists(p => p._1.verein.equals(verein))).keys.head)).foldLeft(accStartriegen.toSet){(acccStartriegen, riegen2) =>
            val (geraetRiege, filteredRiege) = riegen2
            val toMove = geraetRiege(filteredRiege)
            val vereinTurnerRiege = Map(filteredRiege -> toMove)
            val v1 = vereinStats(verein, geraetRiege)
            val anzTurner = geraetRiege.durchgangRiegeSize
            acccStartriegen.find{p =>
              p != geraetRiege &&
              acccStartriegen.contains(geraetRiege) &&
              vereinStats(verein, p) > v1
            }
            match {
              case Some(zielriege) if ((zielriege ++ vereinTurnerRiege).durchgangRiegeSize <= maxRiegenSize2) =>
                println(s"moving $filteredRiege from ${geraetRiege.keys.toSet} to ${zielriege.keys.toSet}")
                val gt = geraetRiege - filteredRiege
                val sg = zielriege ++ Map(filteredRiege -> toMove)
                val r1 = acccStartriegen - zielriege
                val r2 = r1 + sg
                val r3 = r2 - geraetRiege
                val ret = r3 + gt
                ret
              case Some(zielriege) => findSubstitutesFor(vereinTurnerRiege, zielriege) match {
                case Some(substitues) => 
                  val gt = geraetRiege - filteredRiege ++ substitues
                  val sg = zielriege -- substitues.keys ++ Map(filteredRiege -> toMove)
                  if (gt.durchgangRiegeSize > maxRiegenSize2 || sg.durchgangRiegeSize > maxRiegenSize2) {
                    acccStartriegen
                  } else {
                    println(s"switching ${substitues.keys.toSet} with $filteredRiege between ${geraetRiege.keys.toSet} to ${zielriege.keys.toSet}")
                    acccStartriegen - zielriege - geraetRiege + gt + sg
                  }
                case None => acccStartriegen
              }
              case None => acccStartriegen
            }
          }
          ret.toSeq
        }
        if (optimized == startriegen || variantsCache.contains(optimized)) {
          optimized
        } else {
          val evenoptimized = spreadEven(optimized)
          if (evenoptimized == startriegen || variantsCache.contains(evenoptimized)) {
            evenoptimized
          } else {
            _bringVereineTogether(evenoptimized, variantsCache + optimized + evenoptimized)
          }
        }
      }
      _bringVereineTogether(startriegen, Set(startriegen))
    }

    
    val alignedriegen = if(riegen.isEmpty) riegen else handleVereinMerges(combineToDurchgangSize(riegen))

    // Startgeräteverteilung
    distributeToStartgeraete(programm, startgeraete, maxRiegenSize, alignedriegen)
  }  
  
  private def distributeToStartgeraete(programm: String, startgeraete: List[Disziplin], maxRiegenSize: Int, alignedriegen: Seq[RiegeAthletWertungen]) = 
    alignedriegen.zipWithIndex.flatMap{ r =>
      val (rr, index) = r
      val startgeridx =  (index + startgeraete.size) % startgeraete.size
      rr.keys.map{riegenname =>
        println(s"Durchgang $programm (${index / startgeraete.size + 1}), Start ${startgeraete(startgeridx).easyprint}, ${rr(riegenname).size} Tu/Ti der Riege $riegenname")
        (s"$programm (${if(maxRiegenSize > 0) index / startgeraete.size + 1 else 1})", riegenname, startgeraete(startgeridx), rr(riegenname))
      }
    }
}