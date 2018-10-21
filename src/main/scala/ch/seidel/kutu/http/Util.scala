package ch.seidel.kutu.http

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import akka.http.scaladsl.model.RemoteAddress
import spray.json.{JsString, JsValue, JsonReader, _}

import scala.util.Try

trait EnrichedJson {
  implicit class RichJson(jsValue: JsValue) {
    def asOpt[T](implicit reader: JsonReader[T]): Option[T] = Try(jsValue.convertTo[T]).toOption
    def canConvert[T](implicit reader: JsonReader[T]): Boolean = Try(jsValue.convertTo[T]).isSuccess
    def withoutFields(fieldnames: String*) = {
      jsValue.asJsObject.copy(jsValue.asJsObject.fields -- fieldnames)
    }
    def addFields(fieldnames: Map[String, JsValue]) = {
      jsValue.asJsObject.copy(jsValue.asJsObject.fields ++ fieldnames)
    }
    def toJsonStringWithType[T](t: T) = {
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
  
    private def dateToIsoString(date: Date) = localIsoDateFormatter.get().format(date)
  
    private def parseIsoDateString(date: String): Option[Date] = Try{ localIsoDateFormatter.get().parse(date) }.toOption
    def write(date: Date) = JsString(dateToIsoString(date))
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
  
    private def dateToIsoString(date: java.sql.Date) = localIsoDateFormatter.get().format(date)
  
    private def parseIsoDateString(date: String): Option[java.sql.Date] = Try{ new java.sql.Date(localIsoDateFormatter.get().parse(date).getTime) }.toOption
    def write(date: java.sql.Date) = JsString(dateToIsoString(date))
    def read(json: JsValue) = json match {
      case JsString(rawDate) =>
        parseIsoDateString(rawDate)
          .fold(deserializationError(s"Expected ISO Date format, got $rawDate"))(identity)
      case error => deserializationError(s"Expected JsString, got $error")
    }
  }


  import scala.reflect.ClassTag
  import scala.reflect.runtime.universe._

  def getObjectInstance(clsName: String): AnyRef = {
    val mirror = runtimeMirror(getClass.getClassLoader)
    val module = mirror.staticModule(clsName)
    mirror.reflectModule(module).instance.asInstanceOf[AnyRef]
  }

  def objectBy[T: ClassTag](name: String): T = {
    val c = implicitly[ClassTag[T]]
    try {
      getObjectInstance(c + "$" + name + "$").asInstanceOf[T]
    } catch {
      case e: Exception => {
        val cnn = c.toString
        val cn = cnn.substring(0, cnn.lastIndexOf("."))
        getObjectInstance(cn + "." + name + "$").asInstanceOf[T]
      }
    }
  }

  def string2trait[T: TypeTag: ClassTag]: Map[JsValue, T] = {
    val clazz = typeOf[T].typeSymbol.asClass
    clazz.knownDirectSubclasses.filter(sc => sc.toString.startsWith("object ")).map { sc =>
      val objectName = sc.toString.stripPrefix("object ")
      (JsString(objectName), objectBy[T](objectName))
    }.toMap
  }

  class CaseObjectJsonSupport[T: TypeTag: ClassTag] extends RootJsonFormat[T] {
    val string2T: Map[JsValue, T] = string2trait[T]
    def defaultValue: T = deserializationError(s"${implicitly[ClassTag[T]].runtimeClass.getCanonicalName} expected")
    override def read(json: JsValue): T = string2T.getOrElse(json, defaultValue)
    override def write(value: T) = JsString(value.toString())
  }

}

trait Hashing {
  def sha256(text: String): String = {
    // Create a message digest every time since it isn't thread safe!
    val digest = MessageDigest.getInstance("SHA-256")
    digest.digest(text.getBytes(StandardCharsets.UTF_8)).map("%02X".format(_)).mkString
  }
}

trait IpToDeviceID {
  def makeDeviceId(ip: RemoteAddress) = ip.toOption.map(_.getHostAddress).getOrElse("unknown") + "@" + UUID.randomUUID().toString
}
