package ch.seidel.kutu.domain

import scala.io.Source
import scala.util.Using

/**
 * Nutzungsbedingungen, einzige Quelle der Wahrheit für Text und Version.
 *
 * Der Text liegt in `terms/nutzungsbedingungen.md` auf dem Classpath und wird hier einmalig
 * gelesen und interpretiert. Sowohl der Desktop-Dialog als auch der Web-Client rendern genau
 * dieses Dokument; die Web-Oberfläche holt sich Version und Blöcke über `GET /api/terms`.
 *
 * Format (eine bewusst kleine Markdown-Teilmenge, damit es nur einen Parser gibt):
 *   - `key: value` vor der ersten Überschrift sind Kopffelder (`version` ist Pflicht)
 *   - `# ...` ist der Titel, `## ...` eine Abschnittsüberschrift
 *   - Folgen nicht-leerer Zeilen bilden einen Absatz, Leerzeilen trennen Blöcke
 *   - `<!-- ... -->` sind Kommentare und werden nie gerendert
 */
object Terms {

  /** Art eines Blockes, steuert die Darstellung in Desktop-Dialog und Web-Formular. */
  enum BlockKind:
    case Title, Heading, Paragraph

  case class Block(kind: BlockKind, text: String)

  case class Document(version: String, stand: String, title: String, blocks: Seq[Block]):
    require(version.trim.nonEmpty, "Nutzungsbedingungen ohne Version")
    require(title.trim.nonEmpty, "Nutzungsbedingungen ohne Titel")
    require(blocks.nonEmpty, "Nutzungsbedingungen ohne Inhalt")

    /** Einfacher Text für die JavaFX-TextArea: Überschriften ohne Markierung, Stand am Ende. */
    lazy val text: String = {
      val body = blocks.map(_.text).mkString("\n\n")
      s"$body\n\nStand: $stand"
    }

  private val resourceName = "terms/nutzungsbedingungen.md"

  lazy val document: Document = {
    
    val stream = Option(getClass.getResourceAsStream(s"/$resourceName"))
      .getOrElse(throw new IllegalStateException(s"Ressource $resourceName nicht im Classpath"))
    parse(Using.resource(Source.fromInputStream(stream, "UTF-8"))(_.mkString))
  }

  def version: String = document.version

  def stand: String = document.stand

  def title: String = document.title

  def blocks: Seq[Block] = document.blocks

  /** Klartextdarstellung, siehe [[Document.text]]. */
  def text: String = document.text

  private[kutu] def parse(content: String): Document = {
    val lines = stripComments(
      content
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split("\n")
        .iterator
        .map(_.trim)
        .toSeq
    )

    val (headerLines, bodyLines) = lines.span(line => !line.startsWith("#"))

    val header = headerLines.flatMap { line =>
      line.split(":", 2) match {
        case Array(key, value) if key.trim.nonEmpty => Some(key.trim -> value.trim)
        case _ => None
      }
    }.toMap

    val blocks = parseBlocks(bodyLines)
    Document(
      version = header.getOrElse("version", ""),
      stand = header.getOrElse("stand", ""),
      title = blocks.collectFirst { case Block(BlockKind.Title, text) => text }.getOrElse(""),
      blocks = blocks
    )
  }

  /** Verwirft `<!-- ... -->`-Kommentare, auch über mehrere Zeilen. */
  private def stripComments(lines: Seq[String]): Seq[String] = {
    val kept = Seq.newBuilder[String]
    var insideComment = false
    lines.foreach { line =>
      if insideComment then {
        if line.contains("-->") then insideComment = false
      } else if line.startsWith("<!--") then {
        if !line.contains("-->") then insideComment = true
      } else kept += line
    }
    kept.result()
  }

  private def parseBlocks(lines: Seq[String]): Seq[Block] = {
    val collected = Seq.newBuilder[Block]
    val currentText = Seq.newBuilder[String]
    var currentKind: Option[BlockKind] = None

    def flush(): Unit = {
      currentKind.foreach { kind =>
        val text = currentText.result().mkString(" ").trim
        if text.nonEmpty then collected += Block(kind, text)
      }
      currentText.clear()
    }

    def start(kind: BlockKind, line: String): Unit = {
      flush()
      currentKind = Some(kind)
      currentText += line
    }

    lines.foreach { line =>
      if line.startsWith("## ") then start(BlockKind.Heading, line.drop(3).trim)
      else if line.startsWith("# ") then start(BlockKind.Title, line.drop(2).trim)
      else if line.isEmpty then flush()
      // Text nach einer Leerzeile oder direkt unter einer Überschrift ist ein eigener Absatz.
      else if currentKind.contains(BlockKind.Paragraph) then currentText += line
      else start(BlockKind.Paragraph, line)
    }
    flush()
    collected.result()
  }
}
