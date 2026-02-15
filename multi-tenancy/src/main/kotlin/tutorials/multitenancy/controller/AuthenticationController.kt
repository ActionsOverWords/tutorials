package tutorials.multitenancy.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import tutorials.multitenancy.base.extensions.logger
import tutorials.multitenancy.config.TenantContext
import tutorials.multitenancy.dto.LoginRequest
import tutorials.multitenancy.dto.LoginResponse
import tutorials.multitenancy.service.AuthenticationService

@RestController
@RequestMapping("/auth")
class AuthenticationController(
  private val authenticationService: AuthenticationService
) {
  private val logger by logger()

  @PostMapping("/login")
  fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
    logger.info("Login attempt for user: ${request.username}")

    val response = authenticationService.login(request)
    return ResponseEntity.ok(response)
  }

  @GetMapping("/me")
  fun getCurrentUser(authentication: Authentication): ResponseEntity<Map<String, Any>> {
    val tenant = TenantContext.getTenant()
    return ResponseEntity.ok(
      mapOf(
        "username" to authentication.name,
        "tenant" to (tenant ?: "unknown")
      )
    )
  }
}
