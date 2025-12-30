package tutorials.pdf.service

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tutorials.pdf.config.HtmlTemplate
import tutorials.pdf.dto.Report
import java.io.ByteArrayOutputStream

@Component
class OpenHtmlToPdfGenerator(
  val thymeleafTemplateService: ThymeleafTemplateService,
  val quickChartService: QuickChartService,
) : AbstractPdfGenerator() {

  override fun renderHtml(report: Report): String {
    val chartImages = generateChartImages(report)
    return thymeleafTemplateService.renderHtml(HtmlTemplate.OpenHtmlToPdf, report, chartImages)
  }

  private fun generateChartImages(report: Report): Map<String, String>? {
    return if (report.charts.isEmpty()) {
      return null
    } else {
      report.charts.associateBy({ "${it.title}" }) {
        quickChartService.generateChartImage(it)
      }
    }
  }

  override fun convertToPdf(html: String): ByteArray {
    ByteArrayOutputStream().use { outputStream ->
      val builder = PdfRendererBuilder()
      builder.useFastMode()
      builder.withHtmlContent(html, null)
      builder.toStream(outputStream)

      loadFonts(builder)

      builder.run()
      return outputStream.toByteArray()
    }
  }

  private fun loadFonts(builder: PdfRendererBuilder) {
    val regularFontResource = ClassPathResource("static/font/NanumGothic-Regular.ttf")
    val boldFontResource = ClassPathResource("static/font/NanumGothic-Bold.ttf")

    val regularFontBytes = regularFontResource.inputStream.use { it.readBytes() }
    val boldFontBytes = boldFontResource.inputStream.use { it.readBytes() }

    builder.useFont({ regularFontBytes.inputStream() }, "NanumGothic")
    builder.useFont({ boldFontBytes.inputStream() }, "NanumGothic-Bold")
  }
}
