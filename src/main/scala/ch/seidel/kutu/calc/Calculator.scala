package ch.seidel.kutu.calc

import ch.seidel.kutu.calc.parser._
import ch.seidel.kutu.domain.{Resultat, Wertung, WettkampfdisziplinView}

case class Calculator(template: ScoreCalcTemplate) {

  def calculate(w: Wertung, wd: WettkampfdisziplinView, valuesList: List[List[ScoreCalcVariable]]): Wertung = {

    val allValues = valuesList.flatten

    def buildEndResult(wertung: Wertung, variables: List[ScoreCalcVariable], dValue: Double, eValue: Double): Wertung = {
      val result = wd.notenSpez.calcEndnote(dValue, eValue, wd)
      wertung.copy(noteD = Some(dValue), noteE = Some(eValue), endnote = Some(result), variables = Some(template.toView(variables)))
    }

    val wertungen = valuesList.take(if (template.aggregateFn.isEmpty) 1 else valuesList.length)
      .map(values => {
        val dExpression = Expression(MathExpCompiler(template.dExpression(values)))
        val eExpression = Expression(MathExpCompiler(template.eExpression(values)))
        val pExpression = Expression(MathExpCompiler(template.pExpression(values)))
        val d = BigDecimal(dExpression.eval(Map.empty))
        val e = BigDecimal(eExpression.eval(Map.empty))
        val p = BigDecimal(pExpression.eval(Map.empty))
        val dd = d.doubleValue
        val ee = (e - p).doubleValue
        val (dv, ev) = wd.notenSpez.validated(dd, ee, wd)
        buildEndResult(w, allValues, dv, ev)
      })

    template.aggregateFn match {
      case None => wertungen.headOption.getOrElse(w.copy(noteD = None, noteE = None, endnote = None, variables = None))
      case Some(Min) =>
        wertungen.reduce((a,b) => if (a.resultat.endnote < b.resultat.endnote) a else b)
      case Some(Max) =>
        wertungen.reduce((a,b) => if (a.resultat.endnote > b.resultat.endnote) a else b)
      case Some(Avg) =>
        val r = wertungen
          .map(_.resultat)
          .foldLeft(Resultat(0,0,0)){
            (a,b) => a + b
          } / wertungen.length
        buildEndResult(w, allValues, r.noteD.doubleValue, r.noteE.doubleValue)
      case Some(Sum) =>
        val r = wertungen
          .map(_.resultat)
          .foldLeft(Resultat(0,0,0)){
            (a,b) => a + b
          }
        buildEndResult(w, allValues, r.noteD.doubleValue, r.noteE.doubleValue)
    }
  }
}
