package tutorials.proxysql.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester
import tutorials.proxysql.config.AbstractTests.AbstractIntegrationTest
import tutorials.proxysql.domain.Notice
import tutorials.proxysql.dto.NoticeRequest
import tutorials.proxysql.repository.NoticeRepository
import tutorials.proxysql.util.isEqualTo
import tutorials.proxysql.util.isNotNull

@AutoConfigureMockMvc
class NoticeControllerTest(
  private val mockMvcTester: MockMvcTester,
  private val objectMapper: ObjectMapper,
  private val noticeRepository: NoticeRepository,
) : AbstractIntegrationTest() {

  @BeforeEach
  fun setUp() {
    noticeRepository.deleteAll()
  }

  @Test
  fun `공지사항 생성 API 테스트`() {
    // given
    val request = NoticeRequest(
      title = "테스트 공지",
      content = "테스트 내용"
    )

    // when & then
    val result = mockMvcTester.post().uri("/notices")
      .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(request))
      .exchange()

    assertThat(result)
      .hasStatus(HttpStatus.CREATED)
      .bodyJson()
      .hasPathSatisfying("$.id", isNotNull())
      .hasPathSatisfying("$.title", isEqualTo("테스트 공지"))
      .hasPathSatisfying("$.content", isEqualTo("테스트 내용"))
  }

  @Test
  fun `공지사항 수정 API 테스트`() {
    // given
    val created = noticeRepository.save(Notice(title = "원본", content = "원본 내용"))
    val request = NoticeRequest(title = "수정됨", content = "수정된 내용")

    // when & then
    val result = mockMvcTester.put().uri("/notices/${created.id}")
      .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(request))
      .exchange()

    assertThat(result)
      .hasStatusOk()
      .bodyJson()
      .hasPathSatisfying("$.title", isEqualTo("수정됨"))
      .hasPathSatisfying("$.content", isEqualTo("수정된 내용"))
  }

  @Test
  fun `전체 목록 조회 API 테스트`() {
    // given
    noticeRepository.save(Notice(title = "공지1", content = "내용1"))
    noticeRepository.save(Notice(title = "공지2", content = "내용2"))

    // when & then
    val result = mockMvcTester.get().uri("/notices")
      .exchange()

    assertThat(result)
      .hasStatusOk()
      .bodyJson()
      .hasPathSatisfying("$.length()", isEqualTo(2))
  }

  @Test
  fun `활성화된 공지사항 조회 API 테스트`() {
    // given
    noticeRepository.save(Notice(title = "공지1", content = "내용1", enabled = true))
    noticeRepository.save(Notice(title = "공지2", content = "내용2", enabled = false))

    // when & then
    val result = mockMvcTester.get().uri("/notices/active")
      .exchange()

    assertThat(result)
      .hasStatusOk()
      .bodyJson()
      .hasPathSatisfying("$.length()", isEqualTo(1))
      .hasPathSatisfying("$[0].title", isEqualTo("공지1"))
  }
}
