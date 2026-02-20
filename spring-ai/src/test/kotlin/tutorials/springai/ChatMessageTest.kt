package tutorials.springai

import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.core.Ordered
import tutorials.springai.base.extentions.logger
import tutorials.springai.config.IntegrationTest

@IntegrationTest
class ChatMessageTest(
  val chatClientBuilder: ChatClient.Builder,
  val chatMemory: ChatMemory
) {

  val log by logger()

  @Test
  fun `via MessageChatMemoryAdvisor`() {
    val question = "대한민국의 2026년 공휴일이 몇일이고 날짜와 공휴일명 알려줘."

    val chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build()
    val chatClient = chatClientBuilder
      .defaultAdvisors(
        chatMemoryAdvisor,
        SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
      )
      .build()

    val response = chatClient.prompt()
      .user(question)
      .advisors { a -> a.param(ChatMemory.CONVERSATION_ID, "tutorials") }
      .call()
      .content()

    log.debug(response)
  }

  @Test
  fun `via PromptChatMemoryAdvisor`() {
    val question = "대한민국의 2026년 공휴일이 몇일이고 날짜와 공휴일명 알려줘."

    val chatMemoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory).build()
    val chatClient = chatClientBuilder
      .defaultAdvisors(
        chatMemoryAdvisor,
        SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
      )
      .build()

    val response = chatClient.prompt()
      .user(question)
      .advisors { a -> a.param(ChatMemory.CONVERSATION_ID, "tutorials") }
      .call()
      .content()

    log.debug(response)
  }

}