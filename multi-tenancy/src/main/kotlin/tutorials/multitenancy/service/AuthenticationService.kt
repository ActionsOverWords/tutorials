package tutorials.multitenancy.service

import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import tutorials.multitenancy.base.extensions.logger
import tutorials.multitenancy.config.TenantContext
import tutorials.multitenancy.config.TenantResolver
import tutorials.multitenancy.dto.LoginRequest
import tutorials.multitenancy.dto.LoginResponse
import tutorials.multitenancy.repository.UserRepository
import tutorials.multitenancy.security.jwt.JwtProvider

@Service
class AuthenticationService(
  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder,
  private val jwtProvider: JwtProvider,
  private val tenantResolver: TenantResolver
) {
  private val logger by logger()

  fun login(request: LoginRequest): LoginResponse {
    val tenant = tenantResolver.resolve(request.tenant)
    TenantContext.setTenant(tenant)

    try {
      val user = findAndValidateUser(request.username, request.password, tenant)
      val token = jwtProvider.generateToken(user.username, tenant)
      logger.info("User logged in successfully: ${user.username} (tenant: $tenant)")

      return LoginResponse(
        token = token,
        username = user.username,
        tenant = tenant
      )
    } finally {
      TenantContext.clear()
    }
  }

  private fun findAndValidateUser(username: String, password: String, tenant: String) =
    userRepository.findByUsernameAndEnabledTrue(username)
      ?.also { user ->
        validatePassword(password, user.password)
        validateTenant(user.tenantId, tenant)
      }
      ?: throw BadCredentialsException("Invalid username or password")

  private fun validatePassword(rawPassword: String, encodedPassword: String) {
    if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
      throw BadCredentialsException("Invalid username or password")
    }
  }

  private fun validateTenant(userTenantId: String, expectedTenant: String) {
    if (userTenantId != expectedTenant) {
      throw BadCredentialsException("Tenant mismatch")
    }
  }
}
