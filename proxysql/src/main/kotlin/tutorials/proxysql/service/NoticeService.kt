package tutorials.proxysql.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tutorials.proxysql.domain.Notice
import tutorials.proxysql.dto.NoticeRequest
import tutorials.proxysql.dto.NoticeResponse
import tutorials.proxysql.repository.NoticeRepository
import java.util.UUID

/**
 * 공지사항 서비스
 *
 * ProxySQL read/write 분기:
 * - 조회 메서드 (@Transactional(readOnly = true)) → slave 서버
 * - 변경 메서드 (@Transactional) → master 서버
 */
@Service
@Transactional(readOnly = true)
class NoticeService(
  private val noticeRepository: NoticeRepository
) {

  @Transactional
  fun save(request: NoticeRequest): NoticeResponse {
    val notice = Notice(
      title = request.title,
      content = request.content,
      enabled = request.enabled,
      publishFrom = request.publishFrom,
      publishTo = request.publishTo
    )
    return NoticeResponse.from(noticeRepository.save(notice))
  }

  @Transactional
  fun update(id: UUID, request: NoticeRequest): NoticeResponse {
    val notice = noticeRepository.findById(id)
      .orElseThrow { NoSuchElementException("Notice not found: $id") }

    notice.title = request.title
    notice.content = request.content
    notice.enabled = request.enabled
    notice.publishFrom = request.publishFrom
    notice.publishTo = request.publishTo

    return NoticeResponse.from(noticeRepository.save(notice))
  }

  fun findAll(): List<NoticeResponse> {
    return noticeRepository.findAll()
      .map { NoticeResponse.from(it) }
  }

  fun findActiveNotices(): List<NoticeResponse> {
    return noticeRepository.findByEnabledTrueOrderByCreatedAtDesc()
      .map { NoticeResponse.from(it) }
  }
}
