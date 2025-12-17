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
import tutorials.javers.service.JaVersService
import tutorials.javers.service.UserService

class JaversInterceptorTest(
  val javers: Javers,
  val jaVersService: JaVersService,

  val companyRepository: CompanyRepository,
  val userRepository: UserRepository,

  val companyService: CompanyService,
  val userService: UserService,
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
