package tutorials.pdf.service

import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import tutorials.pdf.config.AbstractIntegrationTest
import tutorials.pdf.dto.Report
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.assertTrue

class GotenbergPdfGeneratorTest(
  private val pdfGenerator: GotenbergPdfGenerator,
) : AbstractIntegrationTest() {

  @ParameterizedTest
  @MethodSource("tutorials.pdf.dto.ReportFixture#reports")
  fun generatePdf(report: Report) {
    val pdfByteArray = pdfGenerator.generatePdf(report)

    val pdfPath = createPdf(pdfByteArray, "Gotenberg")

    assertAll(
      { assertTrue(pdfPath.exists()) },
      { assertTrue(Files.deleteIfExists(pdfPath)) },
    )
  }

}
