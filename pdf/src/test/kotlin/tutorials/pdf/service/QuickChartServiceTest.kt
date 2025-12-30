package tutorials.pdf.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import tutorials.pdf.config.AbstractIntegrationTest
import tutorials.pdf.dto.ReportFixture
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.assertTrue

class QuickChartServiceTest(
  private val quickChartService: QuickChartService,
) : AbstractIntegrationTest() {

  @Test
  fun generateChartImage() {
    val chart = ReportFixture.create().charts.first()
    val base64Image = quickChartService.generateChartImage(chart)

    val imagePath = createImageToBase64(base64Image)

    assertAll(
      { assertTrue(imagePath.exists()) },
      { assertTrue(Files.deleteIfExists(imagePath)) },
    )
  }

}
