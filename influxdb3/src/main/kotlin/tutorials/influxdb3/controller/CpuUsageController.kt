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

  @PostMapping("/cpu_usage/sdk")
  fun writeBySdk(@RequestBody @Valid request: CpuUsageSaveRequest): ResponseEntity<Unit> {
    cpuUsageService.writeBySdk(request)
    return ResponseEntity.ok().build()
  }

  @GetMapping("/cpu_usage/sdk")
  fun listBySdk(request: CpuUsageQueryRequest) : ResponseEntity<List<CpuUsageQueryResponse>> {
    val list = cpuUsageService.listBySdk(request)
    return ResponseEntity.ok(list)
  }

  @PostMapping("/cpu_usage/arrow")
  fun writeByArrow(@RequestBody @Valid request: CpuUsageSaveRequest): ResponseEntity<Unit> {
    cpuUsageService.writeByArrow(request)
    return ResponseEntity.ok().build()
  }

  @GetMapping("/cpu_usage/arrow")
  fun listByArrow(request: CpuUsageQueryRequest) : ResponseEntity<List<CpuUsageQueryResponse>> {
    val list = cpuUsageService.listByArrow(request)
    return ResponseEntity.ok(list)
  }

}
