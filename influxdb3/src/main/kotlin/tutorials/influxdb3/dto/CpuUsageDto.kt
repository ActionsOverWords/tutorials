package tutorials.influxdb3.dto

import jakarta.validation.constraints.Size
import tutorials.influxdb3.base.dto.AbstractQueryRequestDto
import tutorials.influxdb3.base.extentions.toWhereClause
import tutorials.influxdb3.base.measurement.MeasurementConst.NANOS_PER_SECOND
import tutorials.influxdb3.measurement.CpuUsage
import java.math.BigInteger
import java.time.Instant

data class CpuUsageSaveRequest(
  @Size(min = 2)
  val architecture: String,
  val usage: Double,
) {
  fun toMeasurement() = CpuUsage(Instant.now())
    .setArchitecture(architecture)
    .setUsagePercent(usage)
}

data class CpuUsageQueryRequest(
  val architecture: String?,
): AbstractQueryRequestDto() {
  override fun toWhereClause(): String {
    val map = mutableMapOf<String, Any>()
    architecture?.let { map["architecture"] = it }
    return map.toWhereClause()
  }
}

data class CpuUsageQueryResponse(
  val time: Instant?,
  val architecture: String?,
  val usagePercent: Double?,
) {
  companion object {
    fun fromRecord(record: Map<String, Any>): CpuUsageQueryResponse {
      val timeValue = record["time"] as BigInteger
      val epochSecond = timeValue.divide(NANOS_PER_SECOND).toLong()
      val nanoAdjustment = timeValue.remainder(NANOS_PER_SECOND).toLong()

      return CpuUsageQueryResponse(
        time = Instant.ofEpochSecond(epochSecond, nanoAdjustment),
        architecture = record["architecture"] as String?,
        usagePercent = record["usage_percent"] as Double?,
      )
    }
  }
}
