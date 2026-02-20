package tutorials.springai

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.document.Document
import org.springframework.ai.model.transformer.KeywordMetadataEnricher
import org.springframework.ai.reader.JsonReader
import org.springframework.ai.reader.TextReader
import org.springframework.ai.reader.jsoup.JsoupDocumentReader
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig
import org.springframework.ai.reader.pdf.PagePdfDocumentReader
import org.springframework.ai.reader.tika.TikaDocumentReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import tutorials.springai.base.extentions.logger
import tutorials.springai.config.IntegrationTest
import java.util.stream.Stream

@IntegrationTest
class EtlTest(
  val chatModel: ChatModel,
  val vectorStore: VectorStore,
) {

  val log by logger()

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("etlFiles")
  fun etl(filename: String, useKeywordExtract: Boolean) {
    // Extract
    val chunkDocuments = extractFile(filename)
    log.debug("Extract Documents size: {}", chunkDocuments.size)

    // Transform
    val documents = transform(chunkDocuments, useKeywordExtract)
    log.debug("Transform Documents size: {}", chunkDocuments.size)

    // Load
    documents.forEach { log.debug("{}", it.text) }
    vectorStore.add(documents)
  }

  private fun extractFile(filename: String) : List<Document> {
    val ext = filename.substringAfterLast(".")
    val resource = ClassPathResource(filename)

    if ("txt".equals(ext, ignoreCase = true)) {
      val reader = TextReader(resource)
      return reader.read()
    } else if ("pdf".equals(ext, ignoreCase = true)) {
      val reader = PagePdfDocumentReader(resource)
      return reader.read()
    } else if ("docx".equals(ext, ignoreCase = true)) {
      val reader = TikaDocumentReader(resource)
      return reader.read()
    } else if ("json".equals(ext, ignoreCase = true)) {
      val reader = JsonReader(resource)
      return reader.read()
    } else if ("html".equals(ext, ignoreCase = true)) {
      return extractHtmlFile(resource)
    } else {
      throw IllegalArgumentException("unsupported file extension: $ext")
    }
  }

  private fun extractHtmlFile(resource: Resource): List<Document> {
    val reader = JsoupDocumentReader(
      resource,
      JsoupDocumentReaderConfig.builder()
        .charset(Charsets.UTF_8.name())
        .build()
    )

    return reader.read()
  }

  private fun transform(
    chunkDocuments: List<Document>,
    useKeywordExtract: Boolean,
  ): List<Document> {
    val splitter = TokenTextSplitter()
    var documents: List<Document> = splitter.apply(chunkDocuments)

    if (useKeywordExtract) {
      val keywordMetadataEnricher = KeywordMetadataEnricher(chatModel, 5)
      documents = keywordMetadataEnricher.apply(documents)
    }

    return documents
  }

  companion object {
    @JvmStatic
    fun etlFiles(): Stream<Arguments> {
      return listOf(
        Arguments.of("대한민국헌법(19880225).txt", true),
        Arguments.of("대한민국헌법(19880225).pdf", false),
        Arguments.of("대한민국헌법(19880225).docx", false),
        Arguments.of("대한민국헌법(19880225).html", false),
        Arguments.of("대한민국헌법(19880225).json", false),
        Arguments.of("lynn.pdf", false),
      ).stream()
    }
  }

}
