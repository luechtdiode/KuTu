package ch.seidel.kutu.renderer

import java.awt.Desktop
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

import scala.language.implicitConversions

import ch.seidel.commons._
import ch.seidel.kutu.KuTuApp
import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.print.PageOrientation
import scalafx.print.Paper
import scalafx.print.Printer
import scalafx.print.PrinterJob
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.web.WebEngine
import scalafx.print.Printer.MarginType
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.util.Base64
import java.awt.image.RenderedImage
import scalafx.print.PrintResolution
import scalafx.print.PrintQuality
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicBoolean
import javax.print.attribute.ResolutionSyntax
import javax.print.attribute.standard.PrinterResolution
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import java.io.FileInputStream
import java.io.BufferedInputStream
import scala.io.Source
import org.slf4j.LoggerFactory


object PrintUtil {
  val logger = LoggerFactory.getLogger(this.getClass)
  
  private val PRINT_TO_BROWSER = new AtomicBoolean(false)
  
  case class FilenameDefault(filename: String, dir: java.io.File)

  def btnPrint(title: String, defaults: FilenameDefault, adjustLinesPerPage: Boolean = false, onGenerateOutput: (Int)=>String, engine: WebEngine = KuTuApp.invisibleWebView.engine, orientation: PageOrientation = PageOrientation.Portrait) = new Button {
    text = "Drucken ..."
    onAction = printDialog(title, defaults, adjustLinesPerPage, onGenerateOutput, engine, orientation)
  }
  
  def printDialog(title: String, defaults: FilenameDefault, adjustLinesPerPage: Boolean = false, onGenerateOutput: (Int)=>String, engine: WebEngine = KuTuApp.invisibleWebView.engine, orientation: PageOrientation = PageOrientation.Portrait)(action: ActionEvent) {

      val dir = defaults.dir
      if(!dir.exists()) {
        dir.mkdirs();
      }
      val selectedFile = new File(dir.getPath + "/" + defaults.filename)
      val txtLinesPerPage = new TextField {
        margin = Insets(10,10,10,0)
  		  text.value = "51"
  	  }
      val chkViaBrowser = new CheckBox("via Browser") {
        margin = Insets(10,10,10,0)
        selected = PRINT_TO_BROWSER.get
      }
      val cmbDrucker = new ComboBox[Printer] {
        margin = Insets(10,10,10,0)
        disable <== when(chkViaBrowser.selected) choose true otherwise false
        PrintUtil.printers.toList.sortBy(p => p.name).foreach {p => items.value.add(p) }
        selectionModel.value.select(PrintUtil.pdfPrinter.getOrElse(PrintUtil.printers.head))
      }
      implicit val impevent = action
  	  PageDisplayer.showInDialog(title, new DisplayablePage() {
  		  def getPage: Node = {
    		  new VBox {
    			  prefHeight = 50
    			  alignment = Pos.TopLeft
    			  
    			  hgrow = Priority.Always
    			  children = (if (adjustLinesPerPage) Seq(
    			      new Label("Zeilen pro Seite (51 für A4 hoch, 34 für A4 quer)"), 
    			      txtLinesPerPage) else Seq.empty) ++ Seq(
    			      chkViaBrowser,
    			      new Label("Drucker") {
    			        disable <== when(chkViaBrowser.selected) choose true otherwise false
    			      },
    			      cmbDrucker)
    		  }
    	  }
    	  }, new Button("OK") {
    	    disable <== when(Bindings.createBooleanBinding(() => {
                        !chkViaBrowser.selected.value && cmbDrucker.selectionModel.value.isEmpty()
                      },
                        chkViaBrowser.selected, cmbDrucker.selectionModel.value.selectedItemProperty
                      )) choose true otherwise false
    		  onAction = (event: ActionEvent) => {
            val file = if(!selectedFile.getName.endsWith(".html") && !selectedFile.getName.endsWith(".htm")) {
              new java.io.File(selectedFile.getAbsolutePath + ".html")
            }
            else {
              selectedFile
            }
            var lpp = 51
            try {
              lpp = Integer.valueOf(txtLinesPerPage.text.value)
              if(lpp < 1) lpp = 51
            }
            catch {
              case e: Exception =>
            }
            PRINT_TO_BROWSER.set(chkViaBrowser.selected.value)
            val toSave = onGenerateOutput(lpp)
            if(chkViaBrowser.selected.value) {
              val os = new BufferedOutputStream(new FileOutputStream(file))
              os.write(toSave.getBytes("UTF-8"))
              os.flush()
              os.close()
              Desktop.getDesktop().open(file)
            }
            else {
          	  engine.loadContent(toSave)
          	  KuTuApp.invokeWithBusyIndicator{
          	    printWebContent(engine, cmbDrucker.getSelectionModel.getSelectedItem, orientation)
          	    onGenerateOutput(0)
          	  }
            }
    		  }
    	  }
    	)    
  }
  def printers = Printer.allPrinters.map(jfxprinter => new Printer(jfxprinter)) 
  
  def pdfPrinter: Option[Printer] = printers.iterator.find{ p => p.name.toUpperCase().contains("PDF24 PDF") } match {
    case Some(p) => Some(p)
    case _ => printers.iterator.find{ p => p.name.toUpperCase().contains("PDF") && !p.name.toUpperCase().contains("FAX")} match {
      case Some(p) => Some(p)
      case _ => Some(Printer.defaultPrinter)
    }
  }
    
  def printWebContentToPdf(engine: WebEngine, orientation: PageOrientation) {
    pdfPrinter.foreach(p => printWebContent(engine, p, orientation))
  }
  
  
  def printWebContent(engine: WebEngine, printdevice: Printer, orientation: PageOrientation) {
    // clear margins
    
	  val inchToMM = 25.4d
    val defaultLeftMarginInch = 0.75d // 54.0 pts
    
    val defaultLayout = printdevice.createPageLayout(
        Paper.A4, 
        orientation, 
        MarginType.Default)
        
    val maxLayout = printdevice.createPageLayout(
        Paper.A4, 
        orientation, 
        MarginType.HardwareMinimum)    
        
    val pointsPerInch = defaultLayout.leftMargin / defaultLeftMarginInch        
    val pointsPerMM = pointsPerInch / inchToMM
    
    def mmToPoints(mm: Integer) = mm * pointsPerMM  
    
    val lb = math.max(mmToPoints(10), maxLayout.getLeftMargin)
    val rb = math.max(mmToPoints(5), maxLayout.getRightMargin)
    val tb = math.max(mmToPoints(5), maxLayout.getTopMargin)
    val bb = math.max(mmToPoints(5), maxLayout.getBottomMargin)
    val layout = printdevice.createPageLayout(
        Paper.A4, 
        orientation, 
        lb, rb, tb, bb)
    
    val job = PrinterJob.createPrinterJob(printdevice)
    try {
      job.getJobSettings().setPageLayout(layout)
      job.getJobSettings().setJobName("KuTuApp Printing Job")
      job.jobSettings.printQuality = PrintQuality.High
//      job.jobSettings.printResolution = new PrintResolution(600,600,ResolutionSyntax.DPI)
      engine.print(job);
    } 
    finally {
      job.endJob()
    }
  }
  
  def locateLogoFile(wettkampfDir: File) = {
    val prefferedLogoFileNames = (List("logo.svg", "logo.png", "logo.jpg", "logo.jpeg").map(name => new java.io.File(wettkampfDir.getPath + "/" + name)) ++ 
                                List("logo.svg", "logo.png", "logo.jpg", "logo.jpeg").map(name => new java.io.File(wettkampfDir.getParentFile + "/" + name)))
    prefferedLogoFileNames.find(_.exists).getOrElse(prefferedLogoFileNames.head);
  }
  
  implicit class ImageFile(file: File) {
    def imageSrcForWebEngine = {
      if(file.getName.endsWith("svg")) {
          val in = new FileInputStream(file)
          val imagedata = try {
            val buffer = Source.fromInputStream(in).mkString;
            "data:image/svg+xml;base64," + Base64.getMimeEncoder().encodeToString(buffer.getBytes())
          } finally {
            in.close
          }
          imagedata
      } else if(file.getName.endsWith("png")) {
        val imageBuffer = ImageIO.read(file)
        val output = new ByteArrayOutputStream()
        ImageIO.write(imageBuffer, "png", output)
        val imagedata = "data:image/png;base64," + Base64.getMimeEncoder().encodeToString(output.toByteArray())
        imagedata
      } else if(file.getName.endsWith("jpg")) {
        val imageBuffer = ImageIO.read(file)
        val output = new ByteArrayOutputStream()
        ImageIO.write(imageBuffer, "jpg", output)
        val imagedata = "data:image/jpg;base64," + Base64.getMimeEncoder().encodeToString(output.toByteArray())
        imagedata
      } else if(file.getName.endsWith("jpeg")) {
        val imageBuffer = ImageIO.read(file)
        val output = new ByteArrayOutputStream()
        ImageIO.write(imageBuffer, "jpeg", output)
        val imagedata = "data:image/jpeg;base64," + Base64.getMimeEncoder().encodeToString(output.toByteArray())
        imagedata        
      } else {
        file.toURI.toString
      }
    }
  }
  
}