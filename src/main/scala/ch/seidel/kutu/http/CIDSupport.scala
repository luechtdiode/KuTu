package ch.seidel.kutu.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directive, Directive1, Directives}


trait CIDSupport extends Directives with RouterLogging with IpToDeviceID {

  private lazy val clientIdKey = "clientid"

  def handleCID: Directive1[String] = {
    val value: Directive[(RemoteAddress, Option[String], Option[String], Option[String])] =
      (extractClientIP & optionalHeaderValueByName("User-Agent") & parameter('clientid.?) & optionalHeaderValueByName(clientIdKey))
    value.tflatMap(handleCIDWith)
  }

  def handleCIDWith(cidOption: (RemoteAddress, Option[String], Option[String], Option[String])): Directive1[String] = {
    val cid = makeDeviceId(cidOption._1, List(cidOption._3, cidOption._4).flatten.headOption.map(_ + cidOption._2.map("/" + _).getOrElse("")))
    log.mdc(Map("CID"->cid))
    provide(cid)
  }

}