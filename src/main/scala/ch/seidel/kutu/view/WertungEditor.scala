package ch.seidel.kutu.view

import ch.seidel.kutu.calc.{ScoreCalcTemplateView, ScoreCalcVariable}
import ch.seidel.kutu.domain._
import scalafx.beans.property.{BufferProperty, DoubleProperty, ObjectProperty}

import scala.jdk.CollectionConverters.IterableHasAsJava

case class WertungEditor(private var lastCommitted: WertungView) {
  def init: WertungView = lastCommitted
  def update(w: Wertung): Unit = {
    lastCommitted = lastCommitted.updatedWertung(w)
    reset
  }
  def update(w: WertungView): Unit = {
    lastCommitted = w
    reset
  }

  type WertungChangeListener = (WertungEditor) => Unit

  var listeners: Set[WertungChangeListener] = Set[WertungChangeListener]()
  def addListener(l: WertungChangeListener): Unit = {
    listeners += l
  }
  def removeListener(l: WertungChangeListener): Unit = {
    listeners -= l
  }
  private var notifying = false

  val matchesSexAssignment: Boolean = lastCommitted.athlet.geschlecht match {
    case "M" => lastCommitted.wettkampfdisziplin.masculin > 0
    case "W" => lastCommitted.wettkampfdisziplin.feminim > 0
    case _ => true
  }
  val noteD = DoubleProperty(Double.NaN)
  val noteE = DoubleProperty(Double.NaN)
  val endnote = DoubleProperty(Double.NaN)
  val dVariables = BufferProperty[ScoreCalcVariableEditor](this, "DVariablen", List.empty)
  val eVariables = BufferProperty[ScoreCalcVariableEditor](this, "EVariablen", List.empty)
  val pVariables = BufferProperty[ScoreCalcVariableEditor](this, "PVariablen", List.empty)
  def variableEditors = (dVariables.value.toList ++ eVariables.value.toList ++ pVariables.value.toList)
  def variableEditorsList = variableEditors.groupBy(_.score.value.index).values.toList

  private def allVariables: List[ScoreCalcVariable] = variableEditors.map(_.score.value)

  val dFormula = ObjectProperty[String](this, "DFormel", "")
  val eFormula = ObjectProperty[String](this, "EFormel", "")
  val calculatedWertung = ObjectProperty[Wertung](this, "Endwertung", lastCommitted.toWertung)

  case class ScoreCalcVariableEditor(init: ScoreCalcVariable) {
    val score = ObjectProperty[ScoreCalcVariable](init)
    val stringvalue = ObjectProperty[String](score.value.value.toString())
    stringvalue.onChange { (_, _, nv) =>
      try {
        score.value = score.value.updated(BigDecimal(nv))
      } catch {
        case e:NumberFormatException =>
          score.value = score.value.updated(BigDecimal(0))
      }
    }
    var notifying = false
    score.onChange {
      if (!notifying) try {
        stringvalue.value = score.value.value.toString()
        onChange()
      }
      finally {
        notifying = false
      }
    }
  }

  reset

  private def onChange(): Unit = {
    if (!notifying) try {
      notifying = true

      val wertung = commit
      wertung.noteD match {
        case Some(d) => noteD.value = d.toDouble
        case _ => noteD.value = Double.NaN
      }
      wertung.noteE match {
        case Some(d) => noteE.value = d.toDouble
        case _ => noteD.value = Double.NaN
      }
      wertung.endnote match {
        case Some(d) => endnote.value = d.toDouble
        case _ => noteD.value = Double.NaN
      }
      notifyListeners(wertung)
    } finally {
      notifying = false
    }
  }

  private def notifyListeners(wertung: Wertung): Unit = {
    val allVars = wertung.variablesList
    dFormula.value = lastCommitted.wettkampfdisziplin.notenSpez.template.map {
      _.dExpressions(allVars)
    }.getOrElse("")
    eFormula.value = lastCommitted.wettkampfdisziplin.notenSpez.template.map {
      _.eExpressions(allVars)
    }.getOrElse("")
    calculatedWertung.value = wertung
    listeners.foreach(f => f(this))
  }

  noteD.onChange {
    onChange()
  }
  noteE.onChange {
    onChange()
  }
  endnote.onChange {
    onChange()
  }
  dVariables.onChange {
    onChange()
  }
  eVariables.onChange {
    onChange()
  }
  pVariables.onChange {
    onChange()
  }

  private def changed(propertyValue: Double, initValue: Option[BigDecimal]): Boolean = {
    if (propertyValue == Double.NaN && initValue.isEmpty) {
      false
    } else if (initValue.map(_.toDouble).contains(propertyValue)) {
      false
    } else {
      true
    }
  }

  def clearInput(): Unit = {
    notifying = true
    try {
      resetVariables()
      noteD.value = Double.NaN
      noteE.value = Double.NaN
      endnote.value = Double.NaN
    } finally {
      notifying = false
      onChange()
    }
  }

  def isDirty =
    changed(noteD.value, lastCommitted.noteD) || changed(noteE.value, lastCommitted.noteE) || changed(endnote.value, lastCommitted.endnote)

  def mapVariablen: Option[ScoreCalcTemplateView] = {
    def mappedScoreCalcTemplateView = {
      lastCommitted.defaultVariables.map { sctv =>
        sctv.copy(
          dVariables = dVariables.value.toList.map(_.score.value),
          eVariables = eVariables.value.toList.map(_.score.value),
          pVariables = pVariables.value.toList.map(_.score.value))
      }.orElse(None)
    }

    def isGeneric = allVariables.exists(_.isGeneric)

    lastCommitted.wettkampfdisziplin.notenSpez.template match {
      case Some(_) => mappedScoreCalcTemplateView
      case None if isGeneric => mappedScoreCalcTemplateView
      case None => None
    }
  }

  def update(vars: List[ScoreCalcVariable]): Wertung = {
    def upateVar(buffer: BufferProperty[ScoreCalcVariableEditor])(v: ScoreCalcVariable): Unit = {
      buffer.value.foreach(e => if (e.score.value.equalID(v)) {
        e.score.value = v
      })
    }

    notifying = true
    try {
      vars.foreach(v => {
        upateVar(dVariables)(v)
        upateVar(eVariables)(v)
        upateVar(pVariables)(v)
      })
    } finally {
      notifying = false
      onChange()
    }
    calculatedWertung.value
  }

  def reset: Unit = {
    notifying = true
    try {
      resetVariables()
      lastCommitted.noteD match {
        case Some(d) =>
          noteD.value = d.toDouble
        case _ =>
          noteD.value = Double.NaN
      }
      lastCommitted.noteE match {
        case Some(d) =>
          noteE.value = d.toDouble
        case _ =>
          noteD.value = Double.NaN
      }
      lastCommitted.endnote match {
        case Some(d) =>
          endnote.value = d.toDouble
        case _ =>
          noteD.value = Double.NaN
      }
    } finally {
      notifying = false
      notifyListeners(commit)
    }
  }

  private def resetVariables(): Unit = {
    lastCommitted.defaultVariables match {
      case Some(v) =>
        dVariables.value.setAll(v.dVariables.map(ScoreCalcVariableEditor(_)).asJavaCollection)
        eVariables.value.setAll(v.eVariables.map(ScoreCalcVariableEditor(_)).asJavaCollection)
        pVariables.value.setAll(v.pVariables.map(ScoreCalcVariableEditor(_)).asJavaCollection)
      case _ =>
        dVariables.value.clear()
        eVariables.value.clear()
        pVariables.value.clear()
    }
    lastCommitted.endnote match {
      case Some(end) if (end > 0 && variableEditors.forall(_.init.value < 0.01)) =>
        dVariables.value.headOption match {
          case Some(d) =>
            d.stringvalue.value = lastCommitted.noteD.map(_.toString).getOrElse("")
          case None =>
        }
        eVariables.value.headOption match {
          case Some(e) => e.stringvalue.value = lastCommitted.noteE.map(_.toString).getOrElse("")
          case None =>
        }
      case _ =>
    }
  }

  def toOption(propertyValue: Double): Option[BigDecimal] =
    if (propertyValue.toString == Double.NaN.toString) None
    else Some(scala.math.BigDecimal(propertyValue))

  def toDouble(propertyValue: Double) = {
    if (propertyValue.toString == Double.NaN.toString) 0d
    else scala.math.BigDecimal(propertyValue).doubleValue
  }

  def toString(propertyValue: Double): String =
    if (propertyValue.toString == Double.NaN.toString) ""
    else propertyValue

  def commit: Wertung = {
    lastCommitted.wettkampfdisziplin.verifiedAndCalculatedWertung(
      init.toWertung.copy(
        noteD = toOption(noteD.value),
        noteE = toOption(noteE.value),
        endnote = toOption(endnote.value),
        variables = mapVariablen)
      )
  }

  def updateAndcommit: Wertung = {
    lastCommitted = lastCommitted.updatedWertung(commit)
    reset
    lastCommitted.toWertung
  }


  def view: WertungView =
    init.updatedWertung(commit)
}
