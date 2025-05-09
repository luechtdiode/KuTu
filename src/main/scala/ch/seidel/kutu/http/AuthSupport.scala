package ch.seidel.kutu.http

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.marshalling.Marshal
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers._
import org.apache.pekko.http.scaladsl.model.ws.{Message, WebSocketRequest}
import org.apache.pekko.http.scaladsl.server.Directives
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.http.scaladsl.server.directives.Credentials.Provided
import org.apache.pekko.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.http.scaladsl.{ClientTransport, Http}
import org.apache.pekko.stream.scaladsl._
import ch.seidel.commons.PageDisplayer
import ch.seidel.jwt
import ch.seidel.kutu.Config
import ch.seidel.kutu.Config._
import ch.seidel.kutu.domain.Wettkampf
import spray.json.{JsObject, RootJsonFormat}

import java.net.{InetSocketAddress, PasswordAuthentication}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

object AuthSupport {
  val OPTION_LOGINRESET = ":option-loginreset"

  private[AuthSupport] var proxyPort: Option[String] = None
  private[AuthSupport] var proxyHost: Option[String] = None
  private[AuthSupport] var proxyUser: Option[String] = None
  private[AuthSupport] var proxyPassword: Option[String] = None
  private[AuthSupport] var clientheader: Option[RawHeader] = None
  
  case class UserCredentials(username: String, password: String)
  
  def getClientSecret: Option[String] = clientheader.map(_.value)
}

trait AuthSupport extends Directives with SprayJsonSupport with Hashing with JwtSupport {
  import AuthSupport._
  import spray.json.DefaultJsonProtocol._
  
  implicit val credsFormat: RootJsonFormat[UserCredentials] = jsonFormat2(UserCredentials)
  
  autoconfigProxy match {
    case (Some(host), Some(port)) =>
      proxyPort = Some(port)
      proxyHost = Some(host)
    case _ =>
  }
  
  def setProxyProperties(port: String, host: String, user: String, password: String): Unit = {
    proxyPort = Some(port)
    proxyHost = Some(host)
    proxyUser = Some(user)
    proxyPassword = Some(password)
  }
//  
//  Authenticator.setDefault(new Authenticator() {
//    override protected def getPasswordAuthentication(): PasswordAuthentication = {
//      import javafx.scene.{ control => jfxsc }
//      import scalafx.Includes._
//      import scalafx.scene.control._
//      import scalafx.scene.layout._
//      import scalafx.scene._
//      import scalafx.beans.binding._
//      import ch.seidel.commons.DisplayablePage
//      import ch.seidel.commons.PageDisplayer
//      
//      if (getRequestorType() == RequestorType.PROXY) {
//        val txtUsername = new TextField {
//          prefWidth = 500
//          promptText = "Username"
//          text = System.getProperty("user.name")
//        }
//    
//        val txtPassword = new PasswordField {
//          prefWidth = 500
//          promptText = "Internet Proxy Passwort"
//        }
//        PageDisplayer.showInDialogFromRoot("Internet Proxy authentication", new DisplayablePage() {
//          def getPage: Node = {
//            new BorderPane {
//              hgrow = Priority.Always
//              vgrow = Priority.Always
//              center = new VBox {
//                children.addAll(
//                    new Label(txtUsername.promptText.value), txtUsername,
//                    new Label(txtPassword.promptText.value), txtPassword
//                    )
//              }
//            }
//          }
//        }, new Button("OK") {
//          disable <== when(Bindings.createBooleanBinding(() => {
//                                txtUsername.text.isEmpty.value && txtPassword.text.isEmpty().value
//                              },
//                                txtUsername.text, txtPassword.text
//                              )) choose true otherwise false
//          onAction = () =>
//            setProxyProperties(
//                host = Config.proxyHost.getOrElse(""), 
//                port = Config.proxyPort.getOrElse(""),
//                user = txtUsername.text.value.trim(),
//                password = txtPassword.text.value.trim())
//          }
//        })
//        getProxyAuth
//      } else { 
//        super.getPasswordAuthentication()
//      }
//    }               
//  })
//    
  def askForUsernamePassword: Option[Seq[String]] = PageDisplayer.askFor("Proxy Login", ("Username", System.getProperty("user.name")), ("Passwort*", proxyPassword.getOrElse("")))
  
  def getProxyAuth: PasswordAuthentication = askForUsernamePassword match {
    case Some(Seq(username, password)) => 
      proxyUser = Some(username)
      proxyPassword = Some(password)
      new PasswordAuthentication(username, proxyPassword.get.toCharArray)
    case _ => new PasswordAuthentication(proxyUser.get, proxyPassword.get.toCharArray)
  }
  
  def httpsProxyTransport: Option[ClientTransport] = proxyHost.flatMap(h =>
    proxyPort.map(p =>
      (proxyUser, proxyPassword) match {
        case (Some(user), Some(password)) => ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(h, Integer.valueOf(p)), headers.BasicHttpCredentials(user, password))
        case _ => askForUsernamePassword match {
          case Some(Seq(user, password)) => 
            proxyUser = Some(user)
            proxyPassword = Some(password)
            ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(h, Integer.valueOf(p)), headers.BasicHttpCredentials(user, password))
          case _ =>  ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(h, Integer.valueOf(p)))
        }
      }
    ))
    
  def clientsettings: ClientConnectionSettings = httpsProxyTransport match {
    case Some(pt) => ClientConnectionSettings(Core.system).withTransport(pt)
    case _        => ClientConnectionSettings(Core.system)
  }

  def poolsettings: ConnectionPoolSettings = ConnectionPoolSettings(Core.system).withConnectionSettings(clientsettings)

  def verify(credentials: Provided, userSecretHashLookup: (String) => String): Boolean = {
    val hash = userSecretHashLookup(credentials.identifier)
    credentials.verify(hash, matchHashed(hash))
  }

  def knownVerein(credentials: Provided, userLookup: (String) => Option[Long]): Boolean = {
    userLookup(credentials.identifier) match {
      case Some(_) => true
      case None    => false
    }
  }

  def userPassAuthenticator(userSecretHashLookup: (String) => String, userLookup: (String) => Option[Long]): AuthenticatorPF[String] = {
    case p @ Credentials.Provided(id) if verify(p, userSecretHashLookup) => id
    case p @ Credentials.Provided(id) if knownVerein(p, userLookup) =>
      id + OPTION_LOGINRESET
  }

  /**
   * Supports header- and body-based request->response with credentials->acces-token
   */
  def httpLoginRequest(uri: String, user: String, pw: String): Future[Unit] = {
    import Core._
    import HttpMethods._
    Marshal(UserCredentials(user, pw)).to[RequestEntity] flatMap { entity =>
      Http().singleRequest(
          HttpRequest(method = POST, uri = uri, entity = entity).addHeader(Authorization(BasicHttpCredentials(user, pw))), settings = poolsettings).map {
        case HttpResponse(StatusCodes.OK, headers, entity, _) =>
          clientheader = headers.find(h => h.is(jwtAuthorizationKey)).flatMap {
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
        case HttpResponse(status, headers, entity, _) => entity match {
          case HttpEntity.Strict(_, text) =>
            throw HTTPFailure(status, text.utf8String)
          case x =>
            throw HTTPFailure(status, x.toString)
        }
      }
    }
  }

  def loginWithWettkampf(p: Wettkampf): Future[HttpResponse] = if (Config.isLocalHostServer) {
    if (!p.hasSecred(homedir, "localhost")) {
      p.saveSecret(homedir, "localhost", jwt.JsonWebToken(jwtHeader, setClaims(p.uuid.get, Int.MaxValue), jwtSecretKey))
    }
    httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", p.uuid.get, p.readSecret(homedir, "localhost").get)
  } else {
    p.uuid.zip(p.readSecret(homedir, remoteHostOrigin)) match {
      case Some((uuid, secret)) =>
        httpRenewLoginRequest(s"$remoteBaseUrl/api/loginrenew", uuid, secret)
      case None =>
        throw new IllegalStateException(s"Der Wettkampf ${p.easyprint} wurde noch nicht im Netz bereitgestellt.")
    }
  }

  def httpRenewLoginRequest(uri: String, wettkampfuuid: String, jwtToken: String): Future[HttpResponse] = {
    import Core._
    import HttpMethods._
    Marshal(UserCredentials(wettkampfuuid, jwtToken)).to[RequestEntity] flatMap { entity =>
      val request = HttpRequest(method = POST, uri = uri, entity = entity)
      val requestWithHeader = request.withHeaders(request.headers :+ RawHeader(jwtAuthorizationKey, jwtToken))
      Http().singleRequest(requestWithHeader, settings = poolsettings).map {
        case response @ HttpResponse(StatusCodes.OK, headers, entity, _) =>
          clientheader = headers.find(h => h.is(jwtAuthorizationKey)).flatMap {
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
      
        case HttpResponse(status, headers, entity, _) => entity match {
          case HttpEntity.Strict(_, text) =>
            throw HTTPFailure(status, text.utf8String)
          case x =>
            throw HTTPFailure(status, x.toString)
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
  
  def withAuthHeader(request: HttpRequest): HttpRequest = {
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
    val (response, streamTerminationPromise) = Http().singleWebSocketRequest(request, clientFlow = flow, settings = clientsettings)

    // make sure that the connection could be established
    Await.result(response, Duration.Inf)

    streamTerminationPromise
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
  def makeHttpGetRequest(url: String): HttpRequest = {
    import HttpMethods._
    withAuthHeader(HttpRequest(GET, uri=url))
  }
  
  def httpGetClientRequest(uri: String): Future[HttpResponse] = {
    import HttpMethods._
    httpClientRequest(HttpRequest(GET, uri=uri))
  }

}