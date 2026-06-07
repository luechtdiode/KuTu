package ch.seidel.kutu.calc

import ch.seidel.kutu.domain.{StandardWettkampf, Wertung, WettkampfdisziplinView}
import ch.seidel.kutu.http.JsonSupport
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.enrichAny

import scala.math.BigDecimal

class ScoreCalcTemplateTest extends AnyWordSpec with Matchers with JsonSupport {
  val t = ScoreCalcTemplate(0,None,None,None,
    "max($Dname1.1, $Dname2.1)^",
    "10 - avg($EE-Name1.3, $EE Name2.3)",
    "($Pname / 10)^",
    None)

  "parse formula source" in {
    assert(t.dVariables === List(
      ScoreCalcVariable("$Dname1.1", "D","name1",Some(1),BigDecimal(0.0)),
      ScoreCalcVariable("$Dname2.1", "D","name2",Some(1),BigDecimal(0.0))))
    assert(t.eVariables === List(
      ScoreCalcVariable("$EE-Name1.3", "E","E-Name1",Some(3),BigDecimal(0.000)),
      ScoreCalcVariable("$EE Name2.3", "E","E Name2",Some(3),BigDecimal(0.000))))
    assert(t.pVariables === List(
      ScoreCalcVariable("$Pname", "P","name",None,BigDecimal(0))))
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
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val dvalues = t.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = t.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = t.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val valuesList = List(values)
    val calculatedWertung = Calculator(t).calculate(wertung, wd, valuesList)
    assert(calculatedWertung match {
      case w: Wertung =>
        w.noteD.exists(_.toDouble == 2.1) &&
          w.noteE.exists(_.toDouble == 8.277) &&
          w.endnote.exists(_.toDouble == 10.377) &&
          w.variables.exists(_.variables == valuesList)
      case null => false
    })
  }

  "calculate n-wertung Min" in {
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val tt = t.copy(aggregateFn = Some(ScoreAggregateFn.Min))
    val dvalues = tt.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = tt.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = tt.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val valuesList = values.groupBy(_.index).values.toList
    val calculatedWertung = Calculator(tt).calculate(wertung, wd, valuesList)
    assert(calculatedWertung match {
      case w: Wertung =>
        w.variables.foreach(scvtv => assert(scvtv.variables === valuesList))
        w.noteD.exists(_.toDouble == 4.1) &&
          w.noteE.exists(_.toDouble == 6.677) &&
          w.endnote.exists(_.toDouble == 10.777) &&
          w.variables.exists(_.variables == valuesList)
      case null => false
    })
  }

  "calculate n-wertung Max" in {
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val tt = t.copy(aggregateFn = Some(ScoreAggregateFn.Max))
    val dvalues = tt.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = tt.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = tt.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val valuesList = values.groupBy(_.index).values.toList
    val calculatedWertung = Calculator(tt).calculate(wertung, wd, valuesList)
    assert(calculatedWertung match {
      case w: Wertung =>
        w.noteD.exists(_.toDouble == 3.1) &&
          w.noteE.exists(_.toDouble == 7.777) &&
          w.endnote.exists(_.toDouble == 10.877) &&
          w.variables.exists(_.variables == valuesList)
      case null => false
    })
  }

  "calculate n-wertung Sum" in {
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val tt = t.copy(aggregateFn = Some(ScoreAggregateFn.Sum))
    val dvalues = tt.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = tt.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = tt.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val valuesList = values.groupBy(_.index).values.toList
    val calculatedWertung = Calculator(tt).calculate(wertung, wd, valuesList)
    assert(calculatedWertung match {
      case w: Wertung =>
        w.noteD.exists(_.toDouble == 7.2) &&
          w.noteE.exists(_.toDouble == 14.454) &&
          w.endnote.exists(_.toDouble == 21.654) &&
          w.variables.exists(_.variables == valuesList)
      case null => false
    })
  }

  "calculate n-wertung Avg" in {
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val tt = t.copy(aggregateFn = Some(ScoreAggregateFn.Avg))
    val dvalues = tt.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = tt.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = tt.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val valuesList = values.groupBy(_.index).values.toList
    val calculatedWertung = Calculator(tt).calculate(wertung, wd, valuesList)
    assert(calculatedWertung match {
      case w: Wertung =>
        w.noteD.exists(_.toDouble == 3.6) &&
          w.noteE.exists(_.toDouble == 7.227) &&
          w.endnote.exists(_.toDouble == 10.827) &&
          w.variables.exists(_.variables == valuesList)
      case null => false
    })
  }

  "parse from json" in {
    val t: ScoreCalcTemplate = TemplateJsonReader(
      """{
      "id": 0, "wettkampf_id": 0,
      "dFormula": "max($Dname1.1, $Dname2.1)^",
      "eFormula": "10 - avg($Ename1.3, $Ename2.3)",
      "pFormula": "($Pname.0 / 10)^",
      "aggregateFn": "Max"
    }""")
    assert(t.aggregateFn === Some(ScoreAggregateFn.Max))
    assert(t.dFormula === "max($Dname1.1, $Dname2.1)^")
    assert(t.dResolveDetails === true)
  }

  "expression-test" in {
    val t: ScoreCalcTemplate = TemplateJsonReader(
      """{
      "id": 0, "wettkampf_id": 0,
      "dFormula": "max($Dname1.1, $Dname2.1)^",
      "eFormula": "avg(10 - $Ename1.3, 10 - $Ename2.3)",
      "pFormula": "($Pname.0 / 10)^",
      "aggregateFn": "Max"
    }""")
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val dvalues = t.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = t.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = t.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val valuesList = values.groupBy(_.index).values.toList
    val calculatedWertung = Calculator(t).calculate(wertung, wd, List(values, values))
    assert(calculatedWertung match {
      case w: Wertung =>
        w.noteD.exists(_.toDouble == 3.1) &&
          w.noteE.exists(_.toDouble == 7.777) &&
          w.endnote.exists(_.toDouble == 10.877) &&
          w.variables.exists(_.variables == valuesList)
      case null => false
    })

  }

  "expression-test2 and render json" in {
    val t: ScoreCalcTemplate = TemplateJsonReader(
      """{
      "id": 0, "wettkampf_id": 0,
      "dFormula": "max($Dname1.1, $Dname2.1)^",
      "eFormula": "avg(max(10 - $Ename1.3, 10 - $Ename2.3), min(10 - $Ename1.3, 10 - $Ename2.3))",
      "pFormula": "($Pname.0 / 10)^",
      "aggregateFn": "Max"
    }""")
    val wertung = Wertung(0, 0, 0, 0, "", None, None, None, None, None, None, None, None)
    val wd = WettkampfdisziplinView(0, null, null, "", None, StandardWettkampf(1d), 1, 1, 1, 3, 1, 0, 30, 1)
    val dvalues = t.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = t.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = t.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues
    val valuesList = values.groupBy(_.index).values.toList
    val calculatedWertung = Calculator(t).calculate(wertung, wd, List(values, values))
    assert(calculatedWertung match {
      case w: Wertung =>
        w.noteD.exists(_.toDouble == 3.1) &&
          w.noteE.exists(_.toDouble == 7.777) &&
          w.endnote.exists(_.toDouble == 10.877) &&
          w.variables.exists(_.variables == valuesList)
      case null => false
    })
    println(calculatedWertung.toJson.prettyPrint)
  }

  "parse from json without aggregateFn" in {
    val t: ScoreCalcTemplate = TemplateJsonReader(
      """{
      "id": 0, "wettkampf_id": 0,
      "dFormula": "max($Dname1.1, $Dname2.1)^",
      "eFormula": "10 - avg($Ename1.3, $Ename2.3)",
      "pFormula": "($Pname.0 / 10)^"
    }""")
    assert(t.aggregateFn === None)
    assert(t.dFormula === "max($Dname1.1, $Dname2.1)^")
    assert(t.dResolveDetails === true)
  }

  "resolveDetails" in {
    assert(t.dResolveDetails === true)
    assert(t.eResolveDetails === false)
    assert(t.pResolveDetails === true)
  }

  "toView and render json" in {
    val dvalues = t.dVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val evalues = t.eVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val pvalues = t.pVariables.zipWithIndex.map(item => item._1.updated(BigDecimal("0.12345") + item._2 + 1))
    val values = dvalues ++ evalues ++ pvalues

    val view: ScoreCalcTemplateView = t.toView(values)

    println(view.toJson.prettyPrint)
  }
}
