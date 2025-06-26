package ch.seidel.kutu.calc

import ch.seidel.kutu.domain.{StandardWettkampf, Wertung, WettkampfdisziplinView}
import ch.seidel.kutu.http.JsonSupport
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.enrichAny

class ScoreCalcTemplateTest extends AnyWordSpec with Matchers with JsonSupport {
  val t = ScoreCalcTemplate(
    "max($Dname1.1, $Dname2.1)^",
    "10 - avg($Ename1.3, $Ename2.3)",
    "($Pname.0 / 10)^",
    None)

  "parse formula source" in {
    assert(t.dVariables === List(
      ScoreCalcVariable("$Dname1.1", "D","name1",Some(1),BigDecimal(0.0)),
      ScoreCalcVariable("$Dname2.1", "D","name2",Some(1),BigDecimal(0.0))))
    assert(t.eVariables === List(
      ScoreCalcVariable("$Ename1.3", "E","name1",Some(3),BigDecimal(0.000)),
      ScoreCalcVariable("$Ename2.3", "E","name2",Some(3),BigDecimal(0.000))))
    assert(t.pVariables === List(
      ScoreCalcVariable("$Pname.0", "P","name",None,BigDecimal(0))))
  }

  "render formula with d-values" in {
    val dvalues = t.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val rendered = t.dExpression(dvalues)
    assert(rendered === "max(1.1, 2.1)")
  }

  "render formula with e-values" in {
    val evalues = t.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val rendered = t.eExpression(evalues)
    assert(rendered === "10 - avg(1.123, 2.123)")
  }

  "render formula with p-values" in {
    val pvalues = t.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val rendered = t.pExpression(pvalues)
    assert(rendered === "(1 / 10)")
  }

  "calculate wertung" in {
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val dvalues = t.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = t.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = t.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val calculatedWertung = Calculator(t).calculate(wertung, wd, List(values, values))
    println(calculatedWertung)
    assert(calculatedWertung === Wertung(0,0,0,0,"",Some(2.1),Some(8.277), Some(10.377),None,None,None))
  }

  "parse from json" in {
    val t: ScoreCalcTemplate = TemplateJsonReader(
      """{
      "dFormula": "max($Dname1.1, $Dname2.1)^",
      "eFormula": "10 - avg($Ename1.3, $Ename2.3)",
      "pFormula": "($Pname.0 / 10)^",
      "aggregateFn": "Max"
    }""")
    assert(t.aggregateFn === Some(Max))
    assert(t.dFormula === "max($Dname1.1, $Dname2.1)^")
    assert(t.dResolveDetails === true)
  }

  "expression-test" in {
    val t: ScoreCalcTemplate = TemplateJsonReader(
      """{
      "dFormula": "max($Dname1.1, $Dname2.1)^",
      "eFormula": "avg(10 - $Ename1.3, 10 - $Ename2.3)",
      "pFormula": "($Pname.0 / 10)^",
      "aggregateFn": "Max"
    }""")
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val dvalues = t.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = t.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = t.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val calculatedWertung = Calculator(t).calculate(wertung, wd, List(values, values))
    println(calculatedWertung)
    assert(calculatedWertung === Wertung(0,0,0,0,"",Some(2.1),Some(8.277), Some(10.377),None,None,None))
  }

  "expression-test2" in {
    val t: ScoreCalcTemplate = TemplateJsonReader(
      """{
      "dFormula": "max($Dname1.1, $Dname2.1)^",
      "eFormula": "avg(max(10 - $Ename1.3, 10 - $Ename2.3), min(10 - $Ename1.3, 10 - $Ename2.3))",
      "pFormula": "($Pname.0 / 10)^",
      "aggregateFn": "Max"
    }""")
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val dvalues = t.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = t.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = t.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val calculatedWertung = Calculator(t).calculate(wertung, wd, List(values, values))
    println(calculatedWertung)
    assert(calculatedWertung === Wertung(0,0,0,0,"",Some(2.1),Some(8.277),Some(10.377),None,None,None))
  }

  "parse from json without aggregateFn" in {
    val t: ScoreCalcTemplate = TemplateJsonReader(
      """{
      "dFormula": "max($Dname1.1, $Dname2.1)^",
      "eFormula": "10 - avg($Ename1.3, $Ename2.3)",
      "pFormula": "($Pname.0 / 10)^"
    }""")
    println(t)
    assert(t.aggregateFn === None)
    assert(t.dFormula === "max($Dname1.1, $Dname2.1)^")
    assert(t.dResolveDetails === true)
  }

  "calculate sum wertungen" in {
    val tt = t.copy(aggregateFn = Some(Sum))
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val dvalues = tt.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = tt.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = tt.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val calculatedWertung = Calculator(tt).calculate(wertung, wd, List(values, values))
    println(calculatedWertung)
    assert(calculatedWertung === Wertung(0,0,0,0,"",Some(4.2),Some(16.554), Some(20.754),None,None,None))
  }

  "resolveDetails" in {
    assert(t.dResolveDetails === true)
    assert(t.eResolveDetails === false)
    assert(t.pResolveDetails === true)
  }

  "toView" in {
    val dvalues = t.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = t.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = t.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues

    val view = t.toView(values)

    println(view.toJson.prettyPrint)
  }
}
