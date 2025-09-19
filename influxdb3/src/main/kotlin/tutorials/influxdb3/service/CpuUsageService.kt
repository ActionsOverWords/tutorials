package tutorials.influxdb3.service

import org.springframework.stereotype.Service
import tutorials.influxdb3.dto.CpuUsageQueryRequest
import tutorials.influxdb3.dto.CpuUsageQueryResponse
import tutorials.influxdb3.dto.CpuUsageSaveRequest
import tutorials.influxdb3.repository.CpuUsageRepository

@Service
class CpuUsageService(
  val sdkCpuUsageRepository: CpuUsageRepository,
  val apacheArrowCpuUsageRepository: CpuUsageRepository,
) {

  fun writeBySdk(request: CpuUsageSaveRequest) {
    sdkCpuUsageRepository.save(request.toMeasurement())
  }

  fun listBySdk(request: CpuUsageQueryRequest): List<CpuUsageQueryResponse> {
    return sdkCpuUsageRepository.findRecents(request)
  }

  fun writeByArrow(request: CpuUsageSaveRequest) {
    apacheArrowCpuUsageRepository.save(request.toMeasurement())
  }

  fun listByArrow(request: CpuUsageQueryRequest): List<CpuUsageQueryResponse> {
    return apacheArrowCpuUsageRepository.findRecents(request)
  }

}
