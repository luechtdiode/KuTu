package ch.seidel.kutu.calc

import ch.seidel.kutu.http.JsonSupport

import scala.math.BigDecimal.RoundingMode

sealed trait ScoreAggregateFn
case object Min extends ScoreAggregateFn
case object Max extends ScoreAggregateFn
case object Avg extends ScoreAggregateFn
case object Sum extends ScoreAggregateFn

object ScoreCalcVariable_ {
  def apply(source: String, prefix: String, name: String, scale: Option[Int]): ScoreCalcVariable = scale match {
    case Some(s) => ScoreCalcVariable(source, prefix, name, scale, BigDecimal("0").setScale(s))
    case _ => ScoreCalcVariable(source, prefix, name, scale, BigDecimal("0"))
  }
}
case class ScoreCalcVariable(source: String, prefix: String, name: String, scale: Option[Int], value: BigDecimal) {
  def updated(newValue: BigDecimal): ScoreCalcVariable = copy(value = newValue.setScale(value.scale, RoundingMode.HALF_UP)): ScoreCalcVariable
}
case object TemplateJsonReader extends JsonSupport {
  import spray.json.enrichString
  def apply(text: String) = scoreCalcTemplateFormat.read(text.parseJson)
}
case class ScoreCalcTemplate(dFormula: String, eFormula: String, pFormula: String, aggregateFn: Option[ScoreAggregateFn]) {
  private val varPattern = "\\$([DEP]{1})([\\w]+[\\w\\d]*)(.([0123]+))?".r

  val dVariables = parseVariables(dFormula)
  val dResolveDetails = dFormula.endsWith("^")
  val eVariables = parseVariables(eFormula)
  val eResolveDetails = eFormula.endsWith("^")
  val pVariables = parseVariables(pFormula)
  val pResolveDetails = pFormula.endsWith("^")

  def dExpression(values: List[ScoreCalcVariable]) = renderExpression(dFormula, values.filter(_.prefix.equals("D")))
  def eExpression(values: List[ScoreCalcVariable]) = renderExpression(eFormula, values.filter(_.prefix.equals("E")))
  def pExpression(values: List[ScoreCalcVariable]) = renderExpression(pFormula, values.filter(_.prefix.equals("P")))

  private def parseVariables(formula: String): List[ScoreCalcVariable] = varPattern.findAllMatchIn(formula).map{ m =>
    val prefix = m.group(1)
    val name = m.group(2)
    val scale = if (m.groupCount == 4 && !m.group(4).equals("0")) Some(m.group(4)) else None
    ScoreCalcVariable_(m.group(0), prefix, name, scale.map(_.toInt))
  }.distinct.toList

  private def renderExpression(formula: String, values: List[ScoreCalcVariable]): String = {
    val f = values.foldLeft(formula) { (acc, variable) =>
      acc.replace(variable.source, variable.value.toString())
    }
    if (f.endsWith("^")) f.dropRight(1) else f
  }

}