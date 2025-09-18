package tutorials.influxdb3.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import tutorials.influxdb3.dto.CpuUsageQueryRequest
import tutorials.influxdb3.dto.CpuUsageQueryResponse
import tutorials.influxdb3.dto.CpuUsageSaveRequest
import tutorials.influxdb3.service.CpuUsageService

@RestController
class CpuUsageController(
  val cpuUsageService: CpuUsageService
) {

  @PostMapping("/cpu_usage")
  fun save(@RequestBody @Valid request: CpuUsageSaveRequest): ResponseEntity<Unit> {
    cpuUsageService.write(request)
    return ResponseEntity.ok().build()
  }

  @GetMapping("/cpu_usage")
  fun list(request: CpuUsageQueryRequest) : ResponseEntity<List<CpuUsageQueryResponse>> {
    val list = cpuUsageService.list(request)
    return ResponseEntity.ok(list)
  }

}
