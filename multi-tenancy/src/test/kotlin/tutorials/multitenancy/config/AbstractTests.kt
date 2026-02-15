package tutorials.multitenancy.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor

abstract class AbstractTests {

  @SpringBootTest
  @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
  abstract class AbstractIntegrationTest
}
