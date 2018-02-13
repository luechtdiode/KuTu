package ch.seidel.kutu.domain

import org.slf4j.LoggerFactory

trait KutuService extends DBService 
  with VereinService 
  with AthletService 
  with WettkampfService 
  with RiegenService
  with DurchgangService
  with KampfrichterService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  
}