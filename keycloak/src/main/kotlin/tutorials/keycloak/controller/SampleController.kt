package tutorials.keycloak.controller

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SampleController {

  @GetMapping("/api/public")
  fun publicEndpoint(): Map<String, String> {
    return mapOf("message" to "public endpoint")
  }

  @GetMapping("/api/secure")
  fun secureEndpoint(authentication: Authentication): Map<String, String?> {
    return when (authentication) {
      is JwtAuthenticationToken -> mapOf(
        "authType" to "jwt",
        "principal" to authentication.name,
        "subject" to authentication.token.subject,
        "issuer" to authentication.token.issuer?.toString(),
        "clientRegistrationId" to null
      )
      is OAuth2AuthenticationToken -> mapOf(
        "authType" to "session",
        "principal" to (
          authentication.principal.attributes["preferred_username"]?.toString()
            ?: authentication.name
          ),
        "subject" to null,
        "issuer" to null,
        "clientRegistrationId" to authentication.authorizedClientRegistrationId
      )
      else -> mapOf(
        "authType" to authentication.javaClass.simpleName,
        "principal" to authentication.name,
        "subject" to null,
        "issuer" to null,
        "clientRegistrationId" to null
      )
    }
  }
}
