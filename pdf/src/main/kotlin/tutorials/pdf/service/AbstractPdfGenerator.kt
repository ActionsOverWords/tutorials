package tutorials.pdf.service

import tutorials.pdf.dto.Report

abstract class AbstractPdfGenerator : PdfGenerator {

  final override fun generatePdf(report: Report): ByteArray {
    val html = renderHtml(report)
    return convertToPdf(html)
  }

  protected abstract fun renderHtml(report: Report): String

  protected abstract fun convertToPdf(html: String): ByteArray

}