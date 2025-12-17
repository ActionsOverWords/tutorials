package tutorials.javers.basic

import org.javers.common.exception.JaversException
import org.javers.core.Javers
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.ClassOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestClassOrder
import org.junit.jupiter.api.assertThrows
import org.springframework.context.annotation.Import
import tutorials.javers.config.AbstractTests.AbstractIntegrationTest
import tutorials.javers.config.JaversTestConfig
import tutorials.javers.domain.Company
import tutorials.javers.domain.PasswordPolicy
import tutorials.javers.repository.CompanyRepository
import tutorials.javers.service.CompanyJaversService

@Import(JaversTestConfig::class)
@TestClassOrder(ClassOrderer.OrderAnnotation::class)
class JaversAuditableTest(
  val javers: Javers,
  val companyJaversService: CompanyJaversService,
  val companyRepository: CompanyRepository,
) : AbstractIntegrationTest() {

  @BeforeEach
  fun setUp() {
    companyRepository.deleteAll()
  }

  private fun findSnapshots(): List<CdoSnapshot> {
    val snapshots = javers.findSnapshots(
      QueryBuilder.anyDomainObject().build()
    )
    logs(snapshots)

    return snapshots
  }

  @Nested
  @Order(1)
  inner class SaveTest {
    @Test
    fun createCompanyByEntity() {
      val company = Company.of(name = "회사", ceoName = "대표")
      companyJaversService.createCompany(company)
      flushAndClear()

      val snapshots = findSnapshots()
      assertEquals(3, snapshots.size)
    }

    @Test
    fun createCompanyByParameter() {
      val exception = assertThrows<JaversException> {
        companyJaversService.createCompany("회사", "대표")
      }

      assertEquals("COMMITTING_TOP_LEVEL_VALUES_NOT_SUPPORTED", exception.code.name)
    }

    @Test
    fun createCompanyAndUser() {
      val exception = assertThrows<JaversException> {
        companyJaversService.createCompanyByJavers(name = "회사", ceoName = "대표")
      }

      assertEquals("COMMITTING_TOP_LEVEL_VALUES_NOT_SUPPORTED", exception.code.name)
    }
  }

  @Nested
  @Order(2)
  inner class UpdateTest {
    @Test
    fun updateCompanyByEntity() {
      val company = Company.of(name = "회사", ceoName = "대표")
      companyJaversService.createCompany(company)
      flushAndClear()

      company.name = "company"
      company.companyOption.securityOption.passwordPolicy = PasswordPolicy.HIGH
      companyJaversService.updateCompany(company)
      flushAndClear()

      val snapshots = findSnapshots()
      assertEquals(5, snapshots.size)
    }

    @Test
    fun updateCompanyByParameter() {
      val company = Company.of(name = "회사", ceoName = "대표")
      companyJaversService.createCompany(company)
      flushAndClear()

      val exception = assertThrows<JaversException> {
        companyJaversService.updateCompany(company.id!!, "company", "ceo")
      }

      assertEquals("COMMITTING_TOP_LEVEL_VALUES_NOT_SUPPORTED", exception.code.name)
    }
  }

  @Nested
  @Order(3)
  inner class DeleteTest {
    @Test
    fun deleteCompanyByEntity() {
      val company = Company.of(name = "회사", ceoName = "대표")
      companyJaversService.createCompany(company)
      flushAndClear()

      companyJaversService.deleteCompany(company)
      flushAndClear()

      val snapshots = findSnapshots()
      assertEquals(4, snapshots.size)
    }

    @Test
    fun deleteCompanyById() {
      val company = Company.of(name = "회사", ceoName = "대표")
      companyJaversService.createCompany(company)
      flushAndClear()

      companyJaversService.deleteCompany(company.id!!)
      flushAndClear()

      val snapshots = findSnapshots()
      assertEquals(4, snapshots.size)
    }
  }

}
