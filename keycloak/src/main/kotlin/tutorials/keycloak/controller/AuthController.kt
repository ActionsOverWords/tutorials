package tutorials.keycloak.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import tutorials.keycloak.auth.KeycloakAuthenticationException
import tutorials.keycloak.auth.KeycloakTokenService
import tutorials.keycloak.auth.KeycloakTokenView
import tutorials.keycloak.auth.PasswordLoginRequest

@RestController
class AuthController(
  private val keycloakTokenService: KeycloakTokenService
) {

  @PostMapping("/api/login/password")
  fun loginWithPassword(@RequestBody request: PasswordLoginRequest): KeycloakTokenView {
    if (request.username.isBlank() || request.password.isBlank()) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required")
    }

    return try {
      keycloakTokenService.passwordGrant(request.username, request.password)
    } catch (ex: KeycloakAuthenticationException) {
      throw ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.message)
    } catch (ex: Exception) {
      throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to connect to keycloak")
    }
  }
}
