package ch.seidel.kutu.domain

trait KutuService extends DBService
  with RegistrationService
  with VereinService 
  with AthletService 
  with WettkampfService 
  with RiegenService
  with DurchgangService
  with WertungsrichterService {
}