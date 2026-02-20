package tutorials.springai

import org.junit.jupiter.api.Test
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.ai.embedding.EmbeddingResponseMetadata
import tutorials.springai.base.extentions.logger
import tutorials.springai.config.IntegrationTest

@IntegrationTest
class EmbeddingModelTest(
  val embeddingModel: EmbeddingModel
) {

  val log by logger()

  @Test
  fun simple() {
    val question = "2026년 공휴일이 몇일이고 날짜와 공휴일명 알려줘."
    val response: EmbeddingResponse = embeddingModel.embedForResponse(listOf(question))

    val metadata: EmbeddingResponseMetadata = response.metadata
    log.debug("model: {}", metadata.model)
    log.debug("dimension: {}", embeddingModel.dimensions())

    val embedding = response.results.first()
    log.debug("차원: {}", embedding.output.size)
    log.debug("Vector: {}", embedding.output)
  }

  @Test
  fun embed() {
    val embed = embeddingModel.embed("king")
    log.debug("embed: {}", embed)
  }

}