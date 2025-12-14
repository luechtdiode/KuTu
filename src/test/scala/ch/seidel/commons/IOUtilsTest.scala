package ch.seidel.commons

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IOUtilsTest extends AnyWordSpec
  with Matchers {

  "IOUtils" should {
    "release aquired resources correctly" in {
      var resource1Released = false
      var resource2Released = false

      val resource1 = new AutoCloseable {
        override def close(): Unit = {
          resource1Released = true
        }
      }

      val resource2 = new AutoCloseable {
        override def close(): Unit = {
          resource2Released = true
        }
      }

      IOUtils.withResources(resource1) { r1 =>
        IOUtils.withResources(resource2) { r2 =>
          // Use resources r1 and r2
          assert(r1 == resource1)
          assert(r2 == resource2)
        }
        resource2Released shouldBe true
      }
      resource1Released shouldBe true
    }

    "handle exceptions during resource usage and closing" in {
      var resourceReleased = false

      val resource = new AutoCloseable {
        override def close(): Unit = {
          resourceReleased = true
          throw new RuntimeException("Error during resource closing")
        }
      }

      val thrown = the[RuntimeException] thrownBy {
        IOUtils.withResources(resource) { r =>
          throw new RuntimeException("Error during resource usage")
        }
      }

      thrown.getMessage shouldBe "Error during resource usage"
      val suppressed = thrown.getSuppressed
      suppressed.length shouldBe 1
      suppressed(0).getMessage shouldBe "Error during resource closing"
      resourceReleased shouldBe true
    }
  }
}
