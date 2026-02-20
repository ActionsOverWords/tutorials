package tutorials.springai

import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.Ordered
import tutorials.springai.base.extentions.logger
import tutorials.springai.config.IntegrationTest

@IntegrationTest
class QuestionAnswerAdvisorTest(
  val chatClientBuilder: ChatClient.Builder,
  val vectorStore: VectorStore,
) {

  val log by logger()

  /**
   * 테스트 실행 전 [EtlTest.etl]를 실행하여 청약 데이터가 VectorDB에 존재해야 한다.
   */
  @Test
  fun search() {
    val search = SearchRequest.builder()
      .similarityThreshold(0.5)
      .topK(5)
      .filterExpression("file_name == 'lynn.pdf'")
      .build()

    val questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
      .searchRequest(search)
      .build()

    val chatClient = chatClientBuilder
      .defaultAdvisors(
        questionAnswerAdvisor,
        SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
      )
      .build()

    val content = chatClient.prompt()
      .user("청약 통장 15년 이상, 무주택 10년 이상, 부부만 사는 경우의 가점은 몇점이야? 항목별로 알려주고, 최종 합산 점수도 알려줘.")
      .call()
      .content()

    log.debug(content)
  }

}
