package ch.seidel.kutu.calc

import ch.seidel.kutu.calc.parser._
import ch.seidel.kutu.domain.{Resultat, Wertung, WettkampfdisziplinView}

case class Calculator(template: ScoreCalcTemplate) {
  def calculate(w: Wertung, wd: WettkampfdisziplinView, values: List[List[ScoreCalcVariable]]): Wertung = {
    //val t = if (wd.kurzbeschreibung.nonEmpty) ScoreCalcTemplate(wd.kurzbeschreibung) else template
    val wertungen = values.take(if (template.aggregateFn.isEmpty) 1 else 2).map(values => {
      val dExpression = template.dExpression(values)
      val eExpression = template.eExpression(values)
      val pExpression = template.pExpression(values)
      val d = BigDecimal(Expression(MathExpCompiler(dExpression)).eval(Map.empty))
      val e = BigDecimal(Expression(MathExpCompiler(eExpression)).eval(Map.empty))
      val p = BigDecimal(Expression(MathExpCompiler(pExpression)).eval(Map.empty))
      wd.verifiedAndCalculatedWertung(w.copy(noteD = Some(d), noteE = Some(e - p)))
    })
    template.aggregateFn match {
      case None => wertungen.head
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
        wd.verifiedAndCalculatedWertung(w.copy(
          noteD = Some(r.noteD), noteE = Some(r.noteE)
        ))
      case Some(Sum) =>
        val r = wertungen
          .map(_.resultat)
          .foldLeft(Resultat(0,0,0)){
            (a,b) => a + b
          }
        wd.verifiedAndCalculatedWertung(w.copy(
          noteD = Some(r.noteD), noteE = Some(r.noteE)
        ))
    }
  }
}
