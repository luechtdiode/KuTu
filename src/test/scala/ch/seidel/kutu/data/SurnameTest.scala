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

}
