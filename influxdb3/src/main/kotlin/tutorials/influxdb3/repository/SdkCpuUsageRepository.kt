package tutorials.influxdb3.repository

import com.influxdb.v3.client.InfluxDBClient
import org.springframework.stereotype.Repository
import tutorials.influxdb3.base.extentions.logger
import tutorials.influxdb3.dto.CpuUsageQueryRequest
import tutorials.influxdb3.dto.CpuUsageQueryResponse
import tutorials.influxdb3.measurement.CpuUsage
import kotlin.streams.asSequence

@Repository
class SdkCpuUsageRepository(
  val influxDBClient: InfluxDBClient,
) : CpuUsageRepository {

  val log = logger()

  override fun save(cpuUsage: CpuUsage) {
    val record = cpuUsage.toLineProtocol()
    log.debug("record: {}", record)

    influxDBClient.writeRecord(record)
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

    return influxDBClient.queryRows(query)
      .use { row ->
        row.asSequence()
          .map { record ->
            CpuUsageQueryResponse.fromRecord(record)
          }
          .toList()
      }
  }

}
