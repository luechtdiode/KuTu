package ch.seidel.commons

import javafx.beans.property.SimpleStringProperty

class RollbackableStringProperty(bean: Object = null, name: String = null, initialValue: String = null)
  extends SimpleStringProperty(initialValue) with Rollbackable[String] {

  def this(initialValue_ : String) = this(initialValue = initialValue_)

  def this(bean_ : Object, name_ : String) = this(bean = bean_, name = name_)

  def this() = this(null)

  override def set(newValue: String) = {
    if (originalValue.isEmpty)
      originalValue = Some(get)
    super.set(newValue)
  }

  // enabling tests
  override def fireValueChangedEvent(): Unit = super.fireValueChangedEvent
}