package tutorials.pdf.service

import org.springframework.stereotype.Component
import tutorials.pdf.base.extentions.logger
import tutorials.pdf.base.extentions.normalizeWhitespace
import tutorials.pdf.config.QuickChartClient
import tutorials.pdf.dto.Chart
import java.util.Base64

@Component
class QuickChartService(
  private val quickChartClient: QuickChartClient,
) {

  private val log by logger()

  fun generateChartImage(chart: Chart): String {
    val chartConfig = buildChartParameter(chart)
    log.debug("{}", chartConfig)

    val imageBytes = quickChartClient.generateChart(chartConfig)
    return Base64.getEncoder().encodeToString(imageBytes)
  }

  private fun buildChartParameter(
    chart: Chart,
  ): String {
    val datasets = chart.data.datasets.joinToString(",") { dataset ->
      """
        {
          label: '${dataset.label}',
          data: [${dataset.data.joinToString(",")}]
        }
      """.trimIndent()
    }

    return """
        {
          type: '${chart.type.name.lowercase()}',
          data: {
            labels: [${chart.data.labels.joinToString(",") { "'$it'" }}],
            datasets: [$datasets]
          },
          options: {
            plugins: {
              title: {
                text: '${chart.title ?: ""}',
                display: true
              }
            }
          }
        }
        """.trimIndent()
      .normalizeWhitespace()
  }

}
