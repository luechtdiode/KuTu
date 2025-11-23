package ch.seidel.jwt

import spray.json.*
import spray.json.DefaultJsonProtocol.*

import scala.util.Try

sealed trait JwtClaimsSet:
  def asJsonString: String

object JwtClaimsSet:
  def apply(claims: Map[String, Any]): JwtClaimsSetMap = JwtClaimsSetMap(claims)
  
  def apply(jvalue: JsValue): JwtClaimsSetJValue = JwtClaimsSetJValue(jvalue)
  
  def apply(json: String): JwtClaimsSetJsonString = JwtClaimsSetJsonString(json)

end JwtClaimsSet

case class JwtClaimsSetMap(claims: Map[String, Any]) extends JwtClaimsSet:

  def asJsonString: String =
    val jsValue = claimsToJsValue(claims)
    jsValue.compactPrint

  private def claimsToJsValue(value: Any): JsValue = value match
    case null => JsNull
    case b: Boolean => JsBoolean(b)
    case n: Int => JsNumber(n)
    case n: Long => JsNumber(n)
    case n: Double => JsNumber(n)
    case n: java.math.BigDecimal => JsNumber(n)
    case s: String => JsString(s)
    case seq: Seq[_] => JsArray(seq.map(claimsToJsValue).toVector)
    case map: Map[_, _] => JsObject(map.map { case (k, v) => (k.toString, claimsToJsValue(v)) })
    case _ => JsString(value.toString)

end JwtClaimsSetMap

case class JwtClaimsSetJValue(jvalue: JsValue) extends JwtClaimsSet:

  def asJsonString: String =
    jvalue.compactPrint

  def asSimpleMap: Try[Map[String, String]] =
    Try {
      jvalue.asJsObject.fields.map { case (k, v) =>
        k -> (v match
          case JsString(s) => s
          case JsNumber(n) => n.toString
          case JsBoolean(b) => b.toString
          case JsNull => "null"
          case JsArray(_) | JsObject(_) => v.compactPrint
          case JsTrue => "true"
          case JsFalse => "false"
        )
      }
    }

end JwtClaimsSetJValue

case class JwtClaimsSetJsonString(json: String) extends JwtClaimsSet:

  def asJsonString: String = json

end JwtClaimsSetJsonString