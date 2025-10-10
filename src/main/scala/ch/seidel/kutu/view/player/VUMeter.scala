package ch.seidel.kutu.view.player

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Parent
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

import java.util

class VUMeter extends Parent {
  final private val BAR_COLOR = Color.web("#cf0f0f")
  private val bars = new util.ArrayList[Rectangle](20)
  private val value = new SimpleDoubleProperty(0) {
    override protected def invalidated(): Unit = {
      super.invalidated()
      val lastBar = get * bars.size()
      for (i <- 0 until bars.size()) {
        bars.get(i).setVisible(i < lastBar)
      }
    }
  }
  for (i <- 0 until 20) {
    val rectangle = new Rectangle(26, 2) {
      setFill(BAR_COLOR)
      setX(-13)
      setY(1 - (i * 4))
      setVisible(false)
    }
    bars.add(rectangle)
  }
  getChildren.addAll(bars)

  val effect: DropShadow = new DropShadow {
    setBlurType(BlurType.TWO_PASS_BOX)
    setRadius(10)
    setSpread(0.4)
    setColor(Color.web("#b10000"))
  }
  setEffect(effect)

  def setValue(v: Double): Unit = {
    value.set(v)
  }

  def getValue: Double = value.get

  def valueProperty: DoubleProperty = value
}