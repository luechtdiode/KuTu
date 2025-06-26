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

  def apply(text: String): ScoreCalcTemplate = scoreCalcTemplateFormat.read(text.parseJson)
}

case class ScoreCalcTemplateView(
                                  dExpression: String, dVariables: List[ScoreCalcVariable], dDetails: Boolean,
                                  eExpression: String, eVariables: List[ScoreCalcVariable], eDetails: Boolean,
                                  pExpression: String, pVariables: List[ScoreCalcVariable], pDetails: Boolean,
                                  aggregateFn: Option[ScoreAggregateFn])

case class ScoreCalcTemplate(dFormula: String, eFormula: String, pFormula: String, aggregateFn: Option[ScoreAggregateFn]) {
  private val varPattern = "\\$([DEP]{1})([\\w]+[\\w\\d]*)(.([0123]+))?".r

  val dVariables: List[ScoreCalcVariable] = parseVariables(dFormula)
  val dResolveDetails: Boolean = dFormula.endsWith("^")
  val eVariables: List[ScoreCalcVariable] = parseVariables(eFormula)
  val eResolveDetails: Boolean = eFormula.endsWith("^")
  val pVariables: List[ScoreCalcVariable] = parseVariables(pFormula)
  val pResolveDetails: Boolean = pFormula.endsWith("^")

  lazy val variables: List[ScoreCalcVariable] = dVariables ++ eVariables ++ pVariables

  def toView(values: List[ScoreCalcVariable]): ScoreCalcTemplateView = {
    val updateVarsOfFn = updateVarsOf(values) _
    ScoreCalcTemplateView(
      dExpression(values), updateVarsOfFn(dVariables), dResolveDetails,
      eExpression(values), updateVarsOfFn(eVariables), eResolveDetails,
      pExpression(values), updateVarsOfFn(pVariables), pResolveDetails,
      aggregateFn
    )
  }

  def dExpression(values: List[ScoreCalcVariable]): String = renderExpression(dFormula, values.filter(_.prefix.equals("D")))

  def eExpression(values: List[ScoreCalcVariable]): String = renderExpression(eFormula, values.filter(_.prefix.equals("E")))

  def pExpression(values: List[ScoreCalcVariable]): String = renderExpression(pFormula, values.filter(_.prefix.equals("P")))

  private def updateVarsOf(values: List[ScoreCalcVariable])(vars: List[ScoreCalcVariable]) =
    vars.map { v =>
      values
        .find(vv => vv.prefix.equals(v.prefix) && vv.name.equals(v.name))
        .map(vv => v.updated(vv.value))
        .getOrElse(v)
    }

  private def parseVariables(formula: String): List[ScoreCalcVariable] = varPattern.findAllMatchIn(formula).map { m =>
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