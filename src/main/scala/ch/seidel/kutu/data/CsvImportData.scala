package ch.seidel.kutu.data

case class CsvImportData(rows: Seq[Map[String, String]], genderValueMapping: Map[String, String])


