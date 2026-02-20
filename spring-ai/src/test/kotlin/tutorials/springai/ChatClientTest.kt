package tutorials.springai

import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.converter.ListOutputConverter
import tutorials.springai.base.extentions.logger
import tutorials.springai.config.IntegrationTest

@IntegrationTest
class ChatClientTest(
  val chatClientBuilder: ChatClient.Builder
) {

  val log by logger()

  @Test
  fun simpleChat() {
    val question = "2026년 공휴일이 몇일이고 날짜와 공휴일명 알려줘."
    val chatClient = chatClientBuilder
      .defaultAdvisors(SimpleLoggerAdvisor())
      .build()

    val response = chatClient.prompt()
      .system(
        """
        대한민국 양력 공휴일에 대해 아래와 같이 대답해주세요.
        예: 01/01 - 신정
      """.trimIndent()
      )
      .user(question)
      .call()
      .content()

    log.debug(response)
  }

  @Test
  fun listOutput() {
    val question = "2026년 공휴일이 몇일이고 날짜와 공휴일명 알려줘."
    val chatClient = chatClientBuilder.build()

    val responses = chatClient.prompt()
      .system(
        """
        대한민국 양력 공휴일에 대해 아래와 같이 대답해주세요.
        예:
        2026년 - 10일
        01/01 - 신정
      """.trimIndent()
      )
      .user(question)
      .call()
      .entity(ListOutputConverter())

    responses?.forEach { log.debug(it.toString()) }
  }

}