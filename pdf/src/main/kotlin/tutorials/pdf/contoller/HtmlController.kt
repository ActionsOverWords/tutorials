package tutorials.pdf.contoller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import tutorials.pdf.config.HtmlTemplate
import tutorials.pdf.dto.Report
import tutorials.pdf.service.QuickChartService
import tutorials.pdf.service.ThymeleafTemplateService

@Controller
class HtmlController(
  private val thymeleafTemplateService: ThymeleafTemplateService,
  private val quickChartService: QuickChartService,
  private val sampleReport: Report,
) {

  @GetMapping("/openhtmltopdf")
  @ResponseBody
  fun openHtmlToPdf(): String {
    val chartImages = generateChartImages()
    return thymeleafTemplateService.renderHtml(HtmlTemplate.OpenHtmlToPdf, sampleReport, chartImages)
  }

  private fun generateChartImages(): Map<String, String>? {
    return if (sampleReport.charts.isEmpty()) {
      null
    } else {
      sampleReport.charts.associateBy({ "${it.title}" }) {
        quickChartService.generateChartImage(it)
      }
    }
  }

  @GetMapping("/gotenberg")
  @ResponseBody
  fun gotenberg(): String {
    return thymeleafTemplateService.renderHtml(HtmlTemplate.Gotenberg, sampleReport)
  }

}
