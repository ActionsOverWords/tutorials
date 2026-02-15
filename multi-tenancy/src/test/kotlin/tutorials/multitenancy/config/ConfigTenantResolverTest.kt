package tutorials.multitenancy.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.BadCredentialsException

class ConfigTenantResolverTest {

  private val tenantProperties = TenantDataSourceProperties(
    tenants = mapOf(
      "tenant-a" to TenantConfig(
        url = "jdbc:mariadb://localhost:3316/tenantdb",
        username = "tenant_a_user",
        password = "tenant_a_pass"
      ),
      "tenant-b" to TenantConfig(
        url = "jdbc:mariadb://localhost:3317/tenantdb",
        username = "tenant_b_user",
        password = "tenant_b_pass"
      )
    )
  )

  private val resolver = ConfigTenantResolver(tenantProperties)

  @Test
  fun `등록된 테넌트는 정규화되어 반환되어야 한다`() {
    assertEquals("tenant-a", resolver.resolve("TENANT-A"))
    assertEquals("tenant-b", resolver.resolve(" tenant-b "))
  }

  @Test
  fun `테넌트가 비어있으면 예외가 발생해야 한다`() {
    val ex1 = assertThrows<BadCredentialsException> { resolver.resolve(null) }
    val ex2 = assertThrows<BadCredentialsException> { resolver.resolve("   ") }

    assertEquals("Tenant is required", ex1.message)
    assertEquals("Tenant is required", ex2.message)
  }

  @Test
  fun `미등록 테넌트는 예외가 발생해야 한다`() {
    val ex = assertThrows<BadCredentialsException> { resolver.resolve("tenant-c") }
    assertEquals("Invalid tenant", ex.message)
  }
}
