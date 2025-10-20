package tutorials.influxdb3.measurement

import com.influxdb.v3.client.InfluxDBClient
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import tutorials.influxdb3.config.AbstractIntegrationTest
import java.time.Instant

/**
 * 테스트 케이스 에러 발생 시
 * <pre>com.influxdb.v3.client.InfluxDBApiException: java.net.ConnectException</pre>
 * Docker Compose(influxdb3/compose.yml)로 InfluxDB3 실행 후 테스트 진행
 * - InfluxDB3 TestContainer 실행 안되는 이슈 있음
 */
class SdkTest : AbstractIntegrationTest() {

  @Autowired
  lateinit var influxDBClient: InfluxDBClient


  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class WriteTest {
    @Test
    fun measurementByOne() {
      val cpuUsage = CpuUsage(Instant.now())
        .setArchitecture("x86")
        .setUsagePercent(90.0)

      val record = cpuUsage.toLineProtocol()
      log.debug("{}", record)

      influxDBClient.writeRecord(record)
    }

    @Test
    fun measurementByMany() {
      val cpuUsages = listOf(
        CpuUsage().setArchitecture("x86").setUsagePercent(22.2),
        CpuUsage().setArchitecture("arm").setUsagePercent(75.0),
      )

      val records = cpuUsages.map { it.toLineProtocol() }
      log.debug("{}", records)

      influxDBClient.writeRecords(records)
    }
  }


  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class QueryTest {
    private var query = "SELECT * FROM cpu_usage ORDER BY time DESC LIMIT 3"

    @Test
    fun query() {
      influxDBClient.query(query)
        .use { row ->
          row.forEach { record ->
            log.debug("{}", record)
          }
        }
    }

    @Test
    fun queryRows() {
      influxDBClient.queryRows(query)
        .use { row ->
          row.forEach { record ->
            log.debug("{}", record)
          }
        }
    }

    @Test
    fun queryPoints() {
      influxDBClient.queryPoints(query)
        .use { row ->
          row.forEach { record ->
            log.debug(
              "measurement:{}, architecture:{}, usage_percent:{}, time:{}",
              record.measurement,
              record.getTag("architecture"),
              record.getField("usage_percent"),
              record.timestamp
            )
          }
        }
    }
  }

}
