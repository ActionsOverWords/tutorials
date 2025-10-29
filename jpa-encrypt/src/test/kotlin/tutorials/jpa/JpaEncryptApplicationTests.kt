package tutorials.jpa

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tutorials.jpa.config.TestcontainersConfiguration

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class JpaEncryptApplicationTests {

  @Test
  fun contextLoads() {
  }

}
