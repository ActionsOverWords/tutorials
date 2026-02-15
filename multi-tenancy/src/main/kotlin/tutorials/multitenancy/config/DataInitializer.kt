package tutorials.multitenancy.config

import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import tutorials.multitenancy.base.extensions.logger
import tutorials.multitenancy.domain.User
import tutorials.multitenancy.repository.UserRepository

@Component
class DataInitializer(
  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

  private val logger by logger()

  override fun run(vararg args: String?) {
    initializeTenantData("tenant-a", "tenancy-A")
    initializeTenantData("tenant-b", "tenancy-B")
  }

  private fun initializeTenantData(tenantId: String, username: String) {
    TenantContext.setTenant(tenantId)

    try {
      val existingUser = userRepository.findByUsername(username)
      if (existingUser == null) {
        val user = User(
          username = username,
          password = passwordEncoder.encode("tutorial"),
          tenantId = tenantId,
          enabled = true
        )
        userRepository.save(user)
        logger.info("Initialized user: $username in tenant: $tenantId")
      } else {
        logger.info("User already exists: $username in tenant: $tenantId")
      }
    } catch (e: Exception) {
      logger.error("Failed to initialize tenant: $tenantId", e)
    } finally {
      TenantContext.clear()
    }
  }
}
