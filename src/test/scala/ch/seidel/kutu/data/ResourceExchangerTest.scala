package ch.seidel.kutu.data

import ch.seidel.kutu.domain.{Athlet, AthletView, Media, WarmUp, Wertung}
import org.scalatest.funsuite.AnyFunSuite

case class MediaWrapper(media: Option[Media])

case class TimestampWrapper(ts: Option[java.sql.Timestamp])

case class SimpleStringWrapper(value: Option[String])

class ResourceExchangerTest extends AnyFunSuite {

  test("testGetValues") {
    assert(ResourceExchanger.getValues(Athlet(33)) ===
      "\"0\",\"0\",\"M\",\"<Name>\",\"<Vorname>\",\"\",\"\",\"\",\"\",\"33\",\"true\"")
  }

  test("testGetHeader") {
    assert(ResourceExchanger.getHeader[AthletView] ===
      "\"id\",\"js_id\",\"geschlecht\",\"name\",\"vorname\",\"gebdat\",\"strasse\",\"plz\",\"ort\",\"verein\",\"activ\"")
  }

  test("testWertungHeaderContainsReserve") {
    assert(ResourceExchanger.getHeader[Wertung].contains("\"reserve\""))
  }

  test("testWertungValuesContainReserve") {
    val value = ResourceExchanger.getValues(Wertung(1, 2, 3, 4, "uuid", None, None, None, None, None, Some(1), None, None, reserve = Some(2)))
    assert(value.endsWith("\"2\""))
  }

  test("testGetValuesEncodesMediaAsBase64Json") {
    val value = ResourceExchanger.getValues(MediaWrapper(Some(Media("mid-1", "audiofile", "mp3"))))
    val decoded = new String(java.util.Base64.getUrlDecoder.decode(value.stripPrefix("\"").stripSuffix("\"")))
    assert(decoded.contains("\"id\":\"mid-1\"") && decoded.contains("\"extension\":\"mp3\""))
  }

  test("testGetValuesFormatsTimestamp") {
    val ts = new java.sql.Timestamp(0L)
    val value = ResourceExchanger.getValues(TimestampWrapper(Some(ts)))
    assert(value === "\"1970-01-01 00:00:00.000000\"")
  }

  test("testGetValuesRendersNoneAsEmptyCell") {
    assert(ResourceExchanger.getValues(MediaWrapper(None)) === "\"\"")
    assert(ResourceExchanger.getValues(TimestampWrapper(None)) === "\"\"")
    assert(ResourceExchanger.getValues(SimpleStringWrapper(None)) === "\"\"")
  }

  test("testGetValuesOfDurchgangContainsTimestampAndDuration") {
    val ts = new java.sql.Timestamp(0L)
    val dg = WarmUp
    val durchgang = ch.seidel.kutu.domain.Durchgang(1, 2, "T", "N", dg, 3, 0L, Some(ts), None, 60L, 6L, 300L)
    val value = ResourceExchanger.getValues(durchgang)
    assert(value.contains("1970-01-01 00:00:00.000000"))
  }

}
