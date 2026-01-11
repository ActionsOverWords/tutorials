package tutorials.proxysql.config

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor
import org.springframework.transaction.annotation.Transactional
import tutorials.proxysql.TestcontainersConfiguration

abstract class AbstractTests {

  /**
   * Repository 테스트용 베이스 클래스
   * MariaDB Testcontainer를 사용
   */
  @DataJpaTest
  @Import(TestcontainersConfiguration::class)
  @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
  @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
  abstract class AbstractRepositoryTest

  /**
   * Service/Controller 통합 테스트용 베이스 클래스
   * MariaDB Testcontainer를 사용
   */
  @SpringBootTest
  @Import(TestcontainersConfiguration::class)
  @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
  @Transactional
  abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var entityManager: EntityManager

    protected fun flushAndClear() {
      entityManager.flush()
      entityManager.clear()
    }
  }
}
