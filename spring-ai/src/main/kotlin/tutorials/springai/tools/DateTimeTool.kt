package tutorials.springai.tools

import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component
import tutorials.springai.base.extentions.logger
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class DateTimeTool {

  val log by logger()

  @Tool(description = "현재 날짜와 시간 정보를 제공합니다.")
  fun getCurrentDateTime(): String {
    val now = LocalDateTime.now()
      .atZone(ZoneId.of("Asia/Seoul"))
      .toString()

    log.info("CurrentDateTime: {}", now)

    return now
  }

  @Tool(description = "ISO-8601 형식으로 제공되는 주어진 시간 동안 사용자 알람 설정합니다.")
  fun setAlarm(time: String, toolContext: ToolContext) {
    val alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME)

    log.info("Alarm set for {}, toolContext id: {}", alarmTime, toolContext.context["id"])

    toolContext.toolCallHistory.forEach {
      log.info("toolContext history: {}", it)
    }
  }

}