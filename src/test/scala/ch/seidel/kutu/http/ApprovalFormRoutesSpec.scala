package ch.seidel.kutu.http

import ch.seidel.kutu.actors.{CompetitionCreated, CompetitionRegistrationClientActor}
import ch.seidel.kutu.base.KuTuBaseSpec
import ch.seidel.kutu.domain.*
import ch.seidel.kutu.mail.{Mailbox, MockedSMTPMailer}
import org.apache.pekko.http.scaladsl.model.HttpMethods.{GET, POST}
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import org.scalatest.concurrent.Eventually
import spray.json.enrichString

import org.scalatest.time.{Millis, Seconds, Span}

import java.sql.Date
import java.util.UUID

class ApprovalFormRoutesSpec extends KuTuBaseSpec with Eventually {

  private val mailer = new MockedSMTPMailer()

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(200, Millis))

  private val creator = CreatorMetaData("Hans Muster", "Musterstrasse 1, 1234 Musterstadt", "+49 123 456789", "1.0")

  private def newWettkampf(name: String, mail: String): Wettkampf = createWettkampf(
    new Date(System.currentTimeMillis()), name, Set(20L), mail, 3333, 7.5d,
    Some(UUID.randomUUID().toString), "", "", "", "Kategorie/AlterAufsteigend/Verein/Vorname/Name/Rotierend/AltInvers", "")

  private def routes = allroutes(x => vereinSecretHashLookup(x), id => extractRegistrationId(id))

  private def approveEntity(mail: String, metaData: CreatorMetaData): HttpEntity.Strict =
    HttpEntity(ContentTypes.`application/json`, approveEMailRequestFormat.write(ApproveEMailRequest(mail, metaData)).compactPrint)

  private def mailbox(mail: String): Mailbox = Mailbox.get(mail)

  /** Nachrichten der Formular-Mail bekommen ein eigenes Postfach, sonst kollidieren die Adressen. */
  private def freshMailboxes(mails: String*): Unit = {
    Mailbox.clearAll()
    mails.foreach(m => Mailbox.get(m).clear())
  }

  "the approval form endpoint" should {

    "reject incomplete creator data with 400" in {
      val mail = "incomplete@test.ch"
      val wk = newWettkampf("ApprovalIncomplete", mail)
      freshMailboxes(mail)

      HttpRequest(POST, s"/api/registrations/${wk.uuid.get}/approvemail",
        entity = approveEntity(mail, creator.copy(creatorPhone = "  "))) ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
      }

      getWettkampfMetaDataOption(UUID.fromString(wk.uuid.get)).get.termsAcceptedAt shouldBe None
    }

    "reject a missing terms version with 400" in {
      val mail = "noterms@test.ch"
      val wk = newWettkampf("ApprovalNoTerms", mail)
      freshMailboxes(mail)

      HttpRequest(POST, s"/api/registrations/${wk.uuid.get}/approvemail",
        entity = approveEntity(mail, creator.copy(termsVersion = "   "))) ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
      }

      getWettkampfMetaDataOption(UUID.fromString(wk.uuid.get)).get.termsAcceptedAt shouldBe None
    }

    "not store creator data for a wrong mail address" in {
      val mail = "owner@test.ch"
      val wk = newWettkampf("ApprovalWrongMail", mail)
      freshMailboxes(mail)

      HttpRequest(POST, s"/api/registrations/${wk.uuid.get}/approvemail",
        entity = approveEntity("falsch@test.ch", creator)) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String].parseJson.convertTo[ApproveEMailResponse].success shouldBe false
      }

      getWettkampfMetaDataOption(UUID.fromString(wk.uuid.get)).get.termsAcceptedAt shouldBe None
      getWettkampfMetaDataOption(UUID.fromString(wk.uuid.get)).get.creatorName shouldBe None
    }

    "store creator data and approve for the matching mail address" in {
      val mail = "happy@test.ch"
      val wk = newWettkampf("ApprovalHappy", mail)
      freshMailboxes(mail)

      HttpRequest(POST, s"/api/registrations/${wk.uuid.get}/approvemail",
        entity = approveEntity(mail, creator)) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val response = entityAs[String].parseJson.convertTo[ApproveEMailResponse]
        response.success shouldBe true
      }

      val metaData = getWettkampfMetaData(UUID.fromString(wk.uuid.get))
      metaData.creatorName shouldBe Some("Hans Muster")
      metaData.creatorAddress shouldBe Some("Musterstrasse 1, 1234 Musterstadt")
      metaData.creatorPhone shouldBe Some("+49 123 456789")
      metaData.termsAccepted shouldBe true
      metaData.termsAcceptedAt should not be empty
      metaData.termsVersion shouldBe Some("1.0")

      eventually {
        mailbox(mail).size() should be >= 1
      }
    }

    "keep stored creator data on a repeated submission" in {
      val mail = "repeat@test.ch"
      val wk = newWettkampf("ApprovalRepeat", mail)
      freshMailboxes(mail)

      HttpRequest(POST, s"/api/registrations/${wk.uuid.get}/approvemail",
        entity = approveEntity(mail, creator)) ~> routes ~> check {
        status should ===(StatusCodes.OK)
      }

      HttpRequest(POST, s"/api/registrations/${wk.uuid.get}/approvemail",
        entity = approveEntity(mail, creator.copy(creatorName = "Jemand Anders"))) ~> routes ~> check {
        status should ===(StatusCodes.OK)
      }

      getWettkampfMetaData(UUID.fromString(wk.uuid.get)).creatorName shouldBe Some("Hans Muster")
    }

    "still allow the legacy GET link without creator data" in {
      val mail = "legacy-get@test.ch"
      val wk = newWettkampf("ApprovalLegacyGet", mail)
      freshMailboxes(mail)

      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/approvemail?mail=$mail") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should include(wk.easyprint)
      }

      getWettkampfMetaDataOption(UUID.fromString(wk.uuid.get)).get.termsAcceptedAt shouldBe None
    }

    "return the fallback message for a GET without mail parameter" in {
      val wk = newWettkampf("ApprovalGetNoMail", "get-nomail@test.ch")
      HttpRequest(GET, s"/api/registrations/${wk.uuid.get}/approvemail") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should include("unable to approve without mail")
      }
    }
  }

  "the approval mail link" should {

    "point to the web form for an uploaded competition" in {
      val mail = "upload-form@test.ch"
      val wk = newWettkampf("LinkUpload", mail)
      freshMailboxes(mail)

      CompetitionRegistrationClientActor.publish(
        CompetitionCreated(wk.uuid.get, "https://kutu.ch/wettkampf-bestaetigen?mail=x", withForm = true), "")

      eventually {
        val messages = mailbox(mail)
        messages.size() should be >= 1
        val text = mailer.getTextFromMessage(messages.get(messages.size() - 1))
        withClue(text) {
          text should include("wettkampf-bestaetigen")
          text should not include "api/registrations"
        }
      }
    }

    "point to the direct api link for a web created competition" in {
      val mail = "web-direct@test.ch"
      val wk = newWettkampf("LinkWeb", mail)
      freshMailboxes(mail)

      CompetitionRegistrationClientActor.publish(
        CompetitionCreated(wk.uuid.get, s"https://kutu.ch/api/registrations/${wk.uuid.get}/approvemail?mail=$mail", withForm = false), "")

      eventually {
        val messages = mailbox(mail)
        messages.size() should be >= 1
        val text = mailer.getTextFromMessage(messages.get(messages.size() - 1))
        withClue(text) {
          text should include("api/registrations")
          text should include("24h")
        }
      }
    }
  }
}
