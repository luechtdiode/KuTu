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
import akka.http.scaladsl.ClientTransport
import java.net.InetSocketAddress
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.settings.ConnectionPoolSettings
import scala.concurrent.Promise
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.WebSocketRequest

trait AuthSupport extends Directives with SprayJsonSupport with Hashing {
  import spray.json.DefaultJsonProtocol._
  
  private var proxyPort: Option[String] = None
  private var proxyHost: Option[String] = None
  private var proxyUser: Option[String] = None
  private var proxyPassword: Option[String] = None
  private var clientheader: Option[RawHeader] = None
  
  case class UserCredentials(username: String, password: String)
  implicit val credsFormat = jsonFormat2(UserCredentials)
  
  def setProxyProperties(port: String, host: String, user: String, password: String) {
    proxyPort = Some(port)
    proxyHost = Some(host)
    proxyUser = Some(user)
    proxyPassword = Some(password)
  }
  
  def httpsProxyTransport = proxyHost.flatMap(h => 
    proxyPort.map(p =>
      (proxyUser, proxyPassword) match {
        case (Some(user), Some(password)) => ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(h, Integer.valueOf(p)), headers.BasicHttpCredentials(user, password))
        case _ => ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(h, Integer.valueOf(p)))
      }
    ))
    
  def poolsettings = ConnectionPoolSettings(Core.system)
    .withConnectionSettings(httpsProxyTransport match { 
      case Some(pt) => ClientConnectionSettings(Core.system).withTransport(pt) 
      case _        => ClientConnectionSettings(Core.system)
    })
  
  def userPassAuthenticator(userSecretHashLookup: (String) => String): AuthenticatorPF[String] = {
    case p @ Credentials.Provided(id) if p.verify(userSecretHashLookup(id), sha256) => id
  }
  
  
  /**
   * Supports header- and body-based request->response with credentials->acces-token
   */
  def httpLoginRequest(uri: String, user: String, pw: String) = {
    import HttpMethods._
    import Core._
    Marshal(UserCredentials(user, pw)).to[RequestEntity] flatMap { entity =>
      Http().singleRequest(
          HttpRequest(method = POST, uri = uri, entity = entity).addHeader(Authorization(BasicHttpCredentials(user, pw))), settings = poolsettings).map {
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
        case HttpResponse(_, headers, entity, _) => entity match {
          case HttpEntity.Strict(_, text) =>
            throw new RuntimeException(text.utf8String)
          case x => 
            throw new RuntimeException(x.toString)
        }
      }
    }
  }
  
  def httpRenewLoginRequest(uri: String, wettkampfuuid: String, jwtToken: String) = {
    import HttpMethods._
    import Core._
    Marshal(UserCredentials(wettkampfuuid, jwtToken)).to[RequestEntity] flatMap { entity =>
      val request = HttpRequest(method = POST, uri = uri, entity = entity)
      Http().singleRequest(request.withHeaders(request.headers :+ RawHeader(jwtAuthorizationKey, jwtToken)), settings = poolsettings).map {
        case response @ HttpResponse(StatusCodes.OK, headers, entity, _) =>
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
          response
      
        case HttpResponse(_, headers, entity, _) => entity match {
          case HttpEntity.Strict(_, text) =>
            throw new RuntimeException(text.utf8String)
          case x => 
            throw new RuntimeException(x.toString)
        }
      }
    }
  }
  
  def httpGet[T](url: String): Future[String] = {
    import Core._
    httpGetClientRequest(url).flatMap{
        case HttpResponse(StatusCodes.OK, headers, entity, _) => Unmarshal(entity).to[String]
        case _ => Future{""}
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
    Http().singleRequest(withAuthHeader(request), settings = poolsettings)
  }
  
  def websocketClientRequest(request: WebSocketRequest, flow: Flow[Message, Message, Promise[Option[Message]]]) : Promise[Option[Message]] = {
    import Core._
    Http().singleWebSocketRequest(request, clientFlow = flow, settings = poolsettings.connectionSettings)._2
  }
  
  def httpPutClientRequest(uri: String, entity: RequestEntity): Future[HttpResponse] = {
    import HttpMethods._
    httpClientRequest(HttpRequest(PUT, uri=uri, entity = entity))
  }
  
  def httpPostClientRequest(uri: String, entity: RequestEntity): Future[HttpResponse] = {
    import HttpMethods._
    httpClientRequest(HttpRequest(POST, uri=uri, entity = entity))
  }
  def httpDeleteClientRequest(uri: String): Future[HttpResponse] = {
    import HttpMethods._
    httpClientRequest(HttpRequest(DELETE, uri=uri))
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