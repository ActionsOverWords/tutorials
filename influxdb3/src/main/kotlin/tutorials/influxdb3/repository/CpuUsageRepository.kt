package tutorials.influxdb3.repository

import tutorials.influxdb3.dto.CpuUsageQueryRequest
import tutorials.influxdb3.dto.CpuUsageQueryResponse
import tutorials.influxdb3.measurement.CpuUsage

interface CpuUsageRepository {

  fun save(cpuUsage: CpuUsage)

  fun findRecents(request: CpuUsageQueryRequest) : List<CpuUsageQueryResponse>

}
