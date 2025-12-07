package ch.seidel.kutu.http

import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.server.{Directive, Directive1, Directives}


trait CIDSupport extends Directives with RouterLogging with IpToDeviceID {

  private lazy val clientIdKey = "clientid"

  def handleCID: Directive1[String] = {
    val value: Directive[(RemoteAddress, Option[String], Option[String])] =
      extractClientIP & parameter(Symbol("clientid").?) & optionalHeaderValueByName(clientIdKey)
    value.tflatMap(handleCIDWith)
  }

  private def handleCIDWith(cidOption: (RemoteAddress, Option[String], Option[String])): Directive1[String] = {
    val cid = makeDeviceId(cidOption._1, List(cidOption._2, cidOption._3).flatten.headOption)
    //log.mdc(Map("CID"->cid))
    provide(cid)
  }

}