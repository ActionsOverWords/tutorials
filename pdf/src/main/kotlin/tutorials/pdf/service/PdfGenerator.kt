package tutorials.pdf.service

import tutorials.pdf.dto.Report

interface PdfGenerator {

  fun generatePdf(report: Report): ByteArray

}
