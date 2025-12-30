package tutorials.pdf.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream
import kotlin.random.Random

object ReportFixture {

  @JvmStatic
  fun reports(): Stream<Report> = Stream.of(
    create(),
    createSimple()
  )

  fun create(): Report {
    return Report(
      title = "레포트",
      createdBy = "system",
      createdAt = LocalDateTime.now(),
      sections = listOf(
        createSection(0),
        createSection(1),
      ),
    ).apply {
      charts = listOf(
        Chart(ChartType.BAR, "막대 그래프", createChartData(2)),
        Chart(ChartType.PIE, "파이 그래프", createChartData(3))
      )
    }
  }

  private fun createSection(section: Int) =
   Section(
     title = "Test Section - $section",
     content = (1..Random.nextInt(10)).joinToString("\n") { "Test content" }
   )

  private fun createChartData(size: Int): ChartData {
    val year = LocalDate.now().year

    return ChartData(
      labels = (1..size).map { "${it}월" },
      datasets = (1 .. size).map {i ->
          ChartDataset(
            label = "${year - i}",
            data =  (1..size).map { Random.nextInt(0, 81) }
          )
        }
    )
  }

  fun createSimple(): Report {
    return Report(
      title = "레포트",
      createdBy = "system",
      createdAt = LocalDateTime.now(),
      sections = listOf(
        createSection(0),
        createSection(1),
      ),
    )
  }

}
