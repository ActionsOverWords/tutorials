package tutorials.pdf.service

import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import tutorials.pdf.config.HtmlTemplate
import tutorials.pdf.dto.Report

@Component
class ThymeleafTemplateService(
  private val templateEngine: TemplateEngine,
) {

  fun renderHtml(
    htmlTemplate: HtmlTemplate,
    report: Report,
    chartImages: Map<String, String>? = null
  ): String {
    val context = Context()

    context.setVariable("report", report)
    chartImages?.let { context.setVariable("chartImages", it) }

    return templateEngine.process(htmlTemplate.templateName, context)
  }

}
