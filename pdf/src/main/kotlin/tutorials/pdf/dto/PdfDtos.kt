package tutorials.pdf.dto

import java.time.LocalDateTime

data class Report(
  val title: String,
  val createdBy: String,
  val createdAt: LocalDateTime,
  val sections: List<Section>,
) {
  var charts: List<Chart> = emptyList()
}

data class Section(
  val title: String,
  val content: String,
)

data class Chart(
  val type: ChartType,
  val title: String?,
  val data: ChartData,
)

data class ChartData(
  val labels: List<String>,
  val datasets: List<ChartDataset>,
)

data class ChartDataset(
  val label: String,
  val data: List<Int>,
)

enum class ChartType{
  PIE,
  BAR,
}
