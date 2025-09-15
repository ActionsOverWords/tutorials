package tutorials

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tutorials.config.InfluxDbTestContainerConfig

@Import(InfluxDbTestContainerConfig::class)
@SpringBootTest
class InfluxdbApplicationTests {

  @Test
  fun contextLoads() {
  }

}
