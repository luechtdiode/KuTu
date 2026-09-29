package ch.seidel.kutu.domain

import ch.seidel.kutu.base.KuTuBaseSpec
import slick.jdbc.PostgresProfile.api.*

import java.sql.{Date, Timestamp}
import java.util.UUID
import scala.concurrent.Await

class WettkampfMetaDataSpec extends KuTuBaseSpec {

  private val creator = CreatorMetaData(
    creatorName = "  Hans Muster  ",
    creatorAddress = "Musterstrasse 1",
    creatorPhone = "+49 123 456789",
    termsVersion = "1.0"
  )

  private def newWettkampf(name: String): Wettkampf = createWettkampf(
    new Date(System.currentTimeMillis()), name, Set(20L), s"$name@test.ch", 3333, 7.5d,
    Some(UUID.randomUUID().toString), "", "", "", "Kategorie/AlterAufsteigend/Verein/Vorname/Name/Rotierend/AltInvers", "")

  "wettkampf metadata" should {

    "return None for an unknown uuid instead of throwing" in {
      getWettkampfMetaDataOption(UUID.randomUUID()) shouldBe None
    }

    "return None for a non uuid string based lookup" in {
      getWettkampfMetaDataOption(UUID.fromString("00000000-0000-0000-0000-000000000000")) shouldBe None
    }

    "update an existing metadata row" in {
      val wk = newWettkampf("MetaDataUpdate")
      val uuid = UUID.fromString(wk.uuid.get)
      getWettkampfMetaDataOption(uuid).isDefined shouldBe true

      val acceptedAt = new Timestamp(System.currentTimeMillis())
      val saved = saveWettkampfCreatorMetaData(uuid, creator, acceptedAt)

      saved.creatorName shouldBe Some("Hans Muster")
      saved.creatorAddress shouldBe Some("Musterstrasse 1")
      saved.creatorPhone shouldBe Some("+49 123 456789")
      saved.termsAccepted shouldBe true
      saved.termsAcceptedAt shouldBe Some(acceptedAt)
      saved.termsVersion shouldBe Some("1.0")

      getWettkampfMetaData(uuid).creatorName shouldBe Some("Hans Muster")
    }

    "trim the stored creator values" in {
      val wk = newWettkampf("MetaDataTrim")
      val uuid = UUID.fromString(wk.uuid.get)
      saveWettkampfCreatorMetaData(uuid, creator, new Timestamp(System.currentTimeMillis()))

      val stored = getWettkampfMetaData(uuid)
      stored.creatorName shouldBe Some("Hans Muster")
      stored.creatorName.exists(name => name == name.trim) shouldBe true
    }

    "overwrite previously stored creator values" in {
      val wk = newWettkampf("MetaDataOverwrite")
      val uuid = UUID.fromString(wk.uuid.get)
      saveWettkampfCreatorMetaData(uuid, creator, new Timestamp(System.currentTimeMillis()))
      saveWettkampfCreatorMetaData(uuid, creator.copy(creatorName = "Anna Anders"),
        new Timestamp(System.currentTimeMillis()))

      getWettkampfMetaData(uuid).creatorName shouldBe Some("Anna Anders")
    }

    "create a missing metadata row" in {
      val wk = newWettkampf("MetaDataInsert")
      val uuid = UUID.fromString(wk.uuid.get)

      Await.result(database.run(sqlu"delete from wettkampfmetadata where uuid=${uuid.toString}").map(_ => ()), scala.concurrent.duration.Duration.Inf)
      getWettkampfMetaDataOption(uuid) shouldBe None

      val saved = saveWettkampfCreatorMetaData(uuid, creator, new Timestamp(System.currentTimeMillis()))
      saved.creatorName shouldBe Some("Hans Muster")
      getWettkampfMetaDataOption(uuid).isDefined shouldBe true
    }

    "mark a freshly created wettkampf as legacy" in {
      val wk = newWettkampf("MetaDataLegacy")
      val uuid = UUID.fromString(wk.uuid.get)
      val metaData = getWettkampfMetaData(uuid)
      metaData.termsAcceptedAt shouldBe None
      metaData.termsVersion shouldBe None
    }

    "mark a wettkampf created with creator data as onboarded" in {
      val wk = createWettkampf(
        new Date(System.currentTimeMillis()), "MetaDataOnboarded", Set(20L), "onboarded@test.ch", 3333, 7.5d,
        Some(UUID.randomUUID().toString), "", "", "", "Kategorie/AlterAufsteigend/Verein/Vorname/Name/Rotierend/AltInvers", "",
        creatorName = Some("Web Ersteller"), creatorAddress = Some("Webstrasse 2"), creatorPhone = Some("+49 999"),
        termsAccepted = true, termsAcceptedAt = Some(new Timestamp(System.currentTimeMillis())), termsVersion = Some("1.0"))

      val metaData = getWettkampfMetaData(UUID.fromString(wk.uuid.get))
      metaData.creatorName shouldBe Some("Web Ersteller")
      metaData.termsAcceptedAt should not be empty
      metaData.termsVersion shouldBe Some("1.0")
    }
  }
}
