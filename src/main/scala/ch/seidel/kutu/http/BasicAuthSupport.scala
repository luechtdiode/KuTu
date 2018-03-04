package ch.seidel.kutu.http

import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.InetAddress

import authentikat.jwt._
import java.util.concurrent.TimeUnit

import akka.util.ByteString
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.{ Directives, Directive1, Route }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpHeader$ParsingResult._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import spray.json.JsValue
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.JsObject
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import ch.seidel.kutu.Config._

trait BasicAuthSupport extends Directives with SprayJsonSupport with Hashing {
  import spray.json.DefaultJsonProtocol._
  
  def userPassAuthenticator(userSecretHashLookup: (String) => String): AuthenticatorPF[String] = {
    case p @ Credentials.Provided(id) if p.verify(userSecretHashLookup(id), sha256) => id
  }
  
  private var clientheader: Option[RawHeader] = None
  
  
  /**
   * Supports header- and body-based request->response with credentials->acces-token
   */
  def httpLoginRequest(uri: String, user: String, pw: String) = {
    import HttpMethods._
    case class UserCredentials(username: String, password: String)
    implicit val credsFormat = jsonFormat2(UserCredentials)
    import Core._
    Marshal(UserCredentials(user, pw)).to[RequestEntity] flatMap { entity =>
      Http().singleRequest(
          HttpRequest(method = POST, uri = uri, entity = entity).addHeader(Authorization(BasicHttpCredentials(user, pw)))).map {
        case HttpResponse(StatusCodes.OK, headers, entity, _) =>
          clientheader = headers.filter(h => h.is(jwtAuthorizationKey)).headOption.flatMap {
            case HttpHeader(_, token) => 
              entity.discardBytes()
              Some(RawHeader(jwtAuthorizationKey, token))
          } match {
            case token @ Some(_) => token
            case _ => Await.result(Unmarshal(entity).to[JsObject].map{json =>
                  json.getFields("token").map(field => RawHeader(jwtAuthorizationKey, field.toString)).headOption
              }, Duration.Inf)            
          }
          println(s"New JWT: $clientheader")
        case x => println("something wrong", x.toString())
      }
    }
  }
  
  def httpRenewLoginRequest(uri: String, wettkampfuuid: String, jwtToken: String) = {
    import HttpMethods._
    import Core._

    Marshal(wettkampfuuid).to[RequestEntity] flatMap { entity =>
      val request = HttpRequest(method = POST, uri = uri, entity = entity)
      Http().singleRequest(request.withHeaders(request.headers :+ RawHeader(jwtAuthorizationKey, jwtToken))).map {r => r match {
        case HttpResponse(StatusCodes.OK, headers, entity, _) =>
          clientheader = headers.filter(h => h.is(jwtAuthorizationKey)).headOption.flatMap {
            case HttpHeader(_, token) => 
              entity.discardBytes()
              Some(RawHeader(jwtAuthorizationKey, token))
          } match {
            case token @ Some(_) => token
            case _ => Await.result(Unmarshal(entity).to[JsObject].map{json =>
                  json.getFields("token").map(field => RawHeader(jwtAuthorizationKey, field.toString)).headOption
              }, Duration.Inf)            
          }
          println(s"renewed JWT: $clientheader")
          r
        case x => println("something wrong", x.toString())
          r
        }
      }
    }
  }
  
  def withAuthHeader(request: HttpRequest) = {
    clientheader match {
      case Some(ch) => request.withHeaders(request.headers :+ ch)
      case _ => request
    }
  }
  
  def httpClientRequest(request: HttpRequest): Future[HttpResponse] = {
    import Core._
    Http().singleRequest(withAuthHeader(request))
  }
  
  def httpPutClientRequest(uri: String, entity: RequestEntity): Future[HttpResponse] = {
    import HttpMethods._
    httpClientRequest(HttpRequest(PUT, uri=uri, entity = entity))
  }
  
  def httpPostClientRequest(uri: String, entity: RequestEntity): Future[HttpResponse] = {
    import HttpMethods._
    httpClientRequest(HttpRequest(POST, uri=uri, entity = entity))
  }
  def makeHttpGetRequest(url: String) = {
    import HttpMethods._
    withAuthHeader(HttpRequest(GET, uri=url))
  }
  
  def httpGetClientRequest(uri: String): Future[HttpResponse] = {
    import HttpMethods._
    httpClientRequest(HttpRequest(GET, uri=uri))
  }

}