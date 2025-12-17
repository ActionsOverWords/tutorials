package tutorials.javers.basic

import org.javers.core.Javers
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import tutorials.javers.config.AbstractTests.AbstractIntegrationTest
import tutorials.javers.config.JaversTestConfig
import tutorials.javers.domain.Company
import tutorials.javers.domain.PasswordPolicy
import tutorials.javers.domain.User
import tutorials.javers.repository.CompanyRepository
import tutorials.javers.repository.UserRepository

@Import(JaversTestConfig::class)
class JaversManualCommitTest(
  val javers: Javers,
  val companyRepository: CompanyRepository,
  val userRepository: UserRepository,
) : AbstractIntegrationTest() {

  @BeforeEach
  fun setUp() {
    userRepository.deleteAll()
    companyRepository.deleteAll()
  }

  @Test
  fun `DB 저장 시 변경 내역이 저장된다`() {
    val user = User(name = "사용자", password = "")
    userRepository.save(user)
    javers.commit("author", user)

    flushAndClear()

    val snapshots = javers.findSnapshots(
      QueryBuilder.byInstance(user).build()
    )
    logs(snapshots)

    assertEquals(1, snapshots.size)
    assertEquals("사용자", snapshots[0].getPropertyValue("name"))
  }

  @Test
  fun `정보 변경 시 변경 내역이 저장된다`() {
    val user = userRepository.save(User(name = "사용자", password = ""))
    javers.commit("author", user)

    val email = "tutorials@tutorials.com"
    user.email = email
    userRepository.save(user)
    javers.commit("author", user)

    flushAndClear()

    val snapshots = javers.findSnapshots(
      QueryBuilder.byInstance(user).build()
    )
    logs(snapshots)

    assertEquals(2, snapshots.size)
    assertEquals(email, snapshots[0].getPropertyValue("email"))
  }

  @Test
  fun `OneToOne 관계도 변경 내역이 관리된다`() {
    val company = Company.of(name = "Company", ceoName = "CEO")
    companyRepository.save(company)
    javers.commit("author", company)

    company.companyOption.securityOption.passwordPolicy = PasswordPolicy.HIGH
    companyRepository.save(company)
    javers.commit("author", company)

    flushAndClear()

    val snapshots = javers.findSnapshots(
      QueryBuilder.anyDomainObject()
        .withVersion(2)
        .build()
    )
    logs(snapshots)

    snapshots.forEach { snapshot ->
      log.debug("")
      log.debug("commitMetadata: {}", snapshot.commitMetadata)
      log.debug("state: {}", snapshot.state)
      log.debug("type: {}", snapshot.type)
      log.debug("version: {}", snapshot.version)
      log.debug("globalId: {}", snapshot.globalId)
      log.debug("changed: {}", snapshot.changed)
    }
  }

  @Test
  fun `CdoSnapshotState를 사용하여 변경된 속성을 비교한다`() {
    val user = userRepository.save(User(name = "사용자", password = "password123"))
    javers.commit("author", user)

    user.email = "user@example.com"
    user.password = "newPassword456"
    userRepository.save(user)
    javers.commit("author", user)

    flushAndClear()

    val snapshots = javers.findSnapshots(
      QueryBuilder.byInstance(user).build()
    )
    logs(snapshots)

    assertEquals(2, snapshots.size)

    val currentState = snapshots[0].state
    val previousState = snapshots[1].state

    val differentProperties = currentState.differentValues(previousState)

    log.debug("Changed properties: {}", differentProperties)
    differentProperties.forEach { propertyName ->
      log.debug(
        "Property '{}' changed from '{}' to '{}'",
        propertyName,
        previousState.getPropertyValue(propertyName),
        currentState.getPropertyValue(propertyName)
      )
    }

    assertEquals(1, differentProperties.size)
    assert(differentProperties.contains("email"))
  }

  @Test
  fun `findSnapshots와 findChanges 비교`() {
    val user = userRepository.save(User(name = "사용자", password = "password123"))
    javers.commit("author", user)

    user.email = "user@example.com"
    user.password = "newPassword456"
    userRepository.save(user)
    javers.commit("author", user)

    user.name = "변경된 사용자"
    userRepository.save(user)
    javers.commit("author", user)

    flushAndClear()

    log.debug("========== findSnapshots ==========")
    val snapshots = javers.findSnapshots(
      QueryBuilder.byInstance(user).build()
    )

    snapshots.forEach { snapshot ->
      log.debug("")
      log.debug("Snapshot version: {}", snapshot.version)
      log.debug("Snapshot commitId: {}", snapshot.commitId)
      log.debug("Snapshot state: {}", snapshot.state)
      log.debug("Snapshot changed properties: {}", snapshot.changed)

      if (snapshot.version > 1) {
        val currentState = snapshot.state
        val previousSnapshot = snapshots.find { it.version == snapshot.version - 1 }
        if (previousSnapshot != null) {
          val differentProps = currentState.differentValues(previousSnapshot.state)
          log.debug("Different properties using CdoSnapshotState: {}", differentProps)
        }
      }
    }

    log.debug("")
    log.debug("========== findChanges ==========")
    val changes = javers.findChanges(
      QueryBuilder.byInstance(user).build()
    )

    changes.forEach { change ->
      log.debug("")
      log.debug("Change type: {}", change.javaClass.simpleName)
      log.debug("Change commitMetadata: {}", change.commitMetadata)
      log.debug("Change affectedGlobalId: {}", change.affectedGlobalId)
      log.debug("Change details: {}", change)
    }

    log.debug("")
    log.debug("========== 비교 결과 ==========")
    log.debug("Total snapshots: {}", snapshots.size)
    log.debug("Total changes: {}", changes.size)

    assertEquals(3, snapshots.size)
    assertEquals(9, changes.size)
  }

}
