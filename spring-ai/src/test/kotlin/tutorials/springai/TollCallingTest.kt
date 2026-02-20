package tutorials.springai

import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import tutorials.springai.base.extentions.logger
import tutorials.springai.config.IntegrationTest
import tutorials.springai.tools.DateTimeTool

@IntegrationTest
class TollCallingTest(
  val chatClientBuilder: ChatClient.Builder,
  val dateTimeTool: DateTimeTool,
) {

  val log by logger()

  @Test
  fun currentDateTime() {
    val chatClient = chatClientBuilder
      .defaultAdvisors(SimpleLoggerAdvisor())
      .build()

    val response = chatClient.prompt()
      .user("현재 날짜가 어떻게 돼?")
      .tools(dateTimeTool)
      .call()
      .content()

    log.debug(response)
  }

  @Test
  fun setAlarm() {
    val chatClient = chatClientBuilder
      .defaultAdvisors(SimpleLoggerAdvisor())
      .build()

    val response = chatClient.prompt()
      .user("10분 후 알람해줘.")
      .tools(dateTimeTool)
      .toolContext(mapOf("id" to "tutorials"))
      .call()
      .content()

    log.debug(response)
  }
}