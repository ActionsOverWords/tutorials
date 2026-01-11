package tutorials.proxysql.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import tutorials.proxysql.dto.NoticeRequest
import tutorials.proxysql.dto.NoticeResponse
import tutorials.proxysql.service.NoticeService
import java.util.UUID

@RestController
@RequestMapping("/notices")
class NoticeController(
  private val noticeService: NoticeService
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@RequestBody request: NoticeRequest): NoticeResponse {
    return noticeService.save(request)
  }

  @PutMapping("/{id}")
  fun update(
    @PathVariable id: UUID,
    @RequestBody request: NoticeRequest
  ): NoticeResponse {
    return noticeService.update(id, request)
  }

  @GetMapping
  fun findAll(): List<NoticeResponse> {
    return noticeService.findAll()
  }

  @GetMapping("/active")
  fun findActive(): List<NoticeResponse> {
    return noticeService.findActiveNotices()
  }
}
