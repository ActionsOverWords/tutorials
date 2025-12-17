package tutorials.javers.basic

import org.javers.core.Javers
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import tutorials.javers.config.AbstractTests.AbstractIntegrationTest
import tutorials.javers.config.JaversTestConfig
import tutorials.javers.domain.Company
import tutorials.javers.domain.PasswordPolicy
import tutorials.javers.repository.CompanyJaversRepository

@Import(JaversTestConfig::class)
class JaversSpringDataAuditableTest(
  val javers: Javers,
  val companyJaversRepository: CompanyJaversRepository,
) : AbstractIntegrationTest() {

  @BeforeEach
  fun setUp() {
    companyJaversRepository.deleteAll()
  }

  private fun findSnapshots(): List<CdoSnapshot> {
    val snapshots = javers.findSnapshots(
      QueryBuilder.anyDomainObject().build()
    )
    logs(snapshots)

    return snapshots
  }

  @Test
  fun createCompany() {
    val company = Company.of(name = "회사", ceoName = "대표")
    companyJaversRepository.save(company)
    flushAndClear()

    val snapshots = findSnapshots()
    assertEquals(3, snapshots.size)
  }

  @Test
  fun updateCompany() {
    val company = Company.of(name = "회사", ceoName = "대표")
    companyJaversRepository.save(company)
    flushAndClear()

    company.companyOption.securityOption.passwordPolicy = PasswordPolicy.HIGH
    companyJaversRepository.save(company)
    flushAndClear()

    val snapshots = findSnapshots()
    assertEquals(4, snapshots.size)
  }

  @Test
  fun deleteCompanyByEntity() {
    val company = Company.of(name = "회사", ceoName = "대표")
    companyJaversRepository.save(company)
    flushAndClear()

    companyJaversRepository.delete(company)
    flushAndClear()

    val snapshots = findSnapshots()
    assertEquals(4, snapshots.size)
  }

  @Test
  fun deleteCompanyById() {
    val company = Company.of(name = "회사", ceoName = "대표")
    companyJaversRepository.save(company)
    flushAndClear()

    companyJaversRepository.deleteById(company.id!!)
    flushAndClear()

    val snapshots = findSnapshots()
    assertEquals(4, snapshots.size)
  }

}
