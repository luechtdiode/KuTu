package ch.seidel.kutu.http

import ch.seidel.jwt.JsonWebToken
import ch.seidel.kutu.Config.{jwtAuthorizationKey, jwtHeader, jwtSecretKey}
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.*
import org.apache.pekko.http.scaladsl.model.HttpMethods.POST
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}

import spray.json.enrichString

import java.sql.Date
import java.util.UUID

class WettkampfRoutesSpec extends KuTuBaseSpec {

  private val creator = CreatorMetaData(
    creatorName = "Hans Muster",
    creatorAddress = "Musterstrasse 1, 1234 Musterstadt",
    creatorPhone = "+49 123 456789",
    termsVersion = "1.0"
  )

  private def legacyWettkampf(name: String): Wettkampf = {
    val wettkampf = createWettkampf(
      new Date(System.currentTimeMillis()), name, Set(20L), s"$name@test.ch", 3333, 7.5d,
      Some(UUID.randomUUID().toString), "", "", "", "Kategorie/AlterAufsteigend/Verein/Vorname/Name/Rotierend/AltInvers", "")
    wettkampf
  }

  /** entspricht der Alt-Reparatur: unendliche Gültigkeit, aber ohne admin-Claim */
  private def legacyJwtFor(userId: String): RawHeader =
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, setClaims(userId, Int.MaxValue), jwtSecretKey))

  private def jwtFor(userId: String, days: Long, isAdmin: Boolean = false): RawHeader =
    RawHeader(jwtAuthorizationKey, JsonWebToken(jwtHeader, setClaims(userId, days, isAdmin), jwtSecretKey))

  private def adminTokenUri(uuid: String): String = s"/api/competition/$uuid/admin-token"

  private def creatorEntity(metaData: CreatorMetaData = creator): HttpEntity.Strict =
    HttpEntity(ContentTypes.`application/json`, adminTokenRequestFormat.write(AdminTokenRequest(metaData)).compactPrint)

  private def withRoutes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  "the admin-token endpoint" should {

    "reject a request without a token" in {
      val wk = legacyWettkampf("AdminTokenNoAuth")
      HttpRequest(POST, adminTokenUri(wk.uuid.get), entity = creatorEntity()) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject a token with an invalid signature" in {
      val wk = legacyWettkampf("AdminTokenBadSignature")
      val forged = RawHeader(jwtAuthorizationKey,
        JsonWebToken(jwtHeader, setClaims(wk.uuid.get, Int.MaxValue), "falsches-geheimnis"))
      HttpRequest(POST, adminTokenUri(wk.uuid.get), entity = creatorEntity())
        .addHeader(forged) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject an expired token" in {
      val wk = legacyWettkampf("AdminTokenExpired")
      val expired = RawHeader(jwtAuthorizationKey,
        JsonWebToken(jwtHeader, setClaimsWithExpiry(wk.uuid.get, System.currentTimeMillis() - 10000), jwtSecretKey))
      HttpRequest(POST, adminTokenUri(wk.uuid.get), entity = creatorEntity())
        .addHeader(expired) ~> withRoutes ~> check {
        status should ===(StatusCodes.Unauthorized)
      }
    }

    "reject a token that belongs to a different competition" in {
      val wk = legacyWettkampf("AdminTokenOtherWk")
      val other = legacyWettkampf("AdminTokenOtherWkForeign")
      HttpRequest(POST, adminTokenUri(wk.uuid.get), entity = creatorEntity())
        .addHeader(legacyJwtFor(other.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Forbidden)
      }
    }

    "answer 404 for an unknown competition" in {
      val unknown = UUID.randomUUID().toString
      HttpRequest(POST, adminTokenUri(unknown), entity = creatorEntity())
        .addHeader(legacyJwtFor(unknown)) ~> withRoutes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }

    "answer 400 for a malformed body" in {
      val wk = legacyWettkampf("AdminTokenMalformed")
      HttpRequest(POST, adminTokenUri(wk.uuid.get),
        entity = HttpEntity(ContentTypes.`application/json`, "{kein json"))
        .addHeader(legacyJwtFor(wk.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.BadRequest)
      }
    }

    "report missing creator data with 409 when the body is absent" in {
      val wk = legacyWettkampf("AdminTokenNoBody")
      HttpRequest(POST, adminTokenUri(wk.uuid.get))
        .addHeader(legacyJwtFor(wk.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "report incomplete creator data with 409" in {
      val wk = legacyWettkampf("AdminTokenIncomplete")
      HttpRequest(POST, adminTokenUri(wk.uuid.get),
        entity = creatorEntity(creator.copy(creatorPhone = "   ")))
        .addHeader(legacyJwtFor(wk.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "issue an admin token and store the creator data" in {
      val wk = legacyWettkampf("AdminTokenHappy")
      HttpRequest(POST, adminTokenUri(wk.uuid.get), entity = creatorEntity())
        .addHeader(legacyJwtFor(wk.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)

        val fromHeader = header(jwtAuthorizationKey).map(_.value()).getOrElse(fail("kein Token-Header"))
        val fromBody = responseAs[String].parseJson.convertTo[AdminTokenResponse].token
        fromBody shouldBe fromHeader

        val claims = fromHeader match {
          case JsonWebToken(_, jwtClaims, _) => jwtClaims.asSimpleMap.toOption.getOrElse(Map.empty)
          case _ => Map.empty[String, String]
        }
        claims.get("admin").shouldBe(Some("true"))
        isExpiryInfinite(claims).shouldBe(true)
        claims.get("user").shouldBe(Some(wk.uuid.get))
      }

      val metaData = getWettkampfMetaData(UUID.fromString(wk.uuid.get))
      metaData.creatorName shouldBe Some("Hans Muster")
      metaData.creatorAddress shouldBe Some("Musterstrasse 1, 1234 Musterstadt")
      metaData.creatorPhone shouldBe Some("+49 123 456789")
      metaData.termsAccepted shouldBe true
      metaData.termsAcceptedAt should not be empty
      metaData.termsVersion shouldBe Some("1.0")
    }

    "accept a non-admin token with finite expiry (the real desktop case)" in {
      val wk = legacyWettkampf("AdminTokenFinite")
      HttpRequest(POST, adminTokenUri(wk.uuid.get), entity = creatorEntity())
        .addHeader(jwtFor(wk.uuid.get, 30L)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "accept an identical repeated request (lost response retry)" in {
      val wk = legacyWettkampf("AdminTokenReplay")
      val request = HttpRequest(POST, adminTokenUri(wk.uuid.get), entity = creatorEntity())
        .addHeader(legacyJwtFor(wk.uuid.get))

      request ~> withRoutes ~> check { status should ===(StatusCodes.OK) }
      request ~> withRoutes ~> check { status should ===(StatusCodes.OK) }
    }

    "answer 409 when different creator data is submitted for an onboarded competition" in {
      val wk = legacyWettkampf("AdminTokenDifferent")
      HttpRequest(POST, adminTokenUri(wk.uuid.get), entity = creatorEntity())
        .addHeader(legacyJwtFor(wk.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }

      HttpRequest(POST, adminTokenUri(wk.uuid.get),
        entity = creatorEntity(creator.copy(creatorName = "Jemand Anders")))
        .addHeader(legacyJwtFor(wk.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }

      getWettkampfMetaData(UUID.fromString(wk.uuid.get)).creatorName shouldBe Some("Hans Muster")
    }

    "answer 409 for an admin token on an already onboarded competition" in {
      val wk = legacyWettkampf("AdminTokenAlreadyOnboarded")
      saveWettkampfCreatorMetaData(UUID.fromString(wk.uuid.get), creator,
        new java.sql.Timestamp(System.currentTimeMillis()))

      HttpRequest(POST, adminTokenUri(wk.uuid.get))
        .addHeader(jwtFor(wk.uuid.get, 30L, isAdmin = true)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }

      HttpRequest(POST, adminTokenUri(wk.uuid.get),
        entity = creatorEntity(creator.copy(creatorPhone = "+41 999 000")))
        .addHeader(jwtFor(wk.uuid.get, 30L, isAdmin = true)) ~> withRoutes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "issue a token without a body for an onboarded competition" in {
      val wk = legacyWettkampf("AdminTokenOnboardedNoBody")
      saveWettkampfCreatorMetaData(UUID.fromString(wk.uuid.get), creator,
        new java.sql.Timestamp(System.currentTimeMillis()))

      HttpRequest(POST, adminTokenUri(wk.uuid.get))
        .addHeader(legacyJwtFor(wk.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        header(jwtAuthorizationKey) should not be empty
      }
    }

    "produce a token that satisfies an existing admin route" in {
      val wk = legacyWettkampf("AdminTokenUsable")
      var token = ""
      HttpRequest(POST, adminTokenUri(wk.uuid.get), entity = creatorEntity())
        .addHeader(legacyJwtFor(wk.uuid.get)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
        token = responseAs[String].parseJson.convertTo[AdminTokenResponse].token
      }

      HttpRequest(POST, s"/api/competition/${wk.uuid.get}/admin-access-link",
        entity = HttpEntity(ContentTypes.`application/json`,
          createAdminAccessLinkFormat.write(CreateAdminAccessLink(7L)).compactPrint))
        .addHeader(RawHeader(jwtAuthorizationKey, token)) ~> withRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "not match a non uuid path segment" in {
      HttpRequest(POST, "/api/competition/keine-uuid/admin-token", entity = creatorEntity())
        .addHeader(legacyJwtFor("keine-uuid")) ~> withRoutes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }
  }
}
