package ch.seidel.jwt

import spray.json.*
import spray.json.DefaultJsonProtocol.*

import scala.util.Try

case class JwtHeader(
  algorithm: Option[String],
  contentType: Option[String],
  typ: Option[String]
):

  def asJsonString: String =
    val obj = JsObject(
      algorithm.map(x => ("alg", JsString(x))).toMap ++
      contentType.map(x => ("cty", JsString(x))).toMap ++
      typ.map(x => ("typ", JsString(x))).toMap
    )
    obj.compactPrint

end JwtHeader

object JwtHeader:

  def apply(algorithm: String, contentType: String = null, typ: String = "JWT"): JwtHeader =
    JwtHeader(Option(algorithm), Option(contentType), Option(typ))

  def fromJsonString(jsonString: String): JwtHeader =
    val ast = jsonString.parseJson.asJsObject

    val alg = ast.fields.get("alg").flatMap {
      case JsString(s) => Some(s)
      case _ => None
    }
    val cty = ast.fields.get("cty").flatMap {
      case JsString(s) => Some(s)
      case _ => None
    }
    val typVal = ast.fields.get("typ").flatMap {
      case JsString(s) => Some(s)
      case _ => None
    }

    JwtHeader(alg, cty, typVal)

  def fromJsonStringOpt(jsonString: String): Option[JwtHeader] =
    Try(fromJsonString(jsonString)).toOption

end JwtHeader