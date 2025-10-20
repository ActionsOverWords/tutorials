package tutorials

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tutorials.config.InfluxDB2TestContainerConfig

@Import(InfluxDB2TestContainerConfig::class)
@SpringBootTest
class InfluxdbApplicationTests {

  @Test
  fun contextLoads() {
  }

}
