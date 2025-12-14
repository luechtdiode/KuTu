package ch.seidel.commons

import ch.seidel.javafx.JavaFxTestBase
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scalafx.Includes.jfxStringProperty2sfx

class RollbackableStringPropertyTest extends AnyWordSpec
  with Matchers
  with JavaFxTestBase {

  "RollbackableStringProperty" should {
    "be created with initial value" in {
      val prop = new RollbackableStringProperty("test value")
      prop.value should be("test value")
    }

    "be created without initial value" in {
      val prop = new RollbackableStringProperty()
      prop.value should be(null)
    }

    "be created with bean and name" in {
      val bean = new Object()
      val name = "propertyName"
      val prop = new RollbackableStringProperty(bean, name)
      prop.getBean should be(bean)
      prop.getName should be(name)
      prop.value should be(null)
    }

    "allow value changes" in {
      val prop = new RollbackableStringProperty("initial")
      prop.value should be("initial")

      prop.value = "new value"
      prop.value should be("new value")
      prop.isDirty() should be(true)

      prop.commit()
      prop.isDirty() should be(false)
    }

    "store initial value and allow rollback" in {
      val prop = new RollbackableStringProperty("initial")
      prop.value should be("initial")

      prop.value = "changed"
      prop.value should be("changed")
      prop.isDirty() should be(true)

      prop.rollback()
      prop.value should be("initial")
    }

    "handle multiple changes and rollbacks correctly" in {
      val prop = new RollbackableStringProperty("start")
      prop.value should be("start")

      prop.value = "first change"
      prop.value should be("first change")
      prop.isDirty() should be(true)

      prop.value = "second change"
      prop.value should be("second change")
      prop.isDirty() should be(true)

      prop.rollback()
      prop.value should be("start")
      prop.isDirty() should be(false)
    }

    "not affect the original value after rollback" in {
      val prop = new RollbackableStringProperty("original")
      prop.value should be("original")

      prop.value = "modified"
      prop.value should be("modified")
      prop.isDirty() should be(true)

      prop.rollback()
      prop.value should be("original")

      prop.value = "new change"
      prop.value should be("new change")
    }
  }
}
