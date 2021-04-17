package ch.seidel.commons

import scala.io.Source
import scala.util.control.NonFatal

/** Helper functions related to I/O. */
object IOUtils {

  /** Load a resource as a string.
    *
    * @param reference using class loader of the `reference` object to load the resource
    * @param path to the resource, relative to the reference, or absolute of it starts with `/`
    */
  def loadResourceAsString(reference: Any, path: String): String = {
    val in = reference.getClass.getResourceAsStream(path)
    Source.fromInputStream(in).mkString
  }

  def withResources[T <: AutoCloseable, V](r: => T)(f: T => V): V = {
    val resource: T = r
    require(resource != null, "resource is null")
    var exception: Throwable = null
    try {
      f(resource)
    } catch {
      case NonFatal(e) =>
        exception = e
        throw e
    } finally {
      closeAndAddSuppressed(exception, resource)
    }
  }

  private def closeAndAddSuppressed(e: Throwable,
                                    resource: AutoCloseable): Unit = {
    if (e != null) {
      try {
        resource.close()
      } catch {
        case NonFatal(suppressed) =>
          e.addSuppressed(suppressed)
      }
    } else {
      resource.close()
    }
  }
}
