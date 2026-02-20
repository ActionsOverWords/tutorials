package tutorials.springai

import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import tutorials.springai.base.extentions.logger
import tutorials.springai.config.IntegrationTest

@IntegrationTest
class VectorStoreTest(
  val vectorStore: VectorStore
) {

  val log by logger()

  @Test
  fun add() {
    val documents = listOf(
      Document("대통령 선거는 5년마다 있음", mapOf("meta" to "헌법")),
      Document("국회의원은 4년마다 투표로 뽑음", mapOf("meta" to "헌법")),
    )

    vectorStore.add(documents)
  }

  @Test
  fun similaritySearch() {
    val search = SearchRequest.builder()
      .query("대통령의 임기는?")
      .topK(1)
      .similarityThreshold(0.5)
      .build()

    val result = vectorStore.similaritySearch(search)
    result.forEach { log.info(it.toString()) }
  }

  @Test
  fun delete() {
    vectorStore.delete("meta == '헌법'")
  }
}