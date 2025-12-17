package tutorials.javers.config

import org.javers.core.Javers
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tutorials.javers.config.AbstractTests.AbstractIntegrationTest
import tutorials.javers.repository.CompanyRepository
import tutorials.javers.repository.UserRepository
import tutorials.javers.service.CompanyService

class JaversInterceptorTest(
  val javers: Javers,

  val companyRepository: CompanyRepository,
  val userRepository: UserRepository,

  val companyService: CompanyService,
) : AbstractIntegrationTest() {

  @BeforeEach
  fun setUp() {
    userRepository.deleteAll()
    companyRepository.deleteAll()
  }

  @Test
  fun createCompanyAndUser() {
    companyService.createCompanyAndUser(
      "회사", "대표",
      "사용자", "password123"
    )
    flushAndClear()

    val snapshots = javers.findSnapshots(
      QueryBuilder.anyDomainObject().build()
    )
    logs(snapshots)

    assertEquals(4, snapshots.size)
    assertEquals("사용자", snapshots[0].getPropertyValue("name"))
  }

}
