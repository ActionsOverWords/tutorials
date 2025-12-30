package tutorials.pdf.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tutorials.pdf.dto.*
import java.time.LocalDateTime

@Configuration
class SampleReportConfig {

  @Bean
  fun sampleReport(): Report {
    return Report(
      title = "2024년 물가 상승과 연봉 변화 분석 보고서",
      createdBy = "경제분석팀",
      createdAt = LocalDateTime.now(),
      sections = listOf(
        Section(
          title = "개요",
          content = """
            본 보고서는 2024년 물가 상승률과 연봉 변화 추이를 분석하여, 실질 구매력의 변화를 파악하고자 한다.

            최근 3년간 소비자물가지수(CPI)는 지속적으로 상승하였으며, 이는 가계의 실질 소득 감소로 이어지고 있다.
            특히 식료품과 에너지 가격의 급등은 서민층의 생활비 부담을 가중시키고 있다.

            반면, 명목 임금 상승률은 물가 상승률을 따라가지 못하고 있어, 실질 임금은 감소하는 추세이다.
            이러한 현상은 소비 위축과 경제 성장 둔화로 이어질 수 있어 주의 깊은 모니터링이 필요하다.
          """.trimIndent()
        ),
        Section(
          title = "상세 분석",
          content = """
            1. 물가 상승 현황

            2024년 1분기 소비자물가지수는 전년 동기 대비 3.8% 상승하였다.
            주요 품목별로는 식료품 4.2%, 주거비 3.5%, 교통비 5.1% 상승하였다.
            특히 외식비는 6.3% 상승하여 가장 높은 상승률을 기록하였다.

            에너지 가격은 국제 유가 변동에 따라 등락을 반복하였으나,
            전반적으로는 상승 압력이 지속되고 있다.

            2. 연봉 변화 추이

            2024년 기업들의 평균 임금 인상률은 2.9%로 집계되었다.
            이는 물가 상승률보다 낮은 수준으로, 실질 임금은 약 0.9% 감소한 것으로 분석된다.

            대기업과 중소기업 간 임금 격차는 더욱 확대되었으며,
            대기업은 평균 3.5% 인상한 반면, 중소기업은 2.1%에 그쳤다.

            직급별로는 임원급이 4.2% 상승한 반면, 일반 사원급은 2.5% 상승에 그쳐
            직급 간 임금 상승률 격차도 벌어지고 있다.

            3. 실질 구매력 분석

            명목 임금 상승률이 물가 상승률을 하회함에 따라,
            가계의 실질 구매력은 전년 대비 감소하였다.

            특히 저소득층의 경우 생활필수품 지출 비중이 높아
            물가 상승의 영향을 더 크게 받고 있다.

            중산층 가계의 저축률도 전년 대비 1.2%p 감소하여,
            미래를 위한 여유 자금 확보가 어려워지고 있는 것으로 나타났다.

            4. 향후 전망 및 제언

            2024년 하반기에도 물가 상승 압력은 지속될 것으로 예상된다.
            다만, 정부의 물가 안정 정책과 기준금리 인상 효과로
            상승률은 점차 둔화될 것으로 전망된다.

            기업들은 실질 임금 하락으로 인한 우수 인력 유출을 방지하기 위해
            물가 상승률을 반영한 적정 수준의 임금 인상이 필요하다.

            정부는 저소득층 지원 정책을 강화하고,
            생활물가 안정을 위한 다각적인 노력을 지속해야 할 것이다.
          """.trimIndent()
        )
      )
    ).apply {
      charts = listOf(
        Chart(
          type = ChartType.BAR,
          title = "연도별 물가 상승률 vs 임금 인상률",
          data = ChartData(
            labels = listOf("2022년", "2023년", "2024년"),
            datasets = listOf(
              ChartDataset(
                label = "물가 상승률(%)",
                data = listOf(5, 4, 4)
              ),
              ChartDataset(
                label = "임금 인상률(%)",
                data = listOf(4, 3, 3)
              )
            )
          )
        ),
        Chart(
          type = ChartType.PIE,
          title = "2024년 가계 지출 구성비",
          data = ChartData(
            labels = listOf("식료품", "주거비", "교통비", "의료비", "교육비", "기타"),
            datasets = listOf(
              ChartDataset(
                label = "지출 비중(%)",
                data = listOf(28, 22, 15, 12, 10, 13)
              )
            )
          )
        )
      )
    }
  }

}