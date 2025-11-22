package ch.seidel.kutu.http

import java.nio.charset.StandardCharsets
import java.security.spec.InvalidKeySpecException
import java.security.{MessageDigest, NoSuchAlgorithmException, SecureRandom}
import java.text.SimpleDateFormat
import java.util.{Base64, Date}
import org.apache.pekko.http.scaladsl.model.RemoteAddress

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import spray.json.{JsString, JsValue, JsonReader, *}

import java.sql
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}
import scala.util.Try

trait EnrichedJson {

  implicit class RichJson(jsValue: JsValue) {
    def asOpt[T](implicit reader: JsonReader[T]): Option[T] = Try(jsValue.convertTo[T]).toOption

    def canConvert[T](implicit reader: JsonReader[T]): Boolean = Try(jsValue.convertTo[T]).isSuccess

    def withoutFields(fieldnames: String*): JsObject = {
      jsValue.asJsObject.copy(jsValue.asJsObject.fields -- fieldnames)
    }

    def addFields(fieldnames: Map[String, JsValue]): JsObject = {
      jsValue.asJsObject.copy(jsValue.asJsObject.fields ++ fieldnames)
    }

    def toJsonStringWithType[T](t: T): String = {
      jsValue.addFields(Map(("type" -> JsString(t.getClass.getSimpleName)))).compactPrint
    }
  }

  implicit class JsonString(string: String) {
    def asType[T](implicit reader: JsonReader[T]): T = string.parseJson.convertTo[T]

    def asJsonOpt[T](implicit reader: JsonReader[T]): Option[T] = Try(string.parseJson.convertTo[T]).toOption

    def canConvert[T](implicit reader: JsonReader[T]): Boolean = Try(string.parseJson.convertTo[T]).isSuccess
  }

  implicit object DateFormat extends JsonFormat[Date] {
    private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
      override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }
    private val localIsoDateTimeFormatter = new ThreadLocal[DateTimeFormatter] {
      override def initialValue(): DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T00:00:00.000+0000'")
    }

    private def dateToIsoString(date: Date) = localIsoDateTimeFormatter.get().format(LocalDate.ofInstant(date.toInstant, ZoneId.of("Z")))

    private def parseIsoDateString(date: String): Option[Date] = Try {
      localIsoDateFormatter.get().parse(date)
    }.toOption

    def write(date: Date): JsString = JsString(dateToIsoString(date))

    def read(json: JsValue) = json match {
      case JsString(rawDate) =>
        parseIsoDateString(rawDate)
          .fold(deserializationError(s"Expected ISO Date format, got $rawDate"))(identity)
      case error => deserializationError(s"Expected JsString, got $error")
    }
  }


  implicit object SqlDateFormat extends JsonFormat[java.sql.Date] {
    private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
      override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }

    private val localIsoDateTimeFormatter = new ThreadLocal[DateTimeFormatter] {
      override def initialValue() = DateTimeFormatter.ofPattern("yyyy-MM-dd'T00:00:00.000+0000'")
    }

    private def dateToIsoString(date: java.sql.Date) = localIsoDateTimeFormatter.get().format(date.toLocalDate)

    private def parseIsoDateString(date: String): Option[java.sql.Date] = Try {
      new java.sql.Date(localIsoDateFormatter.get().parse(date).getTime)
    }.toOption

    def write(date: java.sql.Date): JsString = JsString(dateToIsoString(date))

    def read(json: JsValue): sql.Date = json match {
      case JsString(rawDate) =>
        parseIsoDateString(rawDate)
          .fold(deserializationError(s"Expected ISO Date format, got $rawDate"))(identity)
      case error => deserializationError(s"Expected JsString, got $error")
    }
  }


  import scala.reflect.ClassTag

  case class CaseObjectJsonSupport[T](values: Array[T]) extends RootJsonFormat[T] {
    // We can rely on the automatically generated valueOf method provided by Java/Scala enums
    val valueOfMethod: String => T = (name: String) => values.find(v => v.toString.equals(name)).get

    override def write(obj: T): JsValue = JsString(obj.toString) // Use .name() which is equivalent to .toString for enums

    override def read(json: JsValue): T = json match {
      case JsString(txt) =>
        try {
          valueOfMethod(txt)
        } catch {
          case _: IllegalArgumentException =>
            deserializationError(s"'$txt' is not a valid value for enum ${values.mkString("(", ", ", ")")}")
        }
      case somethingElse =>
        deserializationError(s"Expected a JsString for enum ${values.mkString("(", ", ", ")")} instead of $somethingElse")
    }
  }
}

trait Hashing {
  private val random = new SecureRandom

  def sha256(text: String): String = {
    // Create a message digest every time since it isn't thread safe!
    val digest = MessageDigest.getInstance("SHA-256")
    digest.digest(text.getBytes(StandardCharsets.UTF_8)).map("%02X".format(_)).mkString
  }

  def matchHashed(saltedSecretHash: String)(secret: String) = {
    val split = saltedSecretHash.split(":")
    val salt = Base64.getDecoder.decode(split(0))
    hashedWithSalt(secret, salt)
  }

  def hashed(secret: String) = {
    val saltb = new Array[Byte](16)
    random.nextBytes(saltb)
    hashedWithSalt(secret, saltb)
  }

  private def hashedWithSalt(secret: String, saltb: Array[Byte]) = {
    val iterationCount = 65536
    val spec = new PBEKeySpec(secret.toCharArray, saltb, iterationCount, 256)
    val salt = Base64.getEncoder.encodeToString(saltb)
    try {
      val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      salt + ":" + Base64.getEncoder.encodeToString(factory.generateSecret(spec).getEncoded)
    } catch {
      case e@(_: NoSuchAlgorithmException | _: InvalidKeySpecException) =>
        for (i <- 0 until iterationCount) {
          random.reseed()
        }
        salt + ":" + (salt + secret).hashCode
    }
  }
}

trait IpToDeviceID {
  def makeDeviceId(ip: RemoteAddress, context: Option[String]) =
    ip.toOption.map(_.getHostAddress).getOrElse("unknown") + context.map("@" + _).getOrElse("")

}
