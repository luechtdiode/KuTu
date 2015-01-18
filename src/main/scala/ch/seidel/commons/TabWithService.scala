package ch.seidel.commons

import ch.seidel.domain.KutuService

trait TabWithService {
  val service: KutuService
  lazy val populated = isPopulated
  def isPopulated: Boolean
}