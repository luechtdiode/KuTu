package ch.seidel.kutu.squad

import ch.seidel.kutu.domain.*

import scala.collection.mutable

trait StartGeraetGrouper extends TurnerRiegenBuilder with GeraeteRiegenBuilder {

  def groupWertungen(programm: String, wertungen: Map[AthletView, Seq[WertungView]],
                     grp: List[WertungView => String], grpAll: List[WertungView => String],
                     startgeraete: List[Disziplin], maxRiegenSize: Int, splitSex: SexDivideRule, jahrgangGroup: Boolean, disziplinGeschlecht: Map[Long, (Int, Int)] = Map.empty)
                    (implicit cache: mutable.Map[String, Int]): Seq[(String, String, Disziplin, Seq[(AthletView, Seq[WertungView])])] = {
    if startgeraete.isEmpty then Seq.empty
    else {
      val turnerRiegen = buildTurnerRiegen(wertungen, grp, grpAll)
      buildGeraeteRiegen(programm, startgeraete, turnerRiegen, maxRiegenSize, splitSex, jahrgangGroup, disziplinGeschlecht)
    }
  }
}