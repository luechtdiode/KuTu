package ch.seidel.kutu.view.player

import javafx.event.EventHandler
import javafx.scene.control.{SkinBase, Slider}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane


class KnobSkin(slider: Slider) extends SkinBase[Slider](slider) {
  val behavior = new KnobBehavior(slider)
  private var knobRadius = .0
  private val minAngle = -140
  private val maxAngle = 140
  private var dragOffset = .0
  private var knob: StackPane = null
  private var knobOverlay: StackPane = null
  private var knobDot: StackPane = null
  initialize()
  //requestLayout
  registerChangeListener(slider.minProperty, _ => rotateKnob())
  registerChangeListener(slider.maxProperty, _ => rotateKnob())
  registerChangeListener(slider.valueProperty, _ => rotateKnob())

  private def initialize(): Unit = {
    knob = new StackPane() {
      override protected def layoutChildren(): Unit = {
        knobDot.autosize()
        knobDot.setLayoutX((knob.getWidth - knobDot.getWidth) / 2)
        knobDot.setLayoutY(5 + (knobDot.getHeight / 2))
      }
    }
    knob.getStyleClass.setAll("knob")
    knobOverlay = new StackPane
    knobOverlay.getStyleClass.setAll("knobOverlay")
    knobDot = new StackPane
    knobDot.getStyleClass.setAll("knobDot")
    getChildren.setAll(knob, knobOverlay)
    knob.getChildren.add(knobDot)
    getSkinnable.setOnMousePressed(new EventHandler[MouseEvent]() {
      override def handle(me: MouseEvent): Unit = {
        val dragStart = mouseToValue(me.getX, me.getY)
        val zeroOneValue = (slider.getValue - slider.getMin) / (slider.getMax - slider.getMin)
        dragOffset = zeroOneValue - dragStart
        behavior.knobPressed(me, dragStart)
      }
    })
    slider.setOnMouseReleased(new EventHandler[MouseEvent]() {
      override def handle(me: MouseEvent): Unit = {
        behavior.knobRelease(me, mouseToValue(me.getX, me.getY))
      }
    })
    slider.setOnMouseDragged(new EventHandler[MouseEvent]() {
      override def handle(me: MouseEvent): Unit = {
        val move = mouseToValue(me.getX, me.getY)
        behavior.knobDragged(me, move + dragOffset)
      }
    })
  }

  private def mouseToValue(mouseX: Double, mouseY: Double) = {
    val cx = slider.getWidth / 2
    val cy = slider.getHeight / 2
    val mouseAngle = Math.toDegrees(Math.atan((mouseY - cy) / (mouseX - cx)))
    var topZeroAngle = .0
    if mouseX < cx then topZeroAngle = 90 - mouseAngle
    else topZeroAngle = -(90 + mouseAngle)
    val value = 1 - ((topZeroAngle - minAngle) / (maxAngle - minAngle))
    value
  }

  private def rotateKnob(): Unit = {
    val s = slider
    val zeroOneValue = (s.getValue - s.getMin) / (s.getMax - s.getMin)
    val angle = minAngle + ((maxAngle - minAngle) * zeroOneValue)
    knob.setRotate(angle)
  }

  protected def layoutChildren(): Unit = {
    // calculate the available space
    val x = slider.getInsets.getLeft
    val y = slider.getInsets.getTop
    val w = slider.getWidth - (slider.getInsets.getLeft + slider.getInsets.getRight)
    val h = slider.getHeight - (slider.getInsets.getTop + slider.getInsets.getBottom)
    val cx = x + (w / 2)
    val cy = y + (h / 2)
    // resize thumb to preferred size
    val knobWidth = knob.prefWidth(-1)
    val knobHeight = knob.prefHeight(-1)
    knobRadius = Math.max(knobWidth, knobHeight) / 2
    knob.resize(knobWidth, knobHeight)
    knob.setLayoutX(cx - knobRadius)
    knob.setLayoutY(cy - knobRadius)
    knobOverlay.resize(knobWidth, knobHeight)
    knobOverlay.setLayoutX(cx - knobRadius)
    knobOverlay.setLayoutY(cy - knobRadius)
    rotateKnob()
  }

  protected def computeMinWidth(height: Double): Double = slider.getInsets.getLeft + knob.minWidth(-(1)) + slider.getInsets.getRight

  protected def computeMinHeight(width: Double): Double = slider.getInsets.getTop + knob.minHeight(-(1)) + slider.getInsets.getBottom

  protected def computePrefWidth(height: Double): Double = slider.getInsets.getLeft + knob.prefWidth(-(1)) + slider.getInsets.getRight

  protected def computePrefHeight(width: Double): Double = slider.getInsets.getTop + knob.prefHeight(-(1)) + slider.getInsets.getBottom

  protected def computeMaxWidth(height: Double): Double = Double.MaxValue

  protected def computeMaxHeight(width: Double): Double = Double.MaxValue
}