package tutorials.keycloak.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tutorials.keycloak.config.KeycloakClient

@Service
class KeycloakTokenService(
  private val keycloakClient: KeycloakClient,
  @param:Value("\${spring.security.oauth2.client.registration.keycloak.client-id}") private val clientId: String,
  @param:Value("\${spring.security.oauth2.client.registration.keycloak.client-secret}") private val clientSecret: String
) {
  fun passwordGrant(username: String, password: String): KeycloakTokenView {
    val response = keycloakClient.issuePasswordToken(
      grantType = "password",
      clientId = clientId,
      clientSecret = clientSecret,
      username = username,
      password = password
    )

    return response.toView()
  }
}
