package tutorials.jpa.config

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers
import tutorials.jpa.base.extentions.logger

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class AbstractIntegrationTest {
  val log = logger()

  @Autowired
  lateinit var entityManager: EntityManager

  protected fun flushAndClear() {
    entityManager.flush()
    entityManager.clear()
    log.debug("Flush and clear...")
  }

}

@Testcontainers
@Import(TestcontainersConfiguration::class)
abstract class AbstractContainerTest : AbstractIntegrationTest()
