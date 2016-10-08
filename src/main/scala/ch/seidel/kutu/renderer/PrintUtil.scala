package ch.seidel.kutu.renderer

import scalafx.print.Printer
import scalafx.print.PrinterJob
import scalafx.scene.web.WebEngine
import scalafx.print.PageOrientation
import scalafx.print.Paper
import scalafx.print.Printer.MarginType

object PrintUtil {
  def printers = Printer.allPrinters.map(jfxprinter => new Printer(jfxprinter)) 
  
  def pdfPrinter: Option[Printer] = printers.iterator.find{ p => p.name.startsWith("PDF24 PDF") } match {
    case Some(p) => Some(p)
    case _ => printers.iterator.find{ p => p.name.endsWith("PDF") }
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
  
      engine.print(job);
    } 
    finally {
      job.endJob()
    }
  }
}