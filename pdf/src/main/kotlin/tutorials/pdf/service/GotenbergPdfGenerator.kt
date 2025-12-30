package tutorials.pdf.service

import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Component
import tutorials.pdf.config.GotenbergClient
import tutorials.pdf.config.HtmlTemplate
import tutorials.pdf.dto.Report

@Component
class GotenbergPdfGenerator(
  val thymeleafTemplateService: ThymeleafTemplateService,
  private val gotenbergClient: GotenbergClient,
) : AbstractPdfGenerator() {

  override fun renderHtml(report: Report): String {
    return thymeleafTemplateService.renderHtml(HtmlTemplate.Gotenberg, report)
  }

  override fun convertToPdf(html: String): ByteArray {
    val htmlResource = object : ByteArrayResource(html.toByteArray(Charsets.UTF_8)) {
      override fun getFilename(): String = "index.html"
    }
    return gotenbergClient.generatePdf(htmlResource)
  }

}
