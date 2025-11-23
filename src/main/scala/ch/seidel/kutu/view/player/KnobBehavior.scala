package ch.seidel.kutu.view.player

import com.sun.javafx.scene.control.behavior.BehaviorBase
import com.sun.javafx.scene.control.inputmap.InputMap
import javafx.scene.control.Slider
import javafx.scene.input.MouseEvent


object KnobBehavior {
  /*  protected val SLIDER_BINDINGS: List[Nothing] = new ArrayList[Nothing]

    try SLIDER_BINDINGS.add(new Nothing(TAB, "TraverseNext"))
    SLIDER_BINDINGS.add(new Nothing(TAB, "TraversePrevious").shift)
    SLIDER_BINDINGS.add(new Nothing(UP, "IncrementValue"))
    SLIDER_BINDINGS.add(new Nothing(KP_UP, "IncrementValue"))
    SLIDER_BINDINGS.add(new Nothing(DOWN, "DecrementValue"))
    SLIDER_BINDINGS.add(new Nothing(KP_DOWN, "DecrementValue"))
    SLIDER_BINDINGS.add(new Nothing(LEFT, "TraverseLeft"))
    SLIDER_BINDINGS.add(new Nothing(KP_LEFT, "TraverseLeft"))
    SLIDER_BINDINGS.add(new Nothing(RIGHT, "TraverseRight"))
    SLIDER_BINDINGS.add(new Nothing(KP_RIGHT, "TraverseRight"))
    SLIDER_BINDINGS.add(new Nothing(HOME, KEY_RELEASED, "Home"))
    SLIDER_BINDINGS.add(new Nothing(END, KEY_RELEASED, "End"))

   */
}


class KnobBehavior(slider: Slider) extends BehaviorBase[Slider](slider) {
  private var dragStartX: Double = .0
  private var dragStartY: Double = .0
  /*
    protected def callAction(name: String): Unit = {
      if ("Home" == (name)) {
        home()
      }
      else {
        if ("End" == (name)) {
          end()
        }
        else {
          if ("IncrementValue" == (name)) {
            incrementValue()
          }
          else {
            if ("DecrementValue" == (name)) {
              decrementValue()
            }
            else {
              super.callAction(name)
            }
          }
        }
      }
    }
  */

  /**
   * @param position The position of mouse in 0=min to 1=max range
   */
  def knobRelease(e: MouseEvent, position: Double): Unit = {
    val slider: Slider = getNode
    slider.setValueChanging(false)
    // detect click rather than drag
    if Math.abs(e.getX - dragStartX) < 3 && Math.abs(e.getY - dragStartY) < 3 then {
      slider.adjustValue((position + slider.getMin) * (slider.getMax - slider.getMin))
    }
  }

  /**
   * @param position The position of mouse in 0=min to 1=max range
   */
  def knobPressed(e: MouseEvent, position: Double): Unit = {
    // If not already focused, request focus
    val slider: Slider = getNode
    if !slider.isFocused then {
      slider.requestFocus()
    }
    slider.setValueChanging(true)
    dragStartX = e.getX
    dragStartY = e.getY
  }

  /**
   * @param position The position of mouse in 0=min to 1=max range
   */
  def knobDragged(e: MouseEvent, position: Double): Unit = {
    val slider: Slider = getNode
    slider.adjustValue(slider.getMin + position * (slider.getMax - slider.getMin))
  }

  private def home(): Unit = {
    val slider: Slider = getNode
    slider.adjustValue(slider.getMin)
  }

  private def decrementValue(): Unit = {
    getNode.decrement()
  }

  private  def end(): Unit = {
    val slider: Slider = getNode
    slider.adjustValue(slider.getMax)
  }

  private  def incrementValue(): Unit = {
    getNode.increment()
  }

  override def getInputMap: InputMap[Slider] = new InputMap[Slider](getNode)
}