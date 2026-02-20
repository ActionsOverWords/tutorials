package tutorials.springai

import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import tutorials.springai.base.extentions.logger
import kotlin.getValue

class TokenTextSplitterTest {

  val log by logger()

  @Test
  fun splitter() {
    val document = Document("""
      The Extract, Transform, and Load (ETL) framework serves as the backbone of data processing within the Retrieval Augmented Generation (RAG) use case.
      The ETL pipeline orchestrates the flow from raw data sources to a structured vector store, ensuring data is in the optimal format for retrieval by the AI model.
      The RAG use case is text to augment the capabilities of generative models by retrieving relevant information from a body of data to enhance the quality and relevance of the generated output.
    """.trimIndent(), mapOf("meta" to "ETL"))

    val splitter = TokenTextSplitter.builder()
      .withChunkSize(50)
      .withMinChunkSizeChars(20)
      .withMinChunkLengthToEmbed(10)
      .withMaxNumChunks(5)
      .withKeepSeparator(true)
      .build()

    val splitDocuments = splitter.apply(listOf(document))

    splitDocuments.forEach { splitDocument ->
      log.info("chunk: {}", splitDocument.text)
      log.info("metadata: {}", splitDocument.metadata)
    }
  }

}