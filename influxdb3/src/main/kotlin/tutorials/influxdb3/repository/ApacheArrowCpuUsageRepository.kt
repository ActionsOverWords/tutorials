package tutorials.influxdb3.repository

import org.apache.arrow.flight.CallOption
import org.apache.arrow.flight.FlightClient
import org.apache.arrow.memory.BufferAllocator
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import tutorials.influxdb3.base.convertor.MeasurementToArrowConverter
import tutorials.influxdb3.base.extentions.logger
import tutorials.influxdb3.base.repository.ApacheArrowSaveRepository
import tutorials.influxdb3.dto.CpuUsageQueryRequest
import tutorials.influxdb3.dto.CpuUsageQueryResponse
import tutorials.influxdb3.measurement.CpuUsage
import java.sql.ResultSet

@Repository
class ApacheArrowCpuUsageRepository(
  val jdbcTemplate: JdbcTemplate,
  val allocator: BufferAllocator,
  flightClient: FlightClient,
  callOption: CallOption,
) : ApacheArrowSaveRepository(flightClient, callOption), CpuUsageRepository {

  val log = logger()

  private val cpuUsageRowMapper = RowMapper<CpuUsageQueryResponse> { rs: ResultSet, _: Int ->
    CpuUsageQueryResponse(
      time = rs.getTimestamp("time").toInstant(),
      architecture = rs.getString("architecture"),
      usagePercent = rs.getDouble("usage_percent"),
    )
  }

  override fun save(cpuUsage: CpuUsage) {
    throw UnsupportedOperationException("Not running")
    //val root = MeasurementToArrowConverter.toVectorSchemaRoot(listOf(cpuUsage), CpuUsage::class, allocator)
    //writeBatch(cpuUsage.measurement, root)
  }

  override fun findRecents(request: CpuUsageQueryRequest): List<CpuUsageQueryResponse> {
    val query = """
      SELECT *
      FROM cpu_usage
      ${request.toWhereClause()}
      ORDER BY time ${request.order}
      LIMIT ${request.pageSize}
    """.trimIndent()

    log.debug("query: {}", query)

    return jdbcTemplate.query(query, cpuUsageRowMapper)
  }

}
