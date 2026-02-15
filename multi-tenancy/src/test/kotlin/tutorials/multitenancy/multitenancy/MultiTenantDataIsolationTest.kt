package tutorials.multitenancy.multitenancy

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.CannotCreateTransactionException
import tutorials.multitenancy.config.AbstractTests.AbstractIntegrationTest
import tutorials.multitenancy.config.TenantContext
import tutorials.multitenancy.domain.User
import tutorials.multitenancy.repository.UserRepository
import java.sql.DriverManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MultiTenantDataIsolationTest(
  private val userRepository: UserRepository,
) : AbstractIntegrationTest() {

  companion object {
    private const val TENANT_A_URL = "jdbc:mariadb://localhost:3316/tenantdb"
    private const val TENANT_A_USER = "tenant_a_user"
    private const val TENANT_A_PASSWORD = "tenant_a_pass"

    private const val TENANT_B_URL = "jdbc:mariadb://localhost:3317/tenantdb"
    private const val TENANT_B_USER = "tenant_b_user"
    private const val TENANT_B_PASSWORD = "tenant_b_pass"
  }

  @AfterEach
  fun tearDown() {
    TenantContext.clear()
  }

  @Test
  fun `각 테넌트 DB는 독립적인 데이터를 가져야 한다`() {
    TenantContext.setTenant("tenant-a")
    val usersInTenantA = userRepository.findAll()
    assertTrue(usersInTenantA.size >= 1, "tenant-a에는 최소 1명의 사용자가 있어야 함")
    assertTrue(usersInTenantA.all { it.tenantId == "tenant-a" }, "모든 사용자의 tenantId는 tenant-a여야 함")
    TenantContext.clear()

    TenantContext.setTenant("tenant-b")
    val usersInTenantB = userRepository.findAll()
    assertTrue(usersInTenantB.size >= 1, "tenant-b에는 최소 1명의 사용자가 있어야 함")
    assertTrue(usersInTenantB.all { it.tenantId == "tenant-b" }, "모든 사용자의 tenantId는 tenant-b여야 함")
    TenantContext.clear()

    assertTrue(usersInTenantA.isNotEmpty() && usersInTenantB.isNotEmpty(), "각 테넌트에 사용자 데이터가 존재해야 함")
  }

  @Test
  fun `tenant-a에 새 사용자 추가 시 tenant-b에는 영향을 주지 않아야 한다`() {
    TenantContext.setTenant("tenant-a")
    val initialCountA = userRepository.count()

    val newUserA = User(
      username = "test-user-a-${System.currentTimeMillis()}",
      password = "password",
      tenantId = "tenant-a"
    )
    userRepository.save(newUserA)

    val newCountA = userRepository.count()
    assertEquals(initialCountA + 1, newCountA, "tenant-a에 사용자가 추가되어야 함")
    TenantContext.clear()

    TenantContext.setTenant("tenant-b")
    val countB = userRepository.count()
    TenantContext.clear()

    TenantContext.setTenant("tenant-a")
    val finalCountA = userRepository.count()
    assertEquals(newCountA, finalCountA, "tenant-a의 사용자 수는 유지되어야 함")
    TenantContext.clear()

    TenantContext.setTenant("tenant-b")
    val finalCountB = userRepository.count()
    assertEquals(countB, finalCountB, "tenant-b의 사용자 수는 변하지 않아야 함")
    TenantContext.clear()
  }

  @Test
  fun `동시에 여러 테넌트에 접근해도 데이터 격리가 유지되어야 한다`() {
    val executor = Executors.newFixedThreadPool(4)
    val latch = CountDownLatch(4)
    val results = mutableMapOf<String, Int>()
    val errors = mutableListOf<String>()

    repeat(2) { index ->
      executor.submit {
        try {
          TenantContext.setTenant("tenant-a")
          Thread.sleep((10..50).random().toLong())
          val count = userRepository.count()
          synchronized(results) {
            results["tenant-a-thread-$index"] = count.toInt()
          }

          val users = userRepository.findAll()
          users.forEach { user ->
            if (user.tenantId != "tenant-a") {
              synchronized(errors) {
                errors.add("tenant-a 스레드에서 ${user.tenantId} 사용자 발견: ${user.username}")
              }
            }
          }
        } catch (e: Exception) {
          synchronized(errors) {
            errors.add("tenant-a 스레드 에러: ${e.message}")
          }
        } finally {
          TenantContext.clear()
          latch.countDown()
        }
      }
    }

    repeat(2) { index ->
      executor.submit {
        try {
          TenantContext.setTenant("tenant-b")
          Thread.sleep((10..50).random().toLong())
          val count = userRepository.count()
          synchronized(results) {
            results["tenant-b-thread-$index"] = count.toInt()
          }

          val users = userRepository.findAll()
          users.forEach { user ->
            if (user.tenantId != "tenant-b") {
              synchronized(errors) {
                errors.add("tenant-b 스레드에서 ${user.tenantId} 사용자 발견: ${user.username}")
              }
            }
          }
        } catch (e: Exception) {
          synchronized(errors) {
            errors.add("tenant-b 스레드 에러: ${e.message}")
          }
        } finally {
          TenantContext.clear()
          latch.countDown()
        }
      }
    }

    latch.await(10, TimeUnit.SECONDS)
    executor.shutdown()

    assertTrue(errors.isEmpty(), "에러 발생: ${errors.joinToString(", ")}")

    val tenantAResults = results.filterKeys { it.startsWith("tenant-a") }.values
    assertTrue(tenantAResults.all { it == tenantAResults.first() },
      "tenant-a 스레드들은 동일한 사용자 수를 조회해야 함: $tenantAResults")

    val tenantBResults = results.filterKeys { it.startsWith("tenant-b") }.values
    assertTrue(tenantBResults.all { it == tenantBResults.first() },
      "tenant-b 스레드들은 동일한 사용자 수를 조회해야 함: $tenantBResults")
  }

  @Test
  fun `테넌트 컨텍스트 전환 시 올바른 DB로 라우팅되어야 한다`() {
    repeat(5) { iteration ->
      TenantContext.setTenant("tenant-a")
      val userA = userRepository.findByUsernameAndEnabledTrue("tenancy-A")
      assertNotNull(userA, "iteration $iteration: tenant-a에서 tenancy-A를 찾을 수 있어야 함")
      assertEquals("tenant-a", userA?.tenantId)
      TenantContext.clear()

      TenantContext.setTenant("tenant-b")
      val userB = userRepository.findByUsernameAndEnabledTrue("tenancy-B")
      assertNotNull(userB, "iteration $iteration: tenant-b에서 tenancy-B를 찾을 수 있어야 함")
      assertEquals("tenant-b", userB?.tenantId)
      TenantContext.clear()

      TenantContext.setTenant("tenant-a")
      val userA2 = userRepository.findByUsernameAndEnabledTrue("tenancy-A")
      assertNotNull(userA2, "iteration $iteration: 다시 tenant-a에서 tenancy-A를 찾을 수 있어야 함")
      assertEquals(userA?.id, userA2?.id, "같은 사용자여야 함")
      TenantContext.clear()
    }
  }

  @Test
  fun `각 테넌트 DB 연결은 독립적이어야 한다`() {
    DriverManager.getConnection(TENANT_A_URL, TENANT_A_USER, TENANT_A_PASSWORD).use { conn ->
      assertTrue(conn.isValid(5))
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM users WHERE tenant_id = 'tenant-a'")
      rs.next()
      val countA = rs.getInt("cnt")
      assertTrue(countA >= 1, "tenant-a DB에는 최소 1명의 사용자가 있어야 함")
    }

    DriverManager.getConnection(TENANT_B_URL, TENANT_B_USER, TENANT_B_PASSWORD).use { conn ->
      assertTrue(conn.isValid(5))
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM users WHERE tenant_id = 'tenant-b'")
      rs.next()
      val countB = rs.getInt("cnt")
      assertTrue(countB >= 1, "tenant-b DB에는 최소 1명의 사용자가 있어야 함")
    }

    assertNotEquals(TENANT_A_URL, TENANT_B_URL, "각 테넌트는 독립적인 DB 엔드포인트를 가져야 함")
  }

  @Test
  fun `테넌트 컨텍스트 없이 조회 시 실패해야 한다`() {
    val exception = assertThrows<CannotCreateTransactionException> {
      userRepository.findAll()
    }

    val rootCause = exception.rootCause
    assertTrue(
      rootCause is IllegalStateException &&
        rootCause.message?.contains("Tenant context not set") == true,
      "Tenant context missing 에러가 발생해야 함"
    )
  }
}
