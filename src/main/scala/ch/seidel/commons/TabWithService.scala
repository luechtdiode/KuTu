package ch.seidel.commons

import ch.seidel.kutu.domain.KutuService

trait TabWithService {
  val service: KutuService
  lazy val populated = isPopulated
  def isPopulated: Boolean
  def release: Unit = {}
}