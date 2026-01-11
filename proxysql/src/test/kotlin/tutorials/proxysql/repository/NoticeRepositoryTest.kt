package tutorials.proxysql.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tutorials.proxysql.config.AbstractTests.AbstractRepositoryTest
import tutorials.proxysql.domain.Notice
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoticeRepositoryTest(
  private val noticeRepository: NoticeRepository,
) : AbstractRepositoryTest() {

  @BeforeEach
  fun setUp() {
    noticeRepository.deleteAll()
  }

  @Test
  fun `공지사항 저장 및 조회 테스트`() {
    // given
    val notice = Notice(
      title = "테스트 공지",
      content = "테스트 내용"
    )

    // when
    val saved = noticeRepository.save(notice)
    val found = noticeRepository.findById(saved.id!!)

    // then
    assertTrue(found.isPresent)
    assertEquals(notice.title, found.get().title)
    assertEquals(notice.content, found.get().content)
  }

  @Test
  fun `활성화된 공지사항 목록 조회 테스트`() {
    // given
    val notice1 = Notice(title = "공지1", content = "내용1", enabled = true)
    val notice2 = Notice(title = "공지2", content = "내용2", enabled = false)
    val notice3 = Notice(title = "공지3", content = "내용3", enabled = true)

    noticeRepository.saveAll(listOf(notice1, notice2, notice3))

    // when
    val result = noticeRepository.findByEnabledTrueOrderByCreatedAtDesc()

    // then
    assertEquals(2, result.size)
    assertTrue(result.all { it.enabled })
  }
}
