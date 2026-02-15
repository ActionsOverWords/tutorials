package tutorials.keycloak.config

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.PostExchange
import tutorials.keycloak.auth.KeycloakTokenResponse

interface KeycloakClient {
  @PostExchange(
    url = "/protocol/openid-connect/token",
    contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE
  )
  fun issuePasswordToken(
    @RequestParam("grant_type") grantType: String,
    @RequestParam("client_id") clientId: String,
    @RequestParam("client_secret") clientSecret: String,
    @RequestParam("username") username: String,
    @RequestParam("password") password: String
  ): KeycloakTokenResponse
}
