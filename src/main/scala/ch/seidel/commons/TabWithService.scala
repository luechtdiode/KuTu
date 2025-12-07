package ch.seidel.commons

import ch.seidel.kutu.domain.KutuService

trait TabWithService {
  val service: KutuService
  lazy val populated: Boolean = isPopulated
  def isPopulated: Boolean
  def release: Unit = {}
}