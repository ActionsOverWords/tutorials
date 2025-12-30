package tutorials.pdf.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import tutorials.pdf.base.extentions.logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Base64
import java.util.UUID

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class AbstractIntegrationTest {

  protected val log by logger()

  protected fun createImageToBase64(base64Image: String): Path {
    val imageBytes = Base64.getDecoder().decode(base64Image)
    return createFile("chart-${UUID.randomUUID()}.png", imageBytes)
  }

  protected fun createPdf(pdfByteArray: ByteArray, filename: String): Path {
    return createFile("$filename-${UUID.randomUUID()}.pdf", pdfByteArray)
  }

  private fun createFile(filename: String, contentByteArray: ByteArray): Path {
    val outputDir = Paths.get("build/pdf")
    Files.createDirectories(outputDir)

    val outputFile = outputDir.resolve(filename)
    Files.write(outputFile, contentByteArray)

    val outputAbsolutePath = outputFile.toAbsolutePath()
    log.debug("create file to: {}", outputAbsolutePath)

    return outputAbsolutePath
  }

}
