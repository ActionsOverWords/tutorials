package tutorials.openfeign.querydsl.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor
import org.testcontainers.junit.jupiter.Testcontainers
import tutorials.openfeign.querydsl.base.extentions.logger

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class AbstractIntegrationTest {
  val log = logger()
}

@Testcontainers
@Import(TestcontainersConfiguration::class)
abstract class AbstractContainerTest : AbstractIntegrationTest()
