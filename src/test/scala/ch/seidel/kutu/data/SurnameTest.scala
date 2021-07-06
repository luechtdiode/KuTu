package ch.seidel.kutu.data

import org.scalatest.funsuite.AnyFunSuite

class SurnameTest extends AnyFunSuite {

  test("testIsSurname") {
    assert(Surname.isSurname("Roland").isDefined === true)
  }

  test("testIsMasculin") {
    assert(Surname.isSurname("Roland").get.isMasculin === true)
    assert(Surname.isSurname("Roland").get.isFeminin === false)
  }

  test("testIsFeminin") {
    assert(Surname.isSurname("Susanna").get.isMasculin === false)
    assert(Surname.isSurname("Susanna").get.isFeminin === true)
  }

  test("testMultipleSurnames") {
    // one of them must be unambiguously, all of them should'nt be ambigously
    assert(Surname.isSurname("Dafne Susanna").get.isMasculin === false)
    assert(Surname.isSurname("Dafne Susanna").get.isFeminin === true)
    assert(Surname.isSurname("Steve Robin").get.isMasculin === true)
    assert(Surname.isSurname("Steve Robin").get.isFeminin === false)
    assert(Surname.isSurname("Roland Annette").get.isFeminin === true)
  }

  test("testObjectMultipleSurnames") {
    // one of them must be unambiguously, all of them should'nt be ambigously
    assert(Surname.isMasculin("Dafne Susanna") === false)
    assert(Surname.isFeminim("Dafne Susanna") === true)

    assert(Surname.isMasculin("Steve Robin") === true)
    assert(Surname.isFeminim("Steve Robin") === false)

    assert(Surname.isFeminim("Roland Annette") === false)
    assert(Surname.isMasculin("Roland Annette") === false)
  }

}
