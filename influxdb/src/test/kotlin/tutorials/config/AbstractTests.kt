package tutorials.config

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.reactive.InfluxDBClientReactive
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor
import org.testcontainers.junit.jupiter.Testcontainers
import tutorials.base.extentions.logger

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class AbstractIntegrationTest {
  val log = logger()
}

@Testcontainers
@Import(InfluxDB2TestContainerConfig::class)
abstract class AbstractContainerTest : AbstractIntegrationTest() {
  @Autowired
  lateinit var influxDb2Client: InfluxDBClient

  @Autowired
  lateinit var influxDb2ClientReactive: InfluxDBClientReactive

}
