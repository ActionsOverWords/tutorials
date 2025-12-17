package tutorials.javers.config

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Page
import org.springframework.test.context.TestConstructor
import tutorials.javers.TestcontainersConfiguration
import tutorials.javers.base.extentions.logger

abstract class AbstractTests {

  @SpringBootTest
  @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
  @Import(TestcontainersConfiguration::class)
  @Transactional
  abstract class AbstractIntegrationTest {

    protected val log by logger()

    @Autowired
    lateinit var entityManager: EntityManager

    protected fun logs(snapshots: List<CdoSnapshot>) {
      snapshots.forEach { log.info("{}",  it) }
    }

    protected fun logPage(page: Page<*>) {
      log.debug("Total elements: {}", page.totalElements)
      log.debug("Total pages: {}", page.totalPages)
      log.debug("Current page: {}", page.number)
      log.debug("Page size: {}", page.size)
      log.debug("Has next: {}", page.hasNext())
      log.debug("Content size: {}", page.content.size)
    }

    protected fun flushAndClear() {
      entityManager.flush()
      entityManager.clear()
      log.debug("Flush and clear...")
    }
  }

}
