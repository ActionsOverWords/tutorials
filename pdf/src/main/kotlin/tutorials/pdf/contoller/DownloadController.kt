package tutorials.pdf.contoller

import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import tutorials.pdf.dto.Report
import tutorials.pdf.service.GotenbergPdfGenerator
import tutorials.pdf.service.OpenHtmlToPdfGenerator
import java.nio.charset.StandardCharsets

@Controller
class DownloadController(
  private val openHtmlToPdfGenerator: OpenHtmlToPdfGenerator,
  private val gotenbergPdfGenerator: GotenbergPdfGenerator,
  private val sampleReport: Report,
) {

  @GetMapping("/openhtmltopdf/download")
  fun downloadOpenHtmlToPdf(): ResponseEntity<ByteArray> {
    val pdfBytes = openHtmlToPdfGenerator.generatePdf(sampleReport)
    return createPdfResponse(pdfBytes, "OpenHtmlToPdf")
  }

  @GetMapping("/gotenberg/download")
  fun downloadGotenberg(): ResponseEntity<ByteArray> {
    val pdfBytes = gotenbergPdfGenerator.generatePdf(sampleReport)
    return createPdfResponse(pdfBytes, "Gotenberg")
  }

  private fun createPdfResponse(pdfBytes: ByteArray, generatorType: String) =
    ResponseEntity.ok()
      .contentType(MediaType.APPLICATION_PDF)
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.attachment()
          .filename("${generatorType}.pdf", StandardCharsets.UTF_8)
          .build()
          .toString()
      )
      .body(pdfBytes)

}
