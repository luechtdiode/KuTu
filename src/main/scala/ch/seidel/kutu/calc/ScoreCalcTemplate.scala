package ch.seidel.kutu.calc

import ch.seidel.kutu.domain.DataObject
import ch.seidel.kutu.http.JsonSupport

import scala.math.BigDecimal.RoundingMode

enum ScoreAggregateFn:
  case Min, Max, Avg, Sum
object ScoreAggregateFn {
  val index: Map[String, ScoreAggregateFn] = ScoreAggregateFn.values.map(e => e.toString -> e).toMap
  def apply(fn: Option[String]): Option[ScoreAggregateFn] = fn.flatMap {
    index.get
  }.orElse(None)
}

object ScoreCalcVariable_ {
  def apply(source: String, prefix: String, name: String, scale: Option[Int]): ScoreCalcVariable = scale match {
    case Some(s) => ScoreCalcVariable(source, prefix, name, scale, BigDecimal("0").setScale(s))
    case _ => ScoreCalcVariable(source, prefix, name, scale, BigDecimal("0"))
  }
}

case class ScoreCalcVariable(source: String, prefix: String, name: String, scale: Option[Int], value: BigDecimal, index: Int = 0) extends DataObject  {
  def updated(newValue: BigDecimal): ScoreCalcVariable = copy(value = newValue.setScale(scale.getOrElse(value.scale), RoundingMode.HALF_UP)): ScoreCalcVariable
  def equalID(other: ScoreCalcVariable): Boolean = prefix.equals(other.prefix) && name.equals(other.name) && index.equals(other.index)
  def isGeneric: Boolean = source.isEmpty
}

case object TemplateViewJsonReader extends JsonSupport {

  import spray.json.enrichString

  def apply(text: String): ScoreCalcTemplateView = scoreCalcTemplateViewFormat.read(text.parseJson)
  def apply(text: Option[String]): Option[ScoreCalcTemplateView] = text.map(t => scoreCalcTemplateViewFormat.read(t.parseJson))
}

case class ScoreCalcTemplateView(
                                  dExpression: String, dVariables: List[ScoreCalcVariable], dDetails: Boolean,
                                  eExpression: String, eVariables: List[ScoreCalcVariable], eDetails: Boolean,
                                  pExpression: String, pVariables: List[ScoreCalcVariable], pDetails: Boolean,
                                  aggregateFn: Option[ScoreAggregateFn]) extends DataObject  {
  def variables = (dVariables ++ eVariables ++ pVariables).groupBy(_.index).values.toList
  def readablDFormula = if (dVariables.isEmpty) "" else aggregateFn match {
    case None => dExpression
    case Some(agf) => dVariables.map(v => v.value).mkString(s"$agf(", "," ,")")
  }
  def readablEFormula = if (eVariables.isEmpty) "" else aggregateFn match {
    case None => eExpression
    case Some(agf) => eVariables.map(v => v.value).mkString(s"$agf(", "," ,")")
  }
}
/*
  wettkampf_id integer NOT NULL,
  disziplin_id integer,
  wettkampfdisziplin_id integer,
 */
case object TemplateJsonReader extends JsonSupport {

  import spray.json.enrichString

  def apply(text: String): ScoreCalcTemplate = scoreCalcTemplateFormat.read(text.parseJson)
}

case class ScoreCalcTemplate(id: Long, wettkampfId: Option[Long], disziplinId: Option[Long], wettkampfdisziplinId: Option[Long], dFormula: String, eFormula: String, pFormula: String, aggregateFn: Option[ScoreAggregateFn]) {
  private val varPattern = "\\$([DAEBP]{1})([\\w]+([\\w\\d\\s\\-]*[\\w\\d]{1})?)(\\.([0123]+))?".r

  val scoreCalcTemplateSorter: ScoreCalcTemplate => String = t => {
    val wkm = t.wettkampfId match {
      case Some(_) => 100
      case None => 1000
    }
    val dm = t.disziplinId match {
      case Some(_) => 10
      case None => 2000
    }
    val wdm = t.wettkampfdisziplinId match {
      case Some(_) => 1
      case None => 3000
    }
    f"${(wkm + dm + wdm)}%04d"
  }
  val dVariables: List[ScoreCalcVariable] = parseVariables(dFormula)
  val dResolveDetails: Boolean = dFormula.endsWith("^")
  val eVariables: List[ScoreCalcVariable] = parseVariables(eFormula)
  val eResolveDetails: Boolean = eFormula.endsWith("^")
  val pVariables: List[ScoreCalcVariable] = parseVariables(pFormula)
  val pResolveDetails: Boolean = pFormula.endsWith("^")

  val sortOrder: String = scoreCalcTemplateSorter(this)
  lazy val variables: List[ScoreCalcVariable] = dVariables ++ eVariables ++ pVariables

  def toView(values: List[ScoreCalcVariable]): ScoreCalcTemplateView = {
    val updateVarsOf = updateVarsWith(values) _
    ScoreCalcTemplateView(
      dExpression(values), updateVarsOf(dVariables), dResolveDetails,
      eExpression(values), updateVarsOf(eVariables), eResolveDetails,
      pExpression(values), updateVarsOf(pVariables), pResolveDetails,
      aggregateFn
    )
  }

  def dExpression(values: List[ScoreCalcVariable]): String = renderExpression(dFormula, values
    .filter{
      case ScoreCalcVariable(source, "D", name, scale, value, index) => true
      case ScoreCalcVariable(source, "A", name, scale, value, index) => true
      case _ => false
    })

  def dExpressions(values: List[List[ScoreCalcVariable]]): String = aggregateFn match {
    case None => dExpression(values.flatten)
    case Some(agf) => values.map(exvars => dExpression(exvars)).mkString(s"$agf(", ",", ")")
  }

  def eExpression(values: List[ScoreCalcVariable]): String = renderExpression(eFormula, values
    .filter{
      case ScoreCalcVariable(source, "E", name, scale, value, index) => true
      case ScoreCalcVariable(source, "B", name, scale, value, index) => true
      case _ => false
    })

  def eExpressions(values: List[List[ScoreCalcVariable]]): String = aggregateFn match {
    case None => eExpression(values.flatten)
    case Some(agf) => values.map(exvars => eExpression(exvars)).mkString(s"$agf(", ",", ")")
  }

  def pExpression(values: List[ScoreCalcVariable]): String = renderExpression(pFormula, values.filter(_.prefix.equals("P")))

  private def updateVarsWith(values: List[ScoreCalcVariable])(vars: List[ScoreCalcVariable]) =
    vars
      .map { v => values
        .find(vv => vv.prefix.equals(v.prefix) && vv.name.equals(v.name) && vv.index.equals(v.index))
        .map(vv => v.updated(vv.value))
        .getOrElse(v)
    }
  private def parseVariables(formula: String): List[ScoreCalcVariable] = varPattern.findAllMatchIn(formula).map { m =>
    val prefix = m.group(1)
    val name = m.group(2)
    val scale = if (m.groupCount == 5 && m.group(5) != null && !m.group(5).equals("0")) Some(m.group(5)) else None
    ScoreCalcVariable_(m.group(0), prefix, name, scale.map(_.toInt))
  }.distinct.flatMap{scv => aggregateFn match {
    case None => List(scv.copy(index = 0))
    case Some(_) => List(scv.copy(index = 0), scv.copy(index = 1))
  }}.toList

  def renderExpression(formula: String, values: List[ScoreCalcVariable]): String = {
    val f = values.foldLeft(formula) { (acc, variable) =>
      acc.replace(variable.source, variable.value.toString())
    }
    val ff = variables.foldLeft(f) { (acc, variable) =>
      acc.replace(variable.source, variable.value.toString())
    }
    if (ff.endsWith("^")) ff.dropRight(1) else ff
  }

  def validateFormula(formula: String):String = {
    renderExpression(formula, parseVariables(formula))
  }
}