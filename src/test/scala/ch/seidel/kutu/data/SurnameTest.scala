package ch.seidel.kutu.data

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SurnameTest extends FunSuite {

  test("testIsSurname") {
    assert(Surname.isSurname("Roland").isDefined === true)
  }

  test("testIsMasculin") {
    assert(Surname.isSurname("Roland").get.isMasculin === true)
    assert(Surname.isSurname("Roland").get.isFeminin === false)
  }

  test("testIsFeminim") {
    assert(Surname.isSurname("Susanna").get.isMasculin === false)
    assert(Surname.isSurname("Susanna").get.isFeminin === true)
  }

}
