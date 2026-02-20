package tutorials.springai

import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.rag.Query
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer
import tutorials.springai.base.extentions.logger
import tutorials.springai.config.IntegrationTest

@IntegrationTest
class TransformerTest(
  val chatClientBuilder: ChatClient.Builder,
) {

  val log by logger()

  @Test
  fun compression() {
    val query = Query.builder()
      .text("미국은?")
      .history(
        UserMessage("한국의 수도는 어디입니까?"),
        AssistantMessage("서울 입니다."),
      )
      .build()

    val transformer = CompressionQueryTransformer.builder()
      .chatClientBuilder(chatClientBuilder.defaultAdvisors(SimpleLoggerAdvisor()))
      .build()

    val transformedQuery = transformer.transform(query)
    log.debug(transformedQuery.text)
  }

  @Test
  fun rewriteQuery() {
    val query = Query.builder()
      .text("""
        2025년도에는 개엄 등 기억에 남는 한해였어.
        2026년도의 공휴일은 몇일이야?
      """.trimIndent())
      .build()

    val transformer = RewriteQueryTransformer.builder()
      .chatClientBuilder(chatClientBuilder.defaultAdvisors(SimpleLoggerAdvisor()))
      .build()

    val transformedQuery = transformer.transform(query)
    log.debug(transformedQuery.text)
  }

  @Test
  fun translation() {
    val query = Query.builder()
      .text("안녕")
      .build()

    val transformer = TranslationQueryTransformer.builder()
      .chatClientBuilder(chatClientBuilder.defaultAdvisors(SimpleLoggerAdvisor()))
      .targetLanguage("english")
      .build()

    val transformedQuery = transformer.transform(query)
    log.debug(transformedQuery.text)
  }

  @Test
  fun multiQuery() {
    val query = Query.builder()
      .text("직장인 연봉 1억원의 평균 실수령액은?")
      .build()

    val transformer = MultiQueryExpander.builder()
      .chatClientBuilder(chatClientBuilder.defaultAdvisors(SimpleLoggerAdvisor()))
      .numberOfQueries(3)
      .build()

    val queries = transformer.expand(query)
    queries.forEach { log.debug(it.text) }
  }

}