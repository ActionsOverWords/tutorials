package tutorials.proxysql.service

import org.junit.jupiter.api.Test
import tutorials.proxysql.config.AbstractTests.AbstractIntegrationTest
import tutorials.proxysql.dto.NoticeRequest
import tutorials.proxysql.repository.NoticeRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NoticeServiceTest(
  private val noticeService: NoticeService,
  private val noticeRepository: NoticeRepository,
) : AbstractIntegrationTest() {

  @Test
  fun `공지사항 저장 테스트`() {
    // given
    val request = NoticeRequest(
      title = "테스트 공지",
      content = "테스트 내용"
    )

    // when
    val response = noticeService.save(request)

    // then
    assertNotNull(response.id)
    assertEquals(request.title, response.title)
    assertEquals(request.content, response.content)
  }

  @Test
  fun `공지사항 수정 테스트`() {
    // given
    val saved = noticeService.save(
      NoticeRequest(title = "원본", content = "원본 내용")
    )

    // when
    val updated = noticeService.update(
      saved.id,
      NoticeRequest(title = "수정됨", content = "수정된 내용")
    )

    // then
    assertEquals("수정됨", updated.title)
    assertEquals("수정된 내용", updated.content)
  }

  @Test
  fun `전체 목록 조회 테스트`() {
    // given
    noticeRepository.deleteAll()
    noticeService.save(NoticeRequest(title = "공지1", content = "내용1"))
    noticeService.save(NoticeRequest(title = "공지2", content = "내용2"))

    // when
    val result = noticeService.findAll()

    // then
    assertEquals(2, result.size)
  }

  @Test
  fun `활성화된 공지사항 조회 테스트`() {
    // given
    noticeRepository.deleteAll()
    noticeService.save(NoticeRequest(title = "공지1", content = "내용1", enabled = true))
    noticeService.save(NoticeRequest(title = "공지2", content = "내용2", enabled = false))

    // when
    val result = noticeService.findActiveNotices()

    // then
    assertEquals(1, result.size)
    assertEquals("공지1", result[0].title)
  }
}
