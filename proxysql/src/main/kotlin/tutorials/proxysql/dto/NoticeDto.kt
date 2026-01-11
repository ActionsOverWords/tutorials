package tutorials.proxysql.dto

import tutorials.proxysql.domain.Notice
import java.time.Instant
import java.util.UUID

data class NoticeRequest(
  val title: String,
  val content: String,
  val enabled: Boolean = true,
  val publishFrom: Instant? = null,
  val publishTo: Instant? = null
)

data class NoticeResponse(
  val id: UUID,
  val title: String,
  val content: String,
  val enabled: Boolean,
  val publishFrom: Instant?,
  val publishTo: Instant?,
  val createdAt: Instant
) {
  companion object {
    fun from(notice: Notice) = NoticeResponse(
      id = notice.id!!,
      title = notice.title,
      content = notice.content,
      enabled = notice.enabled,
      publishFrom = notice.publishFrom,
      publishTo = notice.publishTo,
      createdAt = notice.createdAt
    )
  }
}
