package tutorials.javers.service

import org.javers.core.Javers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import tutorials.javers.config.AbstractTests.AbstractIntegrationTest
import tutorials.javers.domain.Company
import tutorials.javers.domain.PasswordPolicy
import tutorials.javers.domain.User
import tutorials.javers.dto.AuditDetailDto
import tutorials.javers.dto.AuditDto
import tutorials.javers.dto.SearchCondition
import tutorials.javers.repository.CompanyRepository
import tutorials.javers.repository.UserRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class JaVersServiceTest(
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

  private fun saveSampleData() {
    val company1 = companyService.createCompany(Company.of(name = "회사1", ceoName = "대표1"))
    companyService.createCompany(Company.of(name = "회사2", ceoName = "대표2"))

    repeat(20) { i ->
      userService.createUser(User(name = "관리자유저$i", password = "pass$i"))
    }

    repeat(15) { i ->
      userService.createUser(User(name = "매니저유저$i", password = "pass$i"))
    }

    company1.name = "변경된회사1"
    companyService.updateCompany(company1)

    flushAndClear()
  }

  @Test
  fun searchAuditHistory() {
    saveSampleData()

    log.debug("========== 1. 조건 없음 - 전체 조회 ==========")
    val result1 = jaVersService.searchAuditHistory(
      SearchCondition(
        pageNumber = 0,
        pageSize = 10
      )
    )
    logSearchResult(result1)
    assertTrue(result1.content.isNotEmpty())

    log.debug("========== 2. 페이지 간 커밋 중복 확인 ==========")
    val result1Page2 = jaVersService.searchAuditHistory(
      SearchCondition(
        pageNumber = 1,
        pageSize = 10
      )
    )
    val page1CommitIds = result1.content.map { it.commitId }.toSet()
    val page2CommitIds = result1Page2.content.map { it.commitId }.toSet()
    assertTrue(page1CommitIds.intersect(page2CommitIds).isEmpty())

    log.debug("========== 3. 모든 조건 지정 ==========")
    val result3 = jaVersService.searchAuditHistory(
      SearchCondition(
        domainType = "User",
        fromDate = LocalDateTime.now().minusMinutes(1),
        toDate = LocalDateTime.now(),
        pageNumber = 0,
        pageSize = 3
      )
    )
    logSearchResult(result3)
    assertTrue(result3.content.all { audit ->
      audit.changedDomains.all { it.domainType.contains("User") }
    })
    assertTrue(result3.content.isNotEmpty())
  }

  private fun logSearchResult(result: Page<AuditDto>) {
    log.debug("========== 변경이력 조회 ==========")
    logPage(result)

    result.content.forEach { audit ->
      log.debug("{}", audit)
    }
  }

  private fun getCommitId(): BigDecimal {
    val page = jaVersService.searchAuditHistory(
      SearchCondition(
        pageNumber = 0,
        pageSize = 3
      )
    )

    logSearchResult(page)

    return page.content.first().commitId
  }

  @Test
  fun getAuditDetailByCreate() {
    val company = companyService.createCompany(Company.of(name = "회사", ceoName = "대표"))
    flushAndClear()

    userService.createUser(User(name = "테스트유저", password = "password123").apply { this.company = company })
    flushAndClear()

    val commitId = getCommitId()
    val detail = jaVersService.getAuditDetail(commitId)

    log.debug("========== CREATE 상세 정보 ==========")
    logDetailResult(detail)

    assertEquals(commitId, detail.commitId)
    assertNotNull(detail.commitDate)

    // 1개 User 테이블 변경
    assertEquals(1, detail.changedTables.size)

    val changedTable = detail.changedTables.first()
    assertEquals("User", changedTable.tableName)
    assertEquals("CREATE", changedTable.changeType)

    // 생성된 속성들 확인
    assertTrue(changedTable.changedProperties.isNotEmpty())
    changedTable.changedProperties.forEach { property ->
      log.debug("  Property: {}, Old: {}, New: {}", property.propertyName, property.oldValue, property.newValue)
      assertNull(property.oldValue) // CREATE이므로 이전 값은 null
      assertNotNull(property.newValue)
    }

    val nameProperty = changedTable.changedProperties.find { it.propertyName == "name" }
    assertNotNull(nameProperty)
    assertEquals("테스트유저", nameProperty?.newValue)

    val enabledProperty = changedTable.changedProperties.find { it.propertyName == "enabled" }
    assertNotNull(enabledProperty)
    assertEquals("true", enabledProperty?.newValue)
  }

  @Test
  fun getAuditDetailByUpdate() {
    val company = companyService.createCompany(Company.of(name = "회사", ceoName = "대표"))
    flushAndClear()

    company.name = "수정된유저"
    val companyOption = company.companyOption
    companyOption.allowedIpAddresses = mutableListOf("127.0.0.1", "0.0.0.0")
    companyOption.securityOption.passwordPolicy = PasswordPolicy.HIGH
    companyService.updateCompany(company)
    flushAndClear()

    val commitId = getCommitId()
    val detail = jaVersService.getAuditDetail(commitId)

    log.debug("========== UPDATE 상세 정보 ==========")
    logDetailResult(detail)

    assertEquals(commitId, detail.commitId)
    assertNotNull(detail.commitDate)

    assertEquals(3, detail.changedTables.size)

    val changedTable = detail.changedTables.first()
    assertEquals("SecurityOption", changedTable.tableName)
    assertEquals("UPDATE", changedTable.changeType)

    assertTrue(changedTable.changedProperties.isNotEmpty())

    val passwordPolicyProperty = changedTable.changedProperties.find { it.propertyName == "passwordPolicy" }
    assertNotNull(passwordPolicyProperty)
    assertEquals("LOW", passwordPolicyProperty?.oldValue)
    assertEquals("HIGH", passwordPolicyProperty?.newValue)
  }

  @Test
  fun getAuditDetailByDelete() {
    val user = userService.createUser(User(name = "테스트유저", password = "password123"))
    flushAndClear()

    userService.deleteUser(user)
    flushAndClear()

    val commitId = getCommitId()
    val detail = jaVersService.getAuditDetail(commitId)

    log.debug("========== DELETE 상세 정보 ==========")
    logDetailResult(detail)

    assertEquals(commitId, detail.commitId)
    assertNotNull(detail.commitDate)

    assertEquals(1, detail.changedTables.size)

    val changedTable = detail.changedTables.first()
    assertEquals("User", changedTable.tableName)
    assertEquals("DELETE", changedTable.changeType)
  }

  private fun logDetailResult(detail: AuditDetailDto) {
    log.debug("CommitId: {}", detail.commitId)
    log.debug("Author: {}", detail.author)
    log.debug("CommitDate: {}", detail.commitDate)
    log.debug("Changed Tables Count: {}", detail.changedTables.size)

    detail.changedTables.forEach { table ->
      log.debug(
        "  Table: {}, ChangeType: {}, Properties Count: {}",
        table.tableName, table.changeType, table.changedProperties.size
      )

      table.changedProperties.forEach { property ->
        log.debug(
          "    Property: {}, Old: {}, New: {}",
          property.propertyName, property.oldValue, property.newValue
        )
      }
    }
  }

}
