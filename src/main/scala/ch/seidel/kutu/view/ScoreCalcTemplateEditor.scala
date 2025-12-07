package ch.seidel.kutu.view

import ch.seidel.kutu.calc.parser.{Expression, MathExpCompiler}
import ch.seidel.kutu.calc.{ScoreAggregateFn, ScoreCalcTemplate}
import ch.seidel.kutu.domain.*
import org.controlsfx.validation.{Severity, ValidationResult, Validator}
import scalafx.beans.property.*

object ScoreCalcTemplateEditor {
  val coldef = Map(
    "prio" -> 40,
    "kategoriedisziplin" -> 180,
    "disziplin" -> 150,
    "dFormula" -> 120,
    "eFormula" -> 120,
    "pFormula" -> 120,
    "aggregateFn" -> 80
  )
}

case class ScoreCalcTempateEditorService(wettkampf: WettkampfView, service: KutuService) {
  val wettkampfdisziplinViews: List[WettkampfdisziplinView] = service.listWettkampfDisziplineViews(wettkampf.toWettkampf)
  val disziplinList: List[Disziplin] = wettkampfdisziplinViews.map(_.disziplin).distinct.sortBy(_.name)

  def loadEditors(): List[ScoreCalcTemplateEditor] = service
    .loadScoreCalcTemplatesAll(wettkampf.id)
    .filter { sct => sct.wettkampfId.contains(wettkampf.id) || sct.disziplinId.exists(did => disziplinList.exists(d => d.id == did)) || sct.wettkampfdisziplinId.exists(wkdid => wettkampfdisziplinViews.exists(wkd => wkd.id == wkdid)) }
    .sortBy(_.sortOrder)
    .map { init => ScoreCalcTemplateEditor(init, this) }

  def editorOf(init: ScoreCalcTemplate): ScoreCalcTemplateEditor = loadEditors().find(e => e.init == init).getOrElse(ScoreCalcTemplateEditor(init, this))

  def newEditor(): ScoreCalcTemplateEditor = {
    val dnote = if wettkampfdisziplinViews.head.isDNoteUsed then s"$$${wettkampfdisziplinViews.head.notenSpez.getDifficultLabel}${wettkampfdisziplinViews.head.notenSpez.getDifficultLabel} Wert.2" else "0"
    ScoreCalcTemplateEditor(ScoreCalcTemplate(0, Some(wettkampf.id), None, None, dnote, s"$$${wettkampfdisziplinViews.head.notenSpez.getExecutionLabel}${wettkampfdisziplinViews.head.notenSpez.getExecutionLabel} Wert.2", "0", None), this)
  }

  def delete(editor: ScoreCalcTemplateEditor): Unit = service.deleteScoreCalcTemplate(editor.init)

  def updated(template: ScoreCalcTemplate): ScoreCalcTemplateEditor = {
    ScoreCalcTemplateEditor(template.wettkampfId match {
      case None =>
        template
      case _ => template.id match {
        case 0 =>
          val newTemplate = template.copy(wettkampfId = Some(wettkampf.id))
          service.createScoreCalcTempate(newTemplate)
        case _ =>
          service.updateScoreCalcTemplate(template)
          template
      }
    }, this)
  }
}

case class ScoreCalcTemplateEditor(init: ScoreCalcTemplate, context: ScoreCalcTempateEditorService) {
  val prio = StringProperty(init.sortOrder)
  val isEditable: Boolean = init.wettkampfId.nonEmpty

  val validState = new StringProperty("")
  val dvalidState = new StringProperty("")
  val evalidState = new StringProperty("")
  val pvalidState = new StringProperty("")

  val editable: BooleanProperty = new BooleanProperty() {
    value = isEditable
  }
  val disziplin = new StringProperty(init.disziplinId match {
    case Some(id) => context.disziplinList.find(d => d.id == id).map(_.name).getOrElse("")
    case None => ""
  })
  val kategoriedisziplin = new StringProperty(init.wettkampfdisziplinId match {
    case Some(id) => context.wettkampfdisziplinViews.find(d => d.id == id).map(wkd => s"${wkd.easyprint}").getOrElse("")
    case None => ""
  })
  disziplin.onChange {
    if !kategoriedisziplin.value.contains(disziplin.value) then {
      kategoriedisziplin.value = ""
    }
    val v = isValid
  }
  kategoriedisziplin.onChange {
    if !kategoriedisziplin.value.contains(disziplin.value) then {
      disziplin.value = ""
    }
    val v = isValid
  }
  val dFormula: StringProperty = new StringProperty(init.dFormula) {
    onChange {
      val v = isValid
    }
  }
  val eFormula: StringProperty = new StringProperty(init.eFormula) {
    onChange {
      val v = isValid
    }
  }
  val pFormula: StringProperty = new StringProperty(init.pFormula) {
    onChange {
      val v = isValid
    }
  }
  val aggregateFn: StringProperty = new StringProperty(init.aggregateFn.map(_.toString).getOrElse("")) {
    onChange {
      val v = isValid
    }
  }

  private def selectedDisziplin: Option[Disziplin] = context.disziplinList.find(d => d.easyprint.equals(disziplin.value))

  private def selectedWettkampfDisziplin: Option[WettkampfdisziplinView] = context.wettkampfdisziplinViews.find(d => d.easyprint.equals(kategoriedisziplin.value))

  private def selectedAggregatFn: Option[ScoreAggregateFn] = ScoreAggregateFn(Some(aggregateFn.value))

  def previewWertung(): WertungEditor = {
    val wkv = selectedWettkampfDisziplin
      .getOrElse(context.wettkampfdisziplinViews
        .find(v => v.disziplin.name.equals(disziplin.value))
        .getOrElse(context.wettkampfdisziplinViews.head))
    val nspatch = wkv.notenSpez match {
      case sw: StandardWettkampf => sw.copy(scoreTemplate = Some(commit))
      case _ => wkv.notenSpez
    }
    val visible = wkv.isDNoteUsed
    if !visible then {
      dFormula.value = "0"
    }

    WertungEditor(WertungView(0, Athlet().toAthletView(None),
      wkv.copy(notenSpez = nspatch),
      context.wettkampf.toWettkampf, None, None, None, None, None, 0, None, None))
  }

  def createValidator: Validator[String] = (control, formeltext) => {
    var renderedFormel = formeltext
    try {
      renderedFormel = init.validateFormula(formeltext.trim)
      Expression(MathExpCompiler(renderedFormel))
      ValidationResult.fromMessageIf(control, "Formel valid", Severity.ERROR, false)
    } catch {
      case e: Exception =>
        ValidationResult.fromMessageIf(control, s"Formel '$renderedFormel' mit Fehler: ${e.getMessage}", Severity.ERROR, true)
    }
  }

  def isValid: Boolean = {
    try {
      val template = commit
      val dState = try {
        val f = template.dExpression(template.variables)
        Expression(MathExpCompiler(f))
        s"D Formel $f OK"
      } catch {
        case e: Exception =>
          s"D Formel mit Fehler: ${e.getMessage}"
      }
      val eState = try {
        val f = template.eExpression(template.variables)
        Expression(MathExpCompiler(f))
        s"E Formel $f OK"
      } catch {
        case e: Exception =>
          s"E Formel mit Fehler: ${e.getMessage}"
      }
      val pState = try {
        val f = template.pExpression(template.variables)
        Expression(MathExpCompiler(f))
        s"Penalty Formel $f OK"
      } catch {
        case e: Exception =>
          s"Penalty Formel mit Fehler: ${e.getMessage}"
      }
      dvalidState.value = dState
      evalidState.value = eState
      pvalidState.value = pState
      validState.value =
        s"""
           |$dState
           |$eState
           |$pState
                            """.stripMargin
      dState.contains("OK") && eState.contains("OK") && pState.contains("OK")
    } catch {
      case e: Exception =>
        validState.value = e.getMessage
        false
    }
  }

  def reset(): Unit = {
    disziplin.value = init.disziplinId match {
      case Some(id) => context.disziplinList.find(d => d.id == id).map(_.name).getOrElse("")
      case None => ""
    }
    kategoriedisziplin.value = init.wettkampfdisziplinId match {
      case Some(id) => context.wettkampfdisziplinViews.find(d => d.id == id).map(wkd => s"${wkd.easyprint}").getOrElse("")
      case None => ""
    }
    dFormula.value = init.dFormula
    eFormula.value = init.eFormula
    pFormula.value = init.pFormula
    aggregateFn.value = init.aggregateFn.map(_.toString).getOrElse("")
  }

  def commit: ScoreCalcTemplate = init.copy(
    disziplinId = selectedDisziplin.map(_.id), wettkampfdisziplinId = selectedWettkampfDisziplin.map(_.id),
    dFormula = dFormula.value.trim, eFormula = eFormula.value.trim, pFormula = pFormula.value.trim,
    aggregateFn = selectedAggregatFn
  )
}
