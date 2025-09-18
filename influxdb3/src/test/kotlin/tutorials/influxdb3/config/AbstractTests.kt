package tutorials.influxdb3.config

import com.influxdb.v3.client.InfluxDBClient
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import org.testcontainers.junit.jupiter.Testcontainers
import tutorials.influxdb3.base.extentions.logger

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class AbstractIntegrationTest {
  val log = logger()
}

@Deprecated("TestContainer 실행 안됨")
@Testcontainers
//@Import(InfluxDB3TestContainerConfig::class)
abstract class AbstractContainerTest : AbstractIntegrationTest() {
  @Autowired
  lateinit var influxDbClient: InfluxDBClient

  @BeforeEach
  fun setup() {
  }

}
