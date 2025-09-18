package tutorials.influxdb3.service

import com.influxdb.v3.client.InfluxDBClient
import org.springframework.stereotype.Service
import tutorials.influxdb3.base.extentions.logger
import tutorials.influxdb3.dto.CpuUsageQueryRequest
import tutorials.influxdb3.dto.CpuUsageQueryResponse
import tutorials.influxdb3.dto.CpuUsageSaveRequest
import kotlin.streams.asSequence

@Service
class CpuUsageService(
  val influxDBClient: InfluxDBClient,
) {

  val log = logger()

  fun write(request: CpuUsageSaveRequest) {
    val record = request.toLineProtocol()
    log.debug("record: {}", record)

    influxDBClient.writeRecord(record)
  }

  fun list(request: CpuUsageQueryRequest): List<CpuUsageQueryResponse> {
    log.debug("request: {}", request)

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
