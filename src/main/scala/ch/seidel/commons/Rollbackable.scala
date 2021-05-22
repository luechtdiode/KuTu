package ch.seidel.commons

import javafx.beans.value.WritableObjectValue

trait Rollbackable[T] extends WritableObjectValue[T] {

  protected var originalValue: Option[T] = None

  def isDirty(): Boolean = originalValue.isDefined

  def rollback(): Unit = {
    originalValue match {
      case Some(v) => {
        set(v)
        originalValue = None
      }
      case None =>
    }
  }
  def commit(): Unit = originalValue = None
}