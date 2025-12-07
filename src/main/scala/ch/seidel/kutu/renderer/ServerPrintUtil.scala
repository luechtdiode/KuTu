package ch.seidel.kutu.renderer

import net.glxn.qrgen.QRCode
import net.glxn.qrgen.image.ImageType

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.util.Base64
import javax.imageio.ImageIO
import scala.io.Source

case class FilenameDefault(filename: String, dir: java.io.File)

object ServerPrintUtil {

  import scala.language.implicitConversions

  inline def escaped(text: String): String = {
    if text == null then {
      ""
    }
    else {
      text.split("\n").toList.map(xml.Utility.escape).mkString("", "<br>", "")
    }
  }

  inline def locateLogoFile(wettkampfDir: File): File = {
    val prefferedLogoFileNames = List("logo.svg", "logo.png", "logo.jpg", "logo.jpeg").map(name => new java.io.File(s"${wettkampfDir.getPath}/$name")) ++
      List("logo.svg", "logo.png", "logo.jpg", "logo.jpeg").map(name => new java.io.File(s"${wettkampfDir.getParentFile}/$name"))
    prefferedLogoFileNames.find(_.exists).getOrElse(prefferedLogoFileNames.head)
  }

  inline def toQRCodeImage(uri: String): String = {
    val out = QRCode.from(uri).to(ImageType.PNG).withSize(200, 200).stream()
    val imagedata = "data:image/png;base64," + Base64.getMimeEncoder().encodeToString(out.toByteArray)
    imagedata
  }

  implicit class ImageFile(file: File) {
    def imageSrcForWebEngine: String = {
      if file.getName.endsWith("svg") then {
        val in = new FileInputStream(file)
        val imagedata = try {
          val buffer = Source.fromInputStream(in).mkString
          "data:image/svg+xml;base64," + Base64.getEncoder.encodeToString(buffer.getBytes())
        } finally {
          in.close()
        }
        imagedata
      } else if file.getName.endsWith("png") then {
        val imageBuffer = ImageIO.read(file)
        val output = new ByteArrayOutputStream()
        ImageIO.write(imageBuffer, "png", output)
        val imagedata = "data:image/png;base64," + Base64.getEncoder.encodeToString(output.toByteArray)
        imagedata
      } else if file.getName.endsWith("jpg") then {
        val imageBuffer = ImageIO.read(file)
        val output = new ByteArrayOutputStream()
        ImageIO.write(imageBuffer, "jpg", output)
        val imagedata = "data:image/jpg;base64," + Base64.getEncoder.encodeToString(output.toByteArray)
        imagedata
      } else if file.getName.endsWith("jpeg") then {
        val imageBuffer = ImageIO.read(file)
        val output = new ByteArrayOutputStream()
        ImageIO.write(imageBuffer, "jpeg", output)
        val imagedata = "data:image/jpeg;base64," + Base64.getEncoder.encodeToString(output.toByteArray)
        imagedata
      } else {
        file.toURI.toASCIIString
      }
    }
  }

}
