package ch.seidel.kutu.view

import ch.seidel.kutu.calc.{ScoreCalcTemplateView, ScoreCalcVariable}
import ch.seidel.kutu.domain._
import scalafx.beans.property.{BufferProperty, DoubleProperty, ObjectProperty}
import scalafx.collections.ObservableBuffer

import scala.jdk.CollectionConverters.IterableHasAsJava

case class WertungEditor(private var lastCommitted: WertungView) {
  import ch.seidel.kutu.domain.given_Conversion_Double_String

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
  val athletText = ObjectProperty[String](this, "athlet", "")
  val noteD = DoubleProperty(Double.NaN)
  val noteE = DoubleProperty(Double.NaN)
  val endnote = DoubleProperty(Double.NaN)
  val dVariables = new BufferProperty[ScoreCalcVariableEditor](this, "DVariablen", ObservableBuffer[ScoreCalcVariableEditor]())
  val eVariables = new BufferProperty[ScoreCalcVariableEditor](this, "EVariablen", ObservableBuffer[ScoreCalcVariableEditor]())
  val pVariables = new BufferProperty[ScoreCalcVariableEditor](this, "PVariablen", ObservableBuffer[ScoreCalcVariableEditor]())
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
        score.value = score.value.updated(BigDecimal(nv).setScale(score.value.scale.getOrElse(0)))
      } catch {
        case e:NumberFormatException =>
          score.value = score.value.updated(BigDecimal(0).setScale(score.value.scale.getOrElse(0)))
      }
    }
    var notifying = false
    score.onChange {
      if !notifying then try {
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
    if !notifying then try {
      notifying = true

      val wertung = commit
      wertung.noteD match {
        case Some(d) => noteD.value = d.toDouble
        case _ => noteD.value = Double.NaN
      }
      wertung.noteE match {
        case Some(d) => noteE.value = d.toDouble
        case _ => noteE.value = Double.NaN
      }
      wertung.endnote match {
        case Some(d) => endnote.value = d.toDouble
        case _ => endnote.value = Double.NaN
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

    athletText.value = {
      val hasmedia = lastCommitted.mediafile.nonEmpty
      s"${lastCommitted.athlet.vorname} ${lastCommitted.athlet.name} ${
        (lastCommitted.athlet.gebdat match {
          case Some(d) => f"$d%tY "
          case _ => " "
        })
      }${if hasmedia then " ♪" else ""}"
    }

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
    if propertyValue.equals(Double.NaN) && initValue.isEmpty then {
      false
    } else if initValue.map(_.toDouble).contains(propertyValue) then {
      false
    } else {
      true
    }
  }

  def clearInput(): Unit = {
    notifying = true
    try {
      noteD.value = Double.NaN
      noteE.value = Double.NaN
      endnote.value = Double.NaN
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
      dVariables.value.foreach(v => v.stringvalue.value = "0")
      eVariables.value.foreach(v => v.stringvalue.value = "0")
      pVariables.value.foreach(v => v.stringvalue.value = "0")
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
          dDetails = false,
          eVariables = eVariables.value.toList.map(_.score.value),
          eDetails = false,
          pVariables = pVariables.value.toList.map(_.score.value),
          pDetails = false)
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
      buffer.value.foreach(e => if e.score.value.equalID(v) then {
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
          noteE.value = Double.NaN
      }
      lastCommitted.endnote match {
        case Some(d) =>
          endnote.value = d.toDouble
        case _ =>
          endnote.value = Double.NaN
      }
      resetVariables()
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
          case Some(dVar) =>
            dVar.stringvalue.value = lastCommitted.noteD.map(_.toString).getOrElse("")
          case None =>
        }
        eVariables.value.headOption match {
          case Some(eVar) => eVar.stringvalue.value = lastCommitted.noteE.map(_.toString).getOrElse("")
          case None =>
        }
      case _ =>
    }
  }

  def toOption(propertyValue: Double): Option[BigDecimal] =
    if propertyValue.toString == Double.NaN.toString then None
    else Some(scala.math.BigDecimal(propertyValue))

  def toDouble(propertyValue: Double) = {
    if propertyValue.toString == Double.NaN.toString then 0d
    else scala.math.BigDecimal(propertyValue).doubleValue
  }

  def toString(propertyValue: Double): String =
    if propertyValue.toString == Double.NaN.toString then ""
    else propertyValue

  def commit: Wertung = {
    lastCommitted.wettkampfdisziplin.verifiedAndCalculatedWertung(
      lastCommitted.toWertung.copy(
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
