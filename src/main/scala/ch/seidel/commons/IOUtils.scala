package ch.seidel.commons

import scala.io.Source

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

}
